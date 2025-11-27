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
import org.joml.Matrix4d;
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
    private BoundingVolumeType type;

    // minx, miny, maxx, maxy, minz, maxz
    double[] region;
    // centerX, centerY, centerZ, halfX1, halfX2, halfX3, halfY1, halfY2, halfY3, halfZ1, halfZ2, halfZ3
    double[] box;
    // centerX, centerY, centerZ, radius
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

    public BoundingVolume(GaiaBoundingBox boundingBox, BoundingVolumeType type) {
        ProjCoordinate minPoint = new ProjCoordinate(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ());
        ProjCoordinate maxPoint = new ProjCoordinate(boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
        Vector3d center = boundingBox.getCenter();
        double lengthX = boundingBox.getLengthX();
        double lengthY = boundingBox.getLengthY();
        double lengthZ = boundingBox.getLengthZ();
        double halfX = lengthX / 2;
        double halfY = lengthY / 2;
        double halfZ = lengthZ / 2;
        double maxLength = Math.max(halfX, halfY);
        this.setType(type);
        if (BoundingVolumeType.REGION == type) {
            region = new double[6];
            // minX, minY (lon, lat)
            region[0] = Math.toRadians(minPoint.x);
            region[1] = Math.toRadians(minPoint.y);
            // maxX, maxY (lon, lat)
            region[2] = Math.toRadians(maxPoint.x);
            region[3] = Math.toRadians(maxPoint.y);
            // minZ, maxZ (altitude)
            region[4] = boundingBox.getMinZ();
            region[5] = boundingBox.getMaxZ();
            for (int i = 0; i < region.length; i++) {
                region[i] = DecimalUtils.cutFast(region[i]);
            }
        } else if (BoundingVolumeType.BOX == type) {
            box = new double[12];
            // center
            box[0] = center.x;
            box[1] = center.y;
            box[2] = center.z;
            // halfX
            box[3] = maxLength;
            box[4] = 0;
            box[5] = 0;
            // halfY
            box[6] = 0;
            box[7] = maxLength;
            box[8] = 0;
            // halfZ
            box[9] = 0;
            box[10] = 0;
            box[11] = halfZ;
        } else if (BoundingVolumeType.SPHERE == type) {
            sphere = new double[4];
            sphere[0] = center.x;
            sphere[1] = center.y;
            sphere[2] = center.z;
            sphere[3] = boundingBox.getLongestDistance();
            for (int i = 0; i < sphere.length; i++) {
                sphere[i] = DecimalUtils.cutFast(sphere[i]);
            }
        } else {
            log.error("Unsupported bounding volume type: {}", type);
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
                GaiaBoundingBox localBoundingBox = tileInfo.getBoundingBox();

                TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
                localBoundingBox = localBoundingBox.convertLocalToLonlatBoundingBox(tileTransformInfo.getPosition());
                BoundingVolume localBoundingVolume = new BoundingVolume(localBoundingBox, BoundingVolume.BoundingVolumeType.REGION);
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
        } else if (BoundingVolumeType.BOX == type) {
            double centerX = box[0];
            double centerY = box[1];
            double centerZ = box[2];
            double halfX1 = box[3];
            double halfY1 = box[6];
            double halfZ1 = box[9];
            double halfX2 = box[4];
            double halfY2 = box[7];
            double halfZ2 = box[10];
            double halfX3 = box[5];
            double halfY3 = box[8];
            double halfZ3 = box[11];

            Vector3d halfVector1 = new Vector3d(halfX1, halfY1, halfZ1);
            Vector3d halfVector2 = new Vector3d(halfX2, halfY2, halfZ2);
            Vector3d halfVector3 = new Vector3d(halfX3, halfY3, halfZ3);
            Matrix4d transformMatrix = new Matrix4d()
                    .identity()
                    .translate(centerX, centerY, centerZ)
                    .scale(halfVector1.x(), halfVector1.y(), halfVector1.z())
                    .rotateX(Math.toRadians(-90))
                    .scale(halfVector2.x(), halfVector2.y(), halfVector2.z())
                    .scale(halfVector3.x(), halfVector3.y(), halfVector3.z());

            GaiaBoundingBox boundingBox = new GaiaBoundingBox();
            boundingBox.addPoint(centerX - halfX1, centerY - halfY1, centerZ - halfZ1);
            boundingBox.addPoint(centerX - halfX2, centerY - halfY2, centerZ - halfZ2);
            boundingBox.addPoint(centerX + halfX3, centerY + halfY3, centerZ + halfZ3);

            for (TileInfo tileInfo : tileInfos) {
                GaiaBoundingBox localBoundingBox = tileInfo.getBoundingBox();
                TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
                Vector3d worldCartesian = tileTransformInfo.getPosition();

                BoundingVolume localBoundingVolume = new BoundingVolume(localBoundingBox, BoundingVolume.BoundingVolumeType.BOX);
                Vector3d center = localBoundingVolume.calcCenter();

                center.add(worldCartesian, center);

                double minX = boundingBox.getMinX();
                double minY = boundingBox.getMinY();
                double maxX = boundingBox.getMaxX();
                double maxY = boundingBox.getMaxY();
                if (center.x() < minX + (maxX - minX) / 2) {
                    if (center.y() < minY + (maxY - minY) / 2) {
                        result.get(3).add(tileInfo); // Bottom Left
                    } else {
                        result.get(0).add(tileInfo); // Top Left
                    }
                } else {
                    if (center.y() < minY + (maxY - minY) / 2) {
                        result.get(2).add(tileInfo); // Bottom Right
                    } else {
                        result.get(1).add(tileInfo); // Top Right
                    }
                }
            }
        } else {
            log.error("Unsupported bounding volume type: {}", type);
            throw new IllegalArgumentException("Unsupported bounding volume type: " + type);
        }
        return result;
    }

    public List<TileInfo> getVolumeIncludeScenes(List<TileInfo> tileInfos, GaiaBoundingBox volume) {
        List<TileInfo> result = new ArrayList<>();
        if (BoundingVolumeType.REGION == type) {
            for (TileInfo tileInfo : tileInfos) {
                GaiaBoundingBox localBoundingBox = tileInfo.getBoundingBox();
                TileTransformInfo tileTransformInfo = tileInfo.getTileTransformInfo();
                localBoundingBox = localBoundingBox.convertLocalToLonlatBoundingBox(tileTransformInfo.getPosition());
                Vector3d center = localBoundingBox.getCenter();

                boolean isInX = volume.getMinX() <= center.x() && center.x() <= volume.getMaxX();
                boolean isInY = volume.getMinY() <= center.y() && center.y() <= volume.getMaxY();
                boolean isInZ = volume.getMinZ() <= center.z() && center.z() <= volume.getMaxZ();
                if (isInX && isInY && isInZ) {
                    result.add(tileInfo);
                }
            }
        } else {
            // TODO
            log.error("Unsupported bounding volume type: {}", type);
        }
        return result;
    }

    public Vector3d calcCenter() {
        if (BoundingVolumeType.REGION == type) {
            return new Vector3d((region[0] + region[2]) / 2, (region[1] + region[3]) / 2, (region[4] + region[5]) / 2);
        } else if (BoundingVolumeType.BOX == type) {
            return new Vector3d(box[0], box[1], box[2]);
        } else if (BoundingVolumeType.SPHERE == type) {
            return new Vector3d(sphere[0], sphere[1], sphere[2]);
        } else {
            log.error("Unsupported bounding volume type: {}", type);
            throw new IllegalArgumentException("Unsupported bounding volume type: " + type);
        }
        //return new Vector3d((region[0] + region[2]) / 2, (region[1] + region[3]) / 2, (region[4] + region[5]) / 2);
    }

    /**
     * Create square bounding volume
     * maximum x or y value is increased to make square bounding volume.
     * @return square bounding volume
     */
    public BoundingVolume createSqureBoundingVolume() {
        if (BoundingVolumeType.REGION == type) {
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
        } else if (BoundingVolumeType.BOX == type) {
            double centerX = box[0];
            double centerY = box[1];
            double centerZ = box[2];
            double halfX1 = box[3];
            double halfY1 = box[6];
            double halfZ1 = box[9];

            double maxLength = Math.max(Math.max(halfX1, halfY1), halfZ1);
            BoundingVolume boundingVolume = new BoundingVolume(BoundingVolumeType.BOX);
            boundingVolume.setBox(new double[]{
                    centerX, centerY, centerZ,
                    maxLength, 0, 0,
                    0, maxLength, 0,
                    0, 0, maxLength
            });
            return boundingVolume;
        } else {
            log.error("Unsupported bounding volume type: {}", type);
            throw new IllegalArgumentException("Unsupported bounding volume type: " + type);
        }
    }
}



