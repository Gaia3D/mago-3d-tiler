package geometry.structure;

import geometry.basic.GaiaBoundingBox;
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
import org.lwjgl.system.MemoryStack;
import renderable.RenderableBuffer;
import renderable.TextureBuffer;

import java.nio.ByteBuffer;
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
    private ArrayList<Integer> indices = new ArrayList<>();
    private ArrayList<GaiaVertex> vertices = new ArrayList<>();
    private ArrayList<GaiaSurface> surfaces = new ArrayList<>();

    private GaiaMaterial material = null;
    private RenderableBuffer renderableBuffer = null;
    private TextureBuffer textureBuffer = null;

    public void renderPrimitive(int program) {
        RenderableBuffer renderableBuffer = this.getRenderableBuffer();
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

            //GaiaTexture texture = material.getTextures().get(TextureType.DIFFUSE);

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
    public RenderableBuffer getRenderableBuffer() {
        if (this.renderableBuffer == null) {
            this.renderableBuffer = new RenderableBuffer();

            ArrayList<Float> positionList = new ArrayList<Float>();
            ArrayList<Float> colorList = new ArrayList<Float>();
            ArrayList<Float> normalList = new ArrayList<Float>();
            ArrayList<Float> textureCoordinateList = new ArrayList<Float>();

            List<Short> indicesList = this.indices.stream()
                    .map((indices) -> Short.valueOf(indices.shortValue()))
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
                Vector2d textureCoordinate = vertex.getTextureCoordinates();
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
        GaiaBufferDataSet gaiaBufferDataSet = new GaiaBufferDataSet();
        

        GaiaBuffer positionBuffer = new GaiaBuffer();
        gaiaBufferDataSet.getBuffers().put(AttributeType.POSITION, positionBuffer);

        GaiaBuffer normalBuffer = new GaiaBuffer();
        gaiaBufferDataSet.getBuffers().put(AttributeType.NORMAL, normalBuffer);

        GaiaBuffer colorBuffer = new GaiaBuffer();
        gaiaBufferDataSet.getBuffers().put(AttributeType.COLOR_0, colorBuffer);

        GaiaBuffer textureCoordinateBuffer = new GaiaBuffer();
        gaiaBufferDataSet.getBuffers().put(AttributeType.TEXCOORD_0, textureCoordinateBuffer);

        return gaiaBufferDataSet;
    }
}
