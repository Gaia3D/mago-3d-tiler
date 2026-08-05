package com.gaia3d.converter;

import com.gaia3d.basic.geometry.tessellator.Point2DTess;
import com.gaia3d.basic.geometry.tessellator.Segment2DTess;
import com.gaia3d.util.VectorUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import org.locationtech.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class InnerRingRemover {

    public Coordinate[] removeAll(Coordinate[] outerRing, List<Coordinate[]> innerRings) {
        PolygonFilter polygonFilter = new PolygonFilter();

        List<Coordinate> outerRingList = List.of(outerRing);
        List<Vector2d> outerRingVector = outerRingList.stream()
                .map(c -> new Vector2d(c.x, c.y))
                .collect(Collectors.toList());
        outerRingVector = polygonFilter.filter(outerRingVector);

        List<InnerRing> innerRingObjects = innerRings.stream().map(innerRing -> {
            List<Vector2d> innerRingVector = List.of(innerRing).stream()
                    .map(c -> new Vector2d(c.x, c.y))
                    .collect(Collectors.toList());
            innerRingVector = polygonFilter.filter(innerRingVector);
            return new InnerRing(innerRingVector);
        }).collect(Collectors.toList());

        innerRingObjects = innerRingObjects.stream().sorted((a, b) -> Double.compare(a.getMinValue(), b.getMinValue())).toList();

        for (InnerRing innerRingObject : innerRingObjects) {
            outerRingVector = removeRing(outerRingVector, innerRingObject);
        }

        List<Coordinate> result = outerRingVector.stream()
                .map(v -> new Coordinate(v.x, v.y))
                .toList();
        outerRing = result.toArray(new Coordinate[0]);

        return outerRing;
    }

    public List<Vector2d> removeRing(List<Vector2d> outerRingVector, InnerRing innerRing) {
        List<Vector2d> innerRingVector = innerRing.getCoordinates();
        Vector2d inneringLeftDown = innerRing.getLeftDown();

        List<Vector2d> nearestOuterRings = outerRingVector.stream().sorted((a, b) -> {
            double distanceA = a.distance(inneringLeftDown);
            double distanceB = b.distance(inneringLeftDown);
            return Double.compare(distanceA, distanceB);
        }).toList();

        Point2DTess leftDownPoint = new Point2DTess(inneringLeftDown, null, null);
        double error = 1E-10;

        Vector2d nearestOuterRing = null;
        for (Vector2d outerRing : nearestOuterRings) {
            nearestOuterRing = outerRing;

            Point2DTess nearestOuterPoint = new Point2DTess(nearestOuterRing, null, null);
            Segment2DTess cuttingSegment = new Segment2DTess(leftDownPoint, nearestOuterPoint);

            Point2DTess vectorA, vectorB;
            Segment2DTess innerRingSegement;
            Point2DTess intersectionPoint;

            boolean isIntersect = false;

            for (int j = 0; j < innerRingVector.size() - 1; j++) {
                Vector2d innerRingVectorA = innerRingVector.get(j);
                Vector2d innerRingVectorB = innerRingVector.get(j + 1);

                vectorA = new Point2DTess(innerRingVectorA, null, null);
                vectorB = new Point2DTess(innerRingVectorB, null, null);
                innerRingSegement = new Segment2DTess(vectorA, vectorB);
                intersectionPoint = new Point2DTess(null, null, null);
                if (cuttingSegment.intersectionWithSegment(innerRingSegement, intersectionPoint, error) == 1) {
                    isIntersect = true;
                }
            }

            Vector2d innerRingVectorA = innerRingVector.get(innerRingVector.size() - 1);
            Vector2d innerRingVectorB = innerRingVector.get(0);
            vectorA = new Point2DTess(innerRingVectorA, null, null);
            vectorB = new Point2DTess(innerRingVectorB, null, null);
            innerRingSegement = new Segment2DTess(vectorA, vectorB);
            intersectionPoint = new Point2DTess(null, null, null);
            if (cuttingSegment.intersectionWithSegment(innerRingSegement, intersectionPoint, error) == 1) {
                isIntersect = true;
            }

            for (int j = 0; j < outerRingVector.size() - 1; j++) {
                Vector2d outerRingVectorA = outerRingVector.get(j);
                Vector2d outerRingVectorB = outerRingVector.get(j + 1);

                vectorA = new Point2DTess(outerRingVectorA, null, null);
                vectorB = new Point2DTess(outerRingVectorB, null, null);
                innerRingSegement = new Segment2DTess(vectorA, vectorB);
                intersectionPoint = new Point2DTess(null, null, null);
                if (cuttingSegment.intersectionWithSegment(innerRingSegement, intersectionPoint, error) == 1) {
                    isIntersect = true;
                }
            }

            Vector2d outerRingVectorA = outerRingVector.get(outerRingVector.size() - 1);
            Vector2d outerRingVectorB = outerRingVector.get(0);

            vectorA = new Point2DTess(outerRingVectorA, null, null);
            vectorB = new Point2DTess(outerRingVectorB, null, null);
            innerRingSegement = new Segment2DTess(vectorA, vectorB);
            intersectionPoint = new Point2DTess(null, null, null);
            if (cuttingSegment.intersectionWithSegment(innerRingSegement, intersectionPoint, error) == 1) {
                isIntersect = true;
            }

            if (!isIntersect) {
                break;
            }
        }
        outerRingVector = changeOrder(outerRingVector, outerRingVector.indexOf(nearestOuterRing));
        return combine(outerRingVector, innerRingVector);
    }

    public List<Vector2d> combine(List<Vector2d> outerRing, List<Vector2d> innerRing) {
        List<Vector2d> result = new ArrayList<>();
        result.addAll(outerRing);
        result.addAll(innerRing);
        result.add(new Vector2d(outerRing.get(0)));
        return result;
    }

    public List<Vector2d> changeOrder(List<Vector2d> list, int index) {
        if (list.get(0).equals(list.get(list.size() - 1))) {
            list.remove(list.size() - 1);
        }

        List<Vector2d> result = list.subList(index, list.size());
        result.addAll(list.subList(0, index));

        if (!result.isEmpty()) {
            result.add(new Vector2d(result.get(0)));
        }
        return result;
    }

    public double cross(Vector2d a, Vector2d b, Vector2d c) {
        Vector2d ab = a.sub(b, new Vector2d());
        ab.normalize();
        Vector2d bc = b.sub(c, new Vector2d());
        bc.normalize();
        return VectorUtils.cross(ab, bc);
    }
}
