package com.gaia3d.basic.geometry.entities;

import org.joml.Vector3d;

public class GaiaSegment {
    private Vector3d startPoint = null;
    private Vector3d endPoint = null;

    // constructor
    public GaiaSegment(Vector3d startPoint, Vector3d endPoint) {
        this.startPoint = startPoint;
        this.endPoint = endPoint;
    }

    public Vector3d getVector() {
        return new Vector3d(endPoint).sub(startPoint);
    }

    public double angRadToVector(Vector3d vector) {
        // returns the angle in radians between this segment and the vector
        Vector3d segment = new Vector3d(endPoint).sub(startPoint);
        return segment.angle(vector);
    }

    public double angRadToSegment(GaiaSegment segment) {
        // returns the angle in radians between this segment and the segment
        Vector3d segment1 = new Vector3d(endPoint).sub(startPoint);
        Vector3d segment2 = new Vector3d(segment.endPoint).sub(segment.startPoint);
        return segment1.angle(segment2);
    }

    public boolean check() {
        return startPoint != null && endPoint != null;
    }
}
