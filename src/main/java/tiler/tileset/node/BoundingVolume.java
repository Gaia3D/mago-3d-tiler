package tiler.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import command.KmlInfo;
import geometry.basic.GaiaBoundingBox;
import geometry.structure.GaiaScene;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;
import tiler.TileInfo;
import util.GlobeUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
@EqualsAndHashCode
public class BoundingVolume {
    private static final float GOLDEN_RATIO = 1.61803398875f;

    @JsonIgnore
    BoundingVolumeType type;

    double[] region;
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

    public BoundingVolume(GaiaBoundingBox boundingBox, CoordinateReferenceSystem source) {
        ProjCoordinate minPoint = new ProjCoordinate(boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ());
        ProjCoordinate maxPoint = new ProjCoordinate(boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());

        ProjCoordinate translatedMinPoint = null;
        ProjCoordinate translatedMaxPoint = null;
        if (source != null) {
            translatedMinPoint = GlobeUtils.transform(source, minPoint);
            translatedMaxPoint = GlobeUtils.transform(source, maxPoint);
        } else {
            translatedMinPoint = minPoint;
            translatedMaxPoint = maxPoint;
        }
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

    public List<List<TileInfo>> distributeScene(List<TileInfo> tileInfos, CoordinateReferenceSystem source) {
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
                GaiaScene scene = tileInfo.getScene();

                //KmlInfo kmlInfo = tileInfo.getKmlInfo();
                //Vector3d test = GlobeUtils.cartesianToGeographicWgs84(kmlInfo.getPosition());

                GaiaBoundingBox localBoundingBox = scene.getBoundingBox();
                //localBoundingBox.translate(-test.x, -test.y, -test.z);

                BoundingVolume localBoundingVolume = new BoundingVolume(localBoundingBox, source);
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
}


