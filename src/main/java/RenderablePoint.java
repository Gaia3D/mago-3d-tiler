import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class RenderablePoint implements RenderableObject {
    Matrix4f transformMatrix;
    Matrix4f modelViewMatrix;
    Matrix4f rotationMatrix;

    Vector3f position;
    Vector3f rotation;

    int[] vbos;

    boolean dirty;

    public RenderablePoint() {
        position = new Vector3f(0, 0, 0);
        rotation = new Vector3f(0, 0, 0);
        this.dirty = false;

        vbos = new int[2];
    }
    @Override
    public void render(int program) {
        System.out.println("point");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f ObjectRotationMatrix = getRotationMatrix();
            int uObjectRotationMatrix = GL20.glGetUniformLocation(program, "uObjectRotationMatrix");
            FloatBuffer rotationMatrixBuffer = stack.mallocFloat(16);
            ObjectRotationMatrix.get(rotationMatrixBuffer);

            GL20.glUniformMatrix4fv(uObjectRotationMatrix, false, rotationMatrixBuffer);


            int aVertexPosition = GL20.glGetAttribLocation(program, "aVertexPosition");
            int aVertexColor = GL20.glGetAttribLocation(program, "aVertexColor");

            GL20.glEnableVertexAttribArray(aVertexPosition);
            GL20.glEnableVertexAttribArray(aVertexColor);

            FloatBuffer positionBuffer = Buffers.newDirectFloatBuffer(pvalues);
            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 0);
            GL20.glBufferData(0, positionBuffer.limit() * 4, positionBuffer, GL20.GL_STATIC_DRAW);
            GL20.glVertexAttribPointer(aVertexPosition, 3, GL20.GL_FLOAT, false, 0, 0);

            GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, 1);
            GL20.glVertexAttribPointer(aVertexColor, 4, GL20.GL_FLOAT, false, 0, 1);
        }
    }
    @Override
    public void getBuffer() {

    }

    public Matrix4f getTransformMatrix() {
        if (this.dirty || this.transformMatrix == null) {
            this.transformMatrix = new Matrix4f(
                    0, 1, 0, 0,
                    0, 0, 1, 0,
                    1, 0, 0, 0,
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
    public Matrix4f getRotationMatrix() {
        if (this.dirty || this.rotationMatrix == null) {
            Matrix4f transformMatrix = new Matrix4f(this.getTransformMatrix());
            Matrix4f rotationMatrix = transformMatrix.invert(new Matrix4f());
            this.rotationMatrix = rotationMatrix;
        }
        return this.rotationMatrix;
    }
}
