package com.melahn.util.helm;

import static com.melahn.util.test.ChartMapTestUtil.isWindows;
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.melahn.util.helm.model.HelmChart;
import com.melahn.util.helm.model.HelmChartLocalCache;
import com.melahn.util.helm.model.HelmChartRepoLocal;
import com.melahn.util.helm.model.HelmChartReposLocal;
import com.melahn.util.helm.model.HelmDeploymentContainer;
import com.melahn.util.helm.model.HelmDeploymentSpec;
import com.melahn.util.helm.model.HelmDeploymentSpecTemplate;
import com.melahn.util.helm.model.HelmDeploymentSpecTemplateSpec;
import com.melahn.util.helm.model.HelmDeploymentTemplate;
import com.melahn.util.test.ChartMapTestUtil;

import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

class ChartMapTest {
    private static String targetTest = "target/test";
    private static String targetTestDirectory = Paths.get(targetTest).toString();
    private static String APPRBaseName = "helm-chartmap-test-chart";
    private static String urlBaseName = "test-chart-file";
    private static Path testOutputPumlFilePathRV = Paths.get(targetTestDirectory, "testChartFileRV.puml");
    private static Path testOutputPumlFilePathNRV = Paths.get(targetTestDirectory, "testChartFileNRV.puml");
    private static Path testOutputPumlFilePathRNV = Paths.get(targetTestDirectory, "testChartFileRNV.puml");
    private static Path testOutputPumlFilePathNRNV = Paths.get(targetTestDirectory, "testChartFileNRNV.puml");
    private static Path testOutputPngFilePathNRNV = Paths.get(targetTestDirectory, "testChartFileNRNV.png");
    private static Path testOutputTextFilePathRV = Paths.get(targetTestDirectory, "testChartFileRV.txt");
    private static Path testOutputTextFilePathNRV = Paths.get(targetTestDirectory, "testChartFileNRV.txt");
    private static Path testOutputTextFilePathRNV = Paths.get(targetTestDirectory, "testChartFileRNV.txt");
    private static Path testOutputTextFilePathNRNV = Paths.get(targetTestDirectory, "testChartFileNRNV.txt");
    private static Path testOutputJSONFilePathRV = Paths.get(targetTestDirectory, "testChartFileRV.json");
    private static Path testOutputJSONFilePathNRV = Paths.get(targetTestDirectory, "testChartFileNRV.json");
    private static Path testOutputJSONFilePathRNV = Paths.get(targetTestDirectory, "testChartFileRNV.json");
    private static Path testOutputJSONFilePathNRNV = Paths.get(targetTestDirectory, "testChartFileNRNV.json");
    private static Path testOutputAPPRPumlPath = Paths.get(targetTestDirectory, APPRBaseName.concat(".puml"));
    private static Path testOutputAPPRPngPath = Paths.get(targetTestDirectory, APPRBaseName.concat(".png"));
    private static Path testOutputChartNamePumlPath = Paths.get(targetTestDirectory, "nginx:9.3.0.puml");
    private static Path testOutputChartNamePngPath = Paths.get(targetTestDirectory, "nginx:9.3.0.png");
    private static Path testOutputChartUrlPumlPath = Paths.get(targetTestDirectory, urlBaseName.concat(".puml"));
    private static Path testOutputChartUrlPngPath = Paths.get(targetTestDirectory, urlBaseName.concat(".png"));
    private static Path testOneFileZipPath = Paths.get("src/test/resource/test-onefile.tgz");
    private static Path testEnvFilePath = Paths.get("resource/example/example-env-spec.yaml");
    private static String testInputFileName = "src/test/resource/test-chart-file.tgz";
    private static String testChartName = "nginx:9.3.0";
    private static String testAPPRChart = "quay.io/melahn/helm-chartmap-test-chart@1.0.2";
    private static String testChartUrl = "https://github.com/melahn/helm-chartmap/raw/master/src/test/resource/test-chart-file.tgz";
    private final PrintStream initialOut = System.out;

    @AfterAll
    static void cleanUp() {
        /**
         * No cleanup to do after test. I don't delete the generated files because they
         * might be handy to have around to diagnose issues in test failures. They are
         * deleted anyway when the test is next run.
         */
        System.out.println("Test complete.  Any generated file can be found in "
                .concat(Paths.get(targetTestDirectory).toAbsolutePath().toString()));
    }

    @BeforeAll
    static void setUp() {
        try {
            if (!Files.exists(Paths.get(".", testInputFileName))) {
                throw new Exception(String.format("test Input File %s does not exist", testInputFileName));
            }
            ChartMapTestUtil.cleanDirectory(testOutputPumlFilePathRV.getParent());
            Files.createDirectories(testOutputPumlFilePathRV.getParent());
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void WeightedDeploymentTemplateTest() throws ChartMapException {
        ChartMap cm = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, false, false,
                false, false);
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
     * Tests the ChartMap.unpackTestChart method
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void unpackChartTest() throws ChartMapException, IOException {
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" starting"));
        ChartMap cm1 = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, false, false,
                false, true);
        cm1.setChartName("foo");
        cm1.createTempDir();
        assertThrows(ChartMapException.class, () -> cm1.unpackChart("foo"));
        // force ChartMapException path when no temp dir
        ChartMap cm2 = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, false, false,
                false, true);
        cm2.setChartName("foo");
        assertThrows(ChartMapException.class, () -> cm2.unpackChart("foo"));
        // force ChartMapException path when null chartmap passed
        cm2.createTempDir();
        assertThrows(ChartMapException.class, () -> cm2.unpackChart(null));
        // test when the tgz has no directory
        ChartMap cm3 = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, false, false,
                false, true);
        cm3.setChartName(null);
        cm3.setChartVersion(null);
        cm3.createTempDir();
        try (ByteArrayOutputStream unpackCharttestOut = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(unpackCharttestOut));
            assertThrows(ChartMapException.class, () -> cm3.unpackChart(testOneFileZipPath.toString()));
            assertTrue(
                    ChartMapTestUtil.streamContains(unpackCharttestOut, "Archive content does not appear to be valid"));
            System.setOut(initialOut);
        }
        // test when the tgz has no directory, this time with a non-null chartname and a
        // null version
        ChartMap cm4 = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, false, false,
                false, true);
        cm4.setChartName("foo");
        cm4.setChartVersion(null);
        cm4.createTempDir();
        try (ByteArrayOutputStream unpackCharttestOut = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(unpackCharttestOut));
            assertThrows(ChartMapException.class, () -> cm4.unpackChart(testOneFileZipPath.toString()));
            assertTrue(
                    ChartMapTestUtil.streamContains(unpackCharttestOut, "Archive content does not appear to be valid"));
            System.setOut(initialOut);
        }
        // test when the tgz has no directory, this time with a non-null version and a
        // null chartname to complete all the variations
        ChartMap cm5 = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, false, false,
                false, true);
        cm5.setChartName(null);
        cm5.setChartVersion("1.1.1");
        cm5.createTempDir();
        try (ByteArrayOutputStream unpackCharttestOut = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(unpackCharttestOut));
            assertThrows(ChartMapException.class, () -> cm5.unpackChart(testOneFileZipPath.toString()));
            assertTrue(
                    ChartMapTestUtil.streamContains(unpackCharttestOut, "Archive content does not appear to be valid"));
            System.setOut(initialOut);
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
        String b = ChartMap.getBaseName(Paths.get("./target").toString());
        assertEquals(null, b);
        ChartMap cm = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                false);
        cm.print();
        assertEquals(PrintFormat.PLANTUML, cm.getPrintFormat());
        cm.setPrintFormat(PrintFormat.JSON);
        assertEquals(PrintFormat.JSON, cm.getPrintFormat());
        assertEquals("chartmap.text", cm.getDefaultOutputFilename());
    }

    /**
     * Tests the ChartMap.loadLocalRepos method, focusing on the corner where an
     * IOException is caught and converted to a thrown ChartMapException. Mockiko
     * spying is used.
     * 
     * @throws ChartMapException
     */
    @Test
    void loadLocalReposTest() throws ChartMapException, IOException {
        ChartMap cm = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                false);
        ChartMap scm = spy(cm);
        ObjectMapper som = spy(ObjectMapper.class);
        doReturn(som).when(scm).getObjectMapper();
        doThrow(IOException.class).when(som).readValue(any(File.class), eq(HelmChartReposLocal.class));
        assertThrows(ChartMapException.class, () -> scm.loadLocalRepos());
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
        ChartMap cm1 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
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
        // Test not helm version 3
        ChartMap cm2 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                false);
        ChartMap scm2 = spy(cm2);
        // Use a command that is the same across all the OS's to mimic a helm not v3
        Process p2 = Runtime.getRuntime().exec(new String[] { "echo", "I am not helm version 3" });
        doReturn(p2).when(scm2).getProcess(any(), eq(null));
        assertThrows(ChartMapException.class, () -> scm2.checkHelmVersion());
        // Use a command that will cause the process' BufferedReader to return null and
        // force the ChartMapException.
        ChartMap cm3 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                false);
        ChartMap scm3 = spy(cm3);
        String nullCommand = isWindows() ? "type" : "cat";
        String nullArgument = isWindows() ? "NUL" : "/dev/null";
        Process p3 = Runtime.getRuntime().exec(new String[] { nullCommand, nullArgument });
        doReturn(p3).when(scm3).getProcess(any(), eq(null));
        assertThrows(ChartMapException.class, () -> scm3.checkHelmVersion());
        // Use a command that will cause the process' BufferedReader to just one
        // character and force the ChartMapException
        ChartMap cm4 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                false);
        ChartMap scm4 = spy(cm4);
        Process p4 = Runtime.getRuntime().exec(new String[] { "echo", "1" });
        doReturn(p4).when(scm4).getProcess(any(), eq(null));
        assertThrows(ChartMapException.class, () -> scm4.checkHelmVersion());
        // Cause an IOException -> ChartMapException on getProcess()
        ChartMap cm5 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                false);
        ChartMap scm5 = spy(cm5);
        doThrow(IOException.class).when(scm5).getProcess(any(), eq(null));
        assertThrows(ChartMapException.class, () -> scm5.checkHelmVersion());
        // Cause an InterruptedException -> ChartMapException on waitFor()
        // Be careful to put InterruptedException case last in the test case since the
        // thread is not usable after that
        ChartMap cm6 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                false);
        ChartMap scm6 = spy(cm6);
        Process p6 = Runtime.getRuntime()
                .exec(new String[] { "echo", "I am going to throw an InterruptedException!!" });
        Process sp6 = spy(p6);
        doReturn(sp6).when(scm6).getProcess(any(), eq(null));
        doThrow(InterruptedException.class).when(sp6).waitFor(ChartMap.PROCESS_TIMEOUT, TimeUnit.MILLISECONDS);
        assertThrows(ChartMapException.class, () -> scm6.checkHelmVersion());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the ChartMap.constructHelmCachePathTest method with all OS type and env
     * var combinations.
     * 
     * @throws ChartMapException
     */
    @Test
    void constructHelmCachePathTest() throws ChartMapException, IOException {
        ChartMap cm1 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true);
        ChartMap scm1 = spy(cm1);
        doReturn(targetTest).when(scm1).getEnv("HOME");
        scm1.constructHelmCachePath(ChartUtil.OSType.MACOS);
        assertEquals(targetTest.concat("/Library/Caches/helm"), scm1.getHelmCachePath());

        ChartMap cm2 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true);
        ChartMap scm2 = spy(cm2);
        doReturn(targetTest).when(scm2).getEnv("HOME");
        scm2.constructHelmCachePath(ChartUtil.OSType.LINUX);
        assertEquals(targetTest.concat("/.cache/helm"), scm2.getHelmCachePath());

        ChartMap cm3 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true);
        ChartMap scm3 = spy(cm3);
        doReturn(targetTest).when(scm3).getEnv("TEMP");
        scm3.constructHelmCachePath(ChartUtil.OSType.WINDOWS);
        assertEquals(targetTest.concat("/helm"), scm3.getHelmCachePath());

        ChartMap cm4 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true);
        ChartMap scm4 = spy(cm4);
        doReturn(targetTest.concat("/XDG_CACHE_HOME")).when(scm4).getEnv("XDG_CACHE_HOME");
        scm4.constructHelmCachePath(ChartUtil.OSType.MACOS);
        assertEquals(targetTest.concat("/XDG_CACHE_HOME"), scm4.getHelmCachePath());

        ChartMap cm5 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true);
        ChartMap scm5 = spy(cm5);
        doReturn(targetTest.concat("/HELM_CACHE_HOME")).when(scm5).getEnv("HELM_CACHE_HOME");
        scm5.constructHelmCachePath(ChartUtil.OSType.MACOS);
        assertEquals(targetTest.concat("/HELM_CACHE_HOME"), scm5.getHelmCachePath());

        // No valid helm cache directory in MACOSX is found so look for the exception
        // and logged error message
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm6 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                    true);
            ChartMap scm6 = spy(cm6);
            doReturn(null).when(scm6).getEnv("HOME");
            assertThrows(ChartMapException.class, () -> scm6.constructHelmCachePath(ChartUtil.OSType.MACOS));
            assertTrue(ChartMapTestUtil.streamContains(o, String.format(ChartMap.CHECK_OS_MSG, ChartMap.HOME)));
            System.setOut(initialOut);
        }

        // No valid helm cache directory in LINUX is found so look for the exception and
        // logged error message
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm7 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                    true);
            ChartMap scm7 = spy(cm7);
            doReturn(null).when(scm7).getEnv("HOME");
            assertThrows(ChartMapException.class, () -> scm7.constructHelmCachePath(ChartUtil.OSType.LINUX));
            assertTrue(ChartMapTestUtil.streamContains(o, String.format(ChartMap.CHECK_OS_MSG, ChartMap.HOME)));
            System.setOut(initialOut);
        }

        // No valid helm cache directory in Windows is found so look for the exception
        // and logged error message
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm8 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                    true);
            ChartMap scm8 = spy(cm8);
            doReturn(null).when(scm8).getEnv("TEMP");
            assertThrows(ChartMapException.class, () -> scm8.constructHelmCachePath(ChartUtil.OSType.WINDOWS));
            assertTrue(ChartMapTestUtil.streamContains(o, String.format(ChartMap.CHECK_OS_MSG, ChartMap.TEMP)));
            System.setOut(initialOut);
        }

        // All other cases
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm9 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                    true);
            assertThrows(ChartMapException.class, () -> cm9.constructHelmCachePath(ChartUtil.OSType.OTHER));
            assertTrue(ChartMapTestUtil.streamContains(o,
                    "Could not locate the helm cache path. Check that your installation of helm is complete and that you are using a supported OS."));
            System.setOut(initialOut);
        }

        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the ChartMap.constructHelmConfigPathTest method with all OS type and env
     * var combinations.
     * 
     * @throws ChartMapException
     */
    @Test
    void constructHelmConfigPathTest() throws ChartMapException, IOException {
        ChartMap cm1 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true, true);
        ChartMap scm1 = spy(cm1);
        doReturn(targetTest).when(scm1).getEnv("HOME");
        scm1.constructHelmConfigPath(ChartUtil.OSType.MACOS);
        assertEquals(targetTest.concat("/Library/Preferences/helm"), scm1.getHelmConfigPath());

        ChartMap cm2 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true, true);
        ChartMap scm2 = spy(cm2);
        doReturn(targetTest).when(scm2).getEnv("HOME");
        scm2.constructHelmConfigPath(ChartUtil.OSType.LINUX);
        assertEquals(targetTest.concat("/.config/helm"), scm2.getHelmConfigPath());

        ChartMap cm3 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true, true);
        ChartMap scm3 = spy(cm3);
        doReturn(targetTest).when(scm3).getEnv("APPDATA");
        scm3.constructHelmConfigPath(ChartUtil.OSType.WINDOWS);
        assertEquals(targetTest.concat("/helm"), scm3.getHelmConfigPath());

        ChartMap cm4 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true, true);
        ChartMap scm4 = spy(cm4);
        doReturn(targetTest.concat("/XDG_CONFIG_HOME")).when(scm4).getEnv("XDG_CONFIG_HOME");
        scm4.constructHelmConfigPath(ChartUtil.OSType.MACOS);
        assertEquals(targetTest.concat("/XDG_CONFIG_HOME"), scm4.getHelmConfigPath());

        ChartMap cm5 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true, true);
        ChartMap scm5 = spy(cm5);
        doReturn(targetTest.concat("/HELM_CONFIG_HOME")).when(scm5).getEnv("HELM_CONFIG_HOME");
        scm5.constructHelmConfigPath(ChartUtil.OSType.MACOS);
        assertEquals(targetTest.concat("/HELM_CONFIG_HOME"), scm5.getHelmConfigPath());

        // No valid helm config directory in MACOS is found so look for the exception
        // and logged error message
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm6 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                    true, true);
            ChartMap scm6 = spy(cm6);
            doReturn(null).when(scm6).getEnv("HOME");
            assertThrows(ChartMapException.class, () -> scm6.constructHelmConfigPath(ChartUtil.OSType.MACOS));
            assertTrue(ChartMapTestUtil.streamContains(o, String.format(ChartMap.CHECK_OS_MSG, ChartMap.HOME)));
            System.setOut(initialOut);
        }

        // No valid helm config directory in LINUX is found so look for the exception
        // and
        // logged error message
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm7 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                    true, true);
            ChartMap scm7 = spy(cm7);
            doReturn(null).when(scm7).getEnv("HOME");
            assertThrows(ChartMapException.class, () -> scm7.constructHelmConfigPath(ChartUtil.OSType.LINUX));
            assertTrue(ChartMapTestUtil.streamContains(o, String.format(ChartMap.CHECK_OS_MSG, ChartMap.HOME)));
            System.setOut(initialOut);
        }

        // No valid helm config directory in Windows is found so look for the exception
        // and logged error message
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm8 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                    true, true);
            ChartMap scm8 = spy(cm8);
            doReturn(null).when(scm8).getEnv("APPDATA");
            assertThrows(ChartMapException.class, () -> scm8.constructHelmConfigPath(ChartUtil.OSType.WINDOWS));
            assertTrue(ChartMapTestUtil.streamContains(o, String.format(ChartMap.CHECK_OS_MSG, ChartMap.APPDATA)));
            System.setOut(initialOut);
        }

        // All other cases
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm9 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                    true, true);
            assertThrows(ChartMapException.class, () -> cm9.constructHelmConfigPath(ChartUtil.OSType.OTHER));
            assertTrue(ChartMapTestUtil.streamContains(o,
                    "Could not locate the helm config path. Check that your installation of helm is complete and that you are using a supported OS."));
            System.setOut(initialOut);
        }

        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the ChartMap.getHelmCommand method.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void constructGetHelmCommandTest() throws ChartMapException, IOException {
        // The helm command is found in HELM_BIN
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm1 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                    true, true);
            ChartMap scm1 = spy(cm1);
            String h = "target/test/HELM_BIN";
            doReturn(h).when(scm1).getEnv("HELM_BIN");
            scm1.getHelmCommand();
            assertTrue(ChartMapTestUtil.streamContains(o, String.format("The helm command %s will be used", h)));
            System.setOut(initialOut);
        }
        // The helm command is not found in HELM_BIN so the default "helm" is used
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm1 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                    true, true);
            ChartMap scm1 = spy(cm1);
            doReturn(null).when(scm1).getEnv("HELM_BIN");
            scm1.getHelmCommand();
            assertTrue(ChartMapTestUtil.streamContains(o, String.format("The helm command %s will be used", "helm")));
            System.setOut(initialOut);
        }
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
        HelmChartRepoLocal r = new HelmChartRepoLocal();
        // Fabricate a HelmChartRepoLocal (I only need tbe url for this test)
        r.setUrl("http://foo");
        String n = "foo";
        String v = "6.6.6";
        // fabricate a cache yaml file with one entry
        String s = "apiVersion: v1\nentries:\n  foo-chart:\n  - name: ".concat(n).concat("\n    version: ").concat(v)
                .concat("\n".concat("    urls:\n    - https://foo\n"));
        String c = "loadChartsFromCacheTest.yaml";
        Path p = Paths.get(targetTestDirectory, c);
        File f = Files.createFile(p).toFile();
        byte[] b = s.getBytes();
        Files.write(p, b);
        // create a test ChartMap and validate I can load the chart from my fabricated
        // cache
        ChartMap cm1 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true);
        cm1.loadChartsFromCache(r, f);
        assertNotNull(cm1.getCharts().get("foo", "6.6.6"));

        // Test for a missing urls element in the cache
        Files.deleteIfExists(p);
        f = Files.createFile(p).toFile();
        s = "apiVersion: v1\nentries:\n  foo-chart:\n  - name: ".concat(n).concat("\n    version: ").concat(v)
                .concat("\n".concat("    urls:\n"));
        b = s.getBytes();
        Files.write(p, b);
        ChartMap cm2 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true);
        cm2.loadChartsFromCache(r, f);
        assertNotNull(cm2.getCharts().get("foo", "6.6.6"));

        // Test for an empty urls array in the cache
        Files.deleteIfExists(p);
        f = Files.createFile(p).toFile();
        s = "apiVersion: v1\nentries:\n  foo-chart:\n  - name: ".concat(n).concat("\n    version: ").concat(v)
                .concat("\n".concat("    urls: []\n"));
        b = s.getBytes();
        Files.write(p, b);
        ChartMap cm3 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true);
        cm3.loadChartsFromCache(r, f);
        assertNotNull(cm3.getCharts().get("foo", "6.6.6"));
        // test for an empty urls array in the cache
        Files.deleteIfExists(p);
        f = Files.createFile(p).toFile();
        s = "apiVersion: v1\nentries:\n  foo-chart:\n  - name: ".concat(n).concat("\n    version: ").concat(v)
                .concat("\n".concat("    urls: []\n"));
        b = s.getBytes();
        Files.write(p, b);
        ChartMap cm4 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true);
        cm4.loadChartsFromCache(r, f);
        assertNotNull(cm4.getCharts().get("foo", "6.6.6"));

        // Test for an empty string element in the urls array
        Files.deleteIfExists(p);
        f = Files.createFile(p).toFile();
        s = "apiVersion: v1\nentries:\n  foo-chart:\n  - name: ".concat(n).concat("\n    version: ").concat(v)
                .concat("\n".concat("    urls:\n    - ''\n"));
        b = s.getBytes();
        Files.write(p, b);
        ChartMap cm5 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true);
        cm5.loadChartsFromCache(r, f);
        assertNotNull(cm5.getCharts().get("foo", "6.6.6"));

        // Finally, force an IOException and check the log to complete all the possible
        // branches
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            Files.deleteIfExists(p);
            f = Files.createFile(p).toFile();
            s = "apiVersion: v1\nentries:\n  foo-chart:\n  - name: ".concat(n).concat("\n    version: ").concat(v)
                    .concat("\n".concat("    urls:\n    - ''\n"));
            b = s.getBytes();
            Files.write(p, b);
            ChartMap cm6 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                    false);
            ChartMap scm6 = spy(cm6);
            ObjectMapper som6 = spy(ObjectMapper.class);
            doReturn(som6).when(scm6).getObjectMapper();
            doThrow(IOException.class).when(som6).readValue(any(File.class), eq(HelmChartLocalCache.class));
            System.setOut(new PrintStream(o));
            scm6.loadChartsFromCache(r, f);
            assertTrue(ChartMapTestUtil.streamContains(o, String.format("Error loading charts from helm cache: ")));
            System.setOut(initialOut);
        }
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
        ChartMap cm1 = createTestMap(ChartOption.FILENAME, "src/test/resource/test-fakechart.tgz",
                Paths.get("test-fakechart.txt"), true, false, false);
        cm1.print();
        assertTrue(
                ChartMapTestUtil.fileContains(Paths.get("test-fakechart.txt"), "There is one referenced Helm Chart"));
        // Force a ChartMapException using a spy
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            ChartMap cm2 = createTestMap(ChartOption.FILENAME, "src/test/resource/test-fakechart.tgz",
                    Paths.get("test-fakechart.txt"), true, false, false);
            ChartMap scm2 = spy(cm2);
            IChartMapPrinter sp2 = spy(IChartMapPrinter.class);
            doReturn(sp2).when(scm2).getPrinter();
            doThrow(ChartMapException.class).when(sp2).printSectionHeader(any(String.class));
            scm2.print();
            assertTrue(ChartMapTestUtil.streamContains(o, "IOException printing charts:"));
            System.setOut(initialOut);
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
        ChartMap cm1 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true);
        try (MockedStatic<Files> mf = Mockito.mockStatic(Files.class)) {
            mf.when(() -> Files.walk(any(Path.class), anyInt())).thenThrow(IOException.class);
            System.out.print("An IOException is swallowed but expect this log message ... ");
            assertThrows(ChartMapException.class, () -> cm1.extractEmbeddedCharts("foo"));
        }
        assertThrows(RuntimeException.class, () -> lambdaWrapper());
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
        Files.deleteIfExists(Paths.get("test-extract", "f"));
        Files.deleteIfExists(Paths.get("test-extract"));
        Path d = Files.createDirectories(Paths.get("test-extract"));
        Files.createFile(Paths.get("test-extract", "f"));
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
            ChartMap cm1 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                    true);
            ChartMap scm1 = spy(cm1);
            doReturn(null).when(scm1).getChart();
            doReturn("foobar").when(scm1).getChartName();
            scm1.resolveChartDependencies();
            assertTrue(ChartMapTestUtil.streamContains(o, "Chart foobar was not found"));
            System.setOut(initialOut);
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
        ChartMap cm1 = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, false, false,
                false);
        ChartMap scm1 = spy(cm1);
        doReturn(null).when(scm1).getChart(anyString());
        assertNull(scm1.getChart());

        // Test getChart(String c) IOException to ChartMapException
        ChartMap cm2 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputTextFilePathNRNV, false, false,
                true);
        cm2.print();
        try (MockedStatic<Files> mf = Mockito.mockStatic(Files.class)) {
            mf.when(() -> Files.copy(any(Path.class), any(Path.class), eq(StandardCopyOption.REPLACE_EXISTING)))
                    .thenThrow(IOException.class);
            assertThrows(ChartMapException.class, () -> cm2.getChart(cm2.getChartName()));
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
        ChartMap cm1 = createTestMap(ChartOption.APPRSPEC, testAPPRChart, testOutputAPPRPumlPath, true, false, false);
        ChartMap scm1 = spy(cm1);
        doThrow(IOException.class).when(scm1).getProcess(any(), any(File.class));
        assertThrows(ChartMapException.class, () -> scm1.print());
        // Use a spy to throw an InterruptedException -> ChartMapException
        // Be careful to put InterruptedException case last in the test case since the
        // thread is not usable after that
        ChartMap cm2 = createTestMap(ChartOption.APPRSPEC, testAPPRChart, testOutputAPPRPumlPath, true, false, false);
        ChartMap scm2 = spy(cm2);
        Process p2 = Runtime.getRuntime()
                .exec(new String[] { "echo", "I am going to throw an InterruptedException!!" });
        Process sp2 = spy(p2);
        doReturn(sp2).when(scm2).getProcess(any(), any(File.class));
        doThrow(InterruptedException.class).when(sp2).waitFor(ChartMap.PROCESS_TIMEOUT, TimeUnit.MILLISECONDS);
        assertThrows(ChartMapException.class, () -> scm2.print());
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
            ChartMap cm1 = createTestMap(ChartOption.APPRSPEC, testAPPRChart, testOutputAPPRPumlPath, true, false,
                    false);
            assertThrows(ChartMapException.class, () -> cm1.downloadChart("http://example.com"));

        }
        // Test a bad http rc using a url that's guraranteed not to exist. See
        // https://github.com/Readify/httpstatus.
        ChartMap cm2 = createTestMap(ChartOption.APPRSPEC, testAPPRChart, testOutputAPPRPumlPath, true, false, false);
        assertThrows(ChartMapException.class, () -> cm2.downloadChart("https://httpstat.us/404"));
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
        ChartMap cm1 = createTestMap(ChartOption.APPRSPEC, testAPPRChart, testOutputAPPRPumlPath, false, true, false);
        ChartMap scm1 = spy(cm1);
        doThrow(IOException.class).when(scm1).getProcess(any(), any(File.class));
        assertThrows(ChartMapException.class, () -> scm1.updateLocalRepo("foo"));
        // Cause a bad exit value from the process
        ChartMap cm2 = createTestMap(ChartOption.APPRSPEC, testAPPRChart, testOutputAPPRPumlPath, false, true, false);
        ChartMap scm2 = spy(cm2);
        Process p2 = Runtime.getRuntime().exec(new String[] { "echo", "I am going to return a bad exitvalue!!!" });
        Process sp2 = spy(p2);
        doReturn(sp2).when(scm2).getProcess(any(), any(File.class));
        doReturn(1).when(sp2).exitValue();
        assertThrows(ChartMapException.class, () -> scm2.updateLocalRepo("foo"));
        // Simulate an InterruptedException -> ChartMapException on waitFor()
        // Be careful to put InterruptedException case last in the test case since the
        // thread is not usable after that
        ChartMap cm3 = createTestMap(ChartOption.APPRSPEC, testAPPRChart, testOutputAPPRPumlPath, false, true, false);
        ChartMap scm3 = spy(cm3);
        Process p3 = Runtime.getRuntime()
                .exec(new String[] { "echo", "I am going to throw an InterruptedException!!" });
        Process sp3 = spy(p3);
        doReturn(sp3).when(scm3).getProcess(any(), any(File.class));
        doThrow(InterruptedException.class).when(sp3).waitFor(ChartMap.PROCESS_TIMEOUT, TimeUnit.MILLISECONDS);
        assertThrows(ChartMapException.class, () -> scm3.updateLocalRepo("foo"));
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
            ChartMap cm1 = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, false,
                    false, false);
            ChartMap scm1 = spy(cm1);
            ObjectMapper som1 = spy(ObjectMapper.class);
            doReturn(som1).when(scm1).getObjectMapper();
            doThrow(IOException.class).when(som1).readValue(any(File.class), eq(HelmChart.class));
            String s = "foo";
            System.setOut(new PrintStream(o));
            scm1.createChart(s);
            assertTrue(ChartMapTestUtil.streamContains(o, "IOException extracting Chart information from ".concat(s)
                    .concat(File.separator).concat(ChartMap.CHART_YAML)));
            System.setOut(new PrintStream(initialOut));
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
        Path d = Paths.get(targetTest, "getConditionMapDirectory");
        Path f = Paths.get(d.toString(), "requirements.yaml");
        Files.deleteIfExists(f);
        Files.deleteIfExists(d);
        Files.createDirectory(d);
        Files.createFile(f);
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            ChartMap cm1 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                    false);
            System.setOut(new PrintStream(o));
            cm1.getConditionMap(d.toString());
            // no need for mockito here since the empty requirements file will induce an
            // IOExceotion
            assertTrue(ChartMapTestUtil.streamContains(o, "IOException parsing requirements file"));
            System.setOut(new PrintStream(initialOut));
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
        ChartMap cm1 = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, false, false,
                false);
        HelmChart h = new HelmChart();
        h.setName("foo");
        h.setVersion("1.1.1");
        cm1.print();
        int i1 = cm1.getChartsReferenced().size();
        Path f1 = Paths.get(targetTest, "collectDependenciesTest");
        Files.deleteIfExists(f1);
        Files.createFile(f1);
        cm1.collectDependencies(f1.toString(), h);
        assertEquals(i1, cm1.getChartsReferenced().size());
        // Test the chart file does not exist case
        ChartMap cm2 = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, false, false,
                false);
        cm2.print();
        int i2 = cm2.getChartsReferenced().size();
        Path d2p = Paths.get(targetTest, "collectDependenciesTestDir1");
        Path d2c = Paths.get(d2p.toString(), "collectDependenciesTestDir2");
        Files.deleteIfExists(d2c);
        Files.deleteIfExists(d2p);
        Files.createDirectory(d2p);
        Files.createDirectory(d2c);
        cm2.collectDependencies(d2p.toString(), null);
        assertEquals(i2, cm2.getChartsReferenced().size());
        // Test an empty Chart.yaml file tp cause an IO Exception -> ChartMapException
        Path p3 = Paths.get(d2c.toString(), ChartMap.CHART_YAML);
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            ChartMap cm3 = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, false,
                    false, false);
            cm3.print();
            Files.createFile(p3);
            System.setOut(new PrintStream(o));
            assertThrows(ChartMapException.class, () -> cm3.collectDependencies(d2p.toString(), null));
            assertTrue(ChartMapTestUtil.streamContains(o, "IOException getting Dependencies"));
            System.setOut(new PrintStream(initialOut));
        }
        // Test the Chart.yaml file references a chart that does not exist
        ChartMap cm4 = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, false, false,
                false);
        cm4.print();
        String s = "apiVersion: v1\nentries:\n  foo-chart:\n  - name: ".concat("foo").concat("\n    version: ")
                .concat("1.1.1").concat("\n".concat("    urls:\n    - https://foo\n"));
        byte[] b = s.getBytes();
        Files.write(p3, b);
        assertThrows(ChartMapException.class, () -> cm4.collectDependencies(d2p.toString(), null));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the ChartMap.checkForCondition method.
     * 
     * @throws ChartMapException
     */
    @Test
    void checkForConditionTest()  throws ChartMapException {
        ChartMap cm1 = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, false,
        false, false);
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
            ChartMap cm1 = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, false,
                    false, false);
            ChartMap scm1 = spy(cm1);
            doThrow(IOException.class).when(scm1).collectValues(anyString(), any(HelmChart.class));
            System.setOut(new PrintStream(o));
            assertThrows(ChartMapException.class, () -> scm1.handleHelmChartCondition(new Boolean(true), "foo", "foo",
                    new HelmChart(), new HelmChart()));
            assertTrue(ChartMapTestUtil.streamContains(o, "IOException collecting values in handleHelmChartCondition"));
            System.setOut(new PrintStream(initialOut));
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
        ChartMap cm1 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true);
        try (MockedStatic<Files> mf = Mockito.mockStatic(Files.class)) {
            mf.when(() -> Files.createTempDirectory(any(), any())).thenThrow(IOException.class);
            assertThrows(ChartMapException.class, () -> cm1.createTempDir());
        }
        System.out.println("IOException -> ChartMapException thrown as expected attempting to create temp dir");
        ChartMap cm2 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true);
        cm2.createTempDir();
        try (MockedStatic<Files> mf = Mockito.mockStatic(Files.class)) {
            mf.when(() -> Files.walk(any(), any())).thenThrow(IOException.class);
            assertThrows(ChartMapException.class, () -> cm2.removeTempDir());
        }
        // create the ChartMap in debug mode and be sure the temp dir is not deleted
        ChartMap cm3 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true, true);
        cm3.print();
        assertTrue(Files.exists(Paths.get(cm3.getTempDirName())));
        // create the ChartMap in non-debug mode and be sure the temp dir is deleted
        ChartMap cm4 = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                true);
        cm4.print();
        assertFalse(Files.exists(Paths.get(cm4.getTempDirName())));
        System.out.println("IOException -> ChartMapException thrown as expected attempting to remove temp dir");
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void pumlChartRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputPumlFilePathRV, true, true,
                true);
        testMap.print();
        assertTrue(Files.exists(testOutputPumlFilePathRV));
    }

    @Test
    void pumlChartNoRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputPumlFilePathNRV, true,
                false, true);
        testMap.print();
        assertTrue(Files.exists(testOutputPumlFilePathNRV));
    }

    @Test
    void pumlChartRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputPumlFilePathRNV, true, true,
                false);
        testMap.print();
        assertTrue(Files.exists(testOutputPumlFilePathRNV));
    }

    @Test
    void pumlChartNoRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputPumlFilePathNRNV, true,
                false, false);
        testMap.print();
        assertTrue(Files.exists(testOutputPumlFilePathNRNV));
        assertTrue(Files.exists(testOutputPngFilePathNRNV));
    }

    @Test
    void textChartRefreshVerboseTest() throws ChartMapException, IOException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathRV, true, true,
                true);
        testMap.print();
        assertTrue(Files.exists(testOutputTextFilePathRV));
        assertTrue(ChartMapTestUtil.fileContains(testOutputTextFilePathRV,
                "WARNING: Chart alfresco-content-services:1.0.3 is stable but depends on alfresco-search:0.0.4 which may not be stable"));
    }

    @Test
    void textChartNoRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRV, true,
                false, true);
        testMap.print();
        assertTrue(Files.exists(testOutputTextFilePathNRV));
    }

    @Test
    void textChartRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathRNV, true, true,
                false);
        testMap.print();
        assertTrue(Files.exists(testOutputTextFilePathRNV));
    }

    @Test
    void textChartNoRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputTextFilePathNRNV, true,
                false, false);
        testMap.print();
        assertTrue(Files.exists(testOutputTextFilePathNRNV));
        // todo compare NR generated files with time stamp removed with a known good
        // result for a better test
    }

    @Test
    void JSONChartRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputJSONFilePathRV, true, true,
                true);
        testMap.print();
        assertTrue(Files.exists(testOutputJSONFilePathRV));

    }

    @Test
    void JSONChartNoRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputJSONFilePathNRV, true,
                false, true);
        testMap.print();
        assertTrue(Files.exists(testOutputJSONFilePathNRV));

    }

    @Test
    void JSONChartRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputJSONFilePathRNV, true, true,
                false);
        testMap.print();
        assertTrue(Files.exists(testOutputJSONFilePathRNV));
    }

    @Test
    void JSONChartNoRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputJSONFilePathNRNV, true,
                false, false);
        testMap.print();
        assertTrue(Files.exists(testOutputJSONFilePathNRNV));
    }

    @Test
    void APPRTest() throws ChartMapException { // test normal path
        ChartMap cm1 = createTestMap(ChartOption.APPRSPEC, testAPPRChart, testOutputAPPRPumlPath, true, false, false);
        cm1.print();
        assertTrue(Files.exists(testOutputAPPRPumlPath));
        // test null appr spec
        assertTrue(Files.exists(testOutputAPPRPngPath));
        assertThrows(ChartMapException.class,
                () -> createTestMap(ChartOption.APPRSPEC, null, testOutputAPPRPumlPath, true, false, false));
        // test various malformed appr specs
        assertThrows(ChartMapException.class, () -> createTestMap(ChartOption.APPRSPEC, "badapprspec/noat",
                testOutputAPPRPumlPath, true, false, false));
        assertThrows(ChartMapException.class, () -> createTestMap(ChartOption.APPRSPEC, "badapprspec@noslash",
                testOutputAPPRPumlPath, true, false, false)); // test chart not found in app repo
        ChartMap cm2 = createTestMap(ChartOption.APPRSPEC, "quay.io/melahn/no-such-chart@1.0.0", testOutputAPPRPumlPath,
                true, false, false);
        assertThrows(ChartMapException.class, () -> cm2.print());
    }

    @Test
    void UrlTest() throws ChartMapException { // test normal path
        ChartMap cm = createTestMap(ChartOption.URL, testChartUrl, testOutputChartUrlPumlPath, true, false, false);
        cm.print();
        assertTrue(Files.exists(testOutputChartUrlPumlPath));
        assertTrue(Files.exists(testOutputChartUrlPngPath)); // test null chart name
        assertThrows(ChartMapException.class,
                () -> createTestMap(ChartOption.URL, null, testOutputChartUrlPumlPath, true, false, false));
    }

    @Test
    void chartNameTest() throws ChartMapException {
        // test normal path without using createTestMap utility function because I want
        // null env var file
        boolean[] switches = { true, false, false, false };
        ChartMap cm1 = new ChartMap(ChartOption.CHARTNAME, testChartName,
                testOutputChartNamePumlPath.toAbsolutePath().toString(), null, switches);
        cm1.print();
        assertTrue(Files.exists(testOutputChartNamePumlPath));
        assertTrue(Files.exists(testOutputChartNamePngPath));
        // test missing version in chartname
        assertThrows(ChartMapException.class, () -> createTestMap(ChartOption.CHARTNAME, "badchartname-noversion",
                testOutputChartNamePumlPath, true, false, false));
        // test chart not found
        ChartMap cm2 = createTestMap(ChartOption.CHARTNAME, "no-such-chart:9.9.9", testOutputChartNamePumlPath, true,
                false, false);
        assertThrows(ChartMapException.class, () -> cm2.print());
    }

    @Test
    void optionsTest() throws ChartMapException {
        boolean[] switches = { true, false, false, false }; // test that a correct option is used
        assertThrows(ChartMapException.class, () -> new ChartMap(null, testChartName,
                testOutputChartNamePumlPath.toAbsolutePath().toString(), null, switches)); //
        // test a bad switches array
        assertThrows(ChartMapException.class, () -> new ChartMap(ChartOption.CHARTNAME, testChartName,
                testOutputChartNamePumlPath.toAbsolutePath().toString(), null, new boolean[3]));
    }

    /**
     * Proves the help text is what is expected.
     */
    @Test
    void helpTest() {
        String helpTextExpected = "\nUsage:\n\n".concat("java -jar helm-chartmap-1.0.3.jar\n").concat("\nFlags:\n")
                .concat("\t-a\t<apprspec>\tA name and version of a chart as an appr specification\n")
                .concat("\t-c\t<chartname>\tA name and version of a chart\n")
                .concat("\t-f\t<filename>\tA location in the file system for a Helm Chart package (a tgz file)\n")
                .concat("\t-u\t<url>\t\tA url for a Helm Chart\n")
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
            cmp = new ChartMapPrinter(createTestMap(ChartOption.FILENAME, testInputFileName, f, true, true, true), "/",
                    null, null);
        } catch (ChartMapException e) {
            System.out.println("First ChartMapException expected and thrown");
            assertFalse(false);
        }
        cmp = new ChartMapPrinter(createTestMap(ChartOption.FILENAME, testInputFileName, f, true, true, true),
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
        ChartMap cm = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputPumlFilePathNRNV, true, false,
                false);
        cm.logger = null;
        cmp = new ChartMapPrinter(cm, f.toString(), null, null);
        String nl = "null logger";
        cmp.writeLine(nl);
        assertTrue(ChartMapTestUtil.fileContains(f, nl));
        // force IOException to ChartMapException with a ChartMap with a null logger
        assertThrows(ChartMapException.class, () -> new ChartMapPrinter(cm, "/", null, null));
        System.out.println("Third ChartMapException expected and thrown");
        // force IOException using mocking
        FileWriter mfr = mock(FileWriter.class);
        doThrow(new IOException("IO Exception occured")).when(mfr).write(anyString());
        ChartMapPrinter cmp2 = new ChartMapPrinter(cm, f.toString(), null, null);
        cmp2.writer = mfr;
        assertThrows(ChartMapException.class, () -> {
            cm.logger = null;
            cmp2.writeLine("IO Exception");
        });
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void PlantUMLChartMapPrinterTest() throws Exception {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputPumlFilePathNRNV, true,
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
        assertTrue(ChartMapTestUtil.fileContains(testOutputPumlFilePathNRNV, "Unknown Repo URL"));
        assertTrue(ChartMapTestUtil.fileContains(testOutputPumlFilePathNRNV, "alfresco_alfresco_imagemagickX1_2"));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void debugTest() throws ChartMapException, IOException {
        ChartMap cm = createTestMap(ChartOption.FILENAME, testInputFileName, testOutputPumlFilePathNRNV, true, false,
                false, true);
        try (ByteArrayOutputStream o = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(o));
            cm.print();
            assertTrue(ChartMapTestUtil.streamContains(o, "was not removed because this is debug mode"));
            System.setOut(new PrintStream(initialOut));
        }
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

    private ChartMap createTestMap(ChartOption option, String input, Path outputPath, boolean generateImage,
            boolean refresh, boolean verbose, boolean... options) throws ChartMapException {
        // debug off makes it less noisy but be careful of any tests that depend on
        // debug entries
        boolean debug = (options.length > 0) ? options[0] : false;
        boolean[] switches = new boolean[] { generateImage, refresh, verbose, debug };
        ChartMap cm = new ChartMap(option, input, outputPath.toAbsolutePath().toString(),
                testEnvFilePath.toAbsolutePath().toString(), switches);
        cm.setLogLevel(); // set this explictly because some of the test cases may depend on a logger and
                          // don't call print
        cm.setHelmEnvironment(); // set this explictly so that test cases can test helm dependent methods without
                                 // necessarily calling print
        return cm;
    }
}