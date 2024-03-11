package com.gaia3d.basic.structure;

import com.gaia3d.basic.exchangable.GaiaBuffer;
import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.types.AttributeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.Serializable;
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
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaNode implements Serializable {
    private String name = "";
    private GaiaNode parent = null;
    private List<GaiaMesh> meshes = new ArrayList<>();
    private List<GaiaNode> children = new ArrayList<>();

    private Matrix4d transformMatrix = new Matrix4d();
    private Matrix4d preMultipliedTransformMatrix = new Matrix4d();
    private GaiaBoundingBox gaiaBoundingBox = null;

    public GaiaNode(GaiaBufferDataSet bufferDataSet) {
        GaiaMesh mesh = new GaiaMesh();
        GaiaPrimitive primitive = bufferDataSet.toPrimitive();
        primitive.setMaterialIndex(bufferDataSet.getMaterialId());

        float[] positionList = new float[0];
        float[] normalList = new float[0];
        byte[] colorList = new byte[0];
        float[] texCoordList = new float[0];
        float[] batchIdList = new float[0];
        List<GaiaVertex> vertexList = new ArrayList<>();

        Map<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();
        for (Map.Entry<AttributeType, GaiaBuffer> entry : buffers.entrySet()) {
            AttributeType attributeType = entry.getKey();
            GaiaBuffer buffer = entry.getValue();
            if (attributeType == AttributeType.POSITION) {
                positionList = buffer.getFloats();
            } else if (attributeType == AttributeType.NORMAL) {
                normalList = buffer.getFloats();
            } else if (attributeType == AttributeType.COLOR) {
                colorList = buffer.getBytes();
            } else if (attributeType == AttributeType.TEXCOORD) {
                texCoordList = buffer.getFloats();
            } else if (attributeType == AttributeType.BATCHID) {
                batchIdList = buffer.getFloats();
            }
        }

        for (int i = 0; i < positionList.length / 3; i++) {
            int vertexIndex = i * 3;
            GaiaVertex vertex = new GaiaVertex();
            if (positionList.length > 0) {
                float positionX = positionList[vertexIndex];
                float positionY = positionList[vertexIndex + 1];
                float positionZ = positionList[vertexIndex + 2];
                vertex.setPosition(new Vector3d(positionX, positionY, positionZ));
            }
            if (normalList.length > 0) {
                float normalX = normalList[vertexIndex];
                float normalY = normalList[vertexIndex + 1];
                float normalZ = normalList[vertexIndex + 2];
                vertex.setNormal(new Vector3d(normalX, normalY, normalZ));
            }
            if (colorList.length > 0) {
                int colorIndex = i * 4;
                byte[] color = new byte[4];
                color[0] = colorList[colorIndex];
                color[1] = colorList[colorIndex + 1];
                color[2] = colorList[colorIndex + 2];
                color[3] = colorList[colorIndex + 3];
                vertex.setColor(color);
            }
            if (texCoordList.length > 0) {
                int texcoordIndex = i * 2;
                float texcoordX = texCoordList[texcoordIndex];
                float texcoordY = texCoordList[texcoordIndex + 1];
                vertex.setTexcoords(new Vector2d(texcoordX, texcoordY));
            }
            if (batchIdList.length > 0) {
                float batchId = batchIdList[i];
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
        Matrix4d sumTransformMatrix = new Matrix4d(this.transformMatrix);
        if (parentTransformMatrix != null) {
            parentTransformMatrix.mul(sumTransformMatrix, sumTransformMatrix);
        }
        for (GaiaMesh mesh : this.getMeshes()) {
            mesh.clone().toGaiaBufferSets(bufferSets, sumTransformMatrix);
        }
        for (GaiaNode child : this.getChildren()) {
            child.toGaiaBufferSets(bufferSets, sumTransformMatrix);
        }
    }

    public long getTriangleCount() {
        long count = 0;
        for (GaiaMesh mesh : this.getMeshes()) {
            count += mesh.getTriangleCount();
        }
        for (GaiaNode child : this.getChildren()) {
            count += child.getTriangleCount();
        }
        return count;
    }

    public void translate(Vector3d translation) {
        for (GaiaMesh mesh : this.getMeshes()) {
            mesh.translate(translation);
        }
        for (GaiaNode child : this.getChildren()) {
            child.translate(translation);
        }
    }

    public GaiaNode clone() {
        GaiaNode clone = new GaiaNode();
        clone.setName(this.name);
        clone.setTransformMatrix(new Matrix4d(this.transformMatrix));
        for (GaiaMesh mesh : this.meshes) {
            clone.getMeshes().add(mesh.clone());
        }
        for (GaiaNode child : this.children) {
            clone.getChildren().add(child.clone());
        }
        return clone;
    }

    public void clear() {
        this.parent = null;
        this.name = null;
        this.transformMatrix = null;
        this.preMultipliedTransformMatrix = null;
        this.gaiaBoundingBox = null;
        this.meshes.forEach(GaiaMesh::clear);
        this.children.forEach(GaiaNode::clear);
        this.meshes.clear();
        this.children.clear();
    }

    public void extractMeshes(List<GaiaMesh> resultMeshes) {
        resultMeshes.addAll(this.getMeshes());
        for (GaiaNode child : this.getChildren()) {
            child.extractMeshes(resultMeshes);
        }
    }
}
