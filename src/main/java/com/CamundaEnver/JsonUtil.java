package com.CamundaEnver;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Utility class for JSON operations.
 * This class provides shared functionality for handling JSON data using the Jackson library.
 * It contains a single instance of ObjectMapper for JSON serialization and deserialization.
 * The constructor is private to prevent instantiation of this utility class.
 */
public final class JsonUtil {
    /**
     * The ObjectMapper instance used for converting Java objects to JSON and vice versa.
     */
    public static final ObjectMapper MAPPER = new ObjectMapper();

    // Private constructor to prevent instantiation
    private JsonUtil() {}
}