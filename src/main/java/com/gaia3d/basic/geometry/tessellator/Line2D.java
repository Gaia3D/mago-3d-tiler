package com.gaia3d.basic.geometry.tessellator;

import org.joml.Vector2d;

public class Line2D {
    // (x,y) = (x0,y0) + lambda * (u, v);
    public Vector2d point = new Vector2d();
    public Vector2d direction = new Vector2d();

    public Line2D(Vector2d point, Vector2d direction) {
        this.point = point;
        this.direction = direction;
    }

    public void setBy2Points(Vector2d point1, Vector2d point2) {
        this.point = point1;
        this.direction = new Vector2d(point2.x - point1.x, point2.y - point1.y);
        this.direction.normalize();
    }

    public boolean isParallel(Line2D line) {
        //double error = 1.0e-10;
        double error = 1.0e-7; // works better with this value.***
        return Math.abs(this.direction.x * line.direction.y - this.direction.y * line.direction.x) < error;
    }

    public boolean pointBelongsToLine(Vector2d point, double error) {

        double error2 = error * error;
        double dx = point.x - this.point.x;
        double dy = point.y - this.point.y;
        double dotProduct = dx * this.direction.x + dy * this.direction.y;
        double squareDistance = dx * dx + dy * dy - dotProduct * dotProduct;
        return squareDistance < error2;
    }

    public boolean intersectionWithLine(Line2D line, Vector2d intersectionPoint, double error) {
        if (this.isParallel(line)) {
            return false;
        }
        double det = this.direction.x * line.direction.y - this.direction.y * line.direction.x;
        double lambda = (line.direction.y * (line.point.x - this.point.x) - line.direction.x * (line.point.y - this.point.y)) / det;
        if (Math.abs(lambda) < error) {
            return false;
        }
        intersectionPoint.x = this.point.x + lambda * this.direction.x;
        intersectionPoint.y = this.point.y + lambda * this.direction.y;
        return true;
    }
}
