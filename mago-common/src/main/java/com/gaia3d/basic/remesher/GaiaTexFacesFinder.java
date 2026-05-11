package com.gaia3d.basic.remesher;

import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaSurface;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GaiaTexFacesFinder {

    public static class Island {
        private final List<Integer> faceIndices = new ArrayList<>();

        public List<Integer> getFaceIndices() {
            return faceIndices;
        }

        public int getFacesCount() {
            return faceIndices.size();
        }

        public List<GaiaFace> getFaces(List<GaiaFace> allFaces) {
            List<GaiaFace> result = new ArrayList<>(faceIndices.size());

            for (int faceIndex : faceIndices) {
                result.add(allFaces.get(faceIndex));
            }

            return result;
        }
    }

    private static class UnionFind {
        private final int[] parent;
        private final byte[] rank;

        public UnionFind(int size) {
            parent = new int[size];
            rank = new byte[size];

            for (int i = 0; i < size; i++) {
                parent[i] = i;
            }
        }

        public int find(int x) {
            int root = x;

            while (parent[root] != root) {
                root = parent[root];
            }

            while (parent[x] != x) {
                int next = parent[x];
                parent[x] = root;
                x = next;
            }

            return root;
        }

        public void union(int a, int b) {
            int rootA = find(a);
            int rootB = find(b);

            if (rootA == rootB) {
                return;
            }

            if (rank[rootA] < rank[rootB]) {
                parent[rootA] = rootB;
            } else if (rank[rootA] > rank[rootB]) {
                parent[rootB] = rootA;
            } else {
                parent[rootB] = rootA;
                rank[rootA]++;
            }
        }
    }

    private static int countTriangles(List<GaiaFace> faces) {
        int triangleCount = 0;

        for (GaiaFace face : faces) {
            if (face == null || face.getIndices() == null) {
                continue;
            }

            triangleCount += face.getIndices().length / 3;
        }

        return triangleCount;
    }

    private static List<Island> buildIslandsFromUnionFind(
            List<GaiaFace> faces,
            UnionFind unionFind
    ) {
        List<Island> result = new ArrayList<>();
        Map<Integer, Island> rootToIsland = new HashMap<>();

        int facesCount = faces.size();

        for (int faceIndex = 0; faceIndex < facesCount; faceIndex++) {
            GaiaFace face = faces.get(faceIndex);

            if (face == null || face.getIndices() == null || face.getIndices().length < 3) {
                continue;
            }

            int root = unionFind.find(faceIndex);

            Island island = rootToIsland.get(root);
            if (island == null) {
                island = new Island();
                rootToIsland.put(root, island);
            }

            island.faceIndices.add(faceIndex);
        }

        result.addAll(rootToIsland.values());

        return result;
    }

    private static long makeEdgeRecord(int i0, int i1, int faceIndex) {
        int a = Math.min(i0, i1);
        int b = Math.max(i0, i1);

        long edgeHash = hashEdgeTo32Bits(a, b);

        return (edgeHash << 32) | (faceIndex & 0xffffffffL);
    }

    private static long getEdgeKeyFromRecord(long record) {
        return record >>> 32;
    }

    private static int getFaceIndexFromRecord(long record) {
        return (int) record;
    }

    private static long hashEdgeTo32Bits(int a, int b) {
        long h = 1469598103934665603L;

        h ^= a;
        h *= 1099511628211L;

        h ^= b;
        h *= 1099511628211L;

        return h & 0xffffffffL;
    }

    public static List<Island> findIslandsLowMemorySafe(GaiaSurface surface) {
        List<Island> result = new ArrayList<>();

        if (surface == null || surface.getFaces() == null || surface.getFaces().isEmpty()) {
            return result;
        }

        List<GaiaFace> faces = surface.getFaces();
        int facesCount = faces.size();

        UnionFind unionFind = new UnionFind(facesCount);

        int totalTriangleCount = countTriangles(faces);

        if (totalTriangleCount == 0) {
            return result;
        }

        int maxEdges = totalTriangleCount * 3;

        long[] edgeKeys = new long[maxEdges];
        int[] faceIndices = new int[maxEdges];

        int edgeCount = 0;

        for (int faceIndex = 0; faceIndex < facesCount; faceIndex++) {
            GaiaFace face = faces.get(faceIndex);

            if (face == null || face.getIndices() == null) {
                continue;
            }

            int[] indices = face.getIndices();

            for (int i = 0; i + 2 < indices.length; i += 3) {
                int i0 = indices[i];
                int i1 = indices[i + 1];
                int i2 = indices[i + 2];

                if (i0 != i1) {
                    edgeKeys[edgeCount] = edgeKey(i0, i1);
                    faceIndices[edgeCount] = faceIndex;
                    edgeCount++;
                }

                if (i1 != i2) {
                    edgeKeys[edgeCount] = edgeKey(i1, i2);
                    faceIndices[edgeCount] = faceIndex;
                    edgeCount++;
                }

                if (i2 != i0) {
                    edgeKeys[edgeCount] = edgeKey(i2, i0);
                    faceIndices[edgeCount] = faceIndex;
                    edgeCount++;
                }
            }
        }

        if (edgeCount == 0) {
            return buildIslandsFromUnionFind(faces, unionFind);
        }

        quickSortEdges(edgeKeys, faceIndices, 0, edgeCount - 1);

        int start = 0;

        while (start < edgeCount) {
            long key = edgeKeys[start];

            int end = start + 1;

            while (end < edgeCount && edgeKeys[end] == key) {
                end++;
            }

            int firstFaceIndex = faceIndices[start];

            for (int i = start + 1; i < end; i++) {
                unionFind.union(firstFaceIndex, faceIndices[i]);
            }

            start = end;
        }

        return buildIslandsFromUnionFind(faces, unionFind);
    }

    private static long edgeKey(int i0, int i1) {
        int a = Math.min(i0, i1);
        int b = Math.max(i0, i1);

        return (((long) a) << 32) | (b & 0xffffffffL);
    }

    private static void quickSortEdges(
            long[] edgeKeys,
            int[] faceIndices,
            int left,
            int right
    ) {
        int i = left;
        int j = right;
        long pivot = edgeKeys[left + (right - left) / 2];

        while (i <= j) {
            while (edgeKeys[i] < pivot) {
                i++;
            }

            while (edgeKeys[j] > pivot) {
                j--;
            }

            if (i <= j) {
                long tmpKey = edgeKeys[i];
                edgeKeys[i] = edgeKeys[j];
                edgeKeys[j] = tmpKey;

                int tmpFace = faceIndices[i];
                faceIndices[i] = faceIndices[j];
                faceIndices[j] = tmpFace;

                i++;
                j--;
            }
        }

        if (left < j) {
            quickSortEdges(edgeKeys, faceIndices, left, j);
        }

        if (i < right) {
            quickSortEdges(edgeKeys, faceIndices, i, right);
        }
    }
}