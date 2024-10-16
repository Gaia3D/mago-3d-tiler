package com.gaia3d.basic.halfedge;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class HalfEdgeNode {
    private HalfEdgeNode parent = null;
    private List<HalfEdgeMesh> meshes = new ArrayList<>();
    private List<HalfEdgeNode> children = new ArrayList<>();

    public void doTrianglesReduction() {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.doTrianglesReduction();
        }
        for (HalfEdgeNode child : children) {
            child.doTrianglesReduction();
        }
    }

    public void deleteObjects() {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.deleteObjects();
        }
        meshes.clear();
        for (HalfEdgeNode child : children) {
            child.deleteObjects();
        }
        children.clear();
    }

    public void checkSandClockFaces() {
        for (HalfEdgeMesh mesh : meshes) {
            mesh.checkSandClockFaces();
        }
        for (HalfEdgeNode child : children) {
            child.checkSandClockFaces();
        }
    }
}
