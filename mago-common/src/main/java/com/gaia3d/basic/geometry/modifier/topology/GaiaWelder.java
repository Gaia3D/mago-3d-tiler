package com.gaia3d.basic.geometry.modifier.topology;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.modifier.Modifier;
import com.gaia3d.basic.geometry.octree.GaiaOctree;
import com.gaia3d.basic.geometry.octree.GaiaOctreeVertices;
import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class GaiaWelder extends Modifier {

    private final GaiaWeldOptions weldOptions;

    public GaiaWelder() {
        super();
        this.weldOptions = GaiaWeldOptions.builder().build();
    }

    public GaiaWelder(GaiaWeldOptions settings) {
        super();
        this.weldOptions = settings;
    }

    @Override
    protected void applyPrimitive(Matrix4d productTransformMatrix, GaiaPrimitive primitive) {
        weldVertices(primitive);
        deleteUnusedVertices(primitive);
    }

    public void weldVertices(GaiaPrimitive primitive) {
        GaiaBoundingBox boundingBox = primitive.getBoundingBox(null);
        if (boundingBox == null) {
            return;
        }
        GaiaBoundingBox cubeBoundingBox = boundingBox.createCubeFromMinPosition();
        GaiaOctreeVertices octreeVertices = new GaiaOctreeVertices(null, cubeBoundingBox);
        octreeVertices.addContents(primitive.getVertices());
        octreeVertices.setLimitDepth(10);
        octreeVertices.setLimitBoxSize(1.0);
        octreeVertices.makeTreeByMinVertexCount(50);

        List<GaiaOctree<GaiaVertex>> octreesWithContents = octreeVertices.extractOctreesWithContents();
        Map<GaiaVertex, GaiaVertex> mapVertexToVertexMaster = new HashMap<>();

        for (GaiaOctree<GaiaVertex> octree : octreesWithContents) {
            List<GaiaVertex> vertices = octree.getContents();
            getWeldableVertexMap(mapVertexToVertexMaster, vertices);
        }

        Map<GaiaVertex, GaiaVertex> mapVertexMasters = new HashMap<>();
        for (GaiaVertex vertexMaster : mapVertexToVertexMaster.values()) {
            mapVertexMasters.put(vertexMaster, vertexMaster);
        }

        List<GaiaVertex> newVerticesArray = new ArrayList<>(mapVertexMasters.values());

        Map<GaiaVertex, Integer> vertexIdxMap = new HashMap<>();
        int verticesCount = newVerticesArray.size();
        for (int i = 0; i < verticesCount; i++) {
            vertexIdxMap.put(newVerticesArray.get(i), i);
        }

        // update the indices of the faces
        Map<GaiaFace, GaiaFace> mapDeleteFaces = new HashMap<>();
        for (GaiaSurface surface : primitive.getSurfaces()) {
            int facesCount = surface.getFaces().size();
            for (int j = 0; j < facesCount; j++) {
                GaiaFace face = surface.getFaces().get(j);
                int[] indices = face.getIndices();
                for (int k = 0; k < indices.length; k++) {
                    GaiaVertex vertex = primitive.getVertices().get(indices[k]);
                    GaiaVertex vertexMaster = mapVertexToVertexMaster.get(vertex);
                    int index = vertexIdxMap.get(vertexMaster);
                    indices[k] = index;
                }

                // check indices
                for (int k = 0; k < indices.length; k++) {
                    int index = indices[k];
                    for (int m = k + 1; m < indices.length; m++) {
                        if (index == indices[m]) {
                            // must remove the face
                            mapDeleteFaces.put(face, face);
                        }
                    }
                }
            }

            if (!mapDeleteFaces.isEmpty()) {
                List<GaiaFace> newFaces = new ArrayList<>();
                for (int j = 0; j < facesCount; j++) {
                    GaiaFace face = surface.getFaces().get(j);
                    if (!mapDeleteFaces.containsKey(face)) {
                        newFaces.add(face);
                    }
                }
                surface.setFaces(newFaces);
            }
        }

        // delete no used vertices
        for (GaiaVertex vertex : primitive.getVertices()) {
            if (!mapVertexMasters.containsKey(vertex)) {
                vertex.clear();
            }
        }
        primitive.getVertices().clear();
        primitive.setVertices(newVerticesArray);
    }

    private void getWeldableVertexMap(Map<GaiaVertex, GaiaVertex> mapVertexToVertexMaster, List<GaiaVertex> vertices) {
        Map<GaiaVertex, GaiaVertex> visitedMap = new HashMap<>();
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            if (visitedMap.containsKey(vertex)) {
                continue;
            }

            mapVertexToVertexMaster.put(vertex, vertex);

            for (int j = i + 1; j < verticesCount; j++) {
                GaiaVertex vertex2 = vertices.get(j);
                if (visitedMap.containsKey(vertex2)) {
                    continue;
                }
                if (isWeldable(vertex, vertex2)) {
                    mapVertexToVertexMaster.put(vertex2, vertex);
                    visitedMap.put(vertex, vertex);
                    visitedMap.put(vertex2, vertex2);
                }
            }
        }
    }

    public void deleteUnusedVertices(GaiaPrimitive primitive) {
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

    private boolean isWeldable(GaiaVertex source, GaiaVertex target) {
        // 1rst, check position.
        Vector3d sourcePosition = source.getPosition();
        Vector3d targetPosition = target.getPosition();
        double distance = sourcePosition.distance(targetPosition);
        if (distance > weldOptions.getError()) {
            return false;
        }

        // 2nd, check texCoord.
        Vector2d sourceTexcoords = source.getTexcoords();
        Vector2d targetTexcoords = target.getTexcoords();
        if (weldOptions.isCheckTexCoord() && sourceTexcoords != null && targetTexcoords != null) {
            double texCoordDist = sourceTexcoords.distance(targetTexcoords);
            if (texCoordDist > weldOptions.getError()) {
                return false;
            }
        }

        // 3rd, check normal.
        Vector3d sourceNormal = source.getNormal();
        Vector3d targetNormal = target.getNormal();
        if (weldOptions.isCheckNormal() && sourceNormal != null && targetNormal != null) {
            double dot = sourceNormal.dot(targetNormal);
            if ((1.0 - dot) > weldOptions.getError()) {
                return false;
            }
        }

        // 4th, check color.
        byte[] sourceColor = source.getColor();
        byte[] targetColor = target.getColor();
        if (weldOptions.isCheckColor() && sourceColor != null && targetColor != null) {
            for (int i = 0; i < sourceColor.length; i++) {
                if (Math.abs(sourceColor[i] - targetColor[i]) > weldOptions.getError()) {
                    return false;
                }
            }
        }

        // 5th, check batchId.
        float sourceBatchId = source.getBatchId();
        float targetBatchId = target.getBatchId();
        if (weldOptions.isCheckBatchId()) {
            return sourceBatchId == targetBatchId;
        }
        return true;
    }
}
