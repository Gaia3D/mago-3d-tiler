package geometry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaNode {
    private GaiaNode parent = null;
    private ArrayList<GaiaMesh> meshes = new ArrayList<>();
    private ArrayList<GaiaNode> children = new ArrayList<>();
    //tm double[16]

    // getTotalIndicesCount
    public static int getTotalIndicesCount(int totalIndices, ArrayList<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            ArrayList<GaiaMesh> meshes = node.getMeshes();
            ArrayList<GaiaNode> children = node.getChildren();
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
    public static ArrayList<Short> getTotalIndices(ArrayList<Short> totalIndices, ArrayList<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            ArrayList<GaiaMesh> meshes = node.getMeshes();
            ArrayList<GaiaNode> children = node.getChildren();
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
    public static int getTotalVerticesCount(int totalVertices, ArrayList<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            ArrayList<GaiaMesh> meshes = node.getMeshes();
            ArrayList<GaiaNode> children = node.getChildren();
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
    public static ArrayList<Float> getTotalVertices(ArrayList<Float> totalVertices, ArrayList<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            ArrayList<GaiaMesh> meshes = node.getMeshes();
            ArrayList<GaiaNode> children = node.getChildren();
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
    public static int getTotalNormalsCount(int totalNormals, ArrayList<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            ArrayList<GaiaMesh> meshes = node.getMeshes();
            ArrayList<GaiaNode> children = node.getChildren();
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
    public static ArrayList<Float> getTotalNormals(ArrayList<Float> totalNormals, ArrayList<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            ArrayList<GaiaMesh> meshes = node.getMeshes();
            ArrayList<GaiaNode> children = node.getChildren();
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

    //getTotalTextureCoordinatesCount
    public static int getTotalTextureCoordinatesCount(int totalTexCoords, ArrayList<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            ArrayList<GaiaMesh> meshes = node.getMeshes();
            ArrayList<GaiaNode> children = node.getChildren();
            for (GaiaMesh mesh : meshes) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    for (GaiaVertex vertex : primitive.getVertices()) {
                        if (vertex.getTextureCoordinates() != null) {
                            totalTexCoords++;
                        }
                    }
                }
            }
            totalTexCoords = getTotalTextureCoordinatesCount(totalTexCoords, children);
        }
        return totalTexCoords;
    }
    //getTotalTextureCoordinates
    public static ArrayList<Float> getTotalTextureCoordinates(ArrayList<Float> totalTexCoords, ArrayList<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            ArrayList<GaiaMesh> meshes = node.getMeshes();
            ArrayList<GaiaNode> children = node.getChildren();
            for (GaiaMesh mesh : meshes) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    for (GaiaVertex vertex : primitive.getVertices()) {
                        if (vertex.getTextureCoordinates() != null) {
                            totalTexCoords.add((float) vertex.getTextureCoordinates().get(0));
                            totalTexCoords.add((float) vertex.getTextureCoordinates().get(1));
                        }
                    }
                }
            }
            totalTexCoords = getTotalTextureCoordinates(totalTexCoords, children);
        }
        return totalTexCoords;
    }

    // getTotalColorsCount
    public static int getTotalColorsCount(int totalColors, ArrayList<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            ArrayList<GaiaMesh> meshes = node.getMeshes();
            ArrayList<GaiaNode> children = node.getChildren();
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
    public static ArrayList<Float> getTotalColors(ArrayList<Float> totalColors, ArrayList<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            ArrayList<GaiaMesh> meshes = node.getMeshes();
            ArrayList<GaiaNode> children = node.getChildren();
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
}
