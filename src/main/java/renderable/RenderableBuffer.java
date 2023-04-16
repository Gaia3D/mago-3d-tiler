package renderable;

import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL20;

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
    public int createBuffer(ArrayList<Float> buffer) {
        float[] datas = this.convertFloatArrayToArrayList(buffer);
        int vbo = vbos[this.vboCount];
        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vbo);
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, datas, GL20.GL_STATIC_DRAW);
        vboCount++;
        return vbo;
    }
    public int createIndicesBuffer(List<Short> buffer) {
        short[] indices = convertShortArrayToArrayList(buffer);
        int vbo = vbos[this.vboCount];
        GL20.glBindBuffer(GL20.GL_ELEMENT_ARRAY_BUFFER, vbo);
        GL20.glBufferData(GL20.GL_ELEMENT_ARRAY_BUFFER, indices, GL20.GL_STATIC_DRAW);
        vboCount++;
        return vbo;
    }

    public short[] convertShortArrayToArrayList(List<Short> shortList) {
        short[] shortArray = new short[shortList.size()];
        int i = 0;
        for (Short s : shortList) {
            shortArray[i++] = (s != null ? s : 0); // Or whatever default you want.
            // it has issue about unsigned short
        }
        return shortArray;
    }

    public float[] convertFloatArrayToArrayList(List<Float> floatList) {
        float[] floatArray = new float[floatList.size()];
        int i = 0;
        for (Float f : floatList) {
            floatArray[i++] = (f != null ? f : Float.NaN); // Or whatever default you want.
        }
        return floatArray;
    }

    public void setTextureBind(int textureVbo) {

    }
}
