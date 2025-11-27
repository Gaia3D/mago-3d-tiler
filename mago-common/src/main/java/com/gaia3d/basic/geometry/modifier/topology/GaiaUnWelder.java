package com.gaia3d.basic.geometry.modifier.topology;

import com.gaia3d.basic.geometry.modifier.Modifier;
import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaVertex;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GaiaUnWelder extends Modifier {

    public GaiaUnWelder() {
        super();
    }

    @Override
    protected void applyPrimitive(Matrix4d productTransformMatrix, GaiaPrimitive primitive) {
        unweldVertices(primitive);
    }

    public void unweldVertices(GaiaPrimitive primitive) {
        List<GaiaVertex> newVertices = new ArrayList<>();
        List<GaiaFace> faces = primitive.extractGaiaAllFaces(null);
        for (GaiaFace face : faces) {
            int[] indices = face.getIndices();
            int[] newIndices = new int[indices.length];
            for (int j = 0; j < indices.length; j++) {
                GaiaVertex vertex = primitive.getVertices().get(indices[j]);
                GaiaVertex newVertex = vertex.clone();
                newVertices.add(newVertex);
                newIndices[j] = newVertices.size() - 1;
            }

            face.setIndices(newIndices);
        }

        if (primitive.getVertices() != null) {
            primitive.getVertices().forEach(GaiaVertex::clear);
            primitive.getVertices().clear();
        }
        primitive.setVertices(newVertices);
    }
}
