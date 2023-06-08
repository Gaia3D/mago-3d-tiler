package tiler.tileset.node;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BoundingVolume {
    @JsonIgnore
    BoundingVolumeType type;

    double[] region;
    double[] box;
    double[] sphere;

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
}


