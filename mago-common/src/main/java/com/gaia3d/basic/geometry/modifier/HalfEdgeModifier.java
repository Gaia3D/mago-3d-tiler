package com.gaia3d.basic.geometry.modifier;

import com.gaia3d.basic.halfedge.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.util.List;

@Slf4j
abstract public class HalfEdgeModifier {

    public void apply(HalfEdgeScene scene) {
        List<HalfEdgeNode> rootNodes = scene.getNodes();
        for (HalfEdgeNode node : rootNodes) {
            applyNode(new Matrix4d(), node);
        }
    }

    public void applyNode(Matrix4d parentTransformMatrix, HalfEdgeNode node) {
        Matrix4d localTransformMatrix = node.getTransformMatrix();
        Matrix4d productTransformMatrix = new Matrix4d(parentTransformMatrix);
        productTransformMatrix.mul(localTransformMatrix);
        List<HalfEdgeNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (HalfEdgeNode child : children) {
                applyNode(productTransformMatrix, child);
            }
        }

        List<HalfEdgeMesh> meshes = node.getMeshes();
        for (HalfEdgeMesh mesh : meshes) {
            applyMesh(productTransformMatrix, mesh);
        }
    }

    public void applyMesh(Matrix4d productTransformMatrix, HalfEdgeMesh mesh) {
        List<HalfEdgePrimitive> primitives = mesh.getPrimitives();
        for (HalfEdgePrimitive primitive : primitives) {
            applyPrimitive(productTransformMatrix, primitive);
        }
    }

    public void applyPrimitive(Matrix4d productTransformMatrix, HalfEdgePrimitive primitive) {
        List<HalfEdgeVertex> vertices = primitive.getVertices();
        for (HalfEdgeVertex vertex : vertices) {
            applyVertex(productTransformMatrix, vertex);
        }

        List<HalfEdgeSurface> surfaces = primitive.getSurfaces();
        for (HalfEdgeSurface surface : surfaces) {
            // Note: In HalfEdgeScene, the vertices are managed by surfaces, no by primitives
            List<HalfEdgeVertex> surfaceVertices = surface.getVertices();
            applySurface(productTransformMatrix, surfaceVertices, surface);
        }
    }

    public void applyVertex(Matrix4d productTransformMatrix, HalfEdgeVertex vertex) {

    }

    public void applySurface(Matrix4d productTransformMatrix, List<HalfEdgeVertex> vertices, HalfEdgeSurface surface) {
        List<HalfEdgeFace> faces = surface.getFaces();
        for (HalfEdgeFace face : faces) {
            applyFace(productTransformMatrix, vertices, face);
        }
    }

    public void applyFace(Matrix4d productTransformMatrix, List<HalfEdgeVertex> vertices, HalfEdgeFace face) {

    }
}
