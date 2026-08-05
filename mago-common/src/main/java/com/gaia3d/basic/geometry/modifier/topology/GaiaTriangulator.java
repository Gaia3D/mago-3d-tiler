package com.gaia3d.basic.geometry.modifier.topology;

import com.gaia3d.basic.geometry.modifier.Modifier;
import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaSurface;
import com.gaia3d.basic.model.GaiaVertex;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class GaiaTriangulator extends Modifier {

    @Override
    protected void applySurface(Matrix4d productTransformMatrix, List<GaiaVertex> vertices, GaiaSurface surface) {
        makeTriangleFaces(surface);
    }

    private void makeTriangleFaces(GaiaSurface surface) {
        List<GaiaFace> facesToAdd = new ArrayList<>();
        List<GaiaFace> triFaces = new ArrayList<>();
        for (GaiaFace face : surface.getFaces()) {
            triFaces.clear();
            triFaces = getTriangleFaces(face, triFaces);
            facesToAdd.addAll(triFaces);
        }

        surface.getFaces().clear();
        surface.getFaces().addAll(facesToAdd);
    }

    private List<GaiaFace> getTriangleFaces(GaiaFace sourceFace, List<GaiaFace> resultGaiaFaces) {
        if (resultGaiaFaces == null) {
            resultGaiaFaces = new ArrayList<>();
        }
        int[] indices = sourceFace.getIndices();
        Vector3d normal = sourceFace.getFaceNormal();
        int indicesCount = indices.length;

        for (int i = 0; i < indicesCount - 2; i += 3) {
            if (i + 2 >= indicesCount) {
                log.error("[ERROR] i + 2 >= indicesCount.");
            }
            GaiaFace gaiaTriangleFace = new GaiaFace();
            gaiaTriangleFace.setIndices(new int[]{indices[i], indices[i + 1], indices[i + 2]});
            if (normal != null) {
                gaiaTriangleFace.setFaceNormal(new Vector3d(normal));
            }
            resultGaiaFaces.add(gaiaTriangleFace);
        }
        return resultGaiaFaces;
    }
}
