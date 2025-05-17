package com.CamundaEnver;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Reusable library for finding BPMN node paths.
 * This class provides functionality to fetch BPMN models and find the shortest path
 * between two nodes identified by their IDs.
 */
public class InvoicePathLibrary implements AutoCloseable {
    public static final String DEFAULT_URL = "https://n35ro2ic4d.execute-api.eu-central-1.amazonaws.com/prod/engine-rest/process-definition/key/invoice/xml";

    private final ApacheBpmnFetcher fetcher; // Fetcher for retrieving BPMN XML data
    private final DefaultBpmnModelService modelService = new DefaultBpmnModelService(); // Service for parsing BPMN models
    private final PathFinderService pathFinder = new PathFinderService(); // Service for finding paths in the BPMN model

    /**
     * Constructs an InvoicePathLibrary with the specified parameters.
     *
     * @param url        the URL to fetch the BPMN XML from
     * @param timeoutMs  the timeout in milliseconds for fetching the XML
     * @param maxRetries the maximum number of retries for fetching the XML
     */
    public InvoicePathLibrary(String url, int timeoutMs, int maxRetries) {
        this.fetcher = new ApacheBpmnFetcher(url, timeoutMs, maxRetries);
        // Adds a shutdown hook to ensure that the fetcher is closed when the JVM shuts down
        Runtime.getRuntime().addShutdownHook(new Thread(fetcher::close));
    }

    /**
     * Finds the shortest path between two nodes in the BPMN model.
     *
     * @param startId the ID of the starting node
     * @param endId   the ID of the ending node
     * @return a PathResult containing success status, message, and the path as a list of node IDs
     * @throws Exception if an error occurs during the fetching or processing of the BPMN model
     */
    public PathResult findPath(String startId, String endId) throws Exception {
        try (fetcher) {
            String xml = fetcher.fetchXml(); // Fetch the BPMN XML
            BpmnModelInstance model = modelService.parseModel(xml); // Parse the XML into a BPMN model
            Map<String, FlowNode> nodeMap = modelService.buildNodeMap(model); // Build a map of nodes

            // Validate the provided node IDs
            if (!nodeMap.containsKey(startId) || !nodeMap.containsKey(endId)) {
                return new PathResult(false, "Invalid node IDs", List.of());
            }
            // Find the shortest path between the start and end nodes
            List<String> path = pathFinder.findShortestPath(nodeMap, startId, endId);
            if (path.isEmpty()) {
                return new PathResult(false, "No path found", List.of());
            }
            return new PathResult(true, "Path found", path); // Return the successful path result
        }
    }

    /**
     * Converts the PathResult to a JSON string.
     *
     * @param result the PathResult to convert
     * @return a JSON string representation of the PathResult
     * @throws IOException if an error occurs during JSON serialization
     */
    public static String toJson(PathResult result) throws IOException {
        return new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(result);
    }

    /**
     * Closes the InvoicePathLibrary and releases any resources held by it.
     */
    @Override
    public void close() {
        fetcher.close(); // Close the fetcher to release resources
    }

    /**
     * Simple Data Transfer Object (DTO) for JSON serialization of path results.
     *
     * @param success indicates if the path finding was successful
     * @param message a message providing additional information
     * @param path    a list of node IDs representing the path
     */
    public record PathResult(boolean success, String message, List<String> path) {}

    /**
     * Creates an instance of InvoicePathLibrary using default configuration.
     *
     * @return a new instance of InvoicePathLibrary with default URL, timeout, and retries
     */
    public static InvoicePathLibrary fromDefaults() {
        String url = Optional.ofNullable(System.getenv("BPMN_URL"))
                .orElseGet(() -> System.getProperty("bpmn.url", DEFAULT_URL));
        int timeout = ConfigUtil.parseEnvOrProp("HTTP_TIMEOUT_MS", "http.timeout.ms", 5000);
        int retries = ConfigUtil.parseEnvOrProp("HTTP_MAX_RETRIES", "http.max.retries", 3);
        return new InvoicePathLibrary(url, timeout, retries);
    }
}