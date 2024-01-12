package com.gaia3d.basic.structure;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;

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
public class GaiaSurface implements Serializable {
    private ArrayList<GaiaFace> faces = new ArrayList<>();

    public void calculateNormal(List<GaiaVertex> vertices) {
        for (GaiaFace face : faces) {
            face.calculateFaceNormal(vertices);
        }
    }

    public int[] getIndices() {
        int index = 0;
        int indicesCount = getIndicesCount();
        int[] resultIndices = new int[indicesCount];
        for (GaiaFace face : faces) {
            for (int indices : face.getIndices()) {
                resultIndices[index++] = indices;
            }
        }
        return resultIndices;
    }

    public int getIndicesCount() {
        int count = 0;
        for (GaiaFace face : faces) {
            count += face.getIndices().length;
        }
        return count;
    }

    public void clear() {
        faces.forEach(GaiaFace::clear);
        faces.clear();
    }
}
