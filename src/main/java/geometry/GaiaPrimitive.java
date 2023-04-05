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
public class GaiaPrimitive {
    private Integer AccessorIndices = -1;
    private ArrayList<Integer> indices = new ArrayList<>(); // 3d
    private ArrayList<GaiaVertex> vertices = new ArrayList<>();
    private ArrayList<GaiaSurface> surfaces = new ArrayList<>();
}
