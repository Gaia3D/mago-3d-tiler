package com.gaia3d.converter.geometry;

import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import org.locationtech.jts.geom.Coordinate;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class InnerRingRemover {

    public Coordinate[] removeAll(Coordinate[] outerRing, List<Coordinate[]> innerRings) {
        List<Coordinate> outerRingList = List.of(outerRing);
        List<Vector2d> outerRingVector = outerRingList.stream()
                .map(c -> new Vector2d(c.x, c.y))
                .collect(Collectors.toList());
        outerRingVector = filterCoordinates(outerRingVector);
        //outerRingVector.remove(outerRingVector.size() - 1); // remove last element

        List<InnerRing> innerRingObjects = innerRings.stream().map(innerRing -> {
            List<Vector2d> innerRingVector = List.of(innerRing).stream()
                    .map(c -> new Vector2d(c.x, c.y))
                    .collect(Collectors.toList());
            //innerRingVector.remove(innerRingVector.size() - 1); // remove last element

            innerRingVector = filterCoordinates(innerRingVector);
            return new InnerRing(innerRingVector);
        }).collect(Collectors.toList());

        innerRingObjects = innerRingObjects.stream().sorted((a, b) -> {
            return Double.compare(a.getMinValue(), b.getMinValue());
        }).collect(Collectors.toList());


        for (InnerRing innerRingObject : innerRingObjects) {
            outerRingVector = removeRing(outerRingVector, innerRingObject);
        }

        List<Coordinate> result = outerRingVector.stream()
                .map(v -> new Coordinate(v.x, v.y))
                .collect(Collectors.toList());
        outerRing = result.toArray(new Coordinate[0]);

        return outerRing;
    }

    public List<Vector2d> removeRing(List<Vector2d> outerRingVector, InnerRing innerRing) {
        /*Vector2d inneringLeftDown = innerRingVector.stream().sorted((a, b) -> {
            double positionA = a.x + a.y;
            double positionB = b.x + b.y;
            return Double.compare(positionA, positionB);
        }).findFirst().orElse(null);
        innerRingVector = changeOrder(innerRingVector, innerRingVector.indexOf(inneringLeftDown));
        innerRingVector.add(inneringLeftDown);*/

        List<Vector2d> innerRingVector = innerRing.getCoordinates();
        Vector2d inneringLeftDown = innerRing.getLeftDown();

        List<Vector2d> nearestOuterRings = outerRingVector.stream().sorted((a, b) -> {
            double distanceA = a.distance(inneringLeftDown);
            double distanceB = b.distance(inneringLeftDown);
            return Double.compare(distanceA, distanceB);
        }).collect(Collectors.toList());

        // check intersect
        Vector2d nearestOuterRing = null;
        for (int i = 0; i < nearestOuterRings.size(); i++) {
            nearestOuterRing = nearestOuterRings.get(i);
            boolean isIntersect = false;
            for (int j = 0; j < innerRingVector.size() - 1; j++) {
                Vector2d innerRingVectorA = innerRingVector.get(j);
                Vector2d innerRingVectorB = innerRingVector.get(j + 1);
                if (isIntersect(inneringLeftDown, nearestOuterRing, innerRingVectorA, innerRingVectorB)) {
                    isIntersect = true;
                }
            }
            Vector2d innerRingVectorA = innerRingVector.get(innerRingVector.size() - 1);
            Vector2d innerRingVectorB = innerRingVector.get(0);
            if (isIntersect(inneringLeftDown, nearestOuterRing, innerRingVectorA, innerRingVectorB)) {
                isIntersect = true;
            }

            for (int j = 0; j < outerRingVector.size() - 1; j++) {
                Vector2d outerRingVectorA = outerRingVector.get(j);
                Vector2d outerRingVectorB = outerRingVector.get(j + 1);
                if (isIntersect(inneringLeftDown, nearestOuterRing, outerRingVectorA, outerRingVectorB)) {
                    isIntersect = true;
                }
            }
            Vector2d outerRingVectorA = outerRingVector.get(outerRingVector.size() - 1);
            Vector2d outerRingVectorB = outerRingVector.get(0);
            if (isIntersect(inneringLeftDown, nearestOuterRing, outerRingVectorA, outerRingVectorB)) {
                isIntersect = true;
            }

            if (!isIntersect) {
                break;
            }
        }
        //nearestOuterRing = nearestOuterRings.get(0);
        outerRingVector = changeOrder(outerRingVector, outerRingVector.indexOf(nearestOuterRing));
        //outerRingVector.add(nearestOuterRing);

        /*Vector2d nearestOuterRing = outerRingVector.stream().sorted((a, b) -> {
            double distanceA = a.distance(inneringLeftDown);
            double distanceB = b.distance(inneringLeftDown);
            return -Double.compare(distanceB, distanceA);
        }).findFirst().orElse(null);
        outerRingVector = changeOrder(outerRingVector, outerRingVector.indexOf(nearestOuterRing));
        outerRingVector.add(nearestOuterRing);*/


        /*for (int j = 0; j < innerRingVector.size() - 1; j++) {
            Vector2d innerRingVectorA = innerRingVector.get(j);
            Vector2d innerRingVectorB = innerRingVector.get(j + 1);
            if (isIntersect(inneringLeftDown, nearestOuterRing, innerRingVectorA, innerRingVectorB)) {
                log.info("intersect : {}", j);
            }
        }*/
        //int nearestIndex = outerRingVector.indexOf(nearest);

        List<Vector2d> combinedVectors = combine(outerRingVector, innerRingVector);
        return combinedVectors;
    }

    public List<Vector2d> combine(List<Vector2d> outerRing, List<Vector2d> innerRing) {
        List<Vector2d> result = new ArrayList<>();
        result.addAll(outerRing);
        //result.add(outerRing.get(0));
        result.addAll(innerRing);
        result.add(outerRing.get(0));
        return result;
    }

    public List<Vector2d> changeOrder(List<Vector2d> list, int index) {
        List<Vector2d> result = list.subList(index, list.size());
        result.addAll(list.subList(0, index));
        return result;
    }

    public double cross(Vector2d a, Vector2d b, Vector2d c) {
        Vector2d ab = a.sub(b, new Vector2d());
        ab.normalize();
        Vector2d bc = b.sub(c, new Vector2d());
        bc.normalize();
        return cross(ab, bc);
    }

    /* Vector2d Cross Product */
    public double cross(Vector2d a, Vector2d b) {
        return (a.x * b.y) - (a.y * b.x);
    }

    public boolean isIntersect(Vector2d a, Vector2d b, Vector2d u, Vector2d v) {
//        if ((a.equals(u) && b.equals(v)) || (a.equals(v) && b.equals(u))) {
//            log.info("SAME INTERSECT, {}:{}:{}:{}", a, b, u, v);
//            return true;
//        }

        double cross1 = cross(a, b, u);
        double cross2 = cross(a, b, v);
//        if (cross1 == 0 && cross2 == 0) {
//            return true;
//        }
        boolean isIntersectA = cross1 * cross2 < 0;

        double cross3 = cross(u, v, a);
        double cross4 = cross(u, v, b);
//        if (cross3 == 0 && cross4 == 0) {
//            return true;
//        }
        boolean isIntersectB = cross3 * cross4 < 0;

        return isIntersectA && isIntersectB;
    }

    public boolean isIntersect(Vector2d a, Vector2d b, Vector2d u) {
        if (a.equals(u) || b.equals(u)) {
            return false;
        }
        double cross1 = cross(a, b, u);
        return cross1 == 0;
    }


    private List<Vector2d> filterCoordinates(List<Vector2d> coordinates) {
        if (coordinates.get(0).equals(coordinates.get(coordinates.size() - 1))) {
            coordinates.remove(coordinates.size() - 1);
        }

        List<Vector2d> result = new ArrayList<>();
        int length = coordinates.size();
        for (int i = 0; i < length; i++) {
            int index1 = (i - 1) % length < 0 ? length - 1 : (i - 1) % length;
            int index2 = i % length;
            int index3 = (i + 1) % length;
            Vector2d prev = coordinates.get(index1);
            Vector2d crnt = coordinates.get(index2);
            Vector2d next = coordinates.get(index3);

            double cross = cross(prev, crnt, next);
            if (crnt.equals(prev) || crnt.equals(next)) {
                //log.info("SAME POINTS, {}:{}:{}", crnt, prev, next);
                continue;
            } else if (Double.isNaN(cross)) {
                //log.info("CROSS IS NAN, {}:{}:{}", crnt, prev, next);
                continue;
            } else if (cross == 0) {
                //log.info("CROSS ZERO, {}:{}:{}", crnt, prev, next);
                continue;
            } else if (Math.abs(cross) < 0.01) {
                //log.info("CROSS NEGATIVE, {}:{}:{}", crnt, prev, next);
                continue;
            }
            result.add(crnt);
        }
        result.add(result.get(0));
        return result;
    }
}
