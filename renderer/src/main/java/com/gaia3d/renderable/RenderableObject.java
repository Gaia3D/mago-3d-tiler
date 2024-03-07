package com.gaia3d.renderable;

import org.joml.Matrix4d;
import org.joml.Vector3d;

public abstract class RenderableObject {
    public Matrix4d transformMatrix;
    public Matrix4d rotationMatrix;

    public Vector3d position;
    public Vector3d rotation;
    public boolean dirty;

    public RenderableObject() {
        this.position = new Vector3d(0.0f, 0.0f, 0.0f);
        this.rotation = new Vector3d(0.0f, 0.0f, 0.0f);
        //getRotationMatrix();
        this.dirty = false;
    }

    abstract public void render(int program);
    abstract public RenderableBufferSet getBuffer();

    public void setPosition(float x, float y, float z) {
        this.position.set(x, y, z);
        this.dirty = true;
    }
    public void setRotation(float x, float y, float z) {
        this.rotation.set(x, y, z);
        this.dirty = true;
    }
    public Matrix4d getTransformMatrix() {
        if (this.dirty || this.transformMatrix == null) {
            Matrix4d transformMatrix = new Matrix4d();
            transformMatrix.identity();
            transformMatrix.rotate(this.rotation.get(1), new Vector3d(0, 1, 0));
            transformMatrix.rotate(this.rotation.get(2), new Vector3d(0, 0, 1));
            transformMatrix.rotate(this.rotation.get(0), new Vector3d(1, 0, 0));
            transformMatrix.set(3, 0, this.position.get(0));
            transformMatrix.set(3, 1, this.position.get(1));
            transformMatrix.set(3, 2, this.position.get(2));
            this.transformMatrix = transformMatrix;
            this.dirty = false;
        }
        return this.transformMatrix;
    }
    public Matrix4d getRotationMatrix() {
        if (this.dirty || this.rotationMatrix == null) {
            Matrix4d rotationMatrix = new Matrix4d(this.getTransformMatrix());
            rotationMatrix.set(3, 0, 0.0f);
            rotationMatrix.set(3, 1, 0.0f);
            rotationMatrix.set(3, 2, 0.0f);
            this.rotationMatrix = rotationMatrix;
            this.dirty = false;
        }
        return this.rotationMatrix;
    }
}
