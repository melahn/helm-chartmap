package com.melahn.util.helm;

import static com.melahn.util.test.ChartMapTestUtil.isWindows;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import com.melahn.util.helm.model.HelmChart;
import com.melahn.util.helm.model.HelmChartRepoLocal;
import com.melahn.util.helm.model.HelmDeploymentContainer;
import com.melahn.util.helm.model.HelmDeploymentSpec;
import com.melahn.util.helm.model.HelmDeploymentSpecTemplate;
import com.melahn.util.helm.model.HelmDeploymentSpecTemplateSpec;
import com.melahn.util.helm.model.HelmDeploymentTemplate;
import com.melahn.util.helm.model.HelmMaintainer;
import com.melahn.util.test.ChartMapTestUtil;

class ChartMapTest {
    static final String APPR_BASE_NAME = "helm-chartmap-test-chart";
    static final String URL_BASE_NAME = "test-chart-file";
    static final String DIVIDER = "-------------------------------------";
    static final PrintStream INITIAL_OUT = System.out;
    // use INPUT_FILE_NAME_1 for test cases where you must use the refresh flag since it contains subcharts that are not in any helm repo
    static final String INPUT_FILE_NAME_1 = "src/test/resource/test-chart-file-1.tgz";
    // use INPUT_FILE_NAME_2 for test cases where you must not use the refresh flag since it contains old helm chart repo names
    static final String INPUT_FILE_NAME_2 = "src/test/resource/test-chart-file-2.tgz";
    static final String TARGET_TEST = "target/test";
    static final String TARGET_TEST_DIR = Paths.get(TARGET_TEST).toString();
    static final Path OUTPUT_PUML_PATH_RV = Paths.get(TARGET_TEST_DIR, "testChartFileRV.puml");
    static final Path OUTPUT_PUML_PATH_NRV = Paths.get(TARGET_TEST_DIR, "testChartFileNRV.puml");
    static final Path OUTPUT_PUML_PATH_RNV = Paths.get(TARGET_TEST_DIR, "testChartFileRNV.puml");
    static final Path OUTPUT_PUML_PATH_NRNV = Paths.get(TARGET_TEST_DIR, "testChartFileNRNV.puml");
    static final Path OUTPUT_PNG_PATH_NRNV = Paths.get(TARGET_TEST_DIR, "testChartFileNRNV.png");
    static final Path OUTPUT_TEXT_PATH_RV = Paths.get(TARGET_TEST_DIR, "testChartFileRV.txt");
    static final Path OUTPUT_TEXT_PATH_NRV = Paths.get(TARGET_TEST_DIR, "testChartFileNRV.txt");
    static final Path OUTPUT_TEXT_PATH_RNV = Paths.get(TARGET_TEST_DIR, "testChartFileRNV.txt");
    static final Path OUTPUT_TEXT_PATH_NRNV = Paths.get(TARGET_TEST_DIR, "testChartFileNRNV.txt");
    static final Path OUTPUT_JSON_PATH_RV = Paths.get(TARGET_TEST_DIR, "testChartFileRV.json");
    static final Path OUTPUT_JSON_PATH_NRV = Paths.get(TARGET_TEST_DIR, "testChartFileNRV.json");
    static final Path OUTPUT_JSON_PATH_RNV = Paths.get(TARGET_TEST_DIR, "testChartFileRNV.json");
    static final Path OUTPUT_JSON_PATH_NRNV = Paths.get(TARGET_TEST_DIR, "testChartFileNRNV.json");
    static final Path OUTPUT_APPR_PUML_PATH = Paths.get(TARGET_TEST_DIR, APPR_BASE_NAME.concat(".puml"));
    static final Path OUTPUT_APPR_PNG_PATH = Paths.get(TARGET_TEST_DIR, APPR_BASE_NAME.concat(".png"));
    static final Path OUTPUT_CHART_NAME_PUML_PATH = Paths.get(TARGET_TEST_DIR, "nginx:11.1.5.puml");
    static final Path OUTPUT_CHART_NAME_PNG_PATH = Paths.get(TARGET_TEST_DIR, "nginx:11.1.5.png");
    static final Path OUTPUT_CHART_URL_PUML_PATH = Paths.get(TARGET_TEST_DIR, URL_BASE_NAME.concat(".puml"));
    static final Path OUTPUT_CHART_URL_PNG_PATH = Paths.get(TARGET_TEST_DIR, URL_BASE_NAME.concat(".png"));
    static final String TEST_APPR_CHART = "quay.io/melahn/helm-chartmap-test-chart@1.0.2";
    static final String TEST_CHART_NAME = "nginx:11.1.5";
    static final String TEST_CHART_URL = "https://github.com/melahn/helm-chartmap/raw/master/".concat(INPUT_FILE_NAME_1);
    static final Path TEST_ONE_FILE_ZIP_PATH = Paths.get("src/test/resource/test-onefile.tgz");
    static final Path TEST_ENV_FILE_PATH = Paths.get("resource/example/example-env-spec.yaml");
    static final String TEST_FAKE_FILE_NAME = "src/test/resource/test-fakechart.tgz";
    static final int TIMEOUT_DEFAULT = 0; // use ChartMap's TIMEOUT_DEFAULT
    static final int TIMEOUT_DOUBLE = ChartMap.TIMEOUT_DEFAULT * 2; 

    @BeforeAll
    static void setUp() {
        System.out.println(DIVIDER.concat(" UNIT TESTS START ").concat(DIVIDER));
        try {
            ChartMapTestUtil.cleanDirectory(OUTPUT_PUML_PATH_RV.getParent());
            Files.createDirectories(OUTPUT_PUML_PATH_RV.getParent());
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @AfterAll
    static void cleanUp() {
        /**
         * No cleanup to do after test. I don't delete the generated files because they
         * might be handy to have around to diagnose issues in test failures. They are
         * deleted anyway when the test is next run.
         */
        System.out.println("Test complete. Any generated file can be found in "
                .concat(Paths.get(TARGET_TEST_DIR).toAbsolutePath().toString()));
        System.out.println(DIVIDER.concat(" UNIT TESTS END ").concat(DIVIDER));
    }

    /**
     * Tests the WeightedDeploymentTemplate inner class.
     * 
     * @throws ChartMapException
     */
    @Test
    void WeightedDeploymentTemplateTest() throws ChartMapException {
        ChartMap cm = createTestMapV11(ChartOption.FILENAME, TEST_FAKE_FILE_NAME, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        HelmDeploymentTemplate hdt = new HelmDeploymentTemplate();
        ChartMap.WeightedDeploymentTemplate wdt = cm.new WeightedDeploymentTemplate("a/b/c/d/e", hdt);
        wdt.setTemplate(hdt);
        assertSame(hdt, wdt.getTemplate());
        assertEquals(5, wdt.getWeight());
        ChartMap.WeightedDeploymentTemplate wdt2 = cm.new WeightedDeploymentTemplate("", hdt);
        assertEquals(1, wdt2.getWeight());
        ChartMap.WeightedDeploymentTemplate wdt3 = cm.new WeightedDeploymentTemplate(null, hdt);
        assertEquals(ChartMap.MAX_WEIGHT, wdt3.getWeight());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the processWeights and getWeight methods.
     * 
     * @throws ChartMapException
     */
    @Test
    void processWeightsTest() throws ChartMapException {
        ChartMap cm = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, Paths.get(TARGET_TEST, "processWeights.txt"), false, true,
                false);
        ChartMap scm = spy(cm);
        doReturn(0).when(scm).getWeight(anyString());
        try {
            scm.print();
        } catch (Exception e) {
            assertTrue(true); // no exception is expected
        }
        assertEquals(ChartMap.MAX_WEIGHT, cm.getWeight(null));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the runTemplateCommand methods.
     * 
     * @throws ChartMapException
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void runTemplateCommandTest() throws ChartMapException, InterruptedException, IOException {
        ChartMap cm = createTestMapV11(ChartOption.FILENAME, TEST_FAKE_FILE_NAME, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        // test that a bogus directory will cause an IO exception
        HelmChart h = new HelmChart();
        h.setName("fooChart");
        assertThrows(IOException.class, () -> cm.runTemplateCommand(new File(TARGET_TEST, "foo"), h));
        System.out.println("IOException thrown as expected");
        // create a templates file that will be in the way of the one to be created by
        // the runtTemplateCommand method so
        // as to induce a ChartMapException
        Path d = Paths.get(TARGET_TEST, "runTemplateCommandTestDir");
        Path t = Paths.get(TARGET_TEST, "runTemplateCommandTestDir", h.getName(), "templates");
        Path y = Paths.get(t.toString(), "com.melahn.util.helm.ChartMap_renderedtemplates.yaml");
        Files.createDirectories(t);
        Files.createFile(y);
        assertThrows(ChartMapException.class, () -> cm.runTemplateCommand(d.toFile(), h));
        System.out.println("ChartMapException thrown as expected");
        // test the IOException case when reading the output of the template command
        Process p1 = Runtime.getRuntime().exec("echo", null);
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            assertThrows(ChartMapException.class, () -> cm.runTemplateCommand(new File("./"), p1, null));
            assertTrue(
                    ChartMapTestUtil.streamContains(o, "IOException running template command"));
            System.setOut(INITIAL_OUT);
            System.out.println("IOException -> ChartMapException thrown as expected when a bogus file is used");
        }
        // test the bad exit value case when running template command using a spy
        Process p2 = Runtime.getRuntime().exec("echo", null);
        Process sp2 = spy(p2);
        doReturn(666).when(sp2).exitValue();
        InputStream es = new ByteArrayInputStream(new byte[] { 'f', 'o', 'o', 'b', 'a', 'r' }); // needed to test the
                                                                                                // logging of the error
                                                                                                // stream
        doReturn(es).when(sp2).getErrorStream();
        try (ByteArrayOutputStream o = new ByteArrayOutputStream();
                ByteArrayOutputStream e = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            assertThrows(ChartMapException.class, () -> cm.runTemplateCommand(new File(TARGET_TEST, "foo"), sp2, new HelmChart()));
            assertTrue(
                    ChartMapTestUtil.streamContains(o, "Error running template command. Exit Value = 666."));
            assertTrue(
                    ChartMapTestUtil.streamContains(o, "foobar")); // put there by the spy input stream
            System.setOut(INITIAL_OUT);
            System.out.println(
                    "IOException -> ChartMapException thrown as expected when the template process returns 666");
        }
        // test for InterruptedException using a spy
        Process p3 = Runtime.getRuntime().exec("echo", null);
        Process sp3 = spy(p3);
        doThrow(InterruptedException.class).when(sp3).waitFor(cm.getTimeout(), TimeUnit.SECONDS);
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            assertThrows(ChartMapException.class, () -> cm.runTemplateCommand(new File(TARGET_TEST, "foo"), sp3, new HelmChart()));
            assertTrue(
                    ChartMapTestUtil.streamContains(o, "InterruptedException running template command"));
            System.setOut(INITIAL_OUT);
            System.out.println("InterruptedException -> ChartMapException thrown as expected");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the getTemplateArray method.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void getTemplateArrayTest() throws ChartMapException, IOException {
        ChartMap cm = createTestMapV11(ChartOption.FILENAME, TEST_FAKE_FILE_NAME, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            // tests the getTemplateArray(File d, String s) signature
            String chartName = "foochart";
            Path p1 = Paths.get(TARGET_TEST, "getTemplateArrayTestFile1");
            Files.createFile(p1);
            // file is a template
            Files.write(p1, ChartMap.START_OF_TEMPLATE.concat(chartName).concat("/templates").getBytes());
            cm.getTemplateArray(p1.toFile(), chartName); // tests that file is a template
            assertFalse(ChartMapTestUtil.streamContains(o, "Exception creating template array"));
            Path p2 = Paths.get(TARGET_TEST, "getTemplateArrayTestFile2");
            Files.createFile(p2);
            // file is not a template (no '#')
            Files.write(p2, " ".concat(ChartMap.START_OF_TEMPLATE).concat(chartName).concat("/templates").getBytes());
            // tests that line from file is long enough but is not a template
            cm.getTemplateArray(p2.toFile(), chartName);
            assertFalse(ChartMapTestUtil.streamContains(o, "Exception creating template array"));
            Path p3 = Paths.get(TARGET_TEST, "getTemplateArrayTestFile3");
            Files.createFile(p3);
            Files.write(p3, " ".getBytes()); // file is not a template (too short)
            // tests that line from file is not long enough and so is not a template
            cm.getTemplateArray(p3.toFile(), chartName);
            assertFalse(ChartMapTestUtil.streamContains(o, "Exception creating template array"));
            cm.getTemplateArray(new File("./"), chartName); // induces an IOException but one that is not thrown
            assertTrue(ChartMapTestUtil.streamContains(o, "IOException creating template array in . with line null"));
            // tests the getTemplateArray(File d, File f) signature
            cm.getTemplateArray(new File("./"), new File("./")); // induces an IOException but one that is not thrown
            assertTrue(ChartMapTestUtil.streamContains(o, "IOException creating template array in . with line null"));
            System.setOut(INITIAL_OUT);
            System.out.println("Expected logged error found");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the error cases in printMap method throws the expected exception when
     * the chart is null. All the
     * other cases are tested as part of the other tests in this file.
     * 
     * @throws ChartMapException
     */
    @Test
    void printMapTest() throws ChartMapException {
        ChartMap cm1 = new ChartMap();
        cm1.printMap();
        assertEquals(null, cm1.getChart());
        ChartMap cm2 = createTestMapV11(ChartOption.FILENAME, TEST_FAKE_FILE_NAME, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        cm2.print();
        cm2.setOutputFilename("./."); /// forces the exception because it is a directory
        assertThrows(ChartMapException.class, () -> cm2.printMap());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the generateImage method.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void generateImageTest() throws ChartMapException, IOException {
        String f = "generateImageTestFile";
        Path p = Paths.get(TARGET_TEST, f.concat(".puml"));
        Files.createFile(p);
        Files.write(p, "@startuml foo".getBytes());
        ChartMap cm1 = new ChartMap();
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            // test that an empty file generates no image and the right warning
            cm1.generateImage(p.toString());
            assertTrue(
                    ChartMapTestUtil.streamContains(o,
                            String.format("Warning: Image file %s.png was not generated from %s", f, p.toString())));
            // test that we handle net.sourceforge.plantuml.SourceFileReader.hasError using
            // a spy
            Files.write(p, "\n@enduml".getBytes());
            net.sourceforge.plantuml.SourceFileReader r = new net.sourceforge.plantuml.SourceFileReader(
                    new File(p.toString()));
            net.sourceforge.plantuml.SourceFileReader sr = spy(r);
            doReturn(true).when(sr).hasError();
            ChartMap cm2 = new ChartMap();
            ChartMap scm2 = spy(cm2);
            doReturn(sr).when(scm2).getPlantUMLReader(any(File.class));
            scm2.generateImage(f);
            assertTrue(
                    ChartMapTestUtil.streamContains(o,
                            "Error in net.sourceforge.plantuml.GeneratedImage trying to generate image"));
            // test that we throw an IOException->ChartMapException when the png file cannot
            // be created
            assertThrows(ChartMapException.class, () -> cm1.generateImage("./."));
            assertTrue(
                    ChartMapTestUtil.streamContains(o, "Error generating image file"));
            System.setOut(INITIAL_OUT);
            System.out.println("Expected warnings found when trying to generate image from bad puml files");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the parseChartName method.
     * 
     * @throws ChartMapException
     */
    @Test
    void parseChartNameTest() throws ChartMapException {
        String c1 = "foo:1.0.0";
        ChartMap cm1 = new ChartMap();
        assertDoesNotThrow(() -> cm1.parseChartName(c1));
        assertEquals("foo", cm1.getChartName());
        assertEquals("1.0.0", cm1.getChartVersion());
        System.out.println(String.format("%s is a valid chart name", c1));
        ChartMap cm2 = new ChartMap();
        String c2 = "foo:1.0.0-ea";
        assertDoesNotThrow(() -> cm2.parseChartName(c2));
        assertEquals("foo", cm2.getChartName());
        assertEquals("1.0.0-ea", cm2.getChartVersion());
        System.out.println(String.format("%s is a valid chart name even though it is not SemVer compliant", c2));
        ChartMap cm3 = new ChartMap();
        String c3 = "foo:1.0.0@ea";
        assertThrows(ChartMapException.class, () -> cm3.parseChartName(c3));
        System.out.println(String.format("%s is not a valid chart name", c3));
        ChartMap cm4 = new ChartMap();
        String c4 = ":1.0.0";
        assertThrows(ChartMapException.class, () -> cm4.parseChartName(c4));
        System.out.println(String.format("%s is not a valid chart name", c4));
        ChartMap cm5 = new ChartMap();
        String c5 = "foo";
        assertThrows(ChartMapException.class, () -> cm5.parseChartName(c5));
        System.out.println(String.format("%s is not a valid chart name", c5));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the printChartDependencies method.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void printChartDependenciesTest() throws ChartMapException, IOException {
        // test for null getDiscoveredDependencies
        ChartMap cm1 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, true,
                false);
        cm1.print();
        HelmChart h1 = new HelmChart();
        h1.setName("foo");
        h1.setVersion("bar");
        HelmChart sh1 = spy(h1);
        doReturn(null).when(sh1).getDiscoveredDependencies();
        try {
            cm1.printChartDependencies(sh1);
        } catch (Exception e) {
            assertFalse(true); // No exception should be thrown even if getDiscoveredDependencies is null
        }
        // test that the ChartMapException is handled
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm2 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                    false);
            cm2.print();
            ChartMap scm2 = spy(cm2);
            ChartMapPrinter cmp2 = new TextChartMapPrinter(cm2, TARGET_TEST.concat("/print-chart-dependencies"), cm2.getCharts(), h1);
            ChartMapPrinter scmp2 = spy(cmp2);
            doReturn(scmp2).when(scm2).getPrinter();
            doThrow(ChartMapException.class).when(scmp2).printSectionHeader(any());
            HelmChart h2 = new HelmChart();
            h2.setName("nginx");
            h2.setVersion("11.1.5");
            scm2.printChartDependencies(h2);
            assertTrue(
                    ChartMapTestUtil.streamContains(o, "Error printing chart dependencies:"));
            System.setOut(INITIAL_OUT);
            System.out.println("Expected log error found");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the printContainerDependencies method.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void printContainerDependenciesTest() throws ChartMapException, IOException {
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                    false);
            cm.print();
            ChartMap scm = spy(cm);
            ChartMapPrinter cmp = new TextChartMapPrinter(cm, TARGET_TEST.concat("/container-dependencies-test"), cm.getCharts(), new HelmChart());
            ChartMapPrinter scmp = spy(cmp);
            doReturn(scmp).when(scm).getPrinter();
            doThrow(ChartMapException.class).when(scmp).printChartToImageDependency(any(), any());
            scm.printContainerDependencies();
            assertTrue(
                    ChartMapTestUtil.streamContains(o, "Error printing image dependencies:"));
            System.setOut(INITIAL_OUT);
            System.out.println("Expected log error found");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the printContainers method.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void printContainersTest() throws ChartMapException, IOException {
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                    false);
            cm.print();
            ChartMap scm = spy(cm);
            ChartMapPrinter cmp = new TextChartMapPrinter(cm, TARGET_TEST.concat("/print-containers-test"), cm.getCharts(), new HelmChart());
            ChartMapPrinter scmp = spy(cmp);
            doReturn(scmp).when(scm).getPrinter();
            doThrow(ChartMapException.class).when(scmp).printSectionHeader(any());
            scm.printContainers();
            assertTrue(
                    ChartMapTestUtil.streamContains(o, "Error printing images: null"));
            System.setOut(INITIAL_OUT);
            System.out.println("Expected log error found");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests that the detectPrintFormat method does not misbehave when a null is
     * passed.
     *
     * @throws ChartMapException
     */
    @Test
    void detectPrintFormatTest() throws ChartMapException {
        ChartMap cm = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        try {
            cm.detectPrintFormat(null);
        } catch (Exception e) {
            assertFalse(true); // no exception should be thrown
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the isStable method.
     * 
     * @throws ChartMapException
     */
    @Test
    void isStableTest() throws ChartMapException {
        ChartMap cm = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        // this set of images should not be stable
        Set<String> s = Stream.of("foo-stable", "foo-snapshot", "foo-alpha", "foo-beta", "foo-trial", "foo-rc")
             .collect(Collectors.toCollection(HashSet::new));
        String[] a = s.toArray(new String[0]);
        HelmChart h = getTestHelmChartWithContainers(a);
        assertFalse(cm.isStable(h, true));
        // this set of images should be stable
        s = Stream.of("foo-stable")
             .collect(Collectors.toCollection(HashSet::new));
        a = s.toArray(new String[0]);
        h = getTestHelmChartWithContainers(a);
        assertTrue(cm.isStable(h, true));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }
    /**
     * Helper function to return a HelmChart whose containers have images of the names
     * provided so the isStable method can be thoroughly tested.
     * 
     * @param s an array of iumage names
     * @return  a HelmChart whose containers have images of the names provided.
     */
    HelmChart getTestHelmChartWithContainers(String s[]) {
        // create a HelmDeploymentContainer array to hold the images
        HelmDeploymentContainer[] hdc = new HelmDeploymentContainer[s.length];
        for (int i = 0; i < s.length; i++) {
            hdc[i] = new HelmDeploymentContainer();
            hdc[i].setImage(s[i]);
        }
        // put this array into a spec template spec
        HelmDeploymentSpecTemplateSpec hdsts = new HelmDeploymentSpecTemplateSpec();
        hdsts.setContainers(hdc);
        // put this spec template spec into a spec template
        HelmDeploymentSpecTemplate hdst = new HelmDeploymentSpecTemplate();
        hdst.setSpec(hdsts);
        // put this spec template into a deployment spec
        HelmDeploymentSpec hds = new HelmDeploymentSpec();
        hds.setTemplate(hdst);
        // put this deployment spec into a deployment template
        HelmDeploymentTemplate hdt = new HelmDeploymentTemplate();
        hdt.setSpec(hds);
        // ... and finally create a helm chart with this deployment template and return it
        HelmChart h = new HelmChart();
        Set<HelmDeploymentTemplate> dt = h.getDeploymentTemplates();
        dt.add(hdt);
        h.setDeploymentTemplates(dt);
        return h;
    }

    /**
     * Tests the processTemplateYaml method.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void processTemplateYamlTest() throws ChartMapException, IOException {
        String c = "fooChart";
        String l0 = "foo"; // split line length not > 1
        String l1 = ChartMap.START_OF_TEMPLATE.concat(c).concat("/templates/").concat(ChartMap.RENDERED_TEMPLATE_FILE);
        String l2 = "foo".concat(c).concat("/templates/").concat(ChartMap.RENDERED_TEMPLATE_FILE);
        String l3 = ChartMap.START_OF_TEMPLATE.concat(c).concat("/nottemplates/");
        String l4 = ChartMap.START_OF_TEMPLATE.concat(c).concat("/nottemplates/").concat("notrenderedtemplatesfile");
        ;
        ChartMap cm = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        // Exercise all the mathematical variations one can find in the yaml line that
        // might signal a helm template element
        try {
            cm.processTemplateYaml(l0, new BufferedReader(new StringReader(c)),
                    new ArrayList<>(Arrays.asList(Boolean.TRUE)), c);
            cm.processTemplateYaml(l1, new BufferedReader(new StringReader(c)),
                    new ArrayList<>(Arrays.asList(Boolean.TRUE)), c);
            cm.processTemplateYaml(l2, new BufferedReader(new StringReader(c)),
                    new ArrayList<>(Arrays.asList(Boolean.TRUE)), c);
            cm.processTemplateYaml(l3, new BufferedReader(new StringReader(c)),
                    new ArrayList<>(Arrays.asList(Boolean.TRUE)), c);
            cm.processTemplateYaml(l4, new BufferedReader(new StringReader(c)),
                    new ArrayList<>(Arrays.asList(Boolean.TRUE)), c);
        } catch (Exception e) {
            assertFalse(true); // no exception is expected
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the ChartMap.unpackTestChart method
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void unpackChartTest() throws ChartMapException, IOException {
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" starting"));
        ChartMap cm1 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        cm1.setChartName("foo");
        cm1.createTempDir();
        assertThrows(ChartMapException.class, () -> cm1.unpackChart("foo"));
        System.out.println("ChartMapException thrown as expected");
        // force ChartMapException path when no temp dir
        ChartMap cm2 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        cm2.setChartName("foo");
        assertThrows(ChartMapException.class, () -> cm2.unpackChart("foo"));
        System.out.println("ChartMapException thrown as expected");
        // force ChartMapException path when null chartmap passed
        cm2.createTempDir();
        assertThrows(ChartMapException.class, () -> cm2.unpackChart(null));
        // test when the tgz has no directory
        System.out.println("ChartMapException thrown as expected");
        ChartMap cm3 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        cm3.setChartName(null);
        cm3.setChartVersion(null);
        cm3.createTempDir();
        try (ByteArrayOutputStream unpackCharttestOut = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(unpackCharttestOut));
            assertThrows(ChartMapException.class, () -> cm3.unpackChart(TEST_ONE_FILE_ZIP_PATH.toString()));
            assertTrue(
                    ChartMapTestUtil.streamContains(unpackCharttestOut, "Archive content does not appear to be valid"));
            System.setOut(INITIAL_OUT);
            System.out.println("ChartMapException thrown as expected");
        }
        // test when the tgz has no directory, this time with a non-null chartname and a
        // null version
        ChartMap cm4 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        cm4.setChartName("foo");
        cm4.setChartVersion(null);
        cm4.createTempDir();
        try (ByteArrayOutputStream unpackCharttestOut = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(unpackCharttestOut));
            assertThrows(ChartMapException.class, () -> cm4.unpackChart(TEST_ONE_FILE_ZIP_PATH.toString()));
            assertTrue(
                    ChartMapTestUtil.streamContains(unpackCharttestOut, "Archive content does not appear to be valid"));
            System.setOut(INITIAL_OUT);
            System.out.println("ChartMapException thrown as expected");
        }
        // test when the tgz has no directory, this time with a non-null version and a
        // null chartname to complete all the variations
        ChartMap cm5 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        cm5.setChartName(null);
        cm5.setChartVersion("1.1.1");
        cm5.createTempDir();
        try (ByteArrayOutputStream unpackCharttestOut = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(unpackCharttestOut));
            assertThrows(ChartMapException.class, () -> cm5.unpackChart(TEST_ONE_FILE_ZIP_PATH.toString()));
            assertTrue(
                    ChartMapTestUtil.streamContains(unpackCharttestOut, "Archive content does not appear to be valid"));
            System.setOut(INITIAL_OUT);
            System.out.println("ChartMapException thrown as expected");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests some utility methods in ChartMap.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void utilityMethodsTest() throws ChartMapException, IOException {
        // Test some getters related to the print format and file name
        String b = ChartMap.getBaseName(Paths.get("./target").toString());
        assertEquals(null, b);
        ChartMap cm1 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        cm1.print();
        assertEquals(PrintFormat.PLANTUML, cm1.getPrintFormat());
        cm1.setPrintFormat(PrintFormat.JSON);
        assertEquals(PrintFormat.JSON, cm1.getPrintFormat());
        assertEquals("chartmap.text", cm1.getDefaultOutputFilename());

        // Test main exception path
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            assertThrows(ChartMapException.class, () -> ChartMap.main(new String[] { "-f foo" }));
            assertTrue(ChartMapTestUtil.streamContains(o, "ChartMapException:"));
            System.setOut(INITIAL_OUT);
            System.out.println("ChartMapException thrown as expected");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the unusual case where the weighted template is not found when applying
     * the templates.
     * 
     * @throws ChartMapException
     */
    @Test
    void applyTemplatesTest() throws ChartMapException {
        ChartMap cm = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        cm.print();
        HelmChart h = cm.chartsReferenced.get("nginx", "11.1.5");
        HashMap<String, ChartMap.WeightedDeploymentTemplate> dtr = cm.deploymentTemplatesReferenced;
        for (HelmDeploymentTemplate t : h.getDeploymentTemplates()) {
            dtr.remove(t.getFileName()); // this will force the case where the weighted template was not found
            cm.deploymentTemplatesReferenced = dtr;
        }
        cm.applyTemplates();
        assertTrue(h.getDeploymentTemplates().isEmpty());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the unusual case where a helm template file has empty content and where
     * it
     * contains an bbject whose kind is not a string.
     * 
     * @throws ChartMapException
     * @throws IOException
     * 
     */
    @Test
    void renderTemplatesTest() throws ChartMapException, IOException {
        Path notMapPath = Paths.get(TARGET_TEST, "notMap.yaml");
        Files.deleteIfExists(notMapPath);
        notMapPath = Files.createFile(notMapPath);
        Files.write(notMapPath, " foo ".getBytes()); // not a Map
        ChartMap cm1 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        ChartMap scm1 = spy(cm1);
        doReturn(notMapPath.toFile()).when(scm1).runTemplateCommand(any(File.class), any(HelmChart.class));
        try {
            // case where Yaml.loadAll does not yield a Map
            scm1.renderTemplates(new File(TARGET_TEST), new HelmChart(), new HelmChart());
        } catch (Exception e) {
            assertFalse(true); // No exception should be thrown
        }
        Path notKindPath = Paths.get(TARGET_TEST, "notKind.yaml");
        Files.deleteIfExists(notKindPath);
        notKindPath = Files.createFile(notKindPath);
        Files.write(notKindPath, "notKind: foo\n".getBytes()); // something not a 'kind' object
        ChartMap cm2 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        ChartMap scm2 = spy(cm2);
        doReturn(notKindPath.toFile()).when(scm2).runTemplateCommand(any(File.class), any(HelmChart.class));
        try {
            // case where Yaml.loadAll does not yield a Map
            scm2.renderTemplates(new File(TARGET_TEST), new HelmChart(), new HelmChart());
        } catch (Exception e) {
            assertFalse(true); // No exception should be thrown here either
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests some error conditions in collectValues
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void collectValuesTest() throws ChartMapException, IOException {
        ChartMap cm1 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        // test the cases where one or both of the parameters are null
        try {
            cm1.collectValues("fooCollectValuesFile", null);
            cm1.collectValues(null, new HelmChart());
            cm1.collectValues(null, null);
        } catch (Exception e) {
            assertFalse(true); // No exception should be thrown
        }
        // test the return of something other than a Map from yaml.load
        Path pathOfEmptyFile = Files.createFile(Paths.get(TARGET_TEST, "values.yaml"));
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm2 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                    true);
            cm2.setVerboseLogLevel(); // this is called explicitly because print is not called
            cm2.collectValues(TARGET_TEST, new HelmChart());
            assertTrue(
                    ChartMapTestUtil.streamContains(o,
                            String.format("The values.yaml file: %s could not be parsed. Possibly it is empty.",
                                    pathOfEmptyFile.toAbsolutePath())));

            System.setOut(INITIAL_OUT);
            System.out.println("Expected error message found");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the ChartMap.loadLocalRepos method, focusing on the corner case where
     * an IOException is caught and converted to a thrown ChartMapException. 
     * 
     * @throws ChartMapException
     */
    @Test
    void loadLocalReposTest() throws ChartMapException, IOException {
        ChartMap cm = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        // set a bogus helm config path to induce the exception
        cm.setHelmRepositoryConfigPath("bogus");
        assertThrows(ChartMapException.class, () -> cm.loadLocalRepos());
        System.out.println("IOException -> ChartMapException thrown as expected");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the ChartMap.checkHelmVersion method.
     * 
     * @throws ChartMapException
     */
    @Test
    void checkHelmVersionTest() throws ChartMapException, InterruptedException, IOException {
        // Test a bad helm command
        ChartMap cm1 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        ChartMap scm1 = spy(cm1);
        // Use a command that is the same across all the OS's so it will run
        Process p1 = Runtime.getRuntime().exec(new String[] { "echo", "I am the foo process" });
        Process sp1 = spy(p1);
        doReturn(sp1).when(scm1).getProcess(any(), eq(null));
        doReturn("helm").when(scm1).getHelmCommand();
        // Return 1 to mimic a bad helm command forcing a ChartMapException
        doReturn(1).when(sp1).exitValue();
        assertThrows(ChartMapException.class, () -> scm1.checkHelmVersion());
        System.out.println("ChartMapException thrown as expected");
        // Test not helm version 3
        ChartMap cm2 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        ChartMap scm2 = spy(cm2);
        // Use a command that is the same across all the OS's to mimic a helm not v3
        Process p2 = Runtime.getRuntime().exec(new String[] { "echo", "I am not helm version 3" });
        doReturn(p2).when(scm2).getProcess(any(), eq(null));
        assertThrows(ChartMapException.class, () -> scm2.checkHelmVersion());
        System.out.println("ChartMapException thrown as expected");
        // Use a command that will cause the process' BufferedReader to return null and
        // force the ChartMapException.
        ChartMap cm3 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        ChartMap scm3 = spy(cm3);
        String nullCommand = isWindows() ? "type" : "cat";
        String nullArgument = isWindows() ? "NUL" : "/dev/null";
        Process p3 = Runtime.getRuntime().exec(new String[] { nullCommand, nullArgument });
        doReturn(p3).when(scm3).getProcess(any(), eq(null));
        assertThrows(ChartMapException.class, () -> scm3.checkHelmVersion());
        System.out.println("ChartMapException thrown as expected");
        // Use a command that will cause the process' BufferedReader to just one
        // character and force the ChartMapException
        ChartMap cm4 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        ChartMap scm4 = spy(cm4);
        Process p4 = Runtime.getRuntime().exec(new String[] { "echo", "1" });
        doReturn(p4).when(scm4).getProcess(any(), eq(null));
        assertThrows(ChartMapException.class, () -> scm4.checkHelmVersion());
        System.out.println("ChartMapException thrown as expected");
        // Cause an IOException -> ChartMapException on getProcess()
        ChartMap cm5 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        ChartMap scm5 = spy(cm5);
        doThrow(IOException.class).when(scm5).getProcess(any(), eq(null));
        assertThrows(ChartMapException.class, () -> scm5.checkHelmVersion());
        System.out.println("IOException -> ChartMapException thrown as expected");
        // Cause an InterruptedException -> ChartMapException on waitFor()
        // Be careful to put InterruptedException case last in the test case since the
        // thread is not usable after that
        ChartMap cm6 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        ChartMap scm6 = spy(cm6);
        Process p6 = Runtime.getRuntime()
                .exec(new String[] { "echo", "I am going to throw an InterruptedException!!" });
        Process sp6 = spy(p6);
        doReturn(sp6).when(scm6).getProcess(any(), eq(null));
        doThrow(InterruptedException.class).when(sp6).waitFor(cm6.getTimeout(), TimeUnit.SECONDS);
        assertThrows(ChartMapException.class, () -> scm6.checkHelmVersion());
        System.out.println("InterruptedException -> ChartMapException thrown as expected");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the ChartMap.getHelmClientInformation method.
     * 
     * @throws ChartMapException
     */
    @Test
    void getHelmClientInformationTest() throws ChartMapException, InterruptedException, IOException {
        // Test the normal case
        ChartMap cm1 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        cm1.getHelmClientInformation();
        assertNotNull(cm1.getHelmCachePath());
        assertNotNull(cm1.getHelmConfigPath());
        assertNotNull(cm1.getHelmRepositoryCachePath());
        assertNotNull(cm1.getHelmRepositoryConfigPath());
        // Force File.setReadable to return false to test security protection of
        // temporary file
        ChartMap cm2 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        ChartMap scm2 = spy(cm2);
        File sf2 = spy(new File("helmTempSpyRead", ".txt"));
        doReturn(false).when(sf2).setReadable(true, true);
        doReturn(true).when(sf2).setWritable(true, true);
        doReturn(true).when(sf2).setExecutable(true, true);
        doReturn(sf2).when(scm2).getTempFile(anyString(),anyString());
        assertThrows(ChartMapException.class, () -> scm2.getHelmClientInformation());
        System.out.println("ChartMapException thrown as expected after setReadable");
        // Force File.setWritable to return false to test security protection of
        // temporary file
        ChartMap cm3 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        ChartMap scm3 = spy(cm3);
        File sf3 = spy(new File("helmTempSpyWrite", ".txt"));
        doReturn(true).when(sf3).setReadable(true, true);
        doReturn(false).when(sf3).setWritable(true, true);
        doReturn(true).when(sf3).setExecutable(true, true);
        doReturn(sf3).when(scm3).getTempFile(anyString(),anyString());
        assertThrows(ChartMapException.class, () -> scm3.getHelmClientInformation());
        System.out.println("ChartMapException thrown as expected after setWritable");
        // Force File.setExecutable to return false to test security protection of
        // temporary file
        ChartMap cm4 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        ChartMap scm4 = spy(cm4);
        File sf4 = spy(new File("helmTempSpyWrite", ".txt"));
        doReturn(true).when(sf4).setReadable(true, true);
        doReturn(true).when(sf4).setWritable(true, true);
        doReturn(false).when(sf4).setExecutable(true, true);
        doReturn(sf4).when(scm4).getTempFile(anyString(),anyString());
        assertThrows(ChartMapException.class, () -> scm4.getHelmClientInformation());
        System.out.println("ChartMapException thrown as expected after setExecutable");
        // Force ProcessBuilder to thrown an IOException to force helm env command failing
        ChartMap cm5 = createTestMapV11(ChartOption.APPRSPEC, TEST_APPR_CHART, OUTPUT_APPR_PUML_PATH, true, false, false);
        ChartMap scm5 = spy(cm5);
        doThrow(IOException.class).when(scm5).getProcessBuilder(any(), any());
        assertThrows(ChartMapException.class, () -> scm5.getHelmClientInformation());
        System.out.println("IOException -> ChartMapException thrown as expected");
        // Force Process.exitValue to return a non-zero exit value
        ChartMap cm6 = createTestMapV11(ChartOption.APPRSPEC, TEST_APPR_CHART, OUTPUT_APPR_PUML_PATH, true, false, false);
        ChartMap scm6 = spy(cm6);
        ProcessBuilder pb6 = new ProcessBuilder("foo", "bar");
        ProcessBuilder spb6 = spy(pb6);
        Process p6 = Runtime.getRuntime().exec(new String[] { "echo", "I am going to return a bad exitValue ... just watch me!!" });
        Process sp6 = spy(p6);
        doReturn(1).when(sp6).exitValue();
        doReturn(sp6).when(spb6).start();
        doReturn(spb6).when(scm6).getProcessBuilder(any(), any());
        assertThrows(ChartMapException.class, () -> scm6.getHelmClientInformation());
        System.out.println("IOException -> ChartMapException thrown as expected with simulated bad exit code");
        // Force Process.start to throw an InterruptedException
        ChartMap cm7 = createTestMapV11(ChartOption.APPRSPEC, TEST_APPR_CHART, OUTPUT_APPR_PUML_PATH, true, false, false);
        ChartMap scm7 = spy(cm7);
        ProcessBuilder pb7 = new ProcessBuilder("foo", "bar");
        ProcessBuilder spb7 = spy(pb7);
        Process p7 = Runtime.getRuntime().exec(new String[] { "echo", "I am going to throw an InterruptedException on waitFor ... just watch me!!" });
        Process sp7 = spy(p7);
        doThrow(InterruptedException.class).when(sp7).waitFor(cm7.getTimeout(), TimeUnit.SECONDS);
        doReturn(sp7).when(spb7).start();
        doReturn(spb7).when(scm7).getProcessBuilder(any(), any());
        assertThrows(ChartMapException.class, () -> scm7.getHelmClientInformation());
        System.out.println("InterruptedException -> ChartMapException thrown as expected with simulated bad waitFor");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }


    /**
     * Test the ChartMap.loadChartsFromCache method.
     * 
     * @throws ChartMapException if an error occured loading the charts
     * @throws IOException       if an error occured fabricating my cache yaml file
     */
    @Test
    void loadChartsFromCacheTest() throws ChartMapException, IOException {
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" starting"));
        HelmChartRepoLocal r = new HelmChartRepoLocal();
        // Fabricate a HelmChartRepoLocal (I only need tbe url for this test)
        r.setUrl("http://foo");
        String n = "foo";
        String v = "6.6.6";
        // fabricate a cache yaml file with one entry
        String s = "apiVersion: v1\nentries:\n  foo-chart:\n  - name: ".concat(n).concat("\n    version: ").concat(v)
        .concat("\n".concat("    urls:\n    - not_a_url\n"));
        String c = "loadChartsFromCacheTest.yaml";
        Path p = Paths.get(TARGET_TEST_DIR, c);
        File f = Files.createFile(p).toFile();
        byte[] b = s.getBytes();
        Files.write(p, b);
        // Create a test ChartMap and validate I can load the chart from my fabricated
        // cache. Note that it will compesate for a missing url in the local helm chart.
        ChartMap cm1 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                true);
        cm1.loadChartsFromCache(r, f);
        assertNotNull(cm1.getCharts().get("foo", "6.6.6"));
        System.out.println("tested loaded a fabricated HelmChart from the cache");
        // Test for a missing urls element in the cache
        Files.deleteIfExists(p);
        f = Files.createFile(p).toFile();
        s = "apiVersion: v1\nentries:\n  foo-chart:\n  - name: ".concat(n).concat("\n    version: ").concat(v)
                .concat("\n".concat("    urls:\n"));
        b = s.getBytes();
        Files.write(p, b);
        ChartMap cm2 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                true);
        cm2.loadChartsFromCache(r, f);
        assertNotNull(cm2.getCharts().get("foo", "6.6.6"));
        System.out.println("tested for a missing urls element in the cache");
        // Test for an empty string element in the urls array
        Files.deleteIfExists(p);
        f = Files.createFile(p).toFile();
        s = "apiVersion: v1\nentries:\n  foo-chart:\n  - name: ".concat(n).concat("\n    version: ").concat(v)
                .concat("\n".concat("    urls: []\n"));
        b = s.getBytes();
        Files.write(p, b);
        ChartMap cm3 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                true);
        cm3.loadChartsFromCache(r, f);
        assertNotNull(cm3.getCharts().get("foo", "6.6.6"));
        System.out.println("tested empty string element in the urls array");
        // test for an empty urls array in the cache
        Files.deleteIfExists(p);
        f = Files.createFile(p).toFile();
        s = "apiVersion: v1\nentries:\n  foo-chart:\n  - name: ".concat(n).concat("\n    version: ").concat(v)
                .concat("\n".concat("    urls: []\n"));
        b = s.getBytes();
        Files.write(p, b);
        ChartMap cm4 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                true);
        cm4.loadChartsFromCache(r, f);
        assertNotNull(cm4.getCharts().get("foo", "6.6.6"));
        System.out.println("tested empty urls array in the cache");

        // Test for an empty string element in the urls array
        Files.deleteIfExists(p);
        f = Files.createFile(p).toFile();
        s = "apiVersion: v1\nentries:\n  foo-chart:\n  - name: ".concat(n).concat("\n    version: ").concat(v)
                .concat("\n".concat("    urls:\n    - ''\n"));
        b = s.getBytes();
        Files.write(p, b);
        ChartMap cm5 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                true);
        cm5.loadChartsFromCache(r, f);
        assertNotNull(cm5.getCharts().get("foo", "6.6.6"));
        System.out.println("tested empty string element in the urls array");

        // Finally, force an Exception and check the log to complete all the possible
        // branches
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm6 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                    false);
            cm6.loadChartsFromCache(r, null);
            assertTrue(ChartMapTestUtil.streamContains(o, String.format("Error loading charts from helm cache: ")));
            System.setOut(INITIAL_OUT);
            System.out.println("Exception -> ChartMapException thrown as expected");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the ChartMap.printCharts method.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void printChartsTest() throws ChartMapException, IOException {
        // Test for a single Chart
        ChartMap cm1 = createTestMapV11(ChartOption.FILENAME, "src/test/resource/test-fakechart.tgz",
                Paths.get(TARGET_TEST, "test-fakechart.txt"), true, false, false);
        cm1.print();
        assertTrue(
                ChartMapTestUtil.fileContains(Paths.get(TARGET_TEST, "test-fakechart.txt"), "There is one referenced Helm Chart"));
        // Force a ChartMapException using a spy
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm2 = createTestMapV11(ChartOption.FILENAME, "src/test/resource/test-fakechart.tgz",
                    Paths.get(TARGET_TEST, "test-fakechart.txt"), true, false, false);
            ChartMap scm2 = spy(cm2);
            IChartMapPrinter sp2 = spy(IChartMapPrinter.class);
            doReturn(sp2).when(scm2).getPrinter();
            doThrow(ChartMapException.class).when(sp2).printSectionHeader(any(String.class));
            scm2.print();
            assertTrue(ChartMapTestUtil.streamContains(o, "IOException printing charts:"));
            System.setOut(INITIAL_OUT);
            System.out.println("ChartMapException thrown as expected");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));

    }

    /**
     * Tests the ChartMap.extractEmbeddedCharts method.
     * 
     * @throws ChartMapException
     */
    @Test
    void testExtractEmbeddedCharts() throws ChartMapException, IOException, RuntimeException {
        // Force a ChartMapException
        ChartMap cm1 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                true);
        try (MockedStatic<Files> mf = Mockito.mockStatic(Files.class)) {
            mf.when(() -> Files.walk(any(Path.class), anyInt())).thenThrow(IOException.class);
            System.out.print("An IOException is swallowed but expect this log message ... ");
            assertThrows(ChartMapException.class, () -> cm1.extractEmbeddedCharts("foo"));
            System.out.println("ChartMapException thrown as expected");
        }
        assertThrows(RuntimeException.class, () -> lambdaWrapper());
        System.out.println("RuntimeException thrown as expected");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /*
     * Raise an exception. Used as part of the test of the LambdaxcpetionWrapper.
     * 
     * @throws Exception
     */
    void raiseException(Exception e) throws Exception {
        throw e;
    }

    /*
     * a LambdaWrapper that just raises a RuntimeException to force that code path
     * in the ChartMap.lambdaExceptionWrapper.
     */
    void lambdaWrapper() throws IOException, RuntimeException {
        // Force the lambdaExceptionWrapper to throw a RuntimeException
        Path testExtractPath = Paths.get(TARGET_TEST, "test-extract");
        Files.deleteIfExists(Paths.get(testExtractPath.toString(), "f"));
        Files.deleteIfExists(testExtractPath);
        Path d = Files.createDirectories(testExtractPath);
        Files.createFile(Paths.get(testExtractPath.toString(), "f"));
        try (Stream<Path> walk = Files.walk(d, 1)) {
            walk.filter(Files::isRegularFile).collect(Collectors.toList())
                    .forEach(ChartMap.lambdaExceptionWrapper(p -> raiseException(new RuntimeException())));
        }
    }

    /**
     * Tests the ChartMap.resolveChartDependencies method.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void resolveChartDependenciesTest() throws ChartMapException, IOException {
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm1 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                    true);
            ChartMap scm1 = spy(cm1);
            doReturn(null).when(scm1).fetchChart();
            doReturn("foobar").when(scm1).getChartName();
            scm1.resolveChartDependencies();
            assertTrue(ChartMapTestUtil.streamContains(o, "Chart foobar was not found"));
            System.setOut(INITIAL_OUT);
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the ChartMap.getChart methods.
     * 
     * @throws ChartMapException
     */
    @Test
    void getChartTest() throws ChartMapException {
        // Test chart not found in getChart()
        ChartMap cm1 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        ChartMap scm1 = spy(cm1);
        doReturn(null).when(scm1).getChart(anyString());
        assertNull(scm1.fetchChart());

        // Test getChart(String c) IOException to ChartMapException
        ChartMap cm2 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_TEXT_PATH_NRNV, false, false,
                true);
        cm2.print();
        try (MockedStatic<Files> mf = Mockito.mockStatic(Files.class)) {
            mf.when(() -> Files.copy(any(Path.class), any(Path.class), eq(StandardCopyOption.REPLACE_EXISTING)))
                    .thenThrow(IOException.class);
            assertThrows(ChartMapException.class, () -> cm2.getChart(cm2.getChartName()));
            System.out.println("IOException -> ChartMapException thrown as expected");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the ChartMap.pullChart method using an APPR spec.
     * 
     * @throws ChartMapException
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void pullChartTest() throws ChartMapException, InterruptedException, IOException {
        // Use a spy to throw an IOException -> ChartMapException
        ChartMap cm1 = createTestMapV11(ChartOption.APPRSPEC, TEST_APPR_CHART, OUTPUT_APPR_PUML_PATH, true, false, false);
        ChartMap scm1 = spy(cm1);
        doThrow(IOException.class).when(scm1).getProcess(any(), any(File.class));
        assertThrows(ChartMapException.class, () -> scm1.print());
        System.out.println("IOException -> ChartMapException thrown as expected");
        // Use a spy to throw an InterruptedException -> ChartMapException
        // Be careful to put InterruptedException case last in the test case since the
        // thread is not usable after that
        ChartMap cm2 = createTestMapV11(ChartOption.APPRSPEC, TEST_APPR_CHART, OUTPUT_APPR_PUML_PATH, true, false, false);
        ChartMap scm2 = spy(cm2);
        Process p2 = Runtime.getRuntime()
                .exec(new String[] { "echo", "I am going to throw an InterruptedException!!" });
        Process sp2 = spy(p2);
        doReturn(sp2).when(scm2).getProcess(any(), any(File.class));
        doThrow(InterruptedException.class).when(sp2).waitFor(cm2.getTimeout(), TimeUnit.SECONDS);
        assertThrows(ChartMapException.class, () -> scm2.print());
        System.out.println("InterruptedException -> ChartMapException thrown as expected");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the ChartMap.downloadChart method.
     * 
     * @throws ChartMapException
     */
    @Test
    void downloadChartTest() throws ChartMapException, IOException {
        // Test IOException -> ChartMapException using a static Mock
        try (MockedStatic<ChartMap> mcm = Mockito.mockStatic(ChartMap.class)) {
            mcm.when(() -> ChartMap.getHttpResponse(any(CloseableHttpClient.class), anyString()))
                    .thenThrow(IOException.class);
            ChartMap cm1 = createTestMapV11(ChartOption.APPRSPEC, TEST_APPR_CHART, OUTPUT_APPR_PUML_PATH, true, false,
                    false);
            // force the creation of the temp dir so test artifacts don't accumulate in the project directory
            cm1.createTempDir();
            assertThrows(ChartMapException.class, () -> cm1.downloadChart("http://example.com"));
            System.out.println("IOException -> ChartMapException thrown as expected");
        }
        // Test a bad http rc using a url that's guraranteed not to exist. See
        // https://github.com/Readify/httpstatus.
        ChartMap cm2 = createTestMapV11(ChartOption.APPRSPEC, TEST_APPR_CHART, OUTPUT_APPR_PUML_PATH, true, false, false);
        // force the creation of the temp dir so test artifacts don't accumulate in the project directory
        cm2.createTempDir();
        assertThrows(ChartMapException.class, () -> cm2.downloadChart("https://httpstat.us/404"));
        System.out.println("ChartMapException thrown as expected");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the ChartMap.updateLocalRepp method.
     * 
     * @throws ChartMapException
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void updateLocalRepoTest() throws ChartMapException, InterruptedException, IOException {
        // Use a spy to throw an IOException -> ChartMapException
        // Make sure refresh is set to true (second boolean parm)
        ChartMap cm1 = createTestMapV11(ChartOption.APPRSPEC, TEST_APPR_CHART, OUTPUT_APPR_PUML_PATH, false, true, false);
        ChartMap scm1 = spy(cm1);
        doThrow(IOException.class).when(scm1).getProcess(any(), any(File.class));
        assertThrows(ChartMapException.class, () -> scm1.updateLocalRepo("foo"));
        System.out.println("IOException -> ChartMapException thrown as expected");
        // Cause a bad exit value from the process
        ChartMap cm2 = createTestMapV11(ChartOption.APPRSPEC, TEST_APPR_CHART, OUTPUT_APPR_PUML_PATH, false, true, false);
        ChartMap scm2 = spy(cm2);
        Process p2 = Runtime.getRuntime().exec(new String[] { "echo", "I am going to return a bad exitvalue!!!" });
        Process sp2 = spy(p2);
        doReturn(sp2).when(scm2).getProcess(any(), any(File.class));
        doReturn(1).when(sp2).exitValue();
        assertThrows(ChartMapException.class, () -> scm2.updateLocalRepo("foo"));
        System.out.println("ChartMapException thrown as expected");
        // Simulate an InterruptedException -> ChartMapException on waitFor()
        // Be careful to put InterruptedException case last in the test case since the
        // thread is not usable after that
        ChartMap cm3 = createTestMapV11(ChartOption.APPRSPEC, TEST_APPR_CHART, OUTPUT_APPR_PUML_PATH, false, true, false);
        ChartMap scm3 = spy(cm3);
        Process p3 = Runtime.getRuntime()
                .exec(new String[] { "echo", "I am going to throw an InterruptedException!!" });
        Process sp3 = spy(p3);
        doReturn(sp3).when(scm3).getProcess(any(), any(File.class));
        doThrow(InterruptedException.class).when(sp3).waitFor(cm3.getTimeout(), TimeUnit.SECONDS);
        assertThrows(ChartMapException.class, () -> scm3.updateLocalRepo("foo"));
        System.out.println("InterruptedException -> ChartMapException thrown as expected");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the ChartMap.createChart method.
     * 
     * @throws IOException
     * @throws ChartMapException
     */
    @Test
    void createChartTest() throws IOException, ChartMapException {
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            ChartMap cm1 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_TEXT_PATH_NRNV, false,
                    false, false);
            String s = "foo";
            System.setOut(new PrintStream(o));
            cm1.createChart(s);
            assertTrue(ChartMapTestUtil.streamContains(o, "IOException extracting Chart information from ".concat(s)
                    .concat(File.separator).concat(ChartMap.CHART_YAML)));
            System.setOut(new PrintStream(INITIAL_OUT));
            System.out.println("IOException thrown as expected");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the ChartMap.getConditionMap method.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void getConditionMapTest() throws ChartMapException, IOException {
        Path d = Paths.get(TARGET_TEST, "getConditionMapDirectory");
        Path f = Paths.get(d.toString(), "requirements.yaml");
        Files.deleteIfExists(f);
        Files.deleteIfExists(d);
        Files.createDirectory(d);
        Files.createFile(f);
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            ChartMap cm1 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                    false);
            System.setOut(new PrintStream(o));
            cm1.getConditionMap(d.toString());
            // no need for mockito here since the empty requirements file will induce an
            // IOExceotion
            assertTrue(ChartMapTestUtil.streamContains(o, "IOException parsing requirements file"));
            System.setOut(new PrintStream(INITIAL_OUT));
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the ChartMap.collectDependencies method.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void collectDependenciesTest() throws ChartMapException, IOException {
        // Test the currentDirectory.list returns null case
        ChartMap cm1 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_2, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        HelmChart h = new HelmChart();
        h.setName("foo");
        h.setVersion("1.1.1");
        cm1.print();
        int i1 = cm1.getChartsReferenced().size();
        Path f1 = Paths.get(TARGET_TEST, "collectDependenciesTest");
        Files.deleteIfExists(f1);
        Files.createFile(f1);
        cm1.collectDependencies(f1.toString(), h);
        assertEquals(i1, cm1.getChartsReferenced().size());
        System.out.println("Tested collectDependencies when currentDirectory.list returns null");
        // Test no Chart.yaml file found 
        ChartMap cm2 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_2, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        Path d2p = Paths.get(TARGET_TEST, "collectDependenciesTestParent");
        Path d2c = Paths.get(d2p.toString(), "collectDependenciesTestChild");
        Files.deleteIfExists(d2c);
        Files.deleteIfExists(d2p);
        Files.createDirectory(d2p);
        Files.createDirectory(d2c);
        cm2.print();
        i1 = cm2.getChartsReferenced().size();
        cm2.collectDependencies(d2p.toString(), null);
        assertEquals(i1, cm2.getChartsReferenced().size());
        System.out.println("Tested collectDependencies when Chart.yaml does not exist");
        // Test the Chart.yaml file references a chart that does not exist
        Path p3 = Paths.get(d2c.toString(), ChartMap.CHART_YAML);
        ChartMap cm3 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_2, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        cm3.print();
        String s = "apiVersion: v1\nentries:\n  foo-chart:\n  - name: ".concat("foo").concat("\n    version: ")
                .concat("1.1.1").concat("\n".concat("    urls:\n    - https://foo\n"));
        byte[] b = s.getBytes();
        Files.write(p3, b);
        assertThrows(ChartMapException.class, () -> cm3.collectDependencies(d2p.toString(), null));
        System.out.println("ChartMapException thrown as expected in collectDependencies");
        // Test the IOException is thrown
        ChartMap cm4 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_2, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        cm4.print();
        Files.delete(p3);
        Files.createFile(p3);
        assertThrows(ChartMapException.class, () -> cm4.collectDependencies(d2p.toString(), null));
        System.out.println("IOException thrown as expected in collectDependencies when empty Chart.yaml file");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the ChartMap.checkForCondition method.
     * 
     * @throws ChartMapException
     */
    @Test
    void checkForConditionTest() throws ChartMapException {
        ChartMap cm1 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_TEXT_PATH_NRNV, false, false,
                false);
        ChartMap scm1 = spy(cm1);
        doReturn(null).when(scm1).getCondition(anyString(), any(HelmChart.class));
        doReturn("foo").when(scm1).getConditionPropertyName(anyString(), any(HelmChart.class));
        assertEquals(new Boolean(true), scm1.checkForCondition("foo", new HelmChart(), new HelmChart()));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the ChartMap.handleHelmChartCondition method.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void handleHelmChartConditionTest() throws ChartMapException, IOException {
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            ChartMap cm1 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_TEXT_PATH_NRNV, false,
                    false, false);
            ChartMap scm1 = spy(cm1);
            doThrow(IOException.class).when(scm1).collectValues(anyString(), any(HelmChart.class));
            System.setOut(new PrintStream(o));
            assertThrows(ChartMapException.class, () -> scm1.handleHelmChartCondition(new Boolean(true), "foo", "foo",
                    new HelmChart(), new HelmChart()));
            assertTrue(ChartMapTestUtil.streamContains(o, "IOException collecting values in handleHelmChartCondition"));
            System.setOut(new PrintStream(INITIAL_OUT));
            System.out.println("IOException -> ChartMapException thrown as expected");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the ChartMap.getCondition method.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void getConditionTest() throws ChartMapException, IOException {
        // Test getCondition throwing an IOException, using a spy
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            ChartMap cm1 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_2, OUTPUT_TEXT_PATH_NRNV, false,
                    false, false);
            ChartMap scm1 = spy(cm1);
            doThrow(IOException.class).when(scm1).getEnvVars();
            String k = "foo-key";
            System.setOut(new PrintStream(o));
            scm1.getCondition(k, new HelmChart());
            System.setOut(new PrintStream(INITIAL_OUT));
            assertTrue(ChartMapTestUtil.streamContains(o, String.format("IO Exception getting condition of %s", k)));
            System.out.println("IOException handled correctly in getCondition");
        }
        // Test the condition where the variable is found with a false value in the env
        // list
        ChartMap cm2 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_TEXT_PATH_NRNV, false, false,
        false);
        assertEquals(Boolean.FALSE,
                cm2.getCondition("alfresco\\-infrastructure.alfresco\\-api\\-gateway.enabled", new HelmChart()));
        System.out.println("getCondition false tested");
        // Test getEnvVars throwing an IOException
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            ChartMap cm3 = new ChartMap(ChartOption.FILENAME, INPUT_FILE_NAME_1, "foo-out.txt", "no-env-var-file-here.yaml", new boolean[] {false, false, false} );
            System.setOut(new PrintStream(o));
            assertThrows(IOException.class, () -> cm3.getEnvVars());
            System.setOut(new PrintStream(INITIAL_OUT));
            assertTrue(ChartMapTestUtil.streamContains(o, String.format("IOException reading Environment Variables File")));
            System.out.println("IOException handled correctly in getEnvVars");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /** 
     * Tests the PLANTUML_LIMIT_SIZE when set as a system property. See the integration test function for the 
     * other cases which rely on the system environment being set.
     * 
     * @throws ChartMapException
     */
    @Test
    void plantUMLLimitSizeTest() throws ChartMapException, IOException, InterruptedException {
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            ChartMap cm1 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_2, OUTPUT_PUML_PATH_NRNV, true,
                    false, true);
            String e = "4000";
            System.setProperty("PLANTUML_LIMIT_SIZE", e);
            System.setOut(new PrintStream(o));
            cm1.print();
            System.setOut(new PrintStream(INITIAL_OUT));
            assertTrue(ChartMapTestUtil.streamContains(o, String.format("PLANTUML_LIMIT_SIZE was already set to %s", e)));
            System.out.println("PLANTUML_LIMIT_SIZE as a system property detected correctly"); 
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the creation and removal of the temp directory used by ChartMap.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void tempDirTest() throws ChartMapException, IOException {
        // force IOException to ChartMapExceptions using static mocking
        ChartMap cm1 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                true);
        try (MockedStatic<Files> mf = Mockito.mockStatic(Files.class)) {
            mf.when(() -> Files.createTempDirectory(any(), any())).thenThrow(IOException.class);
            assertThrows(ChartMapException.class, () -> cm1.createTempDir());
        }
        System.out.println("IOException -> ChartMapException thrown as expected attempting to create temp dir");
        ChartMap cm2 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                true);
        cm2.createTempDir();
        try (MockedStatic<Files> mf = Mockito.mockStatic(Files.class)) {
            mf.when(() -> Files.walk(any(), any())).thenThrow(IOException.class);
            assertThrows(ChartMapException.class, () -> cm2.removeTempDir());
        }
        System.out.println("ChartMapException thrown as expected attempting to remove temp dir");
        // create the ChartMap in non-debug mode and be sure the temp dir is deleted
        ChartMap cm4 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                true);
        cm4.print();
        assertFalse(Files.exists(Paths.get(cm4.getTempDirName())));
        System.out.println("IOException -> ChartMapException thrown as expected attempting to remove temp dir");
        // Force File.setReadable to return false to test security protection of
        // temporary file
        ChartMap cm5 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        ChartMap scm5 = spy(cm5);
        File sf5 = spy(new File(TARGET_TEST, "helmTempDirSpyRead"));
        doReturn(false).when(sf5).setReadable(true, true);
        doReturn(true).when(sf5).setWritable(true, true);
        doReturn(true).when(sf5).setExecutable(true, true);
        doReturn(sf5).when(scm5).getTempDir(anyString());
        assertThrows(ChartMapException.class, () -> scm5.createTempDir());
        System.out.println("ChartMapException thrown as expected after setReadable");
        // Force File.setWritable to return false to test security protection of
        // temporary file
        ChartMap cm6 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        ChartMap scm6 = spy(cm6);
        File sf6 = spy(new File(TARGET_TEST, "helmTempDirSpyWrite"));
        doReturn(true).when(sf6).setReadable(true, true);
        doReturn(false).when(sf6).setWritable(true, true);
        doReturn(true).when(sf6).setExecutable(true, true);
        doReturn(sf6).when(scm6).getTempDir(anyString());
        assertThrows(ChartMapException.class, () -> scm6.createTempDir());
        System.out.println("ChartMapException thrown as expected after setWritable");
        // Force File.setExecutable to return false to test security protection of
        // temporary file
        ChartMap cm7 = createTestMapV11(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, true, false,
                false);
        ChartMap scm7 = spy(cm7);
        File sf7 = spy(new File(TARGET_TEST, "helmTempDirSpyWrite"));
        doReturn(true).when(sf7).setReadable(true, true);
        doReturn(true).when(sf7).setWritable(true, true);
        doReturn(false).when(sf7).setExecutable(true, true);
        doReturn(sf7).when(scm7).getTempDir(anyString());
        assertThrows(ChartMapException.class, () -> scm7.createTempDir());
        System.out.println("ChartMapException thrown as expected after setExecutable");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests IllegalThreadStateExceptionTest is thrown in checkHelmVersion.
     * 
     * This must be done in a separate method because of the processing in handleIllegalStateThreadException.
     */
    @Test
    void illegalThreadStateExceptionTest1() throws ChartMapException, InterruptedException, IOException {
        ChartMap cm1 = createTestMapV12(ChartOption.CHARTNAME, TEST_CHART_NAME, OUTPUT_CHART_NAME_PUML_PATH, 3);
        ChartMap scm1 = spy(cm1);
        scm1.print();
        Process p1 = Runtime.getRuntime()
                .exec(new String[] { "java", "-jar", "target/".concat(new ChartMapTestUtil().getShadedJarName()),
                        "-f", INPUT_FILE_NAME_1, "-r", "-e", "./resource/example/example-env-spec.yaml", "-v", "-o",
                        "target/".concat("illegalThreadStateExceptionTest1.txt") });
        Process sp1 = spy(p1);
        doReturn(sp1).when(scm1).getProcess(any(), eq(null));
        doThrow(IllegalThreadStateException.class).when(sp1).exitValue();
        assertThrows(ChartMapException.class, () -> scm1.checkHelmVersion());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests IllegalThreadStateExceptionTest is thrown in getHelmClientInformation.
     *      
     * This must be done in a separate method because of the processing in handleIllegalStateThreadException.
     */
    @Test
    void illegalThreadStateExceptionTest2() throws ChartMapException, IOException{
        ChartMap cm2 = new ChartMap(ChartOption.CHARTNAME, TEST_CHART_NAME, "target/illegalThreadStateExceptionTest2.puml", null, TIMEOUT_DEFAULT);
        ChartMap scm2 = spy(cm2);
        scm2.print();
        ProcessBuilder pb2 = new ProcessBuilder("foo", "bar");
        ProcessBuilder spb2 = spy(pb2);
        Process p2 = Runtime.getRuntime().exec(new String[] { "echo", "illegalThreadStateExceptionTest2" });
        Process sp2 = spy(p2);
        doThrow(IllegalThreadStateException.class).when(sp2).exitValue();
        doReturn(sp2).when(spb2).start();
        doReturn(spb2).when(scm2).getProcessBuilder(any(), any());
        assertThrows(ChartMapException.class, () -> scm2.getHelmClientInformation());
        System.out.println("IllegalThreadStateException -> ChartMapException thrown as expected in getHelmClientInformation");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }
    
    /**
     * Tests IllegalThreadStateExceptionTest is thrown in pullChart.
     * 
     * This must be done in a separate method because of the processing in handleIllegalStateThreadException.
     */
    @Test
    void illegalThreadStateExceptionTest3() throws ChartMapException, IOException{
        ChartMap cm3 = new ChartMap(ChartOption.CHARTNAME, TEST_CHART_NAME, "target/illegalThreadStateExceptionTest3.puml", null, TIMEOUT_DEFAULT);
        ChartMap scm3 = spy(cm3);
        scm3.print();
        Process p3 = Runtime.getRuntime().exec(new String[] { "echo", "illegalThreadStateExceptionTest3" });
        Process sp3 = spy(p3);
        doReturn(sp3).when(scm3).getProcess(any(), any());
        doThrow(IllegalThreadStateException.class).when(sp3).exitValue();
        assertThrows(ChartMapException.class, () -> scm3.pullChart("foo"));
        System.out.println("IllegalThreadStateException -> ChartMapException thrown as expected in pullChart");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests IllegalThreadStateExceptionTest is thrown in runTemplateCommand.
     * 
     * This must be done in a separate method because of the processing in handleIllegalStateThreadException.
     */
    @Test
    void illegalThreadStateExceptionTest4() throws ChartMapException, IOException{
        ChartMap cm4 = new ChartMap(ChartOption.CHARTNAME, TEST_CHART_NAME, "target/illegalThreadStateExceptionTest4.puml", null, TIMEOUT_DEFAULT, false, true);
        Process p4 = Runtime.getRuntime().exec(new String[] { "echo", "illegalThreadStateExceptionTest4" });
        Process sp4 = spy(p4);
        ChartMap scm4 = spy(cm4);
        doReturn(sp4).when(scm4).getProcess(any(), any());
        doThrow(IllegalThreadStateException.class).when(sp4).exitValue();
        assertThrows(ChartMapException.class, () -> cm4.runTemplateCommand(new File("target/illegalThreadStateExceptionTest4"), sp4, new HelmChart()));
        System.out.println("IllegalThreadStateException -> ChartMapException thrown as expected in runTemplateCommand");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests IllegalThreadStateExceptionTest is thrown in updateLocalRepo.
     * 
     * This must be done in a separate method because of the processing in handleIllegalStateThreadException.
     */
    @Test
    void illegalThreadStateExceptionTest5() throws ChartMapException, IOException{
        ChartMap cm5 = new ChartMap(ChartOption.CHARTNAME, TEST_CHART_NAME, "target/illegalThreadStateExceptionTest5.puml", null, 1, true, true);
        ChartMap scm5 = spy(cm5);
        Process p5 = Runtime.getRuntime()
                .exec(new String[] { "java", "-jar", "target/".concat(new ChartMapTestUtil().getShadedJarName()),
                        "-f", INPUT_FILE_NAME_1, "-r", "-e", "./resource/example/example-env-spec.yaml", "-v", "-o",
                        "target/".concat("illegalThreadStateExceptionTest1.txt") });
        Process sp5 = spy(p5);
        doReturn(sp5).when(scm5).getProcess(any(), any());
        doThrow(IllegalThreadStateException.class).when(sp5).exitValue();
        assertThrows(ChartMapException.class, () -> scm5.updateLocalRepo("target/illegalThreadStateExceptionTest5"));
        System.out.println("IllegalThreadStateException -> ChartMapException thrown as expected in updateLocalRepo");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests handleIllegalStateThreadException.
     * 
     * @throws InterruptedException
     */
    @Test
    void handleIllegalStateThreadExceptionTest() throws ChartMapException, InterruptedException, IOException {
        // handle process that is null
        ChartMap cm1 = new ChartMap(ChartOption.CHARTNAME, TEST_CHART_NAME, "target/handleIllegalStateThreadExceptionTest1.puml", null, TIMEOUT_DEFAULT, false, true);
        assertDoesNotThrow(() ->
                cm1.handleIllegalStateThreadException(null, "foo-command"));
        // handle process that is not alive
        ChartMap cm2 = new ChartMap(ChartOption.CHARTNAME, TEST_CHART_NAME, "target/handleIllegalStateThreadExceptionTest2.puml", null, TIMEOUT_DEFAULT, false, true);
        Process p2 = Runtime.getRuntime().exec(new String[] { "echo", "foo"});
        p2.waitFor(10, TimeUnit.SECONDS); // plenty of time for echo to finish
        assertDoesNotThrow(
                () -> cm2.handleIllegalStateThreadException(p2, "foo-command"));
        // handle process that is alive
        ChartMap cm3 = new ChartMap(ChartOption.CHARTNAME, TEST_CHART_NAME, "target/handleIllegalStateThreadExceptionTest3.puml", null, TIMEOUT_DEFAULT, false, true);
        Process p3 = Runtime.getRuntime().exec(new String[] { "java", "-jar", "target/".concat(new ChartMapTestUtil().getShadedJarName()),
                "-f", INPUT_FILE_NAME_1, "-r", "-e", "./resource/example/example-env-spec.yaml", "-v" });
        p3.waitFor(10, TimeUnit.SECONDS); // not enough time for this process to finish but enough to be started
        assertDoesNotThrow(
                () -> cm3.handleIllegalStateThreadException(p3, "foo-command"));
        // handle process that is alive (the one second timeout case)
        ChartMap cm4 = new ChartMap(ChartOption.CHARTNAME, TEST_CHART_NAME, "target/handleIllegalStateThreadExceptionTest4.puml", null, 1, false, true);
        Process p4 = Runtime.getRuntime().exec(new String[] { "java", "-jar", "target/".concat(new ChartMapTestUtil().getShadedJarName()),
                "-f", INPUT_FILE_NAME_1, "-r", "-e", "./resource/example/example-env-spec.yaml", "-v" });
        p4.waitFor(10, TimeUnit.SECONDS); // not enough time for this process to finish but enough to be started
        assertDoesNotThrow(
                () -> cm4.handleIllegalStateThreadException(p3, "foo-command"));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests all combinations of the number of variable args for the switches.
     */
    @Test
    void varargsTest() {
        boolean[][] a = new boolean[][] { 
            // no varargs
            {},
            // one vararg
            { false }, { true }, 
            // two varargs
            { false, false }, { false, true }, { true, false },{ true, true },
            // three varargs
            { false, false, false }, { false, false, true }, { false, true, false }, { false, true, true },
            { true, false, false }, { true, false, true }, { true, true, false }, { true, true, true }, 
            // more than three varargs
            { false, false, false, false } 
        };
        int i = 0;
        ArrayList<ChartMap> c = new ArrayList<>();
        // first just test that all the variations are exercised
        try {
            for(;i < a.length && a[i].length < 1; i++) {
                c.add(new ChartMap(ChartOption.FILENAME, INPUT_FILE_NAME_1, "varargsTest0.puml",
                TEST_ENV_FILE_PATH.toAbsolutePath().toString(), TIMEOUT_DEFAULT));
                        System.out.println(String.format("varargs = %s", "null"));
            }
            for(;i < a.length && a[i].length < 2; i++) {
                c.add(new ChartMap(ChartOption.FILENAME, INPUT_FILE_NAME_1, "varargsTest1.puml",
                        TEST_ENV_FILE_PATH.toAbsolutePath().toString(), TIMEOUT_DEFAULT, a[i][0]));
                        System.out.println(String.format("varargs = (%s)", a[i][0]));
            }
            for(;i < a.length && a[i].length < 3; i++) {
                for (int j = 0; j < 1; j++) {
                    c.add(new ChartMap(ChartOption.FILENAME, INPUT_FILE_NAME_1, "varargsTest3.puml",
                            TEST_ENV_FILE_PATH.toAbsolutePath().toString(), TIMEOUT_DEFAULT, a[i][j], a[i][j+1]));
                    System.out.println(String.format("varargs = (%s,%s)", a[i][j], a[i][j+1]));
                }
            }
            for(;i < a.length && a[i].length < 4; i++) {
                for (int j = 0; j < 1; j++) {
                    for (int k = 0; k < 1; k++) {
                        c.add(new ChartMap(ChartOption.FILENAME, INPUT_FILE_NAME_1, "varargsTest4.puml",
                                TEST_ENV_FILE_PATH.toAbsolutePath().toString(), TIMEOUT_DEFAULT, a[i][j + k],
                                a[i][j + k + 1], a[i][j + k + 2]));
                        System.out.println(String.format("varargs = (%s,%s,%s)", a[i][j + k],
                                a[i][j + k + 1], a[i][j + k + 2]));
                    }
                }
            }
            for(;i < a.length && a[i].length< 5; i++) {
                c.add(new ChartMap(ChartOption.FILENAME, INPUT_FILE_NAME_1, "varargsTest5.puml",
                        TEST_ENV_FILE_PATH.toAbsolutePath().toString(), TIMEOUT_DEFAULT, a[i][0], a[i][1], a[i][2],
                        a[i][3]));
                        System.out.println(String.format("varargs = (%s,%s,%s,%s)", a[i][0], a[i][1], a[i][2],
                        a[i][3]));
            }
            // now verify that the flags were actuall set properly
            ChartMap[] ca = c.toArray(new ChartMap[0]);
            assertTrue(
                    !ca[0].isRefreshLocalRepo()   &&
                    !ca[1].isRefreshLocalRepo()   &&
                     ca[2].isRefreshLocalRepo()   &&
                    !ca[3].isRefreshLocalRepo()   &&
                    !ca[4].isRefreshLocalRepo()   &&
                     ca[5].isRefreshLocalRepo()   &&
                     ca[6].isRefreshLocalRepo()   &&
                    !ca[7].isRefreshLocalRepo()   &&
                    !ca[8].isRefreshLocalRepo()   &&
                    !ca[9].isRefreshLocalRepo()   &&
                    !ca[10].isRefreshLocalRepo()  &&
                     ca[11].isRefreshLocalRepo()  &&
                     ca[12].isRefreshLocalRepo()  &&
                     ca[13].isRefreshLocalRepo()  &&
                     ca[14].isRefreshLocalRepo()  &&
                    !ca[15].isRefreshLocalRepo());
            assertTrue(
                    !ca[0].isVerbose()   &&
                    !ca[1].isVerbose()   &&
                    !ca[2].isVerbose()   &&
                    !ca[3].isVerbose()   &&
                     ca[4].isVerbose()   &&
                    !ca[5].isVerbose()   &&
                     ca[6].isVerbose()   &&
                    !ca[7].isVerbose()   &&
                    !ca[8].isVerbose()   &&
                     ca[9].isVerbose()   &&
                     ca[10].isVerbose()  &&
                    !ca[11].isVerbose()  &&
                    !ca[12].isVerbose()  &&
                     ca[13].isVerbose()  &&
                     ca[14].isVerbose()  &&
                    !ca[15].isVerbose());
            assertTrue(
                    !ca[0].getGenerateImage()  &&
                    !ca[1].getGenerateImage()  &&
                    !ca[2].getGenerateImage()  &&
                    !ca[3].getGenerateImage()  &&
                    !ca[4].getGenerateImage()  &&
                    !ca[5].getGenerateImage()  &&
                    !ca[6].getGenerateImage()  &&
                    !ca[7].getGenerateImage()  &&
                     ca[8].getGenerateImage()  &&
                    !ca[9].getGenerateImage()  &&
                     ca[10].getGenerateImage() &&
                    !ca[11].getGenerateImage() &&
                     ca[12].getGenerateImage() &&
                    !ca[13].getGenerateImage() &&
                     ca[14].getGenerateImage() &&
                    !ca[15].getGenerateImage());;

        } catch (ChartMapException e) {
            // No exception is expected
            assertFalse(true);
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void pumlChartRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMapV12(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_PUML_PATH_RV, TIMEOUT_DOUBLE, true, true,
                true);
        testMap.print();
        assertTrue(Files.exists(OUTPUT_PUML_PATH_RV));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void pumlChartNoRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMapV12(ChartOption.FILENAME, INPUT_FILE_NAME_2, OUTPUT_PUML_PATH_NRV, TIMEOUT_DEFAULT, false,
                true, false);
        testMap.print();
        assertTrue(Files.exists(OUTPUT_PUML_PATH_NRV));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void pumlChartRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMapV12(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_PUML_PATH_RNV, TIMEOUT_DOUBLE, true, false,
                false);
        testMap.print();
        assertTrue(Files.exists(OUTPUT_PUML_PATH_RNV));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void pumlChartNoRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMapV12(ChartOption.FILENAME, INPUT_FILE_NAME_2, OUTPUT_PUML_PATH_NRNV, TIMEOUT_DEFAULT, false,
                false, true);
        testMap.print();
        assertTrue(Files.exists(OUTPUT_PUML_PATH_NRNV));
        assertTrue(Files.exists(OUTPUT_PNG_PATH_NRNV));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void textChartRefreshVerboseTest() throws ChartMapException, IOException {
        ChartMap testMap = createTestMapV12(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_TEXT_PATH_RV, TIMEOUT_DOUBLE, true, true);
        testMap.print();
        assertTrue(Files.exists(OUTPUT_TEXT_PATH_RV));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void textChartNoRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMapV12(ChartOption.FILENAME, INPUT_FILE_NAME_2, OUTPUT_TEXT_PATH_NRV, TIMEOUT_DEFAULT, false,
                true);
        testMap.print();
        assertTrue(Files.exists(OUTPUT_TEXT_PATH_NRV));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void textChartRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMapV12(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_TEXT_PATH_RNV, TIMEOUT_DOUBLE, true, false);
        testMap.print();
        assertTrue(Files.exists(OUTPUT_TEXT_PATH_RNV));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void textChartNoRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMapV12(ChartOption.FILENAME, INPUT_FILE_NAME_2, OUTPUT_TEXT_PATH_NRNV, TIMEOUT_DEFAULT);
        testMap.print();
        assertTrue(Files.exists(OUTPUT_TEXT_PATH_NRNV));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void JSONChartRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMapV12(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_JSON_PATH_RV, TIMEOUT_DOUBLE, true, true);
        testMap.print();
        assertTrue(Files.exists(OUTPUT_JSON_PATH_RV));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void JSONChartNoRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMapV12(ChartOption.FILENAME, INPUT_FILE_NAME_2, OUTPUT_JSON_PATH_NRV, TIMEOUT_DEFAULT, false,
                true);
        testMap.print();
        assertTrue(Files.exists(OUTPUT_JSON_PATH_NRV));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void JSONChartRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMapV12(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_JSON_PATH_RNV, TIMEOUT_DOUBLE, true, true,
                false);
        testMap.print();
        assertTrue(Files.exists(OUTPUT_JSON_PATH_RNV));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void JSONChartNoRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMapV12(ChartOption.FILENAME, INPUT_FILE_NAME_2, OUTPUT_JSON_PATH_NRNV, TIMEOUT_DEFAULT);
        testMap.print();
        assertTrue(Files.exists(OUTPUT_JSON_PATH_NRNV));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void APPRTest() throws ChartMapException, IOException { // test normal path
        ChartMap cm1 = createTestMapV12(ChartOption.APPRSPEC, TEST_APPR_CHART, OUTPUT_APPR_PUML_PATH, TIMEOUT_DEFAULT,
                false, false, true);
        cm1.print();
        assertTrue(Files.exists(OUTPUT_APPR_PUML_PATH));
        assertEquals("quay.io/melahn", cm1.getApprRepoHostName());
        System.out.println("Normal APPR test variation succeeded");
        // Test null appr spec
        assertTrue(Files.exists(OUTPUT_APPR_PNG_PATH));
        assertThrows(ChartMapException.class,
                () -> createTestMapV12(ChartOption.APPRSPEC, null, OUTPUT_APPR_PUML_PATH, TIMEOUT_DEFAULT, false, false,
                        true));
        System.out.println("ChartMapException thrown as expected with a null appr spec");
        // Test various malformed appr specs
        assertThrows(ChartMapException.class, () -> createTestMapV11(ChartOption.APPRSPEC, "badapprspec/noat",
                OUTPUT_APPR_PUML_PATH, true, false, false));
        System.out.println("ChartMapException thrown as expected with a bad appr spec");
        assertThrows(ChartMapException.class, () -> createTestMapV11(ChartOption.APPRSPEC, "badapprspec@noslash",
                OUTPUT_APPR_PUML_PATH, true, false, false)); // test chart not found in app repo
        System.out.println("ChartMapException thrown as expected with a bad appr spec");
        ChartMap cm2 = createTestMapV12(ChartOption.APPRSPEC, "quay.io/melahn/no-such-chart@1.0.0",
                OUTPUT_APPR_PUML_PATH, 0,
                false, false, true);
        assertThrows(ChartMapException.class, () -> cm2.print());
        System.out.println("ChartMapException thrown as expected");
        // Test the case where the repoUrl is guaranteed to be null
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm3 = createTestMapV12(ChartOption.APPRSPEC, TEST_APPR_CHART, OUTPUT_APPR_PUML_PATH,
                    TIMEOUT_DEFAULT, false, true, true);
            cm3.setApprRepoHostName(null);
            cm3.print();
            assertTrue(
                    ChartMapTestUtil.streamContains(o, "repoUrl set to null"));
            System.setOut(INITIAL_OUT);
            System.out.println("repoUrl was null as expected");
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void UrlTest() throws ChartMapException { // test normal path
        ChartMap cm = createTestMapV11(ChartOption.URL, TEST_CHART_URL, OUTPUT_CHART_URL_PUML_PATH, true, true, false);
        cm.print();
        assertTrue(Files.exists(OUTPUT_CHART_URL_PUML_PATH));
        assertTrue(Files.exists(OUTPUT_CHART_URL_PNG_PATH)); // test null chart name
        assertThrows(ChartMapException.class,
                () -> createTestMapV11(ChartOption.URL, null, OUTPUT_CHART_URL_PUML_PATH, true, false, false));
        System.out.println("ChartMapException thrown as expected with a null chart name");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void chartNameTest() throws ChartMapException {
        // test normal path without using createTestMap utility function because I want
        // null env var file
        boolean[] switches = { true, false, false };
        ChartMap cm1 = new ChartMap(ChartOption.CHARTNAME, TEST_CHART_NAME,
                OUTPUT_CHART_NAME_PUML_PATH.toAbsolutePath().toString(), null, switches);
        cm1.print();
        assertTrue(Files.exists(OUTPUT_CHART_NAME_PUML_PATH));
        assertTrue(Files.exists(OUTPUT_CHART_NAME_PNG_PATH));
        // test missing version in chartname
        assertThrows(ChartMapException.class, () -> createTestMapV11(ChartOption.CHARTNAME, "badchartname-noversion",
                OUTPUT_CHART_NAME_PUML_PATH, true, false, false));
        System.out.println("ChartMapException thrown as expected test missing version in chartname");
        // test chart not found
        ChartMap cm2 = createTestMapV11(ChartOption.CHARTNAME, "no-such-chart:9.9.9", OUTPUT_CHART_NAME_PUML_PATH, true,
                false, false);
        assertThrows(ChartMapException.class, () -> cm2.print());
        System.out.println("ChartMapException thrown as expected when test chart not found");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void optionsTest() throws ChartMapException {
        final int BAD_NUMBER_OF_SWITCHES = 8;
        boolean[] switches = { true, false, false }; // test that a correct option is used
        assertThrows(ChartMapException.class, () -> new ChartMap(null, TEST_CHART_NAME,
                OUTPUT_CHART_NAME_PUML_PATH.toAbsolutePath().toString(), null, switches)); //
        System.out.println("ChartMapException thrown as expected with a bad switches array");
        // test a bad switches array
        assertThrows(ChartMapException.class, () -> new ChartMap(ChartOption.CHARTNAME, TEST_CHART_NAME,
                OUTPUT_CHART_NAME_PUML_PATH.toAbsolutePath().toString(), null, new boolean[BAD_NUMBER_OF_SWITCHES]));
        System.out.println("ChartMapException thrown as expected with a bad number of switches in the array");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Proves the help text is what is expected.
     */
    @Test
    void helpTest() {
        String helpTextExpected = "\nUsage:\n\n".concat("java -jar helm-chartmap-1.1.0.jar\n").concat("\nFlags:\n")
                .concat("\t-a\t<apprspec>\tA name and version of a chart as an appr specification\n")
                .concat("\t-c\t<chartname>\tA name and version of a chart\n")
                .concat("\t-f\t<filename>\tA location in the file system for a Helm Chart package (a tgz file)\n")
                .concat("\t-u\t<url>\t\tA url for a Helm Chart\n")
                .concat("\t-o\t<filename>\tThe location of the output file\n")
                .concat("\t-e\t<filename>\tThe location of an Environment Specification file\n")
                .concat("\t-t\t<seconds>\tThe amount of time to wait for a helm command to complete\n")
                .concat("\t-g\t\t\tGenerate image from PlantUML file\n").concat("\t-r\t\t\tRefresh\n")
                .concat("\t-v\t\t\tVerbose\n").concat("\t-h\t\t\tHelp\n")
                .concat("\nSee https://github.com/melahn/helm-chartmap for more information\n");
        String helpText = ChartMap.getHelp();
        assertEquals (helpText, helpTextExpected);
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void chartUtilTest() {
        // test getValue
        HashMap<String, Object> hm = new HashMap<String, Object>();
        hm.put("fookey1", "foovalue1");
        hm.put("fookey2", new HashMap<String, Object>() {
            {
                put("fookey3", "foovalue3");
                put("fookey4", "foovalue4");
                put("fookey5", " ");
                put("fookey6", new HashMap<String, Object>() {
                    {
                        put("fookey7", "foovalue7");
                        put("fookey8", "foovalue8");
                    }
                });
            }
        });
        assertEquals("foovalue1", ChartUtil.getValue("fookey1", hm));
        assertEquals("foovalue3", ChartUtil.getValue("fookey2.fookey3", hm));
        assertEquals(" ", ChartUtil.getValue("fookey2.fookey5", hm));
        assertNull(ChartUtil.getValue("fookey", null));
        assertNull(ChartUtil.getValue("", hm));
        assertNull(ChartUtil.getValue(".", hm));
        assertNull(ChartUtil.getValue(null, hm));
        assertNull(ChartUtil.getValue("fookey2.fookey6", hm));
        // test os.name detection
        String saveOS = System.getProperty("os.name");
        System.setProperty("os.name", "Windows");
        assertEquals(ChartUtil.OSType.WINDOWS, ChartUtil.getOSType());
        System.setProperty("os.name", "Linux");
        assertEquals(ChartUtil.OSType.LINUX, ChartUtil.getOSType());
        System.setProperty("os.name", "MacOS");
        assertEquals(ChartUtil.OSType.MACOS, ChartUtil.getOSType());
        System.setProperty("os.name", "MVS");
        assertEquals(ChartUtil.OSType.OTHER, ChartUtil.getOSType());
        System.setProperty("os.name", saveOS);
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void ChartMapExceptionTest() {
        long a = ChartMapException.serialVersionUID;
        assertEquals(UUID.fromString("5a8dba66-71e1-492c-bf3b-53cceb67b785").getLeastSignificantBits(), a);
        ChartMapException cme = new ChartMapException("test");
        assertEquals("test", cme.getMessage());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void ChartMapPrinterTest() throws Exception {
        Path d = Paths.get("./target/test/printer");
        Files.createDirectories(d);
        Path f = Paths.get(d.toString(), "test.txt");
        Files.deleteIfExists(f);
        Files.createFile(f);
        ChartMapPrinter cmp = null;
        try {
            cmp = new ChartMapPrinter(createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, f, true, true, true), "/",
                    null, null);
        } catch (ChartMapException e) {
            System.out.println("First ChartMapException expected and thrown");
            assertFalse(false);
        }
        cmp = new ChartMapPrinter(createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, f, true, true, true),
                f.toString(), null, null);
        assertEquals(ChartMapPrinter.NOT_SPECIFIED, cmp.formatString(" "));
        assertEquals(ChartMapPrinter.NOT_SPECIFIED, cmp.formatString(null));
        HelmChart h = new HelmChart();
        h.setRepoUrl(null);
        cmp.printChart(h);
        assertFalse(ChartMapTestUtil.fileContains(f, "repo url"));
        final String OUTPUTFILENAME = "fooout";
        cmp.setOutputFilename(OUTPUTFILENAME);
        assertEquals(OUTPUTFILENAME, cmp.getOutputFilename());
        final int INDENT = 666;
        cmp.setIndent(INDENT);
        assertEquals(INDENT, cmp.getIndent());
        final HelmChart CHART = new HelmChart();
        cmp.setChart(CHART);
        assertEquals(CHART, cmp.getChart());
        final String CHARTTYPELIBRARY = "library";
        h.setType(CHARTTYPELIBRARY);
        assertEquals(CHARTTYPELIBRARY, cmp.getChartType(h));
        final String CHARTTYPEAPPLICATION = "application";
        h.setType(CHARTTYPEAPPLICATION);
        assertEquals(CHARTTYPEAPPLICATION, cmp.getChartType(h));
        h.setType(null);
        assertEquals(CHARTTYPEAPPLICATION, cmp.getChartType(h));
        cmp.printTree(h);
        try {
            cmp.writer.close();
            cmp.writeLine("foo");
        } catch (ChartMapException e) {
            System.out.println("Second ChartMapException expected and thrown");
            assertFalse(false);
        }
        // test case where the ChartMap logger is null and the ChartMapPrinter needs to
        // create its own
        ChartMap cm = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_PUML_PATH_NRNV, true, false, 
                false);
        cm.logger = null;
        cmp = new ChartMapPrinter(cm, f.toString(), null, null);
        String nl = "null logger";
        cmp.writeLine(nl);
        assertTrue(ChartMapTestUtil.fileContains(f, nl));
        // force IOException to ChartMapException with a ChartMap with a null logger
        assertThrows(ChartMapException.class, () -> new ChartMapPrinter(cm, "/", null, null));
        System.out.println("IO Exception -> ChartMapException thrown as expected because the logger is null");
        // force IOException using mocking
        FileWriter mfr = mock(FileWriter.class);
        doThrow(new IOException("IO Exception occured")).when(mfr).write(anyString());
        ChartMapPrinter cmp2 = new ChartMapPrinter(cm, f.toString(), null, null);
        cmp2.writer = mfr;
        assertThrows(ChartMapException.class, () -> {
            cm.logger = null;
            cmp2.writeLine("IO Exception");
        });
        System.out.println("IO Exception -> ChartMapException thrown as expected");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void PlantUMLChartMapPrinterTest() throws Exception {
        // Test bad values for repo and images when print puml files
        ChartMap cm1 = createTestMapV11(ChartOption.FILENAME, INPUT_FILE_NAME_1, OUTPUT_PUML_PATH_NRNV, true,
                true, false);
        cm1.createTempDir();
        cm1.loadLocalRepos();
        cm1.resolveChartDependencies();
        ChartKeyMap ckm = cm1.chartsReferenced;
        HelmChart h = ckm.get("alfresco-content-services", "5.2.0");
        h.setRepoUrl(null);
        ckm.put("alfresco-content-services", "5.2.0", h);
        HashSet<String> ir1 = cm1.imagesReferenced;
        HashSet<String> ir2 = new HashSet<String>();
        for (String i : ir1) {
            if (i.equals("alfresco/alfresco-imagemagick:2.5.7")) {
                i = i.replace(':', 'X');
            }
            ir2.add(i);
        }
        cm1.imagesReferenced = ir2;
        Set<HelmDeploymentTemplate> templates = h.getDeploymentTemplates();
        for (HelmDeploymentTemplate t : templates) {
            HelmDeploymentContainer[] hdc = t.getSpec().getTemplate().getSpec().getContainers();
            for (HelmDeploymentContainer c : hdc) {
                if (c.getImage().equals("alfresco/alfresco-imagemagick:2.5.7")) {
                    c.setImage(c.getImage().replace(':', 'X'));
                }
            }
        }
        h.setDeploymentTemplates(templates);
        cm1.printMap();
        assertTrue(ChartMapTestUtil.fileContains(OUTPUT_PUML_PATH_NRNV, PlantUmlChartMapPrinter.NO_REPO_URL_MESSAGE));
        assertTrue(ChartMapTestUtil.fileContains(OUTPUT_PUML_PATH_NRNV, "alfresco_alfresco_imagemagickX2_5_7"));
        System.out.println("Tested bad values for repo and images tested for PlantUML format");
        // Test odd combinations of maintainer and keywords
        String m = PlantUmlChartMapPrinter.getMaintainers(new HelmMaintainer[0]);
        assertEquals("Maintainers: ", m);
        String k = PlantUmlChartMapPrinter.getKeywords(new String[0]);
        assertEquals("Keywords: ", k);
        k = PlantUmlChartMapPrinter.getKeywords(new String[]{"one"});
        assertEquals("Keywords: one", k);
        System.out.println("Tested odd values for maintainers and keywords for PlantUML format");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void JSONChartMapPrinterTest() throws ChartMapException, FileNotFoundException, IOException {
        final Path jsonDir = Paths.get("target/test/json/");
        final Path jsonFile = Paths.get(jsonDir.toString(), "test.json");
        if (!Files.exists(jsonDir)) {
            Files.createDirectories(jsonDir);
        }
        if (Files.exists(jsonFile)) {
            Files.delete(jsonFile);
        }
        JSONChartMapPrinter jcmp = new JSONChartMapPrinter(null, "target/test/json/test.json", null, null);
        jcmp.printHeader();
        jcmp.printFooter();
        jcmp.printSectionHeader("foo");
        HelmChart h = new HelmChart();
        Set<HelmDeploymentTemplate> templates = new HashSet<HelmDeploymentTemplate>();
        HelmDeploymentContainer c = new HelmDeploymentContainer();
        c.setImage("image");
        c.setParent(null);
        HelmDeploymentContainer[] hdc = new HelmDeploymentContainer[1];
        hdc[0] = c;
        HelmDeploymentTemplate hdt = new HelmDeploymentTemplate();
        HelmDeploymentSpecTemplateSpec hdsts = new HelmDeploymentSpecTemplateSpec();
        hdsts.setContainers(hdc);
        HelmDeploymentSpecTemplate hdst = new HelmDeploymentSpecTemplate();
        hdst.setSpec(hdsts);
        HelmDeploymentSpec hds = new HelmDeploymentSpec();
        hds.setTemplate(hdst);
        hdt.setSpec(hds);
        templates.add(hdt);
        h.setDeploymentTemplates(templates);
        jcmp.addContainers(h, null, new JSONArray());
        JSONObject jo = new JSONObject("{foo: bar}\n");
        jcmp.addImageDetails("foo", jo);
        // Force an exception to complete the test coverage
        Files.deleteIfExists(jsonFile);
        Files.deleteIfExists(Paths.get("target/test/json"));
        try {
            jcmp.printObject(jo);
        } catch (ChartMapException e) {
            System.out.println("ChartMapException expected and thrown");
            assertFalse(Files.exists(jsonFile));
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /*
     * Creates a ChartMap using the API for V1.1.x and earlier.
     */
    private ChartMap createTestMapV11(ChartOption option, String input, Path outputPath, boolean generateImage, 
            boolean refresh, boolean verbose) throws ChartMapException {
        boolean[] switches = new boolean[] { generateImage, refresh, verbose };
        ChartMap cm = new ChartMap(option, input, outputPath.toAbsolutePath().toString(),
                TEST_ENV_FILE_PATH.toAbsolutePath().toString(), switches);
        cm.setHelmEnvironment(); // set this explictly so that test cases can test helm dependent methods without
                                 // necessarily calling print
        return cm;
    }

    /*
     * Creates a ChartMap using the API for V1.2.x.
     */
    private ChartMap createTestMapV12(ChartOption option, String input, Path outputPath, int timeout,
            boolean... switches) throws ChartMapException {
        ChartMap cm = new ChartMap(option, input, outputPath.toAbsolutePath().toString(),
                TEST_ENV_FILE_PATH.toAbsolutePath().toString(), timeout, switches);
        cm.setHelmEnvironment(); // set this explictly so that test cases can test helm dependent methods without
                                 // necessarily calling print
        return cm;
    }
}
