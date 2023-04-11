package geometry.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector3d;
import util.GeometryUtils;

import java.util.ArrayList;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaPrimitive {
    private Integer AccessorIndices = -1;
    private ArrayList<Integer> indices = new ArrayList<>(); // 3d
    private ArrayList<GaiaVertex> vertices = new ArrayList<>();
    private ArrayList<GaiaSurface> surfaces = new ArrayList<>();

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
