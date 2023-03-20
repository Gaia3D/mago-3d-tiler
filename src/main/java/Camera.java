import org.joml.*;
public class Camera {
    Matrix4f transformMatrix;
    Matrix4f modelViewMatrix;
    Matrix4f rotationMatrix;

    Vector3f position;
    Vector3f rotation;

    Vector3f direction;
    Vector3f up;
    Vector3f right;

    int[] vbo;

    boolean dirty;

    public Camera() {
        this.init();
    }

    public void init() {
        this.dirty = false;
        this.transformMatrix = null;
        this.modelViewMatrix = null;
        this.rotationMatrix = null;

        this.position = new Vector3f(0, 0, 0);
        this.rotation = new Vector3f(0, 0, 0);

        this.direction = new Vector3f(0, 0, -1);
        this.up = new Vector3f(0, 1, 0);
        this.right = new Vector3f(1, 0, 0);
    }

    public Matrix4f getTransformMatrix() {
        if (this.dirty || this.transformMatrix == null) {
            this.transformMatrix = new Matrix4f(this.right.get(0), this.right.get(1), this.right.get(2), 0,
                    this.up.get(0), this.up.get(1), this.up.get(2), 0,
                    -this.direction.get(0), -this.direction.get(1), -this.direction.get(2), 0,
                    this.position.get(0), this.position.get(1), this.position.get(2), 1);
        }
        return this.transformMatrix;
    }

    public Matrix4f getModelViewMatrix() {
        if (this.dirty || this.modelViewMatrix == null) {
            Matrix4f transformMatrix = this.getTransformMatrix();
            Matrix4f modelViewMatrix = transformMatrix.invert(new Matrix4f());
            this.modelViewMatrix = modelViewMatrix;
        }
        return this.modelViewMatrix;
    }
}