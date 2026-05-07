package com.gaia3d.basic.remesher;

import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaVertex;
import org.joml.Vector3d;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GaiaFrontierFinder {
    private static int[] collectUsedVertexIndices(List<GaiaFace> faces, int vertexCount) {
        boolean[] used = new boolean[vertexCount];
        int usedCount = 0;

        for (GaiaFace face : faces) {
            if (face == null || face.getIndices() == null) continue;

            int[] indices = face.getIndices();
            for (int index : indices) {
                if (index < 0 || index >= vertexCount) continue;

                if (!used[index]) {
                    used[index] = true;
                    usedCount++;
                }
            }
        }

        int[] result = new int[usedCount];
        int pos = 0;

        for (int i = 0; i < vertexCount; i++) {
            if (used[i]) {
                result[pos++] = i;
            }
        }

        return result;
    }

    private static long qx(GaiaVertex vertex, double invTolerance) {
        return Math.round(vertex.getPosition().x * invTolerance);
    }

    private static long qy(GaiaVertex vertex, double invTolerance) {
        return Math.round(vertex.getPosition().y * invTolerance);
    }

    private static long qz(GaiaVertex vertex, double invTolerance) {
        return Math.round(vertex.getPosition().z * invTolerance);
    }

    private static int estimateEdgeCapacity(List<GaiaFace> faces) {
        if (faces == null || faces.isEmpty()) {
            return 16;
        }

        int triangleCount = 0;

        for (GaiaFace face : faces) {
            if (face == null || face.getIndices() == null) {
                continue;
            }

            triangleCount += face.getIndices().length / 3;
        }

        int estimatedEdges = triangleCount * 3;

        // HashMap trabaja mejor si no va demasiado lleno.
        return Math.max(16, (int) (estimatedEdges / 0.75f) + 1);
    }

    public static int[] buildWeldedVertexIndices_LowMemory(
            List<GaiaVertex> vertices,
            List<GaiaFace> faces,
            double tolerance,
            int[] weldedIndices
    ) {
        int vertexCount = vertices.size();

        //int[] weldedIndices = new int[vertexCount];

        // -1 significa "no usado por estas faces"
        for (int i = 0; i < vertexCount; i++) {
            weldedIndices[i] = -1;
        }

        if (faces == null || faces.isEmpty()) {
            return weldedIndices;
        }

        int[] usedIndices = collectUsedVertexIndices(faces, vertexCount);

        if (usedIndices.length == 0) {
            return weldedIndices;
        }

        double invTolerance = 1.0 / tolerance;

        quickSortByQuantizedPosition(usedIndices, 0, usedIndices.length - 1, vertices, invTolerance);

        int nextWeldedIndex = 0;

        int firstOriginalIndex = usedIndices[0];
        weldedIndices[firstOriginalIndex] = nextWeldedIndex;

        long prevX = qx(vertices.get(firstOriginalIndex), invTolerance);
        long prevY = qy(vertices.get(firstOriginalIndex), invTolerance);
        long prevZ = qz(vertices.get(firstOriginalIndex), invTolerance);

        for (int i = 1; i < usedIndices.length; i++) {
            int originalIndex = usedIndices[i];

            long x = qx(vertices.get(originalIndex), invTolerance);
            long y = qy(vertices.get(originalIndex), invTolerance);
            long z = qz(vertices.get(originalIndex), invTolerance);

            if (x != prevX || y != prevY || z != prevZ) {
                nextWeldedIndex++;
                prevX = x;
                prevY = y;
                prevZ = z;
            }

            weldedIndices[originalIndex] = nextWeldedIndex;
        }

        return weldedIndices;
    }

    private static int compareQuantized(
            int indexA,
            int indexB,
            List<GaiaVertex> vertices,
            double invTolerance
    ) {
        GaiaVertex va = vertices.get(indexA);
        GaiaVertex vb = vertices.get(indexB);

        long ax = qx(va, invTolerance);
        long bx = qx(vb, invTolerance);
        if (ax != bx) return Long.compare(ax, bx);

        long ay = qy(va, invTolerance);
        long by = qy(vb, invTolerance);
        if (ay != by) return Long.compare(ay, by);

        long az = qz(va, invTolerance);
        long bz = qz(vb, invTolerance);
        return Long.compare(az, bz);
    }

    private static void quickSortByQuantizedPosition(
            int[] array,
            int left,
            int right,
            List<GaiaVertex> vertices,
            double invTolerance
    ) {
        int i = left;
        int j = right;
        int pivot = array[left + (right - left) / 2];

        while (i <= j) {
            while (compareQuantized(array[i], pivot, vertices, invTolerance) < 0) {
                i++;
            }

            while (compareQuantized(array[j], pivot, vertices, invTolerance) > 0) {
                j--;
            }

            if (i <= j) {
                int tmp = array[i];
                array[i] = array[j];
                array[j] = tmp;
                i++;
                j--;
            }
        }

        if (left < j) {
            quickSortByQuantizedPosition(array, left, j, vertices, invTolerance);
        }

        if (i < right) {
            quickSortByQuantizedPosition(array, i, right, vertices, invTolerance);
        }
    }

    public static Map<Long, Integer> buildGeometricEdgeCountMap(
            List<GaiaFace> faces,
            int[] weldedIndices
    ) {
        //int estimatedEdges = faces == null ? 16 : Math.max(16, faces.size() * 3);
        Map<Long, Integer> edgeCountMap = new HashMap<>(estimateEdgeCapacity(faces));

        if (faces == null || faces.isEmpty() || weldedIndices == null) {
            return edgeCountMap;
        }

        for (GaiaFace face : faces) {
            if (face == null || face.getIndices() == null) {
                continue;
            }

            int[] indices = face.getIndices();
            int indicesCount = indices.length;

            if (indicesCount < 3) {
                continue;
            }

            int triangleCount = indicesCount / 3;

            for (int t = 0; t < triangleCount; t++) {
                int idx0 = indices[t * 3];
                int idx1 = indices[t * 3 + 1];
                int idx2 = indices[t * 3 + 2];

                if (!isValidOriginalIndex(idx0, weldedIndices.length)
                        || !isValidOriginalIndex(idx1, weldedIndices.length)
                        || !isValidOriginalIndex(idx2, weldedIndices.length)) {
                    continue;
                }

                int i0 = weldedIndices[idx0];
                int i1 = weldedIndices[idx1];
                int i2 = weldedIndices[idx2];

                if (i0 < 0 || i1 < 0 || i2 < 0) {
                    continue;
                }

                addEdge(edgeCountMap, i0, i1);
                addEdge(edgeCountMap, i1, i2);
                addEdge(edgeCountMap, i2, i0);
            }
        }

        return edgeCountMap;
    }

    private static boolean isValidOriginalIndex(int index, int vertexCount) {
        return index >= 0 && index < vertexCount;
    }

    private static void addEdge(
            Map<Long, Integer> edgeCountMap,
            int i0,
            int i1
    ) {
        if (i0 == i1) {
            return;
        }

        long key = edgeKey(i0, i1);
        edgeCountMap.put(key, edgeCountMap.getOrDefault(key, 0) + 1);
    }

    private static long edgeKey(int i0, int i1) {
        int a = Math.min(i0, i1);
        int b = Math.max(i0, i1);

        return (((long) a) << 32) | (b & 0xffffffffL);
    }



    public static boolean isGeometricBoundaryEdge(
            GaiaFace face,
            int edgeIndex,
            int[] weldedIndices,
            Map<Long, Integer> edgeCountMap
    ) {
        if (face == null || face.getIndices() == null || face.getIndices().length < 3) {
            return true;
        }

        int[] indices = face.getIndices();

        int a;
        int b;

        if (edgeIndex == 0) {
            a = indices[0];
            b = indices[1];
        } else if (edgeIndex == 1) {
            a = indices[1];
            b = indices[2];
        } else if (edgeIndex == 2) {
            a = indices[2];
            b = indices[0];
        } else {
            throw new IllegalArgumentException("edgeIndex must be 0, 1 or 2");
        }

        if (!isValidOriginalIndex(a, weldedIndices.length)
                || !isValidOriginalIndex(b, weldedIndices.length)) {
            return true;
        }

        int wa = weldedIndices[a];
        int wb = weldedIndices[b];

        if (wa < 0 || wb < 0) {
            return true;
        }

        long key = edgeKey(wa, wb);
        Integer count = edgeCountMap.get(key);

        return count == null || count == 1;
    }

    public static boolean[] buildWeldedBoundaryVertexFlags(
            Map<Long, Integer> edgeCountMap,
            int weldedVertexCount
    ) {
        boolean[] weldedBoundary = new boolean[weldedVertexCount];

        for (Map.Entry<Long, Integer> entry : edgeCountMap.entrySet()) {
            long key = entry.getKey();
            int count = entry.getValue();

            if (count == 1) {
                int a = edgeKeyA(key);
                int b = edgeKeyB(key);

                if (a >= 0 && a < weldedVertexCount) {
                    weldedBoundary[a] = true;
                }

                if (b >= 0 && b < weldedVertexCount) {
                    weldedBoundary[b] = true;
                }
            }
        }

        return weldedBoundary;
    }

    public static boolean[] buildWeldedBoundaryVertexFlags_SortEdges(
            List<GaiaFace> faces,
            int[] weldedIndices,
            int weldedVertexCount
    ) {
        boolean[] weldedBoundary = new boolean[weldedVertexCount];

        if (faces == null || faces.isEmpty() || weldedIndices == null || weldedVertexCount <= 0) {
            return weldedBoundary;
        }

        int triangleCount = 0;

        for (GaiaFace face : faces) {
            if (face == null || face.getIndices() == null) {
                continue;
            }

            triangleCount += face.getIndices().length / 3;
        }

        if (triangleCount == 0) {
            return weldedBoundary;
        }

        long[] edgeKeys = new long[triangleCount * 3];
        int edgeCount = 0;

        for (GaiaFace face : faces) {
            if (face == null || face.getIndices() == null) {
                continue;
            }

            int[] indices = face.getIndices();
            int triangleCountInFace = indices.length / 3;

            for (int t = 0; t < triangleCountInFace; t++) {
                int idx0 = indices[t * 3];
                int idx1 = indices[t * 3 + 1];
                int idx2 = indices[t * 3 + 2];

                if (!isValidOriginalIndex(idx0, weldedIndices.length)
                        || !isValidOriginalIndex(idx1, weldedIndices.length)
                        || !isValidOriginalIndex(idx2, weldedIndices.length)) {
                    continue;
                }

                int i0 = weldedIndices[idx0];
                int i1 = weldedIndices[idx1];
                int i2 = weldedIndices[idx2];

                if (i0 < 0 || i1 < 0 || i2 < 0) {
                    continue;
                }

                if (i0 != i1) edgeKeys[edgeCount++] = edgeKey(i0, i1);
                if (i1 != i2) edgeKeys[edgeCount++] = edgeKey(i1, i2);
                if (i2 != i0) edgeKeys[edgeCount++] = edgeKey(i2, i0);
            }
        }

        if (edgeCount == 0) {
            return weldedBoundary;
        }

        java.util.Arrays.sort(edgeKeys, 0, edgeCount);

        int start = 0;

        while (start < edgeCount) {
            long key = edgeKeys[start];

            int end = start + 1;
            while (end < edgeCount && edgeKeys[end] == key) {
                end++;
            }

            int count = end - start;

            if (count == 1) {
                int a = edgeKeyA(key);
                int b = edgeKeyB(key);

                if (a >= 0 && a < weldedVertexCount) {
                    weldedBoundary[a] = true;
                }

                if (b >= 0 && b < weldedVertexCount) {
                    weldedBoundary[b] = true;
                }
            }

            start = end;
        }

        return weldedBoundary;
    }

    private static int edgeKeyA(long key) {
        return (int) (key >> 32);
    }

    private static int edgeKeyB(long key) {
        return (int) key;
    }

    public static boolean[] buildOriginalBoundaryVertexFlags(
            int[] weldedIndices,
            boolean[] weldedBoundary
    ) {
        boolean[] originalBoundary = new boolean[weldedIndices.length];

        for (int originalIndex = 0; originalIndex < weldedIndices.length; originalIndex++) {
            int weldedIndex = weldedIndices[originalIndex];

            // Este vértice no pertenece a las faces analizadas.
            // Por tanto, para este cálculo local no es frontera.
            if (weldedIndex < 0) {
                originalBoundary[originalIndex] = false;
                continue;
            }

            if (weldedIndex >= weldedBoundary.length) {
                originalBoundary[originalIndex] = false;
                continue;
            }

            originalBoundary[originalIndex] = weldedBoundary[weldedIndex];
        }

        return originalBoundary;
    }

    public static int getWeldedVertexCount(int[] weldedIndices) {
        int max = -1;

        for (int weldedIndex : weldedIndices) {
            if (weldedIndex > max) {
                max = weldedIndex;
            }
        }

        return max + 1;
    }

    public boolean[] findBoundaryVertices(
            List<GaiaVertex> vertices,
            List<GaiaFace> faces,
            double tolerance,
            int[] weldedIndices
    ) {
        if (vertices == null || vertices.isEmpty()) {
            return new boolean[0];
        }

        if (faces == null || faces.isEmpty()) {
            return new boolean[vertices.size()];
        }

        //int[] weldedIndices =
                buildWeldedVertexIndices_LowMemory(vertices, faces, tolerance, weldedIndices);

        int weldedVertexCount =
                getWeldedVertexCount(weldedIndices);

        boolean[] weldedBoundary =
                buildWeldedBoundaryVertexFlags_SortEdges(
                        faces,
                        weldedIndices,
                        weldedVertexCount
                );

        return buildOriginalBoundaryVertexFlags(weldedIndices, weldedBoundary);
    }

    public void findBoundaryVerticesFromIndices(
            List<GaiaVertex> vertices,
            int[] indices,
            double tolerance,
            int[] weldedIndices,
            boolean[] frontierVertices
    ) {
        if (vertices == null || vertices.isEmpty()) {
            return;
        }

        int vertexCount = vertices.size();

        if (weldedIndices == null || weldedIndices.length < vertexCount) {
            throw new IllegalArgumentException("weldedIndices is null or too small");
        }

        if (frontierVertices == null || frontierVertices.length < vertexCount) {
            throw new IllegalArgumentException("frontierVertices is null or too small");
        }

        java.util.Arrays.fill(frontierVertices, false);
        java.util.Arrays.fill(weldedIndices, -1);

        if (indices == null || indices.length < 3) {
            return;
        }

        buildWeldedVertexIndicesFromIndices(
                vertices,
                indices,
                tolerance,
                weldedIndices
        );

        int weldedVertexCount = getWeldedVertexCount(weldedIndices);

        boolean[] weldedBoundary =
                buildWeldedBoundaryVertexFlagsFromIndices_SortEdges(
                        indices,
                        weldedIndices,
                        weldedVertexCount
                );

        fillOriginalBoundaryVertexFlags(
                weldedIndices,
                weldedBoundary,
                frontierVertices
        );
    }

    private static void fillOriginalBoundaryVertexFlags(
            int[] weldedIndices,
            boolean[] weldedBoundary,
            boolean[] frontierVertices
    ) {
        if (weldedIndices == null || weldedBoundary == null || frontierVertices == null) {
            return;
        }

        int count = Math.min(weldedIndices.length, frontierVertices.length);

        for (int originalIndex = 0; originalIndex < count; originalIndex++) {
            int weldedIndex = weldedIndices[originalIndex];

            if (weldedIndex < 0) {
                continue;
            }

            if (weldedIndex >= weldedBoundary.length) {
                continue;
            }

            if (weldedBoundary[weldedIndex]) {
                frontierVertices[originalIndex] = true;
            }
        }
    }

    public static void buildWeldedVertexIndicesFromIndices(
            List<GaiaVertex> vertices,
            int[] indices,
            double tolerance,
            int[] weldedIndices
    ) {
        int vertexCount = vertices.size();

        boolean[] used = new boolean[vertexCount];
        int usedCount = 0;

        for (int index : indices) {
            if (index < 0 || index >= vertexCount) {
                continue;
            }

            if (!used[index]) {
                used[index] = true;
                usedCount++;
            }
        }

        int[] usedIndices = new int[usedCount];
        int pos = 0;

        for (int i = 0; i < vertexCount; i++) {
            if (used[i]) {
                usedIndices[pos++] = i;
            }
        }

        if (usedIndices.length == 0) {
            return;
        }

        double invTolerance = 1.0 / tolerance;

        quickSortByQuantizedPosition(
                usedIndices,
                0,
                usedIndices.length - 1,
                vertices,
                invTolerance
        );

        int nextWeldedIndex = 0;

        int firstOriginalIndex = usedIndices[0];
        weldedIndices[firstOriginalIndex] = nextWeldedIndex;

        long prevX = qx(vertices.get(firstOriginalIndex), invTolerance);
        long prevY = qy(vertices.get(firstOriginalIndex), invTolerance);
        long prevZ = qz(vertices.get(firstOriginalIndex), invTolerance);

        for (int i = 1; i < usedIndices.length; i++) {
            int originalIndex = usedIndices[i];

            long x = qx(vertices.get(originalIndex), invTolerance);
            long y = qy(vertices.get(originalIndex), invTolerance);
            long z = qz(vertices.get(originalIndex), invTolerance);

            if (x != prevX || y != prevY || z != prevZ) {
                nextWeldedIndex++;
                prevX = x;
                prevY = y;
                prevZ = z;
            }

            weldedIndices[originalIndex] = nextWeldedIndex;
        }
    }

    public static boolean[] buildWeldedBoundaryVertexFlagsFromIndices_SortEdges(
            int[] indices,
            int[] weldedIndices,
            int weldedVertexCount
    ) {
        boolean[] weldedBoundary = new boolean[weldedVertexCount];

        if (indices == null || indices.length < 3 || weldedIndices == null || weldedVertexCount <= 0) {
            return weldedBoundary;
        }

        int triangleCount = indices.length / 3;

        long[] edgeKeys = new long[triangleCount * 3];
        int edgeCount = 0;

        for (int t = 0; t < triangleCount; t++) {
            int idx0 = indices[t * 3];
            int idx1 = indices[t * 3 + 1];
            int idx2 = indices[t * 3 + 2];

            if (idx0 < 0 || idx0 >= weldedIndices.length ||
                    idx1 < 0 || idx1 >= weldedIndices.length ||
                    idx2 < 0 || idx2 >= weldedIndices.length) {
                continue;
            }

            int i0 = weldedIndices[idx0];
            int i1 = weldedIndices[idx1];
            int i2 = weldedIndices[idx2];

            if (i0 < 0 || i1 < 0 || i2 < 0) {
                continue;
            }

            if (i0 != i1) {
                edgeKeys[edgeCount++] = edgeKey(i0, i1);
            }

            if (i1 != i2) {
                edgeKeys[edgeCount++] = edgeKey(i1, i2);
            }

            if (i2 != i0) {
                edgeKeys[edgeCount++] = edgeKey(i2, i0);
            }
        }

        if (edgeCount == 0) {
            return weldedBoundary;
        }

        java.util.Arrays.sort(edgeKeys, 0, edgeCount);

        int start = 0;

        while (start < edgeCount) {
            long key = edgeKeys[start];

            int end = start + 1;

            while (end < edgeCount && edgeKeys[end] == key) {
                end++;
            }

            int count = end - start;

            if (count == 1) {
                int a = edgeKeyA(key);
                int b = edgeKeyB(key);

                if (a >= 0 && a < weldedVertexCount) {
                    weldedBoundary[a] = true;
                }

                if (b >= 0 && b < weldedVertexCount) {
                    weldedBoundary[b] = true;
                }
            }

            start = end;
        }

        return weldedBoundary;
    }
}
