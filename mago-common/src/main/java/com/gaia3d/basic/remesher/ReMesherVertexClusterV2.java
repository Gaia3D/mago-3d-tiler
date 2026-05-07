package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.geometry.modifier.topology.GaiaNormalCleaner;
import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3i;

import java.util.*;

import org.joml.Vector3d;

@Slf4j
public class ReMesherVertexClusterV2 {

    static class EdgeKey {
        int a;
        int b;

        EdgeKey(int i0, int i1) {
            if (i0 < i1) {
                this.a = i0;
                this.b = i1;
            } else {
                this.a = i1;
                this.b = i0;
            }
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof EdgeKey)) return false;
            EdgeKey other = (EdgeKey) obj;
            return a == other.a && b == other.b;
        }

        @Override
        public int hashCode() {
            return 31 * a + b;
        }
    }

    static class CellStats {
        Vector3i cellIndex;

        int triangleCount = 0;
        double areaSum = 0.0;

        Vector3d min = new Vector3d(
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY,
                Double.POSITIVE_INFINITY
        );

        Vector3d max = new Vector3d(
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY,
                Double.NEGATIVE_INFINITY
        );

        boolean remeshable = false;

        CellStats(Vector3i cellIndex) {
            this.cellIndex = new Vector3i(cellIndex);
        }

        void addPoint(Vector3d p) {
            if (p.x < min.x) min.x = p.x;
            if (p.y < min.y) min.y = p.y;
            if (p.z < min.z) min.z = p.z;

            if (p.x > max.x) max.x = p.x;
            if (p.y > max.y) max.y = p.y;
            if (p.z > max.z) max.z = p.z;
        }

        double sizeX() {
            return max.x - min.x;
        }

        double sizeY() {
            return max.y - min.y;
        }

        double sizeZ() {
            return max.z - min.z;
        }

        double maxDim() {
            return Math.max(sizeX(), Math.max(sizeY(), sizeZ()));
        }

        double minDim() {
            return Math.min(sizeX(), Math.min(sizeY(), sizeZ()));
        }
    }

    private static double calculateFaceArea(GaiaFace face, List<GaiaVertex> vertices) {
        int[] idx = face.getIndices();
        if (idx == null || idx.length < 3) return 0.0;

        Vector3d p0 = vertices.get(idx[0]).getPosition();

        double area = 0.0;

        for (int i = 1; i < idx.length - 1; i++) {
            Vector3d p1 = vertices.get(idx[i]).getPosition();
            Vector3d p2 = vertices.get(idx[i + 1]).getPosition();

            Vector3d a = new Vector3d(p1).sub(p0);
            Vector3d b = new Vector3d(p2).sub(p0);

            area += a.cross(b).length() * 0.5;
        }

        return area;
    }

    private static Vector3d calculateFaceCenter(GaiaFace face, List<GaiaVertex> vertices) {
        Vector3d center = new Vector3d();

        int[] idx = face.getIndices();
        if (idx == null || idx.length == 0) return center;

        for (int i : idx) {
            center.add(vertices.get(i).getPosition());
        }

        center.div(idx.length);
        return center;
    }

    private static Map<Vector3i, CellStats> buildCellStats(
            GaiaPrimitive primitive,
            CellGrid3D cellGrid
    ) {
        List<GaiaVertex> vertices = primitive.getVertices();
        Map<Vector3i, CellStats> cellStatsMap = new HashMap<>();

        for (GaiaSurface surface : primitive.getSurfaces()) {
            for (GaiaFace face : surface.getFaces()) {
                int[] idx = face.getIndices();
                if (idx == null || idx.length < 3) continue;

                Vector3d center = calculateFaceCenter(face, vertices);
                Vector3i cell = cellGrid.getCellIndex(center);

                // Importante hacer copia por si getCellIndex reutiliza objeto.
                Vector3i cellKey = new Vector3i(cell);

                CellStats stats = cellStatsMap.computeIfAbsent(
                        cellKey,
                        k -> new CellStats(cellKey)
                );

                stats.triangleCount++;
                stats.areaSum += calculateFaceArea(face, vertices);

                for (int vi : idx) {
                    stats.addPoint(vertices.get(vi).getPosition());
                }
            }
        }

        return cellStatsMap;
    }

    private static void cleanBadRemeshableCells(
            Map<Vector3i, CellStats> cellStatsMap,
            double cellSize
    ) {
        for (CellStats stats : cellStatsMap.values()) {
            if (!stats.remeshable) continue;

            double sx = stats.sizeX();
            double sy = stats.sizeY();
            double sz = stats.sizeZ();

            double[] dims = new double[]{sx, sy, sz};
            Arrays.sort(dims);

            double d1 = dims[1];
            double d2 = dims[2];

            // No remeshear celdas con poquísima geometría propia.
            if (stats.triangleCount < 3) {
                stats.remeshable = false;
                continue;
            }

            // Evita ruido / microfragmentos.
            if (stats.areaSum < cellSize * cellSize * 0.01) {
                stats.remeshable = false;
                continue;
            }

            // Debe tener al menos una extensión razonable.
            if (d2 < cellSize * 0.25) {
                stats.remeshable = false;
                continue;
            }

            // Para superficie útil, la dimensión media no debería ser demasiado ridícula.
            if (d1 < cellSize * 0.08) {
                stats.remeshable = false;
            }
        }
    }

    private static void classifyRemeshableCells(
            Map<Vector3i, CellStats> cellStatsMap,
            double cellSize
    ) {
        int minTriangles = 5;
        double minArea = cellSize * cellSize * 0.03;

        for (CellStats stats : cellStatsMap.values()) {
            stats.remeshable = false;

            double sx = stats.sizeX();
            double sy = stats.sizeY();
            double sz = stats.sizeZ();

            double[] dims = new double[] { sx, sy, sz };
            Arrays.sort(dims);

            double d1 = dims[1];
            double d2 = dims[2];

            if (stats.triangleCount < minTriangles) continue;
            if (stats.areaSum < minArea) continue;
            if (d2 < cellSize * 0.25) continue;

            boolean lineLike =
                    d1 < cellSize * 0.10 &&
                            d2 > cellSize * 0.45;

            if (lineLike) continue;

            boolean surfaceLike =
                    d1 > cellSize * 0.10 &&
                            d2 > cellSize * 0.30;

            if (!surfaceLike) continue;

            double elongation = d2 / Math.max(d1, 1e-9);
            if (elongation > 18.0) continue;

            stats.remeshable = true;
        }
    }

    private static void dilateRemeshableCells(
            Map<Vector3i, CellStats> cellStatsMap,
            int iterations
    ) {
        for (int it = 0; it < iterations; it++) {

            Set<Vector3i> toEnable = new HashSet<>();

            for (Map.Entry<Vector3i, CellStats> entry : cellStatsMap.entrySet()) {
                Vector3i cell = entry.getKey();
                CellStats stats = entry.getValue();

                if (!stats.remeshable) continue;

                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = -1; dy <= 1; dy++) {
                        for (int dz = -1; dz <= 1; dz++) {

                            if (dx == 0 && dy == 0 && dz == 0) continue;

                            Vector3i nb = new Vector3i(
                                    cell.x + dx,
                                    cell.y + dy,
                                    cell.z + dz
                            );

                            if (cellStatsMap.containsKey(nb)) {
                                toEnable.add(nb);
                            }
                        }
                    }
                }
            }

            for (Vector3i cell : toEnable) {
                CellStats stats = cellStatsMap.get(cell);
                if (stats != null) {
                    stats.remeshable = true;
                }
            }
        }
    }

    private static boolean isFaceRemeshable(
            GaiaFace face,
            List<GaiaVertex> vertices,
            CellGrid3D cellGrid,
            Map<Vector3i, CellStats> cellStatsMap
    ) {
        Vector3d center = calculateFaceCenter(face, vertices);
        Vector3i centerCell = new Vector3i(cellGrid.getCellIndex(center));

        CellStats centerStats = cellStatsMap.get(centerCell);
        if (centerStats == null || !centerStats.remeshable) {
            return false;
        }

        int[] idx = face.getIndices();

        for (int vi : idx) {
            Vector3d p = vertices.get(vi).getPosition();
            Vector3i cell = new Vector3i(cellGrid.getCellIndex(p));

            CellStats stats = cellStatsMap.get(cell);
            if (stats == null || !stats.remeshable) {
                return false;
            }
        }

        return true;
    }

    private static class RemeshCluster {
        Vector3d sumPos = new Vector3d();
        Vector3d sumNormal = new Vector3d();

        int count = 0;
        int newIndex = -1;

        void addVertex(GaiaVertex v) {
            sumPos.add(v.getPosition());

            if (v.getNormal() != null) {
                sumNormal.add(v.getNormal());
            }

            count++;
        }

        GaiaVertex createVertex() {
            GaiaVertex newVertex = new GaiaVertex();

            Vector3d avgPos = new Vector3d(sumPos).div(count);
            Vector3d avgNormal = new Vector3d(sumNormal);

            if (avgNormal.lengthSquared() > 1e-12) {
                avgNormal.normalize();
            }

            newVertex.setPosition(avgPos);
            newVertex.setNormal(avgNormal);

            return newVertex;
        }
    }

    public static void reMeshSceneMoveVerticesByCellClassification(
            GaiaScene gaiaScene,
            ReMeshParameters params,
            Vector3i sceneMinCellIndex,
            Vector3i sceneMaxCellIndex
    ) {
        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(gaiaScene);

        if (primitives == null || primitives.isEmpty()) return;

        GaiaPrimitive primitive = primitives.get(0);

        List<GaiaVertex> vertices = primitive.getVertices();
        List<GaiaSurface> surfaces = primitive.getSurfaces();

        if (vertices == null || vertices.isEmpty()) return;

        CellGrid3D cellGrid = params.getCellGrid();
        double cellSize = cellGrid.getCellSize();

        // =========================================================
        // 1. Clasificar celdas
        // =========================================================
        Map<Vector3i, CellStats> cellStatsMap = buildCellStats(primitive, cellGrid);

        classifyRemeshableCells(cellStatsMap, cellSize);
        dilateRemeshableCells(cellStatsMap, 1);
        cleanBadRemeshableCells(cellStatsMap, cellSize);

        // =========================================================
        // 2. Decidir caras remesheables
        // =========================================================
        boolean[][] remeshFaceFlags = buildRemeshFaceFlags(
                surfaces,
                vertices,
                cellGrid,
                cellStatsMap
        );

        // =========================================================
// 3. Detectar vértices que pueden moverse
// =========================================================
        boolean[] boundaryProtectedVertex = buildBoundaryProtectedVertices(
                surfaces,
                remeshFaceFlags,
                vertices.size()
        );

        boolean[] movableVertex = new boolean[vertices.size()];

        for (int sIdx = 0; sIdx < surfaces.size(); sIdx++) {
            GaiaSurface surface = surfaces.get(sIdx);
            List<GaiaFace> faces = surface.getFaces();

            for (int fIdx = 0; fIdx < faces.size(); fIdx++) {

                // Solo miramos caras que realmente queremos remeshear
                if (!remeshFaceFlags[sIdx][fIdx]) {
                    continue;
                }

                GaiaFace face = faces.get(fIdx);
                int[] idx = face.getIndices();

                if (idx == null) {
                    continue;
                }

                for (int vi : idx) {
                    if (vi < 0 || vi >= movableVertex.length) {
                        continue;
                    }

                    // Si es vértice de frontera REMESH/KEEP, NO lo movemos
                    if (boundaryProtectedVertex[vi]) {
                        continue;
                    }

                    movableVertex[vi] = true;
                }
            }
        }

        // =========================================================
        // 4. Crear clusters por celda, usando solo vértices movibles
        // =========================================================
        Map<Vector3i, MoveCluster> cellToCluster = new HashMap<>();

        for (int vi = 0; vi < vertices.size(); vi++) {
            if (!movableVertex[vi]) continue;

            GaiaVertex vertex = vertices.get(vi);
            if (vertex == null || vertex.getPosition() == null) continue;

            Vector3i cell = new Vector3i(
                    cellGrid.getCellIndex(vertex.getPosition())
            );

            CellStats stats = cellStatsMap.get(cell);
            if (stats == null || !stats.remeshable) {
                continue;
            }

            MoveCluster cluster = cellToCluster.computeIfAbsent(
                    cell,
                    k -> new MoveCluster()
            );

            cluster.addVertex(vertex);
        }

        // =========================================================
        // 5. Mover vértices originales a la media de su celda
        // =========================================================
        for (int vi = 0; vi < vertices.size(); vi++) {
            if (!movableVertex[vi]) continue;

            GaiaVertex vertex = vertices.get(vi);
            if (vertex == null || vertex.getPosition() == null) continue;

            Vector3i cell = new Vector3i(
                    cellGrid.getCellIndex(vertex.getPosition())
            );

            MoveCluster cluster = cellToCluster.get(cell);
            if (cluster == null || cluster.count == 0) {
                continue;
            }

            Vector3d avgPosition = cluster.getAveragePosition();

            // IMPORTANTE: asignar copia
            vertex.setPosition(new Vector3d(avgPosition));
        }

        // =========================================================
        // 6. Actualizar min/max cells si lo necesitas
        // =========================================================
        if (sceneMinCellIndex != null && sceneMaxCellIndex != null) {
            updateSceneMinMaxCells(vertices, cellGrid, sceneMinCellIndex, sceneMaxCellIndex);

            sceneMinCellIndex.x += 1;
            sceneMinCellIndex.y += 1;
            sceneMinCellIndex.z += 1;

            sceneMaxCellIndex.x -= 1;
            sceneMaxCellIndex.y -= 1;
            sceneMaxCellIndex.z -= 1;

            params.deleteCellAveragePositionInsideBox(sceneMinCellIndex, sceneMaxCellIndex);
        }

        // =========================================================
        // 7. Después de esta función, en tu pipeline:
        //    weldVertices()
        //    deleteDegeneratedFaces()
        //    delete normals
        // =========================================================
        GaiaNormalCleaner normalCleaner = new GaiaNormalCleaner();
        normalCleaner.apply(gaiaScene);
    }

    private static class MoveCluster {
        Vector3d sumPos = new Vector3d();
        int count = 0;

        void addVertex(GaiaVertex v) {
            if (v == null || v.getPosition() == null) return;

            sumPos.add(v.getPosition());
            count++;
        }

        Vector3d getAveragePosition() {
            if (count == 0) {
                return new Vector3d();
            }

            return new Vector3d(sumPos).div(count);
        }
    }

    private static boolean[][] buildRemeshFaceFlags(
            List<GaiaSurface> surfaces,
            List<GaiaVertex> oldVertices,
            CellGrid3D cellGrid,
            Map<Vector3i, CellStats> cellStatsMap
    ) {
        boolean[][] flags = new boolean[surfaces.size()][];

        for (int sIdx = 0; sIdx < surfaces.size(); sIdx++) {
            GaiaSurface surface = surfaces.get(sIdx);
            List<GaiaFace> faces = surface.getFaces();

            flags[sIdx] = new boolean[faces.size()];

            for (int fIdx = 0; fIdx < faces.size(); fIdx++) {
                GaiaFace face = faces.get(fIdx);

                flags[sIdx][fIdx] = isFaceRemeshable(
                        face,
                        oldVertices,
                        cellGrid,
                        cellStatsMap
                );
            }
        }

        return flags;
    }

    private static boolean[] buildBoundaryProtectedVertices(
            List<GaiaSurface> surfaces,
            boolean[][] remeshFaceFlags,
            int vertexCount
    ) {
        boolean[] protectedVertex = new boolean[vertexCount];

        Map<EdgeKey, Boolean> edgeTouchesRemesh = new HashMap<>();
        Map<EdgeKey, Boolean> edgeTouchesKeep = new HashMap<>();

        for (int sIdx = 0; sIdx < surfaces.size(); sIdx++) {
            GaiaSurface surface = surfaces.get(sIdx);
            List<GaiaFace> faces = surface.getFaces();

            for (int fIdx = 0; fIdx < faces.size(); fIdx++) {
                GaiaFace face = faces.get(fIdx);
                int[] idx = face.getIndices();
                if (idx == null || idx.length < 3) continue;

                boolean remesh = remeshFaceFlags[sIdx][fIdx];

                for (int i = 0; i < idx.length; i++) {
                    int a = idx[i];
                    int b = idx[(i + 1) % idx.length];

                    EdgeKey key = new EdgeKey(a, b);

                    if (remesh) {
                        edgeTouchesRemesh.put(key, true);
                    } else {
                        edgeTouchesKeep.put(key, true);
                    }
                }
            }
        }

        // Edge frontera = tocado por REMESH y por KEEP.
        for (EdgeKey edge : edgeTouchesRemesh.keySet()) {
            if (edgeTouchesKeep.containsKey(edge)) {
                protectedVertex[edge.a] = true;
                protectedVertex[edge.b] = true;
            }
        }

        return protectedVertex;
    }

    private static boolean isDegenerated(int[] idx) {
        if (idx == null || idx.length < 3) return true;

        for (int i = 0; i < idx.length; i++) {
            for (int j = i + 1; j < idx.length; j++) {
                if (idx[i] == idx[j]) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void updateSceneMinMaxCells(
            List<GaiaVertex> vertices,
            CellGrid3D cellGrid,
            Vector3i sceneMinCellIndex,
            Vector3i sceneMaxCellIndex
    ) {
        boolean first = true;

        for (GaiaVertex vertex : vertices) {
            if (vertex == null || vertex.getPosition() == null) continue;

            Vector3i cell = cellGrid.getCellIndex(vertex.getPosition());

            if (first) {
                sceneMinCellIndex.set(cell);
                sceneMaxCellIndex.set(cell);
                first = false;
            } else {
                if (cell.x < sceneMinCellIndex.x) sceneMinCellIndex.x = cell.x;
                if (cell.y < sceneMinCellIndex.y) sceneMinCellIndex.y = cell.y;
                if (cell.z < sceneMinCellIndex.z) sceneMinCellIndex.z = cell.z;

                if (cell.x > sceneMaxCellIndex.x) sceneMaxCellIndex.x = cell.x;
                if (cell.y > sceneMaxCellIndex.y) sceneMaxCellIndex.y = cell.y;
                if (cell.z > sceneMaxCellIndex.z) sceneMaxCellIndex.z = cell.z;
            }
        }
    }
}
