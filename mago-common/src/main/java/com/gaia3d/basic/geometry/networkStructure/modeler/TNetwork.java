package com.gaia3d.basic.geometry.networkStructure.modeler;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TNetwork {
    // topological network.
    // this can be a road network, a river network, a stream network, a canal network, a railway network,
    // a power line network, a gas line network, a water line network, a sewer line network, a telephone line network, a cable line network, etc.
    private List<TNode> nodes;
    private List<TEdge> edges;

    public TNetwork() {
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
    }

    public void makeTEdgesListForTNodes() {
        // 1rst, clear the edges list.
        for (TNode node : nodes) {
            node.getEdges().clear();
        }

        // now, for each tedge, add it to the corresponding tnode.
        for (TEdge edge : edges) {
            edge.getStartNode().addEdge(edge);
            edge.getEndNode().addEdge(edge);
        }
    }

}
