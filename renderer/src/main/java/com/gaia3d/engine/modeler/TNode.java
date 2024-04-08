package com.gaia3d.engine.modeler;

import lombok.Getter;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

public class TNode {
    // topological node.
    // this can be a point, an arc, a pipe elbow, a pipe tee, a pipe cross, a pipe reducer, a pipe valve,
    // a pipe pump, a pipe tank, a pipe reservoir, a pipe junction, a pipe source, a pipe sink, a pipe pipe, etc.
    int id = -1;
    String guid = "";
    Vector3d position;

    @Getter
    List<TEdge> edges = new ArrayList<TEdge>();

    public TNode() {
        position = new Vector3d();
    }

    public TNode(Vector3d position) {
        this.position = position;
    }

    public TNode(double x, double y, double z) {
        this.position = new Vector3d(x, y, z);
    }

    public Vector3d getPosition() {
        return position;
    }

    public void setPosition(Vector3d position) {
        this.position = position;
    }

    public void addEdge(TEdge edge) {
        edges.add(edge);
    }

    public TEdge getEdge(int index) {
        if(index < 0 || index >= edges.size())
            return null;
        return edges.get(index);
    }

}
