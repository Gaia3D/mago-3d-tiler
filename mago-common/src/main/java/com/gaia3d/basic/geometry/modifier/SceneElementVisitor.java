package com.gaia3d.basic.geometry.modifier;

import com.gaia3d.basic.model.*;

@Deprecated
public interface SceneElementVisitor {

    default void visitScene(GaiaScene scene, TraversalContext ctx) {}

    default void visitNode(GaiaNode node, TraversalContext ctx) {}

    default void visitMesh(GaiaMesh mesh, TraversalContext ctx) {}

    default void visitPrimitive(GaiaPrimitive primitive, TraversalContext ctx) {}

    default void visitSurface(GaiaSurface surface, TraversalContext ctx) {}

    default void visitFace(GaiaFace face, TraversalContext ctx) {}

}
