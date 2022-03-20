
package com.melahn.util.helm;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.melahn.util.extract.ArchiveExtract;
import com.melahn.util.helm.model.EnvironmentSpecification;
import com.melahn.util.helm.model.HelmChart;
import com.melahn.util.helm.model.HelmChartLocalCache;
import com.melahn.util.helm.model.HelmChartRepoLocal;
import com.melahn.util.helm.model.HelmChartReposLocal;
import com.melahn.util.helm.model.HelmDeploymentContainer;
import com.melahn.util.helm.model.HelmDeploymentTemplate;
import com.melahn.util.helm.model.HelmRequirement;
import com.melahn.util.helm.model.HelmRequirements;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.collections4.MapIterator;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.Yaml;

public class ChartMap {
    private String apprSpec;
    private HelmChart chart;
    private String chartFilename = null;
    private ChartKeyMap charts = new ChartKeyMap();
    private String chartName = null;
    private String chartVersion;
    private String chartUrl = null;
    HashSet<String> chartsDependenciesPrinted = new HashSet<>();
    protected ChartKeyMap chartsReferenced = new ChartKeyMap();
    private boolean debug = false;
    protected HashMap<String, WeightedDeploymentTemplate> deploymentTemplatesReferenced = new HashMap<>();
    private String envFilename = null;
    HashSet<String> env = new HashSet<>();
    private boolean generateImage = false;
    private String helmCommand;
    private String helmCachePath;
    private String helmConfigPath;
    protected HashSet<String> imagesReferenced = new HashSet<>();
    private HelmChartReposLocal localRepos;
    private String chartMapDebug = CHARTMAP_DEBUG_ENV_VAR;
    private String chartMapVerbose = "CHARTMAP_VERBOSE";
    protected Logger logger;
    private Level logLevelDebug;
    private Level logLevelVerbose;
    private String outputFilename = DEFAULT_OUTPUT_FILENAME;
    private PrintFormat printFormat = PrintFormat.TEXT;
    private IChartMapPrinter printer;
    private boolean refreshLocalRepo = false;
    private String tempDirName = null;
    private boolean verbose = false;

    private static final String DEFAULT_OUTPUT_FILENAME = "chartmap.text";
    private static final int GENERATE_SWITCH = 0;
    private static final String LOG_FORMAT_2 = "{}{}";
    private static final String LOG_FORMAT_3 = "{}{}{}";
    private static final String LOG_FORMAT_4 = "{}{}{}{}";
    private static final String LOG_FORMAT_9 = "{}{}{}{}{}{}{}{}{}";
    private static final int REFRESH_SWITCH = 1;
    private static final String TEMP_DIR = "Temporary Directory ";
    private static final int VERBOSE_SWITCH = 2;

    protected static final String APPDATA = "APPDATA";
    protected static final String CHARTMAP_DEBUG_ENV_VAR = "CHARTMAP_DEBUG";
    protected static final String CHART_YAML = "Chart.yaml";
    protected static final String CHARTS_DIR_NAME = "charts";
    protected static final String CHECK_OS_MSG = " %s is null. Check your OS installation.";
    protected static final String HELM_SUBDIR = "/helm";
    protected static final String HOME = "HOME";
    protected static final String INTERRUPTED_EXCEPTION = "InterruptedException pulling chart from appr using specification %s : %s";
    protected static final int PROCESS_TIMEOUT = 100000; 
    protected static final String RENDERED_TEMPLATE_FILE = "_renderedtemplates.yaml"; 
    protected static final String START_OF_TEMPLATE = "# Source: ";
    protected static final String TEMP = "TEMP";
    protected static final String TEMP_DIR_ERROR = "IOException creating temp directory";
    protected static final int MAX_WEIGHT = 100;

    /**
     * This inner class is used to assign a 'weight' to a template based on its
     * position in the file system (parent templates having the lower weight). A
     * template of the lowest weight is used to determine which containers will be
     * referenced.
     */
    protected class WeightedDeploymentTemplate {
        private int weight;
        private HelmDeploymentTemplate template;

        WeightedDeploymentTemplate(String fileName, HelmDeploymentTemplate t) {
            weight = (fileName != null) ? fileName.split(File.separator).length : MAX_WEIGHT;
            template = t;
        }

        protected int getWeight() {
            return weight;
        }

        protected void setTemplate(HelmDeploymentTemplate t) {
            template = t;
        }

        protected HelmDeploymentTemplate getTemplate() {
            return template;
        }
    }

    /**
     * Parses the command line and generates a Chart Map file
     *
     * @param arg The command line args
     * @throws ChartMapException when an error occurs
     */
    public static void main(String[] arg) throws ChartMapException {
        ChartMap chartMap = new ChartMap();
         try {
            if (chartMap.parseArgs(arg)) {
                chartMap.print();
            }
        } catch (ChartMapException e) {
            chartMap.logger.error("ChartMapException: ".concat(e.getMessage()));
            throw e;
        }
    }

    /**
     * Constructor
     *
     * @param option         The format of the Helm Chart
     * @param chart          The name of the Helm Chart in one of the formats
     *                       specified by the option parameter
     * @param outputFilename The name of the file to which to write the generated
     *                       Chart Map. Note the file is overwritten if it exists
     * @param envFilename    The name of a yaml file that contains a set of
     *                       environment variables which may influence the way the
     *                       charts are rendered by helm.
     * @param switches       An array containing a list of boolean values as follows
     *                       when true switches[0] generates an image from the
     *                       PlantUML file (if any) switches[1] refresh the local
     *                       Helm repo switches[2] provides a little more
     *                       information as the Chart Map is generated 
     * @throws ChartMapException when an error occurs creating the chart map
     **/

    public ChartMap(ChartOption option, String chart, String outputFilename, String envFilename, boolean[] switches)
            throws ChartMapException {
        setDebugLogLevel();
        ArrayList<String> args = new ArrayList<>();
        addOptionsToArgs(args, option);
        args.add(chart);
        if (envFilename != null) {
            args.add("-e");
            args.add(envFilename);
        }
        args.add("-o");
        args.add(outputFilename);
        for (String a : args) {
            if (a == null) {
                throw new ChartMapException("Null parameter");
            }
        }
        addSwitchesToArgs(args, switches);
        parseArgs(args.toArray(new String[args.size()]));
    }

     /**
     * Default constructor.
     * 
     * Just sets the debug log level.
     */
    public ChartMap() {
        setDebugLogLevel();
    }

    /**
     * Sets the value of the chart option in the commandline array.
     * 
     * @param a the array list
     * @param o the chart option (e.g. APPRSPEC)
     * @throws ChartMapException if a null is passed as the option
     */
    private void addOptionsToArgs(ArrayList<String> a, ChartOption o) throws ChartMapException {

        if (o == null) {
            throw new ChartMapException("Invalid Option Specification");
        } else if (o == ChartOption.APPRSPEC) {
            a.add("-a");
        } else if (o == ChartOption.CHARTNAME) {
            a.add("-c");
        } else if (o == ChartOption.FILENAME) {
            a.add("-f");
        } else {
            a.add("-u");
        }
    }

    /**
     * Parses the switches array and adds them to the command line args.
     * 
     * @param args
     * @param switches
     */
    private void addSwitchesToArgs(ArrayList<String> a, boolean[] s) throws ChartMapException {
        if (s.length != 3) {
            throw new ChartMapException("Switches are invalid. There should be three of them.");
        }
        if (s[GENERATE_SWITCH]) {
            a.add("-g");
        }
        if (s[REFRESH_SWITCH]) {
            a.add("-r");
        }
        if (s[VERBOSE_SWITCH]) {
            a.add("-v");
        }
    }

    /**
     * Prints the Chart Map by creating a temp directory, loading the local repo
     * with charts, resolving the dependencies of the selected chart, printing the
     * Chart Map, then cleans up
     *
     * @throws ChartMapException if an error occurs during print
     */
    public void print() throws ChartMapException {
        setVerboseLogLevel();
        setHelmEnvironment();
        createTempDir();
        loadLocalRepos();
        resolveChartDependencies();
        printMap();
        removeTempDir();
    }

    /**
     * Parse the command line args.
     * 
     * At this point we haven't creqted the logger yet because we haven't parsed the
     * commmand line to get the verbose and debug flags which later would have
     * affected what gets logged se we use a logger just for help and error handling
     * when needed.
     *
     * @param args command line args
     * @return boolean true if processing should continue, false otherwise
     * @throws ChartMapException should a parse error occur
     */
    protected boolean parseArgs(String[] args) throws ChartMapException {
        Options options = setOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cmd = parser.parse(options, args);
            int count = parseOptions(cmd);
            parseSwitches(cmd);
            if (args.length == 0 || cmd.hasOption("h") || count == 0) {
                LogManager.getLogger().info(ChartMap.getHelp());
                return false;
            }
            return true;
        } catch (ParseException e) {
            LogManager.getLogger().error(e.getMessage());
            throw new ChartMapException(String.format("Parse Exception: %s", e.getMessage()));
        }
    }

    /**
     * Sets up the options we will use for parsing
     * 
     * @return the Options created
     */
    private Options setOptions() {
        Options options = new Options();
        options.addOption("a", true, "The appr chart location");
        options.addOption("c", true, "The Chart Name");
        options.addOption("e", true, "Environment Variable Filename");
        options.addOption("f", true, "Helm Chart File Name");
        options.addOption("g", false, "Generate Image from PlantUML file");
        options.addOption("h", false, "Help");
        options.addOption("o", true, "The Output Filename");
        options.addOption("r", false, "Update the Helm Chart dependencies");
        options.addOption("u", true, "The Url of the Helm Chart ");
        options.addOption("v", false, "Verbose");
        options.addOption("z", false, "Debug Mode");
        return options;
    }

    /**
     * Parse the options from the command line
     * 
     * @param cmd the command line
     * @return a count of the options found
     * @throws ChartMapException if a parsing error occurs
     */
    private int parseOptions(CommandLine cmd) throws ChartMapException {
        int count = 0; // note these are exclusive options
        if (cmd.hasOption("a")) { // e.g. quay.io/melahn/helm-chartmap-test-chart@1.0.0
            parseApprSpec(cmd.getOptionValue("a"));
            count++;
        }
        if (cmd.hasOption("c")) { // e.g. nginx:9.3.0
            parseChartName(cmd.getOptionValue("c"));
            count++;
        }
        if (cmd.hasOption("e")) {
            setEnvFilename(cmd.getOptionValue("e"));
        }
        if (cmd.hasOption("f")) { // e.g. ./src/test/resource/test-chart-file.tgz
            setChartFilename(cmd.getOptionValue("f"));
            count++;
        }
        if (cmd.hasOption("o")) {
            setOutputFilename(cmd.getOptionValue("o"));
        }
        if (cmd.hasOption("u")) { // e.g.
                                  // https://github.com/melahn/helm-chartmap/raw/master/src/test/resource/test-chart-file.tgz
            setChartUrl(cmd.getOptionValue("u"));
            count++;
        }
        return count;
    }

    /**
     * Parse the switches from the command line
     * 
     * @param cmd the command line
     */
    private void parseSwitches(CommandLine cmd) {
        if (cmd.hasOption("g")) {
            setGenerateImage(true);
        }
        if (cmd.hasOption("r")) {
            setRefreshLocalRepo(true);
        }
        if (cmd.hasOption("v")) {
            setVerbose(true);
        }
        if (cmd.hasOption("z")) {
            setDebug(true);
        }
    }

    /**
     * If the user has specified the debug flag, set the log level so it has a
     * higher priority (ie. a lower level value) than the logger configured in
     * log4j2.xml, which is INFO (level 400). Otherwise set it to a higher level
     * number so debug log entries will be ignored.
     * 
     * Note that log4j will ignore the integer values if the level already exists so
     * there is no point in calling this method twice or initializing the Levels
     * when they are declared.
     * 
     * The reason for the timestamped value for the levels is that they are static
     * in Log4j2 and this can cause issues across multiple usages of ChartMap if a
     * common name is used with some pretty hard to debug problems. This can be seen
     * for example in Junit tests when different debug and verbose levels are used
     * in different tests.
     */
    protected void setDebugLogLevel() {
        String t = String.valueOf(System.currentTimeMillis());
        chartMapDebug = chartMapDebug.concat(t);
        logger = LogManager.getLogger(t);
        if (System.getenv(CHARTMAP_DEBUG_ENV_VAR) != null) {
            setDebug(true);
            logger.info("Debug Mode is ON");
            logLevelDebug = Level.forName(chartMapDebug, 350); // higher priority than INFO
        } else {
            logLevelDebug = Level.forName(chartMapDebug, 450); // lower priority than INFO
        }
    }

    /**
     * If the user has specified the verbose flag, set the log level so it has a
     * higher priority (ie. a lower level value) than the logger configured in
     * log4j2.xml, which is INFO (level 400). Otherwise set it to a higher level
     * number so verbose log entries will be ignored.
     * 
     * Note that log4j will ignore the integer values if the level already exists so
     * there is no point in calling this method twice or initializing the Levels
     * when they are declared.
     * 
     * The reason for the timestamped value for the levels is that they are static
     * in Log4j2 and this can cause issues across multiple usages of ChartMap if a
     * common name is used with some pretty hard to debug problems. This can be seen
     * for example in Junit tests when different debug and verbose levels are used
     * in different tests.
     */
    protected void setVerboseLogLevel() {
        String t = String.valueOf(System.currentTimeMillis());
        chartMapVerbose = chartMapVerbose.concat(t);
        if (isVerbose()) {
            logLevelVerbose = Level.forName(chartMapVerbose, 350); // higher priority than INFO
        } else {
            logLevelVerbose = Level.forName(chartMapVerbose, 450); // lower priority than INFO
        }
    }

    /**
     * Parses an App Registry (APPR) Specification of the expected format
     * <chart-repp>/<org>/<chart-name>@<chart-version> and sets the values chartName
     * and chartVersion
     * 
     * Note: I base chart name part of the regular expression on the Chart name
     * rules described in https://helm.sh/docs/chart_best_practices/conventions/ I
     * don't enforce semver in the version portion
     *
     * @param a an APPR specification
     * @throws ChartMapException if the APPR specification was malformed
     */
    private void parseApprSpec(String a) throws ChartMapException {
        if (!a.matches("[a-z][.a-z0-9]+/[-a-z0-9]+/[-a-z0-9]+@[._-a-zA-Z0-9]+")) {
            throw new ChartMapException("App Registry specification invalid: " + a
                    + ". I was expecting something like quay.io/melahn/helm-chartmap-test-chart@1.0.0");
        }
        String[] apprSpecParts = a.split("@");
        setChartName(apprSpecParts[0].substring(apprSpecParts[0].lastIndexOf('/') + 1, apprSpecParts[0].length()));
        setChartVersion(apprSpecParts[1]);
        apprSpec = a;
    }

    /**
     * Parses a Chart Name of the format <chart-name><chart version> and sets the
     * values of chartName and chartVersion
     * 
     * Note: I base the regular expression on the Chart name rules described in
     * https://helm.sh/docs/chart_best_practices/conventions/ I don't enforce semver
     * in the version portion
     *
     * @param c the Chart Name
     * @throws ChartMapException if the Chart Name was malformed
     */
    private void parseChartName(String c) throws ChartMapException {
        if (!c.matches("[a-z][-a-z0-9]+:[._-a-zA-Z0-9]+")) {
            throw new ChartMapException(
                    "Chart Name invalid: " + c + ". I was expecting something like helm-test-chart:1.0.2");
        }
        String[] chartNameParts = c.split(":");
        setChartName(chartNameParts[0]);
        setChartVersion(chartNameParts[1]);
    }

    /**
     * Prints some help.
     *
     * @return a string containing some help
     */
    public static String getHelp() {
        return "\nUsage:\n\n".concat("java -jar helm-chartmap-1.0.3.jar\n").concat("\nFlags:\n")
                .concat("\t-a\t<apprspec>\tA name and version of a chart as an appr specification\n")
                .concat("\t-c\t<chartname>\tA name and version of a chart\n")
                .concat("\t-f\t<filename>\tA location in the file system for a Helm Chart package (a tgz file)\n")
                .concat("\t-u\t<url>\t\tA url for a Helm Chart\n")
                .concat("\t-o\t<filename>\tA name and version of the chart as an appr specification\n")
                .concat("\t-e\t<filename>\tThe location of an Environment Specification\n")
                .concat("\t-g\t\t\tGenerate image from PlantUML file\n").concat("\t-r\t\t\tRefresh\n")
                .concat("\t-v\t\t\tVerbose\n").concat("\t-h\t\t\tHelp\n")
                .concat("\nSee https://github.com/melahn/helm-chartmap for more information\n");
    }

    /**
     * Finds all the local repositories the user has previously added (using for
     * example 'helm repo add') and constructs a HelmChartReposLocal from them. Then
     * calls a method to load all the charts found in that cache into the charts map
     * where they are raw material for use later.
     * 
     * @throws ChartMapException when an error occurs loading repos
     */
    protected void loadLocalRepos() throws ChartMapException {
        try {
            // in v2 all the repos were nicely collected into a single yaml file in helm
            // home but in v3 the location of the repo list is now os dependent
            logger.info("getHelmConfigPath = {}",getHelmConfigPath());
            String helmRepoFilename = getHelmConfigPath().concat("/repositories.yaml");
            ObjectMapper mapper = getObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            File reposYamlFile = new File(helmRepoFilename);
            localRepos = mapper.readValue(reposYamlFile, HelmChartReposLocal.class);
            // in helm v2, the cache location was set but in v3, it must be synthesized from
            // an OS specific location
            HelmChartRepoLocal[] repos = localRepos.getRepositories();
            String cacheDirname = getHelmCachePath().concat("/repository/");
            for (HelmChartRepoLocal r : repos) {
                r.setCache(cacheDirname.concat(r.getName()).concat("-index.yaml"));
            }
            printLocalRepos();
            loadLocalCharts();
        } catch (IOException e) {
            throw new ChartMapException(String.format("Exception found loading local repos: %s.", e.getMessage()));
        }
    }

    /**
     * 
     * This method was introduced to allow providing a test version of an
     * ObjectMapper to allow testing of exception conditions.
     * 
     * @return an ObjectMapper
     */
    public ObjectMapper getObjectMapper() {
        return new ObjectMapper(new YAMLFactory());
    }

    /**
     * Gets the major version of the helm client and sets helmMajorVersionUsed.
     * 
     * The helm version command offers templated output using go template syntax but
     * the values were not designed to be forward or backward compatible (!) hence
     * the tortured logic here
     * 
     * @throws ChartMapException if a version other than V3 is found
     */
    protected void checkHelmVersion() throws ChartMapException {
        logger.log(logLevelDebug, "+ Entering checkHelmVersion");
        String[] c = { getHelmCommand(), "version", "--template", "{{ .Version }}" };
        try {
            Process p = getProcess(c, null);
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            p.waitFor(PROCESS_TIMEOUT, TimeUnit.MILLISECONDS);
            int exitValue = p.exitValue();
            if (exitValue == 0) {
                String o = br.readLine();
                if (o != null && o.length() > 1 && o.charAt(1) == '3') {
                    logger.log(logLevelDebug, "Helm Version 3 detected");
                    logger.log(logLevelDebug, "+ Exiting checkHelmVersion");
                    return;
                }
                throw new ChartMapException(
                        "Unsupported Helm Version. Please upgrade to helm V3 or use a previous version of ChartMap.");
            } else { // we could not even execute the helm command
                throw new ChartMapException("Error Code: " + exitValue + " executing command " + c[0]
                        + c[1] + c[2] + c[3]);
            }
        } catch (IOException e) {
            // we could not get the output of the helm command
            throw new ChartMapException(
                    String.format("Exception trying to discover Helm Version: %s ", e.getMessage()));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChartMapException(String.format(INTERRUPTED_EXCEPTION, apprSpec, e.getMessage()));
        }
    }

    /**
     * 
     * This method was introduced to allow providing a test version of a Process so
     * as to return non zero exit codes for testing.
     * 
     * @param c the command and its parameters
     * @param d the working directory
     * @return a Process
     * @throws IOException if an error occurs getting the process
     */
    public Process getProcess(String[] c, File d) throws IOException {
        return d!=null?Runtime.getRuntime().exec(c, null, d):Runtime.getRuntime().exec(c, null);
    }

    /**
     * Sets the helm information, include the helm command, version and paths.
     * 
     * @throws ChartMapException if any of the helm information cannot be set
     */
    void setHelmEnvironment() throws ChartMapException {
        helmCommand = getHelmCommand();
        checkHelmVersion();
        getHelmPaths();
    }

    /**
     * Finds the helm cache and configuration paths.
     * 
     * The logic for finding the paths is derived from the rules explained in
     * https://helm.sh/docs/helm/helm/
     * 
     * @throws ChartMapException if either the cache or config directorie could not
     *                           be found
     */
    protected void getHelmPaths() throws ChartMapException {
        ChartUtil.OSType os = ChartUtil.getOSType();
        logger.log(logLevelVerbose, "detected Operating system was {}", os);
        constructHelmCachePath(os);
        constructHelmConfigPath(os);
    }

    
    /** 
     * Constructs the helm cache path following the rules defined by helm.
     * 
     * @param os the type of operating system
     * @throws ChartMapException if a valid path could not be constructed
     */
    protected void constructHelmCachePath(ChartUtil.OSType os) throws ChartMapException {
        String m = null;
        setHelmCachePath(getEnv("HELM_CACHE_HOME") != null ? getEnv("HELM_CACHE_HOME") : getEnv("XDG_CACHE_HOME"));
        // When no other location is set, use a default location based on the operating
        // system
        if (getHelmCachePath() == null && os == ChartUtil.OSType.MACOS) {
            if (getEnv(HOME) == null) {
                m = String.format(ChartMap.CHECK_OS_MSG, ChartMap.HOME);
                logger.error(m);
                throw new ChartMapException(m);
            }
            setHelmCachePath(getEnv(HOME).concat("/Library/Caches/helm"));
        }
        if (getHelmCachePath() == null && os == ChartUtil.OSType.LINUX) {
            if (getEnv(HOME) == null) {
                m = String.format(ChartMap.CHECK_OS_MSG, ChartMap.HOME);
                logger.error(m);
                throw new ChartMapException(m);
            }
            setHelmCachePath(getEnv(HOME).concat("/.cache/helm"));
        }
        if (getHelmCachePath() == null && os == ChartUtil.OSType.WINDOWS) {
            if (getEnv(TEMP) == null) {
                m = String.format(ChartMap.CHECK_OS_MSG, ChartMap.TEMP);
                logger.error(m);
                throw new ChartMapException(m);
            }
            setHelmCachePath(getEnv(TEMP).concat(HELM_SUBDIR));
        }
        if (getHelmCachePath() == null) { // None of the above
            m = "Could not locate the helm cache path. Check that your installation of helm is complete and that you are using a supported OS.";
            logger.error(m);
            throw new ChartMapException(m);
        }
    }

     /** 
     * Constructs the helm config path following the rules defined by helm.
     * 
     * @param os the type of operating system
     * @throws ChartMapException if a valid path could not be constructed
     */
    protected void constructHelmConfigPath(ChartUtil.OSType os) throws ChartMapException {
        String m = null;
        setHelmConfigPath(getEnv("HELM_CONFIG_HOME") != null ? getEnv("HELM_CONFIG_HOME")
                : getEnv("XDG_CONFIG_HOME"));
        // When no other location is set, use a default location based on the operating
        // system
        if (getHelmConfigPath() == null && os == ChartUtil.OSType.MACOS) {
            if (getEnv(HOME) == null) {
                m = String.format(ChartMap.CHECK_OS_MSG, ChartMap.HOME);
                logger.error(m);
                throw new ChartMapException(m);
            }
            setHelmConfigPath(getEnv(HOME).concat("/Library/Preferences/helm"));
        }
        if (getHelmConfigPath() == null && os == ChartUtil.OSType.LINUX) {
            if (getEnv(HOME) == null) {
                m = String.format(ChartMap.CHECK_OS_MSG, ChartMap.HOME);
                logger.error(m);
                throw new ChartMapException(m);
            }
            setHelmConfigPath(getEnv("HOME").concat("/.config/helm"));
        }
        if (getHelmConfigPath() == null && os == ChartUtil.OSType.WINDOWS) {
            if (getEnv(APPDATA) == null) {
                m = String.format(ChartMap.CHECK_OS_MSG, ChartMap.APPDATA);
                logger.error(m);
                throw new ChartMapException(m);
            }
            setHelmConfigPath(getEnv(APPDATA).concat(HELM_SUBDIR));
        }
        if (getHelmConfigPath() == null) { // None of the above
            m = "Could not locate the helm config path. Check that your installation of helm is complete and that you are using a supported OS.";
            logger.error(m);
            throw new ChartMapException(m);
        }
    }

    /**
     * Gets the environment variable. Using my own function for this allows testing
     * of different environment variable values using mocks and spies.  
     * 
     * @param e The name of the variable to fetch
     * @return The value of the variable or null
     */
    protected String getEnv(String e) {
        String v = System.getenv(e);
        logger.log(logLevelDebug, "Environment Variable {} = {}", e, v);
        return v;
    }

    /**
     * Return the helm command to use, giving priority to the value of HELM_BIN if
     * set explictly. Setting HELM_BIN explicitly is arguably more secure since the
     * user does not need to worry then about some evil helm command in the PATH,
     * though it does then rely on the value of HELM_BIN itself being secure.
     * 
     * @return the helm command
     */
    protected String getHelmCommand() {
        String helmBin = getEnv("HELM_BIN");
        logger.log(logLevelDebug, "HELM_BIN = {}", helmBin);
        String helmCommandResolved = helmBin == null ? "helm" : helmBin;
        logger.log(logLevelDebug, "The helm command \'{}\' will be used.", helmCommandResolved);
        return helmCommandResolved;
    }

    /**
     * Prints a summary of some local repo information, if the user wants verbosity
     */
    private void printLocalRepos() {
        HelmChartRepoLocal[] repos = localRepos.getRepositories();
        logger.log(logLevelVerbose, "Api Version: {}", localRepos.getApiVersion());
        logger.log(logLevelVerbose, "Generated: {}", localRepos.getGenerated());
        logger.log(logLevelVerbose, "Number of Repos: {}", localRepos.getRepositories().length);
        for (HelmChartRepoLocal r : repos) {
            logger.log(logLevelVerbose, "\tName: {}", r.getName());
            logger.log(logLevelVerbose, "\tCache: {}", r.getCache());
            logger.log(logLevelVerbose, "\tUrl: {}", r.getUrl());
        }
    }

    /**
     * For each of the local repos the user has previously added (using for example
     * 'helm repo add') calls the function to load the charts
     */
    private void loadLocalCharts() {
        HelmChartRepoLocal[] repos = localRepos.getRepositories();
        for (HelmChartRepoLocal r : repos) {
            File cache = new File(r.getCache());
            loadChartsFromCache(r, cache);
        }
    }

    /**
     * Takes a yaml file containing elements representing cached helm charts
     * and a helm chart object and adds that Helm Chart object to the charts map
     * making8 sure ot set the url of each cached helm chart using the url
     * 
     *
     * @param r a HelmChartRepoLocal (needed for the url only)
     * @param c a yaml file containing a set of helm charts 
     */
    protected void loadChartsFromCache(HelmChartRepoLocal r, File c) {
        HelmChartLocalCache cache;
        try {
            ObjectMapper mapper = getObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            cache = mapper.readValue(c, HelmChartLocalCache.class);
            Map<String, HelmChart[]> entries = cache.getEntries();
            for (Map.Entry<String, HelmChart[]> entry : entries.entrySet()) {
                for (HelmChart h : entry.getValue()) {
                    // Here we remember the url of the chart repo in which we found the chart.
                    // Note that this url cannot be inferred from the url of the chart
                    h.setRepoUrl(r.getUrl());
                    // some chart entries don't contain the full url (ie one starting with 'http')
                    // but rather only contain a relative url and even that is not consistent. We do
                    // our best to construct a url from what we have
                    if (h.getUrls() != null && h.getUrls().length > 0 && !h.getUrls()[0].isEmpty() && !h.getUrls()[0].substring(0, "http".length()).equals("http")) {
                        String[] url = new String[1];
                        url[0] = r.getUrl();
                        if (url[0].charAt(url[0].length() - 1) != '/') {
                            url[0] = url[0].concat("/");
                        }
                        url[0] = url[0].concat(h.getUrls()[0]);
                        h.setUrls(url);
                    }
                    charts.put(h.getName(), h.getVersion(), h);
                }
            }
        } catch (Exception e) {
            logger.info("Error loading charts from helm cache: {}", e.getMessage());
        }
    }

    /**
     * For each chart in the referenced charts map, print the chart. Precede that
     * with a summary of how many charts were referenced
     */
    protected void printCharts() {
        MapIterator<MultiKey<? extends String>, HelmChart> it = chartsReferenced.mapIterator();
        IChartMapPrinter p = getPrinter();
        try {
            if (chartsReferenced.size() == 1) {
                p.printSectionHeader("There is one referenced Helm Chart");
            } else {
                p.printSectionHeader("There are " + chartsReferenced.size() + " referenced Helm Charts");
            }
            while (it.hasNext()) {
                it.next();
                p.printChart(it.getValue());
            }
        } catch (ChartMapException e) {
            logger.error("IOException printing charts: {} ", e.getMessage());
        }
    }

    /**
     * Resolves a charts dependencies by getting the chart and then finding the
     * charts dependencies.
     * 
     * @throws ChartMapException if an error collecting dependencies or applying
     *                           templates
     */
    protected void resolveChartDependencies() throws ChartMapException {
        try {
            String chartDirName = fetchChart();
            if (chart != null) {
                collectDependencies(chartDirName, null);
                applyTemplates();
            } else {
                logger.error("Chart {} was not found", getChartName());
            }
        } catch (ChartMapException e) {
            logger.error("Error resolving chart dependencies: {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Fetches a chart in one of four ways ... 1. If the user
     * specified an appr spec, pull the chart using the helm command line 2. If the
     * user specified the url of a chart package (a tgz file), download the file
     * using http and unpack it 3. If the user specified the name of a local tgz
     * file, there is no need to fetch a chart 4. If the user specified the chart by
     * name, the chart is already in the charts map we create from the repo so find
     * the download url from that entry and download it
     * 
     * @return the name of the directory in which the chart can be found
     * @throws ChartMapException if the chart could not be gotten
     */
    protected String fetchChart() throws ChartMapException {
        String chartDirName = "";
        try {
            if (getApprSpec() != null) {
                chartDirName = pullChart(getApprSpec());
            } else if (getChartUrl() != null) {
                chartDirName = downloadChart(getChartUrl());
            } else if (getChartFilename() != null) {
                chartDirName = getChart(getChartFilename());
            } else {
                HelmChart h = charts.get(getChartName(), chartVersion);
                if (h == null) {
                    throw (new ChartMapException(
                            "chart ".concat(getChartName().concat(":").concat(chartVersion).concat(" not found"))));
                }
                chartDirName = downloadChart(h.getUrls()[0]);
            }
            updateLocalRepo(chartDirName);
        } catch (ChartMapException e) {
            logger.error("Error getting chart: {}", e.getMessage());
            throw (e);
        }
        chart = charts.get(getChartName(), getChartVersion());
        if (chartDirName != null) {
            return chartDirName.substring(0, chartDirName.lastIndexOf(File.separator)); // return the parent directory
        }
        return chartDirName;
    }

    /**
     * Given a chart location in the file system, unpacks the chart
     * 
     * @param chartFilename the location of the chart in the file system
     * @return the name of the directory in which the chart was unpacked e.g.
     *         /temp/helm-chartmap-test-chart_1.0.2/helm-chartmap-test-chart
     * @throws ChartMapException when an IO Exception occurs
     */
    protected String getChart(String chartFilename) throws ChartMapException {
        try {
            Path src = new File(chartFilename).toPath();
            Path tgt = new File(getTempDirName()).toPath().resolve(new File(chartFilename).getName());
            Files.copy(src, tgt, StandardCopyOption.REPLACE_EXISTING);
            String s = tgt.toAbsolutePath().toString();
            return (unpackChart(s));
        } catch (IOException e) {
            throw new ChartMapException("IOException in getChart(".concat(chartFilename).concat(")"));
        }
    }

    /**
     * Downloads a chart using appr into the temp directory
     *
     * @param apprSpec a string specifying the location of the chart
     * @return the name of the directory into which the chart was downloaded e.g.
     *         /temp/melahn_helm-chartmap-test-chart_1.0.0/helm-chartmap-test-chart
     * @throws ChartMapException if the chart could not be pulled 
     */
    protected String pullChart(String apprSpec) throws ChartMapException {
        String chartDirName = null;
        try {
            String[] c = {"helm", "quay", "pull", apprSpec};
            Process p = getProcess(c, new File(getTempDirName()));
            p.waitFor(PROCESS_TIMEOUT, TimeUnit.MILLISECONDS);
            int exitValue = p.exitValue();
            if (exitValue == 0) {
                chartDirName = getTempDirName() + apprSpec.substring(apprSpec.indexOf('/') + 1, apprSpec.length())
                        .replace('@', '_').replace('/', '_') + File.separator + getChartName();
                createChart(chartDirName);
                extractEmbeddedCharts(chartDirName);
            } else {
                throw new ChartMapException(
                        String.format("Error Code: %c executing helm quay pull command ", exitValue));
            }
        } catch (IOException e) {
            throw (new ChartMapException(String.format(
                    "IOException pulling chart from appr using specification %s : %s", apprSpec, e.getMessage())));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ChartMapException(String.format(INTERRUPTED_EXCEPTION, apprSpec, e.getMessage()));
        }
        return chartDirName;
    }

    /**
     * This interface and its implementation is an attempt to simplify the exception
     * handling when using lambda functions, which is notoriously bad
     */
    @FunctionalInterface
    public interface LambdaConsumer<T, E extends Exception> {
        void accept(T t) throws E;
    }

    public static <T> Consumer<T> lambdaExceptionWrapper(LambdaConsumer<T, Exception> throwingConsumer) {
        return x -> {
            try {
                throwingConsumer.accept(x);
            } catch (Exception e) {
                throw new RuntimeException(e); // NOSONAR
                                               // using a generic exception is a code smell but there is no avoiding
                                               // because of lambda functions poor exception handling ... look at
                                               // removing the lambda
            }
        };
    }

    /**
     * Extracts embedded charts found in a chart directory
     * 
     * @param d A directory containing a chart
     * @throws ChartMapException if an exception occurs extracting the embedded
     *                           archives
     */
    public void extractEmbeddedCharts(String d) throws ChartMapException {
        final int MAXDEPTH = 5;
        try (Stream<Path> walk = Files.walk(Paths.get(d), MAXDEPTH)) {
            walk.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().endsWith(".tgz"))
                    .collect(Collectors.toList()).forEach(lambdaExceptionWrapper(
                            p -> getExtracter().extract(p.toString(), Paths.get(d, CHARTS_DIR_NAME))));
        } catch (IOException e) {
            String m = String.format("IO Exception extracting embedded charts from %s", d);
            logger.error(m);
            throw new ChartMapException(m);
        }
    }

    /**
     * Downloads a Helm Chart from a Helm Chart repository to a a tgz file on disk.
     * Unpacks it and creates an entry for the chart in the local charts map
     *
     * @param u A string holding the url of the Helm Chart to be downloaded
     * @return the name of the directory where the chart was pulled into e.g.
     *         /temp/helm-chartmap-test-chart_1.0.2/helm-chartmap-test-chart
     * @throws ChartMapException if an error occured downloading the chart
     */
    protected String downloadChart(String u) throws ChartMapException {
        String chartDirName = null;
        String tgzFileName = tempDirName + this.getClass().getCanonicalName() + "_chart.tgz";
        try (FileOutputStream fos = new FileOutputStream(new File(tgzFileName));) {
            CloseableHttpClient client = HttpClientBuilder.create().build();
            HttpResponse response = getHttpResponse(client, u);
            HttpEntity entity = response.getEntity();
            int rc = response.getStatusLine().getStatusCode();
            if (rc == 200) {
                InputStream is = entity.getContent();
                int b;
                while ((b = is.read()) != -1) {
                    fos.write(b);
                }
                is.close();
                client.close();
                chartDirName = unpackChart(tgzFileName);
                createChart(chartDirName);
            } else {
                String m = String.format("Error downloading chart from URL: %s : %s", u, rc);
                logger.error(m);
                throw new ChartMapException(m);
            }
        } catch (IOException e) {
            String m = String.format("Exception %s downloading chart %s", e.getClass(), chartName);
            logger.error(m);
            throw new ChartMapException(m);
        } 
        return chartDirName;
    }

    /**
     * Gets an HTTP response.  The main purpose of extracting this to a method is to allow
     * test cases for different kinds of responses and exceptions.
     * 
     * @param c Http client.  It is the caller's responsibility to close it.
     * @param u the URL to get
     * @return the HttpResponse
     * @throws IOException if an error occured executing the HTTP request
     */
    protected static HttpResponse getHttpResponse(CloseableHttpClient c, String u) throws IOException {
        HttpGet request = new HttpGet(u);
        return c.execute(request);
    }

    /**
     * Updates the local chart cache using the Helm client. This is only done if the
     * user has specified the refresh parameter on the command line or method call.
     *
     * @param dirName The name of the directory containing the chart
     * @throws ChartMapException if an error occurs updating the local repo
     */
    protected void updateLocalRepo(String dirName) throws ChartMapException {
        // if the user wants us to update the Helm dependencies, do so
        if (this.isRefreshLocalRepo()) {
            String[] c = {"helm", "dep", "update"};
            int exitValue = -1;
            try {
                Process p = getProcess(c, new File(dirName));
                p.waitFor(PROCESS_TIMEOUT, TimeUnit.MILLISECONDS);
                exitValue = p.exitValue();
            } catch (IOException e) {
                throw new ChartMapException("IOException executing helm dep update");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ChartMapException(
                        "InterruptedException while executing helm dep update");
            }
            if (exitValue != 0) {
                throw new ChartMapException("Exception updating chart repo in " + dirName + ".  Exit code: " + exitValue
                        + ".  Possibly you cannot access one of your remote charts repos.");
            } else {
                logger.log(logLevelVerbose, "Updated Helm dependencies");
            }
        }
    }

    /**
     * Creates a chart in the charts map from a Chart.yaml located in the provided
     * directory.
     *
     * @param chartDirName the name of the directory in which the Chart.yaml file is
     *                     to be found
     */
    protected void createChart(String chartDirName) {
        String yamlChartFilename = chartDirName + File.separator + CHART_YAML;
        try {
            ObjectMapper mapper = getObjectMapper();
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            HelmChart h = mapper.readValue(new File(yamlChartFilename), HelmChart.class);
            setChartName(h.getName());
            setChartVersion(h.getVersion());
            // If the chart is already in the charts map we want to transfer any extra
            // information from that chart into the chart we create from the yaml file since 
            // otherwise we will lose it. Currently there is only such piece of information, the repo url
            HelmChart foundChart = charts.get(h.getName(), h.getVersion());
            if (foundChart != null) {
                h.setRepoUrl(foundChart.getRepoUrl());
            }
            // Don't forget the values
            collectValues(chartDirName, h);
            charts.put(h.getName(), h.getVersion(), h);
        } catch (IOException e) {
            logger.error("IOException extracting Chart information from {}", yamlChartFilename);
        }
    }

    /**
     * Unpacks a Helm Chart tgz file.
     *
     * @param chartFilename The name of the tgz file containing the chart
     * @return the name of the directory in which the chart was unpacked e.g.
     *         /temp/helm-chartmap-test-chart_1.0.2/helm-chartmap-test-chart
     * @throws ChartMapException if an error occurs processing the chart
     */
    protected String unpackChart(String chartFilename) throws ChartMapException {
        if (chartFilename == null || tempDirName == null) {
            String m = String.format("chartFilename = %s tempDirName = %s",
                    (chartFilename == null) ? "null" : chartFilename, (tempDirName == null) ? "null" : tempDirName);
            throw new ChartMapException(m);
        }
        try {
            getExtracter().extract(chartFilename, Paths.get(tempDirName));
            // If the Chart Name or Version were not yet extracted, such as would happen if
            // the chart was provided as a local tgz file
            // then extract the chart name and version from the highest order Chart.yaml
            // file and create the entry in the charts map
            // if it is not already there (such as would happen if the chart was in an appr
            // repo)
            if (getChartName() == null || getChartVersion() == null) {
                File[] directories = new File(tempDirName).listFiles(File::isDirectory);
                if (directories.length > 0) {
                    String chartYamlFilename = directories[0] + File.separator + CHART_YAML;
                    File chartYamlFile = new File(chartYamlFilename);
                    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                    mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                    HelmChart h = mapper.readValue(chartYamlFile, HelmChart.class);
                    setChartName(h.getName());
                    setChartVersion(h.getVersion());
                    if (charts.get(chartName, chartVersion) == null) {
                        charts.put(chartName, chartVersion, h);
                    }
                } else {
                    String m = "Archive content does not appear to be valid. No chart found.";
                    logger.error(m);
                    throw new ChartMapException(m);
                }
            }
            return getBaseName(tempDirName);
        } catch (IOException e) {
            String m = String.format("Exception %s unpacking helm chart: %s", e.getClass(), e.getMessage());
            logger.error(m);
            throw new ChartMapException(m);
        }
    }

    /**
     * 
     * Returns a new extracter instance.
     * @return a new ArchiveExtract
     * 
    */
    ArchiveExtract getExtracter() {
        return new ArchiveExtract();
    }

    /**
     * Returns the base name found in a directory, e.g. my-chart
     * 
     * @param d directory name
     * @return the single base name found, null otherwise
     * @throws IOException if an exception occurs listing the directory
     */
    protected static String getBaseName(String d) throws IOException {
        Stream<Path> s = Files.list(Paths.get(d));
        List<Path> l = s.filter(p -> p.toFile().isDirectory()).collect(Collectors.toList());
        s.close();
        return l.size() == 1 ? l.get(0).toString() : null;
    }

    /**
     * Starting at the directory in chartDirName, recursively discovers all the
     * dependents of the Helm Chart and saves the resulting information in the Helm
     * Charts. Along the way, it renders templates for the charts.
     * <p>
     * These dependency relationships are later used to create links between the
     * charts in the printed map.
     *
     * @param chartDirName the name of a directory containing a Helm Chart
     * @param h            the Helm Chart on which dependencies will be collected
     * 
     * @throws ChartMapException when an error occurs collecting dependencies
     */
    protected void collectDependencies(String chartDirName, HelmChart h) throws ChartMapException { 
        HelmChart parentHelmChart = null;
        try {
            if (h != null) {
                logger.log(logLevelVerbose, "Processing Chart {} : {}", h.getName(), h.getVersion());
            }
            File currentDirectory = new File(chartDirName);
            String[] directories = currentDirectory.list((c, n) -> new File(c, n).isDirectory());
            if (directories != null) {
                for (String directory : directories) {
                    if (h != null) {
                        parentHelmChart = charts.get(h.getName(), h.getVersion());
                        chartsReferenced.put(parentHelmChart.getName(), parentHelmChart.getVersion(), parentHelmChart);
                    }
                    File chartFile = new File(chartDirName + File.separator + directory + File.separator + CHART_YAML);
                    if (chartFile.exists()) {
                        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                        // this reference is not in the map
                        HelmChart currentHelmChartFromDisk = mapper.readValue(chartFile, HelmChart.class);
                        HelmChart currentHelmChart = charts.get(currentHelmChartFromDisk.getName(),
                                currentHelmChartFromDisk.getVersion());
                        if (currentHelmChart == null) {
                            // this is most likely because the local Helm charts are out of date and should
                            // be refreshed
                            throw new ChartMapException(String.format("A dependency on %s:%s  was found but that "
                                    + "chart was not found in the local Helm charts cache. "
                                    + "Check to make sure all the helm repos are in your local cache by running the 'helm repo list' command. "
                                    + "Try running the command again with the '-r' option.",
                                    currentHelmChartFromDisk.getName(), currentHelmChartFromDisk.getVersion()));
                        }
                        handleHelmChartCondition(checkForCondition(chartDirName, currentHelmChart, parentHelmChart),
                                chartDirName, directory, currentHelmChart, parentHelmChart);
                    } 
                } 
            } 
        } catch (IOException e) {
            String m = String.format("IOException getting Dependencies: %s", e.getMessage());
            logger.error(m);
            throw new ChartMapException(m);
        }
    }

    /**
     * Check if there is a condition property in the parent Helm Chart that
     * corresponds to the current Helm Chart. If found, get the value
     * 
     * @param chartDirName     the name of the directory where the chart is found
     * @param currentHelmChart the helm chart found in the local charts repo
     * @param parentHelmChart  the parent of the current helm chart
     * @return True if a condition for including the chart was found
     */
    protected Boolean checkForCondition(String chartDirName, HelmChart currentHelmChart, HelmChart parentHelmChart) {
        Boolean condition = Boolean.TRUE;
        String conditionPropertyName = getConditionPropertyName(chartDirName, currentHelmChart);
        if (conditionPropertyName != null) {
            Boolean foundCondition = getCondition(conditionPropertyName, parentHelmChart);
            if (foundCondition != null) {
                condition = foundCondition;
            }
        }
        return condition;
    }

    /**
     * Handles the case where a HelmChart wasn't excluded by a condition property in
     * a parent Helm Chart so it needs to be added to the referenced chart map and
     * attached as a dependent of the parent
     * 
     * Note that charts with false condition properties are not printed at all
     * 
     * @param condition        whether the chart was excluded
     * @param chartDirName     the name of the directory where the chart is found
     * @param directory        a subdirectory of the chartDirName
     * @param currentHelmChart the helm chart found in the local charts repo
     * @param parentHelmChart  the parent of the currentHelmChart
     * 
     * @throws ChartMapException when an error occurs rendering templates or
     *                           collecting values
     */
    protected void handleHelmChartCondition(Boolean condition, String chartDirName, String directory,
            HelmChart currentHelmChart, HelmChart parentHelmChart) throws ChartMapException {
        try {
            if (Boolean.TRUE.equals(condition)) {
                File currentDirectory = new File(chartDirName);
                collectValues(chartDirName + File.separator + directory, currentHelmChart);
                if (parentHelmChart != null) {
                    // add this chart as a dependent
                    parentHelmChart.getDiscoveredDependencies().add(currentHelmChart);
                }
                chartsReferenced.put(currentHelmChart.getName(), currentHelmChart.getVersion(), currentHelmChart);
                renderTemplates(currentDirectory, currentHelmChart, parentHelmChart);
                File chartsDirectory = new File(
                        chartDirName + File.separator + directory + File.separator + CHARTS_DIR_NAME);
                if (chartsDirectory.exists()) {
                    collectDependencies(chartsDirectory.getAbsolutePath(), currentHelmChart); // recursion
                }
            }
        } catch (IOException e) {
            String m = "IOException collecting values in handleHelmChartCondition";
            logger.error(m);
            throw new ChartMapException(m);
        }

    }

    /**
     *
     * Looks for a Boolean value in a Helm Chart's values map
     *
     * @param key The key for a boolean map value that may or may or not exist in
     *            the Helm Chart
     * @param h   The helm chart whose values will be inspected for the key
     * @return A Boolean. If the key was found in the map and it was of a Boolean
     *         type. the return value will be the value of that entry. Otherwise
     *         returns TRUE.
     */
    protected Boolean getCondition(String key, HelmChart h) {
        Boolean condition = Boolean.TRUE;
        Boolean envCondition = Boolean.FALSE;
        /**
         * Check if it was specified as an environment variable, overriding what may be
         * in the chart
         */
        try {
            List<String> vars = getEnvVars();
            for (String s : vars) {
                String n = s.substring(0, s.indexOf('='));
                if (key.equals(n)) {
                    envCondition = Boolean.TRUE;
                    String v = s.substring(s.indexOf('=') + 1, s.length());
                    if (v.equalsIgnoreCase("false")) {
                        condition = Boolean.FALSE;
                    }
                }
            }
        } catch (IOException e) {
            logger.error("IO Exception getting condition of {}", key);
        }
        /**
         * If the condition was not found in the environment variable set, look in the
         * chart
         */
        if (Boolean.FALSE.equals(envCondition)) {
            Object o = ChartUtil.getValue(key, h.getValues());
            if (o instanceof Boolean) {
                condition = (Boolean) o;
            }
        }
        return condition;
    }

    /**
     * Returns the name of a condition property for the chart in the Helm Chart's
     * parent's requirements.yaml file
     *
     * @param chartDirName The name of a directory containing the Helm Chart
     * @param h            A Helm Chart
     * @return The name of a condition property if one exists, null otherwise
     */
    protected String getConditionPropertyName(String chartDirName, HelmChart h) {
        Map<String, String> conditionMap = getConditionMap(
                chartDirName.substring(0, chartDirName.lastIndexOf(File.separator)));
        return conditionMap.get(h.getName());
    }

    /**
     * Helm chart requirements may now include a condition property indicating
     * whether the requirement is true or false. See
     * https://github.com/kubernetes/helm/issues/1837
     *
     * The purpose of this method is to parse a requirements file and answer a map
     * of chart names to their condition property name
     *
     *
     * @param directoryName the name of a directory that may contain a
     *                      requirements.yaml file
     * @return a hash map containing an element for each member of the requirements
     *         file that contains the name of the condition property. The map may be
     *         empty.
     */
    protected HashMap<String, String> getConditionMap(String directoryName) {
        File requirementsFile = new File(directoryName + File.separator + "requirements.yaml");
        HashMap<String, String> conditionMap = new HashMap<>();
        if (requirementsFile.exists()) {
            try {
                ObjectMapper mapper = getObjectMapper();
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                HelmRequirements requirements = mapper.readValue(requirementsFile, HelmRequirements.class);
                List<HelmRequirement> dependents;
                dependents = Arrays.asList(requirements.getDependencies());
                dependents.forEach(r -> conditionMap.put(r.getName(), r.getCondition()));
            } catch (IOException e) {
                logger.error("IOException parsing requirements file {}", requirementsFile.getAbsolutePath());
            }
        }
        return conditionMap;
    }

    /**
     * For each of the referenced charts, find the template that has the highest
     * priority (the 'weighted template' which was previously determined from the
     * location of the hierarchy in the file system (parents having priority over
     * children) and set that one in the chart
     */
    protected void applyTemplates() {
        MapIterator<MultiKey<? extends String>, HelmChart> i = chartsReferenced.mapIterator();
        while (i.hasNext()) {
            i.next();
            HelmChart h = i.getValue();
            HashSet<HelmDeploymentTemplate> a = new HashSet<>();
            for (HelmDeploymentTemplate t : h.getDeploymentTemplates()) {
                // get the template from the weighted templates array
                WeightedDeploymentTemplate w = deploymentTemplatesReferenced.get(t.getFileName());
                if (w != null) {
                    // and use that instead of what the Chart was using. Usually they are not
                    // different
                    HelmDeploymentContainer[] helmDeploymentContainers = w.getTemplate().getSpec().getTemplate()
                            .getSpec().getContainers();
                    for (HelmDeploymentContainer c : helmDeploymentContainers) {
                        c.setParent(t.getSpec().getTemplate().getSpec().getContainers()[0].getParent());
                    }
                    a.add(w.getTemplate());
                }
            }
            h.setDeploymentTemplates(a);
            collectContainers(h);
        }
    }

    /**
     * Collects the values of all the properties found in the values.yaml file in a
     * directory and attaches the result to a Helm Chart object
     *
     * Note one cannot just load the values file into a known model because it is
     * file that has no model, hence the need for this more generic approach
     *
     * @param dirName the name of the directory in which the values file exists
     * @param h       the Helm Chart object to which these values apply
     * @throws IOException if an error occured collecting the values
     */
    @SuppressWarnings("unchecked")
    protected void collectValues(String dirName, HelmChart h) throws IOException {
        if (h == null || dirName == null) {
            return;
        }
        File valuesFile = new File(dirName + File.separator + "values.yaml");
        if (valuesFile.exists()) {
            FileInputStream fis = new FileInputStream(valuesFile);
            Object o = new Yaml().load(fis);
            if (o instanceof Map<?, ?>) {
                Map<String, Object> chartValues = (Map<String, Object>) o;
                h.setValues(chartValues);
            } else {
                logger.log(logLevelVerbose, "The values.yaml file: {} could not be parsed. Possibly it is empty.",
                        valuesFile.getAbsolutePath());
            }
        }
    }

    /**
     * Adds all the containers referenced in the current Helm Chart to the
     * collection of all referenced containers. This collection is used later to
     * print a list of all the containers.
     *
     * @param h The current Helm Chart
     */
    private void collectContainers(HelmChart h) {
        HashSet<HelmDeploymentContainer> containers = (HashSet<HelmDeploymentContainer>) h.getContainers();
        for (HelmDeploymentContainer c : containers) {
            imagesReferenced.add(c.getImage());
        }
    }

    /**
     * Uses the helm command line to render the templates of a chart. The resulting
     * rendered template is saved in the templates directory of the chart with the
     * name this.getClass().getCanonicalName()_renderedtemplates.yaml
     *
     * @param d The directory in which the chart directory exists
     * @param h The Helm Chart containing the templates
     * @param p The Helm Chart that is the parent of h
     * 
     * @throws ChartMapException when an error occurs rendering template
     */
    protected void renderTemplates(File d, HelmChart h, HelmChart p) throws ChartMapException {
        try {
            if (h.getType() != null && h.getType().equals("library")) {
                // skip rendering library charts (these were introduced in Helm V3)
                return;
            }
            File tf = runTemplateCommand(d, h);
            ArrayList<Boolean> a = getTemplateArray(tf, h.getName());
            ArrayList<String> b = getTemplateArray(d, tf);
            int i = 0;
            Yaml y = new Yaml();
            for (Object data : y.loadAll(new FileInputStream(tf))) {
                /**
                 * there may multiple yaml documents in this one document inspect the object to
                 * see if it is a Deployment or a StatefulSet template if it is add to the
                 * deploymentTemplates array
                 */
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> m = (Map<String, Object>) data;
                    Object o = m.get("kind");
                    if (o instanceof String) {
                        processTemplate(new Object[] { h, p, d, o, y }, m, a, b, i);
                    }
                    i++; // index to the next element in the array that indicates whether the template is
                         // of interest for the current chart level
                }
            }
        } catch (

        Exception e) {
            String m = String.format("Exception rendering template for %s : %s", h.getNameFull(), e.getMessage());
            logger.error(m);
            throw new ChartMapException(m);
        }
    }

    private void processTemplate(Object[] o, Map<String, Object> m, ArrayList<Boolean> a, ArrayList<String> b, int i)
            throws JsonProcessingException {
        // pull the parameters out of the array for easier reference. They were only
        // passed in an object array to reduce the size of the parameter list
        HelmChart h = (HelmChart) o[0];
        HelmChart p = (HelmChart) o[1];
        File d = (File) o[2];
        String v = (String) o[3];
        Yaml y = (Yaml) o[4];
        if (v.equals("Deployment") || v.equals("StatefulSet")) {
            String s = y.dump(m);
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
            HelmDeploymentTemplate t = mapper.readValue(s, HelmDeploymentTemplate.class);
            // remember the parent
            HelmDeploymentContainer[] containers = t.getSpec().getTemplate().getSpec().getContainers();
            for (HelmDeploymentContainer c : containers) {
                c.setParent(p);
            }
            // if this template is a child of this chart remember that fact
            if (Boolean.TRUE.equals(a.get(i))) {
                t.setFileName(b.get(i));
                h.getDeploymentTemplates().add(t);
            }
            processWeights(t, p, d, b, i);
        }
    }

    /**
     * Process the weights of the templates previously found against this template.
     * We have the following cases:
     * 
     * 1. The Chart has a dependency on this template and nothing supercedes it in
     * some parent chart
     * 
     * 2. The Chart has a dependency on this template and a superceding version of
     * this template has already been found
     * 
     * 3. The Chart has a dependency on this template and a superceding version of
     * this template will be found later
     * 
     * @param t the template currently being processed
     * @param p the parent of the template
     * @param d the directory of the template
     * @param b the array in which the weights are kept
     * @param i the index of the template in the array
     */
    private void processWeights(HelmDeploymentTemplate t, HelmChart p, File d, ArrayList<String> b, int i) {
        WeightedDeploymentTemplate weightedTemplate = deploymentTemplatesReferenced.get(b.get(i));
        if (weightedTemplate == null) {
            weightedTemplate = new WeightedDeploymentTemplate(d.getAbsolutePath(), t);
            deploymentTemplatesReferenced.put(b.get(i), weightedTemplate);
        } else {
            // remember the parent
            HelmDeploymentContainer[] containers = weightedTemplate.getTemplate().getSpec().getTemplate().getSpec()
                    .getContainers();
            for (HelmDeploymentContainer c : containers) {
                c.setParent(p);
            }
            if (weightedTemplate.getWeight() > getWeight(d.getAbsolutePath())) {
                // a superceding template was found so replace the template that is referenced
                // in the global list of templates
                weightedTemplate.setTemplate(t);
            }
        }
    }

    /**
     * Runs the helm template command
     *
     * @param dir   the directory in which to run the command
     * @param h     the helm chart on which the template command should be run
     * @return      the template file that was generated
     * @throws ChartMapException when an error occurs running the helm template command
     * @throws IOException when an IO error occurs creating files
     * 
     */
    protected File runTemplateCommand(File dir, HelmChart h) throws IOException, ChartMapException {
        String command = helmCommand;
        // Get any variables the user may have specified and append to the command
        List<String> envVars = getEnvVars();
        if (!envVars.isEmpty()) {
            command = command.concat(" --set \"");
            for (int i = 0; i < envVars.size(); i++) {
                command = command.concat(envVars.get(i));
                if (i < envVars.size() - 1) {
                    command = command.concat(",");
                } else {
                    command = command.concat("\" ");
                }
            }
        }
        command = command.concat(" template ").concat(h.getName());
        File f = null;
        try {
            Process p = Runtime.getRuntime().exec(command, null, dir);
            File templateDir = new File(
                    dir.getAbsolutePath() + File.separator + h.getName() + File.separator + "templates");
            templateDir.mkdirs();
            String templateFilename = this.getClass().getCanonicalName() + RENDERED_TEMPLATE_FILE;
            f = new File(templateDir, templateFilename);
            if (!f.createNewFile()) {
                throw new ChartMapException("File: " + f.getAbsolutePath() + " could not be created.");
            }
            runTemplateCommand(f, p, h);
        } catch (ChartMapException e) {
            logger.error("ChartMapException {} running template command: {} ", e.getMessage(), command);
            throw e;
        }
        return f;
    }

    /**
     * Runs the helm template command give a template f to create and a process.
     * Handled in a separate to reduce code complexity of caller.
     *
     * @param f the file to which to write the template
     * @param p the process to use to run the command
     * @param h the helm chart
     * 
     * @throws ChartMapException if an IOException, InterruptedException or a template command error occurs
     */
    protected void runTemplateCommand(File f, Process p, HelmChart h) throws ChartMapException {
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
                BufferedInputStream bis = new BufferedInputStream(p.getInputStream());) {
            byte[] bytes = new byte[16384];
            int len;
            while ((len = bis.read(bytes)) > 0) {
                bos.write(bytes, 0, len);
            }
            p.waitFor(PROCESS_TIMEOUT, TimeUnit.MILLISECONDS);
            int exitValue = p.exitValue();
            if (exitValue != 0) {
                String message;
                InputStream err = p.getErrorStream();
                BufferedReader br = new BufferedReader(new java.io.InputStreamReader(err));
                while ((message = br.readLine()) != null) {
                    logger.error(message);
                }
                err.close();
                logger.error("Error running template command. Exit Value = {}.", exitValue);
                throw new ChartMapException(String.format("Error running template command. Exit Value = %d.", exitValue));
            } 
        } catch (IOException e) {
            logger.error("IOException running template command");
            throw new ChartMapException("IOException running template command");
        } catch (InterruptedException e) {
            logger.error("InterruptedException running template command");
            Thread.currentThread().interrupt();
            throw new ChartMapException("InterruptedException running template command");
        }
    }

    /**
     * Returns a list of any environment variables the user wants set based up on
     * the content of an environment specification file the user may have specified
     *
     * @return a list with each environment variable specified. This may be empty
     *         since such environment variables are not mandatory
     * @throws IOException if the env vars could be get gotten
     */
    protected List<String> getEnvVars() throws IOException {
        ArrayList<String> envVars = new ArrayList<>();
        if (envFilename != null) {
            try {
                ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
                mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
                File envFile = new File(envFilename);
                EnvironmentSpecification envSpec = mapper.readValue(envFile, EnvironmentSpecification.class);
                Map<String, String> vars = envSpec.getEnvironment();
                vars.forEach((k, v) -> envVars.add(getEscapedVariable(k + "=") + v));
            } catch (IOException e) {
                logger.error(LOG_FORMAT_4, "IOException reading Environment Variables File ", envFilename, " : ",
                        e.getMessage());
                throw e;
            }
        }
        return envVars;
    }

    /**
     * 
     * @param v String which may or may not contain values that need to be escaped
     *          to form a value --set argument for helm. Note: this seems to be
     *          necessary since helm V3 though I don't see any documentation about
     *          it
     * @return String with '-' escaped
     */
    private String getEscapedVariable(String v) {
        return v.replace("-", "\\-");
    }

    /**
     * Return a 'weight' of a String, calculated by the number of segments in the
     * String separated by the File separator
     *
     * @param s a String whose weight is desired
     * @return the calculated weight
     */
    protected int getWeight(String s) {
        return (s != null) ? s.split(File.separator).length : MAX_WEIGHT;
    }

    /**
     * Creates an array that can be used to filter out the templates from the
     * rendered templates file that don't pertain to the current chart. Recall that
     * the rendered templates file created in renderTemplates contains all the
     * descendent template files so we need a way to know which of the templates
     * mentioned in that file pertain to the chart at this level so we can draw the
     * right arrows later.
     *
     * @param f the file containing all the rendered templates
     * @param c the name of the chart we are interested in at the moment
     * @return an array with True object if the corresponding template is one that
     *         pertains to the chart
     */
    protected ArrayList<Boolean> getTemplateArray(File f, String c) {
        ArrayList<Boolean> a = new ArrayList<>();
        String line = null;
        try (BufferedReader br = new BufferedReader(new FileReader(f));) {
            line = br.readLine();
            while (line != null) {
                if (line.length() > (START_OF_TEMPLATE + c).length() && line.charAt(0) == '#') {
                    // a pattern like # Source: <chartname>/templates ... means that this is
                    // a template of immediate interest to the chart e.g.
                    // helm-chartmap-test-chart/templates
                    line = processTemplateYaml(line, br, a, c);
                } else {
                    line = br.readLine();
                }
            }
        } catch (IOException e) {
            logger.error("IOException creating template array in {} with line {}", f.getName(), line);
        }
        return a;
    }

   /**
     * Parses a file containing multiple yaml files and returns a array of the file
     * names of those yaml files
     *
     * @param d The directory in which the templates reside
     * @param f A yaml file containing multiple yaml objects, each such object preceded
     *          by a comment of the form "# Source filename" e.g. # Source:
     *          helm-chartmap-test-chart/charts/helm-chartmap-test-subchart/charts/postgresql/templates/deployment.yaml
     * @return an array containing the fully qualified file names of all the
     *         deployment templates mentioned in the yaml file
     */
    protected ArrayList<String> getTemplateArray(File d, File f) {
        ArrayList<String> a = new ArrayList<>();
        String line = null;
        try (FileReader fileReader = new FileReader(f);
                BufferedReader bufferedReader = new BufferedReader(fileReader);) {
            line = bufferedReader.readLine();
            while (line != null) {
                if (line.startsWith(START_OF_TEMPLATE)) {
                    String[] s = line.split(START_OF_TEMPLATE, line.length());
                    String fileName = s[1];
                    boolean endOfYamlInFile = false;
                    line = bufferedReader.readLine();
                    while (!endOfYamlInFile) { // read until you find the end of this yaml object
                        line = bufferedReader.readLine();
                        // EOF or the start of a new yaml section means the current object is completely
                        // read
                        if (line == null || (line.startsWith(START_OF_TEMPLATE))) {
                            endOfYamlInFile = true;
                            a.add(d.getAbsolutePath() + File.separator + fileName);
                        }
                    }
                } else {
                    line = bufferedReader.readLine();
                }
            }
        } catch (IOException e) {
            logger.error("IOException creating template array in {} with line {}", f.getName(), line);
        }
        return a;
    }

    /**
     * 
     * @param l  a line if yaml from the template file
     * @param br a buffered reader to use to read more lines of yaml if needed
     * @param a  a list to which a boolean to signal this is an interesting template
     *           can be added
     * @param c  the name of the current Helm chart being processed
     * @return the line last read
     * @throws IOException when an exception occurs reading the yaml file
     */
    protected String processTemplateYaml(String l, BufferedReader br, ArrayList<Boolean> a, String c) throws IOException {
        String[] s = l.split(File.separator, 3);
        Boolean b = Boolean.FALSE;
        if (s.length > 1 && s[0].equals(START_OF_TEMPLATE.concat(c)) && s[1].equals("templates")
                && !l.endsWith(RENDERED_TEMPLATE_FILE)) { // ignore the template file we generate
            b = Boolean.TRUE; // the yaml files in this section are ones we care about
        }
        boolean endOfYamlInFile = false;
        while (!endOfYamlInFile) { // read until you find the end of this yaml object
            l = br.readLine();
            // EOF or the start of a new yaml section means the current object is completely
            // read
            if (l == null || (l.startsWith(START_OF_TEMPLATE))) {
                endOfYamlInFile = true;
                a.add(b);
            }
        }
        return l;
    }

     /**
     * Prints the Chart Map
     * 
     * @throws ChartMapException when an error occurs printing the chart map
     */
    protected void printMap() throws ChartMapException {
        try {
            if (chart != null) {
                detectPrintFormat(outputFilename);
                if (printFormat.equals(PrintFormat.PLANTUML)) {
                    printer = new PlantUmlChartMapPrinter(this, outputFilename, charts, chart);
                } else if (printFormat.equals(PrintFormat.JSON)) {
                    printer = new JSONChartMapPrinter(this, outputFilename, charts, chart);
                } else {
                    printer = new TextChartMapPrinter(this, outputFilename, charts, chart);
                }
                // JSON print formats are handled differently because the charts, images
                // and dependencies are intermingled in a tree
                if (printFormat.equals(PrintFormat.JSON)) {
                    getPrinter().printTree(chart);
                }
                // Plantuml and Text print formats follow a common pattern of printing
                // first the charts, then the images, then the dependencies
                else {
                    getPrinter().printHeader();
                    printCharts();
                    printContainers();
                    printChartDependencies(chart);
                    printContainerDependencies();
                    getPrinter().printFooter();
                }
                logger.info("File {} generated", outputFilename);
                if (generateImage && printFormat.equals(PrintFormat.PLANTUML)) {
                    generateImage(outputFilename);
                }
            }
        } catch (ChartMapException e) {
            logger.error("Exception printing Map : {}", e.getMessage());
            throw e;
        }
    }

    /**
     * Generates an image from a PUML file
     * 
     * @param f the puml file
     * @throws ChartMapException if an error occurred generaing the image
     */
    protected void generateImage(String f) throws ChartMapException {
        /**
         * PlantUML wants the full path of the input file so get the pwd so it can be
         * generated
         */
        String d = System.getProperty("user.dir");
        Path i = Paths.get(f.replace("puml", "png"));
        try {
            if (Files.exists(i)) {
                Files.delete(i);
                logger.log(logLevelVerbose, LOG_FORMAT_2, i.getFileName(), " deleted");
            }
            net.sourceforge.plantuml.SourceFileReader r = getPlantUMLReader(new File(f));
            boolean e = r.hasError();
            if (!e) {
                List<net.sourceforge.plantuml.GeneratedImage> l = r.getGeneratedImages();
                if (!l.isEmpty()) {
                    File p = l.get(0).getPngFile();
                    logger.info(LOG_FORMAT_4, "Image file ", p.getName(), " generated from ", f);
                } else {
                    logger.warn(LOG_FORMAT_4, "Warning: Image file ", i.getFileName(), " was not generated from ", f);
                }
            } else {
                logger.error(LOG_FORMAT_4,
                        "Error in net.sourceforge.plantuml.GeneratedImage trying to generate image from ", d, "/", f);
            }
        } catch (IOException e) {
            logger.error(LOG_FORMAT_9, "Error generating image file", d, "/", i.getFileName(), " from ", d, "/", f,
                    " : ", e);
            throw new ChartMapException(e.getMessage());
        }
    }

    /**
     * Returns a new net.sourceforge.plantuml.SourceFileReader instance.
     * 
     * @param f the File for the reader
     * @return a new net.sourceforge.plantuml.SourceFileReader instance
     * @throws IOException when an exception occurs creating the reader
     */
    protected net.sourceforge.plantuml.SourceFileReader getPlantUMLReader(File f) throws IOException {
        return new net.sourceforge.plantuml.SourceFileReader(f);
    }

    /**
     * Prints the dependencies of a Helm Chart
     *
     * @param parent the parent helm chart from which recursion starts
     */
    protected void printChartDependencies(HelmChart parent) {
        try {
            if (parent.getNameFull().equals(chart.getNameFull())) {
                getPrinter().printSectionHeader("Chart Dependencies");
            }
            if (parent.getDiscoveredDependencies() != null) {
                // Print the chart to chart dependencies recursively
                boolean stable = isStable(parent, false); // check if the parent chart is stable
                for (HelmChart dependent : parent.getDiscoveredDependencies()) {
                    if (!chartsDependenciesPrinted.contains(parent.getNameFull() + "_" + dependent.getNameFull())) {
                        getPrinter().printChartToChartDependency(parent, dependent);
                        // if the parent is stable and the child is not then print a message if verbose
                        if (stable && !isStable(dependent, true) && isVerbose()) {
                            getPrinter().printComment("WARNING: Chart " + parent.getNameFull() + " is stable but depends on "
                                    + dependent.getNameFull() + " which may not be stable");
                        }
                        chartsDependenciesPrinted.add(parent.getNameFull() + "_" + dependent.getNameFull());
                    }
                    printChartDependencies(dependent); // recursion
                }
            }
        } catch (ChartMapException e) {
            logger.error(LOG_FORMAT_2, "Error printing chart dependencies: ", e.getMessage());
        }
    }

    /**
     * For each of the referenced charts, prints the containers referenced by the
     * chart
     */
    protected void printContainerDependencies() {
        MapIterator<MultiKey<? extends String>, HelmChart> it = chartsReferenced.mapIterator();
        try {
            while (it.hasNext()) {
                it.next();
                HelmChart h = it.getValue();
                for (HelmDeploymentContainer c : h.getContainers()) {
                    getPrinter().printChartToImageDependency(h, c.getImage());
                }
            }
        } catch (ChartMapException e) {
            logger.error(LOG_FORMAT_2, "Error printing image dependencies: ", e.getMessage());
        }
    }

    /**
     * Prints all the referenced Containers
     */
    protected void printContainers() {
        try {
            if (imagesReferenced.size() == 1) {
                getPrinter().printSectionHeader("There is one referenced Docker Image");
            } else {
                getPrinter().printSectionHeader("There are " + imagesReferenced.size() + " referenced Docker Images");
            }
            for (String s : imagesReferenced) {
                getPrinter().printImage(s);
            }
        } catch (ChartMapException e) {
            logger.error(LOG_FORMAT_2, "Error printing images: ", e.getMessage());
        }
    }

    /**
     * Determines the print format to use based on the file extension
     *
     * @param fileName the name of the file to which the chart map will be printed
     */
    protected void detectPrintFormat(String fileName) {
        if (fileName != null) {
            if (fileName.endsWith(".puml")) {
                printFormat = PrintFormat.PLANTUML;
            } else if (fileName.endsWith(".json")) {
                printFormat = PrintFormat.JSON;
            }
        }
    }

    /**
     * Determines whether a Helm Chart is stable based on a very simple heuristic.
     *
     * @param h               the Helm Chart to be inspected
     * @param checkContainers check images if true
     */
    protected boolean isStable(HelmChart h, boolean checkContainers) {
        boolean stable = true;
        if (h.getRepoUrl() != null && h.getRepoUrl().contains("/incubator")) {
            logger.log(logLevelVerbose, "chart {} does not appear to be stable", h.getNameFull());
            stable = false;
        } else if (checkContainers) { // also check the images if needed
            for (HelmDeploymentContainer c : h.getContainers()) {
                String imageName = c.getImage().toLowerCase();
                if (imageName.contains("-snapshot") || imageName.contains("-alpha") || imageName.contains("-beta")
                        || imageName.contains("-trial") || imageName.contains("-rc")) {
                    stable = false;
                    logger.log(logLevelVerbose, "image {} does not appear to be stable", c.getImage());
                }
            }
        }
        return stable;
    }

    /**
     * Creates a temporary used to download and expand the Helm Chart
     * 
     * @throws ChartMapException when an error occurs creating the temp dir
     */
    protected void createTempDir() throws ChartMapException {
        try {
            FileAttribute<Set<PosixFilePermission>> attr = PosixFilePermissions
                    .asFileAttribute(PosixFilePermissions.fromString("rwxr-----"));
            Path p = Files.createTempDirectory(this.getClass().getCanonicalName() + "." + "Temporary.", attr);
            setTempDirName(p.toAbsolutePath().toString() + File.separator);
            logger.log(logLevelVerbose, LOG_FORMAT_3, TEMP_DIR, getTempDirName(),
                    " will be used as the temporary directory.");
        } catch (IOException e) {
            throw new ChartMapException(String.format(TEMP_DIR_ERROR));
        }
    }

    /**
     * Removes the temporary directory created by createTempDir() unless the debug
     * switch was set
     * @throws ChartMapException if an error occured removing the directory
     */
    protected void removeTempDir() throws ChartMapException {
        if (isDebug()) {
            logger.info("{} {} was not removed because this is debug mode", TEMP_DIR, getTempDirName());
        } else {
            try (Stream<Path> s = Files.walk(Paths.get(getTempDirName()), FileVisitOption.FOLLOW_LINKS)) {
                s.sorted(Comparator.reverseOrder()).forEach(lambdaExceptionWrapper(Files::delete));
            } catch (IOException e) {
                logger.error(LOG_FORMAT_2, "IO Exception walking temporary directory ", getTempDirName());
                throw new ChartMapException(e.getMessage());
            }
            logger.log(logLevelVerbose, LOG_FORMAT_3, TEMP_DIR, getTempDirName(), " removed");
        }
    }

    // Getters and Setters

    public String getApprSpec() {
        return apprSpec;
    }

    public HelmChart getChart() {
        return chart;
    }

    public ChartKeyMap getCharts() {
        return charts;
    }

    public ChartKeyMap getChartsReferenced() {
        return chartsReferenced;
    }

    public String getChartFilename() {
        return chartFilename;
    }

    protected void setChartFilename(String f) {
        this.chartFilename = f;
    }

    protected void setChartName(String n) {
        this.chartName = n;
    }

    public String getChartName() {
        return chartName;
    }

    protected void setChartVersion(String v) {
        this.chartVersion = v;
    }

    public String getChartVersion() {
        return chartVersion;
    }

    public String getChartUrl() {
        return chartUrl;
    }

    protected void setChartUrl(String u) {
        this.chartUrl = u;
    }

    public boolean isDebug() {
        return debug;
    }

    protected void setDebug(boolean d) {
        this.debug = d;
    }

    public String getDefaultOutputFilename() {
        return DEFAULT_OUTPUT_FILENAME;
    }

    protected void setEnvFilename(String e) {
        this.envFilename = e;
    }

    protected void setGenerateImage(boolean b) {
        this.generateImage = b;
    }

    public String getHelmCachePath() {
        return helmCachePath;
    }

    protected void setHelmCachePath(String s) {
        helmCachePath = s;
    }

    public String getHelmConfigPath() {
        return helmConfigPath;
    }

    protected void setHelmConfigPath(String s) {
        helmConfigPath = s;
    }

    protected void setOutputFilename(String o) {
        this.outputFilename = o;
    }

    public IChartMapPrinter getPrinter() {
        return printer;
    }

    public PrintFormat getPrintFormat() {
        return printFormat;
    }

    protected void setPrintFormat(PrintFormat p) {
        this.printFormat = p;
    }

    public String getTempDirName() {
        return tempDirName;
    }

    private void setTempDirName(String t) {
        this.tempDirName = t;
    } // keep private since this directory gets recursively removed and so its kinda
      // dangerous

    public boolean isRefreshLocalRepo() {
        return refreshLocalRepo;
    }

    protected void setRefreshLocalRepo(boolean r) {
        this.refreshLocalRepo = r;
    }

    public boolean isVerbose() {
        return verbose;
    }

    protected void setVerbose(boolean v) {
        this.verbose = v;
    }
}