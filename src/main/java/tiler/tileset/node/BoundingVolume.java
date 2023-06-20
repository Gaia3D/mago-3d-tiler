package tiler.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import geometry.basic.GaiaBoundingBox;
import geometry.structure.GaiaScene;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import util.GlobeUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
public class BoundingVolume {
    private static final float GOLDEN_RATIO = 1.61803398875f;

    @JsonIgnore
    BoundingVolumeType type;

    double[] region;
    //double[] box;
    //double[] sphere;

    public BoundingVolume(BoundingVolumeType type) {
        this.type = type;
        if (BoundingVolumeType.BOX == type) {
            region = new double[12];
        } else if (BoundingVolumeType.SPHERE == type) {
            region = new double[4];
        } else if (BoundingVolumeType.REGION == type) {
            region = new double[6];
        }
    }

    public BoundingVolume(GaiaBoundingBox boundingBox, CoordinateReferenceSystem source) {
        ProjCoordinate minPoint = new ProjCoordinate(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ());
        ProjCoordinate maxPoint = new ProjCoordinate(boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
        ProjCoordinate translatedMinPoint = GlobeUtils.transform(source, minPoint);
        ProjCoordinate translatedMaxPoint = GlobeUtils.transform(source, maxPoint);
        double[] rootRegion = new double[6];
        rootRegion[0] = Math.toRadians(translatedMinPoint.x);
        rootRegion[1] = Math.toRadians(translatedMinPoint.y);
        rootRegion[2] = Math.toRadians(translatedMaxPoint.x);
        rootRegion[3] = Math.toRadians(translatedMaxPoint.y);
        rootRegion[4] = boundingBox.getMinZ();
        rootRegion[5] = boundingBox.getMaxZ();
        this.setType(BoundingVolumeType.REGION);
        this.setRegion(rootRegion);
    }

    public enum BoundingVolumeType {
        BOX,
        SPHERE,
        REGION
    }

    //convertRectangle
    public void square() {
        double minX = region[0];
        double maxX = region[2];
        double minY = region[1];
        double maxY = region[3];
        double x = maxX - minX;
        double y = maxY - minY;

        double offset = 0.0d;
        if (x > y) {
            offset = x-y;
            maxY = maxY + offset;
            region[3] = maxY;
        } else if (y > x) {
            offset = y-x;
            maxX = maxX + offset;
            region[2] = maxX;
        }
    }

    public BoundingVolume[] divideBoundingVolume() {
        BoundingVolume[] result = new BoundingVolume[2];
        if (BoundingVolumeType.REGION == type) {
            double minX = region[0];
            double minY = region[1];
            double maxX = region[2];
            double maxY = region[3];
            double minZ = region[4];
            double maxZ = region[5];
            double midX = (minX + maxX) / 2;
            double midY = (minY + maxY) / 2;
            double x = maxX - minX;
            double y = maxY - minY;

            double ratioXTest = x / y;
            double ratioYTest = y / x;
            boolean goldenRatio = ratioXTest > GOLDEN_RATIO || ratioYTest > GOLDEN_RATIO;

            double[] region0;
            double[] region1;
            double[] region2;
            double[] region3;
            if (x > y && goldenRatio) {
                region0 = new double[] {minX, minY, midX, maxY, minZ, maxZ};
                region1 = new double[] {midX, minY, maxX, maxY, minZ, maxZ};

                BoundingVolume boundingVolume0 = new BoundingVolume(BoundingVolumeType.REGION);
                boundingVolume0.setRegion(region0);
                BoundingVolume boundingVolume1 = new BoundingVolume(BoundingVolumeType.REGION);
                boundingVolume1.setRegion(region1);

                result[0] = boundingVolume0;
                result[1] = boundingVolume1;
            } else if (y > x && goldenRatio) {
                region0 = new double[] {minX, minY, maxX, midY, minZ, maxZ};
                region1 = new double[] {minX, midY, maxX, maxY, minZ, maxZ};

                BoundingVolume boundingVolume0 = new BoundingVolume(BoundingVolumeType.REGION);
                boundingVolume0.setRegion(region0);
                BoundingVolume boundingVolume1 = new BoundingVolume(BoundingVolumeType.REGION);
                boundingVolume1.setRegion(region1);

                result[0] = boundingVolume0;
                result[1] = boundingVolume1;

            } else {
                result = new BoundingVolume[4];
                region0 = new double[] {minX, minY, midX, midY, minZ, maxZ};
                region1 = new double[] {midX, minY, maxX, midY, minZ, maxZ};
                region2 = new double[] {midX, midY, maxX, maxY, minZ, maxZ};
                region3 = new double[] {minX, midY, midX, maxY, minZ, maxZ};

                BoundingVolume boundingVolume0 = new BoundingVolume(BoundingVolumeType.REGION);
                boundingVolume0.setRegion(region0);
                BoundingVolume boundingVolume1 = new BoundingVolume(BoundingVolumeType.REGION);
                boundingVolume1.setRegion(region1);
                BoundingVolume boundingVolume2 = new BoundingVolume(BoundingVolumeType.REGION);
                boundingVolume2.setRegion(region2);
                BoundingVolume boundingVolume3 = new BoundingVolume(BoundingVolumeType.REGION);
                boundingVolume3.setRegion(region3);

                result[0] = boundingVolume0;
                result[1] = boundingVolume1;
                result[2] = boundingVolume2;
                result[3] = boundingVolume3;
            }
        }
        return result;
    }

    public BoundingVolume[] divideHalfBoundingVolume() {
        BoundingVolume[] result = new BoundingVolume[2];
        if (BoundingVolumeType.REGION == type) {
            double minX = region[0];
            double minY = region[1];
            double maxX = region[2];
            double maxY = region[3];
            double minZ = region[4];
            double maxZ = region[5];
            double midX = (minX + maxX) / 2;
            double midY = (minY + maxY) / 2;

            double x = maxX - minX;
            double y = maxY - minY;

            double[] region0;
            double[] region1;

            if (x > y) {
                region0 = new double[] {minX, minY, midX, maxY, minZ, maxZ};
                region1 = new double[] {midX, minY, maxX, maxY, minZ, maxZ};
            } else {
                region0 = new double[] {minX, minY, maxX, midY, minZ, maxZ};
                region1 = new double[] {minX, midY, maxX, maxY, minZ, maxZ};
            }

            BoundingVolume boundingVolume0 = new BoundingVolume(BoundingVolumeType.REGION);
            boundingVolume0.setRegion(region0);
            BoundingVolume boundingVolume1 = new BoundingVolume(BoundingVolumeType.REGION);
            boundingVolume1.setRegion(region1);

            result[0] = boundingVolume0;
            result[1] = boundingVolume1;
        }
        return result;
    }

    public BoundingVolume[] divideQuarterBoundingVolume() {
        BoundingVolume[] result = new BoundingVolume[4];
        if (BoundingVolumeType.REGION == type) {
            double minX = region[0];
            double minY = region[1];
            double maxX = region[2];
            double maxY = region[3];
            double minZ = region[4];
            double maxZ = region[5];
            double midX = (minX + maxX) / 2;
            double midY = (minY + maxY) / 2;

            double[] region0 = new double[] {minX, minY, midX, midY, minZ, maxZ};
            double[] region1 = new double[] {midX, minY, maxX, midY, minZ, maxZ};
            double[] region2 = new double[] {midX, midY, maxX, maxY, minZ, maxZ};
            double[] region3 = new double[] {minX, midY, midX, maxY, minZ, maxZ};

            BoundingVolume boundingVolume0 = new BoundingVolume(BoundingVolumeType.REGION);
            boundingVolume0.setRegion(region0);
            BoundingVolume boundingVolume1 = new BoundingVolume(BoundingVolumeType.REGION);
            boundingVolume1.setRegion(region1);
            BoundingVolume boundingVolume2 = new BoundingVolume(BoundingVolumeType.REGION);
            boundingVolume2.setRegion(region2);
            BoundingVolume boundingVolume3 = new BoundingVolume(BoundingVolumeType.REGION);
            boundingVolume3.setRegion(region3);

            result[0] = boundingVolume0;
            result[1] = boundingVolume1;
            result[2] = boundingVolume2;
            result[3] = boundingVolume3;
        }
        return result;
    }

    /*public BoundingVolume[] divideBoundingVolume() {
        BoundingVolume[] result = new BoundingVolume[4];
        if (BoundingVolumeType.REGION == type) {
            double minX = region[0];
            double minY = region[1];
            double maxX = region[2];
            double maxY = region[3];
            double minZ = region[4];
            double maxZ = region[5];
            double midX = (minX + maxX) / 2;
            double midY = (minY + maxY) / 2;

            double[] region0 = new double[] {minX, minY, midX, midY, minZ, maxZ};
            double[] region1 = new double[] {midX, minY, maxX, midY, minZ, maxZ};
            double[] region2 = new double[] {midX, midY, maxX, maxY, minZ, maxZ};
            double[] region3 = new double[] {minX, midY, midX, maxY, minZ, maxZ};

            BoundingVolume boundingVolume0 = new BoundingVolume(BoundingVolumeType.REGION);
            boundingVolume0.setRegion(region0);
            BoundingVolume boundingVolume1 = new BoundingVolume(BoundingVolumeType.REGION);
            boundingVolume1.setRegion(region1);
            BoundingVolume boundingVolume2 = new BoundingVolume(BoundingVolumeType.REGION);
            boundingVolume2.setRegion(region2);
            BoundingVolume boundingVolume3 = new BoundingVolume(BoundingVolumeType.REGION);
            boundingVolume3.setRegion(region3);

            result[0] = boundingVolume0;
            result[1] = boundingVolume1;
            result[2] = boundingVolume2;
            result[3] = boundingVolume3;
        }
        return result;
    }*/

    public List<GaiaScene> contains(List<GaiaScene> scenes, CoordinateReferenceSystem source) {
        return scenes.stream().filter((scene) -> {
            GaiaBoundingBox localBoundingBox = scene.getBoundingBox();
            BoundingVolume localBoundingVolume = new BoundingVolume(localBoundingBox, source);
            Vector3d center = localBoundingVolume.getCenter();
            return this.contains(center);
        }).collect(Collectors.toList());
    }

    public boolean contains(Vector3d position) {
        boolean containX = region[0] <= position.x && position.x <= region[2];
        boolean containY = region[1] <= position.y && position.y <= region[3];
        return containX && containY;
    }

    public Vector3d getCenter() {
        return new Vector3d((region[0] + region[2]) / 2, (region[1] + region[3]) / 2, (region[4] + region[5]) / 2);
    }
}


