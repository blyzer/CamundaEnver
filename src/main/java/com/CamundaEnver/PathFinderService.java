package com.CamundaEnver;

import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.SequenceFlow;
import java.util.*;

/**
 * BFS shortest path finder.
 */
/**
 * Service class for finding the shortest path using Breadth-First Search (BFS) algorithm.
 */
public class PathFinderService {

    /**
     * Finds the shortest path from the start node to the end node in a given map of flow nodes.
     *
     * @param map  A map where the key is a string identifier of the flow node and the value is the FlowNode object.
     * @param start The identifier of the starting node.
     * @param end   The identifier of the ending node.
     * @return A list of strings representing the nodes in the shortest path from start to end, or an empty list if no path exists.
     */
    public List<String> findShortestPath(Map<String, FlowNode> map, String start, String end) {
        if (start.equals(end)) return Collections.singletonList(start);
        Deque<List<String>> queue = new ArrayDeque<>();
        Set<String> visited = new HashSet<>();
        queue.add(Collections.singletonList(start));
        visited.add(start);
        while (!queue.isEmpty()) {
            List<String> path = queue.poll();
            FlowNode node = map.get(path.get(path.size() - 1));
            if (node == null) continue;
            for (SequenceFlow f : node.getOutgoing()) {
                String next = f.getTarget().getId();
                if (visited.contains(next)) continue;
                List<String> np = new ArrayList<>(path); np.add(next);
                if (next.equals(end)) return np;
                visited.add(next);
                queue.add(np);
            }
        }
        return Collections.emptyList();
    }
}
