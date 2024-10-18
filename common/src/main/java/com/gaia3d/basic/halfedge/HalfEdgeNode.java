package com.gaia3d.basic.halfedge;

import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class HalfEdgeNode {
    private HalfEdgeNode parent = null;
    private Matrix4d transformMatrix = new Matrix4d();
    private Matrix4d preMultipliedTransformMatrix = new Matrix4d();
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

    public Matrix4d getFinalTransformMatrix() {
        Matrix4d finalMatrix = new Matrix4d();
        finalMatrix.set(transformMatrix);
        if (parent != null) {
            finalMatrix.mul(parent.getFinalTransformMatrix());
        }
        return finalMatrix;
    }

    public void spendTransformationMatrix() {
        Matrix4d finalMatrix = getFinalTransformMatrix();
        Matrix4d identity = new Matrix4d();
        identity.identity();

        if(finalMatrix.equals(identity)) {
            return;
        }

        for (HalfEdgeMesh mesh : meshes) {
            mesh.transformPoints(finalMatrix);
        }
        for (HalfEdgeNode child : children) {
            child.spendTransformationMatrix();
        }

        // Clear the transform matrix.
        transformMatrix.identity();
    }
}
