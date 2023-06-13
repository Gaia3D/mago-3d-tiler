package tiler.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import geometry.basic.GaiaBoundingBox;
import geometry.structure.GaiaScene;
import geometry.types.FormatType;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;
import org.locationtech.proj4j.ProjCoordinate;
import util.GlobeUtils;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class BoundingVolume {
    @JsonIgnore
    BoundingVolumeType type;

    double[] region;
    double[] box;
    double[] sphere;

    public BoundingVolume(GaiaBoundingBox boundingBox) {
        ProjCoordinate minPoint = new ProjCoordinate(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ());
        ProjCoordinate maxPoint = new ProjCoordinate(boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());
        ProjCoordinate translatedMinPoint = GlobeUtils.transform(null, minPoint);
        ProjCoordinate translatedMaxPoint = GlobeUtils.transform(null, maxPoint);
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

    public enum BoundingVolumeType {
        BOX,
        SPHERE,
        REGION
    }

    public BoundingVolume[] divideBoundingVolume() {
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
            //ouble midZ = (minZ + maxZ) / 2;

            double[] region0 = new double[6];
            double[] region1 = new double[6];
            double[] region2 = new double[6];
            double[] region3 = new double[6];
            double[] region = this.getRegion();

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
        return result;
    }

    public List<GaiaScene> contains(List<GaiaScene> scenes) {
        List<GaiaScene> result = scenes.stream().filter((scene) -> {
            GaiaBoundingBox localBoundingBox = scene.getBoundingBox();
            BoundingVolume localBoundingVolume = new BoundingVolume(localBoundingBox);
            Vector3d center = localBoundingVolume.getCenter();
            return this.contains(center);
        }).collect(Collectors.toList());
        return result;
    }

    public boolean contains(Vector3d position) {
        boolean containX = region[0] <= position.x && position.x <= region[2];
        boolean containY = region[1] <= position.y && position.y <= region[3];
        //boolean containZ = region[4] <= center.z && center.z <= region[5];
        return containX && containY;
    }

    public Vector3d getCenter() {
        return new Vector3d((region[0] + region[2]) / 2, (region[1] + region[3]) / 2, (region[4] + region[5]) / 2);
    }
}


