package com.gaia3d.util;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.geometry.octree.GaiaFaceData;
import com.gaia3d.basic.geometry.octree.GaiaOctree;
import com.gaia3d.basic.halfedge.PlaneType;
import com.gaia3d.basic.model.*;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;

import java.util.ArrayList;
import java.util.List;

/**
 * GeometryUtils
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

    public static boolean areAproxEqualsPoints3d(Vector3d pointA, Vector3d pointB, double epsilon) {
        return Math.abs(pointA.x - pointB.x) < epsilon && Math.abs(pointA.y - pointB.y) < epsilon && Math.abs(pointA.z - pointB.z) < epsilon;
    }

    public static int getNextIdx(int idx, int pointsCount) {
        return (idx + 1) % pointsCount;
    }

    public static int getPrevIdx(int idx, int pointsCount) {
        return (idx + pointsCount - 1) % pointsCount;
    }

    public static GaiaPrimitive getPrimitiveFromBoundingBox(GaiaBoundingBox bbox, boolean left, boolean right, boolean front, boolean rear, boolean bottom, boolean top) {
        GaiaPrimitive resultPrimitive = new GaiaPrimitive();

        // make 6 GaiaSurface. Each surface has 2 gaiaFaces.
        double minX = bbox.getMinX();
        double minY = bbox.getMinY();
        double minZ = bbox.getMinZ();
        double maxX = bbox.getMaxX();
        double maxY = bbox.getMaxY();
        double maxZ = bbox.getMaxZ();

        // 24 vertices.

        //                           3--------2
        //                          /        /     <- top
        //                         /        /
        //                        0--------1
        //
        //
        //                             rear
        //                  2        3--------2          2
        //                 /|        |        |         /|
        //                / |        |        |        / |
        //     left ->   3  |     3--------2  |       3  |    <- right
        //               |  1     |  0-----|--1       |  1
        //               | /      |        |          | /
        //               0        0--------1          0
        //                          front
        //
        //
        //                          3--------2
        //                          /        /
        //                         /        /   <- bottom
        //                        0--------1

        if (left) {
            GaiaPrimitive leftPrimitive = new GaiaPrimitive();

            GaiaVertex vertex0 = new GaiaVertex();
            // Left
            Vector3d normalLeft = new Vector3d(-1, 0, 0);
            vertex0.setPosition(new Vector3d(minX, minY, minZ));
            vertex0.setNormal(normalLeft);

            GaiaVertex vertex1 = new GaiaVertex();
            vertex1.setPosition(new Vector3d(minX, maxY, minZ));
            vertex1.setNormal(normalLeft);

            GaiaVertex vertex2 = new GaiaVertex();
            vertex2.setPosition(new Vector3d(minX, maxY, maxZ));
            vertex2.setNormal(normalLeft);

            GaiaVertex vertex3 = new GaiaVertex();
            vertex3.setPosition(new Vector3d(minX, minY, maxZ));
            vertex3.setNormal(normalLeft);

            leftPrimitive.getVertices().add(vertex0);
            leftPrimitive.getVertices().add(vertex1);
            leftPrimitive.getVertices().add(vertex2);
            leftPrimitive.getVertices().add(vertex3);

            // LeftSurface.
            GaiaSurface leftSurface = new GaiaSurface();
            // 0, 3, 2, 1. The normal is (-1, 0, 0).

            // Face0 (0, 3, 2).
            GaiaFace face10 = new GaiaFace();
            face10.setIndices(new int[]{0, 3, 2});
            leftSurface.getFaces().add(face10);

            // Face1 (0, 2, 1).
            GaiaFace face11 = new GaiaFace();
            face11.setIndices(new int[]{0, 2, 1});
            leftSurface.getFaces().add(face11);

            leftPrimitive.getSurfaces().add(leftSurface);
            GaiaPrimitiveUtils.mergePrimitives(resultPrimitive, leftPrimitive);
        }

        if (right) {
            GaiaPrimitive rightPrimitive = new GaiaPrimitive();

            // Right.
            Vector3d normalRight = new Vector3d(1, 0, 0);
            GaiaVertex vertex0 = new GaiaVertex(); // coincident with vertex5
            vertex0.setPosition(new Vector3d(maxX, minY, minZ));
            vertex0.setNormal(normalRight);

            GaiaVertex vertex1 = new GaiaVertex(); // coincident with vertex9
            vertex1.setPosition(new Vector3d(maxX, maxY, minZ));
            vertex1.setNormal(normalRight);

            GaiaVertex vertex2 = new GaiaVertex(); // coincident with vertex10
            vertex2.setPosition(new Vector3d(maxX, maxY, maxZ));
            vertex2.setNormal(normalRight);

            GaiaVertex vertex3 = new GaiaVertex(); // coincident with vertex6
            vertex3.setPosition(new Vector3d(maxX, minY, maxZ));
            vertex3.setNormal(normalRight);

            rightPrimitive.getVertices().add(vertex0);
            rightPrimitive.getVertices().add(vertex1);
            rightPrimitive.getVertices().add(vertex2);
            rightPrimitive.getVertices().add(vertex3);

            // RightSurface
            GaiaSurface rightSurface = new GaiaSurface();
            // 0, 1, 2, 3. The normal is (1, 0, 0)

            // Face0 (0, 1, 2)
            GaiaFace face8 = new GaiaFace();
            face8.setIndices(new int[]{0, 1, 2});
            rightSurface.getFaces().add(face8);

            // Face1 (0, 2, 3)
            GaiaFace face9 = new GaiaFace();
            face9.setIndices(new int[]{0, 2, 3});
            rightSurface.getFaces().add(face9);

            rightPrimitive.getSurfaces().add(rightSurface);
            GaiaPrimitiveUtils.mergePrimitives(resultPrimitive, rightPrimitive);
        }

        if (front) {
            GaiaPrimitive frontPrimitive = new GaiaPrimitive();

            Vector3d normalFront = new Vector3d(0, -1, 0);
            GaiaVertex vertex0 = new GaiaVertex(); // coincident with vertex0
            vertex0.setPosition(new Vector3d(minX, minY, minZ));
            vertex0.setNormal(normalFront);

            GaiaVertex vertex1 = new GaiaVertex(); // coincident with vertex1
            vertex1.setPosition(new Vector3d(maxX, minY, minZ));
            vertex1.setNormal(normalFront);

            GaiaVertex vertex2 = new GaiaVertex();
            vertex2.setPosition(new Vector3d(maxX, minY, maxZ));
            vertex2.setNormal(normalFront);

            GaiaVertex vertex3 = new GaiaVertex();
            vertex3.setPosition(new Vector3d(minX, minY, maxZ));
            vertex3.setNormal(normalFront);

            frontPrimitive.getVertices().add(vertex0);
            frontPrimitive.getVertices().add(vertex1);
            frontPrimitive.getVertices().add(vertex2);
            frontPrimitive.getVertices().add(vertex3);

            // FrontSurface
            GaiaSurface frontSurface = new GaiaSurface();
            // 0, 1, 2, 3. The normal is (0, -1, 0)

            // Face0 (0, 1, 2)
            GaiaFace face4 = new GaiaFace();
            face4.setIndices(new int[]{0, 1, 2});
            frontSurface.getFaces().add(face4);

            // Face1 (0, 2, 3)
            GaiaFace face5 = new GaiaFace();
            face5.setIndices(new int[]{0, 2, 3});
            frontSurface.getFaces().add(face5);

            frontPrimitive.getSurfaces().add(frontSurface);
            GaiaPrimitiveUtils.mergePrimitives(resultPrimitive, frontPrimitive);
        }

        if (rear) {
            GaiaPrimitive rearPrimitive = new GaiaPrimitive();

            // Rear
            Vector3d normalRear = new Vector3d(0, 1, 0);
            GaiaVertex vertex0 = new GaiaVertex(); // coincident with vertex3
            vertex0.setPosition(new Vector3d(minX, maxY, minZ));
            vertex0.setNormal(normalRear);

            GaiaVertex vertex1 = new GaiaVertex(); // coincident with vertex2
            vertex1.setPosition(new Vector3d(maxX, maxY, minZ));
            vertex1.setNormal(normalRear);

            GaiaVertex vertex2 = new GaiaVertex();
            vertex2.setPosition(new Vector3d(maxX, maxY, maxZ));
            vertex2.setNormal(normalRear);

            GaiaVertex vertex3 = new GaiaVertex();
            vertex3.setPosition(new Vector3d(minX, maxY, maxZ));
            vertex3.setNormal(normalRear);

            rearPrimitive.getVertices().add(vertex0);
            rearPrimitive.getVertices().add(vertex1);
            rearPrimitive.getVertices().add(vertex2);
            rearPrimitive.getVertices().add(vertex3);

            // RearSurface
            GaiaSurface backSurface = new GaiaSurface();
            // 0, 3, 2, 1. The normal is (0, 1, 0)

            // Face0 (0, 3, 2)
            GaiaFace face6 = new GaiaFace();
            face6.setIndices(new int[]{0, 3, 2});
            backSurface.getFaces().add(face6);

            // Face1 (0, 2, 1)
            GaiaFace face7 = new GaiaFace();
            face7.setIndices(new int[]{0, 2, 1});
            backSurface.getFaces().add(face7);

            rearPrimitive.getSurfaces().add(backSurface);
            GaiaPrimitiveUtils.mergePrimitives(resultPrimitive, rearPrimitive);
        }

        if (bottom) {
            GaiaPrimitive bottomPrimitive = new GaiaPrimitive();
            // Bottom
            GaiaVertex vertex0 = new GaiaVertex();
            Vector3d normalBottom = new Vector3d(0, 0, -1);
            vertex0.setPosition(new Vector3d(minX, minY, minZ));
            vertex0.setNormal(normalBottom);

            GaiaVertex vertex1 = new GaiaVertex();
            vertex1.setPosition(new Vector3d(maxX, minY, minZ));
            vertex1.setNormal(normalBottom);

            GaiaVertex vertex2 = new GaiaVertex();
            vertex2.setPosition(new Vector3d(maxX, maxY, minZ));
            vertex2.setNormal(normalBottom);

            GaiaVertex vertex3 = new GaiaVertex();
            vertex3.setPosition(new Vector3d(minX, maxY, minZ));
            vertex3.setNormal(normalBottom);

            bottomPrimitive.getVertices().add(vertex0);
            bottomPrimitive.getVertices().add(vertex1);
            bottomPrimitive.getVertices().add(vertex2);
            bottomPrimitive.getVertices().add(vertex3);

            // BottomSurface
            GaiaSurface bottomSurface = new GaiaSurface();
            // 0, 3, 2, 1. The normal is (0, 0, -1)
            // Face0 (0, 2, 1)
            GaiaFace face0 = new GaiaFace();
            face0.setIndices(new int[]{0, 2, 1});
            bottomSurface.getFaces().add(face0);

            // Face1 (0, 3, 2)
            GaiaFace face1 = new GaiaFace();
            face1.setIndices(new int[]{0, 3, 2});
            bottomSurface.getFaces().add(face1);

            bottomPrimitive.getSurfaces().add(bottomSurface);
            GaiaPrimitiveUtils.mergePrimitives(resultPrimitive, bottomPrimitive);
        }

        if (top) {
            GaiaPrimitive topPrimitive = new GaiaPrimitive();
            // Top
            Vector3d normalTop = new Vector3d(0, 0, 1);
            GaiaVertex vertex0 = new GaiaVertex(); // coincident with vertex7
            vertex0.setPosition(new Vector3d(minX, minY, maxZ));
            vertex0.setNormal(normalTop);

            GaiaVertex vertex1 = new GaiaVertex(); // coincident with vertex6
            vertex1.setPosition(new Vector3d(maxX, minY, maxZ));
            vertex1.setNormal(normalTop);

            GaiaVertex vertex2 = new GaiaVertex(); // coincident with vertex10
            vertex2.setPosition(new Vector3d(maxX, maxY, maxZ));
            vertex2.setNormal(normalTop);

            GaiaVertex vertex3 = new GaiaVertex(); // coincident with vertex11
            vertex3.setPosition(new Vector3d(minX, maxY, maxZ));
            vertex3.setNormal(normalTop);

            topPrimitive.getVertices().add(vertex0);
            topPrimitive.getVertices().add(vertex1);
            topPrimitive.getVertices().add(vertex2);
            topPrimitive.getVertices().add(vertex3);

            // TopSurface
            GaiaSurface topSurface = new GaiaSurface();
            // 0, 1, 2, 3. The normal is (0, 0, 1)
            //Vector3d normal = new Vector3d(0, 0, 1);
            // Face0 (0, 1, 2)
            GaiaFace face2 = new GaiaFace();
            face2.setIndices(new int[]{0, 1, 2});
            topSurface.getFaces().add(face2);

            // Face1 (0, 2, 3)
            GaiaFace face3 = new GaiaFace();
            face3.setIndices(new int[]{0, 2, 3});
            topSurface.getFaces().add(face3);

            topPrimitive.getSurfaces().add(topSurface);
            GaiaPrimitiveUtils.mergePrimitives(resultPrimitive, topPrimitive);
        }

        return resultPrimitive;
    }

    public static GaiaPrimitive getPrimitiveFromBoundingBox(GaiaBoundingBox bbox) {
        //GaiaPrimitive resultPrimitive = getPrimitiveFromBoundingBox(bbox, true, true, true, true, true, true);
        GaiaPrimitive resultPrimitive = new GaiaPrimitive();

        // make 6 GaiaSurface. Each surface has 2 gaiaFaces
        double minX = bbox.getMinX();
        double minY = bbox.getMinY();
        double minZ = bbox.getMinZ();
        double maxX = bbox.getMaxX();
        double maxY = bbox.getMaxY();
        double maxZ = bbox.getMaxZ();

        // 24 vertices

        //                          23--------22
        //                          /        /     <- top
        //                         /        /
        //                       20--------21
        //
        //
        //                             rear
        //                  14      11--------10         18
        //                 /|        |        |         /|
        //                / |        |        |        / |
        //     left ->  15  |     7--------6  |      19  |    <- right
        //               |  13    |  8-----|--9       |  17
        //               | /      |        |          | /
        //               12       4--------5          16
        //                          front
        //
        //
        //                          3--------2
        //                          /        /
        //                         /        /   <- bottom
        //                        0--------1


        GaiaVertex vertex0 = new GaiaVertex();
        // Bottom
        Vector3d normalBottom = new Vector3d(0, 0, -1);
        vertex0.setPosition(new Vector3d(minX, minY, minZ));
        vertex0.setNormal(normalBottom);

        GaiaVertex vertex1 = new GaiaVertex();
        vertex1.setPosition(new Vector3d(maxX, minY, minZ));
        vertex1.setNormal(normalBottom);

        GaiaVertex vertex2 = new GaiaVertex();
        vertex2.setPosition(new Vector3d(maxX, maxY, minZ));
        vertex2.setNormal(normalBottom);

        GaiaVertex vertex3 = new GaiaVertex();
        vertex3.setPosition(new Vector3d(minX, maxY, minZ));
        vertex3.setNormal(normalBottom);

        // Front
        Vector3d normalFront = new Vector3d(0, -1, 0);
        GaiaVertex vertex4 = new GaiaVertex(); // coincident with vertex0
        vertex4.setPosition(new Vector3d(minX, minY, minZ));
        vertex4.setNormal(normalFront);

        GaiaVertex vertex5 = new GaiaVertex(); // coincident with vertex1
        vertex5.setPosition(new Vector3d(maxX, minY, minZ));
        vertex5.setNormal(normalFront);

        GaiaVertex vertex6 = new GaiaVertex();
        vertex6.setPosition(new Vector3d(maxX, minY, maxZ));
        vertex6.setNormal(normalFront);

        GaiaVertex vertex7 = new GaiaVertex();
        vertex7.setPosition(new Vector3d(minX, minY, maxZ));
        vertex7.setNormal(normalFront);

        // Rear
        Vector3d normalRear = new Vector3d(0, 1, 0);
        GaiaVertex vertex8 = new GaiaVertex(); // coincident with vertex3
        vertex8.setPosition(new Vector3d(minX, maxY, minZ));
        vertex8.setNormal(normalRear);

        GaiaVertex vertex9 = new GaiaVertex(); // coincident with vertex2
        vertex9.setPosition(new Vector3d(maxX, maxY, minZ));
        vertex9.setNormal(normalRear);

        GaiaVertex vertex10 = new GaiaVertex();
        vertex10.setPosition(new Vector3d(maxX, maxY, maxZ));
        vertex10.setNormal(normalRear);

        GaiaVertex vertex11 = new GaiaVertex();
        vertex11.setPosition(new Vector3d(minX, maxY, maxZ));
        vertex11.setNormal(normalRear);

        // Left
        Vector3d normalLeft = new Vector3d(-1, 0, 0);
        GaiaVertex vertex12 = new GaiaVertex(); // coincident with vertex0
        vertex12.setPosition(new Vector3d(minX, minY, minZ));
        vertex12.setNormal(normalLeft);

        GaiaVertex vertex13 = new GaiaVertex(); // coincident with vertex3
        vertex13.setPosition(new Vector3d(minX, maxY, minZ));
        vertex13.setNormal(normalLeft);

        GaiaVertex vertex14 = new GaiaVertex(); // coincident with vertex11
        vertex14.setPosition(new Vector3d(minX, maxY, maxZ));
        vertex14.setNormal(normalLeft);

        GaiaVertex vertex15 = new GaiaVertex(); // coincident with vertex7
        vertex15.setPosition(new Vector3d(minX, minY, maxZ));
        vertex15.setNormal(normalLeft);

        // Right
        Vector3d normalRight = new Vector3d(1, 0, 0);
        GaiaVertex vertex16 = new GaiaVertex(); // coincident with vertex5
        vertex16.setPosition(new Vector3d(maxX, minY, minZ));
        vertex16.setNormal(normalRight);

        GaiaVertex vertex17 = new GaiaVertex(); // coincident with vertex9
        vertex17.setPosition(new Vector3d(maxX, maxY, minZ));
        vertex17.setNormal(normalRight);

        GaiaVertex vertex18 = new GaiaVertex(); // coincident with vertex10
        vertex18.setPosition(new Vector3d(maxX, maxY, maxZ));
        vertex18.setNormal(normalRight);

        GaiaVertex vertex19 = new GaiaVertex(); // coincident with vertex6
        vertex19.setPosition(new Vector3d(maxX, minY, maxZ));
        vertex19.setNormal(normalRight);

        // Top
        Vector3d normalTop = new Vector3d(0, 0, 1);
        GaiaVertex vertex20 = new GaiaVertex(); // coincident with vertex7
        vertex20.setPosition(new Vector3d(minX, minY, maxZ));
        vertex20.setNormal(normalTop);

        GaiaVertex vertex21 = new GaiaVertex(); // coincident with vertex6
        vertex21.setPosition(new Vector3d(maxX, minY, maxZ));
        vertex21.setNormal(normalTop);

        GaiaVertex vertex22 = new GaiaVertex(); // coincident with vertex10
        vertex22.setPosition(new Vector3d(maxX, maxY, maxZ));
        vertex22.setNormal(normalTop);

        GaiaVertex vertex23 = new GaiaVertex(); // coincident with vertex11
        vertex23.setPosition(new Vector3d(minX, maxY, maxZ));
        vertex23.setNormal(normalTop);


        resultPrimitive.getVertices().add(vertex0);
        resultPrimitive.getVertices().add(vertex1);
        resultPrimitive.getVertices().add(vertex2);
        resultPrimitive.getVertices().add(vertex3);
        resultPrimitive.getVertices().add(vertex4);
        resultPrimitive.getVertices().add(vertex5);
        resultPrimitive.getVertices().add(vertex6);
        resultPrimitive.getVertices().add(vertex7);
        resultPrimitive.getVertices().add(vertex8);
        resultPrimitive.getVertices().add(vertex9);
        resultPrimitive.getVertices().add(vertex10);
        resultPrimitive.getVertices().add(vertex11);
        resultPrimitive.getVertices().add(vertex12);
        resultPrimitive.getVertices().add(vertex13);
        resultPrimitive.getVertices().add(vertex14);
        resultPrimitive.getVertices().add(vertex15);
        resultPrimitive.getVertices().add(vertex16);
        resultPrimitive.getVertices().add(vertex17);
        resultPrimitive.getVertices().add(vertex18);
        resultPrimitive.getVertices().add(vertex19);
        resultPrimitive.getVertices().add(vertex20);
        resultPrimitive.getVertices().add(vertex21);
        resultPrimitive.getVertices().add(vertex22);
        resultPrimitive.getVertices().add(vertex23);


        // BottomSurface
        GaiaSurface bottomSurface = new GaiaSurface();
        // 0, 3, 2, 1. The normal is (0, 0, -1)
        // Face0 (0, 2, 1)
        GaiaFace face0 = new GaiaFace();
        face0.setIndices(new int[]{0, 2, 1});
        bottomSurface.getFaces().add(face0);

        // Face1 (0, 3, 2)
        GaiaFace face1 = new GaiaFace();
        face1.setIndices(new int[]{0, 3, 2});
        bottomSurface.getFaces().add(face1);

        resultPrimitive.getSurfaces().add(bottomSurface);

        // TopSurface
        GaiaSurface topSurface = new GaiaSurface();
        // 20, 21, 22, 23. The normal is (0, 0, 1)
        //Vector3d normal = new Vector3d(0, 0, 1);
        // Face0 (20, 21, 22)
        GaiaFace face2 = new GaiaFace();
        face2.setIndices(new int[]{20, 21, 22});
        topSurface.getFaces().add(face2);

        // Face1 (20, 22, 23)
        GaiaFace face3 = new GaiaFace();
        face3.setIndices(new int[]{20, 22, 23});
        topSurface.getFaces().add(face3);

        resultPrimitive.getSurfaces().add(topSurface);

        // FrontSurface
        GaiaSurface frontSurface = new GaiaSurface();
        // 4, 5, 6, 7. The normal is (0, -1, 0)

        // Face0 (4, 5, 6)
        GaiaFace face4 = new GaiaFace();
        face4.setIndices(new int[]{4, 5, 6});
        frontSurface.getFaces().add(face4);

        // Face1 (4, 6, 7)
        GaiaFace face5 = new GaiaFace();
        face5.setIndices(new int[]{4, 6, 7});
        frontSurface.getFaces().add(face5);

        resultPrimitive.getSurfaces().add(frontSurface);

        // RearSurface
        GaiaSurface backSurface = new GaiaSurface();
        // 8, 11, 10, 9. The normal is (0, 1, 0)

        // Face0 (8, 11, 10)
        GaiaFace face6 = new GaiaFace();
        face6.setIndices(new int[]{8, 11, 10});
        backSurface.getFaces().add(face6);

        // Face1 (8, 10, 9)
        GaiaFace face7 = new GaiaFace();
        face7.setIndices(new int[]{8, 10, 9});
        backSurface.getFaces().add(face7);

        resultPrimitive.getSurfaces().add(backSurface);

        // RightSurface
        GaiaSurface rightSurface = new GaiaSurface();
        // 16, 17, 18, 19. The normal is (1, 0, 0)

        // Face0 (16, 17, 18)
        GaiaFace face8 = new GaiaFace();
        face8.setIndices(new int[]{16, 17, 18});
        rightSurface.getFaces().add(face8);

        // Face1 (16, 18, 19)
        GaiaFace face9 = new GaiaFace();
        face9.setIndices(new int[]{16, 18, 19});
        rightSurface.getFaces().add(face9);

        resultPrimitive.getSurfaces().add(rightSurface);

        // LeftSurface
        GaiaSurface leftSurface = new GaiaSurface();
        // 12, 15, 14, 13. The normal is (-1, 0, 0)

        // Face0 (12, 15, 14)
        GaiaFace face10 = new GaiaFace();
        face10.setIndices(new int[]{12, 15, 14});
        leftSurface.getFaces().add(face10);

        // Face1 (12, 14, 13)
        GaiaFace face11 = new GaiaFace();
        face11.setIndices(new int[]{12, 14, 13});
        leftSurface.getFaces().add(face11);

        resultPrimitive.getSurfaces().add(leftSurface);

        return resultPrimitive;
    }

    public static GaiaNode getGaiaNodeWithPrimitivesAsBox(GaiaNode node) {
        GaiaNode resultNode = new GaiaNode();

        // Check if exists meshes in the node
        int meshesCount = node.getMeshes().size();
        for (int j = 0; j < meshesCount; j++) {
            GaiaMesh mesh = node.getMeshes().get(j);
            GaiaMesh resultMesh = new GaiaMesh();
            int primitivesCount = mesh.getPrimitives().size();
            for (int k = 0; k < primitivesCount; k++) {
                GaiaPrimitive primitive = mesh.getPrimitives().get(k);
                GaiaBoundingBox bbox = primitive.getBoundingBox(null);
                GaiaPrimitive resultPrimitive = getPrimitiveFromBoundingBox(bbox);
                resultMesh.getPrimitives().add(resultPrimitive);
            }

            resultNode.getMeshes().add(resultMesh);

        }

        int childrenCount = node.getChildren().size();
        for (int j = 0; j < childrenCount; j++) {
            GaiaNode childNode = node.getChildren().get(j);
            GaiaNode resultChildNode = getGaiaNodeWithPrimitivesAsBox(childNode);
            resultNode.getChildren().add(resultChildNode);
        }

        return resultNode;
    }

    public static GaiaScene getGaiaSceneWithPrimitivesAsBox(GaiaScene gaiaScene) {
        GaiaScene resultScene = new GaiaScene();

        int nodesCount = gaiaScene.getNodes().size();
        for (int i = 0; i < nodesCount; i++) {
            GaiaNode node = gaiaScene.getNodes().get(i);
            GaiaNode resultNode = getGaiaNodeWithPrimitivesAsBox(node);

            resultScene.getNodes().add(resultNode);
        }

        return resultScene;
    }

    public static Vector4d getAverageColor(List<GaiaFaceData> faceDataList) {
        Vector4d resultColor = new Vector4d();
        resultColor.set(0.0, 0.0, 0.0, 0.0);
        int averageColorCount = 0;
        for (GaiaFaceData faceData : faceDataList) {
            Vector4d color = faceData.getAverageColor();
            if (color == null) {
                continue;
            }
            resultColor.x += color.x;
            resultColor.y += color.y;
            resultColor.z += color.z;
            resultColor.w += color.w;
            averageColorCount++;
        }

        if (averageColorCount == 0) {
            return null;
        }

        resultColor.x /= averageColorCount;
        resultColor.y /= averageColorCount;
        resultColor.z /= averageColorCount;
        resultColor.w /= averageColorCount;

        return resultColor;
    }

    public static List<Vector3d> getCleanPoints3dArray(List<Vector3d> pointsArray, List<Vector3d> cleanPointsArray, double error) {
        // Here checks uroborus, and check if there are adjacent points in the same position
        if (cleanPointsArray == null) {
            cleanPointsArray = new ArrayList<>();
        } else {
            cleanPointsArray.clear();
        }

        int pointsCount = pointsArray.size();
        Vector3d firstPoint = null;
        Vector3d lastPoint = null;
        for (int i = 0; i < pointsCount; i++) {
            Vector3d currPoint = pointsArray.get(i);
            if (i == 0) {
                firstPoint = currPoint;
                lastPoint = currPoint;
                cleanPointsArray.add(currPoint);
                continue;
            }

            if (!currPoint.equals(firstPoint) && !currPoint.equals(lastPoint)) {

                if (GeometryUtils.areAproxEqualsPoints3d(currPoint, firstPoint, error)) {
                    // the polygon is uroborus
                    continue;
                }

                if (GeometryUtils.areAproxEqualsPoints3d(currPoint, lastPoint, error)) {
                    // the point is the same as the last point
                    continue;
                }

                cleanPointsArray.add(currPoint);
                lastPoint = currPoint;
            }

        }

        // now, erase colineal points
        double dotProdError = 1.0 - 1e-10;
        pointsCount = cleanPointsArray.size();
        for (int i = 0; i < pointsCount; i++) {
            int idxPrev = GeometryUtils.getPrevIdx(i, pointsCount);
            int idxNext = GeometryUtils.getNextIdx(i, pointsCount);
            Vector3d prevPoint = cleanPointsArray.get(idxPrev);
            Vector3d currPoint = cleanPointsArray.get(i);
            Vector3d nextPoint = cleanPointsArray.get(idxNext);

            Vector3d v1 = new Vector3d();
            Vector3d v2 = new Vector3d();
            currPoint.sub(prevPoint, v1);
            nextPoint.sub(currPoint, v2);
            v1.normalize();
            v2.normalize();

            double dotProd = v1.dot(v2);
            if (Math.abs(dotProd) >= dotProdError) {
                // the points are colineal
                cleanPointsArray.remove(i);
                i--;
                pointsCount--;
            }
        }

        return cleanPointsArray;
    }

    public static GaiaScene getGaiaSceneLego(GaiaScene gaiaScene, float octreeMinSize) {
        GaiaScene resultScene = new GaiaScene();
        GaiaOctree gaiaOctree = GaiaOctreeUtils.getSceneOctree(gaiaScene, octreeMinSize);

        List<GaiaOctree> octreeList = new ArrayList<>();
        gaiaOctree.extractOctreesWithContents(octreeList);

        GaiaNode nodeRoot = new GaiaNode();
        resultScene.getNodes().add(nodeRoot);

        GaiaNode node = new GaiaNode();
        nodeRoot.getChildren().add(node);

        GaiaMesh mesh = new GaiaMesh();
        node.getMeshes().add(mesh);

        GaiaPrimitive primitiveMaster = new GaiaPrimitive();
        mesh.getPrimitives().add(primitiveMaster);

        for (GaiaOctree octree : octreeList) {
            boolean[] hasNeighbor = octree.hasNeighbor(); // left, right, front, rear, bottom, top.
            GaiaBoundingBox bbox = octree.getBoundingBox();

            GaiaPrimitive primitive = getPrimitiveFromBoundingBox(bbox, !hasNeighbor[0], !hasNeighbor[1], !hasNeighbor[2], !hasNeighbor[3], !hasNeighbor[4], !hasNeighbor[5]);

            //Vector4d randomColor = new Vector4d(Math.random(), Math.random(), Math.random(), 1.0);
            Vector4d averageColor = GeometryUtils.getAverageColor(octree.getFaceDataList());

            //averageColor = new Vector4d(0.9, 0.9, 0.9, 1.0);

            if (averageColor == null) {
                averageColor = new Vector4d(1.0, 0.0, 1.0, 1.0);
            }
            byte[] color = new byte[4];
            color[0] = (byte) (averageColor.x * 255);
            color[1] = (byte) (averageColor.y * 255);
            color[2] = (byte) (averageColor.z * 255);
            color[3] = (byte) (averageColor.w * 255);


            List<GaiaVertex> vertices = primitive.getVertices();
            for (GaiaVertex vertex : vertices) {
                vertex.setColor(color);
            }
            // End Test.------------------------------------------------------------------------------------------------

            GaiaPrimitiveUtils.mergePrimitives(primitiveMaster, primitive);
        }

        return resultScene;
    }

    public static boolean isInvalidVector(Vector3d vector) {
        return !isValidVector(vector);
    }

    public static boolean isValidVector(Vector3d vector) {
        boolean valid = true;
        if (!Double.isNaN(vector.get(0)) && !Double.isNaN(vector.get(1)) && !Double.isNaN(vector.get(2))) {
            // check if vector is zero
            valid = vector.x != 0.0 || vector.y != 0.0 || vector.z != 0.0;
        } else {
            valid = false;
        }
        return valid;
    }

    public static Vector3d calcNormal3D(Vector3d p1, Vector3d p2, Vector3d p3) {
        Vector3d p2SubP1 = new Vector3d(p2).sub(p1);
        Vector3d p3SubP2 = new Vector3d(p3).sub(p2);
        Vector3d normal = new Vector3d(p2SubP1).cross(p3SubP2);
        normal.normalize();
        return normal;
    }

    public static Vector3d calcNormal3D(GaiaVertex vertex1, GaiaVertex vertex2, GaiaVertex vertex3) {
        Vector3d position1 = vertex1.getPosition();
        Vector3d position2 = vertex2.getPosition();
        Vector3d position3 = vertex3.getPosition();
        return calcNormal3D(position1, position2, position3);
    }

    public static void calculateNormal3D(List<Vector3d> polygon, Vector3d resultNormal) {
        // calculate the normal of the polygon
        int pointsCount = polygon.size();
        if (pointsCount < 3) {
            return;
        }

        for (int i = 0; i < pointsCount; i++) {
            int idxNext = getNextIdx(i, pointsCount);
            int idxPrev = getPrevIdx(i, pointsCount);
            Vector3d currPoint = polygon.get(i);
            Vector3d nextPoint = polygon.get(idxNext);
            Vector3d prevPoint = polygon.get(idxPrev);

            Vector3d v1 = new Vector3d();
            Vector3d v2 = new Vector3d();
            currPoint.sub(prevPoint, v1);
            // check if v1 valid
            if (!isValidVector(v1)) {
                // v1 is invalid
                continue;
            }
            v1.normalize();

            nextPoint.sub(currPoint, v2);
            // check if v2 valid
            if (!isValidVector(v2)) {
                // v2 is invalid
                continue;
            }
            v2.normalize();

            Vector3d cross = new Vector3d();
            v1.cross(v2, cross);


            if (!isValidVector(cross)) {
                // cross is invalid
                continue;
            }

            cross.normalize();

            double dotProd = v1.dot(v2);
            double angRad = Math.acos(dotProd); // because v1 and v2 are normalized

            resultNormal.add(cross.x * angRad, cross.y * angRad, cross.z * angRad);
        }

        resultNormal.normalize();
    }



    public static PlaneType getBestPlaneToProject(Vector3d normal) {

        float absX = Math.abs((float) normal.x);
        float absY = Math.abs((float) normal.y);
        float absZ = Math.abs((float) normal.z);

        if (absX > absY && absX > absZ) {
            // the best plane is the YZ plane
            if (normal.x > 0) {
                return PlaneType.YZ;
            } else {
                return PlaneType.YZNEG;
            }
        } else if (absY > absX && absY > absZ) {
            // the best plane is the XZ plane
            if (normal.y > 0) {
                return PlaneType.XZ;
            } else {
                return PlaneType.XZNEG;
            }
        } else {
            // the best plane is the XY plane
            if (normal.z > 0) {
                return PlaneType.XY;
            } else {
                return PlaneType.XYNEG;
            }
        }
    }
}
