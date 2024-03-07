package com.gaia3d.renderable;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.opengl.GL20;

import java.util.ArrayList;

@Setter
@Getter
public class RenderableBufferSet {
    private int[] vbos;
    private int vboCount = 0;

    private int indicesLength;
    private int indicesVbo;
    private int positionVbo;
    private int colorVbo;

    public RenderableBufferSet() {
        this.initVbo();
    }
    public void initVbo() {
        vbos = new int[4];
        GL20.glGenBuffers(vbos);
        vboCount = 0;
    }
    public void setIndiceBind(int vbo) {
        GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vbo);
    }
    public void setAttribute(int vbo, int attributeLocation, int size, int pointer) {
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        GL20.glVertexAttribPointer(attributeLocation, size, GL20.GL_FLOAT, false, 0, pointer);
        GL20.glEnableVertexAttribArray(attributeLocation);
    }
    public int createBuffer(float[] buffer) {
        int vbo = vbos[this.vboCount];
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, buffer, GL20.GL_STATIC_DRAW);
        vboCount++;
        return vbo;
    }
    public int createBuffer(ArrayList<Float> buffer) {
        float[] datas = this.convertFloatArrayToArrayList(buffer);
        int vbo = vbos[this.vboCount];
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, datas, GL20.GL_STATIC_DRAW);
        vboCount++;
        return vbo;
    }
    public int createIndicesBuffer(short[] buffer) {
        int vbo = vbos[this.vboCount];
        GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vbo);
        GL20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, buffer, GL20.GL_STATIC_DRAW);
        vboCount++;
        return vbo;
    }
    public int createIndicesBuffer(ArrayList<Short> buffer) {
        short[] indices = convertShortArrayToArrayList(buffer);
        int vbo = vbos[this.vboCount];
        GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vbo);
        GL20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, indices, GL20.GL_STATIC_DRAW);
        vboCount++;
        return vbo;
    }

    public short[] convertShortArrayToArrayList(ArrayList<Short> shortList) {
        short[] shortArray = new short[shortList.size()];
        int i = 0;
        for (Short s : shortList) {
            shortArray[i++] = (s != null ? s : 0); // Or whatever default you want.
            // it has issue about unsigned short
        }
        return shortArray;
    }

    public float[] convertFloatArrayToArrayList(ArrayList<Float> floatList) {
        float[] floatArray = new float[floatList.size()];
        int i = 0;
        for (Float f : floatList) {
            floatArray[i++] = (f != null ? f : Float.NaN); // Or whatever default you want.
        }
        return floatArray;
    }

}
