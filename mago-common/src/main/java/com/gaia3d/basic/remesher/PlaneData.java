package com.gaia3d.basic.remesher;

import org.joml.Vector3d;

public class PlaneData {

    public final Vector3d normal = new Vector3d();
    public final Vector3d centroid = new Vector3d();

    public double d = 0.0;
    public double area = 0.0;

    public boolean isValid() {
        return area > 1e-12 && normal.lengthSquared() > 1e-20;
    }

    public double signedDistanceToPoint(Vector3d p) {
        return normal.dot(p) + d;
    }

    public double distanceToPoint(Vector3d p) {
        return Math.abs(signedDistanceToPoint(p));
    }
}