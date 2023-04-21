package renderable;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;

public class OriginObject extends RenderableObject {
    RenderableBuffer renderableBuffer;
    int[] vbos;
    boolean dirty;
    float size;

    public OriginObject() {
        super();
        this.size = 16.0f;
        this.setPosition(0.0f, 0.0f, 0.0f);
        this.setRotation(0.0f, 0.0f, 0.0f);
    }
    @Override
    public void render(int program) {
        RenderableBuffer renderableBuffer = this.getBuffer();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4f objectTransformMatrix = getTransformMatrix();
            int uObjectTransformMatrix = GL20.glGetUniformLocation(program, "uObjectTransformMatrix");
            float[] objectTransformMatrixBuffer = new float[16];
            objectTransformMatrix.get(objectTransformMatrixBuffer);

            GL20.glUniformMatrix4fv(uObjectTransformMatrix, false, objectTransformMatrixBuffer);

            int aVertexPosition = GL20.glGetAttribLocation(program, "aVertexPosition");
            int aVertexColor = GL20.glGetAttribLocation(program, "aVertexColor");

            renderableBuffer.setIndiceBind(renderableBuffer.getIndicesVbo());
            renderableBuffer.setAttribute(renderableBuffer.getPositionVbo(), aVertexPosition, 3, 0);
            renderableBuffer.setAttribute(renderableBuffer.getColorVbo(), aVertexColor, 4, 0);

            GL20.glDrawElements(GL20.GL_LINES, renderableBuffer.getIndicesLength(), GL20.GL_UNSIGNED_SHORT, 0);
        }
    }
    @Override
    public RenderableBuffer getBuffer() {
        if (this.renderableBuffer == null) {
            RenderableBuffer renderableBuffer = new RenderableBuffer();

            ArrayList<Short> indicesList = new ArrayList<Short>();
            ArrayList<Float> positionList = new ArrayList<Float>();
            ArrayList<Float> colorList = new ArrayList<Float>();
            // forward
            positionList.add(0.0f);
            positionList.add(0.0f);
            positionList.add(0.0f);

            positionList.add(this.size);
            positionList.add(0.0f);
            positionList.add(0.0f);

            positionList.add(0.0f);
            positionList.add(0.0f);
            positionList.add(0.0f);

            positionList.add(0.0f);
            positionList.add(this.size);
            positionList.add(0.0f);


            positionList.add(0.0f);
            positionList.add(0.0f);
            positionList.add(0.0f);

            positionList.add(0.0f);
            positionList.add(0.0f);
            positionList.add(this.size);


            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);

            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);

            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);

            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);

            colorList.add(0.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);

            colorList.add(0.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);

            // forward
            indicesList.add((short) 0);
            indicesList.add((short) 1);

            indicesList.add((short) 2);
            indicesList.add((short) 3);

            indicesList.add((short) 4);
            indicesList.add((short) 5);

            int indicesVbo = renderableBuffer.createIndicesBuffer(indicesList);
            int positionVbo = renderableBuffer.createBuffer(positionList);
            int colorVbo = renderableBuffer.createBuffer(colorList);

            renderableBuffer.setPositionVbo(positionVbo);
            renderableBuffer.setColorVbo(colorVbo);
            renderableBuffer.setIndicesVbo(indicesVbo);
            renderableBuffer.setIndicesLength(indicesList.size());
            this.renderableBuffer = renderableBuffer;
        }
        return this.renderableBuffer;
    }
}
