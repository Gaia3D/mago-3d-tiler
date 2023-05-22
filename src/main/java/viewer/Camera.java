package viewer;

import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.joml.Vector4d;

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
        this.position = new Vector3d(0, 0, 500);
        this.rotation = new Vector3d(0, 1, 0);
        this.direction = new Vector3d(0, 0, -1);
        this.up = new Vector3d(0, 1, 0);
        this.right = new Vector3d(1, 0, 0);
    }
    // getTransformMatrix
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
    // getModelViewMatrix
    public Matrix4d getModelViewMatrix() {
        if (this.dirty || this.modelViewMatrix == null) {
            Matrix4d transformMatrix = this.getTransformMatrix();
            this.modelViewMatrix = transformMatrix.invert(new Matrix4d());
        }
        return this.modelViewMatrix;
    }
    // move forward
    public void moveForward(float value) {
        this.position.add(this.direction.mul(value, new Vector3d()));
        this.dirty = true;
    }
    // move right
    public void moveRight(float value) {
        this.position.add(this.right.mul(value, new Vector3d()));
        this.dirty = true;
    }
    // move up
    public void moveUp(float value) {
        this.position.add(this.up.mul(value, new Vector3d()));
        this.dirty = true;
    }
    // 카메라 이동 (from Copilot)
    public void move(float xValue, float yValue, float zValue) {
        this.position.add(xValue, yValue, zValue);
        this.dirty = true;
    }
    // 카메라 위치 변경 (from Copilot)
    public void setPosition(float xValue, float yValue, float zValue) {
        this.position.set(xValue, yValue, zValue);
        this.dirty = true;
    }
    // 회전 (from Copilot)
    public void rotate(float xValue, float yValue) {
        this.rotationOrbit(xValue, yValue, this.position);
    }
    //lookat (from Copilot)
    public void lookAt(Vector3d target) {
        Vector3d direction = new Vector3d(target);
        direction.sub(this.position);
        direction.normalize();

        Vector3d right = new Vector3d(0, 0, 1);
        direction.cross(right, right);
        right.normalize();

        Vector3d up = new Vector3d(direction);
        right.cross(up, up);
        up.normalize();

        this.direction = direction;
        this.right = right;
        this.up = up;
        this.dirty = true;
    }

    //lookat from x,y,z (from Copilot)
    public void lookAt(float x, float y, float z) {
        Vector3d target = new Vector3d(x, y, z);
        this.lookAt(target);
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

        Vector3d rotatedRight;
        rotatedRight = new Vector3d(new Vector3d(0, 0, 1));
        rotatedDirection.cross(rotatedRight, rotatedRight);
        rotatedRight.normalize();

        Vector3d rotatedUp = new Vector3d(rotatedDirection);
        rotatedRight.cross(rotatedUp, rotatedUp);
        rotatedUp.normalize();

        this.direction = rotatedDirection;
        this.up = rotatedUp;
        this.right = rotatedRight;
        this.position = returnedCameraPosition;

        this.dirty = true;
    }

    private void calcRight() {
        Vector3d direction = new Vector3d(this.direction);
        this.right = direction.cross(this.up);
    }
}