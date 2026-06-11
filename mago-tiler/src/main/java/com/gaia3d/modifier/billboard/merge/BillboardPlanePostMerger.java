package com.gaia3d.modifier.billboard.merge;

import com.gaia3d.basic.geometry.entities.GaiaPlane;
import com.gaia3d.modifier.billboard.BillboardPlane;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.*;

@Slf4j
@RequiredArgsConstructor
public class BillboardPlanePostMerger {

    private final MergeConfig config;

    public List<BillboardPlane> merge(List<BillboardPlane> inputPlanes) {
        if (inputPlanes == null || inputPlanes.isEmpty()) {
            return Collections.emptyList();
        }
        config.prepare();

        List<MergeNode> nodes = new ArrayList<>();
        for (int i = 0; i < inputPlanes.size(); i++) {
            BillboardPlane plane = inputPlanes.get(i);
            if (plane == null) {
                continue;
            }
            nodes.add(MergeNode.from(i, plane));
        }

        boolean merged;
        int iteration = 0;

        do {
            merged = false;
            iteration++;

            Map<GridKey, List<Integer>> grid = buildGrid(nodes, config.gridCellSize);
            List<MergeCandidate> candidates = collectMergeCandidates(nodes, grid, config);

            if (candidates.isEmpty()) {
                break;
            }

            candidates.sort((c1, c2) -> Double.compare(c2.score, c1.score));

            boolean[] reserved = new boolean[nodes.size()];
            List<MergeCandidate> selected = new ArrayList<>();

            for (MergeCandidate candidate : candidates) {
                if (candidate.i >= reserved.length || candidate.j >= reserved.length) {
                    continue;
                }

                MergeNode a = nodes.get(candidate.i);
                MergeNode b = nodes.get(candidate.j);

                if (!a.active || !b.active) {
                    continue;
                }

                if (reserved[candidate.i] || reserved[candidate.j]) {
                    continue;
                }

                reserved[candidate.i] = true;
                reserved[candidate.j] = true;
                selected.add(candidate);
            }

            if (selected.isEmpty()) {
                break;
            }

            log.info("[Iteration {}] candidates={}, selected={}, activeNodes={}", iteration, candidates.size(), selected.size(), countActiveNodes(nodes));

            for (MergeCandidate candidate : selected) {
                MergeNode a = nodes.get(candidate.i);
                MergeNode b = nodes.get(candidate.j);

                if (!a.active || !b.active) {
                    continue;
                }

                MergeNode mergedNode = mergeNodes(a, b, candidate.projection);

                a.active = false;
                b.active = false;
                nodes.add(mergedNode);

                merged = true;
            }

        } while (merged);

        List<BillboardPlane> result = new ArrayList<>();
        for (MergeNode node : nodes) {
            if (node.active) {
                result.add(node.toBillboardPlane());
            }
        }

        log.info("Merge finished. Input planes: {}, output planes: {}", inputPlanes.size(), result.size());
        return result;
    }

    private List<MergeCandidate> collectMergeCandidates(List<MergeNode> nodes, Map<GridKey, List<Integer>> grid, MergeConfig config) {
        List<MergeCandidate> candidates = new ArrayList<>();

        for (int i = 0; i < nodes.size(); i++) {
            MergeNode a = nodes.get(i);
            if (!a.active) {
                continue;
            }

            GridKey key = toGridKey(a.center, config.gridCellSize);
            List<Integer> nearby = getNearbyCandidates(key, grid);

            for (int j : nearby) {
                if (j <= i) {
                    continue;
                }

                MergeNode b = nodes.get(j);
                if (!b.active) {
                    continue;
                }

                MergeEvaluation evaluation = evaluateMerge(a, b, config);
                if (!evaluation.mergeable) {
                    continue;
                }

                MergeCandidate candidate = new MergeCandidate();
                candidate.i = i;
                candidate.j = j;
                candidate.score = evaluation.score;
                candidate.projection = evaluation.projection;
                candidates.add(candidate);
            }
        }

        return candidates;
    }

    private Map<GridKey, List<Integer>> buildGrid(List<MergeNode> nodes, double cellSize) {
        Map<GridKey, List<Integer>> grid = new HashMap<>();

        for (int i = 0; i < nodes.size(); i++) {
            MergeNode node = nodes.get(i);
            if (!node.active) {
                continue;
            }

            GridKey key = toGridKey(node.center, cellSize);
            grid.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
        }

        return grid;
    }

    private GridKey toGridKey(Vector3d position, double cellSize) {
        int gx = fastFloor(position.x / cellSize);
        int gy = fastFloor(position.y / cellSize);
        int gz = fastFloor(position.z / cellSize);
        return new GridKey(gx, gy, gz);
    }

    private List<Integer> getNearbyCandidates(GridKey key, Map<GridKey, List<Integer>> grid) {
        List<Integer> result = new ArrayList<>();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    GridKey neighbor = new GridKey(key.x + dx, key.y + dy, key.z + dz);

                    List<Integer> list = grid.get(neighbor);
                    if (list != null && !list.isEmpty()) {
                        result.addAll(list);
                    }
                }
            }
        }

        return result;
    }

    private int countActiveNodes(List<MergeNode> nodes) {
        int count = 0;
        for (MergeNode node : nodes) {
            if (node.active) {
                count++;
            }
        }
        return count;
    }

    private int fastFloor(double value) {
        int i = (int) value;
        return value < i ? i - 1 : i;
    }

    private double intervalGap(double minA, double maxA, double minB, double maxB) {
        if (maxA < minB) {return minB - maxA;}
        if (maxB < minA) {return minA - maxB;}
        return 0.0;
    }

    private MergeEvaluation evaluateMerge(MergeNode a, MergeNode b, MergeConfig config) {
        double centerDistanceSquared = a.center.distanceSquared(b.center);
        if (centerDistanceSquared > config.maxCenterDistanceSquared) {
            return MergeEvaluation.fail();
        }

        //double normalDot = Math.abs(a.normal.dot(b.normal));
        double normalDot = a.normal.dot(b.normal);
        if (normalDot < config.minNormalDot) {
            return MergeEvaluation.fail();
        }

        double distAB = distancePointToPlane(a.center, b.center, b.normal);
        double distBA = distancePointToPlane(b.center, a.center, a.normal);
        double planeDistance = Math.max(distAB, distBA);
        if (planeDistance > config.maxPlaneDistance) {
            return MergeEvaluation.fail();
        }

        MergeProjection projection = computeMergedProjection(a, b);

        if (!isRectCloseEnough(projection, a, b, config.maxRectGap)) {
            return MergeEvaluation.fail();
        }

        double mergedRectArea = projection.getRectArea();
        if (mergedRectArea <= 1e-12) {
            return MergeEvaluation.fail();
        }

        double occupiedArea = a.getRectArea() + b.getRectArea();
        double efficiency = occupiedArea / mergedRectArea;
        if (efficiency < config.minEfficiency) {
            return MergeEvaluation.fail();
        }

        double thickness = projection.depthMax - projection.depthMin;
        if (thickness > config.maxThickness) {
            return MergeEvaluation.fail();
        }

        double planeScoreDenom = config.maxPlaneDistance <= 1e-12 ? 1.0 : config.maxPlaneDistance;
        double thicknessScoreDenom = config.maxThickness <= 1e-12 ? 1.0 : config.maxThickness;

        double normalScore = remap(normalDot, config.minNormalDot, 1.0);
        double planeScore = 1.0 - clamp01(planeDistance / planeScoreDenom);
        double efficiencyScore = remap(efficiency, config.minEfficiency, 1.0);
        double thicknessScore = 1.0 - clamp01(thickness / thicknessScoreDenom);

        double score = (normalScore * 0.35) + (planeScore * 0.20) + (efficiencyScore * 0.30) + (thicknessScore * 0.15);

        MergeEvaluation evaluation = new MergeEvaluation();
        evaluation.mergeable = true;
        evaluation.score = score;
        evaluation.normalDot = normalDot;
        evaluation.planeDistance = planeDistance;
        evaluation.efficiency = efficiency;
        evaluation.thickness = thickness;
        evaluation.projection = projection;
        return evaluation;
    }

    private MergeProjection computeMergedProjection(MergeNode a, MergeNode b) {
        MergeProjection projection = new MergeProjection();

        Vector3d mergedNormal = new Vector3d(a.normal).add(b.normal);
        if (mergedNormal.lengthSquared() < 1e-12) {
            mergedNormal.set(a.normal);
        }
        mergedNormal.normalize();

        Vector3d mergedCenter = new Vector3d(a.center).add(b.center).mul(0.5);

        Vector3d tangent = buildStableTangent(mergedNormal);
        Vector3d bitangent = new Vector3d(mergedNormal).cross(tangent).normalize();

        projection.center = mergedCenter;
        projection.normal = mergedNormal;
        projection.tangent = tangent;
        projection.bitangent = bitangent;

        projection.minU = Double.POSITIVE_INFINITY;
        projection.minV = Double.POSITIVE_INFINITY;
        projection.maxU = Double.NEGATIVE_INFINITY;
        projection.maxV = Double.NEGATIVE_INFINITY;
        projection.depthMin = Double.POSITIVE_INFINITY;
        projection.depthMax = Double.NEGATIVE_INFINITY;

        updateProjectionBounds(projection, a);
        updateProjectionBounds(projection, b);

        return projection;
    }

    private boolean isRectCloseEnough(MergeProjection projection, MergeNode a, MergeNode b, double maxGap) {
        ProjectionBounds boundsA = projectBounds(projection, a);
        ProjectionBounds boundsB = projectBounds(projection, b);

        double gapU = intervalGap(boundsA.minU, boundsA.maxU, boundsB.minU, boundsB.maxU);
        double gapV = intervalGap(boundsA.minV, boundsA.maxV, boundsB.minV, boundsB.maxV);
        return gapU <= maxGap && gapV <= maxGap;
    }

    private ProjectionBounds projectBounds(MergeProjection projection, MergeNode node) {
        ProjectionBounds bounds = new ProjectionBounds();
        for (Vector3d point : node.getSamplePoints()) {
            Vector3d diff = new Vector3d(point).sub(projection.center);
            double u = diff.dot(projection.tangent);
            double v = diff.dot(projection.bitangent);

            if (u < bounds.minU) {bounds.minU = u;}
            if (u > bounds.maxU) {bounds.maxU = u;}
            if (v < bounds.minV) {bounds.minV = v;}
            if (v > bounds.maxV) {bounds.maxV = v;}
        }
        return bounds;
    }

    private void updateProjectionBounds(MergeProjection projection, MergeNode node) {
        List<Vector3d> points = node.getSamplePoints();
        for (Vector3d point : points) {
            Vector3d diff = new Vector3d(point).sub(projection.center);

            double u = diff.dot(projection.tangent);
            double v = diff.dot(projection.bitangent);
            double d = diff.dot(projection.normal);

            if (u < projection.minU) {projection.minU = u;}
            if (u > projection.maxU) {projection.maxU = u;}
            if (v < projection.minV) {projection.minV = v;}
            if (v > projection.maxV) {projection.maxV = v;}
            if (d < projection.depthMin) {projection.depthMin = d;}
            if (d > projection.depthMax) {projection.depthMax = d;}
        }
    }

    private MergeNode mergeNodes(MergeNode a, MergeNode b, MergeProjection projection) {
        MergeNode merged = new MergeNode();
        merged.id = Math.min(a.id, b.id);
        merged.active = true;

        merged.sourcePlanes = new ArrayList<>();
        merged.sourcePlanes.addAll(a.sourcePlanes);
        merged.sourcePlanes.addAll(b.sourcePlanes);

        merged.center = new Vector3d(projection.center);
        merged.normal = new Vector3d(projection.normal);
        merged.tangent = new Vector3d(projection.tangent);
        merged.bitangent = new Vector3d(projection.bitangent);

        merged.minU = projection.minU;
        merged.minV = projection.minV;
        merged.maxU = projection.maxU;
        merged.maxV = projection.maxV;
        merged.depthMin = projection.depthMin;
        merged.depthMax = projection.depthMax;

        merged.samplePoints = new ArrayList<>();
        merged.samplePoints.addAll(a.samplePoints);
        merged.samplePoints.addAll(b.samplePoints);

        merged.faces = new ArrayList<>();
        merged.faces.addAll(a.faces);
        merged.faces.addAll(b.faces);

        merged.plane = new GaiaPlane(merged.center, merged.normal);

        return merged;
    }

    private double distancePointToPlane(Vector3d point, Vector3d planePoint, Vector3d planeNormal) {
        return Math.abs(new Vector3d(point).sub(planePoint).dot(planeNormal));
    }

    private Vector3d buildStableTangent(Vector3d normal) {
        Vector3d up = Math.abs(normal.z) < 0.9 ? new Vector3d(0.0, 0.0, 1.0) : new Vector3d(0.0, 1.0, 0.0);

        Vector3d tangent = up.cross(normal, new Vector3d());
        if (tangent.lengthSquared() < 1e-12) {
            tangent = new Vector3d(1.0, 0.0, 0.0).cross(normal, new Vector3d());
        }
        tangent.normalize();
        return tangent;
    }

    private double remap(double value, double min, double max) {
        if (max <= min) {
            return 1.0;
        }
        return clamp01((value - min) / (max - min));
    }

    private double clamp01(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }

    private static class ProjectionBounds {
        double minU = Double.POSITIVE_INFINITY;
        double minV = Double.POSITIVE_INFINITY;
        double maxU = Double.NEGATIVE_INFINITY;
        double maxV = Double.NEGATIVE_INFINITY;
    }
}
