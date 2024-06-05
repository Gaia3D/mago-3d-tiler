package com.gaia3d.basic.geometry.networkStructure.modeler;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class TNode {
    // topological node.
    // this can be a point, an arc, a pipe elbow, a pipe tee, a pipe cross, a pipe reducer, a pipe valve,
    // a pipe pump, a pipe tank, a pipe reservoir, a pipe junction, a pipe source, a pipe sink, a pipe pipe, etc.
    private int id = -1;
    private String guid = "";
    private Vector3d position;
    private List<TEdge> edges = new ArrayList<>();

    public TNode() {
        position = new Vector3d();
    }

    public TNode(Vector3d position) {
        this.position = position;
    }

    public TNode(double x, double y, double z) {
        this.position = new Vector3d(x, y, z);
    }

    public void addEdge(TEdge edge) {
        edges.add(edge);
    }

    public TEdge getEdge(int index) {
        if (index < 0 || index >= edges.size()) return null;
        return edges.get(index);
    }

}
