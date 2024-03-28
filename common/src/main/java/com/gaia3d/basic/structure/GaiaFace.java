package com.gaia3d.basic.structure;

import com.gaia3d.util.GeometryUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.Serializable;
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
public class GaiaFace implements Serializable {
    private int[] indices;
    private Vector3d faceNormal = new Vector3d();

    public void calculateFaceNormal(List<GaiaVertex> vertices) {
        if (indices.length < 3) {
            log.error("[Error][calculateFaceNormal] : not enough indices. (indices.length < 3)");
            return;
        }
        for (int i = 0; i < indices.length; i+=3) {
            int indices1 = indices[i];
            int indices2 = indices[i + 1];
            int indices3 = indices[i + 2];
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

    public Vector3d calcNormal(Vector3d p1, Vector3d p2, Vector3d p3) {
        Vector3d p2SubP1 = new Vector3d(p2).sub(p1);
        Vector3d p3SubP2 = new Vector3d(p3).sub(p2);
        Vector3d normal = new Vector3d(p2SubP1).cross(p3SubP2);
        normal.normalize();
        p3SubP2 = null;
        p2SubP1 = null;
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

    public void clear() {
        indices = null;
        faceNormal = null;
    }

    public GaiaFace clone() {
        return new GaiaFace(indices.clone(), new Vector3d(faceNormal));
    }

    public boolean hasCoincidentIndices(GaiaFace face) {
        for (int i = 0; i < indices.length; i++) {
            for (int j = 0; j < face.getIndices().length; j++) {
                if (indices[i] == face.getIndices()[j]) {
                    return true;
                }
            }
        }
        return false;
    }

    public double calculateArea(List<GaiaVertex> vertices) {
        double area = 0.0;
        for (int i = 0; i < indices.length; i+=3) {
            int indices1 = indices[i];
            int indices2 = indices[i + 1];
            int indices3 = indices[i + 2];
            GaiaVertex vertex1 = vertices.get(indices1);
            GaiaVertex vertex2 = vertices.get(indices2);
            GaiaVertex vertex3 = vertices.get(indices3);
            area += GeometryUtils.getTriangleArea(vertex1, vertex2, vertex3);
        }
        return area;
    }
}
