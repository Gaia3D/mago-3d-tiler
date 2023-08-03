package basic.structure;

import basic.exchangable.GaiaBuffer;
import basic.exchangable.GaiaBufferDataSet;
import basic.geometry.GaiaBoundingBox;
import basic.types.AttributeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
        List<Float> colorList = new ArrayList<>();
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
                float[] colors = buffer.getFloats();
                colorList = ArrayUtils.convertListToFloatArray(colors);
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
            if (CollectionUtils.isNotEmpty(colorList)) {
                int colorIndex = i * 4;
                float colorR = colorList.get(colorIndex);
                float colorG = colorList.get(colorIndex + 1);
                float colorB = colorList.get(colorIndex + 2);
                float colorA = colorList.get(colorIndex + 3);
                vertex.setColor(new Vector4d(colorR, colorG, colorB, colorA));
            }
            if (CollectionUtils.isNotEmpty(texCoordList)) {
                int texcoordIndex = i * 2;
                float texcoordX = texCoordList.get(texcoordIndex);
                float texcoordY = texCoordList.get(texcoordIndex + 1);
                vertex.setTexcoords(new Vector2d(texcoordX, texcoordY));
            }
            if (CollectionUtils.isNotEmpty(batchIdList)) {
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

    public void translate(Vector3d translation) {
        for (GaiaMesh mesh : this.getMeshes()) {
            mesh.translate(translation);
        }
        for (GaiaNode child : this.getChildren()) {
            child.translate(translation);
        }
    }
}
