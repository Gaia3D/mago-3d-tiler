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

    public Vector3d intersectionAASegment(GaiaSegment aaSegment, int axis) {
        if(axis == 0) { // X-axis
            // ax + by + cz + d = 0
            // x = (-d -by - cz) / a;
            double segStartX = aaSegment.getStartPoint().x;
            double segEndX = aaSegment.getEndPoint().x;
            double segStartY = aaSegment.getStartPoint().y;
            double segStartZ = aaSegment.getStartPoint().z;
            double segMinX = Math.min(segStartX, segEndX);
            double segMaxX = Math.max(segStartX, segEndX);
            double x = (-d - b * segStartY - c * segStartZ) / a;
            // Now check if the calculated x is within the segment bounds
            if (x < segMinX || x > segMaxX) {
                return null; // Intersection is outside the segment bounds
            }
            return new Vector3d(x, segStartY, segStartZ); // Return the
        } else if(axis == 1) { // Y-axis
            // ax + by + cz + d = 0
            // y = (-d - ax - cz) / b;
            double segStartY = aaSegment.getStartPoint().y;
            double segEndY = aaSegment.getEndPoint().y;
            double segStartX = aaSegment.getStartPoint().x;
            double segStartZ = aaSegment.getStartPoint().z;
            double segMinY = Math.min(segStartY, segEndY);
            double segMaxY = Math.max(segStartY, segEndY);
            double y = (-d - a * segStartX - c * segStartZ) / b;
            // Now check if the calculated y is within the segment bounds
            if (y < segMinY || y > segMaxY) {
                return null; // Intersection is outside the segment bounds
            }
            return new Vector3d(segStartX, y, segStartZ); // Return the intersection point on the Y-axis
        } else if(axis == 2) { // Z-axis
            // ax + by + cz + d = 0
            // z = (-d - ax - by) / c;
            double segStartZ = aaSegment.getStartPoint().z;
            double segEndZ = aaSegment.getEndPoint().z;
            double segStartX = aaSegment.getStartPoint().x;
            double segStartY = aaSegment.getStartPoint().y;
            double segMinZ = Math.min(segStartZ, segEndZ);
            double segMaxZ = Math.max(segStartZ, segEndZ);
            double z = (-d - a * segStartX - b * segStartY) / c;
            // Now check if the calculated z is within the segment bounds
            if (z < segMinZ || z > segMaxZ) {
                return null; // Intersection is outside the segment bounds
            }
            return new Vector3d(segStartX, segStartY, z); // Return the intersection point on the Z-axis
        }
        return null;
    }
}
