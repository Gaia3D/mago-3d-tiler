package com.gaia3d.converter.geometry.extrusion;

import com.gaia3d.converter.geometry.GaiaTriangle;
import com.gaia3d.util.VectorUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Deprecated
public class Tessellator2D {

    public List<GaiaTriangle> tessellate(List<Vector3d> positions3D, double altitude) {
        List<Vector2d> positions = positions3D.stream().map(p -> new Vector2d(p.x, p.y)).collect(Collectors.toList());

        startEnd(positions);
        boolean isCCW = this.validateAngle(positions);
        if (!isCCW) {
            log.warn("IS CCW POLYGON");
            Collections.reverse(positions);
        }

        List<GaiaTriangle> result = new ArrayList<>();
        List<List<Vector2d>> convexes = convertConvex(null, positions);
        convexes.forEach((convex) -> {
            List<GaiaTriangle> triangles = convertTriangles(convex, altitude);
            result.addAll(triangles);
        });

        return result;
    }

    private void startEnd(List<Vector2d> positions) {
        Vector2d start = positions.get(0);
        Vector2d end = positions.get(positions.size() - 1);
        if (start.equals(end)) {
            positions.remove(positions.size() - 1);
        }
    }

    private List<GaiaTriangle> convertTriangles(List<Vector2d> positions, double height) {
        List<GaiaTriangle> result = new ArrayList<>();
        for (int i = 0; i < positions.size() - 2; i++) {
            Vector3d vec3A = new Vector3d(positions.get(0).x, positions.get(0).y, height);
            Vector3d vec3B = new Vector3d(positions.get(i + 1).x, positions.get(i + 1).y, height);
            Vector3d vec3C = new Vector3d(positions.get(i + 2).x, positions.get(i + 2).y, height);
            GaiaTriangle triangle = new GaiaTriangle(vec3A, vec3B, vec3C);
            result.add(triangle);
        }
        return result;
    }

    private List<List<Vector2d>> convertConvex(List<List<Vector2d>> result, List<Vector2d> positions) {
        if (result == null) {
            result = new ArrayList<>();
        }
        if (isConvex(positions)) {
            result.add(positions);
        } else {
            Vector2d clockWisePosition = getClockWisePosition(positions);
            if (clockWisePosition == null) {
                return result;
            }

            int clockWiseIndex = positions.indexOf(clockWisePosition);
            List<Vector2d> nearestPositions = sortNearest(positions, clockWiseIndex);

            boolean isSuccess = false;
            for (Vector2d nearestPosition : nearestPositions) {
                List<List<Vector2d>> polygons = splitConvex(positions, clockWisePosition, nearestPosition);
                List<Vector2d> splitA = polygons.get(0);
                List<Vector2d> splitB = polygons.get(1);

                boolean isIntersection = findIntersection(positions, clockWisePosition, nearestPosition);
                if (!isIntersection) {
                    boolean angleA = this.validateAngle(polygons.get(0));
                    boolean angleB = this.validateAngle(polygons.get(1));
                    if (angleA == angleB) {
                        convertConvex(result, polygons.get(0));
                        convertConvex(result, polygons.get(1));
                        isSuccess = true;
                        break;
                    }
                }
            }

            if (!isSuccess) {
                log.warn("is FAILED");
            }
        }
        return result;
    }

    private List<List<Vector2d>> splitConvex(List<Vector2d> positions, Vector2d positionA, Vector2d positionB) {
        List<List<Vector2d>> result = new ArrayList<>();
        result.add(createSplits(positions, positionA, positionB));
        result.add(createSplits(positions, positionB, positionA));
        return result;
    }

    private List<Vector2d> createSplits(List<Vector2d> positions, Vector2d startPosition, Vector2d endPosition) {
        List<Vector2d> result = new ArrayList<>();
        result.add(startPosition);
        result.add(endPosition);
        int index = positions.indexOf(endPosition);
        for (int i = 0; i < positions.size() - 1; i++) {
            int crnt = index % positions.size();
            int next = (index + 1) % positions.size();
            Vector2d crntPosition = positions.get(crnt);
            Vector2d nextPosition = positions.get(next);
            if (nextPosition == startPosition || nextPosition == endPosition) {
                break;
            } else if (!crntPosition.equals(nextPosition)) {
                result.add(nextPosition);
            }
            index++;
        }
        return result;
    }

    private List<Vector2d> sortNearest(List<Vector2d> positions, int clockWiseIndex) {
        Vector2d prevPosition = positions.get((clockWiseIndex - 1) % positions.size() < 0 ? positions.size() - 1 : (clockWiseIndex - 1) % positions.size());
        Vector2d clockWisePosition = positions.get(clockWiseIndex);
        Vector2d nextPosition = positions.get((clockWiseIndex + 1) % positions.size());

        List<Vector2d> result = new ArrayList<>();

        for (Vector2d position : positions) {
            if (position != prevPosition && position != clockWisePosition && position != nextPosition) {
                result.add(position);
            }
        }

        result.sort((p1, p2) -> {
            double d1 = clockWisePosition.distanceSquared(p1);
            double d2 = clockWisePosition.distanceSquared(p2);
            return Double.compare(d1, d2);
        });

        return result;
    }

    private Vector2d getClockWisePosition(List<Vector2d> positions) {
        for (int i = 0; i < positions.size(); i++) {
            int index1 = (i - 1) % positions.size() < 0 ? positions.size() - 1 : (i - 1) % positions.size();
            int index2 = i % positions.size();
            int index3 = (i + 1) % positions.size();
            Vector2d position1 = positions.get(index1);
            Vector2d position2 = positions.get(index2);
            Vector2d position3 = positions.get(index3);
            double normal = VectorUtils.cross(position1, position2, position3);
            if (normal < 0) {
                return position2;
            }
        }
        return null;
    }

    private boolean isConvex(List<Vector2d> positions) {
        for (int i = 0; i < positions.size(); i++) {
            int index1 = (i - 1) % positions.size() < 0 ? positions.size() - 1 : (i - 1) % positions.size();
            int index2 = i % positions.size();
            int index3 = (i + 1) % positions.size();

            Vector2d position1 = positions.get(index1);
            Vector2d position2 = positions.get(index2);
            Vector2d position3 = positions.get(index3);

            double normal = VectorUtils.cross(position1, position2, position3);
            if (normal < 0) {
                return false;
            }
        }
        return true;
    }

    private boolean validateAngle(List<Vector2d> positions) {
        double angleSum = 0;
        double resverseAngleSum = 0;
        for (int i = 0; i < positions.size() - 1; i++) {
            int index1 = (i - 1) % positions.size() < 0 ? positions.size() - 1 : (i - 1) % positions.size();
            int index2 = i % positions.size();
            int index3 = (i + 1) % positions.size();
            double normal = VectorUtils.cross(positions.get(index1), positions.get(index2), positions.get(index3));
            double angle = VectorUtils.calcAngle(positions.get(index1), positions.get(index2), positions.get(index3));
            if (normal > 0) {
                angleSum += angle;
            } else {
                resverseAngleSum += angle;
            }
        }
        return angleSum > resverseAngleSum;
    }


    private boolean findIntersection(List<Vector2d> positions, Vector2d startPosition, Vector2d endPosition) {
        for (int index = 0; index < positions.size(); index++) {
            int next = (index + 1) % positions.size();
            Vector2d crntPosition = positions.get(index);
            Vector2d nextPosition = positions.get(next);

            if (startPosition.equals(crntPosition) || endPosition.equals(nextPosition)) {
                log.info("SAME INTERSECT1, {}:{}:{}:{}", startPosition, endPosition, crntPosition, nextPosition);
                return true;
            }
            if (startPosition.equals(nextPosition) || endPosition.equals(crntPosition)) {
                log.info("SAME INTERSECT2, {}:{}:{}:{}", startPosition, endPosition, crntPosition, nextPosition);
                return true;
            }


            if (VectorUtils.isIntersection(startPosition, endPosition, crntPosition, nextPosition)) {
                return true;
            }
            if (VectorUtils.isIntersection(startPosition, endPosition, crntPosition)) {
                return true;
            }
        }
        return false;
    }
}
