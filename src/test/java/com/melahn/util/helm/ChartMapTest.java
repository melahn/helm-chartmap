package com.melahn.util.helm;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Stream;

import org.junit.Test;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class ChartMapTest {

    private static Path testOutputPumlFilePathRV = Paths.get("target/test/testChartFileRV.puml");
    private static Path testOutputPumlFilePathNRV = Paths.get("target/test/testChartFileNRV.puml");
    private static Path testOutputPumlFilePathRNV = Paths.get("target/test/testChartFileRNV.puml");
    private static Path testOutputPumlFilePathNRNV = Paths.get("target/test/testChartFileNRNV.puml");
    private static Path testOutputPngFilePathNRNV = Paths.get("target/test/testChartFileNRNV.png");
    private static Path testOutputTextFilePathRV = Paths.get("target/test/testChartFileRV.txt");
    private static Path testOutputTextFilePathNRV = Paths.get("target/test/testChartFileNRV.txt");
    private static Path testOutputTextFilePathRNV = Paths.get("target/test/testChartFileRNV.txt");
    private static Path testOutputTextFilePathNRNV = Paths.get("target/test/testChartFileNRNV.txt");
    private static Path testOutputImageRV = Paths.get("target/test/testChartFileRV.png");
    private static Path testOutputImageNRV = Paths.get("target/test/testChartFileNRV.png");
    private static Path testOutputImageRNV = Paths.get("target/test/testChartFileRNV.png");
    private static Path testOutputImageNRNV = Paths.get("target/test/testChartFileNRNV.png");
    private static Path testInputFilePath = Paths.get("src/test/resource/test-chart-file.tgz");
    private static Path testEnvFilePath = Paths.get("resource/example/example-env-spec.yaml");

    @AfterClass
    public static void cleanUp() {
        /**
         * No cleanup to do after test.  I don't delete the generated files
         * because they might be handy to have around to diagnose issues in
         * test failures.   They are deleted anyway when the test is next run.
         */
        System.out.println("Test complete.  Any generated file can be found in " +
                testOutputPumlFilePathRV.getParent().toAbsolutePath().toString());
    }

    @BeforeClass
    public static void setUp() {
        try {
            if (!Files.exists(testInputFilePath)) {
                throw new Exception("test Input File " + testInputFilePath.toAbsolutePath() + " does not exist");
            }
            deleteCreatedFiles();
            Files.createDirectories(testOutputPumlFilePathRV.getParent());
            assertNotNull(System.getenv("HELM_HOME"));
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
    }

    private static void deleteCreatedFiles() {
        try {
            System.out.println("Deleting any previously created files");
            Files.deleteIfExists(testOutputPumlFilePathRV);
            Files.deleteIfExists(testOutputPumlFilePathNRV);
            Files.deleteIfExists(testOutputPumlFilePathRNV);
            Files.deleteIfExists(testOutputPumlFilePathNRNV);
            Files.deleteIfExists(testOutputTextFilePathRV);
            Files.deleteIfExists(testOutputTextFilePathNRV);
            Files.deleteIfExists(testOutputTextFilePathRNV);
            Files.deleteIfExists(testOutputTextFilePathNRNV);
            Files.deleteIfExists(testOutputImageRV);
            Files.deleteIfExists(testOutputImageNRV);
            Files.deleteIfExists(testOutputImageRNV);
            Files.deleteIfExists(testOutputImageNRNV);
            //Files.deleteIfExists(testHelp);
        } catch (IOException e) {
            System.out.println("Error deleting created files: " + e.getMessage());
        }
    }

    @Test
    public void printTestPumlChartRefreshVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputPumlFilePathRV,
                    true, true, true);
            if (testMap != null) {
                testMap.print();
            }
            Assert.assertTrue(Files.exists(testOutputPumlFilePathRV));
        } catch (Exception e) {
            fail("printTestPumlChartRefreshVerbose failed:" + e.getMessage());
        }
    }

    @Test
    public void printTestPumlChartNoRefreshVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputPumlFilePathNRV,
                    true, false, true);
            if (testMap != null) {
                testMap.print();
            }
            Assert.assertTrue(Files.exists(testOutputPumlFilePathNRV));
        } catch (Exception e) {
            fail("printTestPumlChartNoRefreshVerbose failed:" + e.getMessage());
        }
    }

    @Test
    public void printTestPumlChartRefreshNoVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputPumlFilePathRNV,
                    true, true, false);
            if (testMap != null) {
                testMap.print();
            }
            Assert.assertTrue(Files.exists(testOutputPumlFilePathRNV));
        } catch (Exception e) {
            fail("printTestPumlChartRefreshNoVerbose failed:" + e.getMessage());
        }
    }

    @Test
    public void printTestPumlChartNoRefreshNoVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputPumlFilePathNRNV,
                    true, false, false);
            if (testMap != null) {
                testMap.print();
            }
            Assert.assertTrue(Files.exists(testOutputPumlFilePathNRNV));
            Assert.assertTrue(Files.exists(testOutputPngFilePathNRNV));
        } catch (Exception e) {
            fail("printTestPumlChartNoRefreshNoVerbose failed:" + e.getMessage());
        }
    }

    @Test
    public void printTestTextChartRefreshVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputTextFilePathRV,
                    true, true, true);
            if (testMap != null) {
                testMap.print();
            }
            Assert.assertTrue(Files.exists(testOutputTextFilePathRV));
            Assert.assertTrue(testStableWarning(testOutputTextFilePathRV));
        } catch (Exception e) {
            fail("printTestTextChartRefreshVerbose failed:" + e.getMessage());
        }
    }

    @Test
    public void printTestTextChartNoRefreshVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputTextFilePathNRV,
                    true, false, true);
            if (testMap != null) {
                testMap.print();
            }
            Assert.assertTrue(Files.exists(testOutputTextFilePathNRV));
        } catch (Exception e) {
            fail("printTestTextChartNoRefreshVerbose failed:" + e.getMessage());
        }
    }

    @Test
    public void printTestTextChartRefreshNoVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputTextFilePathRNV,
                    true, true, false);
            if (testMap != null) {
                testMap.print();
            }
            Assert.assertTrue(Files.exists(testOutputTextFilePathRNV));
        } catch (Exception e) {
            fail("printTestTextChartRefreshNoVerbose failed:" + e.getMessage());
        }
    }

    @Test
    public void printTestTextChartNoRefreshNoVerbose() {
        try {
            ChartMap testMap = createTestMap(ChartOption.FILENAME, testInputFilePath, testOutputTextFilePathNRNV,
                    true, false, false);
            if (testMap != null) {
                testMap.print();
            }
            Assert.assertTrue(Files.exists(testOutputTextFilePathNRNV));
            // todo compare NR generated files with time stamp removed with a known good result for a better test
        } catch (Exception e) {
            fail("printTestTextChartNRefreshNoVerbose failed:" + e.getMessage());
        }
    }

    @Test
    public void testHelp() {
        String helpTextExpected = "\nUsage:\n\n"
                .concat("java -jar helm-chartmap-1.0.2.jar\n")
                .concat("\nFlags:\n")
                .concat("\t-a\t<apprspec>\tA name and version of a chart as an appr specification\n")
                .concat("\t-c\t<chartname>\tA name and version of a chart\n")
                .concat("\t-f\t<filename>\tA location in the file system for a Helm Chart package (a tgz file)\n")
                .concat("\t-u\t<url>\t\tA url for a Helm Chart\n")
                .concat("\t-d\t<directoryname>\tThe file system location of HELM_HOME\n")
                .concat("\t-o\t<filename>\tA name and version of the chart as an appr specification\n")
                .concat("\t-e\t<filename>\tThe location of an Environment Specification\n")
                .concat("\t-g\t\t\tGenerate image from PlantUML file\n")
                .concat("\t-r\t\t\tRefresh\n")
                .concat("\t-v\t\t\tVerbose\n")
                .concat("\t-h\t\t\tHelp\n")
                .concat("\nSee https://github.com/melahn/helm-chartmap for more information\n");
        try {
            String helpText = ChartMap.getHelp();
            assert(helpText.equals(helpTextExpected));
        } catch (Exception e) {
        fail("testHelp failed:" + e.getMessage());
        }
    }

    private ChartMap createTestMap(ChartOption option, Path inputPath, Path outputPath,
                                   boolean generateImage, boolean refresh, boolean verbose) throws Exception {
        ChartMap testMap = null;
        try {
            testMap = new ChartMap(
                    option,
                    inputPath.toAbsolutePath().toString(),
                    outputPath.toAbsolutePath().toString(),
                    System.getenv("HELM_HOME"),
                    testEnvFilePath.toAbsolutePath().toString(),
                    generateImage,
                    refresh,
                    verbose);
        } catch (Exception e) {
            System.out.println("Exception createTestMap: " + e.getMessage());
        }
        return testMap;
    }

    boolean found = false;
    private boolean testStableWarning(Path p) {
        setFound(false);
        String expectedWarning = "WARNING: Chart alfresco-content-services:1.0.3 is stable but depends on alfresco-search:0.0.4 which may not be stable";
        try (Stream<String> lines = Files.lines( p.toAbsolutePath() ))
        {
            lines.forEach((String line) -> {
                if (line.contains(expectedWarning)) {
                    System.out.println("Expected warning found");
                    setFound(true);
                }
            });
        }
        catch (IOException e)
        {
            System.out.println("Exception: " + e.getMessage());
        }
        return getFound();
    }

    private void setFound (boolean f) {
        found = f;
    }
    private boolean getFound() {
        return found;
    }

}