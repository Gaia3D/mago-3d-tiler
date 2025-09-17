package com.gaia3d.basic.geometry.entities;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Slf4j
@Getter
@Setter

public class GaiaPlane {
    private final double a;
    private final double b;
    private final double c;
    private final double d;

    public GaiaPlane() {
        this.a = 0;
        this.b = 0;
        this.c = 0;
        this.d = 0;
    }

    public GaiaPlane(double a, double b, double c, double d) {
        this.a = a;
        this.b = b;
        this.c = c;
        this.d = d;
    }

    public GaiaPlane(Vector3d position, Vector3d normal) {
        this.a = normal.x;
        this.b = normal.y;
        this.c = normal.z;
        this.d = -position.dot(normal);
    }

    public double distanceToPoint(Vector3d point) {
        return a * point.x + b * point.y + c * point.z + d;
    }

    public Vector3d getNormal() {
        return new Vector3d(a, b, c);
    }

    public Vector3d intersectionSegment(GaiaSegment aaSegment) {
        Vector3d normal = getNormal();
        Vector3d start = aaSegment.getStartPoint();
        Vector3d end = aaSegment.getEndPoint();
        double eps = 1e-9;

        Vector3d dir = new Vector3d(end).sub(start); // direction vector of the segment

        double denom = normal.dot(dir);
        if (Math.abs(denom) < eps) {
            // Segment is parallel to the plane
            return null;
        }

        double t = (d - normal.dot(start)) / denom;

        if (t < 0.0 || t > 1.0) {
            // The intersection point is outside the segment
            return null;
        }

        return new Vector3d(start).fma(t, dir);
    }
}
