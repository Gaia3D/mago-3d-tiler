package geometry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaNode {
    private GaiaNode parent = null;
    private ArrayList<GaiaMesh> meshes = new ArrayList<>();
    private ArrayList<GaiaNode> children = new ArrayList<>();
    //tm double[16]

    //getTotalIndices
    public static int getTotalIndices(int totalIndices, ArrayList<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            ArrayList<GaiaMesh> meshes = node.getMeshes();
            ArrayList<GaiaNode> children = node.getChildren();
            for (GaiaMesh mesh : meshes) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    totalIndices += primitive.getIndices().size();
                }
            }
            totalIndices = getTotalIndices(totalIndices, children);
        }
        return totalIndices;
    }

    //getTotalVertices
    public static int getTotalVertices(int totalVertices, ArrayList<GaiaNode> nodeList) {
        for (GaiaNode node : nodeList) {
            ArrayList<GaiaMesh> meshes = node.getMeshes();
            ArrayList<GaiaNode> children = node.getChildren();
            for (GaiaMesh mesh : meshes) {
                for (GaiaPrimitive primitive : mesh.getPrimitives()) {
                    totalVertices += primitive.getVertices().size();
                }
            }
            totalVertices = getTotalVertices(totalVertices, children);
        }
        return totalVertices;
    }

    //getTotalNormals
    public static int getTotalNormals(int totalNormals, ArrayList<GaiaNode> nodeList) {
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
            totalNormals = getTotalNormals(totalNormals, children);
        }
        return totalNormals;
    }

    //getTotalTexCoords
    public static int getTextureCoordinates(int totalTexCoords, ArrayList<GaiaNode> nodeList) {
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
            totalTexCoords = getTextureCoordinates(totalTexCoords, children);
        }
        return totalTexCoords;
    }

    //getTotalColors
    public static int getTotalColors(int totalColors, ArrayList<GaiaNode> nodeList) {
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
            totalColors = getTotalColors(totalColors, children);
        }
        return totalColors;
    }
}
