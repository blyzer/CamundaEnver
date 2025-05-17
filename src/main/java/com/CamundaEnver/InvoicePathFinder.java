package com.CamundaEnver;

/**
 * The InvoicePathFinder class is responsible for finding a path between two nodes
 * in an invoice processing system. It serves as the entry point for the application.
 */
public class InvoicePathFinder {

    /**
     * The main method that executes the InvoicePathFinder application.
     * It expects two command-line arguments: the start node ID and the end node ID.
     * If the arguments are not provided correctly, it prints the usage message and exits.
     *
     * @param args Command-line arguments where args[0] is the start node ID and args[1] is the end node ID.
     */
    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java -jar InvoicePathFinder.jar <startNodeId> <endNodeId>");
            System.exit(1);
        }
        String start = args[0], end = args[1];
        try (InvoicePathLibrary lib = InvoicePathLibrary.fromDefaults()) {
            InvoicePathLibrary.PathResult result = lib.findPath(start, end);
            System.out.println(InvoicePathLibrary.toJson(result));
            System.exit(result.success() ? 0 : 1);
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(99);
        }
    }
}