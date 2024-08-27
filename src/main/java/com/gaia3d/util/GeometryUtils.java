package com.gaia3d.util;

import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.structure.GaiaFace;
import com.gaia3d.basic.structure.GaiaVertex;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.List;

/**
 * GeometryUtils
 *
 * @author znkim
 * @since 1.0.0
 */
public class GeometryUtils {
    public static boolean isIdentity(float[] matrix) {
        return matrix[0] == 1 && matrix[1] == 0 && matrix[2] == 0 && matrix[3] == 0 && matrix[4] == 0 && matrix[5] == 1 && matrix[6] == 0 && matrix[7] == 0 && matrix[8] == 0 && matrix[9] == 0 && matrix[10] == 1 && matrix[11] == 0 && matrix[12] == 0 && matrix[13] == 0 && matrix[14] == 0 && matrix[15] == 1;
    }

    public static GaiaRectangle getTexCoordsBoundingRectangle(List<GaiaVertex> vertices, GaiaRectangle boundingRectangle) {
        if (boundingRectangle == null) {
            boundingRectangle = new GaiaRectangle();
        }

        boolean isFirst = true;
        for (GaiaVertex vertex : vertices) {
            if (isFirst) {
                boundingRectangle.setInit(vertex.getTexcoords());
                isFirst = false;
            } else {
                boundingRectangle.addPoint(vertex.getTexcoords());
            }
        }

        return boundingRectangle;
    }

    public static GaiaRectangle getTexCoordsBoundingRectangleOfFaces(List<GaiaFace> faces, List<GaiaVertex> vertices, GaiaRectangle boundingRectangle) {
        if (boundingRectangle == null) {
            boundingRectangle = new GaiaRectangle();
        }

        boolean is1rst = true;
        for (GaiaFace face : faces) {
            int[] indices = face.getIndices();
            for (int index : indices) {
                GaiaVertex vertex = vertices.get(index);
                if (is1rst) {
                    boundingRectangle.setInit(vertex.getTexcoords());
                    is1rst = false;
                } else {
                    boundingRectangle.addPoint(vertex.getTexcoords());
                }
            }
        }
        return boundingRectangle;
    }

    public static double getTriangleArea(GaiaVertex vertexA, GaiaVertex vertexB, GaiaVertex vertexC) {
        double area = 0.0;
        Vector3d vectorA = vertexA.getPosition();
        Vector3d vectorB = vertexB.getPosition();
        Vector3d vectorC = vertexC.getPosition();

        Vector3d vectorAB = new Vector3d();
        vectorAB.x = vectorB.x - vectorA.x;
        vectorAB.y = vectorB.y - vectorA.y;
        vectorAB.z = vectorB.z - vectorA.z;

        Vector3d vectorAC = new Vector3d();
        vectorAC.x = vectorC.x - vectorA.x;
        vectorAC.y = vectorC.y - vectorA.y;
        vectorAC.z = vectorC.z - vectorA.z;

        Vector3d crossProduct = new Vector3d();
        vectorAB.cross(vectorAC, crossProduct);

        area = crossProduct.length() / 2.0;
        return area;
    }

    public static boolean areAproxEqualsPoints2d(Vector2d pointA, Vector2d pointB, double epsilon) {
        return Math.abs(pointA.x - pointB.x) < epsilon && Math.abs(pointA.y - pointB.y) < epsilon;
    }
}
