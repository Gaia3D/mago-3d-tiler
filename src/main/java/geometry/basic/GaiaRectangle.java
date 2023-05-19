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
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;

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
}
