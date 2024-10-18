package com.gaia3d.basic.geometry.entities;

import org.joml.Vector3d;

public class GaiaPlane {
    private double a, b, c, d;

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


}
