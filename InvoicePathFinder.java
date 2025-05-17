import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A tool to find the shortest path between two BPMN nodes in a process model.
 * Downloads the model from a configurable URL and uses BFS for pathfinding.
 */
public class InvoicePathFinder {
    private static final Logger logger = LoggerFactory.getLogger(InvoicePathFinder.class);

    public static void main(String[] args) {
        int code = run(args);
        if (args.length != 2) {
            System.err.println("Usage: java -jar InvoicePathFinder.jar <startNodeId> <endNodeId>");
            System.exit(1);
        }
        System.exit(code);
    }

    /**
     * Main application logic that orchestrates fetching, parsing, and searching the BPMN model.
     */
    public static int run(String[] args) {
        if (args.length != 2) {
            logger.error("Usage: java -jar InvoicePathFinder.jar <startNodeId> <endNodeId>");
            return 1;
        }
        String startId = args[0], endId = args[1];

        // Determine BPMN XML URL from environment variable or system property
        String url = Optional.ofNullable(System.getenv("BPMN_URL"))
                .orElseGet(() -> System.getProperty("bpmn.url",
                        "https://n35ro2ic4d.execute-api.eu-central-1.amazonaws.com/prod/engine-rest/process-definition/key/invoice/xml"));

        // Read timeout and retry config
        int timeoutMs = parseEnvOrProp("HTTP_TIMEOUT_MS", "http.timeout.ms", 5000);
        int maxRetries = parseEnvOrProp("HTTP_MAX_RETRIES", "http.max.retries", 3);

        try (ApacheBpmnFetcher fetcher = new ApacheBpmnFetcher(url, timeoutMs, maxRetries)) {
            Runtime.getRuntime().addShutdownHook(new Thread(fetcher::close));

            DefaultBpmnModelService modelService = new DefaultBpmnModelService();
            PathFinderService pathFinder = new PathFinderService();

            // Download and parse the BPMN XML
            String xml = fetcher.fetchXml();
            BpmnModelInstance model = modelService.parseModel(xml);
            Map<String, FlowNode> nodeMap = modelService.buildNodeMap(model);

            // Ensure both nodes exist in the model
            if (!nodeMap.containsKey(startId) || !nodeMap.containsKey(endId)) {
                logger.error("Node IDs not found: start={}, end={}", startId, endId);
                return 2;
            }

            // Compute shortest path using BFS
            List<String> path = pathFinder.findShortestPath(nodeMap, startId, endId);
            if (path.isEmpty()) {
                logger.warn("No path found from {} to {}", startId, endId);
                return 3;
            }

            logger.info("Shortest path from {} to {}: {}", startId, endId, path);
            return 0;
        } catch (Exception e) {
            logger.error("Error during path finding", e);
            return 99;
        }
    }

    /**
     * Reads an integer configuration value from an environment variable or system property.
     * Falls back to a default if not found or invalid.
     */
    private static int parseEnvOrProp(String envKey, String propKey, int defaultVal) {
        String env = System.getenv(envKey);
        if (env != null) {
            try {
                return Integer.parseInt(env);
            } catch (NumberFormatException e) {
                logger.warn("Invalid value for env {}: '{}'", envKey, env);
            }
        }
        String prop = System.getProperty(propKey);
        if (prop != null) {
            try {
                return Integer.parseInt(prop);
            } catch (NumberFormatException e) {
                logger.warn("Invalid value for system property {}: '{}'", propKey, prop);
            }
        }
        return defaultVal;
    }

    /**
     * Fetches BPMN XML from a remote HTTP endpoint, with support for retry and graceful shutdown.
     */
    public static class ApacheBpmnFetcher implements Runnable, Closeable {
        private final CloseableHttpClient client;
        private final String url;
        private final AtomicBoolean closed = new AtomicBoolean(false);

        public ApacheBpmnFetcher(String url, int timeoutMs, int maxRetries) {
            RequestConfig config = RequestConfig.custom()
                    .setConnectTimeout(timeoutMs)
                    .setSocketTimeout(timeoutMs)
                    .setConnectionRequestTimeout(timeoutMs)
                    .build();

            // Retry only on IO exceptions, and up to maxRetries
            HttpRequestRetryHandler retryHandler = (ex, count, ctx) -> {
                if (count > maxRetries) return false;
                boolean retry = ex instanceof IOException;
                if (!retry) {
                    logger.warn("Not retrying due to non-retryable exception: {}", ex.getClass().getSimpleName());
                }
                return retry;
            };

            this.client = HttpClients.custom()
                    .setDefaultRequestConfig(config)
                    .setRetryHandler(retryHandler)
                    .build();
            this.url = url;
        }

        /**
         * Executes the HTTP request and extracts the BPMN XML content.
         */
        public String fetchXml() throws IOException {
            HttpGet get = new HttpGet(url);
            get.addHeader("Accept", "application/json");
            HttpResponse resp = client.execute(get);
            int code = resp.getStatusLine().getStatusCode();
            if (code != 200) throw new IOException("HTTP " + code);
            HttpEntity entity = resp.getEntity();
            if (entity == null) throw new IOException("Empty response");
            try (InputStream is = entity.getContent()) {
                JsonNode root = JsonUtil.MAPPER.readTree(is);
                JsonNode xmlNode = root.get("bpmn20Xml");
                if (xmlNode == null) throw new IOException("Missing bpmn20Xml field");
                return xmlNode.asText();
            }
        }

        @Override
        public void run() {
            close();
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                try {
                    client.close();
                } catch (IOException e) {
                    logger.warn("Error closing HTTP client", e);
                }
            }
        }
    }

    /**
     * Service class that parses the BPMN model and builds a map of all nodes for quick lookup.
     */
    public static class DefaultBpmnModelService {
        public BpmnModelInstance parseModel(String xml) {
            return Bpmn.readModelFromStream(
                    new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
            );
        }

        /**
         * Recursively collects all FlowNodes from the model, including nested ones in SubProcesses.
         */
        public Map<String, FlowNode> buildNodeMap(BpmnModelInstance model) {
            Map<String, FlowNode> map = new HashMap<>();
            Collection<FlowNode> topLevelNodes = model.getModelElementsByType(FlowNode.class);
            for (FlowNode node : topLevelNodes) {
                collectFlowNodes(node, map);
            }
            return map;
        }

        private void collectFlowNodes(FlowNode node, Map<String, FlowNode> map) {
            if (node.getId() != null && !map.containsKey(node.getId())) {
                map.put(node.getId(), node);
            }

            // Recursively include nodes from SubProcesses
            if (node instanceof org.camunda.bpm.model.bpmn.instance.SubProcess) {
                org.camunda.bpm.model.bpmn.instance.SubProcess subProcess =
                        (org.camunda.bpm.model.bpmn.instance.SubProcess) node;
                for (FlowNode childNode : subProcess.getFlowElements().stream()
                        .filter(e -> e instanceof FlowNode)
                        .map(e -> (FlowNode) e)
                        .toList()) {
                    collectFlowNodes(childNode, map);
                }
            }
        }
    }

    /**
     * Service that performs a BFS traversal to find the shortest path between two nodes.
     */
    public static class PathFinderService {

        public List<String> findShortestPath(Map<String, FlowNode> map, String start, String end) {
            if (start.equals(end)) return Collections.singletonList(start);

            Deque<List<String>> queue = new ArrayDeque<>();
            Set<String> visited = new HashSet<>();
            Map<String, Integer> gatewayVisitCounts = new HashMap<>();

            queue.add(Collections.singletonList(start));
            visited.add(start);

            while (!queue.isEmpty()) {
                List<String> path = queue.poll();
                String lastId = path.get(path.size() - 1);
                FlowNode currentNode = map.get(lastId);
                if (currentNode == null) continue;

                for (SequenceFlow flow : currentNode.getOutgoing()) {
                    FlowNode nextNode = flow.getTarget();
                    String nextId = nextNode.getId();

                    // If we're in an Event-Based Gateway, pick only the first path
                    if (currentNode instanceof org.camunda.bpm.model.bpmn.instance.EventBasedGateway) {
                        List<String> newPath = getCasesOutgoingFlow(end, queue, visited, path, nextId);
                        if (newPath != null) return newPath;
                        break;
                    }

                    // For join gateways, wait until all incoming paths are visited
                    if (nextNode instanceof org.camunda.bpm.model.bpmn.instance.ParallelGateway ||
                            nextNode instanceof org.camunda.bpm.model.bpmn.instance.InclusiveGateway) {

                        long totalIncoming = nextNode.getIncoming().stream().count();
                        int count = gatewayVisitCounts.getOrDefault(nextId, 0) + 1;
                        gatewayVisitCounts.put(nextId, count);

                        if (count < totalIncoming) {
                            continue;
                        }
                    }

                    List<String> newPath = getCasesOutgoingFlow(end, queue, visited, path, nextId);
                    if (newPath != null) return newPath;
                }
            }

            return Collections.emptyList();
        }

        /**
         * Helper method to construct new paths and track visited nodes.
         */
        private List<String> getCasesOutgoingFlow(String end, Deque<List<String>> queue, Set<String> visited, List<String> path, String nextId) {
            if (!visited.contains(nextId)) {
                List<String> newPath = new ArrayList<>(path);
                newPath.add(nextId);
                if (nextId.equals(end)) return newPath;
                visited.add(nextId);
                queue.add(newPath);
            }
            return null;
        }
    }

    /**
     * Shared singleton ObjectMapper for JSON parsing.
     */
    public static final class JsonUtil {
        public static final ObjectMapper MAPPER = new ObjectMapper();

        private JsonUtil() {}
    }
}