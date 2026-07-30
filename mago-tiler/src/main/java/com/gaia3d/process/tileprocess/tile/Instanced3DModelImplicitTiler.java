package com.gaia3d.process.tileprocess.tile;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.types.LevelOfDetail;
import com.gaia3d.command.mago.GlobalConstants;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Instanced3DModelImplicitTiler extends AbstractImplicitModelTiler {
    private static final double MAXIMUM_DISTANCE = 1000.0d;
    private static final double MAXIMUM_GEOMETRIC_ERROR = 64.0d;
    private static final double METERS_PER_DEGREE = 111_320.0d;

    @Override
    protected boolean shouldSplit(List<TileInfo> tileInfos, GaiaBoundingBox cellBoundingBox, int depth) {
        if (globalOptions.isRefineAdd() && tileInfos.size() > contentBatchSize(tileInfos.size())) {
            return true;
        }
        return tileInfos.size() > globalOptions.getMaxInstance() || longestSideMeters(cellBoundingBox) > MAXIMUM_DISTANCE;
    }

    @Override
    protected long countForLog(List<TileInfo> tileInfos) {
        return tileInfos.size();
    }

    @Override
    protected Node.RefineType refineType() {
        return globalOptions.isRefineAdd() ? Node.RefineType.ADD : Node.RefineType.REPLACE;
    }

    @Override
    protected double rootGeometricError(List<TileInfo> tileInfos, GaiaBoundingBox rootBoundingBox) {
        if (tileInfos.isEmpty()) {
            return MAXIMUM_GEOMETRIC_ERROR;
        }
        return Math.max(MAXIMUM_GEOMETRIC_ERROR, calcGeometricError(List.of(tileInfos.get(0))));
    }

    @Override
    protected List<TileInfo> contentTileInfos(List<TileInfo> tileInfos, GaiaBoundingBox cellBoundingBox, SplitResult splitResult, boolean canSplit, int depth) {
        if (!globalOptions.isRefineAdd()) {
            return super.contentTileInfos(tileInfos, cellBoundingBox, splitResult, canSplit, depth);
        }
        if (!canSplit) {
            return tileInfos;
        }
        int contentCount = Math.min(tileInfos.size(), contentBatchSize(tileInfos.size()));
        List<TileInfo> shuffledTileInfos = new ArrayList<>(tileInfos);
        Collections.shuffle(shuffledTileInfos, new Random(shuffleSeed(depth, tileInfos.size())));
        return shuffledTileInfos.stream()
                .limit(contentCount)
                .toList();
    }

    @Override
    protected List<TileInfo> childTileInfos(List<TileInfo> tileInfos, List<TileInfo> contentTileInfos, SplitResult splitResult, int childIndex, boolean canSplit, int depth) {
        List<TileInfo> childTileInfos = splitResult.children().get(childIndex);
        if (!globalOptions.isRefineAdd() || contentTileInfos.isEmpty()) {
            return childTileInfos;
        }

        Set<TileInfo> contentSet = Collections.newSetFromMap(new IdentityHashMap<>());
        contentSet.addAll(contentTileInfos);
        List<TileInfo> remainTileInfos = new ArrayList<>();
        for (TileInfo childTileInfo : childTileInfos) {
            if (!contentSet.contains(childTileInfo)) {
                remainTileInfos.add(childTileInfo);
            }
        }
        return remainTileInfos;
    }

    @Override
    protected LevelOfDetail lodForContent(List<TileInfo> tileInfos, int depth) {
        if (globalOptions.isRefineAdd()) {
            return LevelOfDetail.LOD0;
        }
        return super.lodForContent(tileInfos, depth);
    }

    @Override
    protected boolean allowDegenerateSplit(List<TileInfo> tileInfos, GaiaBoundingBox cellBoundingBox, int depth) {
        return globalOptions.isRefineAdd() && tileInfos.size() > contentBatchSize(tileInfos.size());
    }

    private int contentBatchSize(int instanceCount) {
        int divideSize = instanceCount / 4;
        if (divideSize > GlobalConstants.DEFAULT_MAX_I3DM_FEATURE_COUNT) {
            return GlobalConstants.DEFAULT_MAX_I3DM_FEATURE_COUNT;
        }
        return Math.max(divideSize, GlobalConstants.DEFAULT_MIN_I3DM_FEATURE_COUNT);
    }

    private long shuffleSeed(int depth, int instanceCount) {
        return 31L * depth + instanceCount;
    }

    private double longestSideMeters(GaiaBoundingBox boundingBox) {
        double centerLatitude = (boundingBox.getMinY() + boundingBox.getMaxY()) / 2.0d;
        double metersPerDegreeLongitude = Math.max(1.0d, Math.cos(Math.toRadians(centerLatitude)) * METERS_PER_DEGREE);
        double widthMeters = Math.abs(boundingBox.getMaxX() - boundingBox.getMinX()) * metersPerDegreeLongitude;
        double heightMeters = Math.abs(boundingBox.getMaxY() - boundingBox.getMinY()) * METERS_PER_DEGREE;
        return Math.max(widthMeters, heightMeters);
    }
}
