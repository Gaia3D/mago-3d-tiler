package com.gaia3d.basic.geometry.modifier;

import com.gaia3d.basic.model.*;
import org.joml.Matrix4d;

public final class SceneTraverser {

    public void traverse(GaiaScene scene, SceneElementVisitor visitor) {
        Matrix4d identity = new Matrix4d().identity();
        TraversalContext rootCtx = new TraversalContext(identity);

        visitor.visitScene(scene, rootCtx);
        for (GaiaNode node : scene.getNodes()) {
            traverseNode(node, rootCtx, visitor);
        }
    }

    private void traverseNode(GaiaNode node, TraversalContext parentCtx, SceneElementVisitor visitor) {
        Matrix4d nodeWorld = new Matrix4d(parentCtx.worldMatrix())
                .mul(node.getTransformMatrix()); 
        TraversalContext nodeCtx = parentCtx.withWorld(nodeWorld);

        visitor.visitNode(node, parentCtx);
        for (GaiaMesh mesh : node.getMeshes()) {
            traverseMesh(mesh, nodeCtx, visitor);
        }
    }

    private void traverseMesh(GaiaMesh mesh, TraversalContext parentCtx, SceneElementVisitor visitor) {
        visitor.visitMesh(mesh, parentCtx);
        for (GaiaPrimitive primitive : mesh.getPrimitives()) {
            traversePrimitive(primitive, parentCtx, visitor);
        }
    }

    private void traversePrimitive(GaiaPrimitive primitive, TraversalContext parentCtx, SceneElementVisitor visitor) {
        visitor.visitPrimitive(primitive, parentCtx);
        for (GaiaSurface surface : primitive.getSurfaces()) {
            traverseSurface(surface, parentCtx, visitor);
        }
    }

    private void traverseSurface(GaiaSurface surface, TraversalContext parentCtx, SceneElementVisitor visitor) {
        visitor.visitSurface(surface, parentCtx);
        for (GaiaFace face : surface.getFaces()) {
            visitor.visitFace(face, parentCtx);
        }
    }

}
