package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.model.*;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.*;

public class TextureAwareReMesher {
    private static class EdgeKey {
        final int a;
        final int b;

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
            int h = 17;
            h = 31 * h + a;
            h = 31 * h + b;
            return h;
        }
    }
    private static class CellClusterKey {
        final Vector3i cell;

        CellClusterKey(Vector3i cell) {
            this.cell = new Vector3i(cell);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof CellClusterKey)) return false;

            CellClusterKey other = (CellClusterKey) obj;

            return cell.x == other.cell.x
                    && cell.y == other.cell.y
                    && cell.z == other.cell.z;
        }

        @Override
        public int hashCode() {
            int h = 17;
            h = 31 * h + cell.x;
            h = 31 * h + cell.y;
            h = 31 * h + cell.z;
            return h;
        }
    }

    private static class CellStats {
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

    private static class MoveCluster {
        Vector3d sumPos = new Vector3d();
        int count = 0;

        List<Integer> positionGroupIds = new ArrayList<>();

        void addPositionGroup(int groupId, PositionGroup group) {
            if (group == null || group.position == null) return;

            sumPos.add(group.position);
            positionGroupIds.add(groupId);
            count++;
        }

        Vector3d getAveragePosition() {
            if (count == 0) {
                return new Vector3d();
            }

            return new Vector3d(sumPos).div(count);
        }
    }

    private static void classifyTextureIslands(List<GaiaSurface> surfaces) {
        int globalIslandId = 0;

        for (GaiaSurface surface : surfaces) {
            globalIslandId = classifyTextureIslands(surface, globalIslandId);
        }
    }

    private static int classifyTextureIslands(
            GaiaSurface surface,
            int startIslandId
    ) {
        List<GaiaFace> faces = surface.getFaces();
        if (faces == null || faces.isEmpty()) {
            return startIslandId;
        }

        Map<EdgeKey, List<Integer>> edgeToFaces = new HashMap<>();

        for (int fIdx = 0; fIdx < faces.size(); fIdx++) {
            GaiaFace face = faces.get(fIdx);
            int[] idx = face.getIndices();

            if (idx == null || idx.length < 3) continue;

            for (int i = 0; i < idx.length; i++) {
                int a = idx[i];
                int b = idx[(i + 1) % idx.length];

                EdgeKey key = new EdgeKey(a, b);

                edgeToFaces
                        .computeIfAbsent(key, k -> new ArrayList<>())
                        .add(fIdx);
            }
        }

        List<List<Integer>> neighbors = new ArrayList<>();
        for (int i = 0; i < faces.size(); i++) {
            neighbors.add(new ArrayList<>());
        }

        for (List<Integer> linkedFaces : edgeToFaces.values()) {
            if (linkedFaces.size() < 2) continue;

            for (int i = 0; i < linkedFaces.size(); i++) {
                for (int j = i + 1; j < linkedFaces.size(); j++) {
                    int a = linkedFaces.get(i);
                    int b = linkedFaces.get(j);

                    neighbors.get(a).add(b);
                    neighbors.get(b).add(a);
                }
            }
        }

        boolean[] visited = new boolean[faces.size()];
        int islandId = startIslandId;

        for (int i = 0; i < faces.size(); i++) {
            if (visited[i]) continue;

            Queue<Integer> q = new ArrayDeque<>();
            q.add(i);
            visited[i] = true;

            while (!q.isEmpty()) {
                int f = q.poll();

                faces.get(f).setClassifyId(islandId);

                for (int nb : neighbors.get(f)) {
                    if (!visited[nb]) {
                        visited[nb] = true;
                        q.add(nb);
                    }
                }
            }

            islandId++;
        }

        return islandId;
    }

    private static boolean[] buildMultiIslandVertexFlags(
            List<GaiaSurface> surfaces,
            int vertexCount
    ) {
        @SuppressWarnings("unchecked")
        Set<Integer>[] vertexIslands = new HashSet[vertexCount];

        for (GaiaSurface surface : surfaces) {
            for (GaiaFace face : surface.getFaces()) {
                int islandId = face.getClassifyId();
                int[] idx = face.getIndices();

                if (idx == null) continue;

                for (int vi : idx) {
                    if (vi < 0 || vi >= vertexCount) continue;

                    if (vertexIslands[vi] == null) {
                        vertexIslands[vi] = new HashSet<>();
                    }

                    vertexIslands[vi].add(islandId);
                }
            }
        }

        boolean[] result = new boolean[vertexCount];

        for (int i = 0; i < vertexCount; i++) {
            result[i] = vertexIslands[i] != null && vertexIslands[i].size() > 1;
        }

        return result;
    }

    private static int[] buildVertexDominantIslandId(
            List<GaiaSurface> surfaces,
            int vertexCount
    ) {
        Map<Integer, Map<Integer, Integer>> vertexIslandCounts = new HashMap<>();

        for (GaiaSurface surface : surfaces) {
            for (GaiaFace face : surface.getFaces()) {
                int islandId = face.getClassifyId();
                int[] idx = face.getIndices();

                if (idx == null) continue;

                for (int vi : idx) {
                    if (vi < 0 || vi >= vertexCount) continue;

                    Map<Integer, Integer> counts =
                            vertexIslandCounts.computeIfAbsent(vi, k -> new HashMap<>());

                    counts.put(islandId, counts.getOrDefault(islandId, 0) + 1);
                }
            }
        }

        int[] dominant = new int[vertexCount];
        Arrays.fill(dominant, -1);

        for (Map.Entry<Integer, Map<Integer, Integer>> entry : vertexIslandCounts.entrySet()) {
            int vi = entry.getKey();

            int bestIsland = -1;
            int bestCount = -1;

            for (Map.Entry<Integer, Integer> c : entry.getValue().entrySet()) {
                if (c.getValue() > bestCount) {
                    bestCount = c.getValue();
                    bestIsland = c.getKey();
                }
            }

            dominant[vi] = bestIsland;
        }

        return dominant;
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



    public void reMeshScene(
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
        // 0. Guardar posiciones y texCoords originales
        // =========================================================
        List<Vector3d> originalPositions = new ArrayList<>(vertices.size());
        List<Vector2d> originalTexcoords = new ArrayList<>(vertices.size());

        for (GaiaVertex vertex : vertices) {
            if (vertex != null && vertex.getPosition() != null) {
                originalPositions.add(new Vector3d(vertex.getPosition()));
            } else {
                originalPositions.add(null);
            }

            if (vertex != null && vertex.getTexcoords() != null) {
                originalTexcoords.add(new Vector2d(vertex.getTexcoords()));
            } else {
                originalTexcoords.add(null);
            }
        }

        // =========================================================
        // 1. Clasificar celdas remesheables
        // =========================================================
        Map<Vector3i, CellStats> cellStatsMap = buildCellStats(primitive, cellGrid);

        classifyRemeshableCells(cellStatsMap, cellSize);
        dilateRemeshableCells(cellStatsMap, 1);
        cleanBadRemeshableCells(cellStatsMap, cellSize);

        // =========================================================
        // 2. Clasificar caras remesheables
        // =========================================================
        boolean[][] remeshFaceFlags = buildRemeshFaceFlags(
                surfaces,
                vertices,
                cellGrid,
                cellStatsMap
        );

        // =========================================================
        // 3. Clasificar islas de textura / islas topológicas
        // =========================================================
        classifyTextureIslands(surfaces);

        // =========================================================
        // 4. Calcular isla dominante de cada vértice
        // =========================================================
        int[] vertexIslandId = buildVertexDominantIslandId(
                surfaces,
                vertices.size()
        );

        // =========================================================
        // 5. Agrupar vértices por posición geométrica
        //
        // Importante:
        // Vértices con misma posición pero distinta texCoord deben moverse juntos,
        // pero NO soldarse aquí.
        // =========================================================
        double positionEpsilon = cellSize * 1e-6;

        int[] vertexToPositionGroup = new int[vertices.size()];
        Arrays.fill(vertexToPositionGroup, -1);

        List<PositionGroup> positionGroups = buildPositionGroups(
                vertices,
                positionEpsilon,
                vertexIslandId,
                vertexToPositionGroup
        );

        // =========================================================
        // 6. Detectar grupos de posición que pueden moverse
        // =========================================================
        boolean[] movableGroup = new boolean[positionGroups.size()];

        for (int sIdx = 0; sIdx < surfaces.size(); sIdx++) {
            GaiaSurface surface = surfaces.get(sIdx);
            List<GaiaFace> faces = surface.getFaces();

            for (int fIdx = 0; fIdx < faces.size(); fIdx++) {
                if (!remeshFaceFlags[sIdx][fIdx]) {
                    continue;
                }

                GaiaFace face = faces.get(fIdx);
                int[] idx = face.getIndices();

                if (idx == null) continue;

                for (int vi : idx) {
                    if (vi < 0 || vi >= vertexToPositionGroup.length) {
                        continue;
                    }

                    int groupId = vertexToPositionGroup[vi];
                    if (groupId < 0) continue;

                    movableGroup[groupId] = true;
                    positionGroups.get(groupId).movable = true;
                }
            }
        }

        // =========================================================
        // 7. Crear clusters por celda, usando PositionGroups
        //
        // OJO:
        // Aquí NO usamos islandId en la key.
        // Si usas islandId, dos vértices en la misma posición pero con
        // texCoords/islas distintas podrían moverse diferente y crear grietas.
        // =========================================================
        Map<CellClusterKey, MoveCluster> clusterMap = new HashMap<>();

        for (int groupId = 0; groupId < positionGroups.size(); groupId++) {
            if (!movableGroup[groupId]) continue;

            PositionGroup group = positionGroups.get(groupId);

            Vector3i cell = new Vector3i(
                    cellGrid.getCellIndex(group.position)
            );

            CellStats stats = cellStatsMap.get(cell);
            if (stats == null || !stats.remeshable) {
                continue;
            }

            CellClusterKey key = new CellClusterKey(cell);

            MoveCluster cluster = clusterMap.computeIfAbsent(
                    key,
                    k -> new MoveCluster()
            );

            cluster.addPositionGroup(groupId, group);
        }

        // =========================================================
        // 8. Debug de clusters
        // =========================================================
        int singletonClusters = 0;
        int usefulClusters = 0;
        int positionGroupsInUsefulClusters = 0;

        for (MoveCluster cluster : clusterMap.values()) {
            if (cluster.count < 2) {
                singletonClusters++;
            } else {
                usefulClusters++;
                positionGroupsInUsefulClusters += cluster.count;
            }
        }

        int totalPositionGroups = positionGroups.size();
        int multiVertexGroups = 0;
        int movableGroups = 0;

        for (int i = 0; i < positionGroups.size(); i++) {
            PositionGroup group = positionGroups.get(i);

            if (group.vertexIndices.size() > 1) {
                multiVertexGroups++;
            }

            if (movableGroup[i]) {
                movableGroups++;
            }
        }

        System.out.println("positionGroups = " + totalPositionGroups);
        System.out.println("multiVertexPositionGroups = " + multiVertexGroups);
        System.out.println("movableGroups = " + movableGroups);
        System.out.println("clusterMap size = " + clusterMap.size());
        System.out.println("singletonClusters = " + singletonClusters);
        System.out.println("usefulClusters = " + usefulClusters);
        System.out.println("positionGroupsInUsefulClusters = " + positionGroupsInUsefulClusters);

        // =========================================================
        // 9. Mover grupos completos y marcar vértices movidos
        // =========================================================
        boolean[] movedVertex = new boolean[vertices.size()];

        for (MoveCluster cluster : clusterMap.values()) {
            if (cluster.count < 2) {
                continue;
            }

            Vector3d avgPosition = cluster.getAveragePosition();

            for (int groupId : cluster.positionGroupIds) {
                PositionGroup group = positionGroups.get(groupId);

                for (int vi : group.vertexIndices) {
                    if (vi < 0 || vi >= vertices.size()) continue;

                    GaiaVertex vertex = vertices.get(vi);
                    if (vertex == null) continue;

                    vertex.setPosition(new Vector3d(avgPosition));
                    movedVertex[vi] = true;
                }

                group.position.set(avgPosition);
            }
        }

        // =========================================================
        // 10. Recalcular texCoords de los vértices movidos
        //
        // Esto evita que la textura antigua quede estirada de forma absurda
        // después de mover la geometría.
        // =========================================================
        recalculateMovedVertexTexcoords(
                surfaces,
                vertices,
                originalPositions,
                originalTexcoords,
                movedVertex
        );

        // =========================================================
        // 11. Actualizar min/max cells
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
        // 12. Pipeline posterior recomendado fuera de aquí:
        //
        // weldVertices comparando texCoords
        // deleteDegeneratedFaces
        // deleteNoUsedVertices
        // limpiar normales
        // =========================================================
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

    private static void recalculateMovedVertexTexcoords(
            List<GaiaSurface> surfaces,
            List<GaiaVertex> vertices,
            List<Vector3d> originalPositions,
            List<Vector2d> originalTexcoords,
            boolean[] movedVertex
    ) {
        // vertex -> incident faces
        Map<Integer, List<GaiaFace>> vertexToFaces = new HashMap<>();

        for (GaiaSurface surface : surfaces) {
            for (GaiaFace face : surface.getFaces()) {
                int[] idx = face.getIndices();
                if (idx == null || idx.length < 3) continue;

                for (int vi : idx) {
                    if (vi < 0 || vi >= vertices.size()) continue;

                    vertexToFaces
                            .computeIfAbsent(vi, k -> new ArrayList<>())
                            .add(face);
                }
            }
        }

        for (int vi = 0; vi < vertices.size(); vi++) {
            if (!movedVertex[vi]) continue;

            GaiaVertex vertex = vertices.get(vi);
            if (vertex == null || vertex.getPosition() == null) continue;

            List<GaiaFace> faces = vertexToFaces.get(vi);
            if (faces == null || faces.isEmpty()) continue;

            Vector3d newPos = vertex.getPosition();

            Vector2d bestUv = null;
            double bestScore = Double.POSITIVE_INFINITY;

            for (GaiaFace face : faces) {
                int[] idx = face.getIndices();
                if (idx == null || idx.length != 3) continue;

                int i0 = idx[0];
                int i1 = idx[1];
                int i2 = idx[2];

                Vector2d uv0 = originalTexcoords.get(i0);
                Vector2d uv1 = originalTexcoords.get(i1);
                Vector2d uv2 = originalTexcoords.get(i2);

                if (uv0 == null || uv1 == null || uv2 == null) {
                    continue;
                }

                Vector3d p0 = originalPositions.get(i0);
                Vector3d p1 = originalPositions.get(i1);
                Vector3d p2 = originalPositions.get(i2);

                BarycentricResult result = closestBarycentricOnTriangle(
                        newPos,
                        p0,
                        p1,
                        p2
                );

                if (result == null) continue;

                Vector2d uv = interpolateUv(
                        uv0,
                        uv1,
                        uv2,
                        result.u,
                        result.v,
                        result.w
                );

                if (result.distanceSquared < bestScore) {
                    bestScore = result.distanceSquared;
                    bestUv = uv;
                }
            }

            if (bestUv != null) {
                vertex.setTexcoords(bestUv);
            }
        }
    }

    private static class BarycentricResult {
        double u;
        double v;
        double w;
        double distanceSquared;
    }

    private static BarycentricResult closestBarycentricOnTriangle(
            Vector3d p,
            Vector3d a,
            Vector3d b,
            Vector3d c
    ) {
        Vector3d ab = new Vector3d(b).sub(a);
        Vector3d ac = new Vector3d(c).sub(a);
        Vector3d ap = new Vector3d(p).sub(a);

        double d00 = ab.dot(ab);
        double d01 = ab.dot(ac);
        double d11 = ac.dot(ac);
        double d20 = ap.dot(ab);
        double d21 = ap.dot(ac);

        double denom = d00 * d11 - d01 * d01;

        if (Math.abs(denom) < 1e-20) {
            return null;
        }

        double v = (d11 * d20 - d01 * d21) / denom;
        double w = (d00 * d21 - d01 * d20) / denom;
        double u = 1.0 - v - w;

        // Clamping simple para que si cae un poco fuera del triángulo,
        // use el punto más cercano aproximado.
        if (u < 0.0) u = 0.0;
        if (v < 0.0) v = 0.0;
        if (w < 0.0) w = 0.0;

        double sum = u + v + w;
        if (sum < 1e-20) {
            return null;
        }

        u /= sum;
        v /= sum;
        w /= sum;

        Vector3d closest = new Vector3d(a).mul(u)
                .add(new Vector3d(b).mul(v))
                .add(new Vector3d(c).mul(w));

        BarycentricResult result = new BarycentricResult();
        result.u = u;
        result.v = v;
        result.w = w;
        result.distanceSquared = closest.distanceSquared(p);

        return result;
    }

    private static Vector2d interpolateUv(
            Vector2d uv0,
            Vector2d uv1,
            Vector2d uv2,
            double u,
            double v,
            double w
    ) {
        return new Vector2d(uv0).mul(u)
                .add(new Vector2d(uv1).mul(v))
                .add(new Vector2d(uv2).mul(w));
    }

    private static class UvKey {
        final int u;
        final int v;

        UvKey(Vector2d texcoord, double uvEpsilon) {
            this.u = (int) Math.round(texcoord.x / uvEpsilon);
            this.v = (int) Math.round(texcoord.y / uvEpsilon);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof UvKey)) return false;
            UvKey other = (UvKey) obj;
            return u == other.u && v == other.v;
        }

        @Override
        public int hashCode() {
            int h = 17;
            h = 31 * h + u;
            h = 31 * h + v;
            return h;
        }
    }

    private static class PositionKey {
        final long x;
        final long y;
        final long z;

        PositionKey(Vector3d p, double epsilon) {
            this.x = Math.round(p.x / epsilon);
            this.y = Math.round(p.y / epsilon);
            this.z = Math.round(p.z / epsilon);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof PositionKey)) return false;
            PositionKey other = (PositionKey) obj;
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            int h = 17;
            h = 31 * h + Long.hashCode(x);
            h = 31 * h + Long.hashCode(y);
            h = 31 * h + Long.hashCode(z);
            return h;
        }
    }

    private static class PositionGroup {
        final List<Integer> vertexIndices = new ArrayList<>();
        final Vector3d position = new Vector3d();

        int dominantIslandId = -1;
        boolean movable = false;

        void addVertex(int vertexIndex, GaiaVertex vertex) {
            vertexIndices.add(vertexIndex);

            if (vertexIndices.size() == 1) {
                position.set(vertex.getPosition());
            }
        }
    }

    private static List<PositionGroup> buildPositionGroups(
            List<GaiaVertex> vertices,
            double epsilon,
            int[] vertexIslandId,
            int[] vertexToPositionGroup
    ) {
        Map<PositionKey, PositionGroup> map = new HashMap<>();

        for (int i = 0; i < vertices.size(); i++) {
            GaiaVertex vertex = vertices.get(i);
            if (vertex == null || vertex.getPosition() == null) continue;

            PositionKey key = new PositionKey(vertex.getPosition(), epsilon);

            PositionGroup group = map.computeIfAbsent(key, k -> new PositionGroup());
            group.addVertex(i, vertex);
        }

        List<PositionGroup> groups = new ArrayList<>(map.values());

        for (int groupId = 0; groupId < groups.size(); groupId++) {
            PositionGroup group = groups.get(groupId);

            Map<Integer, Integer> islandCounts = new HashMap<>();

            for (int vi : group.vertexIndices) {
                vertexToPositionGroup[vi] = groupId;

                int islandId = vertexIslandId[vi];
                if (islandId >= 0) {
                    islandCounts.put(islandId, islandCounts.getOrDefault(islandId, 0) + 1);
                }
            }

            int bestIsland = -1;
            int bestCount = -1;

            for (Map.Entry<Integer, Integer> e : islandCounts.entrySet()) {
                if (e.getValue() > bestCount) {
                    bestCount = e.getValue();
                    bestIsland = e.getKey();
                }
            }

            group.dominantIslandId = bestIsland;
        }

        return groups;
    }

}
