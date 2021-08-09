package com.melahn.util.helm;

import java.util.Locale;
import java.util.Map;

public final class ChartUtil {

    /**
     * Not called
     */
    private ChartUtil() {

    }

    /**
     * Recursively searches the values map and finds a value for a given key
     *
     * @param k      key whose value is to be retrieved
     * @param values a map of values such as from a values.yaml file
     * @return the value of the key
     */
    @SuppressWarnings("unchecked")
    public static Object getValue(String k, Map<String, Object> values) {
        String head = null;
        String tail = null;
        if (k != null && values != null) {
            head = getHead(k);
            tail = getTail(k);
        }
        if (head != null && !head.isEmpty()) {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                if (head.equals(entry.getKey()) && !(entry.getValue() instanceof Map)) {
                    return entry.getValue(); // end the recursion
                } else if (head.equals(entry.getKey()) && !tail.isEmpty()) {
                    return getValue(tail, (Map<String, Object>) entry.getValue()); // continue the recursion
                }
            }
        }
        return null;
    }

    /**
     * Extracts the leading portion of a string
     * 
     * @param k the string from which the extraction should occur
     * @return returns a String with the leading segment of the string up but not
     *         including the dot. If the string does not contains a dot returns the
     *         input string.
     */
    private static String getHead(String k) {
        if ((k.contains("."))) {
            return k.substring(0, k.indexOf('.')); // pop the first segment off the key string
        } else {
            return k;
        }
    }

    /**
     * Extracts the tail portion of a string
     * 
     * @param k the string from which the extraction should occur
     * @return returns a String with the trailing segment of the string starting
     *         with the position just past the dot. If the string does not contains
     *         a dot returns the empty string.
     */
    private static String getTail(String k) {
        if ((k.contains("."))) {
            return k.substring(k.indexOf('.') + 1, k.length()); // pop the last segment off the key string
        } else {
            return "";
        }
    }

    public enum OSType {
        WINDOWS, MACOS, LINUX, OTHER
      }

    /**
     * Used to derive the current OS using the os.name
     * system property.  I use this instead of SystemUtils
     * because I want to be able to set the OS name dynamically
     * using a system property to allow for or more complete 
     * test coverage.
     * 
     * I don't cache the value in this class because I wamt to be able to 
     * spoof other OS types than the real type, again for
     * test purposes.
     * 
     * @return the operating system 
     */
    public static OSType getOSType() {
        String s = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);  // no default
        if (s.indexOf("mac") >= 0)  {
            return OSType.MACOS;
        } else if (s.indexOf("win") >= 0) { // ignore darwin
            return OSType.WINDOWS;
        } else if (s.indexOf("nux") >= 0) {
            return OSType.LINUX;
        } else {
            return OSType.OTHER;
        }
    }
}
