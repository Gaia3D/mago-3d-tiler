package com.gaia3d.basic.remesher;

import com.gaia3d.basic.model.GaiaFace;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j

public class PlaneCluster {

    private final List<GaiaFace> faces = new ArrayList<>();
    private final Set<Integer> vertexIndices = new HashSet<>();

    private final Vector3d normalSum = new Vector3d();
    private final Vector3d centroidSum = new Vector3d();

    private final Vector3d normal = new Vector3d();
    private final Vector3d centroid = new Vector3d();

    private double d = 0.0;
    private double areaSum = 0.0;

    private double minTriangleArea = Double.POSITIVE_INFINITY;
    private double maxTriangleArea = 0.0;
    private double averageTriangleArea = 0.0;

    public void addFace(GaiaFace face, PlaneData plane) {
        if (face == null || plane == null || !plane.isValid()) {
            return;
        }

        faces.add(face);

        int[] indices = face.getIndices();
        if (indices != null) {
            for (int idx : indices) {
                vertexIndices.add(idx);
            }
        }

        Vector3d faceNormal = new Vector3d(plane.normal);

        // Importantísimo:
        // si la normal de la cara apunta en sentido contrario,
        // la invertimos antes de sumarla.
        if (normalSum.lengthSquared() > 1e-20) {
            if (normalSum.dot(faceNormal) < 0.0) {
                faceNormal.negate();
            }
        }

        Vector3d weightedNormal = new Vector3d(faceNormal);
        weightedNormal.mul(plane.area);
        normalSum.add(weightedNormal);

        Vector3d weightedCentroid = new Vector3d(plane.centroid);
        weightedCentroid.mul(plane.area);
        centroidSum.add(weightedCentroid);

        areaSum += plane.area;

        minTriangleArea = Math.min(minTriangleArea, plane.area);
        maxTriangleArea = Math.max(maxTriangleArea, plane.area);
    }

    public void finalizeCluster() {
        if (faces.isEmpty()) {
            return;
        }

        if (areaSum <= 1e-12) {
            return;
        }

        normal.set(normalSum);

        if (normal.lengthSquared() <= 1e-20) {
            return;
        }

        normal.normalize();

        centroid.set(centroidSum);
        centroid.mul(1.0 / areaSum);

        d = -normal.dot(centroid);

        averageTriangleArea = areaSum / faces.size();
    }

    public boolean isValid() {
        return !faces.isEmpty()
                && areaSum > 1e-12
                && normal.lengthSquared() > 1e-20;
    }

    public double signedDistanceToPoint(Vector3d p) {
        return normal.dot(p) + d;
    }

    public double distanceToPoint(Vector3d p) {
        return Math.abs(signedDistanceToPoint(p));
    }

    public Vector3d projectPoint(Vector3d p) {
        Vector3d result = new Vector3d(p);

        double signedDistance = signedDistanceToPoint(p);

        Vector3d correction = new Vector3d(normal);
        correction.mul(signedDistance);

        result.sub(correction);

        return result;
    }

    public void projectPointInPlace(Vector3d p) {
        double signedDistance = signedDistanceToPoint(p);

        Vector3d correction = new Vector3d(normal);
        correction.mul(signedDistance);

        p.sub(correction);
    }

    public void projectPointSoftInPlace(Vector3d p, double strength) {
        strength = Math.max(0.0, Math.min(1.0, strength));

        Vector3d projected = projectPoint(p);
        p.lerp(projected, strength);
    }

    public List<GaiaFace> getFaces() {
        return faces;
    }

    public Set<Integer> getVertexIndices() {
        return vertexIndices;
    }

    public Vector3d getNormal() {
        return normal;
    }

    public Vector3d getCentroid() {
        return centroid;
    }

    public double getD() {
        return d;
    }

    public double getAreaSum() {
        return areaSum;
    }

    public double getMinTriangleArea() {
        return minTriangleArea;
    }

    public double getMaxTriangleArea() {
        return maxTriangleArea;
    }

    public double getAverageTriangleArea() {
        return averageTriangleArea;
    }
}
