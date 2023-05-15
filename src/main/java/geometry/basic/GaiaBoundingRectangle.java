package geometry.basic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector2d;
import org.joml.Vector2d;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaBoundingRectangle {
    private double minX;
    private double minY;
    private double minZ;
    private double maxX;
    private double maxY;
    private double maxZ;

    //getCenterVector2d
    public Vector2d getCenter() {
        return new Vector2d((minX + maxX) / 2, (minY + maxY) / 2);
    }

    public Vector2d getVolume() {
        return new Vector2d(maxX - minX, maxY - minY);
    }

    public Vector2d getCenterCorrected() {
        return new Vector2d((minX + maxX) / 2, (minY + maxY) / 2);
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
    public void addBoundingBox(GaiaBoundingRectangle boundingBox) {
        if (boundingBox.minX < minX) {
            minX = boundingBox.minX;
        }
        if (boundingBox.minY < minY) {
            minY = boundingBox.minY;
        }
        if (boundingBox.maxX > maxX) {
            maxX = boundingBox.maxX;
        }
        if (boundingBox.maxY > maxY) {
            maxY = boundingBox.maxY;
        }
    }
}
