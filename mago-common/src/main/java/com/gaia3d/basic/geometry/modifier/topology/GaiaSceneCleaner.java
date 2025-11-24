package com.gaia3d.basic.geometry.modifier.topology;

import com.gaia3d.basic.geometry.modifier.Modifier;
import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.util.*;

@Slf4j
public class GaiaSceneCleaner extends Modifier {

    @Override
    protected void applyPrimitive(Matrix4d productTransformMatrix, GaiaPrimitive primitive) {
        int facesDeletedCount = 0;
        for (GaiaSurface surface : primitive.getSurfaces()) {
            facesDeletedCount = surface.deleteDegeneratedFaces(primitive.getVertices());
        }

        if (facesDeletedCount > 0) {
            deleteNoUsedVertices(primitive);
        }
    }

    public int deleteDegeneratedFaces(GaiaSurface surface, List<GaiaVertex> vertices) {
        Set<GaiaFace> facesToDelete = new HashSet<>();
        for (GaiaFace face : surface.getFaces()) {
            if (face.isDegenerated(vertices)) {
                facesToDelete.add(face);
            }
        }
        int facesToDeleteCount = facesToDelete.size();
        surface.getFaces().removeAll(facesToDelete);

        return facesToDeleteCount;
    }

    public void deleteNoUsedVertices(GaiaPrimitive primitive) {
        // Sometimes, there are no used vertices
        // The no used vertices must be deleted (vertex indices of the faces will be modified!)
        Map<GaiaVertex, Integer> vertexIdxMap = new HashMap<>();
        int surfacesCount = primitive.getSurfaces().size();
        for (int i = 0; i < surfacesCount; i++) {
            GaiaSurface surface = primitive.getSurfaces().get(i);
            List<GaiaFace> faces = surface.getFaces();
            for (GaiaFace face : faces) {
                int[] indices = face.getIndices();
                for (int index : indices) {
                    GaiaVertex vertex = primitive.getVertices().get(index);
                    vertexIdxMap.put(vertex, index);
                }
            }
        }

        int vertexCount = primitive.getVertices().size();
        for (int i = 0; i < vertexCount; i++) {
            GaiaVertex vertex = primitive.getVertices().get(i);
            if (!vertexIdxMap.containsKey(vertex)) {
                vertex.clear();
            }
        }

        vertexCount = primitive.getVertices().size();
        int usedVertexCount = vertexIdxMap.size();
        if (vertexCount != usedVertexCount) {
            // Exists no used vertices
            List<GaiaVertex> usedVertices = new ArrayList<>();
            int idx = 0;
            Map<GaiaVertex, Integer> vertexIdxMap2 = new HashMap<>();
            for (GaiaVertex vertex : vertexIdxMap.keySet()) {
                usedVertices.add(vertex);
                vertexIdxMap2.put(vertex, idx);
                idx++;
            }

            // now, update the indices of the faces
            for (int i = 0; i < surfacesCount; i++) {
                GaiaSurface surface = primitive.getSurfaces().get(i);
                List<GaiaFace> faces = surface.getFaces();
                for (GaiaFace face : faces) {
                    int[] indices = face.getIndices();
                    for (int j = 0; j < indices.length; j++) {
                        GaiaVertex vertex = primitive.getVertices().get(indices[j]);
                        idx = vertexIdxMap2.get(vertex);
                        indices[j] = idx;
                    }
                }
            }

            // Finally, update the vertices
            primitive.getVertices().clear();
            primitive.setVertices(usedVertices);
        }
    }
}
