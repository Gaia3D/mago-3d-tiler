package com.gaia3d.util;

import com.gaia3d.basic.geometry.GaiaRectangle;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;

@Slf4j
public class VectorUtils {
    public static double cross(Vector2d v1, Vector2d v2, Vector2d v3) {
        Vector2d v1v2 = v1.sub(v2, new Vector2d());
        v1v2.normalize();
        Vector2d v2v3 = v2.sub(v3, new Vector2d());
        v2v3.normalize();
        return cross(v1v2, v2v3);
    }

    public static double cross(Vector2d a, Vector2d b) {
        return (a.x * b.y) - (a.y * b.x);
    }

    public static boolean isIntersection(Vector2d a, Vector2d b, Vector2d u, Vector2d v) {
        GaiaRectangle rect1 = new GaiaRectangle(a, b);
        GaiaRectangle rect2 = new GaiaRectangle(u, v);
        if (!rect1.intersects(rect2, 0.0)) {
            // Intersection check with bounding box
            return false;
        } else if (a.equals(u) && b.equals(v) || a.equals(v) && b.equals(u)){
            // Same line case;
            return true;
        } else if (a.equals(u) || a.equals(v) || b.equals(u) || b.equals(v)) {
            // Same point case;
            return false;
        }


        double cross1 = cross(a, b, u);
        double cross2 = cross(a, b, v);
        if (cross1 == 0 && cross2 == 0) {
            return true;
        }
        boolean isIntersectA = cross1 * cross2 < 0;

        double cross3 = cross(u, v, a);
        double cross4 = cross(u, v, b);
        if (cross3 == 0 && cross4 == 0) {
            return true;
        }
        boolean isIntersectB = cross3 * cross4 < 0;
        return isIntersectA == isIntersectB;
    }

    public static boolean isIntersection(Vector2d v1, Vector2d v2, Vector2d v3) {
        if (v1.equals(v3) || v2.equals(v3)) {
            return false;
        }
        double cross1 = cross(v1, v2, v3);
        return cross1 == 0;
    }

    public static double calcAngle(Vector2d a, Vector2d b, Vector2d c) {
        Vector2d v1 = new Vector2d();
        Vector2d v2 = new Vector2d();
        b.sub(a, v1);
        c.sub(b, v2);
        v1.normalize();
        v2.normalize();
        return Math.toDegrees(v1.angle(v2));
    }
}