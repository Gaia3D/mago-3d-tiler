package com.gaia3d.basic.geometry.modifier;

import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.util.List;

@Slf4j
abstract public class Modifier {

    public void apply(GaiaScene scene) {
        List<GaiaNode> rootNodes = scene.getNodes();
        for (GaiaNode node : rootNodes) {
            applyNode(new Matrix4d(), node);
        }
    }

    protected void applyNode(Matrix4d parentTransformMatrix, GaiaNode node) {
        Matrix4d localTransformMatrix = node.getTransformMatrix();
        Matrix4d productTransformMatrix = new Matrix4d(parentTransformMatrix);
        productTransformMatrix.mul(localTransformMatrix);
        List<GaiaNode> children = node.getChildren();
        if (children != null && !children.isEmpty()) {
            for (GaiaNode child : children) {
                applyNode(productTransformMatrix, child);
            }
        }

        List<GaiaMesh> meshes = node.getMeshes();
        for (GaiaMesh mesh : meshes) {
            applyMesh(productTransformMatrix, mesh);
        }
    }

    protected void applyMesh(Matrix4d productTransformMatrix, GaiaMesh mesh) {
        List<GaiaPrimitive> primitives = mesh.getPrimitives();
        for (GaiaPrimitive primitive : primitives) {
            applyPrimitive(productTransformMatrix, primitive);
        }
    }

    protected void applyPrimitive(Matrix4d productTransformMatrix, GaiaPrimitive primitive) {
        List<GaiaVertex> vertices = primitive.getVertices();
        for (GaiaVertex vertex : vertices) {
            applyVertex(productTransformMatrix, vertex);
        }

        List<GaiaSurface> surfaces = primitive.getSurfaces();
        for (GaiaSurface surface : surfaces) {
            applySurface(productTransformMatrix, vertices, surface);
        }
    }

    protected void applyVertex(Matrix4d productTransformMatrix, GaiaVertex vertex) {

    }

    protected void applySurface(Matrix4d productTransformMatrix, List<GaiaVertex> vertices, GaiaSurface surface) {
        List<GaiaFace> faces = surface.getFaces();
        for (GaiaFace face : faces) {
            applyFace(productTransformMatrix, vertices, face);
        }
    }

    protected void applyFace(Matrix4d productTransformMatrix, List<GaiaVertex> vertices, GaiaFace face) {

    }
}
