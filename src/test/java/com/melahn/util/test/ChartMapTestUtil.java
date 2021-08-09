package com.melahn.util.test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

public class ChartMapTestUtil {

    /**
     * Answers true if the log contains a particular entry.
     * 
     * @param baos the log
     * @param s    entry being looked for
     * @return true if the log contains s, false otherwise
     */
    public static boolean logContains(ByteArrayOutputStream baos, String s) {
        return baos.toString().contains(s);
    }

    /**
     * Cleans a directory to a depth of 3
     * 
     * @param d Path of the directory
     */
    public static void cleanDirectory(Path d) {
        final int depth = 3; // go three deep
        try {
            System.out.println("Deleting any previously created files");
            if (Files.exists(d)) {
                Files.walk(d, depth).filter(Files::isRegularFile)
                              .forEach(p -> p.toFile().delete());
                Files.deleteIfExists(d);
            }
        } catch (IOException e) {
            System.out.println("Error deleting previously created files: " + e.getMessage());
        }
    }

    /**
     *
     * Start a sub-process, setting and removing some environment variables and
     * system properties beforehand.
     *
     * @param e Environment variables to add and remove
     * @param o value of os.type to set
     * @param t test phase (e.g. "integration-test")
     * @param j Jacoco agent string
     * @param c name of class to run
     * @param p directory in which to run the process
     * @param l log file
     * @return exit value of process
     * @throws IOException
     * @throws InterruptedException
     */
    int createProcess(Object[] e, String o, String t, String j, String c, Path p, File l)
                             throws IOException, InterruptedException {
        ProcessBuilder pb = null;
        String shadedJarName = this.getShadedJarName();
        if (o == null) {
            pb = new ProcessBuilder("java",
                        "-javaagent:".concat(j),
                        "-cp",
                        shadedJarName,
                        c,
                        t);
        } else {
            pb = new ProcessBuilder("java",
                        "-javaagent:".concat(j),
                        "-cp",
                        shadedJarName,
                        "-Dos.name=".concat(o),
                        c,
                        t);
        }
        // modify the environment
        String[] a = (String[]) e[0];
        Map<String, String> v = pb.environment();
        for (int i = 0; i < a.length; i = i + 2) {
            if (a[i] != null && a[i + 1] != null) {
                v.put(a[i], a[i + 1]);
            }
        }
        String[] r = (String[]) e[1];
        for (int i = 0; i < r.length; i++) {
            v.remove(r[i]);
        }
        // Capture fhe output in case its interesting for debugging
        pb.directory(p.toAbsolutePath().toFile());
        pb.redirectErrorStream(true);
        pb.redirectOutput(Redirect.appendTo(l));
        Process process = pb.start();
        final int waitTime = 10;
        process.waitFor(waitTime, TimeUnit.SECONDS);
        return process.exitValue();
    }

    /**
     * Get the name of the shaded jar based on the version in the pom.
     *
     * @return the name of the shaded jar
     */
    String getShadedJarName() throws IOException {
        final Properties properties = new Properties();
        properties.load(this.getClass().getClassLoader().getResourceAsStream("resources.properties"));
        return "test-environment-".concat(properties.getProperty("shaded.jar.version")).concat(".jar");
    }
}

