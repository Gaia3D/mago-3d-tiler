package renderable.primitive;

import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import renderable.RenderableBuffer;
import renderable.RenderableObject;

import java.util.ArrayList;

public class RectangleObject extends RenderableObject {
    RenderableBuffer renderableBuffer;

    public RectangleObject() {
        super();
        this.setPosition(0.0f, 0.0f, -1.0f);
        this.setRotation(1.0f, 1.0f, 0.0f);
    }
    @Override
    public void render(int program) {
        RenderableBuffer renderableBuffer = this.getBuffer();
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

        //GL20.glDrawArrays(GL20.GL_TRIANGLES, 0, 3);
        GL20.glDrawElements(GL20.GL_TRIANGLES, renderableBuffer.getIndicesLength(), GL20.GL_UNSIGNED_SHORT, 0);
    }
    @Override
    public RenderableBuffer getBuffer() {
        if (this.renderableBuffer == null) {
            RenderableBuffer renderableBuffer = new RenderableBuffer();

            ArrayList<Short> indicesList = new ArrayList<>();
            ArrayList<Float> positionList = new ArrayList<>();
            ArrayList<Float> colorList = new ArrayList<>();

            positionList.add(-0.25f);
            positionList.add(-0.25f);
            positionList.add(0.0f);

            positionList.add(0.25f);
            positionList.add(-0.25f);
            positionList.add(0.0f);

            positionList.add(0.25f);
            positionList.add(0.25f);
            positionList.add(0.0f);

            positionList.add(-0.25f);
            positionList.add(0.25f);
            positionList.add(0.0f);

            colorList.add(1.0f);
            colorList.add(0.0f);
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

            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);

            indicesList.add((short) 0);
            indicesList.add((short) 1);
            indicesList.add((short) 2);

            indicesList.add((short) 0);
            indicesList.add((short) 2);
            indicesList.add((short) 3);

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
