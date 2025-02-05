package com.gaia3d.basic.geometry.network.modeler;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TopologicalEdge {
    // topological edge.
    // this can be a line, an arc, a pipe, a road, a river, a stream, a canal, a railway, a power line, a gas line, a water line, a sewer line, a telephone line, a cable line, etc.
    private int id = -1;
    private String guid = "";
    private TopologicalNode startNode;
    private TopologicalNode endNode;

    public TopologicalEdge() {
        startNode = new TopologicalNode();
        endNode = new TopologicalNode();
    }

    public TopologicalEdge(TopologicalNode startNode, TopologicalNode endNode) {
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public TopologicalNode getTheAnotherNode(TopologicalNode node) {
        if (node == startNode) {
            return endNode;
        } else if (node == endNode) {
            return startNode;
        } else {
            return null;
        }
    }

}
