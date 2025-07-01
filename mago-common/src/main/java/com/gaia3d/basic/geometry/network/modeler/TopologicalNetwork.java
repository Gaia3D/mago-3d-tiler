package com.gaia3d.basic.geometry.network.modeler;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TopologicalNetwork {
    // topological network.
    // this can be a road network, a river network, a stream network, a canal network, a railway network,
    // a power line network, a gas line network, a water line network, a sewer line network, a telephone line network, a cable line network, etc.
    private List<TopologicalNode> nodes;
    private List<TopologicalEdge> edges;

    public TopologicalNetwork() {
        nodes = new ArrayList<>();
        edges = new ArrayList<>();
    }

    public void makeTopologicalEdgesListForTopologicalNodes() {
        // 1rst, clear the edges list.
        for (TopologicalNode node : nodes) {
            node.getEdges().clear();
        }

        // now, for each tedge, add it to the corresponding tnode.
        for (TopologicalEdge edge : edges) {
            edge.getStartNode().addEdge(edge);
            edge.getEndNode().addEdge(edge);
        }
    }

}
