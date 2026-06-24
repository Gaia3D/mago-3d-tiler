package com.gaia3d.basic.remesher;

import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.*;

@Slf4j
public class DominantPlaneProjector {

    double maxPlaneDistance = 0.05;

    private static class FaceGeometryDebugStats {
        int total = 0;

        int nullRef = 0;
        int nullFace = 0;
        int nullVertices = 0;
        int nullIndices = 0;
        int shortIndices = 0;
        int outOfBounds = 0;
        int nullVertex = 0;
        int nullPosition = 0;
        int degenerated = 0;

        int success = 0;

        int printedExamples = 0;
        int maxExamples = 5;

    }

    public static class FaceCluster {
        public int id = -1;
        public final List<GaiaFace> faces = new ArrayList<>();

        public Vector3d normal = new Vector3d();
        public Vector3d centroid = new Vector3d();
        public double area = 0.0;

        public FaceCluster(int id) {
            this.id = id;
        }
    }

    private static class VertexRef {
        GaiaPrimitive primitive;
        int vertexIndex;
        GaiaVertex vertex;

        VertexRef(GaiaPrimitive primitive, int vertexIndex, GaiaVertex vertex) {
            this.primitive = primitive;
            this.vertexIndex = vertexIndex;
            this.vertex = vertex;
        }
    }

    private static class FaceRef {
        GaiaPrimitive primitive;
        GaiaFace face;
        int localFaceIndex;

        Vector3d normal = new Vector3d();
        Vector3d centroid = new Vector3d();
        double area = 0.0;

        FaceRef(GaiaPrimitive primitive, GaiaFace face, int localFaceIndex) {
            this.primitive = primitive;
            this.face = face;
            this.localFaceIndex = localFaceIndex;
        }
    }

    private static class PosKey {
        final long x;
        final long y;
        final long z;

        PosKey(Vector3d p, double eps) {
            this.x = Math.round(p.x / eps);
            this.y = Math.round(p.y / eps);
            this.z = Math.round(p.z / eps);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof PosKey)) {return false;}
            PosKey other = (PosKey) o;
            return x == other.x && y == other.y && z == other.z;
        }

        @Override
        public int hashCode() {
            int h = Long.hashCode(x);
            h = 31 * h + Long.hashCode(y);
            h = 31 * h + Long.hashCode(z);
            return h;
        }
    }

    private static class EdgeKey {
        final PosKey a;
        final PosKey b;

        EdgeKey(PosKey p0, PosKey p1) {
            if (compare(p0, p1) <= 0) {
                this.a = p0;
                this.b = p1;
            } else {
                this.a = p1;
                this.b = p0;
            }
        }

        private static int compare(PosKey p0, PosKey p1) {
            if (p0.x != p1.x) {return Long.compare(p0.x, p1.x);}
            if (p0.y != p1.y) {return Long.compare(p0.y, p1.y);}
            return Long.compare(p0.z, p1.z);
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof EdgeKey)) {return false;}
            EdgeKey other = (EdgeKey) o;
            return a.equals(other.a) && b.equals(other.b);
        }

        @Override
        public int hashCode() {
            int h = a.hashCode();
            h = 31 * h + b.hashCode();
            return h;
        }
    }

    public PrimitiveClusterBuildResult buildClustersOnPrimitiveWithResult(
            GaiaPrimitive primitive,
            double positionEpsilon,
            double maxNormalAngleDeg,
            int minFacesPerCluster
    ) {
        PrimitiveClusterBuildResult result = new PrimitiveClusterBuildResult();

        if (primitive == null) {
            return result;
        }

        List<GaiaVertex> vertices = primitive.getVertices();

        if (vertices == null || vertices.isEmpty()) {
            log.debug("buildClustersOnPrimitiveWithResult: primitive has no vertices");
            return result;
        }

        if (primitive.getSurfaces() == null || primitive.getSurfaces().isEmpty()) {
            log.debug("buildClustersOnPrimitiveWithResult: primitive has no surfaces");
            return result;
        }

        double cosMaxAngle = Math.cos(Math.toRadians(maxNormalAngleDeg));

        FaceGeometryDebugStats stats = new FaceGeometryDebugStats();

        int faceIndex = 0;
        int failedFaces = 0;

        for (GaiaSurface surface : primitive.getSurfaces()) {
            if (surface == null || surface.getFaces() == null) {
                continue;
            }

            for (GaiaFace face : surface.getFaces()) {
                if (face == null) {
                    continue;
                }

                FaceRef ref = new FaceRef(primitive, face, faceIndex++);

                if (!computeFaceGeometry(ref, vertices, stats)) {
                    failedFaces++;
                    continue;
                }

                result.faceRefs.add(ref);
                result.faceRefMap.put(face, ref);
            }
        }

        log.debug("buildClustersOnPrimitiveWithResult: vertices = " + vertices.size());
        log.debug("buildClustersOnPrimitiveWithResult: total faces = " + faceIndex);
        log.debug("buildClustersOnPrimitiveWithResult: valid faceRefs = " + result.faceRefs.size());
        log.debug("buildClustersOnPrimitiveWithResult: failed faces = " + failedFaces);

        if (result.faceRefs.isEmpty()) {
            return result;
        }

        Map<EdgeKey, List<FaceRef>> edgeMap =
                buildEdgeMap(result.faceRefs, vertices, positionEpsilon);

        Map<FaceRef, Boolean> visited = new IdentityHashMap<>();

        int clusterId = 0;

        for (FaceRef seed : result.faceRefs) {
            if (visited.containsKey(seed)) {
                continue;
            }

            FaceCluster cluster = new FaceCluster(clusterId++);

            floodFillCluster(
                    seed,
                    cluster,
                    visited,
                    edgeMap,
                    vertices,
                    positionEpsilon,
                    cosMaxAngle
            );

            if (cluster.faces.size() >= minFacesPerCluster) {
                computeClusterGeometry(cluster, result.faceRefMap);
                result.clusters.add(cluster);
            }
        }

        log.debug("buildClustersOnPrimitiveWithResult: clusters = " + result.clusters.size());

        return result;
    }

    private Map<EdgeKey, List<FaceRef>> buildEdgeMap(
            List<FaceRef> faceRefs,
            List<GaiaVertex> vertices,
            double eps
    ) {
        Map<EdgeKey, List<FaceRef>> edgeMap = new HashMap<>();

        for (FaceRef ref : faceRefs) {
            int[] indices = ref.face.getIndices();
            if (indices == null || indices.length < 3) {continue;}

            for (int i = 0; i < indices.length; i++) {
                int idx0 = indices[i];
                int idx1 = indices[(i + 1) % indices.length];

                Vector3d p0 = vertices.get(idx0).getPosition();
                Vector3d p1 = vertices.get(idx1).getPosition();

                PosKey k0 = new PosKey(p0, eps);
                PosKey k1 = new PosKey(p1, eps);

                EdgeKey edgeKey = new EdgeKey(k0, k1);

                edgeMap.computeIfAbsent(edgeKey, k -> new ArrayList<>()).add(ref);
            }
        }

        return edgeMap;
    }

    private void floodFillCluster(
            FaceRef seed,
            FaceCluster cluster,
            Map<FaceRef, Boolean> visited,
            Map<EdgeKey, List<FaceRef>> edgeMap,
            List<GaiaVertex> vertices,
            double eps,
            double cosMaxAngle
    ) {
        ArrayDeque<FaceRef> queue = new ArrayDeque<>();

        visited.put(seed, true);
        queue.add(seed);

        while (!queue.isEmpty()) {
            FaceRef current = queue.poll();
            cluster.faces.add(current.face);

            int[] indices = current.face.getIndices();
            if (indices == null || indices.length < 3) {continue;}

            for (int i = 0; i < indices.length; i++) {
                int idx0 = indices[i];
                int idx1 = indices[(i + 1) % indices.length];

                Vector3d p0 = vertices.get(idx0).getPosition();
                Vector3d p1 = vertices.get(idx1).getPosition();

                EdgeKey edgeKey = new EdgeKey(
                        new PosKey(p0, eps),
                        new PosKey(p1, eps)
                );

                List<FaceRef> neighbors = edgeMap.get(edgeKey);
                if (neighbors == null) {continue;}

                for (FaceRef neighbor : neighbors) {
                    if (neighbor == current) {continue;}
                    if (visited.containsKey(neighbor)) {continue;}

                    double dot = Math.abs(current.normal.dot(neighbor.normal));

                    double dist = Math.abs(
                            seed.normal.x * (neighbor.centroid.x - seed.centroid.x) +
                                    seed.normal.y * (neighbor.centroid.y - seed.centroid.y) +
                                    seed.normal.z * (neighbor.centroid.z - seed.centroid.z)
                    );

                    if (dot >= cosMaxAngle && dist <= maxPlaneDistance) {
                        visited.put(neighbor, true);
                        queue.add(neighbor);
                    }
                }
            }
        }
    }

    private boolean computeFaceGeometry(
            FaceRef ref,
            List<GaiaVertex> vertices,
            FaceGeometryDebugStats stats
    ) {
        stats.total++;

        if (ref == null) {
            stats.nullRef++;
            return false;
        }

        if (ref.face == null) {
            stats.nullFace++;
            return false;
        }

        if (vertices == null || vertices.isEmpty()) {
            stats.nullVertices++;
            return false;
        }

        int[] indices = ref.face.getIndices();

        if (indices == null) {
            stats.nullIndices++;

            if (stats.printedExamples < stats.maxExamples) {
                log.debug("Example null indices. faceIndex = " + ref.localFaceIndex);
                stats.printedExamples++;
            }

            return false;
        }

        if (indices.length < 3) {
            stats.shortIndices++;

            if (stats.printedExamples < stats.maxExamples) {
                log.debug("Example short indices. faceIndex = " + ref.localFaceIndex +
                        ", indices.length = " + indices.length);
                stats.printedExamples++;
            }

            return false;
        }

        int idx0 = indices[0];
        int idx1 = indices[1];
        int idx2 = indices[2];

        if (idx0 < 0 || idx0 >= vertices.size() ||
                idx1 < 0 || idx1 >= vertices.size() ||
                idx2 < 0 || idx2 >= vertices.size()) {

            stats.outOfBounds++;

            if (stats.printedExamples < stats.maxExamples) {
                log.debug("Example out of bounds. faceIndex = " + ref.localFaceIndex +
                        ", vertices.size = " + vertices.size() +
                        ", indices.length = " + indices.length +
                        ", idx0 = " + idx0 +
                        ", idx1 = " + idx1 +
                        ", idx2 = " + idx2);
                stats.printedExamples++;
            }

            return false;
        }

        GaiaVertex gv0 = vertices.get(idx0);
        GaiaVertex gv1 = vertices.get(idx1);
        GaiaVertex gv2 = vertices.get(idx2);

        if (gv0 == null || gv1 == null || gv2 == null) {
            stats.nullVertex++;
            return false;
        }

        Vector3d p0 = gv0.getPosition();
        Vector3d p1 = gv1.getPosition();
        Vector3d p2 = gv2.getPosition();

        if (p0 == null || p1 == null || p2 == null) {
            stats.nullPosition++;
            return false;
        }

        double ux = p1.x - p0.x;
        double uy = p1.y - p0.y;
        double uz = p1.z - p0.z;

        double vx = p2.x - p0.x;
        double vy = p2.y - p0.y;
        double vz = p2.z - p0.z;

        double nx = uy * vz - uz * vy;
        double ny = uz * vx - ux * vz;
        double nz = ux * vy - uy * vx;

        double len = Math.sqrt(nx * nx + ny * ny + nz * nz);

        if (len < 1e-12) {
            stats.degenerated++;

            if (stats.printedExamples < stats.maxExamples) {
                log.debug("Example degenerated. faceIndex = " + ref.localFaceIndex +
                        ", len = " + len +
                        ", indices = [" + idx0 + ", " + idx1 + ", " + idx2 + "]" +
                        ", p0 = " + p0 +
                        ", p1 = " + p1 +
                        ", p2 = " + p2);
                stats.printedExamples++;
            }

            return false;
        }

        ref.area = 0.5 * len;
        ref.normal.set(nx / len, ny / len, nz / len);

        ref.centroid.set(0, 0, 0);

        for (int idx : indices) {
            if (idx < 0 || idx >= vertices.size()) {
                continue;
            }

            GaiaVertex vertex = vertices.get(idx);
            if (vertex == null || vertex.getPosition() == null) {
                continue;
            }

            ref.centroid.add(vertex.getPosition());
        }

        ref.centroid.div(indices.length);

        stats.success++;
        return true;
    }

    private void computeClusterGeometry(
            FaceCluster cluster,
            Map<GaiaFace, FaceRef> faceRefMap
    ) {
        Vector3d normalSum = new Vector3d();
        Vector3d centroidSum = new Vector3d();

        double totalArea = 0.0;

        for (GaiaFace face : cluster.faces) {
            FaceRef ref = faceRefMap.get(face);
            if (ref == null) {continue;}

            Vector3d weightedNormal = new Vector3d(ref.normal).mul(ref.area);
            Vector3d weightedCentroid = new Vector3d(ref.centroid).mul(ref.area);

            normalSum.add(weightedNormal);
            centroidSum.add(weightedCentroid);

            totalArea += ref.area;
        }

        if (totalArea > 1e-12) {
            cluster.normal.set(normalSum);
            cluster.normal.normalize();

            cluster.centroid.set(centroidSum).div(totalArea);
            cluster.area = totalArea;
        }
    }

    private Map<PosKey, List<VertexRef>> buildCoincidentVertexMap(
            GaiaPrimitive primitive,
            Vector3d[] originalPositions,
            double positionEpsilon
    ) {
        Map<PosKey, List<VertexRef>> map = new HashMap<>();

        if (primitive == null || primitive.getVertices() == null) {
            return map;
        }

        List<GaiaVertex> vertices = primitive.getVertices();

        for (int i = 0; i < vertices.size(); i++) {
            GaiaVertex vertex = vertices.get(i);
            if (vertex == null) {continue;}

            if (i < 0 || i >= originalPositions.length) {continue;}

            Vector3d originalPosition = originalPositions[i];
            if (originalPosition == null) {continue;}

            PosKey key = new PosKey(originalPosition, positionEpsilon);

            map.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(new VertexRef(primitive, i, vertex));
        }

        return map;
    }


    private Vector3d[] copyOriginalPositions(GaiaPrimitive primitive) {
        List<GaiaVertex> vertices = primitive.getVertices();

        Vector3d[] originalPositions = new Vector3d[vertices.size()];

        for (int i = 0; i < vertices.size(); i++) {
            GaiaVertex vertex = vertices.get(i);

            if (vertex != null && vertex.getPosition() != null) {
                originalPositions[i] = new Vector3d(vertex.getPosition());
            }
        }

        return originalPositions;
    }

    private Vector3d projectPointToPlane(
            Vector3d point,
            Vector3d planePoint,
            Vector3d planeNormal
    ) {
        Vector3d result = new Vector3d(point);

        double dist =
                planeNormal.x * (point.x - planePoint.x) +
                        planeNormal.y * (point.y - planePoint.y) +
                        planeNormal.z * (point.z - planePoint.z);

        result.x -= planeNormal.x * dist;
        result.y -= planeNormal.y * dist;
        result.z -= planeNormal.z * dist;

        return result;
    }

    public void projectClustersOnScene_SimpleTest(
            GaiaScene scene,
            double positionEpsilon,
            double maxNormalAngleDeg,
            int minFacesPerCluster,
            boolean useProjectionToPlane
    ) {
        if (scene == null || scene.getNodes() == null) {
            return;
        }

        for (GaiaNode node : scene.getNodes()) {
            projectClustersOnNode(
                    node,
                    positionEpsilon,
                    maxNormalAngleDeg,
                    minFacesPerCluster,
                    useProjectionToPlane
            );
        }
    }

    private void projectClustersOnNode(
            GaiaNode node,
            double positionEpsilon,
            double maxNormalAngleDeg,
            int minFacesPerCluster,
            boolean useProjectionToPlane
    ) {
        if (node == null) {
            return;
        }

        if (node.getMeshes() != null) {
            for (GaiaMesh mesh : node.getMeshes()) {
                projectClustersOnMesh(
                        mesh,
                        positionEpsilon,
                        maxNormalAngleDeg,
                        minFacesPerCluster,
                        useProjectionToPlane
                );
            }
        }

        if (node.getChildren() != null) {
            for (GaiaNode child : node.getChildren()) {
                projectClustersOnNode(
                        child,
                        positionEpsilon,
                        maxNormalAngleDeg,
                        minFacesPerCluster,
                        useProjectionToPlane
                );
            }
        }
    }

    private void projectClustersOnMesh(
            GaiaMesh mesh,
            double positionEpsilon,
            double maxNormalAngleDeg,
            int minFacesPerCluster,
            boolean useProjectionToPlane
    ) {
        if (mesh == null || mesh.getPrimitives() == null) {
            return;
        }

        for (GaiaPrimitive primitive : mesh.getPrimitives()) {
            projectClustersOnPrimitiveAuto(
                    primitive,
                    positionEpsilon,
                    maxNormalAngleDeg,
                    minFacesPerCluster,
                    useProjectionToPlane
            );
        }
    }

    private void projectClustersOnPrimitiveAuto(
            GaiaPrimitive primitive,
            double positionEpsilon,
            double maxNormalAngleDeg,
            int minFacesPerCluster,
            boolean useProjectionToPlane
    ) {
        if (primitive == null) {
            return;
        }

        List<GaiaVertex> vertices = primitive.getVertices();

        if (vertices == null || vertices.isEmpty()) {
            return;
        }

        // 1. Crear FaceClusters.
        PrimitiveClusterBuildResult buildResult = buildClustersOnPrimitiveWithResult(
                primitive,
                positionEpsilon,
                maxNormalAngleDeg,
                minFacesPerCluster
        );

        if (buildResult == null ||
                buildResult.clusters == null ||
                buildResult.clusters.isEmpty()) {

            log.debug("projectClustersOnPrimitiveAuto: no clusters.");
            return;
        }

        List<FaceCluster> clusters = buildResult.clusters;

        log.debug("projectClustersOnPrimitiveAuto: primitive vertices = " + vertices.size());
        log.debug("projectClustersOnPrimitiveAuto: clusters = " + clusters.size());

        // 2. Test sencillo.
        if (useProjectionToPlane) {
            projectClustersOnPrimitive_SimpleTest(
                    primitive,
                    clusters,
                    positionEpsilon
            );
        } else {
            moveClustersAlongNormal_SimpleTest(
                    primitive,
                    clusters,
                    positionEpsilon,
                    0.2 // moveDistance test
            );
        }
    }

    private static class PrimitiveClusterBuildResult {
        List<FaceCluster> clusters = new ArrayList<>();
        Map<GaiaFace, FaceRef> faceRefMap = new IdentityHashMap<>();
        List<FaceRef> faceRefs = new ArrayList<>();
    }

    public void projectClustersOnPrimitive_SimpleTest(
            GaiaPrimitive primitive,
            List<FaceCluster> clusters,
            double positionEpsilon
    ) {
        if (primitive == null || clusters == null || clusters.isEmpty()) {
            return;
        }

        List<GaiaVertex> vertices = primitive.getVertices();

        if (vertices == null || vertices.isEmpty()) {
            return;
        }

        Vector3d[] originalPositions = copyOriginalPositions(primitive);

        Map<PosKey, List<VertexRef>> coincidentVertexMap =
                buildCoincidentVertexMap(
                        primitive,
                        originalPositions,
                        positionEpsilon
                );

        log.debug("SimpleTest: clusters = " + clusters.size());
        log.debug("SimpleTest: vertices = " + vertices.size());
        log.debug("SimpleTest: coincident groups = " + coincidentVertexMap.size());

        // Para este test, yo probaría pequeños primero.
        clusters.sort(Comparator.comparingDouble(a -> a.area));

        int movedGroups = 0;
        int movedVertices = 0;
        int skippedTooFar = 0;

        double maxMoveDistance = 0.01;

        for (FaceCluster cluster : clusters) {
            if (cluster == null || cluster.faces == null || cluster.faces.isEmpty()) {
                continue;
            }

            if (cluster.normal == null || cluster.centroid == null) {
                continue;
            }

            if (cluster.normal.lengthSquared() < 1e-20) {
                continue;
            }

            Set<PosKey> projectedInThisCluster = new HashSet<>();

            Vector3d planeNormal = new Vector3d(cluster.normal);
            planeNormal.normalize();

            Vector3d planePoint = cluster.centroid;

            for (GaiaFace face : cluster.faces) {
                if (face == null) {continue;}

                int[] indices = face.getIndices();
                if (indices == null || indices.length < 3) {continue;}

                for (int idx : indices) {
                    if (idx < 0 || idx >= originalPositions.length) {
                        continue;
                    }

                    Vector3d originalPos = originalPositions[idx];
                    if (originalPos == null) {
                        continue;
                    }

                    PosKey key = new PosKey(originalPos, positionEpsilon);

                    if (!projectedInThisCluster.add(key)) {
                        continue;
                    }

                    List<VertexRef> coincidentRefs = coincidentVertexMap.get(key);

                    if (coincidentRefs == null || coincidentRefs.isEmpty()) {
                        continue;
                    }

                    Vector3d projected = projectPointToPlane(
                            originalPos,
                            planePoint,
                            planeNormal
                    );

                    if (projected.distance(originalPos) > maxMoveDistance) {
                        skippedTooFar++;
                        continue;
                    }

                    for (VertexRef ref : coincidentRefs) {
                        if (ref == null || ref.vertex == null) {continue;}
                        if (ref.vertex.getPosition() == null) {continue;}

                        ref.vertex.getPosition().set(projected);
                        movedVertices++;
                    }

                    movedGroups++;
                }
            }
        }

        log.debug("SimpleTest: movedGroups = " + movedGroups);
        log.debug("SimpleTest: movedVertices writes = " + movedVertices);
        log.debug("SimpleTest: skippedTooFar = " + skippedTooFar);
    }

    public void moveClustersAlongNormal_SimpleTest(
            GaiaPrimitive primitive,
            List<FaceCluster> clusters,
            double positionEpsilon,
            double moveDistance
    ) {
        if (primitive == null || clusters == null || clusters.isEmpty()) {
            return;
        }

        List<GaiaVertex> vertices = primitive.getVertices();

        if (vertices == null || vertices.isEmpty()) {
            return;
        }

        Vector3d[] originalPositions = copyOriginalPositions(primitive);

        Map<PosKey, List<VertexRef>> coincidentVertexMap =
                buildCoincidentVertexMap(
                        primitive,
                        originalPositions,
                        positionEpsilon
                );

        log.debug("MoveTest: clusters = " + clusters.size());
        log.debug("MoveTest: vertices = " + vertices.size());
        log.debug("MoveTest: coincident groups = " + coincidentVertexMap.size());

        int movedGroups = 0;
        int movedVertices = 0;

        for (FaceCluster cluster : clusters) {
            if (cluster == null || cluster.faces == null || cluster.faces.isEmpty()) {
                continue;
            }

            if (cluster.normal == null || cluster.normal.lengthSquared() < 1e-20) {
                continue;
            }

            Vector3d n = new Vector3d(cluster.normal);
            n.normalize();

            for (GaiaFace face : cluster.faces) {
                if (face == null) {continue;}

                int[] indices = face.getIndices();
                if (indices == null || indices.length < 3) {continue;}

                for (int idx : indices) {
                    if (idx < 0 || idx >= originalPositions.length) {continue;}

                    Vector3d originalPos = originalPositions[idx];
                    if (originalPos == null) {continue;}

                    PosKey key = new PosKey(originalPos, positionEpsilon);

                    List<VertexRef> coincidentRefs = coincidentVertexMap.get(key);
                    if (coincidentRefs == null || coincidentRefs.isEmpty()) {continue;}

                    Vector3d moved = new Vector3d(originalPos).add(
                            n.x * moveDistance,
                            n.y * moveDistance,
                            n.z * moveDistance
                    );

                    for (VertexRef ref : coincidentRefs) {
                        if (ref == null || ref.vertex == null) {continue;}
                        if (ref.vertex.getPosition() == null) {continue;}

                        ref.vertex.getPosition().set(moved);
                        movedVertices++;
                    }

                    movedGroups++;
                }
            }
        }

        log.debug("MoveTest: movedGroups = " + movedGroups);
        log.debug("MoveTest: movedVertices writes = " + movedVertices);
    }


}