package com.CamundaEnver;

/**
 * Utility for reading configuration from environment variables or system properties.
 * This class provides methods to retrieve integer values from the environment or system properties,
 * falling back to a default value if neither source provides a valid integer.
 */
public class ConfigUtil {

    /**
     * Parses an integer value from the specified environment variable or system property.
     *
     * @param envKey    the key of the environment variable to check
     * @param propKey   the key of the system property to check
     * @param defaultVal the default value to return if neither the environment variable nor the system property is set or valid
     * @return the parsed integer value from the environment variable or system property, or the default value if neither is valid
     */
    public static int parseEnvOrProp(String envKey, String propKey, int defaultVal) {
        String env = System.getenv(envKey);
        if (env != null) {
            try { return Integer.parseInt(env); } catch (NumberFormatException ignored) {}
        }
        String prop = System.getProperty(propKey);
        if (prop != null) {
            try { return Integer.parseInt(prop); } catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }
}
