package com.gaia3d.basic.geometry.tessellator;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2d;
import org.joml.Vector3d;

@Getter
@Setter
public class Point2DTess {
    public Vector2d point;
    public Vector3d parentPoint;
    public Vector2d parentPoint2d;
    public int idxInList = -1;

    public Point2DTess(Vector2d point, Vector3d parentPoint3d, Vector2d parentPoint2d) {
        this.point = point;
        this.parentPoint = parentPoint3d;
        this.parentPoint2d = parentPoint2d;
    }

    public double squareDistanceTo(Point2DTess point) {
        double dx = this.point.x - point.point.x;
        double dy = this.point.y - point.point.y;
        return dx * dx + dy * dy;
    }

    public double distanceTo(Point2DTess point) {
        return Math.sqrt(squareDistanceTo(point));
    }
}
