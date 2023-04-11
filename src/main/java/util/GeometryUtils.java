package util;

import geometry.structure.GaiaPrimitive;
import geometry.structure.GaiaVertex;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.awt.geom.Rectangle2D;

public class GeometryUtils {
    public static Vector3d calcNormal(Vector3d p1, Vector3d p2, Vector3d p3) {
        Vector3d p2SubP1 = new Vector3d(p2).sub(p1);
        Vector3d p3SubP1 = new Vector3d(p3).sub(p1);
        Vector3d normal = new Vector3d(p2SubP1).cross(p3SubP1);
        normal.normalize();
        return normal;
    }

    public static Vector3d calcNormal(GaiaVertex v1, GaiaVertex v2, GaiaVertex v3) {
        return calcNormal(v1.getPosition(), v2.getPosition(), v3.getPosition());
    }

    public static void genNormals(GaiaVertex v1, GaiaVertex v2, GaiaVertex v3) {
        Vector3d normal = calcNormal(v1, v2, v3);
        v1.setNormal(normal);
        v2.setNormal(normal);
        v3.setNormal(normal);
    }

    private static Rectangle2D getTextureCoordinatesBoundingRectangle(GaiaPrimitive primitive) {
        Rectangle2D rect = new Rectangle2D.Double();
        for (GaiaVertex vertex : primitive.getVertices()) {
            Vector2d textureCoordinates = vertex.getTextureCoordinates();
            if (textureCoordinates.x < rect.getMinX()) {
                rect.setRect(textureCoordinates.x, rect.getMinY(), rect.getWidth(), rect.getHeight());
            }
            if (textureCoordinates.y < rect.getMinY()) {
                rect.setRect(rect.getMinX(), textureCoordinates.y, rect.getWidth(), rect.getHeight());
            }
            if (textureCoordinates.x > rect.getMaxX()) {
                rect.setRect(rect.getMinX(), rect.getMinY(), textureCoordinates.x, rect.getHeight());
            }
            if (textureCoordinates.y > rect.getMaxY()) {
                rect.setRect(rect.getMinX(), rect.getMinY(), rect.getWidth(), textureCoordinates.y);
            }
        }
        return rect;
    }
}
