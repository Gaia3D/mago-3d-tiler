package renderable;

import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;
import util.ArrayUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Setter
@Getter
public class RenderableBuffer {
    private int[] vbos;
    private int vboCount = 0;
    private Matrix4d transformMatrix;

    private int indicesLength;
    private int indicesVbo;
    private int positionVbo;
    private int normalVbo;
    private int colorVbo;
    private int textureCoordinateVbo;
    private int textureVbo;

    public RenderableBuffer() {
        this.initVbo();
    }
    public void initVbo() {
        vbos = new int[6];
        vboCount = 0;
        GL20.glGenBuffers(vbos);
    }
    public void setIndiceBind(int vbo) {
        GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vbo);
    }
    public void setAttribute(int vbo, int attributeLocation, int size, int pointer) {
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        GL20.glVertexAttribPointer(attributeLocation, size, GL20.GL_FLOAT, false, 0, pointer);
        GL20.glEnableVertexAttribArray(attributeLocation);
    }

    public int createBuffer(float[] datas) {
        int vbo = vbos[this.vboCount];
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, datas, GL20.GL_STATIC_DRAW);
        vboCount++;
        return vbo;
    }
    public int createBuffer(List<Float> buffer) {
        float[] datas = ArrayUtils.convertFloatArrayToArrayList(buffer);
        int vbo = vbos[this.vboCount];
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, datas, GL20.GL_STATIC_DRAW);
        vboCount++;
        return vbo;
    }
    
    public int createIndicesBuffer(short[] indices) {
        int vbo = vbos[this.vboCount];
        GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vbo);
        GL20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, indices, GL20.GL_STATIC_DRAW);
        vboCount++;
        return vbo;
    }
    public int createIndicesBuffer(List<Short> buffer) {
        short[] indices = ArrayUtils.convertShortArrayToArrayList(buffer);
        int vbo = vbos[this.vboCount];
        GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vbo);
        GL20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, indices, GL20.GL_STATIC_DRAW);
        vboCount++;
        return vbo;
    }
}
