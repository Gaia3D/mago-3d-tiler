package geometry.structure;

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
public class GaiaSurface {
    private ArrayList<GaiaFace> faces = new ArrayList<>();

    public void calculateNormal(List<GaiaVertex> vertices) {
        for (GaiaFace face : faces) {
            face.calculateFaceNormal(vertices);
        }
    }
}
