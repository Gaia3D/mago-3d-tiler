package com.gaia3d.basic.geometry.tessellator;

import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.util.GeometryUtils;
import lombok.NoArgsConstructor;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor
public class GaiaTessellator {
    // tessellate.***
    public void tessellate3D(List<Vector3d> points3dArray, List<Integer> resultTrianglesIndices) {
        // 1rst, must know the normal of the polygon to project the polygon to a plane and resilve the tessellation in 2d.***
        Vector3d normal = new Vector3d();
        calculateFastNormal3D(points3dArray, normal); // the normal can be reversed.***

        Vector3d normalTest = new Vector3d();
        calculateNormal3D(points3dArray, normalTest);

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
                projectedPoints2D.add(new Point2DTess(new Vector2d(vertex.y, vertex.z), vertex, null));
            }
        } else if (bestPlane.equals("XZ")) {
            // the best plane is the XZ plane.***
            for (Vector3d vertex : points3dArray) {
                projectedPoints2D.add(new Point2DTess(new Vector2d(vertex.x, vertex.z), vertex, null));
            }
        } else {
            // the best plane is the XY plane.***
            for (Vector3d vertex : points3dArray) {
                projectedPoints2D.add(new Point2DTess(new Vector2d(vertex.x, vertex.y), vertex, null));
            }
        }

        // clear the projectedPoints2D.***
        projectedPoints2D = getCleanPoints2DTessArray(projectedPoints2D, null, 1E-10);

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

    public void tessellate3D(List<Vector3d> points3dArray, List<List<Vector3d>> interiorPolygons, List<Vector3d> resultPolygonPoints, List<Integer> resultTrianglesIndices) {
        // 1rst, must know the normal of the polygon to project the polygon to a plane and resolve the tessellation in 2d.***
        Vector3d normal = new Vector3d();
        calculateFastNormal3D(points3dArray, normal); // the normal can be reversed.***

        Vector3d normalTest = new Vector3d();
        calculateNormal3D(points3dArray, normalTest);

        Map<Vector3d, Integer> mapPoints3dIndices = new HashMap<>();
        int pointsCount = points3dArray.size();
        for (int i = 0; i < pointsCount; i++) {
            mapPoints3dIndices.put(points3dArray.get(i), i);
        }

        // now, with the normal, find the best plane axis aligned to project the polygon.***
        // possible planes : XY, XZ, YZ.***
        // the best plane is the plane that has the normal more aligned to the plane normal.***
        List<Point2DTess> projectedPoints2D = new ArrayList<>();
        List<List<Point2DTess>> interiorProjectedPoints2D = new ArrayList<>();
        int interiorPolygonsCount = interiorPolygons.size();

        String bestPlane = getBestPlaneToProject(normal);

        if (bestPlane.equals("YZ")) {
            // the best plane is the YZ plane.***
            for (Vector3d vertex : points3dArray) {
                projectedPoints2D.add(new Point2DTess(new Vector2d(vertex.y, vertex.z), vertex, null));
            }

            for (int i = 0; i < interiorPolygonsCount; i++) {
                List<Vector3d> interiorPolygon = interiorPolygons.get(i);
                List<Point2DTess> interiorProjectedPoints = new ArrayList<>();
                for (Vector3d vertex : interiorPolygon) {
                    interiorProjectedPoints.add(new Point2DTess(new Vector2d(vertex.y, vertex.z), vertex, null));
                }
                interiorProjectedPoints2D.add(interiorProjectedPoints);
            }
        } else if (bestPlane.equals("XZ")) {
            // the best plane is the XZ plane.***
            for (Vector3d vertex : points3dArray) {
                projectedPoints2D.add(new Point2DTess(new Vector2d(vertex.x, vertex.z), vertex, null));
            }

            for (int i = 0; i < interiorPolygonsCount; i++) {
                List<Vector3d> interiorPolygon = interiorPolygons.get(i);
                List<Point2DTess> interiorProjectedPoints = new ArrayList<>();
                for (Vector3d vertex : interiorPolygon) {
                    interiorProjectedPoints.add(new Point2DTess(new Vector2d(vertex.x, vertex.z), vertex, null));
                }
                interiorProjectedPoints2D.add(interiorProjectedPoints);
            }
        } else {
            // the best plane is the XY plane.***
            for (Vector3d vertex : points3dArray) {
                projectedPoints2D.add(new Point2DTess(new Vector2d(vertex.x, vertex.y), vertex, null));
            }

            for (int i = 0; i < interiorPolygonsCount; i++) {
                List<Vector3d> interiorPolygon = interiorPolygons.get(i);
                List<Point2DTess> interiorProjectedPoints = new ArrayList<>();
                for (Vector3d vertex : interiorPolygon) {
                    interiorProjectedPoints.add(new Point2DTess(new Vector2d(vertex.x, vertex.y), vertex, null));
                }
                interiorProjectedPoints2D.add(interiorProjectedPoints);
            }
        }

        // calculate the boundingRectangle of the exteriorPolygon.***
        GaiaRectangle boundingRectangle = new GaiaRectangle();
        int exteriorPointsCount = projectedPoints2D.size();
        for (int i = 0; i < exteriorPointsCount; i++) {
            Vector2d currPoint = projectedPoints2D.get(i).getPoint();
            if (i == 0) {
                boundingRectangle.setInit(currPoint);
            } else {
                boundingRectangle.addPoint(currPoint);
            }
        }

        // now, calculate the factor to transform the boundingRectangle to a square.***
        double bRectWidth = boundingRectangle.getWidth();
        double bRectHeight = boundingRectangle.getHeight();
        double xFactor = 1.0;
        double yFactor = 1.0;
        if(bRectWidth > bRectHeight)
        {
            // the width is bigger than the height.***
            yFactor = bRectWidth / bRectHeight;
        }
        else if(bRectWidth < bRectHeight)
        {
            // the height is bigger than the width.***
            xFactor = bRectHeight / bRectWidth;
        }

        if(xFactor > 10 || yFactor > 10)
        {
            // now, multiply all point2d of exterior & interior polygons by the factors.***
            // Exterior points.***
            for (int i = 0; i < exteriorPointsCount; i++) {
                Point2DTess point2D = projectedPoints2D.get(i);
                Vector2d point = point2D.getPoint();
                point.x *= xFactor;
                point.y *= yFactor;
            }

            // Interior points.***
            for (int i = 0; i < interiorPolygonsCount; i++) {
                List<Point2DTess> interiorPoints = interiorProjectedPoints2D.get(i);
                int interiorPointsCount = interiorPoints.size();
                for (int j = 0; j < interiorPointsCount; j++) {
                    Point2DTess point2D = interiorPoints.get(j);
                    Vector2d point = point2D.getPoint();
                    point.x *= xFactor;
                    point.y *= yFactor;
                }
            }
        }

        // clear the projectedPoints2D.***
        double epsilon = 1E-10; // 1e-10(original).***

        // clean the exterior points.***
        projectedPoints2D = getCleanPoints2DTessArray(projectedPoints2D, null, epsilon);

        // clean the interior points.***
        for(int i=0; i<interiorPolygonsCount; i++)
        {
            List<Point2DTess> interiorProjectedPoints = interiorProjectedPoints2D.get(i);
            interiorProjectedPoints = getCleanPoints2DTessArray(interiorProjectedPoints, null, epsilon);
            interiorProjectedPoints2D.set(i, interiorProjectedPoints);
        }

        // Create polygon2DTess.***
        Polygon2DTess exteriorPolygon2DTess = new Polygon2DTess(projectedPoints2D);

        List<Polygon2DTess> interiorPolygons2DTess = new ArrayList<>();
        int interiorPointsCountTest = 0;
        for(int i=0; i<interiorPolygonsCount; i++)
        {
            List<Point2DTess> interiorProjectedPoints = interiorProjectedPoints2D.get(i);
            interiorPointsCountTest += interiorProjectedPoints.size();
            Polygon2DTess interiorPolygon2D = new Polygon2DTess(interiorProjectedPoints);
            interiorPolygons2DTess.add(interiorPolygon2D);
        }

        // Now, check the normals. the exteriorNormal must be inverse of interiorNormals.************************************
        List<Integer> resultConcaveIndices = new ArrayList<>();
        float exteriorPolygonNormal = exteriorPolygon2DTess.calculateNormal2D(resultConcaveIndices);

        int interiorPolygonsCount2 = interiorPolygons2DTess.size();
        for(int i=0; i<interiorPolygonsCount2; i++)
        {
            Polygon2DTess interiorPolygon2D = interiorPolygons2DTess.get(i);
            List<Integer> resultConcaveIndices2 = new ArrayList<>();
            float interiorPolygonNormal = interiorPolygon2D.calculateNormal2D(resultConcaveIndices2);
            if(exteriorPolygonNormal * interiorPolygonNormal > 0)
            {
                // the normals are not inverse.***
                interiorPolygon2D.reverse();
            }
        }
        // End check the normals.--------------------------------------------------------------------------------------------

        Polygon2DTess resultPolygon2dTess = tessellateHoles(exteriorPolygon2DTess, interiorPolygons2DTess, resultTrianglesIndices);

        int pointsCount2 = resultPolygon2dTess.getPoints().size();
        for(int i=0; i<pointsCount2; i++)
        {
            Point2DTess point2DTess = resultPolygon2dTess.getPoint(i);
            Vector3d parentVertex = point2DTess.getParentPoint();
            resultPolygonPoints.add(parentVertex);
        }
    }



    public List<Point2DTess> getCleanPoints2DTessArray(List<Point2DTess> points2DArray, List<Point2DTess> ResultPoints2DArray, double error) {
        // this function eliminates colineal points, same position points and check uroborus.***
        if (ResultPoints2DArray == null) {
            ResultPoints2DArray = new ArrayList<>();
        }

        // 1rst, check uroborus.***
        int pointsCount = points2DArray.size();
        Point2DTess firstPoint = null;
        Point2DTess lastPoint = null;
        for (int i = 0; i < pointsCount; i++) {
            Point2DTess currPoint = points2DArray.get(i);
            if (i == 0) {
                ResultPoints2DArray.add(currPoint);
                firstPoint = currPoint;
                lastPoint = currPoint;
                continue;
            }

            if (!currPoint.equals(firstPoint) && !currPoint.equals(lastPoint)) {
                Vector2d curr2d = currPoint.getPoint();
                Vector2d last2d = lastPoint.getPoint();
                Vector2d first2d = firstPoint.getPoint();

                if(GeometryUtils.areAproxEqualsPoints2d(curr2d, first2d, error))
                {
                    // the polygon is uroborus.***
                    continue;
                }

                if(GeometryUtils.areAproxEqualsPoints2d(curr2d, last2d, error))
                {
                    // the point is the same as the last point.***
                    continue;
                }

                ResultPoints2DArray.add(currPoint);
                lastPoint = currPoint;
            }
        }

        // now, erase colineal points.***
        double dotProdError = 1.0 - 1e-10;
        pointsCount = ResultPoints2DArray.size();
        for (int i = 0; i < pointsCount; i++) {
            int idxPrev = getPrevIdx(i, pointsCount);
            int idxNext = getNextIdx(i, pointsCount);
            Point2DTess prevPoint = ResultPoints2DArray.get(idxPrev);
            Point2DTess currPoint = ResultPoints2DArray.get(i);
            Point2DTess nextPoint = ResultPoints2DArray.get(idxNext);

            Vector2d v1 = new Vector2d();
            Vector2d v2 = new Vector2d();
            currPoint.getPoint().sub(prevPoint.getPoint(), v1);
            nextPoint.getPoint().sub(currPoint.getPoint(), v2);
            v1.normalize();
            v2.normalize();

            double dotProd = v1.dot(v2);
            if (Math.abs(dotProd) >= dotProdError)
            {
                // the points are colineal.***
                ResultPoints2DArray.remove(i);
                i--;
                pointsCount--;
            }
        }

        return ResultPoints2DArray;
    }

    public Polygon2DTess getCleanPolygon2DTess(Polygon2DTess polygon, Polygon2DTess resultCleanPolygon, double error) {
        // this function eliminates collinear points, same position points and check uroborus.***
        if (resultCleanPolygon == null) {
            resultCleanPolygon = new Polygon2DTess(new ArrayList<>());
        }

        List<Point2DTess> cleanPoint2DTessArray = getCleanPoints2DTessArray(polygon.getPoints(), null, error);
        resultCleanPolygon.setPoints(cleanPoint2DTessArray);
        return resultCleanPolygon;
    }

    public void getExteriorAndInteriorsPolygons_TEST(List<Vector2d> pointsArray, List<Polygon2DTess> exteriorPolygons, List<Polygon2DTess> interiorPolygons, double error) {
        int pointsCount = pointsArray.size();
        List<Point2DTess> points = new ArrayList<>();
        Polygon2DTess currPolygon = new Polygon2DTess(points);
        Vector2d firstPoint = null;

        List<Polygon2DTess> tempPolygons = new ArrayList<>();

        for (int i = 0; i < pointsCount; i++) {
            Vector2d point = pointsArray.get(i);

            if (firstPoint == null) {
                firstPoint = point;
                points = new ArrayList<>();
                currPolygon.addPoint(new Point2DTess(point, null, null));
            } else {
                if (!firstPoint.equals(point)) {
                    currPolygon.addPoint(new Point2DTess(point, null, null));
                } else {
                    tempPolygons.add(currPolygon);
                    currPolygon = new Polygon2DTess(points);
                    firstPoint = null;
                }
            }

        }

        int tempPolygonsCount = tempPolygons.size();
        for (int i = 0; i < tempPolygonsCount; i++) {
            Polygon2DTess polygon = tempPolygons.get(i);
            Polygon2DTess cleanPolygon = getCleanPolygon2DTess(polygon, null, error);
            if (i == 0) {
                exteriorPolygons.add(cleanPolygon);
            } else {
                interiorPolygons.add(cleanPolygon);
            }
        }
    }

    private Polygon2DTess eliminateHoleBySplitSegment(Polygon2DTess exteriorPolygon, Polygon2DTess hole, int extPointIdx, int holePointIdx, Polygon2DTess resultPolygon) {
        if (resultPolygon == null) {
            resultPolygon = new Polygon2DTess(new ArrayList<>());
        }

        List<Point2DTess> extPoints = exteriorPolygon.getPoints();
        List<Point2DTess> holePoints = hole.getPoints();
        int extPointsCount = extPoints.size();
        int holePointsCount = holePoints.size();

        // 1rst, copy the extPolygon into resultPolygon, starting from extPointIdx.***
        boolean finished = false;
        int i = 0;
        int currIdx = extPointIdx;
        while (!finished && i < extPointsCount) {
            Point2DTess point = extPoints.get(currIdx);
            resultPolygon.addPoint(point);
            currIdx = getNextIdx(currIdx, extPointsCount);
            if (currIdx == extPointIdx) {
                finished = true;

                // must add the 1rst point.***
                point = exteriorPolygon.getPoint(currIdx);
                Vector2d newPoint = new Vector2d(point.getPoint().x, point.getPoint().y);
                Point2DTess newPoint2dtess = new Point2DTess(newPoint, point.getParentPoint(), null);
                resultPolygon.addPoint(newPoint2dtess);
            }
            i++;
        }

        // now copy the holePolygon into resultPolygon, starting from holePointIdx.***
        finished = false;
        i = 0;
        currIdx = holePointIdx;
        while (!finished && i < holePointsCount) {
            Point2DTess point = holePoints.get(currIdx);
            resultPolygon.addPoint(point);
            currIdx = getNextIdx(currIdx, holePointsCount);
            if (currIdx == holePointIdx) {
                finished = true;

                // must add the 1rst point.***
                point = hole.getPoint(currIdx);
                Vector2d newPoint = new Vector2d(point.getPoint().x, point.getPoint().y);
                Point2DTess newPoint2dtess = new Point2DTess(newPoint, point.getParentPoint(), null);
                resultPolygon.addPoint(newPoint2dtess);

            }
            i++;
        }


        return resultPolygon;
    }

    private Polygon2DTess eliminateHole(Polygon2DTess exteriorPolygon, Polygon2DTess hole, Polygon2DTess resultPolygon) {
        if (resultPolygon == null) {
            resultPolygon = new Polygon2DTess(new ArrayList<>());
        }

        int holePointIdx = hole.getMostLeftDownPoint2DIdx();
        Point2DTess holeLeftDownPoint = hole.getPoint(holePointIdx);
        List<Integer> exteriorSortedIndices = new ArrayList<>();
        getPointsIdxSortedByDistToPoint(exteriorPolygon, holeLeftDownPoint, exteriorSortedIndices);

        List<Integer> resultConvexIndices = new ArrayList<>();
        float normalExteriorPolygon = exteriorPolygon.calculateNormal2D(resultConvexIndices);
        int extPointsCount = exteriorSortedIndices.size();
        boolean finished = false;
        int i = 0;
        double error = 1E-10;
        while (!finished && i < extPointsCount) {
            int extPointIdx = exteriorSortedIndices.get(i);
            Point2DTess extPoint = exteriorPolygon.getPoint(extPointIdx);

            // Now, find the closest point of the hole with the extPoint.***********************************
            List<Integer> holeSortedIndices = new ArrayList<>();
            getPointsIdxSortedByDistToPoint(hole, extPoint, holeSortedIndices);
            // use only the 1rst point.***
            holePointIdx = holeSortedIndices.get(0);
            Point2DTess holeLeftDownPoint2 = hole.getPoint(holePointIdx);
            //-----------------------------------------------------------------------------------------------

            Segment2DTess segment = new Segment2DTess(extPoint, holeLeftDownPoint2);
            if (exteriorPolygon.isSegmentIntersectingPolygon(segment, error)) {
                // the segment is intersecting the polygon.***
                i++;
                continue;
            }

            // the segment is not intersecting the polygon.***
            // now, check if the segment is intersecting the hole.***
            if (!hole.isSegmentIntersectingPolygon(segment, error)) {
                // the segment is intersecting the hole.***
                // eliminate the hole by split the segment.***
                Polygon2DTess resultPolygonAux = new Polygon2DTess(new ArrayList<>());
                eliminateHoleBySplitSegment(exteriorPolygon, hole, extPointIdx, holePointIdx, resultPolygonAux);

                // now, check the resultPolygonAux butterfly case.***
                resultConvexIndices.clear();
                float normal = resultPolygonAux.calculateNormal2D(resultConvexIndices);
                if(normal * normalExteriorPolygon > 0)
                {
                    // the normal is positive.***
                    // the resultPolygon is not butterfly.***
                    finished = true;
                    resultPolygon.points.clear();
                    resultPolygon.points.addAll(resultPolygonAux.points);
                    break;
                }
            }

            i++;
        }

        if(!finished)
        {
            return exteriorPolygon;
        }

        return resultPolygon;
    }

    private void getPointsIdxSortedByDistToPoint(Polygon2DTess polygon, Point2DTess point, List<Integer> resultSortedIndices) {
        // get the points sorted by distance to the point.***
        List<Point2DTess> points = polygon.getPoints();
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
        mapIdxDist.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEachOrdered(x -> resultSortedIndices.add(x.getKey()));
    }

    public void tessellateHoles2D(List<Vector2d> exteriorPoints, List<List<Vector2d>> interiorPoints, List<Vector2d> resultPositions, List<Integer> resultIndices) {
        // make exteriorPolygon2DTess.***
        List<Point2DTess> points = new ArrayList<>();
        for (Vector2d point : exteriorPoints) {
            points.add(new Point2DTess(point, null, null));
        }

        Polygon2DTess exteriorPolygon = new Polygon2DTess(points);
        List<Polygon2DTess> interiorPolygons = new ArrayList<>();
        int holesCount = interiorPoints.size();
        for (int i = 0; i < holesCount; i++) {
            List<Vector2d> holePoints = interiorPoints.get(i);
            points = new ArrayList<>();
            for (Vector2d point : holePoints) {
                points.add(new Point2DTess(point, null, null));
            }
            Polygon2DTess hole = new Polygon2DTess(points);
            interiorPolygons.add(hole);
        }

        List<Integer> resultIndices2 = new ArrayList<>();
        Polygon2DTess resultPolygon = tessellateHoles(exteriorPolygon, interiorPolygons, resultIndices2);

        int pointsCount = resultPolygon.getPoints().size();
        for (int i = 0; i < pointsCount; i++) {
            Point2DTess point = resultPolygon.getPoint(i);
            Vector2d point2D = point.getPoint();
            resultPositions.add(point2D);
        }

        resultIndices.addAll(resultIndices2);

    }

    public Polygon2DTess tessellateHoles(Polygon2DTess exteriorPolygon, List<Polygon2DTess> interiorPolygons, List<Integer> resultIndices) {
        // check polygons sense.***
        List<Integer> concaveIndices = new ArrayList<>();
        GaiaRectangle exteriorBRect = exteriorPolygon.getBoundingRectangle();
        Vector2d extMinPoint = exteriorBRect.getLeftBottomPoint();
        TreeMap<Double, Polygon2DTess> mapDistHolePolygon = new TreeMap<>();

        int holesCount = interiorPolygons.size();
        for (int i = 0; i < holesCount; i++) {
            Polygon2DTess hole = interiorPolygons.get(i);

            GaiaRectangle rect = hole.getBoundingRectangle();
            Vector2d minPoint = rect.getLeftBottomPoint();
            int mostLeftDownPointIdx = hole.getMostLeftDownPoint2DIdx();
            Point2DTess mostLeftDownPoint = hole.getPoint(mostLeftDownPointIdx);

            double squareDist = extMinPoint.distanceSquared(mostLeftDownPoint.getPoint()); // original.***
            //double squareDist = extMinPoint.distanceSquared(minPoint);
            mapDistHolePolygon.put(squareDist, hole);
        }

        // must sort the mapDistHolePolygon from small to big squaredDist.***
        List<Polygon2DTess> sortedPolygons = mapDistHolePolygon.values().stream().collect(Collectors.toList());


        int keysCount = sortedPolygons.size();

        for (int i = 0; i < keysCount; i++) {
            Polygon2DTess hole = sortedPolygons.get(i);
            Polygon2DTess resultPolygon = eliminateHole(exteriorPolygon, hole, null);
            exteriorPolygon = resultPolygon;
        }

        List<Polygon2DTess> resultConvexPolygons = new ArrayList<>();
        this.tessellate2D(exteriorPolygon, resultConvexPolygons);
        exteriorPolygon.setPointsIdxInList();

        // Make maps & Arrays.***********************************************************************************************
        int extPointsCount = exteriorPolygon.getPoints().size();
        // make map <originalPoints3d, originalPoint3d>.***
        Map<Vector3d, Vector3d> mapOriginalPoint2DOriginalPoint = new HashMap<>();
        for (int i = 0; i < extPointsCount; i++) {
            Point2DTess point2D = exteriorPolygon.getPoint(i);
            Vector3d originalPoint = point2D.getParentPoint();
            mapOriginalPoint2DOriginalPoint.put(originalPoint, originalPoint);
        }

        // make originalPoints3d array from mapOriginalPoint2DOriginalPoint.***
        List<Vector3d> originalPoints3d = new ArrayList<>();
        for (Vector3d originalPoint : mapOriginalPoint2DOriginalPoint.keySet()) {
            originalPoints3d.add(originalPoint);
        }

        // make map <originalPoint3d, idx>.***
        Map<Vector3d, Integer> mapOriginalPoint2DIdx = new HashMap<>();
        int originalPoints3dCount = originalPoints3d.size();
        for (int i = 0; i < originalPoints3dCount; i++) {
            Vector3d originalPoint = originalPoints3d.get(i);
            mapOriginalPoint2DIdx.put(originalPoint, i);
        }
        // End make maps & Arrays.-------------------------------------------------------------------------------------------


        // finally create the resultPolygon.***
        Polygon2DTess resultPolygon = new Polygon2DTess(new ArrayList<>());
        for (int i = 0; i < originalPoints3dCount; i++) {
            Point2DTess point2D = new Point2DTess(new Vector2d(), originalPoints3d.get(i), null);
            resultPolygon.addPoint(point2D);
        }

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
                int idxInList = mapOriginalPoint2DIdx.get(parentVertex);
                resultIndices.add(idxInList);
                resultPolygon.getPoint(idxInList).getPoint().set(point2D.getPoint());
            }
        }

        return resultPolygon;
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
            for (Integer sortedIndex : sortedIndices) {
                idxB = sortedIndex;
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
                if (segment.getLengthSquared() < error) {
                    // the segment is too short.***
                    continue;
                }

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
