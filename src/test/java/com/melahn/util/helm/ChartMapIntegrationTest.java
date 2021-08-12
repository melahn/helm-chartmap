package com.melahn.util.helm;

import static com.melahn.util.test.ChartMapTestUtil.logContains;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
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
import org.junit.jupiter.api.Test;

public class ChartMapIntegrationTest {
    // needed for main test; it would be nice not to get from the pom instead
    private static String versionSuffix = "-1.0.3-SNAPSHOT";
    private static String targetTestDirectory = Paths.get("target/test").toString();
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
    private static String testInputFilePath = "src/test/resource/test-chart-file.tgz";
    private static String testChartName = "nginx:9.3.0";
    private static String testAPPRChart = "quay.io/melahn/helm-chartmap-test-chart@1.0.2";
    private static String testChartUrl = "https://github.com/melahn/helm-chartmap/raw/master/src/test/resource/test-chart-file.tgz";
    private final PrintStream initialOut = System.out;

    @Test
    void chartMapMainTest() throws ChartMapException, IOException, InterruptedException {
        String OutputFile = "testChartFileRV.txt";
        if (Files.notExists(Paths.get("./target/test"))) {
            Files.createDirectories(Paths.get("./target/test"));
        }
        // I use the previolsy generated shaded jar just to resolve the third party
        // dependencies.
        // This could miss a bug introduced by a third party dependency upgrade though
        // it
        // would be caught on the next test because the shaded jar would have this
        // updated dependency. Perhaps there is a better way
        String a[] = new String[] { "java", "-cp",
                ".:../../resource/jar/helm-chartmap".concat(versionSuffix).concat(".jar"),
                "com.melahn.util.helm.ChartMap", "-f", "../../".concat(testInputFilePath.toString()), "-e",
                "../../".concat(testEnvFilePath.toString()), "-o", OutputFile };
        // normalcase calling main as a new process
        Process p = Runtime.getRuntime().exec(a, null, new File(targetTestDirectory.toString()));
        p.waitFor(30000, TimeUnit.MILLISECONDS);
        assertEquals(0, p.exitValue());
        assertTrue(Files.exists(Paths.get(targetTestDirectory, OutputFile)));
        Files.deleteIfExists(Paths.get(targetTestDirectory, OutputFile));
        String[] b = new String[] { "-f", testInputFilePath.toString(), "-e", testEnvFilePath.toString(), "-o",
                Paths.get(targetTestDirectory, OutputFile).toString() };
        // normal case calling main directly
        ChartMap.main(b);
        assertTrue(Files.exists(Paths.get(targetTestDirectory, OutputFile)));
        // bad env filename to force main exception handling
        String[] c = new String[] { "-f", testInputFilePath.toString(), "-e", "nofilehere.yaml", "-o",
                Paths.get(targetTestDirectory, OutputFile).toString() };
        assertThrows(ChartMapException.class, () -> ChartMap.main(c));
        // test bad option
        String[] d = new String[] { "-B", testInputFilePath.toString(), "-e", testEnvFilePath.toString(), "-o",
                Paths.get(targetTestDirectory, OutputFile).toString() };
        assertThrows(ChartMapException.class, () -> ChartMap.main(d));
        // test two options
        String[] e = new String[] { "-f", "-a", "-u", "-c", testInputFilePath.toString(), "-e",
                testEnvFilePath.toString(), "-o", Paths.get(targetTestDirectory, OutputFile).toString() };
        try (ByteArrayOutputStream mainTestOut = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(mainTestOut));
            assertThrows(ChartMapException.class, () -> ChartMap.main(e));
            assertTrue(logContains(mainTestOut, "Parse Exception"));
            System.setOut(new PrintStream(initialOut));
        }
        // test missing option
        String[] f = new String[] { testInputFilePath.toString(), "-e", testEnvFilePath.toString(), "-o",
                Paths.get(targetTestDirectory, OutputFile).toString() };
        try (ByteArrayOutputStream mainTestOut = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(mainTestOut));
            ChartMap.main(f);
            assertTrue(logContains(mainTestOut, "Usage"));
            System.setOut(new PrintStream(initialOut));
        }
        // no args
        try (ByteArrayOutputStream mainTestOut = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(mainTestOut));
            ChartMap.main(new String[0]);
            assertTrue(logContains(mainTestOut, "Usage"));
            System.setOut(new PrintStream(initialOut));
        }
        // help
        try (ByteArrayOutputStream mainTestOut = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(mainTestOut));
            ChartMap.main(new String[] { "-h" });
            assertTrue(logContains(mainTestOut, "Usage"));
            System.setOut(new PrintStream(initialOut));
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void WeightedDeploymentTemplateTest() throws ChartMapException {
        ChartMap cm = createTestMap(ChartOption.FILENAME, testInputFilePath.toString(), testOutputTextFilePathNRNV,
            false, false, false, false);
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
     * Tests the protected method ChartMap.unpackTestChart
     * 
     * @throws ChartMapException
     * 
     * @throws IOException
     */
    @Test
    void unpackChartTest() throws ChartMapException, IOException {
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" starting"));
        // force IOException -> ChartMapException path
        ChartMap cm1 = createTestMap(ChartOption.FILENAME, testInputFilePath.toString(), testOutputTextFilePathNRNV,
                false, false, false, true);
        cm1.setChartName("foo");
        cm1.createTempDir();
        assertThrows(ChartMapException.class, () -> cm1.unpackChart("foo"));
        // force ChartMapException path when no temp dir
        ChartMap cm2 = createTestMap(ChartOption.FILENAME, testInputFilePath.toString(), testOutputTextFilePathNRNV,
                false, false, false, true);
        cm2.setChartName("foo");
        assertThrows(ChartMapException.class, () -> cm2.unpackChart("foo"));
        // force ChartMapException path when null chartmap passed
        cm2.createTempDir();
        assertThrows(ChartMapException.class, () -> cm2.unpackChart(null));
        // test when the tgz has no directory
        ChartMap cm3 = createTestMap(ChartOption.FILENAME, testInputFilePath.toString(), testOutputTextFilePathNRNV,
                false, false, false, true);
        cm3.setChartName(null);
        cm3.setChartVersion(null);
        cm3.createTempDir();
        try (ByteArrayOutputStream unpackCharttestOut = new ByteArrayOutputStream()) {
            System.setOut(new PrintStream(unpackCharttestOut));
            assertThrows(ChartMapException.class, () -> cm3.unpackChart(testOneFileZipPath.toString()));
            assertTrue(logContains(unpackCharttestOut, "Archive content does not appear to be valid"));
            System.setOut(initialOut);
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests some utility methods in ChartMap
     * 
     * @throws IOException
     * 
     * @throws ChartMapException
     */

    @Test
    void utilityMethodsTest() throws IOException, ChartMapException {
        Path d = Paths.get("./target");
        String b = ChartMap.getBaseName(d.toString());
        assertEquals(null, b);
        ChartMap cm = createTestMap(ChartOption.CHARTNAME, testChartName, testOutputChartNamePumlPath, true, false,
                false);
        cm.print();
        assertEquals(PrintFormat.PLANTUML, cm.getPrintFormat());
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @Test
    void pumlChartRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputPumlFilePathRV, true, true,
                true);
        testMap.print();
        assertTrue(Files.exists(testOutputPumlFilePathRV));
    }

    @Test
    void pumlChartNoRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputPumlFilePathNRV, true,
                false, true);
        testMap.print();
        assertTrue(Files.exists(testOutputPumlFilePathNRV));
    }

    @Test
    void pumlChartRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputPumlFilePathRNV, true, true,
                false);
        testMap.print();
        assertTrue(Files.exists(testOutputPumlFilePathRNV));
    }

    @Test
    void pumlChartNoRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputPumlFilePathNRNV, true,
                false, false);
        testMap.print();
        assertTrue(Files.exists(testOutputPumlFilePathNRNV));
        assertTrue(Files.exists(testOutputPngFilePathNRNV));
    }

    @Test
    void textChartRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputTextFilePathRV, true, true,
                true);
        testMap.print();
        assertTrue(Files.exists(testOutputTextFilePathRV));
        assertTrue(fileContains(testOutputTextFilePathRV,
                "WARNING: Chart alfresco-content-services:1.0.3 is stable but depends on alfresco-search:0.0.4 which may not be stable"));
    }

    @Test
    void textChartNoRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputTextFilePathNRV, true,
                false, true);
        testMap.print();
        assertTrue(Files.exists(testOutputTextFilePathNRV));
    }

    @Test
    void textChartRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputTextFilePathRNV, true, true,
                false);
        testMap.print();
        assertTrue(Files.exists(testOutputTextFilePathRNV));
    }

    @Test
    void textChartNoRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputTextFilePathNRNV, true,
                false, false);
        testMap.print();
        assertTrue(Files.exists(testOutputTextFilePathNRNV));
        // todo compare NR generated files with time stamp removed with a known good
        // result for a better test
    }

    @Test
    void JSONChartRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputJSONFilePathRV, true, true,
                true);
        testMap.print();
        assertTrue(Files.exists(testOutputJSONFilePathRV));

    }

    @Test
    void JSONChartNoRefreshVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputJSONFilePathNRV, true,
                false, true);
        testMap.print();
        assertTrue(Files.exists(testOutputJSONFilePathNRV));

    }

    @Test
    void JSONChartRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputJSONFilePathRNV, true, true,
                false);
        testMap.print();
        assertTrue(Files.exists(testOutputJSONFilePathRNV));
    }

    @Test
    void JSONChartNoRefreshNoVerboseTest() throws ChartMapException {
        ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputJSONFilePathNRNV, true,
                false, false);
        testMap.print();
        assertTrue(Files.exists(testOutputJSONFilePathNRNV));
    }

    @Test
    void APPRTest() throws ChartMapException { // test normal path
        ChartMap cm1 = createTestMap(ChartOption.APPRSPEC, testAPPRChart, testOutputAPPRPumlPath, true, false, false);
        cm1.print();
        assertTrue(Files.exists(testOutputAPPRPumlPath));
        assertTrue(Files.exists(testOutputAPPRPngPath)); // test null appr spec
        assertThrows(ChartMapException.class,
                () -> createTestMap(ChartOption.APPRSPEC, null, testOutputAPPRPumlPath, true, false, false)); // test
                                                                                                              // malformed
                                                                                                              // appr
                                                                                                              // specs
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
        ChartMap cm1 = createTestMap(ChartOption.URL, testChartUrl, testOutputChartUrlPumlPath, true, false, false);
        cm1.print();
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

    int createProcess(String[] a, String[] r, String o) throws IOException, InterruptedException {
        ProcessBuilder pb = new ProcessBuilder("java", "-Dos.name=".concat(o),
                "-javaagent:../../lib/org.jacoco.agent-0.8.7-runtime.jar=destfile=../jacoco.exec,append=true", "-cp",
                ".:../../resource/jar/helm-chartmap".concat(versionSuffix).concat(".jar"),
                "com.melahn.util.helm.ChartMap", "-f", "../../".concat(testInputFilePath.toString()), "-e",
                "../../".concat(testEnvFilePath.toString()), "-o", "cp-test.txt", "-v");
        Map<String, String> v = pb.environment();
        for (int i = 0; i < a.length; i = i + 2) {
            if (a[i] != null && a[i + 1] != null) {
                v.put(a[i], a[i + 1]);
            }
        }
        for (int i = 0; i < r.length; i++) {
            v.remove(r[i]);
        }
        // Capture fhe output in case its interesting for debugging
        pb.directory(new File(targetTestDirectory));
        File log = Paths.get(targetTestDirectory, "sub-process-out.txt").toFile();
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.appendTo(log));
        Process process = pb.start();
        process.waitFor(10, TimeUnit.SECONDS);
        return process.exitValue();
    }

    @Test
    void helpTest() {
        String helpTextExpected = "\nUsage:\n\n".concat("java -jar helm-chartmap-1.0.2.jar\n").concat("\nFlags:\n")
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

    private ChartMap createTestMap(ChartOption option, String input, Path outputPath, boolean generateImage,
            boolean refresh, boolean verbose, boolean... options) throws ChartMapException {
        // debug off makes it less noisy but be careful of any tests that depend on
        // debug entries
        boolean debug = (options.length > 0) ? options[0] : false;
        boolean[] switches = new boolean[] { generateImage, refresh, verbose, debug };
        ChartMap cm = new ChartMap(option, input, outputPath.toAbsolutePath().toString(),
                testEnvFilePath.toAbsolutePath().toString(), switches);
        cm.setLogLevel(); // set this explictly because some of the test cases may depend on a logger and don't call print
        cm.setHelmEnvironment(); // set this explictly so that test cases can test helm dependent methods without necessarily calling print
        return cm;
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
}