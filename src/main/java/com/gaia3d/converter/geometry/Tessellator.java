package com.gaia3d.converter.geometry;

import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.*;

@Slf4j
public class Tessellator {

    public List<GaiaTriangle> tessellate(List<Vector3d> positions) {
        startEnd(positions);

        boolean isCCW = this.validateAngle(positions);
        if (!isCCW) {
            // reverse array
            Collections.reverse(positions);
        }

        List<GaiaTriangle> result = new ArrayList<>();
        List<List<Vector3d>> convexes = convertConvex(null, positions);
        convexes.forEach((convex) -> {
            List<GaiaTriangle> triangles = convertTriangles(convex);
            result.addAll(triangles);
        });
        return result;
    }

    private void startEnd(List<Vector3d> positions) {
        Vector3d start = positions.get(0);
        Vector3d end = positions.get(positions.size() - 1);
        if (start.equals(end)) {
            positions.remove(positions.size() - 1);
        }
    }

    private List<GaiaTriangle> convertTriangles(List<Vector3d> positions) {
        List<GaiaTriangle> result = new ArrayList<>();
        for (int i = 0; i < positions.size() - 2; i++) {
            Vector3d position1 = positions.get(0);
            Vector3d position2 = positions.get(i + 1);
            Vector3d position3 = positions.get(i + 2);
            GaiaTriangle triangle = new GaiaTriangle(position1, position2, position3);
            result.add(triangle);
        }
        return result;
    }

    private List<List<Vector3d>> convertConvex(List<List<Vector3d>> result, List<Vector3d> positions) {
        if (result == null) {
            result = new ArrayList<>();
        }
        //log.warn("new");
        if (isConvex(positions)) {
            //log.warn("isConvex");
            result.add(positions);
        } else {
            //log.warn("isConcave");
            Vector3d clockWisePosition = getClockWisePosition(positions);
            if (clockWisePosition == null) {
                return result;
            }

            int clockWiseIndex = positions.indexOf(clockWisePosition);

            List<Vector3d> nearestPositions = sortNearest(positions, clockWiseIndex);
            for (Vector3d nearestPosition : nearestPositions) {
                List<List<Vector3d>> polygons = splitConvex(positions, clockWisePosition, nearestPosition);

                boolean isIntersection = findIntersection(positions, clockWisePosition, nearestPosition);
                if (!isIntersection) {
                    boolean angleA = this.validateAngle(polygons.get(0));
                    boolean angleB = this.validateAngle(polygons.get(1));
                    if (angleA == angleB) {
                        convertConvex(result, polygons.get(0));
                        convertConvex(result, polygons.get(1));
                        break;
                    }
                }
            }
        }

        //List<Vector3d> convex = positions; // temp only convex for test
        //result.add(convex);
        return result;
    }

    private List<List<Vector3d>> splitConvex(List<Vector3d> positions, Vector3d positionA, Vector3d positionB) {
        List<List<Vector3d>> result = new ArrayList<>();
        result.add(createSplits(positions, positionA, positionB));
        result.add(createSplits(positions, positionB, positionA));
        return result;
    }

    private List<Vector3d> createSplits(List<Vector3d> positions, Vector3d startPosition, Vector3d endPosition) {
        List<Vector3d> result = new ArrayList<>();
        result.add(startPosition);
        result.add(endPosition);
        int index = positions.indexOf(endPosition);
        for (int i = 0; i < positions.size() - 1; i++) {
            int crnt = index % positions.size();
            int next = (index + 1) % positions.size();
            Vector3d crntPosition = positions.get(crnt);
            Vector3d nextPosition = positions.get(next);
            if (nextPosition == startPosition || nextPosition == endPosition) {
                break;
            } else if (!crntPosition.equals(nextPosition)) {
                result.add(nextPosition);
            }
            index++;
        }
        return result;
    }

    private List<Vector3d> sortNearest(List<Vector3d> positions, int clockWiseIndex) {
        Vector3d prevPosition = positions.get((clockWiseIndex - 1) % positions.size() < 0 ? positions.size() - 1 : (clockWiseIndex - 1) % positions.size());
        Vector3d clockWisePosition = positions.get(clockWiseIndex);
        Vector3d nextPosition = positions.get((clockWiseIndex + 1) % positions.size());

        List<Vector3d> result = new ArrayList<>();

        for (Vector3d position : positions) {
            if (position != prevPosition && position != clockWisePosition && position != nextPosition) {
                result.add(position);
            }
        }

        Collections.sort(result, (p1, p2) -> {
            double d1 = clockWisePosition.distanceSquared(p1);
            double d2 = clockWisePosition.distanceSquared(p2);
            if (d1 < d2) {
                return -1;
            } else if (d1 > d2) {
                return 1;
            } else {
                return 0;
            }
        });


        /*result = result.stream().sorted(Comparator.comparing((position) -> {
            return clockWisePosition.distance(positions.get(clockWiseIndex));
        })).collect(Collectors.toList());*/
        return result;
    }

    private Vector3d getClockWisePosition(List<Vector3d> positions) {
        for (int i = 0; i < positions.size(); i++) {
            int index1 = (i - 1) % positions.size() < 0 ? positions.size() - 1 : (i - 1) % positions.size();
            int index2 = i % positions.size();
            int index3 = (i + 1) % positions.size();
            Vector3d position1 = positions.get(index1);
            Vector3d position2 = positions.get(index2);
            Vector3d position3 = positions.get(index3);
            Vector3d normal = calcNormal(position1, position2, position3);
            if (isClockWise(normal)) {
                return position2;
            }
        }
        return null;
    }

    private boolean isConvex(List<Vector3d> positions) {
        for (int i = 0; i < positions.size(); i++) {
            int index1 = (i - 1) % positions.size() < 0 ? positions.size() - 1 : (i - 1) % positions.size();
            int index2 = i % positions.size();
            int index3 = (i + 1) % positions.size();
            //log.info("index1: {}, index2: {}, index3: {}", index1, index2, index3);

            Vector3d position1 = positions.get(index1);
            Vector3d position2 = positions.get(index2);
            Vector3d position3 = positions.get(index3);
            Vector3d normal = calcNormal(position1, position2, position3);
            if (isClockWise(normal)) {
                return false;
            }
        }
        return true;
    }

    private boolean validateAngle(List<Vector3d> positions) {
        double angleSum = 0;
        double resverseAngleSum = 0;
        for (int i = 0; i < positions.size() - 1; i++) {
            int index1 = (i - 1) % positions.size() < 0 ? positions.size() - 1 : (i - 1) % positions.size();
            int index2 = i % positions.size();
            int index3 = (i + 1) % positions.size();
            double normal = calcNormal(positions.get(index1), positions.get(index2), positions.get(index3)).get(2);
            double angle = calcAngle(positions.get(index1), positions.get(index2), positions.get(index3));
            if (normal > 0) {
                angleSum += angle;
            } else {
                resverseAngleSum += angle;
            }
        }
        return angleSum > resverseAngleSum;
    }

    private boolean isClockWise(Vector3d normal) {
        return normal.get(2) < 0;
    }

    private double calcAngle(Vector3d a, Vector3d b, Vector3d c) {
        Vector3d v1 = new Vector3d();
        Vector3d v2 = new Vector3d();
        b.sub(a, v1);
        c.sub(b, v2);
        return Math.toDegrees(v1.angle(v2));
    }

    private Vector3d calcNormal(Vector3d a, Vector3d b, Vector3d c) {
        Vector3d c1 = cross(a, b, c);
        c1.normalize();
        return c1;
    }

    private Vector3d cross(Vector3d a, Vector3d b, Vector3d c) {
        Vector3d v1 = new Vector3d();
        Vector3d v2 = new Vector3d();
        b.sub(a, v1);
        c.sub(b, v2);
        Vector3d c1 = new Vector3d();
        v1.cross(v2, c1);
        return c1;
    }

    private boolean findIntersection(List<Vector3d> positions, Vector3d startPosition, Vector3d endPosition) {
        for (int index = 0; index < positions.size(); index++) {
            int next = (index + 1) % positions.size();
            Vector3d crntPosition = positions.get(index);
            Vector3d nextPosition = positions.get(next);
            if (isIntersection(startPosition, endPosition, crntPosition, nextPosition)) {
                return true;
            }

            if (isIntersection(startPosition, endPosition, crntPosition)) {
                return true;
            }
        }
        return false;
    }

    private boolean isIntersection(Vector3d a1, Vector3d a2, Vector3d b1, Vector3d b2) {
        Vector3d c1 = cross(a1, a2, b1);
        Vector3d c2 = cross(a1, a2, b2);
        Vector3d c3 = cross(b1, b2, a1);
        Vector3d c4 = cross(b1, b2, a2);
        double d1 = c1.dot(c2);
        double d2 = c3.dot(c4);
        if (d1 == 0 && d2 == 0) {
            return false;
        } else {
            return d1 <= 0 && d2 <= 0;
        }
    }

    // find intersection point
    /*private boolean isIntersection(Vector3d a1, Vector3d a2, Vector3d p1) {
        Vector3d c1 = cross(a1, a2, p1);
        if (c1.z() == 0) {
            return true;
        }
        return false;
    }*/

    // find intersection point
    private boolean isIntersection(Vector3d a1, Vector3d a2, Vector3d p1) {
        if (a1.equals(p1) || a2.equals(p1)) {
            return false;
        }
        Vector3d c1 = cross(a1, a2, p1);
        if (c1.z() == 0) {
            return true;
        }
        return false;
    }
}
