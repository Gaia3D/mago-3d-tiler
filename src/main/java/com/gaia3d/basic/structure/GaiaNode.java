package com.gaia3d.basic.structure;

import com.gaia3d.basic.exchangable.GaiaBuffer;
import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.util.ArrayUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class that represents a node of a Gaia object.
 * It contains the meshes and children.
 * The meshes are used for rendering.
 * The children are used for hierarchical structure.
 * @author znkim
 * @since 1.0.0
 * @see <a href="https://en.wikipedia.org/wiki/Scene_graph">Scene graph</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaNode {
    private GaiaNode parent = null;
    private String name = "";
    private List<GaiaMesh> meshes = new ArrayList<>();
    private List<GaiaNode> children = new ArrayList<>();

    private Matrix4d transformMatrix = new Matrix4d();
    private Matrix4d preMultipliedTransformMatrix = new Matrix4d();
    private GaiaBoundingBox gaiaBoundingBox = null;

    public GaiaNode(GaiaBufferDataSet bufferDataSet) {
        GaiaMesh mesh = new GaiaMesh();

        GaiaPrimitive primitive = bufferDataSet.toPrimitive();
        primitive.setMaterialIndex(bufferDataSet.getMaterialId());

        List<Float> positionList = new ArrayList<>();
        List<Float> normalList = new ArrayList<>();
        List<Byte> colorList = new ArrayList<>();
        List<Float> texCoordList = new ArrayList<>();
        List<Float> batchIdList = new ArrayList<>();
        List<GaiaVertex> vertexList = new ArrayList<>();

        Map<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();
        for (Map.Entry<AttributeType, GaiaBuffer> entry : buffers.entrySet()) {
            AttributeType attributeType = entry.getKey();
            GaiaBuffer buffer = entry.getValue();
            if (attributeType == AttributeType.POSITION) {
                float[] positions = buffer.getFloats();
                positionList = ArrayUtils.convertListToFloatArray(positions);
            } else if (attributeType == AttributeType.NORMAL) {
                float[] normals = buffer.getFloats();
                normalList = ArrayUtils.convertListToFloatArray(normals);
            } else if (attributeType == AttributeType.COLOR) {
                byte[] colors = buffer.getBytes();
                colorList = ArrayUtils.convertByteListToShortArray(colors);
            } else if (attributeType == AttributeType.TEXCOORD) {
                float[] texCoords = buffer.getFloats();
                texCoordList = ArrayUtils.convertListToFloatArray(texCoords);
            } else if (attributeType == AttributeType.BATCHID) {
                float[] texCoords = buffer.getFloats();
                batchIdList = ArrayUtils.convertListToFloatArray(texCoords);
            }
        }

        for (int i = 0; i < positionList.size() / 3; i++) {
            int vertexIndex = i * 3;
            GaiaVertex vertex = new GaiaVertex();
            if (!positionList.isEmpty()) {
                float positionX = positionList.get(vertexIndex);
                float positionY = positionList.get(vertexIndex + 1);
                float positionZ = positionList.get(vertexIndex + 2);
                vertex.setPosition(new Vector3d(positionX, positionY, positionZ));
            }
            if (!normalList.isEmpty()) {
                float normalX = normalList.get(vertexIndex);
                float normalY = normalList.get(vertexIndex + 1);
                float normalZ = normalList.get(vertexIndex + 2);
                vertex.setNormal(new Vector3d(normalX, normalY, normalZ));
            }
            if (!colorList.isEmpty()) {
                int colorIndex = i * 4;
                byte[] color = new byte[4];
                color[0] = colorList.get(colorIndex);
                color[1] = colorList.get(colorIndex + 1);
                color[2] = colorList.get(colorIndex + 2);
                color[3] = colorList.get(colorIndex + 3);
                vertex.setColor(color);
            }
            if (!texCoordList.isEmpty()) {
                int texcoordIndex = i * 2;
                float texcoordX = texCoordList.get(texcoordIndex);
                float texcoordY = texCoordList.get(texcoordIndex + 1);
                vertex.setTexcoords(new Vector2d(texcoordX, texcoordY));
            }
            if (!batchIdList.isEmpty()) {
                float batchId = batchIdList.get(i);
                vertex.setBatchId(batchId);
            }
            vertexList.add(vertex);
        }
        primitive.setVertices(vertexList);
        mesh.getPrimitives().add(primitive);
        this.meshes.add(mesh);
    }

    public GaiaBoundingBox getBoundingBox(Matrix4d parentTransformMatrix) {
        GaiaBoundingBox boundingBox = null;
        Matrix4d transformMatrix = new Matrix4d(this.transformMatrix);
        if (parentTransformMatrix != null) {
            parentTransformMatrix.mul(transformMatrix, transformMatrix);
        }
        for (GaiaMesh mesh : this.getMeshes()) {
            GaiaBoundingBox meshBoundingBox = mesh.getBoundingBox(transformMatrix);
            if (boundingBox == null) {
                boundingBox = meshBoundingBox;
            } else {
                boundingBox.addBoundingBox(meshBoundingBox);
            }
        }
        for (GaiaNode child : this.getChildren()) {
            GaiaBoundingBox childBoundingBox = child.getBoundingBox(transformMatrix);
            if (childBoundingBox == null) {
                continue;
            }
            if (boundingBox == null) {
                boundingBox = childBoundingBox;
            } else {
                boundingBox.addBoundingBox(childBoundingBox);
            }
        }
        return boundingBox;
    }

    /**
     * recalculate transform
     */
    public void recalculateTransform() {
        GaiaNode node = this;
        node.setPreMultipliedTransformMatrix(new Matrix4d(node.getTransformMatrix()));
        if (node.getParent() != null) {
            Matrix4d parentPreMultipliedTransformMatrix = node.getParent().getPreMultipliedTransformMatrix();
            Matrix4d preMultipliedTransformMatrix = node.getPreMultipliedTransformMatrix();
            parentPreMultipliedTransformMatrix.mul(preMultipliedTransformMatrix, preMultipliedTransformMatrix);
        }
        for (GaiaNode child : node.getChildren()) {
            child.recalculateTransform();
        }
    }

    public void toGaiaBufferSets(List<GaiaBufferDataSet> bufferSets, Matrix4d parentTransformMatrix) {
        Matrix4d transformMatrix = new Matrix4d(this.transformMatrix);
        if (parentTransformMatrix != null) {
            parentTransformMatrix.mul(transformMatrix, transformMatrix);
        }
        for (GaiaMesh mesh : this.getMeshes()) {
            mesh.toGaiaBufferSets(bufferSets, transformMatrix);
        }
        for (GaiaNode child : this.getChildren()) {
            child.toGaiaBufferSets(bufferSets, transformMatrix);
        }
    }

    public void translate(Vector3d translation) {
        for (GaiaMesh mesh : this.getMeshes()) {
            mesh.translate(translation);
        }
        for (GaiaNode child : this.getChildren()) {
            child.translate(translation);
        }
    }
}
