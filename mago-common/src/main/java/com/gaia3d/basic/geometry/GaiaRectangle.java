package com.gaia3d.basic.geometry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector2d;

import java.io.Serializable;

/**
 * GaiaRectangle is a class to store the bounding rectangle of a geometry.
 * It can be used to calculate the center and volume of the geometry.
 * It can also be used to convert the local bounding rectangle to lonlat bounding rectangle.
 * It can also be used to calculate the longest distance of the geometry.
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaRectangle implements Serializable {
    private double minX;
    private double minY;
    private double maxX;
    private double maxY;

    public GaiaRectangle(Vector2d minPoint, Vector2d maxPoint) {
        setInit(minPoint);
        addPoint(maxPoint);
    }

    public GaiaRectangle(GaiaRectangle rect) {
        minX = rect.minX;
        minY = rect.minY;
        maxX = rect.maxX;
        maxY = rect.maxY;
    }

    public Vector2d getCenter() {
        return new Vector2d((minX + maxX) / 2, (minY + maxY) / 2);
    }

    public Vector2d getVolume() {
        return new Vector2d(maxX - minX, maxY - minY);
    }

    public Vector2d getCenterCorrected() {
        return new Vector2d((minX + maxX) / 2, (minY + maxY) / 2);
    }

    public Vector2d getRange() {
        return new Vector2d(maxX - minX, maxY - minY);
    }

    public Vector2d getLeftBottomPoint() {
        return new Vector2d(minX, minY);
    }

    public Vector2d getRightTopPoint() {
        return new Vector2d(maxX, minY);
    }

    public double getBoundingArea() {
        return (maxX * maxY);
    }

    public double getArea() {
        return ((maxX - minX) * (maxY - minY));
    }

    public double getPerimeter() {
        return (maxX - minX) * 2 + (maxY - minY) * 2;
    }

    //setInit
    public void setInit(Vector2d vector2d) {
        minX = vector2d.x;
        minY = vector2d.y;
        maxX = vector2d.x;
        maxY = vector2d.y;
    }

    //addPoint
    public void addPoint(Vector2d vector2d) {
        if (vector2d.x < minX) {
            minX = vector2d.x;
        }
        if (vector2d.y < minY) {
            minY = vector2d.y;
        }
        if (vector2d.x > maxX) {
            maxX = vector2d.x;
        }
        if (vector2d.y > maxY) {
            maxY = vector2d.y;
        }
    }

    public void addPoint(double x, double y) {
        if (x < minX) {
            minX = x;
        }
        if (y < minY) {
            minY = y;
        }
        if (x > maxX) {
            maxX = x;
        }
        if (y > maxY) {
            maxY = y;
        }
    }

    public void translate(Vector2d vector2d) {
        minX += vector2d.x;
        minY += vector2d.y;
        maxX += vector2d.x;
        maxY += vector2d.y;
    }

    //addBoundingBox
    public void addBoundingRectangle(GaiaRectangle boundingRectangle) {
        if (boundingRectangle.minX < minX) {
            minX = boundingRectangle.minX;
        }
        if (boundingRectangle.minY < minY) {
            minY = boundingRectangle.minY;
        }
        if (boundingRectangle.maxX > maxX) {
            maxX = boundingRectangle.maxX;
        }
        if (boundingRectangle.maxY > maxY) {
            maxY = boundingRectangle.maxY;
        }
    }

    public double getWidth() {
        return maxX - minX;
    }

    public double getHeight() {
        return maxY - minY;
    }

    public int getWidthInt() {
        //return (int)Math.ceil(maxX) - (int)Math.floor(minX);
        return (int) Math.ceil(getWidth());
    }

    public int getHeightInt() {
        //return (int)Math.ceil(maxY) - (int)Math.floor(minY);
        return (int) Math.ceil(getHeight());
    }

    public void copyFrom(GaiaRectangle rectangle) {
        minX = rectangle.minX;
        minY = rectangle.minY;
        maxX = rectangle.maxX;
        maxY = rectangle.maxY;
    }

    public boolean intersects(GaiaRectangle compare, double error) {
        if (compare.minX > this.maxX - error) {
            return false;
        } else if (compare.maxX < this.minX + error) {
            return false;
        } else if (compare.minY > this.maxY - error) {
            return false;
        } else return !(compare.maxY < this.minY + error);
    }

    public boolean intersectsInXAxis(GaiaRectangle compare) {
        return (minX < compare.maxX && maxX > compare.minX);
    }

    public boolean intersectsInYAxis(GaiaRectangle compare) {
        return (minY < compare.maxY && maxY > compare.minY);
    }

    public boolean intersectsInSomeAxis(GaiaRectangle compare) {
        // check axis x
        boolean intersectsInX = false;
        if (compare.minX > this.maxX) {
            intersectsInX = false;
        } else if (compare.maxX < this.minX) {
            intersectsInX = false;
        } else {
            return true;
        }

        // check axis y
        boolean intersectsInY = false;
        if (compare.minY > this.maxY) {
            intersectsInY = false;
        } else if (compare.maxY < this.minY) {
            intersectsInY = false;
        } else {
            return true;
        }

        return false;
    }

    public GaiaRectangle clone() {
        return new GaiaRectangle(minX, minY, maxX, maxY);
    }

    public void setSize(double minX, double minY, double maxX, double maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }
}
