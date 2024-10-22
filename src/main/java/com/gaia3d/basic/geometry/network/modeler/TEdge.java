package com.gaia3d.basic.geometry.network.modeler;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TEdge {
    // topological edge.
    // this can be a line, an arc, a pipe, a road, a river, a stream, a canal, a railway, a power line, a gas line, a water line, a sewer line, a telephone line, a cable line, etc.
    private int id = -1;
    private String guid = "";
    private TNode startNode;
    private TNode endNode;

    public TEdge() {
        startNode = new TNode();
        endNode = new TNode();
    }

    public TEdge(TNode startNode, TNode endNode) {
        this.startNode = startNode;
        this.endNode = endNode;
    }

    public TNode getTheAnotherNode(TNode node) {
        if (node == startNode) {
            return endNode;
        } else if (node == endNode) {
            return startNode;
        } else {
            return null;
        }
    }

}
