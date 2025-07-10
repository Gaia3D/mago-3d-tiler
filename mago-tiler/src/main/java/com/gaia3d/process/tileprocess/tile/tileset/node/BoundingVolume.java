package com.gaia3d.process.tileprocess.tile.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.DecimalUtils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.locationtech.proj4j.ProjCoordinate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class BoundingVolume implements Serializable {
    private static final float GOLDEN_RATIO = 1.61803398875f;

    @JsonIgnore
    BoundingVolumeType type;

    double[] region; // minx, miny, maxx, maxy, minz, maxz
    double[] box;
    double[] sphere;

    public BoundingVolume(BoundingVolumeType type) {
        this.type = type;
        if (BoundingVolumeType.REGION == type) {
            region = new double[6];
        } else if (BoundingVolumeType.BOX == type) {
            box = new double[12];
        } else if (BoundingVolumeType.SPHERE == type) {
            sphere = new double[4];
        }
    }

    //public BoundingVolume(GaiaBoundingBox boundingBox, CoordinateReferenceSystem source) {
    public BoundingVolume(GaiaBoundingBox boundingBox) {
        ProjCoordinate minPoint = new ProjCoordinate(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ());
        ProjCoordinate maxPoint = new ProjCoordinate(boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
        double[] rootRegion = new double[6];
        rootRegion[0] = Math.toRadians(minPoint.x);
        rootRegion[1] = Math.toRadians(minPoint.y);
        rootRegion[2] = Math.toRadians(maxPoint.x);
        rootRegion[3] = Math.toRadians(maxPoint.y);
        rootRegion[4] = boundingBox.getMinZ();
        rootRegion[5] = boundingBox.getMaxZ();
        for (int i = 0; i < rootRegion.length; i++) {
            rootRegion[i] = DecimalUtils.cutFast(rootRegion[i]);
        }
        this.setType(BoundingVolumeType.REGION);
        this.setRegion(rootRegion);
    }

    public BoundingVolume(BoundingVolume boundingVolume) {
        this.type = boundingVolume.type;
        if (BoundingVolumeType.REGION == type) {
            region = new double[6];
            System.arraycopy(boundingVolume.region, 0, region, 0, 6);
        } else if (BoundingVolumeType.BOX == type) {
            box = new double[12];
            System.arraycopy(boundingVolume.box, 0, box, 0, 12);
        } else if (BoundingVolumeType.SPHERE == type) {
            sphere = new double[4];
            System.arraycopy(boundingVolume.sphere, 0, sphere, 0, 4);
        }
    }

    public enum BoundingVolumeType {
        BOX,
        SPHERE,
        REGION
    }

    public List<List<TileInfo>> distributeScene(List<TileInfo> tileInfos) {
        List<List<TileInfo>> result = new ArrayList<>();
        result.add(new ArrayList<>());
        result.add(new ArrayList<>());
        result.add(new ArrayList<>());
        result.add(new ArrayList<>());

        if (BoundingVolumeType.REGION == type) {
            double minX = region[0];
            double minY = region[1];
            double maxX = region[2];
            double maxY = region[3];
            double midX = (minX + maxX) / 2;
            double midY = (minY + maxY) / 2;
            for (TileInfo tileInfo : tileInfos) {
                //GaiaScene scene = tileInfo.getScene();
                GaiaBoundingBox localBoundingBox = tileInfo.getBoundingBox();

                TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
                localBoundingBox = localBoundingBox.convertLocalToLonlatBoundingBox(tileTransformInfo.getPosition());
                BoundingVolume localBoundingVolume = new BoundingVolume(localBoundingBox);
                Vector3d center = localBoundingVolume.calcCenter();

                if (midX < center.x()) {
                    if (midY < center.y()) {
                        result.get(2).add(tileInfo);
                    } else {
                        result.get(1).add(tileInfo);
                    }
                } else {
                    if (midY < center.y()) {
                        result.get(3).add(tileInfo);
                    } else {
                        result.get(0).add(tileInfo);
                    }
                }
            }
        }
        return result;
    }

    public Vector3d calcCenter() {
        return new Vector3d((region[0] + region[2]) / 2, (region[1] + region[3]) / 2, (region[4] + region[5]) / 2);
    }

    /**
     * Create square bounding volume
     * maximum x or y value is increased to make square bounding volume.
     * @return square bounding volume
     */
    public BoundingVolume createSqureBoundingVolume() {
        double minX = region[0];
        double minY = region[1];
        double maxX = region[2];
        double maxY = region[3];
        double xLength = maxX - minX;
        double yLength = maxY - minY;
        double offset = Math.abs(xLength - yLength);
        if (xLength > yLength) {
            maxY = maxY + offset;
        } else {
            maxX = maxX + offset;
        }
        BoundingVolume boundingVolume = new BoundingVolume(BoundingVolumeType.REGION);
        boundingVolume.setRegion(new double[]{minX, minY, maxX, maxY, region[4], region[5]});
        return boundingVolume;
    }
}


