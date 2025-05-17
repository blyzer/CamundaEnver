package com.CamundaEnver;

import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import java.util.List;
import java.util.Map;

/**
 * Service wrapper for programmatic use without CLI.
 */
/**
 * The InvoicePathService class provides a service wrapper for finding paths
 * between two nodes in a system without using a command-line interface (CLI).
 */
public class InvoicePathService {
    private final InvoicePathLibrary library;

    /**
     * Constructs an instance of InvoicePathService with the specified parameters.
     *
     * @param url        the URL to be used for the invoice path library
     * @param timeoutMs  the timeout in milliseconds for service requests
     * @param maxRetries the maximum number of retry attempts for failed requests
     */
    public InvoicePathService(String url, int timeoutMs, int maxRetries) {
        this.library = new InvoicePathLibrary(url, timeoutMs, maxRetries);
    }

    /**
     * Finds the path between two nodes identified by their IDs.
     *
     * @param start the ID of the starting node
     * @param end   the ID of the ending node
     * @return a list of node IDs representing the path from start to end
     * @throws Exception if an error occurs during the path finding process
     * @throws IllegalArgumentException if the path finding fails
     */
    public List<String> findPath(String start, String end) throws Exception {
        try (InvoicePathLibrary lib = library) {
            InvoicePathLibrary.PathResult result = lib.findPath(start, end);
            if (!result.success()) {
                throw new IllegalArgumentException(result.message());
            }
            return result.path();
        }
    }
}