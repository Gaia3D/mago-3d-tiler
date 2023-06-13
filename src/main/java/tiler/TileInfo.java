package tiler;

import geometry.basic.GaiaBoundingBox;
import geometry.exchangable.GaiaUniverse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TileInfo {
    GaiaUniverse universe;
    GaiaBoundingBox boundingBox;
}
