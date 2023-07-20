package tiler;

import geometry.basic.GaiaBoundingBox;
import geometry.exchangable.GaiaUniverse;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BatchInfo {
    LevelOfDetail lod;
    GaiaUniverse universe;
    GaiaBoundingBox boundingBox;
    String nodeCode;
}
