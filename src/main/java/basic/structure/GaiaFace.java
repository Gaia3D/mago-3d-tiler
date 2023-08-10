package basic.structure;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

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
public class GaiaFace {
    private ArrayList<Integer> indices = new ArrayList<>();
    private Vector3d faceNormal = new Vector3d();

    public void calculateFaceNormal(List<GaiaVertex> vertices) {
        if (indices.size() < 3) {
            log.error("[calculateFaceNormal Error] : indices.size() < 3");
            return;
        }

        for (int i = 0; i < indices.size(); i+=3) {
            int indices1 = indices.get(i);
            int indices2 = indices.get(i + 1);
            int indices3 = indices.get(i + 2);
            GaiaVertex vertex1 = vertices.get(indices1);
            GaiaVertex vertex2 = vertices.get(indices2);
            GaiaVertex vertex3 = vertices.get(indices3);
            calcNormal(vertex1, vertex2, vertex3);
        }

        Vector3d firstNormal = vertices.get(0).getNormal();
        this.faceNormal = new Vector3d(firstNormal);
    }

    public boolean validateNormal(Vector3d normal) {
        return !Double.isNaN(normal.lengthSquared())
                && !Double.isNaN(normal.x())
                && !Double.isNaN(normal.y())
                && !Double.isNaN(normal.z())
                && !Float.isNaN((float) normal.x())
                && !Float.isNaN((float) normal.y())
                && !Float.isNaN((float) normal.z());
    }

    public static Vector3d calcNormal(Vector3d p1, Vector3d p2, Vector3d p3) {
        Vector3d p2SubP1 = new Vector3d(p2).sub(p1);
        Vector3d p3SubP2 = new Vector3d(p3).sub(p2);
        Vector3d normal = new Vector3d(p2SubP1).cross(p3SubP2);
        normal.normalize();
        return normal;
    }

    public void calcNormal(GaiaVertex vertex1, GaiaVertex vertex2, GaiaVertex vertex3) {
        Vector3d position1 = vertex1.getPosition();
        Vector3d position2 = vertex2.getPosition();
        Vector3d position3 = vertex3.getPosition();
        Vector3d resultNormal = calcNormal(position1, position2, position3);

        if (!validateNormal(resultNormal)) {
            resultNormal = new Vector3d(1.0, 1.0, 1.0);
            resultNormal.normalize();
        }
        vertex1.setNormal(new Vector3d(resultNormal));
        vertex2.setNormal(new Vector3d(resultNormal));
        vertex3.setNormal(new Vector3d(resultNormal));
    }
}
