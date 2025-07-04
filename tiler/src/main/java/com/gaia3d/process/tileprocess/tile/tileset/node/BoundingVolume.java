package com.gaia3d.process.tileprocess.tile.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.converter.kml.KmlInfo;
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
        this(boundingBox, false);
    }

    public BoundingVolume(GaiaBoundingBox boundingBox, boolean cartesian) {
        if (cartesian) {
            // The first three elements define the x, y, and z values for the center of the box.
            // The next three elements (with indices 3, 4, and 5) define the x-axis direction and half-length.
            // The next three elements (indices 6, 7, and 8) define the y-axis direction and half-length.
            // The last three elements (indices 9, 10, and 11) define the z-axis direction and half-length.
            Vector3d minpos = boundingBox.getMinPosition();
            Vector3d center = boundingBox.getCenter();
            this.setType(BoundingVolumeType.BOX);
            this.setBox(
                    new double[] {
                        center.x,
                        center.y,
                        center.z,
                        center.x - minpos.x,
                        0, // Do we also need an y component?
                        0,
                        0,
                        center.y - minpos.y,
                        0,
                        0,
                        0,
                        center.z - minpos.z,
                    }
                );
        } else {
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

                KmlInfo kmlInfo = tileInfo.getKmlInfo();
                localBoundingBox = localBoundingBox.convertLocalToLonlatBoundingBox(kmlInfo.getPosition());
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
        if (region != null) {
            return new Vector3d((region[0] + region[2]) / 2, (region[1] + region[3]) / 2, (region[4] + region[5]) / 2);
        } else if (box != null) {
            return new Vector3d(box[0], box[1], box[2]);
        } else  {
            return new Vector3d();
        }
    }

    /**
     * Create square bounding volume
     * maximum x or y value is increased to make square bounding volume.
     * @return square bounding volume
     */
     public BoundingVolume createSquareBoundingVolume() {
         if (region != null) {
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
             BoundingVolume boundingVolume = new BoundingVolume(
                 BoundingVolumeType.REGION
             );
             boundingVolume.setRegion(
                 new double[] { minX, minY, maxX, maxY, region[4], region[5] }
             );
             return boundingVolume;
         } else if (box != null) {
             Vector3d xaxis = new Vector3d(box[3], box[4], 0);
             Vector3d yaxis = new Vector3d(box[6], box[7], 0);
             double maxlen = Math.max(xaxis.length(), yaxis.length());
             xaxis = xaxis.mul(maxlen/xaxis.length());
             yaxis = yaxis.mul(maxlen/yaxis.length());
             BoundingVolume boundingVolume = new BoundingVolume(
                 BoundingVolumeType.BOX
             );
             boundingVolume.setBox(
                 new double[] {
                     box[0],
                     box[1],
                     box[2],
                     xaxis.x,
                     xaxis.y,
                     box[5],
                     yaxis.x,
                     yaxis.y,
                     box[8],
                     box[9],
                     box[10],
                     box[11]
                 }
             );
             return boundingVolume;
         } else {
             return new BoundingVolume(BoundingVolumeType.REGION);
         }
     }
}


