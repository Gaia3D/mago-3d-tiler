package com.gaia3d.basic.geometry.networkStructure.modeler;

import java.util.ArrayList;
import java.util.List;

public class TNetwork
{
    // topological network.
    // this can be a road network, a river network, a stream network, a canal network, a railway network,
    // a power line network, a gas line network, a water line network, a sewer line network, a telephone line network, a cable line network, etc.
    private List<TNode> nodes;
    private List<TEdge> edges;
    public TNetwork() {
        nodes = new ArrayList<TNode>();
        edges = new ArrayList<TEdge>();
    }

    public List<TNode> getNodes() {
        return nodes;
    }

    public void setNodes(List<TNode> nodes) {
        this.nodes = nodes;
    }

    public List<TEdge> getEdges() {
        return edges;
    }

    public void setEdges(List<TEdge> edges) {
        this.edges = edges;
    }

    public void makeTEdgesListForTNodes()
    {
        // 1rst, clear the edges list.
        for (int i = 0; i < nodes.size(); i++) {
            TNode node = nodes.get(i);
            node.getEdges().clear();
        }

        // now, for each tedge, add it to the corresponding tnode.
        for (int i = 0; i < edges.size(); i++) {
            TEdge edge = edges.get(i);
            edge.getStartNode().addEdge(edge);
            edge.getEndNode().addEdge(edge);
        }
    }

}
