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

    private static String CLASS_NAME = "com.melahn.util.helm.ChartMap";
    private static final String DIVIDER = "-------------------------------------";
    private static final Path JACOCO_AGENT_PATH = Paths.get("", "lib/org.jacoco.agent-0.8.8-runtime").toAbsolutePath();
    private static final String JACOCO_AGENT_STRING = JACOCO_AGENT_PATH.toString()
            .concat(".jar=destfile=../jacoco.exec,append=true");
    private static final ChartMapTestUtil UTILITY = new ChartMapTestUtil();
    private static final String TARGET_TEST_DIR_NAME = "target/integration-test";
    private static final Path TARGET_TEST_PATH = Paths.get(TARGET_TEST_DIR_NAME);
    private static final String TEST_ENV_FILE_NAME = "../../resource/example/example-env-spec.yaml";
    private static final String TEST_FAKE_CHART_FILE_NAME = "src/test/resource/test-fakechart.tgz";
    // use TEST_INPUT_FILE_NAME_1 for test cases where you must use the refresh flag since it contains subcharts that are not in any helm repo
    private static final String TEST_INPUT_FILE_NAME_1 = "../../src/test/resource/test-chart-file-1.tgz";
    // use TEST_INPUT_FILE_NAME_2 for test cases where you must not use the refresh flag since it contains old helm chart repo names
    private static final String TEST_INPUT_FILE_NAME_2 = "../../src/test/resource/test-chart-file-2.tgz";
    private static final Path LOG_FILE_PATH = Paths.get(TARGET_TEST_DIR_NAME, "sub-process-out.txt");
    private static final String OUTPUT_FILE_NAME = "testChartFile.puml";
    private static final Path OUTPUT_FILE_PATH = Paths.get(TARGET_TEST_DIR_NAME, "testChartFile.puml");

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
        args = Arrays.asList("-f", TEST_INPUT_FILE_NAME_1, "-e", TEST_ENV_FILE_NAME, "-o", OUTPUT_FILE_NAME, "-r");
        UTILITY.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null, JACOCO_AGENT_STRING,
                CLASS_NAME, TARGET_TEST_PATH, LOG_FILE_PATH);
        assertTrue(Files.exists(OUTPUT_FILE_PATH));
        Files.delete(OUTPUT_FILE_PATH);
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
        args = Arrays.asList("-c", "wordpress:8.1.2", "-e", TEST_ENV_FILE_NAME, "-o", OUTPUT_FILE_NAME);
        UTILITY.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null, JACOCO_AGENT_STRING,
                CLASS_NAME, TARGET_TEST_PATH, LOG_FILE_PATH);
        assertTrue(Files.exists(OUTPUT_FILE_PATH));
        Files.delete(OUTPUT_FILE_PATH);
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
        args = Arrays.asList("-f", TEST_INPUT_FILE_NAME_1, "-e", TEST_ENV_FILE_NAME, "-o", OUTPUT_FILE_NAME, "-z", "-r");
        Files.deleteIfExists(LOG_FILE_PATH);
        UTILITY.createProcess(args,
                new String[][] { new String[] { ChartMap.CHARTMAP_DEBUG_ENV_VAR, "Y" }, new String[] {} }, null,
                JACOCO_AGENT_STRING, CLASS_NAME, TARGET_TEST_PATH, LOG_FILE_PATH);
        assertTrue(Files.exists(LOG_FILE_PATH));
        assertTrue(Files.exists(OUTPUT_FILE_PATH));
        assertTrue(ChartMapTestUtil.fileContains(LOG_FILE_PATH, "was not removed because this is debug mode"));
        Files.delete(OUTPUT_FILE_PATH);
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
        Files.deleteIfExists(OUTPUT_FILE_PATH);
        args = Arrays.asList("-f", TEST_FAKE_CHART_FILE_NAME, "-e", "nofilehere.yaml", "-o", OUTPUT_FILE_NAME);
        UTILITY.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null, JACOCO_AGENT_STRING,
                CLASS_NAME, TARGET_TEST_PATH, LOG_FILE_PATH);
        assertNotEquals(0, UTILITY.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null,
                JACOCO_AGENT_STRING, CLASS_NAME, TARGET_TEST_PATH, LOG_FILE_PATH));
        assertFalse(Files.exists(OUTPUT_FILE_PATH));
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
        Files.deleteIfExists(OUTPUT_FILE_PATH);
        args = Arrays.asList("-BADOPTION", "-f", TEST_FAKE_CHART_FILE_NAME, "-e", TEST_ENV_FILE_NAME, "-o", OUTPUT_FILE_NAME);
        assertNotEquals(0, UTILITY.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null,
                JACOCO_AGENT_STRING, CLASS_NAME, TARGET_TEST_PATH, LOG_FILE_PATH));
        assertFalse(Files.exists(OUTPUT_FILE_PATH));
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
        Files.deleteIfExists(OUTPUT_FILE_PATH);
        args = Arrays.asList("-f", "-a", "-u", "-c", TEST_FAKE_CHART_FILE_NAME, "-e", TEST_ENV_FILE_NAME, "-o", OUTPUT_FILE_NAME);
        assertNotEquals(0, UTILITY.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null,
                JACOCO_AGENT_STRING, CLASS_NAME, TARGET_TEST_PATH, LOG_FILE_PATH));
        assertTrue(ChartMapTestUtil.fileContains(LOG_FILE_PATH,
                "ChartMapException: Parse Exception: Missing argument for option: f"));
        assertFalse(Files.exists(OUTPUT_FILE_PATH));
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
        args = Arrays.asList(TEST_INPUT_FILE_NAME_2, "-e", TEST_FAKE_CHART_FILE_NAME, "-o", OUTPUT_FILE_NAME);
        UTILITY.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null, JACOCO_AGENT_STRING,
                CLASS_NAME, TARGET_TEST_PATH, LOG_FILE_PATH);
        assertTrue(ChartMapTestUtil.fileContains(LOG_FILE_PATH, "Usage:"));
        assertFalse(Files.exists(OUTPUT_FILE_PATH));
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
        UTILITY.createProcess(Arrays.asList(), new String[][] { new String[] {}, new String[] {} }, null,
                JACOCO_AGENT_STRING, CLASS_NAME, TARGET_TEST_PATH, LOG_FILE_PATH);
        assertTrue(ChartMapTestUtil.fileContains(LOG_FILE_PATH, "Usage:"));
        assertFalse(Files.exists(OUTPUT_FILE_PATH));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }

    /** 
     * Tests different combinations of PLANTUML_LIMIT_SIZE.
     * 
     * See the unit test function for the other case which relies
     * on a system property being set.
     * 
     * @throws ChartMapException
     */
    @Test
    void plantUMLLimitSizeTest() throws ChartMapException, IOException, InterruptedException {
        // Test the normal case, PLANTUML_LIMIT_SIZE not defined at all
        args = Arrays.asList("-f", TEST_INPUT_FILE_NAME_2, "-e", TEST_ENV_FILE_NAME, "-o", OUTPUT_FILE_NAME, "-g", "-v");
        UTILITY.createProcess(args, new String[][] { new String[] {}, new String[] {"PLANTUML_LIMIT_SIZE"} }, null, JACOCO_AGENT_STRING,
                CLASS_NAME, TARGET_TEST_PATH, LOG_FILE_PATH);
        assertTrue(ChartMapTestUtil.fileContains(LOG_FILE_PATH,"PLANTUML_LIMIT_SIZE set to 8192"));
        assertTrue(Files.exists(OUTPUT_FILE_PATH));
        // Test env var PLANTUML_LIMIT_SIZE set to 8000
        String e = "8000";
        UTILITY.createProcess(args, new String[][] { new String[] {"PLANTUML_LIMIT_SIZE", e}, new String[] {} }, null, JACOCO_AGENT_STRING,
                CLASS_NAME, TARGET_TEST_PATH, LOG_FILE_PATH);
        assertTrue(ChartMapTestUtil.fileContains(LOG_FILE_PATH, String.format("PLANTUML_LIMIT_SIZE was already set to %s", e)));
        assertTrue(Files.exists(OUTPUT_FILE_PATH));
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
        UTILITY.createProcess(args, new String[][] { new String[] {}, new String[] {} }, null, JACOCO_AGENT_STRING,
                CLASS_NAME, TARGET_TEST_PATH, LOG_FILE_PATH);
        assertTrue(ChartMapTestUtil.fileContains(LOG_FILE_PATH, "Usage:"));
        assertFalse(Files.exists(OUTPUT_FILE_PATH));
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
        args = Arrays.asList("-f", TEST_INPUT_FILE_NAME_2, "-e", TEST_ENV_FILE_NAME, "-o", OUTPUT_FILE_NAME);
        UTILITY.createProcess(args, new String[][] {
                new String[] { ChartMap.CHARTMAP_DEBUG_ENV_VAR, "Y", "HELM_BIN", "helm" }, new String[] {} }, null,
                JACOCO_AGENT_STRING, CLASS_NAME, TARGET_TEST_PATH, LOG_FILE_PATH);
        assertTrue(ChartMapTestUtil.fileContains(LOG_FILE_PATH,
                String.format("The helm command \'%s\' will be used", "helm")));

        // The helm command is not found in HELM_BIN so the default "helm" is used. Note
        // the removal of HELM_BIN from the environment before starting the process
        UTILITY.createProcess(args,
                new String[][] { new String[] { ChartMap.CHARTMAP_DEBUG_ENV_VAR, "Y" }, new String[] { "HELM_BIN" } },
                null, JACOCO_AGENT_STRING, CLASS_NAME, TARGET_TEST_PATH, LOG_FILE_PATH);
        assertTrue(
                ChartMapTestUtil.fileContains(LOG_FILE_PATH, String.format("The helm command \'%s\' will be used", "helm")));
        System.out.println(new Throwable().getStackTrace()[0].getMethodName().concat(" completed"));
    }
}