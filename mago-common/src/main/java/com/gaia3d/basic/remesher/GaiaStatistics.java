package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.model.*;

import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Slf4j
public class GaiaStatistics {
    private static final double EPSILON = 1e-9;

    public double areaTotal = 0.0;
    public int trianglesCount = 0;
    public double trianglesDensity = 0.0;

    public double normalVariance = 0.0;
//    normalVariance ≈ 0.00  -> todos los triángulos miran parecido
//    normalVariance ≈ 0.05  -> casi plano / ordenado
//    normalVariance ≈ 0.15  -> algo rugoso
//    normalVariance ≈ 0.25+ -> bastante caótico
//    normalVariance ≈ 0.40+ -> muy caótico

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

        this.minEdgeSize = Math.min(this.minEdgeSize, other.minEdgeSize);
        this.maxEdgeSize = Math.max(this.maxEdgeSize, other.maxEdgeSize);

        this.minX = Math.min(this.minX, other.minX);
        this.minY = Math.min(this.minY, other.minY);
        this.minZ = Math.min(this.minZ, other.minZ);

        this.maxX = Math.max(this.maxX, other.maxX);
        this.maxY = Math.max(this.maxY, other.maxY);
        this.maxZ = Math.max(this.maxZ, other.maxZ);

        this.normalVariance += other.normalVariance * other.areaTotal;
    }

    public void finishAccumulatedStatistics() {
        if (this.areaTotal <= EPSILON || this.trianglesCount == 0) {
            return;
        }

        if (this.edgeSizeCount > 0) {
            this.averageEdgeSize = this.edgeSizeTotal / this.edgeSizeCount;
        }

        double sizeX = this.maxX - this.minX;
        double sizeY = this.maxY - this.minY;
        double sizeZ = this.maxZ - this.minZ;

        this.verticalRange = sizeZ;

        double projectedAreaXY = sizeX * sizeY;
        double projectedAreaXZ = sizeX * sizeZ;
        double projectedAreaYZ = sizeY * sizeZ;

        double maxProjectedArea = Math.max(projectedAreaXY, Math.max(projectedAreaXZ, projectedAreaYZ));
        maxProjectedArea = Math.max(maxProjectedArea, EPSILON);

        this.trianglesDensity = this.trianglesCount / maxProjectedArea;
        this.areaFoldRatio = this.areaTotal / maxProjectedArea;

        this.normalVariance = this.normalVariance / this.areaTotal;
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

            stats.edgeSizeTotal += edge01 + edge12 + edge20;
            stats.edgeSizeCount += 3;

            stats.minEdgeSize = Math.min(stats.minEdgeSize, edge01);
            stats.minEdgeSize = Math.min(stats.minEdgeSize, edge12);
            stats.minEdgeSize = Math.min(stats.minEdgeSize, edge20);

            stats.maxEdgeSize = Math.max(stats.maxEdgeSize, edge01);
            stats.maxEdgeSize = Math.max(stats.maxEdgeSize, edge12);
            stats.maxEdgeSize = Math.max(stats.maxEdgeSize, edge20);
            ////////////////////////////

            weightedNormalSum.x += normal.x * area;
            weightedNormalSum.y += normal.y * area;
            weightedNormalSum.z += normal.z * area;
        }

        if (stats.trianglesCount == 0 || stats.areaTotal <= EPSILON) {
            return stats;
        }

        stats.trianglesDensity = stats.trianglesCount / stats.areaTotal;

        double sizeX = stats.maxX - stats.minX;
        double sizeY = stats.maxY - stats.minY;
        double sizeZ = stats.maxZ - stats.minZ;

        stats.verticalRange = sizeZ;

        double projectedAreaXY = sizeX * sizeY;
        double projectedAreaXZ = sizeX * sizeZ;
        double projectedAreaYZ = sizeY * sizeZ;

        double maxProjectedArea = Math.max(projectedAreaXY, Math.max(projectedAreaXZ, projectedAreaYZ));
        stats.areaFoldRatio = stats.areaTotal / (maxProjectedArea + EPSILON);

        Vector3d averageNormal = new Vector3d(weightedNormalSum);
        if (averageNormal.lengthSquared() > EPSILON) {
            averageNormal.normalize();
        } else {
            averageNormal.set(0.0, 0.0, 1.0);
        }

        if (stats.edgeSizeCount > 0) {
            stats.averageEdgeSize = stats.edgeSizeTotal / stats.edgeSizeCount;
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
//            if (area <= EPSILON) {
//                continue;
//            }

            Vector3d normal = calculateFaceNormal(p0, p1, p2);

            double dot = Math.abs(normal.dot(averageNormal));
            dot = clamp(dot, 0.0, 1.0);

            double deviation = 1.0 - dot;

            varianceSum += deviation * area;
            varianceWeightSum += area;
        }

        stats.normalVariance = varianceSum / (varianceWeightSum + EPSILON);

        return stats;
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
