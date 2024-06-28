package com.gaia3d.basic.geometry.tessellator;

import com.gaia3d.basic.geometry.GaiaRectangle;
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

    public List<Point2DTess> getCleanPoints2DTessArray(List<Point2DTess> points2DArray, List<Point2DTess> ResultPoints2DArray, double error)
    {
        // this function eliminates colineal points, same position points and check uroborus.***
        if(ResultPoints2DArray == null)
        {
            ResultPoints2DArray = new ArrayList<>();
        }

        // 1rst, check uroborus.***
        int pointsCount = points2DArray.size();
        Point2DTess firstPoint = null;
        Point2DTess lastPoint = null;
        for(int i=0; i<pointsCount; i++)
        {
            Point2DTess currPoint = points2DArray.get(i);
            if(i == 0)
            {
                ResultPoints2DArray.add(currPoint);
                firstPoint = currPoint;
                lastPoint = currPoint;
                continue;
            }

            if(!currPoint.equals(firstPoint) && !currPoint.equals(lastPoint))
            {
                ResultPoints2DArray.add(currPoint);
                lastPoint = currPoint;
            }
            else {
                int hola = 0;
            }
        }

        // now, erase colineal points.***
        pointsCount = ResultPoints2DArray.size();
        for(int i=0; i<pointsCount; i++)
        {
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
            if(Math.abs(dotProd) > 1.0 - error)
            {
                // the points are colineal.***
                ResultPoints2DArray.remove(i);
                i--;
                pointsCount--;
            }
        }

        return ResultPoints2DArray;

    }

    public Polygon2DTess getCleanPolygon2DTess(Polygon2DTess polygon, Polygon2DTess resultCleanPolygon, double error)
    {
        // this function eliminates colineal points, same position points and check uroborus.***
        if(resultCleanPolygon == null)
        {
            resultCleanPolygon = new Polygon2DTess(new ArrayList<>());
        }

        List<Point2DTess> cleanPoint2DTessArray = getCleanPoints2DTessArray(polygon.getPoints(), null, error);
        resultCleanPolygon.setPoints(cleanPoint2DTessArray);
        return resultCleanPolygon;
    }

    public void getExteriorAndInteriorsPolygons_TEST(List<Vector2d> pointsArray, List<Polygon2DTess> exteriorPolygons,List<Polygon2DTess> interiorPolygons, double error )
    {
        int pointsCount = pointsArray.size();
        List<Point2DTess> points = new ArrayList<>();
        Polygon2DTess currPolygon = new Polygon2DTess(points);
        Vector2d firstPoint = null;

        List<Polygon2DTess> tempPolygons = new ArrayList<>();

        for(int i=0; i<pointsCount; i++)
        {
            Vector2d point = pointsArray.get(i);

            if(firstPoint == null)
            {
                firstPoint = point;
                points = new ArrayList<>();
                currPolygon.addPoint(new Point2DTess(point, null, null));
            }
            else
            {
                if(!firstPoint.equals(point))
                {
                    currPolygon.addPoint(new Point2DTess(point, null, null));
                }
                else
                {
                    tempPolygons.add(currPolygon);
                    currPolygon = new Polygon2DTess(points);
                    firstPoint = null;
                }
            }

        }

        int tempPolygonsCount = tempPolygons.size();
        for(int i=0; i<tempPolygonsCount; i++)
        {
            Polygon2DTess polygon = tempPolygons.get(i);
            Polygon2DTess cleanPolygon = getCleanPolygon2DTess(polygon, null, error);
            if(i == 0)
            {
                exteriorPolygons.add(cleanPolygon);
            }
            else
            {
                interiorPolygons.add(cleanPolygon);
            }
        }
    }

    private Polygon2DTess eliminateHoleBySplitSegment(Polygon2DTess exteriorPolygon, Polygon2DTess hole, int extPointIdx, int holePointIdx, Polygon2DTess resultPolygon)
    {
        if(resultPolygon == null)
        {
            resultPolygon = new Polygon2DTess(new ArrayList<>());
        }

        List<Point2DTess> extPoints = exteriorPolygon.getPoints();
        List<Point2DTess> holePoints = hole.getPoints();
        int extPointsCount = extPoints.size();
        int holePointsCount = holePoints.size();

        // 1rst, copy the extPolygon into resultPolygon, starting from extPointIdx.***
        boolean finished = false;
        int i=0;
        int currIdx = extPointIdx;
        while(!finished && i<extPointsCount)
        {
            Point2DTess point = extPoints.get(currIdx);
            resultPolygon.addPoint(point);
            currIdx = getNextIdx(currIdx, extPointsCount);
            if(currIdx == extPointIdx)
            {
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
        i=0;
        currIdx = holePointIdx;
        while(!finished && i<holePointsCount)
        {
            Point2DTess point = holePoints.get(currIdx);
            resultPolygon.addPoint(point);
            currIdx = getNextIdx(currIdx, holePointsCount);
            if(currIdx == holePointIdx)
            {
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

    private Polygon2DTess eliminateHole(Polygon2DTess exteriorPolygon, Polygon2DTess hole, Polygon2DTess resultPolygon)
    {
        if(resultPolygon == null)
        {
            resultPolygon = new Polygon2DTess(new ArrayList<>());
        }

        int holePointIdx = hole.getMostLeftDownPoint2DIdx();
        Point2DTess holeLeftDownPoint = hole.getPoint(holePointIdx);
        List<Integer> exteriorSortedIndices = new ArrayList<>();
        getPointsIdxSortedByDistToPoint(exteriorPolygon, holeLeftDownPoint, exteriorSortedIndices);

        int extPointsCount = exteriorSortedIndices.size();
        boolean finished = false;
        int i=0;
        double error = 1E-10;
        while(!finished && i < extPointsCount)
        {
            int extPointIdx = exteriorSortedIndices.get(i);
            Point2DTess extPoint = exteriorPolygon.getPoint(extPointIdx);
            Segment2DTess segment = new Segment2DTess(extPoint, holeLeftDownPoint);
            if(exteriorPolygon.isSegmentIntersectingPolygon(segment, error))
            {
                // the segment is intersecting the polygon.***
                i++;
                continue;
            }

            // the segment is not intersecting the polygon.***
            // now, check if the segment is intersecting the hole.***
            if(!hole.isSegmentIntersectingPolygon(segment, error))
            {
                // the segment is intersecting the hole.***
                // eliminate the hole by split the segment.***
                eliminateHoleBySplitSegment(exteriorPolygon, hole, extPointIdx, holePointIdx, resultPolygon);
                finished = true;
                break;
            }

            i++;
        }

        if(!finished)
        {
            // the hole is not intersecting the exteriorPolygon.***
            // so, must return the exteriorPolygon.***
            int hola = 0;
        }

        return resultPolygon;
    }

    private void getPointsIdxSortedByDistToPoint(Polygon2DTess polygon, Point2DTess point, List<Integer> resultSortedIndices)
    {
        // get the points sorted by distance to the point.***
        List<Point2DTess> points = polygon.getPoints();
        int pointsCount = points.size();
        if(pointsCount == 0)
        {
            return;
        }

        Map<Integer, Double> mapIdxDist = new HashMap<>();
        for(int i=0; i<pointsCount; i++)
        {
            Point2DTess point2D = points.get(i);
            if(point2D == point)
            {
                // skip the same point.***
                continue;
            }
            double dist = point.squareDistanceTo(point2D);
            mapIdxDist.put(i, dist);
        }

        // sort the map.***
        mapIdxDist.entrySet().stream().sorted(Map.Entry.comparingByValue()).forEachOrdered(x -> resultSortedIndices.add(x.getKey()));
    }

    public void tessellateHoles2D(List<Vector2d> exteriorPoints, List<List<Vector2d>> interiorPoints, List<Vector2d> resultPositions, List<Integer> resultIndices)
    {
        // make exteriorPolygon2DTess.***
        List<Point2DTess> points = new ArrayList<>();
        for(Vector2d point : exteriorPoints)
        {
            points.add(new Point2DTess(point, null, null));
        }

        Polygon2DTess exteriorPolygon = new Polygon2DTess(points);
        List<Polygon2DTess> interiorPolygons = new ArrayList<>();
        int holesCount = interiorPoints.size();
        for(int i=0; i<holesCount; i++)
        {
            List<Vector2d> holePoints = interiorPoints.get(i);
            points = new ArrayList<>();
            for(Vector2d point : holePoints)
            {
                points.add(new Point2DTess(point, null, null));
            }
            Polygon2DTess hole = new Polygon2DTess(points);
            interiorPolygons.add(hole);
        }

        List<Integer> resultIndices2 = new ArrayList<>();
        Polygon2DTess resultPolygon = tessellateHoles(exteriorPolygon, interiorPolygons, resultIndices2);

        int pointsCount = resultPolygon.getPoints().size();
        for(int i=0; i<pointsCount; i++)
        {
            Point2DTess point = resultPolygon.getPoint(i);
            Vector2d point2D = point.getPoint();
            resultPositions.add(point2D);
        }

        resultIndices.addAll(resultIndices2);

    }

    public Polygon2DTess tessellateHoles(Polygon2DTess exteriorPolygon, List<Polygon2DTess> interiorPolygons, List<Integer> resultIndices)
    {
        // check polygons sense.***
        List<Integer> concaveIndices = new ArrayList<>();
        GaiaRectangle exteriorBRect = exteriorPolygon.getBoundingRectangle();
        Vector2d extMinPoint = exteriorBRect.getLeftBottomPoint();
        Map<Double, Polygon2DTess> mapDistHolePolygon = new HashMap<>();

        int holesCount = interiorPolygons.size();
        for(int i=0; i<holesCount; i++)
        {
            Polygon2DTess hole = interiorPolygons.get(i);

            GaiaRectangle rect = hole.getBoundingRectangle();
            Vector2d minPoint = rect.getLeftBottomPoint();

            double squareDist = extMinPoint.distanceSquared(minPoint);
            mapDistHolePolygon.put(squareDist, hole);
        }

        // must sort the mapDistHolePolygon from small to big squaredDist.***
        List<Polygon2DTess> sortedPolygons = new ArrayList<>();
        mapDistHolePolygon.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEachOrdered(x -> sortedPolygons.add(x.getValue()));


        // traverse map.
        int keysCount = sortedPolygons.size();

        for(int i=0; i<keysCount; i++)
        {
            Polygon2DTess hole = sortedPolygons.get(i);
            Polygon2DTess resultPolygon = eliminateHole(exteriorPolygon, hole, null);
            exteriorPolygon = resultPolygon;
        }

        List<Polygon2DTess> resultConvexPolygons = new ArrayList<>();
        this.tessellate2D(exteriorPolygon, resultConvexPolygons);
        exteriorPolygon.setPointsIdxInList();


        // finally create the resultPolygon.***
        Polygon2DTess resultPolygon = new Polygon2DTess(new ArrayList<>());
        resultPolygon.getPoints().addAll(exteriorPolygon.getPoints());

        int convexPolygonsCount = resultConvexPolygons.size();
        for(int i=0; i<convexPolygonsCount; i++)
        {
            Polygon2DTess convexPolygon = resultConvexPolygons.get(i);
            List<Integer> convexIndices = new ArrayList<>();
            convexPolygon.getTrianglesIndicesAsConvexPolygon(convexIndices);

            int convexIndicesCount = convexIndices.size();
            for(int j=0; j<convexIndicesCount; j+=1)
            {
                int idx = convexIndices.get(j);
                Point2DTess point2D = convexPolygon.getPoint(idx);
                int idxInList = point2D.getIdxInList();
                resultIndices.add(idxInList);
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
