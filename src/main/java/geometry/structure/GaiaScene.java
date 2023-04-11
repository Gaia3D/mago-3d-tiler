package geometry.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaScene {
    private ArrayList<GaiaNode> nodes = new ArrayList<>();
    private ArrayList<GaiaMaterial> materials = new ArrayList<>();

    // getTotalIndicesCount
    public int getTotalIndicesCount() {
        return GaiaNode.getTotalIndicesCount(0, nodes);
    }
    // getTotalIndices
    public ArrayList<Short> getTotalIndices() {
        return GaiaNode.getTotalIndices(new ArrayList<Short>(), nodes);
    }

    // getTotalVerticesCount
    public int getTotalVerticesCount() {
        return GaiaNode.getTotalVerticesCount(0, nodes);
    }
    // getTotalVertices
    public ArrayList<Float> getTotalVertices() {
        return GaiaNode.getTotalVertices(new ArrayList<Float>(), nodes);
    }

    //getTotalNormalsCount
    public int getTotalNormalsCount() {
        return GaiaNode.getTotalNormalsCount(0, nodes);
    }
    //getTotalNormals
    public ArrayList<Float> getTotalNormals() {
        return GaiaNode.getTotalNormals(new ArrayList<Float>(), nodes);
    }

    //getTotalTexCoordsCount
    public int getTotalTextureCoordinatesCount() {
        return GaiaNode.getTotalTextureCoordinatesCount(0, nodes);
    }
    //getTotalTexCoords
    public ArrayList<Float> getTotalTextureCoordinates() {
        return GaiaNode.getTotalTextureCoordinates(new ArrayList<Float>(), nodes);
    }

    //getTotalColorsCount
    public int getTotalColorsCount() {
        return GaiaNode.getTotalColorsCount(0, nodes);
    }
    //getTotalColors
    public ArrayList<Float> getTotalColors() {
        return GaiaNode.getTotalColors(new ArrayList<Float>(), nodes);
    }

    public int getTotalTextureSize() {
        //return GaiaMaterial.getTotalTextureSize(0, materials);
        return 0;
    }
}
