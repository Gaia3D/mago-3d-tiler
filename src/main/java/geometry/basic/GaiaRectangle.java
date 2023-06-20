package geometry.basic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector2d;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaRectangle {
    private double minX;
    private double minY;
    private double maxX;
    private double maxY;

    public GaiaRectangle(Vector2d minPoint, Vector2d maxPoint) {
        setInit(minPoint);
        addPoint(maxPoint);
    }

    /*public Vector2d getCenter() {
        return new Vector2d((minX + maxX) / 2, (minY + maxY) / 2);
    }*/

    /*public Vector2d getVolume() {
        return new Vector2d(maxX - minX, maxY - minY);
    }*/

    /*public Vector2d getCenterCorrected() {
        return new Vector2d((minX + maxX) / 2, (minY + maxY) / 2);
    }*/

    public Vector2d getRange() {
        return new Vector2d(maxX - minX, maxY - minY);
    }

    public Vector2d getLeftBottomPoint() {
        return new Vector2d(minX, maxY);
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

    // interects
    /*public boolean intersects(GaiaRectangle rectangle) {
        return (minX < rectangle.maxX
                && maxX > rectangle.minX
                && minY < rectangle.maxY
                && maxY > rectangle.minY);
    }*/

    public boolean intersects(GaiaRectangle compare, double error) {
        if (compare.minX > this.maxX - error) {
            return false;
        } else if (compare.maxX < this.minX + error) {
            return false;
        } else if (compare.minY > this.maxY - error) {
            return false;
        } else if (compare.maxY < this.minY + error) {
            return false;
        }
        return true;
    }
}
