package com.gaia3d.basic.geometry.modifier;

import org.joml.Matrix4d;

public class TraversalContext {
    private final Matrix4d worldMatrix;

    public TraversalContext(Matrix4d worldMatrix) {
        this.worldMatrix = worldMatrix;
    }

    public Matrix4d worldMatrix() {
        return worldMatrix;
    }

    public TraversalContext withWorld(Matrix4d newWorld) {
        return new TraversalContext(newWorld);
    }
}
