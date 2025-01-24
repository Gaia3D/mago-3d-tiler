package com.gaia3d.renderer.engine.scene;

import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;

@Setter
@Getter
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

        this.position = new Vector3d(0, 0, 40);
        this.rotation = new Vector3d(0, 0, 0);

        this.direction = new Vector3d(0, 0, -1);
        this.up = new Vector3d(0, 1, 0);
        this.right = new Vector3d(1, 0, 0);
    }

    public void setPosition(Vector3d position) {
        this.position = position;
        this.dirty = true;
    }

    public void setDirection(Vector3d direction) {
        this.direction = direction;
        this.dirty = true;
    }

    public void setUp(Vector3d up) {
        this.up = up;
        this.dirty = true;
    }

    public Matrix4d getTransformMatrix() {
        if (this.dirty || this.transformMatrix == null) {
            this.calcRight();
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

    public void moveFront(float value) {
        Vector3d front = new Vector3d(this.direction);
        front.mul(value);
        this.position.add(front);
        this.dirty = true;
    }

    public void translate(Vector3d translation) {
        this.position.add(translation);
        this.dirty = true;
    }

    public void calculateCameraXYPlane(Vector3d camPos, Vector3d camTarget)
    {
        Vector3d camDirection = new Vector3d(camTarget);
        camDirection.sub(camPos);
        camDirection.normalize();

        // if the camDir is perpendicular to planeXY, then the camRight and camUp will be the same as the world right and up
        if (Math.abs(camDirection.dot(new Vector3d(0, 0, 1))) > 0.9999) {
            this.position = camPos;
            this.direction = camDirection;
            this.right = new Vector3d(1, 0, 0);
            this.up = new Vector3d(0, 1, 0);
            this.dirty = true;
            return;
        }

        // calculate the right and up vectors
        // do cross product to correct right and up
        this.position = camPos;
        this.direction = camDirection;
        Vector3d currDir = new Vector3d(this.direction);
        this.up = new Vector3d(0, 0, 1);
        currDir.cross(this.up); // dir cross up = right
        currDir.normalize();
        this.right = currDir;
        Vector3d currRight = new Vector3d(this.right);
        currRight.cross(this.direction); // right cross dir = up
        currRight.normalize();
        this.up = currRight;
        this.dirty = true;
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

        Vector3d rotatedRight = new Vector3d(this.right);
        rotatedRight.mul(totalRotationMatrix3);
        rotatedRight.normalize();

        Vector3d rotatedUp = new Vector3d(this.up);
        rotatedUp.mul(totalRotationMatrix3);
        rotatedUp.normalize();

        this.direction = rotatedDirection;
        this.up = rotatedUp;
        this.right = rotatedRight;
        this.position = returnedCameraPosition;

        // do cross product to correct right and up
        rotatedDirection = new Vector3d(this.direction);
        rotatedDirection.cross(this.up); // dir cross up = right
        rotatedDirection.normalize();
        this.right = rotatedDirection;
        rotatedRight = new Vector3d(this.right);
        rotatedRight.cross(this.direction); // right cross dir = up
        rotatedRight.normalize();
        this.up = rotatedRight;

        this.dirty = true;
    }

    public Vector3d calculateUpVector(Vector3d direction) {
        // check if the direction is perpendicular to the z axis
        Vector3d zAxis = new Vector3d(0, 0, 1);
        if (Math.abs(direction.dot(zAxis)) > 0.9999) {
            return new Vector3d(0, 1, 0);
        }
        Vector3d right = new Vector3d(direction);
        right.cross(zAxis);
        right.normalize();
        Vector3d up = new Vector3d(right);
        up.cross(direction);
        up.normalize();
        return up;
    }

    private void calcRight() {
        Vector3d direction = new Vector3d(this.direction);
        this.right = direction.cross(this.up);
    }
}
