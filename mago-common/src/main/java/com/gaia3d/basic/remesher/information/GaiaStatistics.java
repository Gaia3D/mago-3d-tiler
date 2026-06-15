package com.gaia3d.basic.remesher.information;

import com.gaia3d.basic.model.*;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Slf4j
@Getter
@Setter
public class GaiaStatistics {
    private static final double EPSILON = 1e-9;
    private static final double EDGE_1MM   = 0.001;
    private static final double EDGE_1CM   = 0.01;
    private static final double EDGE_5CM   = 0.05;
    private static final double EDGE_10CM  = 0.10;
    private static final double EDGE_25CM  = 0.25;
    private static final double EDGE_50CM  = 0.50;
    private static final double EDGE_100CM = 1.00;
    private static final double EDGE_150CM = 1.50;
    private static final double EDGE_200CM = 2.00;

    public double areaTotal = 0.0;
    public int trianglesCount = 0;
    public double trianglesDensity = 0.0;

    public double normalVariance = 0.0;
//    normalVariance ≈ 0.00  -> todos los triángulos miran parecido
//    normalVariance ≈ 0.05  -> casi plano / ordenado
//    normalVariance ≈ 0.15  -> algo rugoso
//    normalVariance ≈ 0.25+ -> bastante caótico
//    normalVariance ≈ 0.40+ -> muy caótico
    private double normalVarianceWeightedSum = 0.0;

    public double verticalRange = 0.0;
    public double areaFoldRatio = 0.0;

//    trianglesDensity  -> hay muchos triángulos pequeños
//    normalVariance    -> las normales son caóticas
//    areaFoldRatio     -> hay mucha superficie plegada dentro del volumen (lisa = 1, plegada > 1)

    public double minX = Double.POSITIVE_INFINITY;
    public double minY = Double.POSITIVE_INFINITY;
    public double minZ = Double.POSITIVE_INFINITY;

    public double maxX = Double.NEGATIVE_INFINITY;
    public double maxY = Double.NEGATIVE_INFINITY;
    public double maxZ = Double.NEGATIVE_INFINITY;

    public double edgeSizeTotal = 0.0;
    public int edgeSizeCount = 0;
    public double averageEdgeSize = 0.0;

    public double minEdgeSize = Double.POSITIVE_INFINITY;
    public double maxEdgeSize = 0.0;

    public double trianglesDensityByXY = 0.0;
    public double trianglesDensityByMaxProjection = 0.0;
    public double trianglesDensityBySurfaceArea = 0.0;
    public double trianglesDensityByMaxProjectedArea = 0.0;

    public double areaFoldRatioByXY = 0.0;
    public double areaFoldRatioByMaxProjection = 0.0;

    public int edgesBelow1mm = 0;
    public int edgesBelow1cm = 0;
    public int edgesBelow5cm = 0;
    public int edgesBelow10cm = 0;
    public int edgesBelow25cm = 0;
    public int edgesBelow50cm = 0;
    public int edgesBelow100cm = 0;
    public int edgesBelow150cm = 0;
    public int edgesBelow200cm = 0;

    public double edgesBelow1mmRatio = 0.0;
    public double edgesBelow1cmRatio = 0.0;
    public double edgesBelow5cmRatio = 0.0;
    public double edgesBelow10cmRatio = 0.0;
    public double edgesBelow25cmRatio = 0.0;
    public double edgesBelow50cmRatio = 0.0;
    public double edgesBelow100cmRatio = 0.0;
    public double edgesBelow150cmRatio = 0.0;
    public double edgesBelow200cmRatio = 0.0;



    public void logGaiaStatisticsCompact(String title) {
        if (!hasValidBoundingBox()) {
            log.info(
                    "{} | triangles={}, area={}, density={}, normalVariance={}, verticalRange={}, areaFoldRatio={}, " +
                            "avgEdge={}, minEdge={}, maxEdge={}, bboxSize=INVALID",
                    title,
                    trianglesCount,
                    areaTotal,
                    trianglesDensity,
                    normalVariance,
                    verticalRange,
                    areaFoldRatio,
                    averageEdgeSize,
                    minEdgeSize,
                    maxEdgeSize
            );
            return;
        }

        double sizeX = maxX - minX;
        double sizeY = maxY - minY;
        double sizeZ = maxZ - minZ;

        log.info(
                "{} | triangles={}, area={}, density={}, normalVariance={}, verticalRange={}, areaFoldRatio={}, " +
                        "avgEdge={}, minEdge={}, maxEdge={}, bboxSize=({}, {}, {})",
                title,
                trianglesCount,
                areaTotal,
                trianglesDensity,
                normalVariance,
                verticalRange,
                areaFoldRatio,
                averageEdgeSize,
                minEdgeSize,
                maxEdgeSize,
                sizeX,
                sizeY,
                sizeZ
        );
    }

    public void logGaiaStatistics(String title) {

        log.info("========== GaiaStatistics : {} ==========", title);

        log.info("Triangles:");
        log.info("  trianglesCount   = {}", trianglesCount);
        log.info("  areaTotal        = {}", areaTotal);
        log.info("  trianglesDensity = {}", trianglesDensity);

        log.info("Normals / shape:");
        log.info("  normalVariance   = {}", normalVariance);
        log.info("  verticalRange    = {}", verticalRange);
        log.info("  areaFoldRatio    = {}", areaFoldRatio);

        log.info("BoundingBox:");
        log.info("  minX = {}, minY = {}, minZ = {}", minX, minY, minZ);
        log.info("  maxX = {}, maxY = {}, maxZ = {}", maxX, maxY, maxZ);
        log.info("  sizeX = {}", maxX - minX);
        log.info("  sizeY = {}", maxY - minY);
        log.info("  sizeZ = {}", maxZ - minZ);

        log.info("Edges:");
        log.info("  edgeSizeTotal   = {}", edgeSizeTotal);
        log.info("  edgeSizeCount   = {}", edgeSizeCount);
        log.info("  averageEdgeSize = {}", averageEdgeSize);
        log.info("  minEdgeSize     = {}", minEdgeSize);
        log.info("  maxEdgeSize     = {}", maxEdgeSize);

        log.info("Small edges:");
        log.info("  edgesBelow1mm   = {} ({} %)", edgesBelow1mm, edgesBelow1mmRatio * 100.0);
        log.info("  edgesBelow1cm   = {} ({} %)", edgesBelow1cm, edgesBelow1cmRatio * 100.0);
        log.info("  edgesBelow5cm   = {} ({} %)", edgesBelow5cm, edgesBelow5cmRatio * 100.0);
        log.info("  edgesBelow10cm  = {} ({} %)", edgesBelow10cm, edgesBelow10cmRatio * 100.0);
        log.info("  edgesBelow25cm  = {} ({} %)", edgesBelow25cm, edgesBelow25cmRatio * 100.0);
        log.info("  edgesBelow50cm  = {} ({} %)", edgesBelow50cm, edgesBelow50cmRatio * 100.0);
        log.info("  edgesBelow100cm = {} ({} %)", edgesBelow100cm, edgesBelow100cmRatio * 100.0);
        log.info("  edgesBelow150cm = {} ({} %)", edgesBelow150cm, edgesBelow150cmRatio * 100.0);
        log.info("  edgesBelow200cm = {} ({} %)", edgesBelow200cm, edgesBelow200cmRatio * 100.0);

        log.info("Interpretation:");
        log.info("  normalVariance: 0.00=planar, 0.05=ordered, 0.15=rough, 0.25+=chaotic, 0.40+=very chaotic");
        log.info("  areaFoldRatio : 1.0=smooth/simple, >1.0=folded/complex surface");
        log.info("  trianglesDensity: high value means many small triangles per area");

        log.info("==========================================");
    }

    private boolean hasValidBoundingBox() {
        return Double.isFinite(minX) && Double.isFinite(minY) && Double.isFinite(minZ)
                && Double.isFinite(maxX) && Double.isFinite(maxY) && Double.isFinite(maxZ)
                && maxX >= minX
                && maxY >= minY
                && maxZ >= minZ;
    }

    private static double calculateDistance(Vector3d a, Vector3d b) {
        double dx = b.x - a.x;
        double dy = b.y - a.y;
        double dz = b.z - a.z;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static GaiaStatistics calculateStatistics(GaiaScene scene) {
        GaiaStatistics totalStats = new GaiaStatistics();

        if (scene == null || scene.getNodes() == null) {
            return totalStats;
        }

        for (GaiaNode node : scene.getNodes()) {
            accumulateNodeStatistics(node, totalStats);
        }

        totalStats.finishAccumulatedStatistics();

        return totalStats;
    }

    private static void accumulateNodeStatistics(GaiaNode node, GaiaStatistics totalStats) {
        if (node == null) {
            return;
        }

        if (node.getMeshes() != null) {
            for (GaiaMesh mesh : node.getMeshes()) {
                accumulateMeshStatistics(mesh, totalStats);
            }
        }

        if (node.getChildren() != null) {
            for (GaiaNode child : node.getChildren()) {
                accumulateNodeStatistics(child, totalStats);
            }
        }
    }

    private static void accumulateMeshStatistics(GaiaMesh mesh, GaiaStatistics totalStats) {
        if (mesh == null || mesh.getPrimitives() == null) {
            return;
        }

        for (GaiaPrimitive primitive : mesh.getPrimitives()) {
            GaiaStatistics primitiveStats = GaiaStatistics.calculateStatistics(primitive);
            totalStats.accumulate(primitiveStats);
        }
    }

    public void accumulate(GaiaStatistics other) {
        if (other == null || other.trianglesCount == 0) {
            return;
        }

        this.areaTotal += other.areaTotal;
        this.trianglesCount += other.trianglesCount;

        this.edgeSizeTotal += other.edgeSizeTotal;
        this.edgeSizeCount += other.edgeSizeCount;

        this.edgesBelow1mm += other.edgesBelow1mm;
        this.edgesBelow1cm += other.edgesBelow1cm;
        this.edgesBelow5cm += other.edgesBelow5cm;
        this.edgesBelow10cm += other.edgesBelow10cm;
        this.edgesBelow25cm += other.edgesBelow25cm;
        this.edgesBelow50cm += other.edgesBelow50cm;
        this.edgesBelow100cm += other.edgesBelow100cm;
        this.edgesBelow150cm += other.edgesBelow150cm;
        this.edgesBelow200cm += other.edgesBelow200cm;

        this.minEdgeSize = Math.min(this.minEdgeSize, other.minEdgeSize);
        this.maxEdgeSize = Math.max(this.maxEdgeSize, other.maxEdgeSize);

        this.minX = Math.min(this.minX, other.minX);
        this.minY = Math.min(this.minY, other.minY);
        this.minZ = Math.min(this.minZ, other.minZ);

        this.maxX = Math.max(this.maxX, other.maxX);
        this.maxY = Math.max(this.maxY, other.maxY);
        this.maxZ = Math.max(this.maxZ, other.maxZ);

        this.normalVarianceWeightedSum += other.normalVarianceWeightedSum;
    }

    public void finishAccumulatedStatistics() {
        finishEdgeStatistics();

        if (this.areaTotal <= EPSILON || this.trianglesCount == 0) {
            this.trianglesDensity = 0.0;
            this.areaFoldRatio = 0.0;
            this.normalVariance = 0.0;
            this.verticalRange = 0.0;
            return;
        }

        double sizeX = this.maxX - this.minX;
        double sizeY = this.maxY - this.minY;
        double sizeZ = this.maxZ - this.minZ;

        this.verticalRange = sizeZ;

        double projectedAreaXY = sizeX * sizeY;
        double projectedAreaXZ = sizeX * sizeZ;
        double projectedAreaYZ = sizeY * sizeZ;

        double maxProjectedArea = Math.max(
                projectedAreaXY,
                Math.max(projectedAreaXZ, projectedAreaYZ)
        );

        maxProjectedArea = Math.max(maxProjectedArea, EPSILON);

        this.trianglesDensity = this.trianglesCount / maxProjectedArea;
        this.areaFoldRatio = this.areaTotal / maxProjectedArea;

        if (this.areaTotal > EPSILON) {
            this.normalVariance = this.normalVarianceWeightedSum / this.areaTotal;
        } else {
            this.normalVariance = 0.0;
        }
    }

    private static void accumulateEdgeStatistics(GaiaStatistics stats, double edgeLength) {
        if (stats == null || edgeLength <= EPSILON || !Double.isFinite(edgeLength)) {
            return;
        }

        stats.edgeSizeTotal += edgeLength;
        stats.edgeSizeCount++;

        stats.minEdgeSize = Math.min(stats.minEdgeSize, edgeLength);
        stats.maxEdgeSize = Math.max(stats.maxEdgeSize, edgeLength);

        if (edgeLength < EDGE_1MM) {
            stats.edgesBelow1mm++;
        }
        if (edgeLength < EDGE_1CM) {
            stats.edgesBelow1cm++;
        }
        if (edgeLength < EDGE_5CM) {
            stats.edgesBelow5cm++;
        }
        if (edgeLength < EDGE_10CM) {
            stats.edgesBelow10cm++;
        }
        if (edgeLength < EDGE_25CM) {
            stats.edgesBelow25cm++;
        }
        if (edgeLength < EDGE_50CM) {
            stats.edgesBelow50cm++;
        }
        if (edgeLength < EDGE_100CM) {
            stats.edgesBelow100cm++;
        }
        if (edgeLength < EDGE_150CM) {
            stats.edgesBelow150cm++;
        }
        if (edgeLength < EDGE_200CM) {
            stats.edgesBelow200cm++;
        }
    }

    private void resetDerivedStatistics() {
        this.verticalRange = 0.0;
        this.trianglesDensity = 0.0;
        this.areaFoldRatio = 0.0;
        this.normalVariance = 0.0;

        this.trianglesDensityByXY = 0.0;
        this.trianglesDensityByMaxProjection = 0.0;
        this.trianglesDensityBySurfaceArea = 0.0;

        this.areaFoldRatioByXY = 0.0;
        this.areaFoldRatioByMaxProjection = 0.0;
    }

    public static GaiaStatistics calculateStatistics(GaiaPrimitive primitive) {
        GaiaStatistics totalStats = new GaiaStatistics();

        if (primitive == null || primitive.getSurfaces() == null) {
            return totalStats;
        }

        for (GaiaSurface surface : primitive.getSurfaces()) {
            if (surface == null || surface.getFaces() == null) {
                continue;
            }

            GaiaStatistics surfaceStats =
                    GaiaStatistics.calculateStatistics(surface.getFaces(), primitive.getVertices());

            totalStats.accumulate(surfaceStats);
        }

        totalStats.finishAccumulatedStatistics();

        return totalStats;
    }

    public static GaiaStatistics calculateStatistics(List<GaiaFace> faces, List<GaiaVertex> vertices) {
        GaiaStatistics stats = new GaiaStatistics();

        if (faces == null || faces.isEmpty() || vertices == null || vertices.isEmpty()) {
            return stats;
        }

        //List<GaiaVertex> vertices = primitive.getVertices();

        Vector3d weightedNormalSum = new Vector3d();

        // ------------------------------------------------------------
        // 1) First pass:
        //    - count triangles
        //    - calculate total area
        //    - calculate bbox
        //    - calculate weighted average normal
        // ------------------------------------------------------------
        for (GaiaFace face : faces) {
            if (face == null || face.getIndices() == null || face.getIndices().length < 3) {
                continue;
            }

            int[] indices = face.getIndices();

            int idx0 = indices[0];
            int idx1 = indices[1];
            int idx2 = indices[2];

            if (!isValidIndex(idx0, vertices.size())
                    || !isValidIndex(idx1, vertices.size())
                    || !isValidIndex(idx2, vertices.size())) {
                continue;
            }

            Vector3d p0 = vertices.get(idx0).getPosition();
            Vector3d p1 = vertices.get(idx1).getPosition();
            Vector3d p2 = vertices.get(idx2).getPosition();

            if (p0 == null || p1 == null || p2 == null) {
                continue;
            }

            updateBoundingBox(stats, p0);
            updateBoundingBox(stats, p1);
            updateBoundingBox(stats, p2);

            Vector3d normal = calculateFaceNormal(p0, p1, p2);
            double area = calculateTriangleArea(p0, p1, p2);

            if (area <= EPSILON) {
                continue;
            }

            stats.trianglesCount++;
            stats.areaTotal += area;

            //////////////////////////////
            double edge01 = calculateDistance(p0, p1);
            double edge12 = calculateDistance(p1, p2);
            double edge20 = calculateDistance(p2, p0);

            accumulateEdgeStatistics(stats, edge01);
            accumulateEdgeStatistics(stats, edge12);
            accumulateEdgeStatistics(stats, edge20);
            ////////////////////////////

            weightedNormalSum.x += normal.x * area;
            weightedNormalSum.y += normal.y * area;
            weightedNormalSum.z += normal.z * area;
        }

        // ------------------------------------------------------------
        // Finish first-pass derived statistics.
        // ------------------------------------------------------------
        stats.finishEdgeStatistics();

        if (stats.trianglesCount == 0 || stats.areaTotal <= EPSILON) {
            return stats;
        }

        double sizeX = stats.maxX - stats.minX;
        double sizeY = stats.maxY - stats.minY;
        double sizeZ = stats.maxZ - stats.minZ;

        stats.verticalRange = sizeZ;

        double projectedAreaXY = sizeX * sizeY;
        double projectedAreaXZ = sizeX * sizeZ;
        double projectedAreaYZ = sizeY * sizeZ;

        double maxProjectedArea = Math.max(
                projectedAreaXY,
                Math.max(projectedAreaXZ, projectedAreaYZ)
        );

        maxProjectedArea = Math.max(maxProjectedArea, EPSILON);

        // IMPORTANT:
        // Use the same criterion as finishAccumulatedStatistics().
        stats.trianglesDensity = stats.trianglesCount / maxProjectedArea;
        stats.areaFoldRatio = stats.areaTotal / maxProjectedArea;

        Vector3d averageNormal = new Vector3d(weightedNormalSum);
        if (averageNormal.lengthSquared() > EPSILON) {
            averageNormal.normalize();
        } else {
            averageNormal.set(0.0, 0.0, 1.0);
        }

        // ------------------------------------------------------------
        // 2) Second pass:
        //    - calculate normal variance
        // ------------------------------------------------------------
        double varianceSum = 0.0;
        double varianceWeightSum = 0.0;

        for (GaiaFace face : faces) {
            if (face == null || face.getIndices() == null || face.getIndices().length < 3) {
                continue;
            }

            int[] indices = face.getIndices();

            int idx0 = indices[0];
            int idx1 = indices[1];
            int idx2 = indices[2];

            if (!isValidIndex(idx0, vertices.size())
                    || !isValidIndex(idx1, vertices.size())
                    || !isValidIndex(idx2, vertices.size())) {
                continue;
            }

            Vector3d p0 = vertices.get(idx0).getPosition();
            Vector3d p1 = vertices.get(idx1).getPosition();
            Vector3d p2 = vertices.get(idx2).getPosition();

            if (p0 == null || p1 == null || p2 == null) {
                continue;
            }

            double area = calculateTriangleArea(p0, p1, p2);
            if (area <= EPSILON) {
                continue;
            }

            Vector3d normal = calculateFaceNormal(p0, p1, p2);

            double dot = Math.abs(normal.dot(averageNormal));
            dot = clamp(dot, 0.0, 1.0);

            double deviation = 1.0 - dot;

            varianceSum += deviation * area;
            varianceWeightSum += area;
        }

        if (varianceWeightSum > EPSILON) {
            stats.normalVariance = varianceSum / varianceWeightSum;
            stats.normalVarianceWeightedSum = varianceSum;
        } else {
            stats.normalVariance = 0.0;
            stats.normalVarianceWeightedSum = 0.0;
        }

        return stats;
    }

    private void finishEdgeStatistics() {
        if (this.edgeSizeCount <= 0) {
            this.averageEdgeSize = 0.0;
            this.minEdgeSize = 0.0;
            this.maxEdgeSize = 0.0;

            this.edgesBelow1mmRatio = 0.0;
            this.edgesBelow1cmRatio = 0.0;
            this.edgesBelow5cmRatio = 0.0;
            this.edgesBelow10cmRatio = 0.0;
            this.edgesBelow25cmRatio = 0.0;
            this.edgesBelow50cmRatio = 0.0;
            this.edgesBelow100cmRatio = 0.0;
            this.edgesBelow150cmRatio = 0.0;
            this.edgesBelow200cmRatio = 0.0;
            return;
        }

        this.averageEdgeSize = this.edgeSizeTotal / this.edgeSizeCount;

        this.edgesBelow1mmRatio = (double) this.edgesBelow1mm / this.edgeSizeCount;
        this.edgesBelow1cmRatio = (double) this.edgesBelow1cm / this.edgeSizeCount;
        this.edgesBelow5cmRatio = (double) this.edgesBelow5cm / this.edgeSizeCount;
        this.edgesBelow10cmRatio = (double) this.edgesBelow10cm / this.edgeSizeCount;
        this.edgesBelow25cmRatio = (double) this.edgesBelow25cm / this.edgeSizeCount;
        this.edgesBelow50cmRatio = (double) this.edgesBelow50cm / this.edgeSizeCount;
        this.edgesBelow100cmRatio = (double) this.edgesBelow100cm / this.edgeSizeCount;
        this.edgesBelow150cmRatio = (double) this.edgesBelow150cm / this.edgeSizeCount;
        this.edgesBelow200cmRatio = (double) this.edgesBelow200cm / this.edgeSizeCount;
    }

    private static boolean isValidIndex(int index, int verticesCount) {
        return index >= 0 && index < verticesCount;
    }

    private static void updateBoundingBox(GaiaStatistics stats, Vector3d p) {
        if (p.x < stats.minX) stats.minX = p.x;
        if (p.y < stats.minY) stats.minY = p.y;
        if (p.z < stats.minZ) stats.minZ = p.z;

        if (p.x > stats.maxX) stats.maxX = p.x;
        if (p.y > stats.maxY) stats.maxY = p.y;
        if (p.z > stats.maxZ) stats.maxZ = p.z;
    }

    private static double calculateTriangleArea(Vector3d p0, Vector3d p1, Vector3d p2) {
        double ux = p1.x - p0.x;
        double uy = p1.y - p0.y;
        double uz = p1.z - p0.z;

        double vx = p2.x - p0.x;
        double vy = p2.y - p0.y;
        double vz = p2.z - p0.z;

        double cx = uy * vz - uz * vy;
        double cy = uz * vx - ux * vz;
        double cz = ux * vy - uy * vx;

        return 0.5 * Math.sqrt(cx * cx + cy * cy + cz * cz);
    }

    private static Vector3d calculateFaceNormal(Vector3d p0, Vector3d p1, Vector3d p2) {
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

        if (len <= EPSILON) {
            return new Vector3d(0.0, 0.0, 1.0);
        }

        return new Vector3d(nx / len, ny / len, nz / len);
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
