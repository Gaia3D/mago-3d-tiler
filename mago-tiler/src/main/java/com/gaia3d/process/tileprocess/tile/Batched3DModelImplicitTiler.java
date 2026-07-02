package com.gaia3d.process.tileprocess.tile;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.types.LevelOfDetail;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;

import java.util.*;

public class Batched3DModelImplicitTiler extends AbstractImplicitModelTiler {
    private static final int MAX_LEVEL_OF_DETAIL = 23;
    private static final double TARGET_LEAF_TILE_SIZE_METERS = 125.0d;
    private static final double METERS_PER_DEGREE = 111_320.0d;
    private int physicalLeafDepth = 0;

    @Override
    protected void beforeBuildImplicitTiles(List<TileInfo> tileInfos, GaiaBoundingBox rootBoundingBox) {
        physicalLeafDepth = calculatePhysicalLeafDepth(rootBoundingBox);
    }

    @Override
    protected boolean shouldSplit(List<TileInfo> tileInfos, GaiaBoundingBox cellBoundingBox, int depth) {
        return depth < physicalLeafDepth;
    }

    @Override
    protected long countForLog(List<TileInfo> tileInfos) {
        return tileInfos.size();
    }

    @Override
    protected List<TileInfo> contentTileInfos(List<TileInfo> tileInfos, GaiaBoundingBox cellBoundingBox, SplitResult splitResult, boolean canSplit, int depth) {
        if (globalOptions.isRefineAdd()) {
            if (!hasContent(tileInfos, cellBoundingBox, depth)) {
                return List.of();
            }
            int lodError = lodForDepth(depth).getGeometricErrorBlock();
            return tileInfos.stream().filter(tileInfo -> tileInfo.getBoundingBox().getLongestDistance() >= lodError).toList();
        }
        return tileInfos;
    }

    @Override
    protected boolean hasContent(List<TileInfo> tileInfos, GaiaBoundingBox cellBoundingBox, int depth) {
        return depth >= contentStartDepth() && depth <= physicalLeafDepth;
    }

    @Override
    protected Node.RefineType refineType() {
        return globalOptions.isRefineAdd() ? Node.RefineType.ADD : Node.RefineType.REPLACE;
    }

    @Override
    protected double rootGeometricError(List<TileInfo> tileInfos, GaiaBoundingBox rootBoundingBox) {
        LevelOfDetail rootLod = LevelOfDetail.getByLevel(Math.min(MAX_LEVEL_OF_DETAIL, globalOptions.getMaxLod() + 1));
        double refineScale = globalOptions.isRefineAdd() ? 4.0d : 2.0d;
        return rootLod.getGeometricError() * Math.pow(2.0d, contentStartDepth()) * refineScale;
    }

    @Override
    protected double tilesetGeometricError(List<TileInfo> tileInfos, GaiaBoundingBox rootBoundingBox) {
        return Math.max(rootGeometricError(tileInfos, rootBoundingBox), Math.max(rootBoundingBox.getLongestDistance(), calcGeometricError(tileInfos)));
    }

    @Override
    protected LevelOfDetail lodForDepth(int depth) {
        int minLod = globalOptions.getMinLod();
        int maxLod = globalOptions.getMaxLod();
        int relativeDepth = Math.max(0, depth - contentStartDepth());
        return LevelOfDetail.getByLevel(Math.max(minLod, maxLod - relativeDepth));
    }

    @Override
    protected LevelOfDetail lodForContent(List<TileInfo> tileInfos, int depth) {
        return lodForDepth(depth);
    }

    @Override
    protected boolean allowDegenerateSplit(List<TileInfo> tileInfos, GaiaBoundingBox cellBoundingBox, int depth) {
        return depth < physicalLeafDepth;
    }

    @Override
    protected boolean isolateTextureLod() {
        return true;
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

    private int contentStartDepth() {
        int lodSpan = Math.max(0, globalOptions.getMaxLod() - globalOptions.getMinLod());
        return Math.max(0, physicalLeafDepth - lodSpan);
    }

    private int calculatePhysicalLeafDepth(GaiaBoundingBox rootBoundingBox) {
        double centerLatitude = (rootBoundingBox.getMinY() + rootBoundingBox.getMaxY()) / 2.0d;
        double metersPerDegreeLongitude = Math.max(1.0d, Math.cos(Math.toRadians(centerLatitude)) * METERS_PER_DEGREE);
        double widthMeters = Math.abs(rootBoundingBox.getMaxX() - rootBoundingBox.getMinX()) * metersPerDegreeLongitude;
        double heightMeters = Math.abs(rootBoundingBox.getMaxY() - rootBoundingBox.getMinY()) * METERS_PER_DEGREE;
        double tileSizeMeters = Math.max(widthMeters, heightMeters);

        int maxDepth = globalOptions.getMaxNodeDepth();
        int minimumLeafDepth = Math.max(0, globalOptions.getMaxLod() - globalOptions.getMinLod());
        int depth = 0;
        double targetLeafTileSizeMeters = targetLeafTileSizeMeters();
        while (tileSizeMeters > targetLeafTileSizeMeters && depth < maxDepth) {
            tileSizeMeters /= 2.0d;
            depth++;
        }
        return Math.min(maxDepth, Math.max(minimumLeafDepth, depth));
    }

    private double targetLeafTileSizeMeters() {
        return globalOptions.isRefineAdd() ? TARGET_LEAF_TILE_SIZE_METERS * 4.0d : TARGET_LEAF_TILE_SIZE_METERS;
    }
}
