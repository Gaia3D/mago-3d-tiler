package com.gaia3d.engine.scene;

import lombok.Getter;
import org.joml.Matrix4f;

@Getter
public class Projection {
    private static final float FOV = (float) Math.toRadians(90.0f);
    private static final float Z_FAR = 10000.f;
    private static final float Z_NEAR = 0.01f;

    private Matrix4f projMatrix;

    public Projection(int width, int height) {
        projMatrix = new Matrix4f();
        updateProjMatrix(width, height);
    }

    public Matrix4f getProjMatrix() {
        return projMatrix;
    }

    public void updateProjMatrix(int width, int height) {
        projMatrix.setPerspective(FOV, (float) width / height, Z_NEAR, Z_FAR);
    }
}