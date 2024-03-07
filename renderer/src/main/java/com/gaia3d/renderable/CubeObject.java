package com.gaia3d.renderable;

import org.joml.Matrix4d;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;

import java.util.ArrayList;

public class CubeObject extends RenderableObject {
    RenderableBufferSet renderableBuffer;
    int[] vbos;
    boolean dirty;
    float size;

    public CubeObject() {
        super();
        this.size = 0.25f;
        this.setPosition(0.0f, 0.0f, -1.0f);
        this.setRotation((float) Math.toRadians(0), 0.0f, 0.0f);
    }
    @Override
    public void render(int program) {
        RenderableBufferSet renderableBuffer = this.getBuffer();

        //try (MemoryStack stack = MemoryStack.stackPush()) {
            Matrix4d objectRotationMatrix = getTransformMatrix();
            int uObjectRotationMatrix = GL20.glGetUniformLocation(program, "uObjectRotationMatrix");
            float[] objectRotationMatrixBuffer = new float[16];
            objectRotationMatrix.get(objectRotationMatrixBuffer);

            GL20.glUniformMatrix4fv(uObjectRotationMatrix, false, objectRotationMatrixBuffer);

            int aVertexPosition = GL20.glGetAttribLocation(program, "aVertexPosition");
            int aVertexColor = GL20.glGetAttribLocation(program, "aVertexColor");

            renderableBuffer.setIndiceBind(renderableBuffer.getIndicesVbo());
            renderableBuffer.setAttribute(renderableBuffer.getPositionVbo(), aVertexPosition, 3, 0);
            renderableBuffer.setAttribute(renderableBuffer.getColorVbo(), aVertexColor, 4, 0);

            //GL20.glDrawArrays(GL20.GL_TRIANGLES, 0, 3);
            GL20.glDrawElements(GL20.GL_TRIANGLES, renderableBuffer.getIndicesLength(), GL20.GL_UNSIGNED_SHORT, 0);
        //}
    }
    @Override
    public RenderableBufferSet getBuffer() {
        if (this.renderableBuffer == null) {
            RenderableBufferSet renderableBuffer = new RenderableBufferSet();

            ArrayList<Short> indicesList = new ArrayList<Short>();
            ArrayList<Float> positionList = new ArrayList<Float>();
            ArrayList<Float> colorList = new ArrayList<Float>();
            // forward
            positionList.add(-size);
            positionList.add(-size);
            positionList.add(size);

            positionList.add(size);
            positionList.add(-size);
            positionList.add(size);

            positionList.add(size);
            positionList.add(size);
            positionList.add(size);

            positionList.add(-size);
            positionList.add(size);
            positionList.add(size);

            // backward
            positionList.add(-size);
            positionList.add(-size);
            positionList.add(-size);

            positionList.add(size);
            positionList.add(-size);
            positionList.add(-size);

            positionList.add(size);
            positionList.add(size);
            positionList.add(-size);

            positionList.add(-size);
            positionList.add(size);
            positionList.add(-size);

            // left
            positionList.add(-size);
            positionList.add(-size);
            positionList.add(-size);

            positionList.add(-size);
            positionList.add(-size);
            positionList.add(size);

            positionList.add(-size);
            positionList.add(size);
            positionList.add(size);

            positionList.add(-size);
            positionList.add(size);
            positionList.add(-size);

            // right
            positionList.add(size);
            positionList.add(-size);
            positionList.add(size);

            positionList.add(size);
            positionList.add(-size);
            positionList.add(-size);

            positionList.add(size);
            positionList.add(size);
            positionList.add(-size);

            positionList.add(size);
            positionList.add(size);
            positionList.add(size);

            // up
            positionList.add(-size);
            positionList.add(size);
            positionList.add(size);

            positionList.add(size);
            positionList.add(size);
            positionList.add(size);

            positionList.add(size);
            positionList.add(size);
            positionList.add(-size);

            positionList.add(-size);
            positionList.add(size);
            positionList.add(-size);

            // down
            positionList.add(-size);
            positionList.add(-size);
            positionList.add(size);

            positionList.add(size);
            positionList.add(-size);
            positionList.add(size);

            positionList.add(size);
            positionList.add(-size);
            positionList.add(-size);

            positionList.add(-size);
            positionList.add(-size);
            positionList.add(-size);

            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
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
            colorList.add(0.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);


            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);

            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);

            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(0.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);

            // forward
            indicesList.add((short) 0);
            indicesList.add((short) 2);
            indicesList.add((short) 1);
            indicesList.add((short) 0);
            indicesList.add((short) 2);
            indicesList.add((short) 3);

            // backward
            indicesList.add((short) 4);
            indicesList.add((short) 5);
            indicesList.add((short) 6);
            indicesList.add((short) 4);
            indicesList.add((short) 6);
            indicesList.add((short) 7);
            // left
            indicesList.add((short) 8);
            indicesList.add((short) 9);
            indicesList.add((short) 10);
            indicesList.add((short) 8);
            indicesList.add((short) 10);
            indicesList.add((short) 11);
            // right
            indicesList.add((short) 12);
            indicesList.add((short) 13);
            indicesList.add((short) 14);
            indicesList.add((short) 12);
            indicesList.add((short) 14);
            indicesList.add((short) 15);
            // up
            indicesList.add((short) 16);
            indicesList.add((short) 17);
            indicesList.add((short) 18);
            indicesList.add((short) 16);
            indicesList.add((short) 18);
            indicesList.add((short) 19);
            // down
            indicesList.add((short) 20);
            indicesList.add((short) 21);
            indicesList.add((short) 22);
            indicesList.add((short) 20);
            indicesList.add((short) 22);
            indicesList.add((short) 23);


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