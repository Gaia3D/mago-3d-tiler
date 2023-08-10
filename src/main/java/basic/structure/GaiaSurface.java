package basic.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents a face of a Gaia object.
 * It contains the indices and the face normal.
 * The face normal is calculated by the indices and the vertices.
 * @author znkim
 * @since 1.0.0
 * @see <a href="https://en.wikipedia.org/wiki/Face_normal">Face normal</a>
 */
@Slf4j
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

    public ArrayList<Integer> getIndices() {
        ArrayList<Integer> resultIndices = new ArrayList<>();
        for (GaiaFace face : faces) {
            resultIndices.addAll(face.getIndices());
        }
        return resultIndices;
    }
}
