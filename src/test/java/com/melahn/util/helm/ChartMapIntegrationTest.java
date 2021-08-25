package com.melahn.util.helm;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import com.melahn.util.test.ChartMapTestUtil;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ChartMapIntegrationTest {

    private static final String TARGET_TEST_DIR_NAME = "target/integration-test";
    private static final Path TARGET_TEST_PATH = Paths.get(TARGET_TEST_DIR_NAME);

     /**
     * Cleans the target test directory and creates one anew.
     * 
     */
    @BeforeAll
    static void setUp() {
        try {
            ChartMapTestUtil.cleanDirectory(TARGET_TEST_PATH);
            Files.createDirectories(TARGET_TEST_PATH);
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests the shaded jar by running various test combinations of valid and invalid parameters.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void chartMapMainTest() throws InterruptedException, IOException  {
        final String className = "com.melahn.util.helm.ChartMap";
        final Path JaCocoAgentPath = Paths.get("", "lib/org.jacoco.agent-0.8.7-runtime").toAbsolutePath();
        final String JaCocoAgentString = JaCocoAgentPath.toString().concat(".jar=destfile=../jacoco.exec,append=true");
        final Path logFilePath = Paths.get(TARGET_TEST_DIR_NAME, "sub-process-out.txt");
        final String outputFileName = "testChartFileRV.txt";
        final Path outputFilePath = Paths.get(TARGET_TEST_DIR_NAME, "testChartFileRV.txt");
        final String testEnvFileName = "../../resource/example/example-env-spec.yaml";
        final String testInputFileName = "../../src/test/resource/test-chart-file.tgz";
        final String targetTestDirName = "target/integration-test";
        final Path targetTestPath = Paths.get(targetTestDirName);
        final ChartMapTestUtil utility = new ChartMapTestUtil();

        List<String> a = Arrays.asList("-f", testInputFileName, "-e", testEnvFileName, "-o", outputFileName);
        // normal case calling main directly
        utility.createProcess(a, new String[][] { new String[] {}, new String[] {} }, null, JaCocoAgentString,
                className, targetTestPath, logFilePath);
        assertTrue(Files.exists(outputFilePath));
        Files.delete(outputFilePath);
        // bad env filename to force main exception handling
        a = Arrays.asList("-f", testInputFileName, "-e", "nofilehere.yaml", "-o", outputFileName);
        utility.createProcess(a, new String[][] { new String[] {}, new String[] {} }, null, JaCocoAgentString,
                className, TARGET_TEST_PATH, logFilePath);
        assertNotEquals(0, utility.createProcess(a, new String[][] { new String[] {}, new String[] {} }, null,
                JaCocoAgentString, className, TARGET_TEST_PATH, logFilePath));
        assertFalse(Files.exists(outputFilePath));
        // test bad option
        List<String> a3 = Arrays.asList("-BADOPTION", "-f", testInputFileName, "-e", testEnvFileName, "-o",
                outputFileName);
        assertNotEquals(0, utility.createProcess(a3, new String[][] { new String[] {}, new String[] {} }, null,
                JaCocoAgentString, className, TARGET_TEST_PATH, logFilePath));
        assertFalse(Files.exists(outputFilePath));
        // test multiple options
        List<String> a4 = Arrays.asList("-f", "-a", "-u", "-c", testInputFileName, "-e", testEnvFileName, "-o",
                outputFileName);
        assertNotEquals(0, utility.createProcess(a4, new String[][] { new String[] {}, new String[] {} }, null,
                JaCocoAgentString, className, TARGET_TEST_PATH, logFilePath));
        assertTrue(ChartMapTestUtil.logContains(logFilePath,
                "ChartMapException:Parse Exception: Missing argument for option: f"));
        assertFalse(Files.exists(outputFilePath));
        // test missing option
        List<String> a5 = Arrays.asList(testInputFileName, "-e", testEnvFileName, "-o", outputFileName);
        utility.createProcess(a5, new String[][] { new String[] {}, new String[] {} }, null, JaCocoAgentString,
                className, TARGET_TEST_PATH, logFilePath);
        assertTrue(ChartMapTestUtil.logContains(logFilePath, "Usage:"));
        assertFalse(Files.exists(outputFilePath));
        // test no args
        utility.createProcess(Arrays.asList(), new String[][] { new String[] {}, new String[] {} }, null,
                JaCocoAgentString, className, TARGET_TEST_PATH, logFilePath);
        assertTrue(ChartMapTestUtil.logContains(logFilePath, "Usage:"));
        assertFalse(Files.exists(outputFilePath));
        // test help
        List<String> a6 = Arrays.asList("-h");
        utility.createProcess(a6, new String[][] { new String[] {}, new String[] {} }, null, JaCocoAgentString,
                className, TARGET_TEST_PATH, logFilePath);
        assertTrue(ChartMapTestUtil.logContains(logFilePath, "Usage:"));
        assertFalse(Files.exists(outputFilePath));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }
}