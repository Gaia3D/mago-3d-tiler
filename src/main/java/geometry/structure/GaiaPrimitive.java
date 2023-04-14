package geometry.structure;

import geometry.basic.GaiaBoundingBox;
import geometry.basic.GaiaVBO;
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
import util.GeometryUtils;

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
    private ArrayList<Integer> indices = new ArrayList<>(); // 3d
    private ArrayList<GaiaVertex> vertices = new ArrayList<>();
    private ArrayList<GaiaSurface> surfaces = new ArrayList<>();

    private GaiaMaterial material = null;
    private RenderableBuffer renderableBuffer = null;

    public void render(int program) {
        RenderableBuffer renderableBuffer = this.getRenderableBuffer();

        int aVertexPosition = GL20.glGetAttribLocation(program, "aVertexPosition");
        int aVertexColor = GL20.glGetAttribLocation(program, "aVertexColor");
        int aVertexNormal = GL20.glGetAttribLocation(program, "aVertexNormal");
        int aVertexTextureCoordinate = GL20.glGetAttribLocation(program, "aVertexTextureCoordinate");

        renderableBuffer.setIndiceBind(renderableBuffer.getIndicesVbo());
        renderableBuffer.setAttribute(renderableBuffer.getPositionVbo(), aVertexPosition, 3, 0);
        renderableBuffer.setAttribute(renderableBuffer.getColorVbo(), aVertexColor, 4, 0);
        renderableBuffer.setAttribute(renderableBuffer.getNormalVbo(), aVertexNormal, 3, 0);
        renderableBuffer.setAttribute(renderableBuffer.getTextureCoordinateVbo(), aVertexTextureCoordinate, 2, 0);

        if (materialIndex != -1) {
            renderableBuffer.setTextureBind(renderableBuffer.getTextureVbo());
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

    public GaiaBoundingBox getBoundingBox(Matrix4d transform) {
        GaiaBoundingBox boundingBox = null;
        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();

            Matrix4d transformMatrix = transform;
            Vector3d transformedPosition = new Vector3d(position);
            if (transform != null) {
                transformMatrix.transformPosition(position, transformedPosition);
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

    public void genNormals() {
        for (int i = 0; i < indices.size(); i += 3) {
            GaiaVertex vertex1 = vertices.get(indices.get(i));
            GaiaVertex vertex2 = vertices.get(indices.get(i + 1));
            GaiaVertex vertex3 = vertices.get(indices.get(i + 2));
            if (vertex1.getNormal() != null && vertex2.getNormal() != null && vertex3.getNormal() != null) {
                continue;
            }
            GeometryUtils.genNormals(vertex1, vertex2, vertex3);
        }
    }
}
