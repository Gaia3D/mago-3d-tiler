package com.gaia3d.basic.geometry.modifier.topology;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.modifier.Modifier;
import com.gaia3d.basic.geometry.octree.GaiaOctree;
import com.gaia3d.basic.geometry.octree.GaiaOctreeVertices;
import com.gaia3d.basic.halfedge.UnionFind;
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
        if (boundingBox == null) return;

        GaiaBoundingBox cubeBoundingBox = boundingBox.createCubeFromMinPosition();

        GaiaOctreeVertices octreeVertices = new GaiaOctreeVertices(null, cubeBoundingBox);
        octreeVertices.addContents(primitive.getVertices());
        octreeVertices.setLimitDepth(10);
        octreeVertices.setLimitBoxSize(0.2);
        octreeVertices.makeTreeByMinVertexCount(50);

        List<GaiaOctree<GaiaVertex>> octrees = octreeVertices.extractOctreesWithContents();
        if(octrees == null || octrees.isEmpty()) {
            log.debug("Welding : no octree cells with vertices. skipping welding.");
            return;
        }
        GaiaOctree<GaiaVertex> octreeSample = octrees.get(0);
        log.debug("Welding : octree depth : " + octreeSample.getDepth());

        // 🔥 1. Union-Find
        UnionFind<GaiaVertex> uf = new UnionFind<>();

        log.debug("Welding : Union-Find");
        for (GaiaVertex v : primitive.getVertices()) {
            uf.makeSet(v);
        }

        // 🔥 2. Weld dentro de cada celda
        log.debug("Welding : Checking weldable vertices in octree cells");
        int octreesCount = octrees.size();
        log.debug("Welding : octrees count: " + octreesCount);
        int currOct=0;
        for (GaiaOctree<GaiaVertex> octree : octrees) {
            currOct++;

            List<GaiaVertex> vertices = octree.getContents();
            int n = vertices.size();

            if(currOct % 2000 == 0 || currOct == octreesCount) {
                log.debug("Welding : current octree {} / {}", currOct, octreesCount);
                log.debug("Welding : vertices count in octree : " + n);
            }

            for (int i = 0; i < n; i++) {
                GaiaVertex v1 = vertices.get(i);

                for (int j = i + 1; j < n; j++) {
                    GaiaVertex v2 = vertices.get(j);

                    if (isWeldable(v1, v2)) {
                        uf.union(v1, v2);
                    }
                }
            }
        }

        // 🔥 3. Crear mapa vertex → master
        log.debug("Welding : making vertex-master map");
        Map<GaiaVertex, GaiaVertex> vertexToMaster = new HashMap<>();
        Map<GaiaVertex, Integer> masterToIndex = new HashMap<>();
        List<GaiaVertex> newVertices = new ArrayList<>();

        for (GaiaVertex v : primitive.getVertices()) {
            GaiaVertex root = uf.find(v);

            vertexToMaster.put(v, root);

            if (!masterToIndex.containsKey(root)) {
                int index = newVertices.size();
                masterToIndex.put(root, index);
                newVertices.add(root);
            }
        }

        // 🔥 4. Reindexar caras + eliminar degeneradas
        log.debug("Welding : reIndexing face vertices idx");
        for (GaiaSurface surface : primitive.getSurfaces()) {

            List<GaiaFace> newFaces = new ArrayList<>();

            for (GaiaFace face : surface.getFaces()) {
                int[] indices = face.getIndices();
                boolean degenerate = false;

                for (int i = 0; i < indices.length; i++) {
                    GaiaVertex v = primitive.getVertices().get(indices[i]);
                    GaiaVertex master = vertexToMaster.get(v);
                    indices[i] = masterToIndex.get(master);
                }

                // comprobar degeneración
                for (int i = 0; i < indices.length; i++) {
                    for (int j = i + 1; j < indices.length; j++) {
                        if (indices[i] == indices[j]) {
                            degenerate = true;
                            break;
                        }
                    }
                    if (degenerate) break;
                }

                if (!degenerate) {
                    newFaces.add(face);
                }
            }

            surface.setFaces(newFaces);
        }

        // 🔥 5. Reemplazar vertices
        primitive.getVertices().clear();
        primitive.setVertices(newVertices);
    }

    public void deleteUnusedVertices(GaiaPrimitive primitive) {
        if (primitive == null) {
            return;
        }

        List<GaiaVertex> vertices = primitive.getVertices();
        List<GaiaSurface> surfaces = primitive.getSurfaces();

        if (vertices == null || vertices.isEmpty()) {
            return;
        }

        if (surfaces == null || surfaces.isEmpty()) {
            vertices.clear();
            return;
        }
        log.debug("Welding : deleting no used vertices");

        final int vertexCount = vertices.size();

        // 1) Marcar vértices usados
        boolean[] used = new boolean[vertexCount];

        log.debug("Deleting NO-Used vertices : marking used vertices");
        for (GaiaSurface surface : surfaces) {
            if (surface == null || surface.getFaces() == null) {
                continue;
            }

            for (GaiaFace face : surface.getFaces()) {
                if (face == null || face.getIndices() == null) {
                    continue;
                }

                int[] indices = face.getIndices();

                for (int index : indices) {
                    if (index >= 0 && index < vertexCount) {
                        used[index] = true;
                    }
                }
            }
        }

        // 2) Contar usados
        int usedVertexCount = 0;
        for (boolean b : used) {
            if (b) {
                usedVertexCount++;
            }
        }

        // Si todos están usados, no hay nada que hacer
        if (usedVertexCount == vertexCount) {
            return;
        }

        // Si ninguno está usado
        if (usedVertexCount == 0) {
            vertices.clear();
            return;
        }

        // 3) Crear oldIndex -> newIndex
        int[] oldToNew = new int[vertexCount];

        log.debug("Deleting NO-Used vertices : creating oldIdx -> newIdx");
        int newIndex = 0;
        for (int oldIndex = 0; oldIndex < vertexCount; oldIndex++) {
            if (used[oldIndex]) {
                oldToNew[oldIndex] = newIndex++;
            } else {
                oldToNew[oldIndex] = -1;

                // Opcional. Si GaiaVertex tiene buffers grandes, esto ayuda antes de liberar referencias.
                GaiaVertex unusedVertex = vertices.get(oldIndex);
                if (unusedVertex != null) {
                    unusedVertex.clear();
                }
            }
        }

        // 4) Compactar lista de vértices manteniendo el orden original
        List<GaiaVertex> compactedVertices = new ArrayList<>(usedVertexCount);

        for (int oldIndex = 0; oldIndex < vertexCount; oldIndex++) {
            if (used[oldIndex]) {
                compactedVertices.add(vertices.get(oldIndex));
            }
        }

        // 5) Actualizar índices de las caras
        log.debug("Deleting NO-Used vertices : updating face vertex indices");
        int surfacesCount =  surfaces.size();
        log.debug("SurfacesCount : " + surfacesCount);
        int s = 0;
        for (GaiaSurface surface : surfaces) {
            s++;
            log.debug("Surface : " + surface);
            if (surface == null || surface.getFaces() == null) {
                continue;
            }
            int f = 0;
            int facesCount =  surface.getFaces().size();
            for (GaiaFace face : surface.getFaces()) {
                f++;
                if (face == null || face.getIndices() == null) {
                    continue;
                }

                if(f % 50000 == 0){
                    log.debug("upDating face Idx : " + f + " / " + facesCount);
                }

                int[] indices = face.getIndices();

                for (int i = 0; i < indices.length; i++) {
                    int oldIndex = indices[i];

                    if (oldIndex >= 0 && oldIndex < vertexCount) {
                        indices[i] = oldToNew[oldIndex];
                    } else {
                        indices[i] = -1; // índice inválido
                    }
                }
            }
        }

        int compactedVerticesCount = compactedVertices.size();
        log.debug("Welding : vertices welded : " + compactedVerticesCount + " / "  + vertexCount);

        // 6) Sustituir vertices
        primitive.setVertices(compactedVertices);
    }

    public void deleteUnusedVertices_original(GaiaPrimitive primitive) {
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
            if (texCoordDist > weldOptions.getTexCoordError()) {
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
