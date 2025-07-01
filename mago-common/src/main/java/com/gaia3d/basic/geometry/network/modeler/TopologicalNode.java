package com.gaia3d.basic.geometry.network.modeler;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TopologicalNode {
    // topological node.
    // this can be a point, an arc, a pipe elbow, a pipe tee, a pipe cross, a pipe reducer, a pipe valve,
    // a pipe pump, a pipe tank, a pipe reservoir, a pipe junction, a pipe source, a pipe sink, a pipe pipe, etc.
    private int id = -1;
    private String guid = "";
    private Vector3d position;
    private List<TopologicalEdge> edges = new ArrayList<>();

    public TopologicalNode() {
        position = new Vector3d();
    }

    public TopologicalNode(Vector3d position) {
        this.position = position;
    }

    public TopologicalNode(double x, double y, double z) {
        this.position = new Vector3d(x, y, z);
    }

    public void addEdge(TopologicalEdge edge) {
        edges.add(edge);
    }

    public TopologicalEdge getEdge(int index) {
        if (index < 0 || index >= edges.size()) return null;
        return edges.get(index);
    }

}
