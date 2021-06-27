package com.melahn.util.helm;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.melahn.util.helm.model.HelmChart;
import com.melahn.util.helm.model.HelmDeploymentContainer;
import com.melahn.util.helm.model.HelmDeploymentSpec;
import com.melahn.util.helm.model.HelmDeploymentSpecTemplate;
import com.melahn.util.helm.model.HelmDeploymentSpecTemplateSpec;
import com.melahn.util.helm.model.HelmDeploymentTemplate;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ChartMapTest {
    private static String VERSIONSUFFIX = "-1.0.3-SNAPSHOT"; // needed for main test; it would be nice not to get this
                                                             // from the pom instead
    private static String TARGETTESTDIRECTORY = Paths.get("target/test").toString();
    private static Path testOutputPumlFilePathRV = Paths.get(TARGETTESTDIRECTORY, "testChartFileRV.puml");
    private static Path testOutputPumlFilePathNRV = Paths.get(TARGETTESTDIRECTORY, "testChartFileNRV.puml");
    private static Path testOutputPumlFilePathRNV = Paths.get(TARGETTESTDIRECTORY, "testChartFileRNV.puml");
    private static Path testOutputPumlFilePathNRNV = Paths.get(TARGETTESTDIRECTORY, "testChartFileNRNV.puml");
    private static Path testOutputPngFilePathNRNV = Paths.get(TARGETTESTDIRECTORY, "testChartFileNRNV.png");
    private static Path testOutputTextFilePathRV = Paths.get(TARGETTESTDIRECTORY, "testChartFileRV.txt");
    private static Path testOutputTextFilePathNRV = Paths.get(TARGETTESTDIRECTORY, "testChartFileNRV.txt");
    private static Path testOutputTextFilePathRNV = Paths.get(TARGETTESTDIRECTORY, "testChartFileRNV.txt");
    private static Path testOutputTextFilePathNRNV = Paths.get(TARGETTESTDIRECTORY, "testChartFileNRNV.txt");
    private static Path testOutputJSONFilePathRV = Paths.get(TARGETTESTDIRECTORY, "testChartFileRV.json");
    private static Path testOutputJSONFilePathNRV = Paths.get(TARGETTESTDIRECTORY, "testChartFileNRV.json");
    private static Path testOutputJSONFilePathRNV = Paths.get(TARGETTESTDIRECTORY, "testChartFileRNV.json");
    private static Path testOutputJSONFilePathNRNV = Paths.get(TARGETTESTDIRECTORY, "testChartFileNRNV.json");
    private static Path testInputFilePath = Paths.get("src/test/resource/test-chart-file.tgz");
    private static Path testOneFileZipPath = Paths.get("src/test/resource/test-onefile.tgz");
    private static Path testEnvFilePath = Paths.get("resource/example/example-env-spec.yaml");
    private final PrintStream initialOut = System.out;

    @AfterAll
    public static void cleanUp() {
        /**
         * No cleanup to do after test. I don't delete the generated files because they
         * might be handy to have around to diagnose issues in test failures. They are
         * deleted anyway when the test is next run.
         */
        System.out.println("Test complete.  Any generated file can be found in "
                .concat(Paths.get(TARGETTESTDIRECTORY).toAbsolutePath().toString()));
    }

    @BeforeAll
    public static void setUp() {
        try {
            if (!Files.exists(testInputFilePath)) {
                throw new Exception("test Input File " + testInputFilePath.toAbsolutePath() + " does not exist");
            }
            deletePreviouslyCreatedFiles();
            Files.createDirectories(testOutputPumlFilePathRV.getParent());
            assertNotNull(System.getenv("HELM_HOME"));
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void chartMapMainTest() throws IOException, InterruptedException {
        final String OUTPUTFILE = "testChartFileRV.txt";
        if (Files.notExists(Paths.get("./target/test"))) {
            Files.createDirectories(Paths.get("./target/test"));
        }
        String c[] = new String[12];
        c[0] = "java";
        c[1] = "-cp";
        // I use the previulsy generated shaded jar just to resolve the third party
        // dependencies. This could miss a bug
        // introduced by a third party dependency upgrade though it would be caught on
        // the next test because the shaded
        // jar would have this updated dependency. Perhaps there is a better way
        c[2] = ".:../../resource/jar/helm-chartmap".concat(VERSIONSUFFIX).concat(".jar");
        c[3] = "com.melahn.util.helm.ChartMap";
        c[4] = "-f";
        c[5] = "../../".concat(testInputFilePath.toString());
        c[6] = "-d";
        c[7] = System.getenv("HELM_HOME");
        c[8] = "-e";
        c[9] = "../../".concat(testEnvFilePath.toString());
        c[10] = "-o";
        c[11] = OUTPUTFILE;
        Process p = Runtime.getRuntime().exec(c, null, new File(TARGETTESTDIRECTORY.toString()));
        p.waitFor(30000, TimeUnit.MILLISECONDS);
        assertEquals(0, p.exitValue());
        assertTrue(Files.exists(Paths.get(TARGETTESTDIRECTORY, OUTPUTFILE)));
        Files.deleteIfExists(Paths.get(TARGETTESTDIRECTORY, OUTPUTFILE));
        String[] a = new String[8];
        a[0] = "-f";
        a[1] = testInputFilePath.toString();
        a[2] = "-d";
        a[3] = System.getenv("HELM_HOME");
        a[4] = "-e";
        a[5] = testEnvFilePath.toString();
        a[6] = "-o";
        a[7] = Paths.get(TARGETTESTDIRECTORY, OUTPUTFILE).toString();
        ChartMap.main(a);
        assertTrue(Files.exists(Paths.get(TARGETTESTDIRECTORY, OUTPUTFILE)));
    }

    @Test
    public void WeightedDeploymentTemplateTest() throws ChartMapException {
        ChartMap cm = new ChartMap(ChartOption.FILENAME, testInputFilePath.toString(),
                testOutputTextFilePathNRNV.toString(), System.getenv("HELM_HOME"),
                testEnvFilePath.toAbsolutePath().toString(), new boolean[] { false, false, false, false });
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

    @Test
    public void urlOptionTest() throws ChartMapException {
        String url = "https://kubernetes-charts.alfresco.com/stable/alfresco-identity-service-3.0.0.tgz";
        ChartMap cm = new ChartMap(ChartOption.URL, url, testOutputTextFilePathNRNV.toString(),
                System.getenv("HELM_HOME"), testEnvFilePath.toAbsolutePath().toString(),
                new boolean[] { false, false, false, false });
        cm.print();
        assertTrue(Files.exists(testOutputTextFilePathNRNV));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the protected method ChartMap.unpackTestChart
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void unpackChartTest() throws ChartMapException, IOException {
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" starting"));
        ByteArrayOutputStream unpackCharttestOut = new ByteArrayOutputStream();
        System.setOut(new PrintStream(unpackCharttestOut));
        // force IOException -> ChartMapException path
        ChartMap cm1 = new ChartMap(ChartOption.FILENAME, testInputFilePath.toString(),
                testOutputTextFilePathNRNV.toString(), System.getenv("HELM_HOME"),
                testEnvFilePath.toAbsolutePath().toString(), new boolean[] { false, false, false, true });
        cm1.setChartName("foo");
        cm1.createTempDir();
        assertThrows(ChartMapException.class, () -> cm1.unpackChart("foo"));
        // force ChartMapException path when no temp dir
        ChartMap cm2 = new ChartMap(ChartOption.FILENAME, testInputFilePath.toString(),
                testOutputTextFilePathNRNV.toString(), System.getenv("HELM_HOME"),
                testEnvFilePath.toAbsolutePath().toString(), new boolean[] { false, false, false, true });
        cm2.setChartName("foo");
        assertThrows(ChartMapException.class, () -> cm2.unpackChart("foo"));
        // force ChartMapException path when null chartmap passed
        cm2.createTempDir();
        assertThrows(ChartMapException.class, () -> cm2.unpackChart(null));
        // test when the tgz has no directory
        ChartMap cm3 = new ChartMap(ChartOption.FILENAME, testInputFilePath.toString(),
                testOutputTextFilePathNRNV.toString(), System.getenv("HELM_HOME"),
                testEnvFilePath.toAbsolutePath().toString(), new boolean[] { false, false, false, true });
        cm3.setChartName(null);
        cm3.setChartVersion(null);
        cm3.createTempDir();
        assertThrows(ChartMapException.class, () -> cm3.unpackChart(testOneFileZipPath.toString()));
        assertTrue(logContains(unpackCharttestOut, "Archive content does not appear to be valid"));
        System.setOut(initialOut);
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests some utility methods in ChartMap
     * 
     * @throws IOException
     * @throws ChartMapException
     */

    @Test
    void utilityMethodsTest() throws IOException, ChartMapException {
        Path d = Paths.get("./target");
        String b = ChartMap.getBaseName(d.toString());
        assertEquals(null, b);
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void pumlChartRefreshVerboseTest() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputPumlFilePathRV, true,
                    true, true);
            if (testMap != null) {
                testMap.print();
            }
            assertTrue(Files.exists(testOutputPumlFilePathRV));
        } catch (Exception e) {
            fail("printTestPumlChartRefreshVerbose failed:" + e.getMessage());
        }
    }

    @Test
    void pumlChartNoRefreshVerboseTest() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputPumlFilePathNRV, true,
                    false, true);
            if (testMap != null) {
                testMap.print();
            }
            assertTrue(Files.exists(testOutputPumlFilePathNRV));
        } catch (Exception e) {
            fail("printTestPumlChartNoRefreshVerbose failed:" + e.getMessage());
        }
    }

    @Test
    void pumlChartRefreshNoVerboseTest() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputPumlFilePathRNV, true,
                    true, false);
            if (testMap != null) {
                testMap.print();
            }
            assertTrue(Files.exists(testOutputPumlFilePathRNV));
        } catch (Exception e) {
            fail("printTestPumlChartRefreshNoVerbose failed:" + e.getMessage());
        }
    }

    @Test
    void pumlChartNoRefreshNoVerboseTest() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputPumlFilePathNRNV, true,
                    false, false);
            if (testMap != null) {
                testMap.print();
            }
            assertTrue(Files.exists(testOutputPumlFilePathNRNV));
            assertTrue(Files.exists(testOutputPngFilePathNRNV));
        } catch (Exception e) {
            fail("printTestPumlChartNoRefreshNoVerbose failed:" + e.getMessage());
        }
    }

    @Test
    void textChartRefreshVerboseTest() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputTextFilePathRV, true,
                    true, true);
            if (testMap != null) {
                testMap.print();
            }
            assertTrue(Files.exists(testOutputTextFilePathRV));
            assertTrue(fileContains(testOutputTextFilePathRV,
                    "WARNING: Chart alfresco-content-services:1.0.3 is stable but depends on alfresco-search:0.0.4 which may not be stable"));
        } catch (Exception e) {
            fail("printTestTextChartRefreshVerbose failed:" + e.getMessage());
        }
    }

    @Test
    void textChartNoRefreshVerboseTest() throws Exception {
        // try {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputTextFilePathNRV, true,
                false, true);
        if (testMap != null) {
            testMap.print();
            // }
            assertTrue(Files.exists(testOutputTextFilePathNRV));
            // } catch (Exception e) {
            // fail("printTestTextChartNoRefreshVerbose failed:" + e.getMessage());
        }
    }

    @Test
    void textChartRefreshNoVerboseTest() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputTextFilePathRNV, true,
                    true, false);
            if (testMap != null) {
                testMap.print();
            }
            assertTrue(Files.exists(testOutputTextFilePathRNV));
        } catch (Exception e) {
            fail("printTestTextChartRefreshNoVerbose failed:" + e.getMessage());
        }
    }

    @Test
    void textChartNoRefreshNoVerboseTest() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputTextFilePathNRNV, true,
                    false, false);
            if (testMap != null) {
                testMap.print();
            }
            assertTrue(Files.exists(testOutputTextFilePathNRNV));
            // todo compare NR generated files with time stamp removed with a known good
            // result for a better test
        } catch (Exception e) {
            fail("printTestTextChartNRefreshNoVerbose failed:" + e.getMessage());
        }
    }

    @Test
    void JSONChartRefreshVerboseTest() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputJSONFilePathRV, true,
                    true, true);
            if (testMap != null) {
                testMap.print();
            }
            assertTrue(Files.exists(testOutputJSONFilePathRV));
        } catch (Exception e) {
            fail("printTestJSONChartRefreshVerbose failed:" + e.getMessage());
        }
    }

    @Test
    void JSONChartNoRefreshVerboseTest() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputJSONFilePathNRV, true,
                    false, true);
            if (testMap != null) {
                testMap.print();
            }
            assertTrue(Files.exists(testOutputJSONFilePathNRV));
        } catch (Exception e) {
            fail("printTestJSONChartNoRefreshVerbose failed:" + e.getMessage());
        }
    }

    @Test
    void JSONChartRefreshNoVerboseTest() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputJSONFilePathRNV, true,
                    true, false);
            if (testMap != null) {
                testMap.print();
            }
            assertTrue(Files.exists(testOutputJSONFilePathRNV));
        } catch (Exception e) {
            fail("printTestJSONChartRefreshNoVerbose failed:" + e.getMessage());
        }
    }

    @Test
    void JSONChartNoRefreshNoVerboseTest() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputJSONFilePathNRNV, true,
                    false, false);
            if (testMap != null) {
                testMap.print();
            }
            assertTrue(Files.exists(testOutputJSONFilePathNRNV));
        } catch (Exception e) {
            fail("printTestJSONChartNRefreshNoVerbose failed:" + e.getMessage());
        }
    }

    @Test
    void helpTest() {
        String helpTextExpected = "\nUsage:\n\n".concat("java -jar helm-chartmap-1.0.2.jar\n").concat("\nFlags:\n")
                .concat("\t-a\t<apprspec>\tA name and version of a chart as an appr specification\n")
                .concat("\t-c\t<chartname>\tA name and version of a chart\n")
                .concat("\t-f\t<filename>\tA location in the file system for a Helm Chart package (a tgz file)\n")
                .concat("\t-u\t<url>\t\tA url for a Helm Chart\n")
                .concat("\t-d\t<directoryname>\tThe file system location of HELM_HOME\n")
                .concat("\t-o\t<filename>\tA name and version of the chart as an appr specification\n")
                .concat("\t-e\t<filename>\tThe location of an Environment Specification\n")
                .concat("\t-g\t\t\tGenerate image from PlantUML file\n").concat("\t-r\t\t\tRefresh\n")
                .concat("\t-v\t\t\tVerbose\n").concat("\t-h\t\t\tHelp\n")
                .concat("\nSee https://github.com/melahn/helm-chartmap for more information\n");
        try {
            String helpText = ChartMap.getHelp();
            assert (helpText.equals(helpTextExpected));
        } catch (Exception e) {
            fail("testHelp failed:" + e.getMessage());
        }
    }

    @Test
    void chartUtilTest() {
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
            cmp = new ChartMapPrinter(createTestMap(ChartOption.FILENAME, testInputFilePath, f, true, true, true), "/",
                    null, null);
        } catch (ChartMapException e) {
            System.out.println("First ChartMapException expected and thrown");
            assertFalse(false);
        }
        cmp = new ChartMapPrinter(createTestMap(ChartOption.FILENAME, testInputFilePath, f, true, true, true),
                f.toString(), null, null);
        assertEquals(ChartMapPrinter.NOT_SPECIFIED, cmp.formatString(" "));
        assertEquals(ChartMapPrinter.NOT_SPECIFIED, cmp.formatString(null));
        HelmChart h = new HelmChart();
        h.setRepoUrl(null);
        cmp.printChart(h);
        assertFalse(fileContains(f, "repo url"));
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
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void PlantUMLChartMapPrinterTest() throws Exception {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputPumlFilePathNRNV, true,
                false, false);
        testMap.createTempDir();
        testMap.loadLocalRepos();
        testMap.resolveChartDependencies();
        ChartKeyMap ckm = testMap.chartsReferenced;
        HelmChart h = ckm.get("alfresco-content-services", "1.0.3");
        h.setRepoUrl(null);
        ckm.put("alfresco-content-services", "1.0.3", h);
        HashSet<String> ir1 = testMap.imagesReferenced;
        HashSet<String> ir2 = new HashSet<String>();
        for (String i : ir1) {
            if (i.equals("alfresco/alfresco-imagemagick:1.2")) {
                i = i.replace(':', 'X');
            }
            ir2.add(i);
        }
        testMap.imagesReferenced = ir2;
        Set<HelmDeploymentTemplate> templates = h.getDeploymentTemplates();
        for (HelmDeploymentTemplate t : templates) {
            HelmDeploymentContainer[] hdc = t.getSpec().getTemplate().getSpec().getContainers();
            for (HelmDeploymentContainer c : hdc) {
                if (c.getImage().equals("alfresco/alfresco-imagemagick:1.2")) {
                    c.setImage(c.getImage().replace(':', 'X'));
                }
            }
        }
        h.setDeploymentTemplates(templates);
        testMap.printMap();
        assertTrue(fileContains(testOutputPumlFilePathNRNV, "Unknown Repo URL"));
        assertTrue(fileContains(testOutputPumlFilePathNRNV, "alfresco_alfresco_imagemagickX1_2"));
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

    private ChartMap createTestMap(ChartOption option, Path inputPath, Path outputPath, boolean generateImage,
            boolean refresh, boolean verbose) throws Exception {
        ChartMap testMap = null;
        boolean[] switches;
        boolean debug = false; // less noisy but be careful of any tests that depend on debug entries
        switches = new boolean[] { generateImage, refresh, verbose, debug };
        try {
            testMap = new ChartMap(option, inputPath.toAbsolutePath().toString(),
                    outputPath.toAbsolutePath().toString(), System.getenv("HELM_HOME"),
                    testEnvFilePath.toAbsolutePath().toString(), switches);
        } catch (Exception e) {
            System.out.println("Exception createTestMap: " + e.getMessage());
        }
        return testMap;
    }

    private static void deletePreviouslyCreatedFiles() {
        try {
            System.out.println("Deleting any previously created files");
            if (Files.exists(Paths.get("./target/test/printer"))) {
                Files.walk(Paths.get("./target/test/printer"), 1).filter(Files::isRegularFile)
                        .forEach(p -> p.toFile().delete());
                Files.delete(Paths.get("./target/test/printer"));
            }
            if (Files.exists(Paths.get("./target/test"))) {
                Files.walk(Paths.get("./target/test/"), 1).filter(Files::isRegularFile)
                        .forEach(p -> p.toFile().delete());
                Files.deleteIfExists(Paths.get("./target/test"));
            }
        } catch (IOException e) {
            System.out.println("Error deleting previously created files: " + e.getMessage());
        }
    }

    private boolean fileContains(Path p, String s) {
        setFound(false);
        try (Stream<String> lines = Files.lines(p.toAbsolutePath())) {
            lines.forEach((String line) -> {
                if (line.contains(s)) {
                    System.out.println(String.format("Expected line with \"%s\" found", s));
                    setFound(true);
                }
            });
        } catch (IOException e) {
            System.out.println("Exception: " + e.getMessage());
        }
        return getFound();
    }

    boolean found = false; // used by Lambda in fileContains

    private void setFound(boolean f) {
        found = f;
    }

    private boolean getFound() {
        return found;
    }

    /**
     * Answers true if the log contains a particular entry
     * 
     * @param bais the log
     * @param s    entry being looked for
     * @return true if the log contains s, false otherwise
     */
    private boolean logContains(ByteArrayOutputStream bais, String s) {
        return bais.toString().contains(s);
    }
}