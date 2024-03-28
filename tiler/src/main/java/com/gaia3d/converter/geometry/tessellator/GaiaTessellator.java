package com.gaia3d.converter.geometry.tessellator;

import lombok.NoArgsConstructor;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
public class GaiaTessellator {
    // tessellate.***
    public void tessellate3D(List<Vector3d> points3dArray, List<Integer> resultTrianglesIndices) {
        // 1rst, must know the normal of the polygon to project the polygon to a plane and resilve the tessellation in 2d.***
        Vector3d normal = new Vector3d();
        calculateFastNormal3D(points3dArray, normal); // the normal can be reversed.***

        Map<Vector3d, Integer> mapPoints3dIndices = new HashMap<>();
        int pointsCount = points3dArray.size();
        for (int i = 0; i < pointsCount; i++) {
            mapPoints3dIndices.put(points3dArray.get(i), i);
        }

        // now, with the normal, find the best plane axis aligned to project the polygon.***
        // possible planes : XY, XZ, YZ.***
        // the best plane is the plane that has the normal more aligned to the plane normal.***
        List<Point2DTess> projectedPoints2D = new ArrayList<>();
        String bestPlane = getBestPlaneToProject(normal);

        if (bestPlane.equals("YZ")) {
            // the best plane is the YZ plane.***
            for (Vector3d vertex : points3dArray) {
                projectedPoints2D.add(new Point2DTess(new Vector2d(vertex.y, vertex.z), vertex));
            }
        } else if (bestPlane.equals("XZ")) {
            // the best plane is the XZ plane.***
            for (Vector3d vertex : points3dArray) {
                projectedPoints2D.add(new Point2DTess(new Vector2d(vertex.x, vertex.z), vertex));
            }
        } else {
            // the best plane is the XY plane.***
            for (Vector3d vertex : points3dArray) {
                projectedPoints2D.add(new Point2DTess(new Vector2d(vertex.x, vertex.y), vertex));
            }
        }

        // now, must resolve the tessellation in 2d.***
        Polygon2DTess polygon2D = new Polygon2DTess(projectedPoints2D);
        List<Polygon2DTess> resultConvexPolygons = new ArrayList<>();
        tessellate2D(polygon2D, resultConvexPolygons);

        int convexPolygonsCount = resultConvexPolygons.size();
        for (int i = 0; i < convexPolygonsCount; i++) {
            Polygon2DTess convexPolygon = resultConvexPolygons.get(i);
            List<Integer> convexIndices = new ArrayList<>();
            convexPolygon.getTrianglesIndicesAsConvexPolygon(convexIndices);

            int convexIndicesCount = convexIndices.size();
            for (int j = 0; j < convexIndicesCount; j += 1) {
                int idx = convexIndices.get(j);
                Point2DTess point2D = convexPolygon.getPoint(idx);
                Vector3d parentVertex = point2D.getParentPoint();
                int parentVertexIndex = mapPoints3dIndices.get(parentVertex);
                resultTrianglesIndices.add(parentVertexIndex);
            }
        }

        polygon2D = null;
        resultConvexPolygons.clear();
    }

    public void getPointsIdxSortedByDistToPoint(Point2DTess point, List<Point2DTess> points, List<Integer> resultIndices) {
        // get the points sorted by distance to the point.***
        int pointsCount = points.size();
        if (pointsCount == 0) {
            return;
        }

        Map<Integer, Double> mapIdxDist = new HashMap<>();
        for (int i = 0; i < pointsCount; i++) {
            Point2DTess point2D = points.get(i);
            if (point2D == point) {
                // skip the same point.***
                continue;
            }
            double dist = point.squareDistanceTo(point2D);
            mapIdxDist.put(i, dist);
        }

        // sort the map.***
        mapIdxDist.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEachOrdered(x -> resultIndices.add(x.getKey()));
    }

    public void tessellate2D(Polygon2DTess polygon2D, List<Polygon2DTess> resultConvexPolygons) {
        // 1rst, must know the normal of the polygon to project the polygon to a plane and resolve the tessellation in 2d.***
        List<Integer> concaveIndices = new ArrayList<>();
        float normal = polygon2D.calculateNormal2D(concaveIndices);

        int concaveIndicesCount = concaveIndices.size();
        if (concaveIndicesCount == 0) {
            // the polygon is convex.***
            resultConvexPolygons.add(polygon2D);
            return;
        }

        // the polygon is concave.***
        // now, for any concave vertex, find the closest vertex to split the polygon.***
        boolean finded = false;
        int idxB = -1;
        int i = 0;
        double error = 1E-10;
        while (!finded && i < concaveIndicesCount) {
            int idxA = concaveIndices.get(i);
            Point2DTess pointA = polygon2D.getPoint(idxA);

            List<Integer> sortedIndices = new ArrayList<>();
            getPointsIdxSortedByDistToPoint(pointA, polygon2D.getPoints(), sortedIndices);

            int sortedIndicesCount = sortedIndices.size();
            for (int j = 0; j < sortedIndicesCount; j++) {
                idxB = sortedIndices.get(j);
                if (idxA == idxB) {
                    // skip the same point.***
                    continue;
                }

                // skip adjacent points.***
                if (idxA == polygon2D.getPrevIdx(idxB) || idxA == polygon2D.getNextIdx(idxB)) {
                    continue;
                }

                Point2DTess pointB = polygon2D.getPoint(idxB);
                Segment2DTess segment = new Segment2DTess(pointA, pointB);
                if (polygon2D.isSegmentIntersectingPolygon(segment, error)) {
                    // the segment is intersecting the polygon.***
                    continue;
                }

                List<Polygon2DTess> resultSplittedPolygons = new ArrayList<>();
                polygon2D.splitPolygon(idxA, idxB, resultSplittedPolygons);

                if (resultSplittedPolygons.size() < 2) {
                    // the polygon failed to split.***
                    continue;
                }

                Polygon2DTess polygonA = resultSplittedPolygons.get(0);
                Polygon2DTess polygonB = resultSplittedPolygons.get(1);
                List<Integer> concaveIndicesA = new ArrayList<>();
                List<Integer> concaveIndicesB = new ArrayList<>();
                float normalA = polygonA.calculateNormal2D(concaveIndicesA);
                float normalB = polygonB.calculateNormal2D(concaveIndicesB);

                if (normalA == normal && normalB == normal) {
                    // the polygon was splitted successfully.***
                    // now check if the polygonA is convex.***
                    if (concaveIndicesA.size() == 0) {
                        resultConvexPolygons.add(polygonA);
                    } else {
                        tessellate2D(polygonA, resultConvexPolygons);
                    }

                    // now check if the polygonB is convex.***
                    if (concaveIndicesB.size() == 0) {
                        resultConvexPolygons.add(polygonB);
                    } else {
                        tessellate2D(polygonB, resultConvexPolygons);
                    }

                    finded = true;
                    break;
                }
            }
            i++;
        }
    }

    private String getBestPlaneToProject(Vector3d normal) {
        float absX = Math.abs((float) normal.x);
        float absY = Math.abs((float) normal.y);
        float absZ = Math.abs((float) normal.z);

        if (absX > absY && absX > absZ) {
            // the best plane is the YZ plane.***
            return "YZ";
        } else if (absY > absX && absY > absZ) {
            // the best plane is the XZ plane.***
            return "XZ";
        } else {
            // the best plane is the XY plane.***
            return "XY";
        }
    }

    private boolean isValidVector(Vector3d vector) {
        boolean valid = true;
        if (!Double.isNaN(vector.get(0)) && !Double.isNaN(vector.get(1)) && !Double.isNaN(vector.get(2))) {
            // check if vector is zero.***
            valid = vector.x != 0.0 || vector.y != 0.0 || vector.z != 0.0;
        } else {
            valid = false;
        }

        return valid;
    }

    private boolean isValidVector(Vector2d vector) {
        boolean valid = true;
        if (!Double.isNaN(vector.get(0)) && !Double.isNaN(vector.get(1))) {
            // check if vector is zero.***
            valid = vector.x != 0.0 || vector.y != 0.0;
        } else {
            valid = false;
        }

        return valid;
    }

    public int getNextIdx(int idx, int pointsCount) {
        return (idx + 1) % pointsCount;
    }

    public int getPrevIdx(int idx, int pointsCount) {
        return (idx + pointsCount - 1) % pointsCount;
    }

    public void calculateFastNormal3D(List<Vector3d> polygon, Vector3d resultNormal) {
        // *************************************
        // take the 1rst valid cross product.***
        // *************************************
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
            // check if v1 valid.***
            if (!isValidVector(v1)) {
                // v1 is invalid.***
                continue;
            }
            v1.normalize();

            nextPoint.sub(currPoint, v2);
            // check if v2 valid.***
            if (!isValidVector(v2)) {
                // v2 is invalid.***
                continue;
            }
            v2.normalize();

            Vector3d cross = new Vector3d();
            v1.cross(v2, cross);
            if (isValidVector(cross)) {
                // cross is valid.***
                cross.normalize();
                resultNormal.set(cross);
                return;
            }
        }
    }

    public void calculateNormal3D(List<Vector3d> polygon, Vector3d resultNormal) {
        // calculate the normal of the polygon.***
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
            // check if v1 valid.***
            if (!isValidVector(v1)) {
                // v1 is invalid.***
                continue;
            }
            v1.normalize();

            nextPoint.sub(currPoint, v2);
            // check if v2 valid.***
            if (!isValidVector(v2)) {
                // v2 is invalid.***
                continue;
            }
            v2.normalize();

            Vector3d cross = new Vector3d();
            v1.cross(v2, cross);

            if (!isValidVector(cross)) {
                // cross is invalid.***
                continue;
            }

            cross.normalize();

            double dotProd = v1.dot(v2);
            double angRad = Math.acos(dotProd); // because v1 and v2 are normalized.***

            resultNormal.add(cross.x * angRad, cross.y * angRad, cross.z * angRad);
        }

        resultNormal.normalize();
    }
}
