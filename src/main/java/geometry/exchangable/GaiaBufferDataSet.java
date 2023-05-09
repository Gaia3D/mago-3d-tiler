package geometry.exchangable;

import geometry.structure.*;
import geometry.types.AttributeType;
import geometry.types.TextureType;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL20;
import org.lwjgl.system.MemoryStack;
import renderable.RenderableBuffer;
import renderable.TextureBuffer;
import util.ArrayUtils;
import util.BinaryUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GaiaBufferDataSet<T> {
    private LinkedHashMap<AttributeType, GaiaBuffer> buffers;
    private int id = -1;
    private String guid = "no_guid";
    private int materialId;

    RenderableBuffer renderableBuffer = null;
    TextureBuffer textureBuffer = null;

    public GaiaBufferDataSet() {
        this.buffers = new LinkedHashMap<>();
    }

    public void render(int program, List<GaiaMaterial> materials) {
        int uObjectTransformMatrix = GL20.glGetUniformLocation(program, "uObjectTransformMatrix");
        float[] objectTransformMatrixBuffer = new float[16];
        Matrix4d preMultipliedTransformMatrix = new Matrix4d().identity();
        preMultipliedTransformMatrix.get(objectTransformMatrixBuffer);
        GL20.glUniformMatrix4fv(uObjectTransformMatrix, false, objectTransformMatrixBuffer);

        GaiaMaterial material = materials.get(materialId);
        RenderableBuffer renderableBuffer = this.getRenderableBuffer(material);
        TextureBuffer textureBuffer = this.getTextureBuffer();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            int uTextureType = GL20.glGetUniformLocation(program, "uTextureType");
            int aVertexPosition = GL20.glGetAttribLocation(program, "aVertexPosition");
            int aVertexColor = GL20.glGetAttribLocation(program, "aVertexColor");
            int aVertexNormal = GL20.glGetAttribLocation(program, "aVertexNormal");
            int aVertexTextureCoordinate = GL20.glGetAttribLocation(program, "aVertexTextureCoordinate");

            renderableBuffer.setIndiceBind(renderableBuffer.getIndicesVbo());
            renderableBuffer.setAttribute(renderableBuffer.getPositionVbo(), aVertexPosition, 3, 0);
            renderableBuffer.setAttribute(renderableBuffer.getColorVbo(), aVertexColor, 4, 0);
            renderableBuffer.setAttribute(renderableBuffer.getNormalVbo(), aVertexNormal, 3, 0);
            renderableBuffer.setAttribute(renderableBuffer.getTextureCoordinateVbo(), aVertexTextureCoordinate, 2, 0);

            List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);
            if (textures.size() > 0) {
                GaiaTexture texture = textures.get(0);
                if (texture.getBufferedImage() == null) {
                    //texture.setFormat(GL20.GL_RGB);
                    //texture.readImage();
                    //texture.loadBuffer();
                }
                if (textureBuffer.getVboCount() < 1) {
                    ByteBuffer byteBuffer = texture.loadTextureBuffer();
                    int textureVbo = textureBuffer.createGlTexture(texture);
                    textureBuffer.setTextureVbo(textureVbo);
                }
                GL20.glUniform1i(uTextureType, 1);
                textureBuffer.setTextureBind(textureBuffer.getTextureVbo());
            } else {
                GL20.glUniform1i(uTextureType, 0);
            }
            GL20.glDrawElements(GL20.GL_TRIANGLES, renderableBuffer.getIndicesLength(), GL20.GL_UNSIGNED_SHORT, 0);
            GL20.glUniform1i(uTextureType, 0);
        }
    }

    public RenderableBuffer getRenderableBuffer(GaiaMaterial material) {
        if (this.renderableBuffer == null) {
            this.renderableBuffer = new RenderableBuffer();

            //short[] indices = buffers.get(AttributeType.INDICE).getShorts();
            //float[] positionBuffer = buffers.get(AttributeType.POSITION).getFloats();
            //float[] normalBuffer = buffers.get(AttributeType.NORMAL).getFloats();

            short[] indices = null;
            GaiaBuffer indiceBuffer = buffers.get(AttributeType.INDICE);
            if (indiceBuffer != null) {
                indices = indiceBuffer.getShorts();
                int indicesVbo = renderableBuffer.createIndicesBuffer(indices);
                renderableBuffer.setIndicesVbo(indicesVbo);
                renderableBuffer.setIndicesLength(indices.length);
            }

            float[] positionBuffer = null;
            GaiaBuffer positionBufferBuffer = buffers.get(AttributeType.POSITION);
            if (positionBufferBuffer != null) {
                positionBuffer = positionBufferBuffer.getFloats();
                int positionVbo = renderableBuffer.createBuffer(positionBuffer);
                renderableBuffer.setPositionVbo(positionVbo);
            }

            float[] normalBuffer = null;
            GaiaBuffer normalBufferBuffer = buffers.get(AttributeType.NORMAL);
            if (normalBufferBuffer != null) {
                normalBuffer = normalBufferBuffer.getFloats();
                int normalVbo = renderableBuffer.createBuffer(normalBuffer);
                renderableBuffer.setNormalVbo(normalVbo);
            }

            float[] textureCoordinateList = null;
            GaiaBuffer textureCoordinateBuffer = buffers.get(AttributeType.TEXCOORD);
            if (textureCoordinateBuffer != null) {
                textureCoordinateList = textureCoordinateBuffer.getFloats();
                int textureCoordinateVbo = renderableBuffer.createBuffer(textureCoordinateList);
                renderableBuffer.setTextureCoordinateVbo(textureCoordinateVbo);
            }

            float[] colorList = null;
            GaiaBuffer colorBuffer = buffers.get(AttributeType.COLOR);
            if (colorBuffer != null) {
                colorList = colorBuffer.getFloats();
                int colorVbo = renderableBuffer.createBuffer(colorList);
                renderableBuffer.setColorVbo(colorVbo);
            } else {
                Vector4d diffuseColor = material.getDiffuseColor();
                int length = positionBuffer.length / 3 * 4;
                colorList = new float[length];
                for (int i = 0; i < length; i+=4) {
                    colorList[i] = (float) diffuseColor.get(0);
                    colorList[i + 1] = (float) diffuseColor.get(1);
                    colorList[i + 2] = (float) diffuseColor.get(2);
                    colorList[i + 3] = (float) diffuseColor.get(3);
                }
                int colorVbo = renderableBuffer.createBuffer(colorList);
                renderableBuffer.setColorVbo(colorVbo);
            }

            //nt indicesVbo = renderableBuffer.createIndicesBuffer(indices);
            //int positionVbo = renderableBuffer.createBuffer(positionBuffer);
            //int colorVbo = renderableBuffer.createBuffer(colorList);
            //int normalVbo = renderableBuffer.createBuffer(normalBuffer);
            //int textureCoordinateVbo = renderableBuffer.createBuffer(textureCoordinateList);

            //renderableBuffer.setPositionVbo(positionVbo);
            //renderableBuffer.setNormalVbo(normalVbo);
            //renderableBuffer.setTextureCoordinateVbo(textureCoordinateVbo);
            //renderableBuffer.setIndicesVbo(indicesVbo);
            //renderableBuffer.setIndicesLength(indices.length);
        }
        return this.renderableBuffer;
    }

    public TextureBuffer getTextureBuffer() {
        if (this.textureBuffer == null) {
            this.textureBuffer = new TextureBuffer();
        }
        return this.textureBuffer;
    }

    public void write(DataOutputStream stream) throws IOException {
        BinaryUtils.writeInt(stream, id);
        BinaryUtils.writeText(stream, guid);
        BinaryUtils.writeInt(stream, materialId);
        BinaryUtils.writeInt(stream,  buffers.size());
        for (Map.Entry<AttributeType, GaiaBuffer> entry : buffers.entrySet()) {
            AttributeType attributeType = entry.getKey();
            GaiaBuffer buffer = entry.getValue();
            BinaryUtils.writeText(stream, attributeType.getGaia());
            buffer.writeBuffer(stream);
        }
    }

    //read
    public void read(DataInputStream stream) throws IOException {
        this.setId(BinaryUtils.readInt(stream));
        this.setGuid(BinaryUtils.readText(stream));
        this.setMaterialId(BinaryUtils.readInt(stream));
        int size = BinaryUtils.readInt(stream);
        for (int i = 0; i < size; i++) {
            String gaiaAttribute = BinaryUtils.readText(stream);
            AttributeType attributeType = AttributeType.getGaiaAttribute(gaiaAttribute);
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.readBuffer(stream);
            buffers.put(attributeType, buffer);
        }
    }

    public GaiaPrimitive toPrimitive() {
        List<Integer> indices = null;
        List<GaiaVertex> vertices = new ArrayList<>();
        GaiaPrimitive primitive = new GaiaPrimitive();
        //int elementsCount = buffers.get(AttributeType.POSITION).getElementsCount();

        for (Map.Entry<AttributeType, GaiaBuffer> entry : buffers.entrySet()) {
            AttributeType attributeType = entry.getKey();
            GaiaBuffer buffer = entry.getValue();

            if (attributeType.equals(AttributeType.INDICE)) {
                indices = ArrayUtils.convertIntArrayListToShortArray(buffer.getShorts());
            } else if (attributeType.equals(AttributeType.POSITION)) {
                List<Float> positions = ArrayUtils.convertArrayListToFloatArray(buffer.getFloats());
                if (vertices.size() > 0) {
                    int positionCount = 0;
                    for (int i = 0; i < vertices.size(); i++) {
                        GaiaVertex vertex = vertices.get(i);
                        Vector3d position = new Vector3d(positions.get(positionCount++), positions.get(positionCount++), positions.get(positionCount++));
                        vertex.setPosition(position);
                    }
                } else {
                    int positionSize = positions.size();
                    for (int i = 0; i < positionSize; i += 3) {
                        GaiaVertex vertex = new GaiaVertex();
                        Vector3d position = new Vector3d(positions.get(i), positions.get(i + 1), positions.get(i + 2));
                        vertex.setPosition(position);
                        vertices.add(vertex);
                    }
                }
            } else if (attributeType.equals(AttributeType.NORMAL)) {
                List<Float> normals = ArrayUtils.convertArrayListToFloatArray(buffer.getFloats());
                if (vertices.size() > 0) {
                    int normalCount = 0;
                    for (int i = 0; i < vertices.size(); i++) {
                        GaiaVertex vertex = vertices.get(i);
                        Vector3d normal = new Vector3d(normals.get(normalCount++), normals.get(normalCount++), normals.get(normalCount++));
                        vertex.setNormal(normal);
                    }
                } else {
                    int normalSize = normals.size();
                    for (int i = 0; i < normalSize; i += 3) {
                        GaiaVertex vertex = new GaiaVertex();
                        Vector3d normal = new Vector3d(normals.get(i), normals.get(i + 1), normals.get(i + 2));
                        vertex.setNormal(normal);
                        vertices.add(vertex);
                    }
                }
            } else if (attributeType.equals(AttributeType.TEXCOORD)) {
                List<Float> texcoords = ArrayUtils.convertArrayListToFloatArray(buffer.getFloats());
                if (vertices.size() > 0) {
                    int texcoordCount = 0;
                    for (int i = 0; i < vertices.size(); i++) {
                        GaiaVertex vertex = vertices.get(i);
                        Vector2d texcoord = new Vector2d(texcoords.get(texcoordCount++), texcoords.get(texcoordCount++));
                        vertex.setTextureCoordinates(texcoord);
                    }
                } else {
                    int texcoordSize = texcoords.size();
                    for (int i = 0; i < texcoordSize; i += 2) {
                        GaiaVertex vertex = new GaiaVertex();
                        Vector2d texcoord = new Vector2d(texcoords.get(i), texcoords.get(i + 1));
                        vertex.setTextureCoordinates(texcoord);
                        vertices.add(vertex);
                    }
                }
            }
        }

        primitive.setIndices(indices);
        primitive.setVertices(vertices);

        return primitive;
    }
}
