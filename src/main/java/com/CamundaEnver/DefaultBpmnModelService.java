package com.CamundaEnver;

import org.camunda.bpm.model.bpmn.Bpmn;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Parses BPMN XML into model and builds node map.
 */
/**
 * Service class for handling BPMN (Business Process Model and Notation) models.
 * This class provides functionality to parse BPMN XML into a model instance
 * and to build a map of flow nodes from the parsed model.
 */
public class DefaultBpmnModelService {

    /**
     * Parses a BPMN XML string into a BpmnModelInstance.
     *
     * @param xml the BPMN XML string to be parsed
     * @return a BpmnModelInstance representing the parsed BPMN model
     */
    public BpmnModelInstance parseModel(String xml) {
        return Bpmn.readModelFromStream(
                new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8))
        );
    }

    /**
     * Builds a map of flow nodes from the given BpmnModelInstance.
     *
     * @param model the BpmnModelInstance from which to build the node map
     * @return a map where the keys are flow node IDs and the values are the corresponding FlowNode objects
     */
    public Map<String, FlowNode> buildNodeMap(BpmnModelInstance model) {
        Collection<FlowNode> nodes = model.getModelElementsByType(FlowNode.class);
        Map<String, FlowNode> map = new HashMap<>(nodes.size());
        for (FlowNode n : nodes) map.put(n.getId(), n);
        return map;
    }
}
