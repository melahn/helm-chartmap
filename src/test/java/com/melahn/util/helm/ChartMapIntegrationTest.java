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

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Tests the shaded jar by running various test combinations of valid and
 * invalid parameters.
 */
class ChartMapIntegrationTest {

    private List<String> args = null;
    private final String className = "com.melahn.util.helm.ChartMap";
    private final Path JaCocoAgentPath = Paths.get("", "lib/org.jacoco.agent-0.8.7-runtime").toAbsolutePath();
    private final String JaCocoAgentString = JaCocoAgentPath.toString()
            .concat(".jar=destfile=../jacoco.exec,append=true");
    private final Path logFilePath = Paths.get(TARGET_TEST_DIR_NAME, "sub-process-out.txt");
    private final String outputFileName = "testChartFileRV.txt";
    private final Path outputFilePath = Paths.get(TARGET_TEST_DIR_NAME, "testChartFileRV.txt");
    private final String testEnvFileName = "../../resource/example/example-env-spec.yaml";
    private final String testInputFileName = "../../src/test/resource/test-chart-file.tgz";
    private final String targetTestDirName = "target/integration-test";
    private final Path targetTestPath = Paths.get(targetTestDirName);
    private final ChartMapTestUtil utility = new ChartMapTestUtil();
    private static final String TARGET_TEST_DIR_NAME = "target/integration-test";
    private static final Path TARGET_TEST_PATH = Paths.get(TARGET_TEST_DIR_NAME);
    private static final String DIVIDER = "-------------------------------------";

    /**
     * Cleans the target test directory and creates one anew.
     * 
     */
    @BeforeAll
    static void setUp() {
        System.out.println(DIVIDER.concat(" INTEGRATION TESTS START ").concat(DIVIDER));
        try {
            ChartMapTestUtil.cleanDirectory(TARGET_TEST_PATH);
            Files.createDirectories(TARGET_TEST_PATH);
        } catch (Exception e) {
            fail("Test setup failed: " + e.getMessage());
        }
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    @AfterAll
    static void cleanUp() {
        System.out.println(DIVIDER.concat(" INTEGRATION TESTS END ").concat(DIVIDER));
    }

    /**
     * Tests the no error, normal case in the shaded jar.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void normalLocalTest() throws InterruptedException, IOException {
        args = Arrays.asList("-f", testInputFileName, "-e", testEnvFileName, "-o", outputFileName);
        utility.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null, JaCocoAgentString,
                className, targetTestPath, logFilePath);
        assertTrue(Files.exists(outputFilePath));
        Files.delete(outputFilePath);
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests with http download, normal case in the shaded jar.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void normalHttpTest() throws InterruptedException, IOException {
        args = Arrays.asList("-c", "wordpress:8.1.2", "-e", testEnvFileName, "-o", outputFileName);
        utility.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null, JaCocoAgentString,
                className, targetTestPath, logFilePath);
        assertTrue(Files.exists(outputFilePath));
        Files.delete(outputFilePath);
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests debug in the shaded jar.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void debugTest() throws InterruptedException, IOException {
        args = Arrays.asList("-f", testInputFileName, "-e", testEnvFileName, "-o", outputFileName, "-z");
        Files.deleteIfExists(logFilePath);
        utility.createProcess(args,
                new String[][] { new String[] { ChartMap.CHARTMAP_DEBUG_ENV_VAR, "Y" }, new String[] {} }, null,
                JaCocoAgentString, className, targetTestPath, logFilePath);
        assertTrue(Files.exists(logFilePath));
        assertTrue(Files.exists(outputFilePath));
        assertTrue(ChartMapTestUtil.fileContains(logFilePath, "was not removed because this is debug mode"));
        Files.delete(outputFilePath);
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests a bad environment file in the shaded jar.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void badEnvTest() throws InterruptedException, IOException {
        args = Arrays.asList("-f", testInputFileName, "-e", "nofilehere.yaml", "-o", outputFileName);
        utility.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null, JaCocoAgentString,
                className, TARGET_TEST_PATH, logFilePath);
        assertNotEquals(0, utility.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null,
                JaCocoAgentString, className, TARGET_TEST_PATH, logFilePath));
        assertFalse(Files.exists(outputFilePath));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests a bad option in the shaded jar.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void badOptionTest() throws InterruptedException, IOException {
        args = Arrays.asList("-BADOPTION", "-f", testInputFileName, "-e", testEnvFileName, "-o", outputFileName);
        assertNotEquals(0, utility.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null,
                JaCocoAgentString, className, TARGET_TEST_PATH, logFilePath));
        assertFalse(Files.exists(outputFilePath));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests multiple options in the shaded jar.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void multipleOptionTest() throws InterruptedException, IOException {
        args = Arrays.asList("-f", "-a", "-u", "-c", testInputFileName, "-e", testEnvFileName, "-o", outputFileName);
        assertNotEquals(0, utility.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null,
                JaCocoAgentString, className, TARGET_TEST_PATH, logFilePath));
        assertTrue(ChartMapTestUtil.fileContains(logFilePath,
                "ChartMapException: Parse Exception: Missing argument for option: f"));
        assertFalse(Files.exists(outputFilePath));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests missing option in the shaded jar.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void missingOptionTest() throws InterruptedException, IOException {
        args = Arrays.asList(testInputFileName, "-e", testEnvFileName, "-o", outputFileName);
        utility.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null, JaCocoAgentString,
                className, TARGET_TEST_PATH, logFilePath);
        assertTrue(ChartMapTestUtil.fileContains(logFilePath, "Usage:"));
        assertFalse(Files.exists(outputFilePath));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests no args in the shaded jar.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void missingNoArgsTest() throws InterruptedException, IOException {
        utility.createProcess(Arrays.asList(), new String[][] { new String[] {}, new String[] {} }, null,
                JaCocoAgentString, className, TARGET_TEST_PATH, logFilePath);
        assertTrue(ChartMapTestUtil.fileContains(logFilePath, "Usage:"));
        assertFalse(Files.exists(outputFilePath));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Tests help in the shaded jar.
     * 
     * @throws InterruptedException
     * @throws IOException
     */
    @Test
    void helpTest() throws InterruptedException, IOException {
        args = Arrays.asList("-h");
        utility.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null, JaCocoAgentString,
                className, TARGET_TEST_PATH, logFilePath);
        assertTrue(ChartMapTestUtil.fileContains(logFilePath, "Usage:"));
        assertFalse(Files.exists(outputFilePath));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /**
     * Test the ChartMap.getHelmCommand method in the shaded jar. This sets debug
     * mode.
     * 
     * @throws ChartMapException
     * @throws IOException
     */
    @Test
    void constructGetHelmCommandTest() throws ChartMapException, InterruptedException, IOException {
        // The helm command is found in HELM_BIN
        args = Arrays.asList("-f", testInputFileName, "-e", testEnvFileName, "-o", outputFileName);
        utility.createProcess(args, new String[][] {
                new String[] { ChartMap.CHARTMAP_DEBUG_ENV_VAR, "Y", "HELM_BIN", "helm" }, new String[] {} }, null,
                JaCocoAgentString, className, TARGET_TEST_PATH, logFilePath);
        assertTrue(ChartMapTestUtil.fileContains(logFilePath,
                String.format("The helm command \'%s\' will be used", "helm")));

        // The helm command is not found in HELM_BIN so the default "helm" is used. Note
        // the removal of HELM_BIN from the environment before starting the process
        utility.createProcess(args,
                new String[][] { new String[] { ChartMap.CHARTMAP_DEBUG_ENV_VAR, "Y" }, new String[] { "HELM_BIN" } },
                null, JaCocoAgentString, className, TARGET_TEST_PATH, logFilePath);
        assertTrue(
                ChartMapTestUtil.fileContains(logFilePath, String.format("The helm command \'%s\' will be used", "helm")));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }
}