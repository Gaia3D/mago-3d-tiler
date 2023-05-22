package geometry.structure;

import geometry.basic.GaiaBoundingBox;
import geometry.exchangable.GaiaBuffer;
import geometry.exchangable.GaiaBufferDataSet;
import geometry.types.AttributeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.joml.*;
import org.lwjgl.opengl.GL20;
import util.ArrayUtils;

import java.util.*;
import java.util.List;

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

        List<Integer> indiceList = new ArrayList<>();
        List<Float> positionList = new ArrayList<>();
        List<Float> normalList = new ArrayList<>();
        List<Float> texCoordList = new ArrayList<>();
        List<GaiaVertex> vertexList = new ArrayList<>();

        LinkedHashMap<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();
        for (Map.Entry<AttributeType, GaiaBuffer> entry : buffers.entrySet()) {
            AttributeType attributeType = entry.getKey();
            GaiaBuffer buffer = entry.getValue();
            if (attributeType == AttributeType.POSITION) {
                float[] positions = buffer.getFloats();
                positionList = ArrayUtils.convertListToFloatArray(positions);
            } else if (attributeType == AttributeType.NORMAL) {
                float[] normals = buffer.getFloats();
                normalList = ArrayUtils.convertListToFloatArray(normals);
            } else if (attributeType == AttributeType.TEXCOORD) {
                float[] texCoords = buffer.getFloats();
                texCoordList = ArrayUtils.convertListToFloatArray(texCoords);
            } else if (attributeType == AttributeType.INDICE) {
                short[] indices = buffer.getShorts();
                indiceList = ArrayUtils.convertIntListToShortArray(indices);
            }
        }
        primitive.setIndices(indiceList);

        for (int i = 0; i < positionList.size() / 3; i++) {
            int vertexIndex = i * 3;
            GaiaVertex vertex = new GaiaVertex();
            if (CollectionUtils.isNotEmpty(positionList)) {
                float positionX = positionList.get(vertexIndex);
                float positionY = positionList.get(vertexIndex + 1);
                float positionZ = positionList.get(vertexIndex + 2);
                vertex.setPosition(new Vector3d(positionX, positionY, positionZ));
            }
            if (CollectionUtils.isNotEmpty(normalList)) {
                float normalX = normalList.get(vertexIndex);
                float normalY = normalList.get(vertexIndex + 1);
                float normalZ = normalList.get(vertexIndex + 2);
                vertex.setNormal(new Vector3d(normalX, normalY, normalZ));
            }
            if (CollectionUtils.isNotEmpty(texCoordList)) {
                int texcoordIndex = i * 2;
                float texcoordX = texCoordList.get(texcoordIndex);
                float texcoordY = texCoordList.get(texcoordIndex + 1);
                vertex.setTexcoords(new Vector2d(texcoordX, texcoordY));
            }
            vertexList.add(vertex);
        }
        primitive.setVertices(vertexList);
        mesh.getPrimitives().add(primitive);
        this.meshes.add(mesh);
    }

    public void renderNode(int program) {
        int uObjectTransformMatrix = GL20.glGetUniformLocation(program, "uObjectTransformMatrix");
        float[] objectTransformMatrixBuffer = new float[16];
        Matrix4d preMultipliedTransformMatrix = this.getPreMultipliedTransformMatrix();
        //Matrix4d preMultipliedTransformMatrix = new Matrix4d().identity();
        preMultipliedTransformMatrix.get(objectTransformMatrixBuffer);
        GL20.glUniformMatrix4fv(uObjectTransformMatrix, false, objectTransformMatrixBuffer);

        for (GaiaMesh mesh : meshes) {
            mesh.renderMesh(program);
        }
        for (GaiaNode child : children) {
            child.renderNode(program);
        }
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
        //node.setPreMultipliedTransformMatrix(new Matrix4d().identity());
        if (node.getParent() != null) {
            Matrix4d parentPreMultipliedTransformMatrix = node.getParent().getPreMultipliedTransformMatrix();
            Matrix4d preMultipliedTransformMatrix = node.getPreMultipliedTransformMatrix();
            parentPreMultipliedTransformMatrix.mul(preMultipliedTransformMatrix, preMultipliedTransformMatrix);
        }
        for (GaiaNode child : node.getChildren()) {
            child.recalculateTransform();
        }
    }

    public Matrix4d toGaiaBufferSets(List<GaiaBufferDataSet> bufferSets, Matrix4d transformMatrix) {
        if (bufferSets == null) {
            bufferSets = new ArrayList<GaiaBufferDataSet>();
        }
        for (GaiaMesh mesh : this.getMeshes()) {
            if (transformMatrix == null && this.getPreMultipliedTransformMatrix() != null) {
                transformMatrix = this.getPreMultipliedTransformMatrix();
            }
            mesh.toGaiaBufferSets(bufferSets);
        }
        for (GaiaNode child : this.getChildren()) {
            transformMatrix = child.toGaiaBufferSets(bufferSets, transformMatrix);
        }
        return transformMatrix;
    }
}
