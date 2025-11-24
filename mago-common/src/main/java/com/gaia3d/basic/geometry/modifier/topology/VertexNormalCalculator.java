package com.gaia3d.basic.geometry.modifier.topology;

import com.gaia3d.basic.geometry.modifier.Modifier;
import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaSurface;
import com.gaia3d.basic.model.GaiaVertex;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class VertexNormalCalculator extends Modifier {

    @Override
    protected void applyPrimitive(Matrix4d productTransformMatrix, GaiaPrimitive primitive) {
        List<GaiaVertex> vertices = primitive.getVertices();
        List<GaiaSurface> surfaces = primitive.getSurfaces();
        for (GaiaSurface surface : surfaces) {
            applySurface(surface, vertices);
        }
    }

    protected void applySurface(GaiaSurface surface, List<GaiaVertex> vertices) {
        List<GaiaFace> faces = surface.getFaces();
        for (GaiaFace face : faces) {
            calculateFaceNormal(face, vertices);
        }
        calculateVertexNormals(surface, vertices);
    }

    public void calculateVertexNormals(GaiaSurface surface, List<GaiaVertex> vertices) {
        Map<GaiaVertex, List<GaiaFace>> mapVertexFaces = getMapVertexFaces(surface, vertices);
        for (Map.Entry<GaiaVertex, List<GaiaFace>> entry : mapVertexFaces.entrySet()) {
            GaiaVertex vertex = entry.getKey();
            List<GaiaFace> vertexFaces = entry.getValue();
            Vector3d normal = new Vector3d();
            for (GaiaFace face : vertexFaces) {
                normal.add(face.getFaceNormal());
            }
            normal.normalize();
            vertex.setNormal(normal);
        }
    }

    public Map<GaiaVertex, List<GaiaFace>> getMapVertexFaces(GaiaSurface surface, List<GaiaVertex> vertices) {
        Map<GaiaVertex, List<GaiaFace>> mapVertexFaces = new HashMap<>();
        for (GaiaFace face : surface.getFaces()) {
            int[] indices = face.getIndices();
            for (int index : indices) {
                GaiaVertex vertex = vertices.get(index);
                if (!mapVertexFaces.containsKey(vertex)) {
                    mapVertexFaces.put(vertex, new ArrayList<>());
                }
                mapVertexFaces.get(vertex).add(face);
            }
        }
        return mapVertexFaces;
    }

    protected void calculateFaceNormal(GaiaFace face, List<GaiaVertex> vertices) {
        int[] indices = face.getIndices();
        if (indices.length < 3) {
            log.error("[ERROR] calculateFaceNormal not enough indices. (indices.length < 3)");
            return;
        }
        for (int i = 0; i < indices.length; i += 3) {
            int indices1 = indices[i];
            int indices2 = indices[i + 1];
            int indices3 = indices[i + 2];
            GaiaVertex vertex1 = vertices.get(indices1);
            GaiaVertex vertex2 = vertices.get(indices2);
            GaiaVertex vertex3 = vertices.get(indices3);
            face.setFaceNormal(calcNormal(vertex1, vertex2, vertex3));
        }
    }

    protected Vector3d calcNormal(GaiaVertex vertex1, GaiaVertex vertex2, GaiaVertex vertex3) {
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

        return resultNormal;
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

    protected boolean validateNormal(Vector3d normal) {
        return !Double.isNaN(normal.lengthSquared()) && !Double.isNaN(normal.x()) && !Double.isNaN(normal.y()) && !Double.isNaN(normal.z()) && !Float.isNaN((float) normal.x()) && !Float.isNaN((float) normal.y()) && !Float.isNaN((float) normal.z());
    }
}
