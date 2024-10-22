package com.gaia3d.basic.geometry.tessellator;

import com.gaia3d.basic.geometry.GaiaRectangle;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Getter
@Setter
public class Polygon2DTess {
    public List<Point2DTess> points;
    public GaiaRectangle boundingRect;

    public Polygon2DTess(List<Point2DTess> points) {
        this.points = points;
    }

    public Point2DTess getPoint(int index) {
        return points.get(index);
    }

    public void getTrianglesIndicesAsConvexPolygon(List<Integer> resultConvexIndices) {
        // get the triangles indices as convex polygon.***
        int pointsCount = points.size();
        if (pointsCount < 3) {
            return;
        }

        // in a convexPolygon, the triangle indices are (0, 1, 2), (0, 2, 3), (0, 3, 4), ... (0, n-2, n-1).***
        for (int i = 2; i < pointsCount; i++) {
            resultConvexIndices.add(0);
            resultConvexIndices.add(i - 1);
            resultConvexIndices.add(i);
        }
    }

    public void setPointsIdxInList() {
        int pointsCount = points.size();
        for (int i = 0; i < pointsCount; i++) {
            points.get(i).setIdxInList(i);
        }
    }

    private boolean isInvalidVector(Vector2d vector) {
        return !isValidVector(vector);
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

    public void reverse() {
        Collections.reverse(points);
    }

    public float calculateNormal2D(List<Integer> resultConcaveIndices) {
        // calculate the normal of the polygon.***
        int pointsCount = points.size();
        if (pointsCount < 3) {
            return 0.0f;
        }

        float normal = 0.0f;
        float angRadSum = 0.0f;
        List<Integer> positivePoints = new ArrayList<>();
        List<Integer> negativePoints = new ArrayList<>();

        double error = 1.0e-10;

        for (int i = 0; i < pointsCount; i++) {
            int idxNext = getNextIdx(i);
            int idxPrev = getPrevIdx(i);
            Vector2d currPoint = getPoint(i).getPoint();
            Vector2d nextPoint = getPoint(idxNext).getPoint();
            Vector2d prevPoint = getPoint(idxPrev).getPoint();

            Vector2d v1 = new Vector2d();
            Vector2d v2 = new Vector2d();
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

            double dot = v1.dot(v2);

            // if dot == 1.0, then this point is a convex point.***
            if (dot > 1.0 - error) {
                continue;
            }

            // if cross > 0 => CCW, if cross < 0 => CW.***
            double cross = v1.x * v2.y - v1.y * v2.x;
            if (cross < 0) {
                cross = -1.0;
                negativePoints.add(i);
            } else {
                cross = 1.0;
                positivePoints.add(i);
            }

            double angRad = Math.acos(dot); // because v1 and v2 are normalized.***
            angRadSum += (float) (angRad * cross);

//            // TODO: check if this is necessary.***
//            if (Math.abs(normal) < 1e-5) {
//                normal = 0.0f;
//                return normal;
//            }
        }

        float angRadSumError = 1e-4F;
        if (Math.abs(angRadSum) < angRadSumError) {
            // 1e-6 works ok.***
            // probably the polygon is a line, or a self-intersecting polygon (butterfly polygon).***
            normal = 0.0f;
            return normal;
        }

        normal = angRadSum / Math.abs(angRadSum);

        if (normal > 0.0f) {
            normal = 1.0f;
            resultConcaveIndices.addAll(negativePoints);
        } else if (normal < 0.0f) {
            normal = -1.0f;
            resultConcaveIndices.addAll(positivePoints);
        }

        return normal;
    }

    public int getPointsCount() {
        return points.size();
    }

    public Segment2DTess getSegment2DTess(int idx, Segment2DTess resultSegment) {
        // get the segment of the polygon.***
        int pointsCount = points.size();
        if (pointsCount < 3) {
            return null;
        }

        int i1 = idx;
        int i2 = (idx + 1) % pointsCount;
        if (resultSegment == null) {
            resultSegment = new Segment2DTess(points.get(i1), points.get(i2));
        } else {
            resultSegment.setPoint1(points.get(i1));
            resultSegment.setPoint2(points.get(i2));
        }

        return resultSegment;
    }

    public boolean isSegmentIntersectingPolygon(Segment2DTess segment, double error) {
        // check if the segment is intersecting the polygon.***
        // 0 = no intersection, 1 = intersection.***
        int pointsCount = points.size();
        if (pointsCount < 3) {
            return false;
        }

        //int intersectionsCount = 0;
        Point2DTess intersectionPoint = new Point2DTess(null, null, null);
        Segment2DTess polygonSegment = new Segment2DTess(null, null);
        for (int i = 0; i < pointsCount; i++) {
            //*********************************************************************
            // 0 = no intersection,
            // 1 = intersection point is inside the both segments,
            // 2 = intersection point is the start point of this segment,
            // 3 = intersection point is the end point of this segment,
            // 4 = intersection point is the start point of the segment,
            // 5 = intersection point is the end point of the segment.
            // 6 = lines are collinear.
            //*********************************************************************

            getSegment2DTess(i, polygonSegment);
            int intersectionType = polygonSegment.intersectionWithSegment(segment, intersectionPoint, error);
            if (intersectionType == 1 || intersectionType == 6) {
                return true;
            }
        }

        return false;
    }

    public int getNextIdx(int currIdx) {
        int pointsCount = points.size();
        return (currIdx + 1) % pointsCount;
    }

    public int getPrevIdx(int currIdx) {
        int pointsCount = points.size();
        return (currIdx - 1 + pointsCount) % pointsCount;
    }

    public GaiaRectangle getBoundingRectangle() {
        if (boundingRect == null) {
            boundingRect = new GaiaRectangle();

            int pointsCount = points.size();
            for (int i = 0; i < pointsCount; i++) {
                Vector2d currPoint = points.get(i).getPoint();
                if (i == 0) {
                    boundingRect.setInit(currPoint);
                } else {
                    boundingRect.addPoint(currPoint);
                }
            }
        }
        return boundingRect;
    }

    public int getMostLeftDownPoint2DIdx() {
        // the most leftDown point is the point that is closest to the leftDownPoint of the boundingRectangle.***
        GaiaRectangle boundingRect = getBoundingRectangle();
        Vector2d leftDownPoint = boundingRect.getLeftBottomPoint();
        //Point2DTess mostLeftDownPoint = null;
        int resultIdx = -1;
        double minDist = Double.MAX_VALUE;
        int pointsCount = points.size();
        for (int i = 0; i < pointsCount; i++) {
            Point2DTess point = points.get(i);
            Vector2d point2D = point.getPoint();
            double dist = leftDownPoint.distanceSquared(point2D);
            if (dist < minDist) {
                minDist = dist;
                resultIdx = i;
            }
        }

        return resultIdx;
    }

    public void splitPolygon(int idx1, int idx2, List<Polygon2DTess> resultSplittedPolygons) {
        // split this polygon in polygonA & polygonB.***
        // polygonA = (idx1, idx2), polygonB = (idx2, idx1).***
        int pointsCount = points.size();
        if (pointsCount < 3) {
            return;
        }

        List<Point2DTess> pointsA = new ArrayList<>();
        pointsA.add(points.get(idx1));
        pointsA.add(points.get(idx2));
        List<Point2DTess> pointsB = new ArrayList<>();
        pointsB.add(points.get(idx2));
        pointsB.add(points.get(idx1));

        // polygonA.***
        boolean finished = false;
        int currIdx = idx2;
        int startIdx = idx1;
        int i = 0;
        while (!finished && i < pointsCount) {
            int nextIdx = getNextIdx(currIdx);
            if (nextIdx == startIdx) {
                finished = true;
            } else {
                pointsA.add(points.get(nextIdx));
                currIdx = nextIdx;
            }
            i++;
        }

        // polygonB.***
        finished = false;
        currIdx = idx1;
        startIdx = idx2;
        i = 0;
        while (!finished && i < pointsCount) {
            int nextIdx = getNextIdx(currIdx);
            if (nextIdx == startIdx) {
                finished = true;
            } else {
                pointsB.add(points.get(nextIdx));
                currIdx = nextIdx;
            }
            i++;
        }

        Polygon2DTess polygonA = new Polygon2DTess(pointsA);
        Polygon2DTess polygonB = new Polygon2DTess(pointsB);

        resultSplittedPolygons.add(polygonA);
        resultSplittedPolygons.add(polygonB);
    }

    public void addPoint(Point2DTess point2DTess) {
        points.add(point2DTess);
    }

    public void removePoint(int i) {
        points.remove(i);
    }
}
