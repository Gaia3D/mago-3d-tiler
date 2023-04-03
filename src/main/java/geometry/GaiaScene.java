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
public class GaiaScene {
    private ArrayList<GaiaNode> nodes = new ArrayList<>();
    private ArrayList<GaiaMaterial> materials = new ArrayList<>();

    // getTotalIndices
    public int getTotalIndices() {
        return GaiaNode.getTotalIndices(0, nodes);
    }

    // getTotalVertices
    public int getTotalVertices() {
        return GaiaNode.getTotalVertices(0, nodes);
    }

    //getTotalNormals
    public int getTotalNormals() {
        return GaiaNode.getTotalNormals(0, nodes);
    }

    //getTotalTexCoords
    public int getTotalTexCoords() {
        return GaiaNode.getTextureCoordinates(0, nodes);
    }

    //getTotalColors
    public int getTotalColors() {
        return GaiaNode.getTotalColors(0, nodes);
    }
}
