package com.gaia3d.basic.remesher;

import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaVertex;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GaiaFrontierFinder {
    private static int[] collectUsedVertexIndices(List<GaiaFace> faces, int vertexCount) {
        boolean[] used = new boolean[vertexCount];
        int usedCount = 0;

        for (GaiaFace face : faces) {
            if (face == null || face.getIndices() == null) {continue;}

            int[] indices = face.getIndices();
            for (int index : indices) {
                if (index < 0 || index >= vertexCount) {continue;}

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
        if (ax != bx) {return Long.compare(ax, bx);}

        long ay = qy(va, invTolerance);
        long by = qy(vb, invTolerance);
        if (ay != by) {return Long.compare(ay, by);}

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

    private static boolean isValidOriginalIndex(int index, int vertexCount) {
        return index >= 0 && index < vertexCount;
    }

    private static long edgeKey(int i0, int i1) {
        int a = Math.min(i0, i1);
        int b = Math.max(i0, i1);

        return (((long) a) << 32) | (b & 0xffffffffL);
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

                if (i0 != i1) {edgeKeys[edgeCount++] = edgeKey(i0, i1);}
                if (i1 != i2) {edgeKeys[edgeCount++] = edgeKey(i1, i2);}
                if (i2 != i0) {edgeKeys[edgeCount++] = edgeKey(i2, i0);}
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
}
