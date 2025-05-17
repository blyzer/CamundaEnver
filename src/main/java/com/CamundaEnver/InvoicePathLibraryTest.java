package com.CamundaEnver;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

/**
 * Unit tests for InvoicePathLibrary and InvoicePathService.
 */
/**
 * Unit tests for the InvoicePathLibrary and InvoicePathService classes.
 * This class contains tests to validate the functionality of finding paths
 * between nodes in an invoice processing workflow.
 */
public class InvoicePathLibraryTest {

    private static final String TEST_URL = "https://n35ro2ic4d.execute-api.eu-central-1.amazonaws.com/prod/engine-rest/process-definition/key/invoice/xml";
    private static final String START = "StartEvent_1";
    private static final String END = "invoiceProcessed";

    /**
     * Tests the integration of the InvoicePathLibrary's findPath method.
     * It verifies that a valid path can be found from the start node to the end node,
     * and checks that the path is not null, not empty, and starts and ends with the correct nodes.
     *
     * @throws Exception if an error occurs during the path finding process
     */
    @Test
    public void testFindPathIntegration() throws Exception {
        try (InvoicePathLibrary lib = new InvoicePathLibrary(TEST_URL, 5000, 3)) {
            InvoicePathLibrary.PathResult result = lib.findPath(START, END);
            assertTrue(result.success(), "Expected success result");
            List<String> path = result.path();
            assertNotNull(path, "Path should not be null");
            assertFalse(path.isEmpty(), "Path should not be empty");
            assertEquals(START, path.get(0), "Path must start with the start node");
            assertEquals(END, path.get(path.size() - 1), "Path must end with the end node");
        }
    }

    /**
     * Tests the InvoicePathService's findPath method.
     * It checks that a valid path can be found from the start node to the end node,
     * and verifies that the path is not null, not empty, and starts and ends with the correct nodes.
     *
     * @throws Exception if an error occurs during the path finding process
     */
    @Test
    public void testFindPathService() throws Exception {
        InvoicePathService service = new InvoicePathService(TEST_URL, 5000, 3);
        List<String> path = service.findPath(START, END);
        assertNotNull(path, "Service path should not be null");
        assertFalse(path.isEmpty(), "Service path should not be empty");
        assertEquals(START, path.get(0));
        assertEquals(END, path.get(path.size() - 1));
    }

    /**
     * Tests the behavior of the InvoicePathLibrary when invalid nodes are provided.
     * It verifies that the findPath method returns a failure result and an empty path
     * when the start and end nodes are invalid.
     *
     * @throws Exception if an error occurs during the path finding process
     */
    @Test
    public void testInvalidNode() throws Exception {
        try (InvoicePathLibrary lib = new InvoicePathLibrary(TEST_URL, 5000, 3)) {
            InvoicePathLibrary.PathResult result = lib.findPath("invalidStart", "invalidEnd");
            assertFalse(result.success(), "Expected failure result");
            assertTrue(result.path().isEmpty(), "Expected empty path for invalid nodes");
        }
    }
}
