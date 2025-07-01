package com.gaia3d.renderer.engine.scene;

import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4f;

@Getter
@Setter
public class Projection {
    private int projectionType; // 0: perspective, 1: orthographic

    // perspective
    private float FOV = (float) Math.toRadians(90.0f);
    private float Z_FAR = 10000.f;
    private float Z_NEAR = 0.01f;
    private int width;
    private int height;

    // orthographic
    private float left;
    private float right;
    private float bottom;
    private float top;
    private float near;
    private float far;

    private Matrix4f projMatrix;

    public Projection(int projectionType, int width, int height) {
        this.projectionType = projectionType;
        this.width = width;
        this.height = height;
        projMatrix = new Matrix4f();
        updateProjMatrix(width, height);
    }

    public void setProjectionType(int projectionType) {
        this.projectionType = projectionType;
        this.updateProjMatrix(width, height);
    }

    public void setProjectionOrthographic(float left, float right, float bottom, float top, float near, float far) {
        this.projectionType = 1;
        this.left = left;
        this.right = right;
        this.bottom = bottom;
        this.top = top;
        this.near = near;
        this.far = far;

        projMatrix.setOrtho(left, right, bottom, top, near, far);
    }

    public void setProjectionPerspective(float FOV, float Z_NEAR, float Z_FAR) {
        this.projectionType = 0;
        this.FOV = FOV;
        this.Z_NEAR = Z_NEAR;
        this.Z_FAR = Z_FAR;
    }

    public void updateProjMatrix(int width, int height) {
        if (projectionType == 1) {
            projMatrix.setOrtho(left, right, bottom, top, near, far);
        } else {
            projMatrix.setPerspective(FOV, (float) width / height, Z_NEAR, Z_FAR);
        }
    }
}
