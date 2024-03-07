package com.gaia3d.engine.scene;

import org.joml.*;
import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;

import java.lang.Math;
public class Camera {
    Matrix4d transformMatrix;
    Matrix4d modelViewMatrix;
    Matrix4d rotationMatrix;

    Vector3d position;
    Vector3d rotation;

    Vector3d direction;
    Vector3d up;
    Vector3d right;

    boolean dirty;

    public Camera() {
        this.init();
    }

    public void init() {
        this.dirty = false;
        this.transformMatrix = null;
        this.modelViewMatrix = null;
        this.rotationMatrix = null;

        this.position = new Vector3d(0, 0, 0);
        this.rotation = new Vector3d(0, 0, 0);

        this.direction = new Vector3d(0, 0, -1);
        this.up = new Vector3d(0, 1, 0);
        this.right = new Vector3d(1, 0, 0);
    }

    public Matrix4d getTransformMatrix() {
        if (this.dirty || this.transformMatrix == null) {
            //this.calcRight();
            this.transformMatrix = new Matrix4d(this.right.get(0), this.right.get(1), this.right.get(2), 0,
                    this.up.get(0), this.up.get(1), this.up.get(2), 0,
                    -this.direction.get(0), -this.direction.get(1), -this.direction.get(2), 0,
                    this.position.get(0), this.position.get(1), this.position.get(2), 1);
            this.dirty = false;
        }
        return this.transformMatrix;
    }
    public Matrix4d getModelViewMatrix() {
        if (this.dirty || this.modelViewMatrix == null) {
            Matrix4d transformMatrix = this.getTransformMatrix();
            Matrix4d modelViewMatrix = transformMatrix.invert(new Matrix4d());
            this.modelViewMatrix = modelViewMatrix;
        }
        return this.modelViewMatrix;
    }

    public void rotationOrbit(float xValue, float yValue, Vector3d pivotPosition) {
        Vector3d pitchAxis = this.right;

        Matrix4d headingMatrix = new Matrix4d();
        headingMatrix.rotationZ(xValue);

        Matrix4d pitchMatrix = new Matrix4d();
        pitchMatrix.rotation(yValue, pitchAxis);

        Matrix4d totalRotationMatrix = new Matrix4d(headingMatrix);
        totalRotationMatrix.mul(pitchMatrix);

        Vector3d translatedCameraPosition = new Vector3d(this.position);
        translatedCameraPosition.sub(pivotPosition);

        Vector4d translatedCameraPositionVec4 = new Vector4d(translatedCameraPosition.get(0), translatedCameraPosition.get(1), translatedCameraPosition.get(2), 1.0f);
        Vector4d transformedCameraPosition = new Vector4d(translatedCameraPositionVec4);
        transformedCameraPosition = transformedCameraPosition.mul(totalRotationMatrix);
        Vector3d transformedCameraPositionVec3 = new Vector3d(transformedCameraPosition.get(0), transformedCameraPosition.get(1), transformedCameraPosition.get(2));

        Vector3d returnedCameraPosition = new Vector3d(transformedCameraPositionVec3);
        returnedCameraPosition.add(pivotPosition);

        Matrix3d totalRotationMatrix3 = new Matrix3d();
        totalRotationMatrix.get3x3(totalRotationMatrix3);

        Vector3d rotatedDirection = new Vector3d(this.direction);
        rotatedDirection.mul(totalRotationMatrix3);
        rotatedDirection.normalize();

        double dotResult = Math.abs(rotatedDirection.dot(0, 0, 1));
        Vector3d rotatedRight, rotatedUp;
        rotatedRight = new Vector3d(new Vector3d(0, 0, 1));
        rotatedRight.cross(rotatedDirection);
        rotatedRight.normalize();

        rotatedUp = new Vector3d(rotatedDirection);
        rotatedUp.cross(rotatedRight);
        rotatedUp.normalize();

        if (dotResult > 0.995d || Double.isNaN(rotatedRight.x)) {
            rotatedUp = new Vector3d(this.up);
            rotatedUp.mul(totalRotationMatrix3);
            rotatedUp.normalize();

            rotatedRight = new Vector3d(rotatedDirection);
            rotatedRight.cross(rotatedUp);
            rotatedRight.normalize();

            this.direction = rotatedDirection;
            this.up = rotatedUp;
            this.right = rotatedRight;
            this.position = returnedCameraPosition;
        } else {
            this.direction = rotatedDirection;
            this.up = rotatedUp;
            this.right = rotatedRight;
            this.position = returnedCameraPosition;
        }

        this.dirty = true;
    }


    private void calcRight() {
        Vector3d direction = new Vector3d(this.direction);
        this.right = direction.cross(this.up);
    }
}
