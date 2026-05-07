package com.gaia3d.basic.remesher;

import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaSurface;
import com.gaia3d.basic.model.GaiaVertex;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GaiaWeldedFacesFinder {

    public static class Island {
        private final List<GaiaFace> faces = new ArrayList<>();
        private final List<Integer> faceIndices = new ArrayList<>();

        public List<GaiaFace> getFaces() {
            return faces;
        }

        public List<Integer> getFaceIndices() {
            return faceIndices;
        }

        public int getFacesCount() {
            return faces.size();
        }
    }

    public static class PositionKey {
        public final long x;
        public final long y;
        public final long z;

        public PositionKey(Vector3d p, double tolerance) {
            this.x = Math.round(p.x / tolerance);
            this.y = Math.round(p.y / tolerance);
            this.z = Math.round(p.z / tolerance);
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (!(obj instanceof PositionKey)) return false;

            PositionKey other = (PositionKey) obj;
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            int result = Long.hashCode(x);
            result = 31 * result + Long.hashCode(y);
            result = 31 * result + Long.hashCode(z);
            return result;
        }
    }

    public static class EdgeKey {
        public final int a;
        public final int b;

        public EdgeKey(int i0, int i1) {
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
            if (this == obj) return true;
            if (!(obj instanceof EdgeKey)) return false;

            EdgeKey other = (EdgeKey) obj;
            return a == other.a && b == other.b;
        }

        @Override
        public int hashCode() {
            int result = Integer.hashCode(a);
            result = 31 * result + Integer.hashCode(b);
            return result;
        }
    }

    private static class UnionFind {
        private final int[] parent;
        private final int[] rank;

        public UnionFind(int size) {
            this.parent = new int[size];
            this.rank = new int[size];

            for (int i = 0; i < size; i++) {
                parent[i] = i;
                rank[i] = 0;
            }
        }

        public int find(int x) {
            int p = parent[x];

            if (p != x) {
                parent[x] = find(p);
            }

            return parent[x];
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

    public static List<Island> findIslands(
            GaiaSurface surface,
            List<GaiaVertex> vertices,
            double tolerance
    ) {
        List<Island> result = new ArrayList<>();

        if (surface == null || surface.getFaces() == null || surface.getFaces().isEmpty()) {
            return result;
        }

        if (vertices == null || vertices.isEmpty()) {
            return result;
        }

        List<GaiaFace> faces = surface.getFaces();
        int facesCount = faces.size();

        int[] weldedIndices = buildWeldedVertexIndices(vertices, tolerance);

        UnionFind unionFind = new UnionFind(facesCount);

        /*
         * Edge geométrico -> lista de faces que usan ese edge.
         *
         * Si varias faces usan el mismo edge welded, entonces pertenecen
         * a la misma isla geométrica.
         */
        Map<EdgeKey, List<Integer>> edgeToFaceIndices = new HashMap<>();

        for (int faceIndex = 0; faceIndex < facesCount; faceIndex++) {
            GaiaFace face = faces.get(faceIndex);

            if (face == null || face.getIndices() == null || face.getIndices().length < 3) {
                continue;
            }

            int[] indices = face.getIndices();
            int indicesCount = indices.length;

            for (int i = 0; i < indicesCount; i++) {
                int originalA = indices[i];
                int originalB = indices[(i + 1) % indicesCount];

                if (!isValidIndex(originalA, weldedIndices.length)
                        || !isValidIndex(originalB, weldedIndices.length)) {
                    continue;
                }

                int weldedA = weldedIndices[originalA];
                int weldedB = weldedIndices[originalB];

                if (weldedA == weldedB) {
                    continue;
                }

                EdgeKey edgeKey = new EdgeKey(weldedA, weldedB);

                List<Integer> edgeFaces = edgeToFaceIndices.computeIfAbsent(
                        edgeKey,
                        k -> new ArrayList<>()
                );

                /*
                 * Todas las faces que comparten este edge geométrico
                 * quedan en la misma isla.
                 */
                for (int otherFaceIndex : edgeFaces) {
                    unionFind.union(faceIndex, otherFaceIndex);
                }

                edgeFaces.add(faceIndex);
            }
        }

        Map<Integer, Island> rootToIsland = new HashMap<>();

        for (int faceIndex = 0; faceIndex < facesCount; faceIndex++) {
            GaiaFace face = faces.get(faceIndex);

            if (face == null || face.getIndices() == null || face.getIndices().length < 3) {
                continue;
            }

            int root = unionFind.find(faceIndex);

            Island island = rootToIsland.computeIfAbsent(root, k -> new Island());

            island.faces.add(face);
            island.faceIndices.add(faceIndex);
        }

        result.addAll(rootToIsland.values());

        return result;
    }

    public static int[] buildWeldedVertexIndices(
            List<GaiaVertex> vertices,
            double tolerance
    ) {
        int vertexCount = vertices.size();

        int[] weldedIndices = new int[vertexCount];

        Map<PositionKey, Integer> positionToWeldedIndex = new HashMap<>();

        int nextWeldedIndex = 0;

        for (int i = 0; i < vertexCount; i++) {
            GaiaVertex vertex = vertices.get(i);

            if (vertex == null || vertex.getPosition() == null) {
                weldedIndices[i] = -1;
                continue;
            }

            Vector3d position = vertex.getPosition();

            PositionKey key = new PositionKey(position, tolerance);

            Integer weldedIndex = positionToWeldedIndex.get(key);

            if (weldedIndex == null) {
                weldedIndex = nextWeldedIndex;
                positionToWeldedIndex.put(key, weldedIndex);
                nextWeldedIndex++;
            }

            weldedIndices[i] = weldedIndex;
        }

        return weldedIndices;
    }

    private static boolean isValidIndex(int index, int count) {
        return index >= 0 && index < count;
    }
}