package geometry.structure;

import geometry.basic.GaiaBoundingBox;
import geometry.basic.GaiaRectangle;
import geometry.exchangable.GaiaBuffer;
import geometry.exchangable.GaiaBufferDataSet;
import geometry.types.AttributeType;
import geometry.types.TextureType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import org.lwjgl.opengl.GL20;
import renderable.RenderableBuffer;
import renderable.TextureBuffer;
import util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaPrimitive {
    private Integer accessorIndices = -1;
    private Integer materialIndex = -1;
    private List<Integer> indices = new ArrayList<>();
    private List<GaiaVertex> vertices = new ArrayList<>();
    private List<GaiaSurface> surfaces = new ArrayList<>();

    private GaiaMaterial material = null;
    private RenderableBuffer renderableBuffer = null;
    private TextureBuffer textureBuffer = null;

    public void renderPrimitive(int program) {
        RenderableBuffer renderableBuffer = this.getRenderableBuffer();
        TextureBuffer textureBuffer = this.getTextureBuffer();

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
            if (textureBuffer.getVboCount() < 1) {
                texture.loadTextureBuffer();
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
    public RenderableBuffer getRenderableBuffer() {
        if (this.renderableBuffer == null) {
            this.renderableBuffer = new RenderableBuffer();

            ArrayList<Float> positionList = new ArrayList<>();
            ArrayList<Float> colorList = new ArrayList<>();
            ArrayList<Float> normalList = new ArrayList<>();
            ArrayList<Float> textureCoordinateList = new ArrayList<>();

            List<Short> indicesList = this.indices.stream()
                    .map(Integer::shortValue)
                    .collect(Collectors.toList());

            for (GaiaVertex vertex : this.vertices) {
                Vector3d position = vertex.getPosition();
                if (position != null) {
                    positionList.add((float) position.x);
                    positionList.add((float) position.y);
                    positionList.add((float) position.z);
                }
                Vector3d normal = vertex.getNormal();
                if (normal != null) {
                    normalList.add((float) normal.x);
                    normalList.add((float) normal.y);
                    normalList.add((float) normal.z);
                }
                Vector2d textureCoordinate = vertex.getTexcoords();
                if (textureCoordinate != null) {
                    textureCoordinateList.add((float) textureCoordinate.x);
                    textureCoordinateList.add((float) textureCoordinate.y);
                }
                Vector4d diffuseColor = material.getDiffuseColor();
                if (diffuseColor != null) {
                    colorList.add((float) diffuseColor.x);
                    colorList.add((float) diffuseColor.y);
                    colorList.add((float) diffuseColor.z);
                    colorList.add((float) diffuseColor.w);
                }
            }

            int indicesVbo = renderableBuffer.createIndicesBuffer(indicesList);
            int positionVbo = renderableBuffer.createBuffer(positionList);
            int colorVbo = renderableBuffer.createBuffer(colorList);
            int normalVbo = renderableBuffer.createBuffer(normalList);
            int textureCoordinateVbo = renderableBuffer.createBuffer(textureCoordinateList);

            renderableBuffer.setPositionVbo(positionVbo);
            renderableBuffer.setColorVbo(colorVbo);
            renderableBuffer.setNormalVbo(normalVbo);
            renderableBuffer.setTextureCoordinateVbo(textureCoordinateVbo);
            renderableBuffer.setIndicesVbo(indicesVbo);
            renderableBuffer.setIndicesLength(indicesList.size());
        }
        return this.renderableBuffer;
    }

    private TextureBuffer getTextureBuffer() {
        if (this.textureBuffer == null) {
            this.textureBuffer = new TextureBuffer();
        }
        return this.textureBuffer;
    }

    public GaiaBoundingBox getBoundingBox(Matrix4d transform) {
        GaiaBoundingBox boundingBox = null;
        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();

            Vector3d transformedPosition = new Vector3d(position);
            if (transform != null) {
                transform.transformPosition(position, transformedPosition);
            }

            if (boundingBox == null) {
                boundingBox = new GaiaBoundingBox();
                boundingBox.setInit(transformedPosition);
            } else {
                boundingBox.addPoint(transformedPosition);
            }
        }
        return boundingBox;
    }

    public void calculateNormal() {
        for (GaiaSurface surface : surfaces) {
            surface.calculateNormal(this.vertices);
        }
    }

    public GaiaBufferDataSet toGaiaBufferSet() {
        List<Short> indicesList = this.indices.stream()
                .map(Integer::shortValue)
                .collect(Collectors.toList());
        ArrayList<Float> positionList = new ArrayList<>();
        ArrayList<Float> batchIdList = new ArrayList<>();
        ArrayList<Float> normalList = new ArrayList<>();
        ArrayList<Float> textureCoordinateList = new ArrayList<>();
        GaiaRectangle texcoordBoundingRectangle = null;

        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            if (position != null) {
                positionList.add((float) position.x);
                positionList.add((float) position.y);
                positionList.add((float) position.z);
            }
            Vector3d normal = vertex.getNormal();
            if (normal != null) {
                normalList.add((float) normal.x);
                normalList.add((float) normal.y);
                normalList.add((float) normal.z);
            }
            batchIdList.add(0.0f);
            Vector2d textureCoordinate = vertex.getTexcoords();
            if (textureCoordinate != null) {
                if (texcoordBoundingRectangle == null) {
                    texcoordBoundingRectangle = new GaiaRectangle();
                    texcoordBoundingRectangle.setInit(textureCoordinate);
                } else {
                    texcoordBoundingRectangle.addPoint(textureCoordinate);
                }
                textureCoordinateList.add((float) textureCoordinate.x);
                textureCoordinateList.add((float) textureCoordinate.y);
            }
        }

        GaiaBufferDataSet gaiaBufferDataSet = new GaiaBufferDataSet();
        if (indicesList.size() > 0) {
            GaiaBuffer indicesBuffer = new GaiaBuffer();
            indicesBuffer.setGlTarget(GL20.GL_ELEMENT_ARRAY_BUFFER);
            indicesBuffer.setGlType(GL20.GL_UNSIGNED_SHORT);
            indicesBuffer.setElementsCount(indicesList.size());
            indicesBuffer.setGlDimension((byte) 1);
            indicesBuffer.setShorts(ArrayUtils.convertShortArrayToList(indicesList));
            gaiaBufferDataSet.getBuffers().put(AttributeType.INDICE, indicesBuffer);
        }

        if (normalList.size() > 0) {
            GaiaBuffer normalBuffer = new GaiaBuffer();
            normalBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            normalBuffer.setGlType(GL20.GL_FLOAT);
            normalBuffer.setElementsCount(vertices.size());
            normalBuffer.setGlDimension((byte) 3);
            normalBuffer.setFloats(ArrayUtils.convertFloatArrayToList(normalList));
            gaiaBufferDataSet.getBuffers().put(AttributeType.NORMAL, normalBuffer);
        }

        if (batchIdList.size() > 0) {
            GaiaBuffer batchIdBuffer = new GaiaBuffer();
            batchIdBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            batchIdBuffer.setGlType(GL20.GL_FLOAT);
            batchIdBuffer.setElementsCount(vertices.size());
            batchIdBuffer.setGlDimension((byte) 1);
            batchIdBuffer.setFloats(ArrayUtils.convertFloatArrayToList(batchIdList));
            gaiaBufferDataSet.getBuffers().put(AttributeType.BATCHID, batchIdBuffer);
        }

        if (positionList.size() > 0) {
            GaiaBuffer positionBuffer = new GaiaBuffer();
            positionBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            positionBuffer.setGlType(GL20.GL_FLOAT);
            positionBuffer.setElementsCount(vertices.size());
            positionBuffer.setGlDimension((byte) 3);
            positionBuffer.setFloats(ArrayUtils.convertFloatArrayToList(positionList));
            gaiaBufferDataSet.getBuffers().put(AttributeType.POSITION, positionBuffer);
        }

        if (textureCoordinateList.size() > 0) {
            GaiaBuffer textureCoordinateBuffer = new GaiaBuffer();
            textureCoordinateBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            textureCoordinateBuffer.setGlType(GL20.GL_FLOAT);
            textureCoordinateBuffer.setElementsCount(vertices.size());
            textureCoordinateBuffer.setGlDimension((byte) 2);
            textureCoordinateBuffer.setFloats(ArrayUtils.convertFloatArrayToList(textureCoordinateList));
            gaiaBufferDataSet.getBuffers().put(AttributeType.TEXCOORD, textureCoordinateBuffer);
        }

        gaiaBufferDataSet.setTexcoordBoundingRectangle(texcoordBoundingRectangle);
        return gaiaBufferDataSet;
    }
}
