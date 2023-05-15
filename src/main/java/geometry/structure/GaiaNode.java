package geometry.structure;

import geometry.basic.GaiaBoundingBox;
import geometry.exchangable.GaiaBuffer;
import geometry.exchangable.GaiaBufferDataSet;
import geometry.types.AttributeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

        List<Integer> indiceList = null;
        List<Float> positionList = null;
        List<Float> normalList = null;
        List<Float> texCoordList = null;
        List<GaiaVertex> vertexList = new ArrayList<>();

        LinkedHashMap<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();
        for (Map.Entry<AttributeType, GaiaBuffer> entry : buffers.entrySet()) {
            AttributeType attributeType = entry.getKey();
            GaiaBuffer buffer = entry.getValue();
            if (attributeType == AttributeType.POSITION) {
                float[] positions = buffer.getFloats();
                positionList = ArrayUtils.convertArrayListToFloatArray(positions);
            } else if (attributeType == AttributeType.NORMAL) {
                float[] normals = buffer.getFloats();
                normalList = ArrayUtils.convertArrayListToFloatArray(normals);
            } else if (attributeType == AttributeType.TEXCOORD) {
                float[] texCoords = buffer.getFloats();
                texCoordList = ArrayUtils.convertArrayListToFloatArray(texCoords);
            } else if (attributeType == AttributeType.INDICE) {
                short[] indices = buffer.getShorts();
                indiceList = ArrayUtils.convertIntArrayListToShortArray(indices);
            }
        }
        primitive.setIndices(indiceList);

        for (int i = 0; i < positionList.size() / 3; i++) {
            int vertexIndex = i * 3;

            float positionX = positionList.get(vertexIndex);
            float positionY = positionList.get(vertexIndex + 1);
            float positionZ = positionList.get(vertexIndex + 2);

            float normalX = normalList.get(vertexIndex);
            float normalY = normalList.get(vertexIndex + 1);
            float normalZ = normalList.get(vertexIndex + 2);

            int texcoordIndex = i * 2;
            float texcoordX = texCoordList.get(texcoordIndex);
            float texcoordY = texCoordList.get(texcoordIndex + 1);

            GaiaVertex vertex = new GaiaVertex();
            vertex.setPosition(new Vector3d(positionX, positionY, positionZ));
            vertex.setNormal(new Vector3d(normalX, normalY, normalZ));
            vertex.setTexcoords(new Vector2d(texcoordX, texcoordY));

            vertexList.add(vertex);
        }
        primitive.setVertices(vertexList);
        mesh.getPrimitives().add(primitive);
        this.meshes.add(mesh);
        /*bufferDataSet.getBuffers().forEach((attributeType, buffer) -> {
            GaiaPrimitive primitive = new GaiaPrimitive();
        });*/
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
     * @return
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

    // getTotalIndicesCount
    public static int getTotalIndicesCount(int totalIndices, List<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            List<GaiaMesh> meshes = node.getMeshes();
            List<GaiaNode> children = node.getChildren();
            for (GaiaMesh mesh : meshes) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    totalIndices += primitive.getIndices().size();
                }
            }
            totalIndices = getTotalIndicesCount(totalIndices, children);
        }
        return totalIndices;
    }
    // getTotalIndices
    public static List<Short> getTotalIndices(List<Short> totalIndices, List<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            List<GaiaMesh> meshes = node.getMeshes();
            List<GaiaNode> children = node.getChildren();
            for (GaiaMesh mesh : meshes) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    for (Integer indices : primitive.getIndices()) {
                        totalIndices.add(indices.shortValue());
                    }
                }
            }
            totalIndices = getTotalIndices(totalIndices, children);
        }
        return totalIndices;
    }

    // getTotalVerticesCount
    public static int getTotalVerticesCount(int totalVertices, List<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            List<GaiaMesh> meshes = node.getMeshes();
            List<GaiaNode> children = node.getChildren();
            for (GaiaMesh mesh : meshes) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    totalVertices += primitive.getVertices().size();
                }
            }
            totalVertices = getTotalVerticesCount(totalVertices, children);
        }
        return totalVertices;
    }
    // getTotalVertices
    public static List<Float> getTotalVertices(List<Float> totalVertices, List<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            List<GaiaMesh> meshes = node.getMeshes();
            List<GaiaNode> children = node.getChildren();
            for (GaiaMesh mesh : meshes) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    for (GaiaVertex vertex : primitive.getVertices()) {
                        if (vertex.getPosition() != null) {
                            totalVertices.add((float) vertex.getPosition().get(0));
                            totalVertices.add((float) vertex.getPosition().get(1));
                            totalVertices.add((float) vertex.getPosition().get(2));
                        }
                    }
                }
            }
            totalVertices = getTotalVertices(totalVertices, children);
        }
        return totalVertices;
    }

    //getTotalNormalsCount
    public static int getTotalNormalsCount(int totalNormals, List<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            List<GaiaMesh> meshes = node.getMeshes();
            List<GaiaNode> children = node.getChildren();
            for (GaiaMesh mesh : meshes) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    for (GaiaVertex vertex : primitive.getVertices()) {
                        if (vertex.getNormal() != null) {
                            totalNormals++;
                        }
                    }
                }
            }
            totalNormals = getTotalNormalsCount(totalNormals, children);
        }
        return totalNormals;
    }
    //getTotalNormals
    public static List<Float> getTotalNormals(List<Float> totalNormals, List<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            List<GaiaMesh> meshes = node.getMeshes();
            List<GaiaNode> children = node.getChildren();
            for (GaiaMesh mesh : meshes) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    for (GaiaVertex vertex : primitive.getVertices()) {
                        if (vertex.getNormal() != null) {
                            totalNormals.add((float) vertex.getNormal().get(0));
                            totalNormals.add((float) vertex.getNormal().get(1));
                            totalNormals.add((float) vertex.getNormal().get(2));
                        }
                    }
                }
            }
            totalNormals = getTotalNormals(totalNormals, children);
        }
        return totalNormals;
    }

    //getTotalTexcoordsCount
    public static int getTotalTexcoordsCount(int totalTexCoords, List<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            List<GaiaMesh> meshes = node.getMeshes();
            List<GaiaNode> children = node.getChildren();
            for (GaiaMesh mesh : meshes) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    for (GaiaVertex vertex : primitive.getVertices()) {
                        if (vertex.getTexcoords() != null) {
                            totalTexCoords++;
                        }
                    }
                }
            }
            totalTexCoords = getTotalTexcoordsCount(totalTexCoords, children);
        }
        return totalTexCoords;
    }
    //getTotalTexcoords
    public static List<Float> getTotalTexcoords(List<Float> totalTexCoords, List<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            List<GaiaMesh> meshes = node.getMeshes();
            List<GaiaNode> children = node.getChildren();
            for (GaiaMesh mesh : meshes) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    for (GaiaVertex vertex : primitive.getVertices()) {
                        if (vertex.getTexcoords() != null) {
                            totalTexCoords.add((float) vertex.getTexcoords().get(0));
                            totalTexCoords.add((float) vertex.getTexcoords().get(1));
                        }
                    }
                }
            }
            totalTexCoords = getTotalTexcoords(totalTexCoords, children);
        }
        return totalTexCoords;
    }

    // getTotalColorsCount
    public static int getTotalColorsCount(int totalColors, List<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            List<GaiaMesh> meshes = node.getMeshes();
            List<GaiaNode> children = node.getChildren();
            for (GaiaMesh mesh : meshes) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    for (GaiaVertex vertex : primitive.getVertices()) {
                        if (vertex.getColor() != null) {
                            totalColors++;
                        }
                    }
                }
            }
            totalColors = getTotalColorsCount(totalColors, children);
        }
        return totalColors;
    }
    // getTotalColors
    public static List<Float> getTotalColors(List<Float> totalColors, List<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            List<GaiaMesh> meshes = node.getMeshes();
            List<GaiaNode> children = node.getChildren();
            for (GaiaMesh mesh : meshes) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    for (GaiaVertex vertex : primitive.getVertices()) {
                        if (vertex.getColor() != null) {
                            totalColors.add((float) vertex.getColor().get(0));
                            totalColors.add((float) vertex.getColor().get(1));
                            totalColors.add((float) vertex.getColor().get(2));
                            totalColors.add((float) vertex.getColor().get(3));
                        }
                    }
                }
            }
            totalColors = getTotalColors(totalColors, children);
        }
        return totalColors;
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
