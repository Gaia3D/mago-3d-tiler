package tiler;

import geometry.basic.GaiaBoundingBox;
import geometry.exchangable.GaiaUniverse;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ContentInfo {
    String nodeCode;
    LevelOfDetail lod;
    List<TileInfo> tileInfos;
    GaiaUniverse universe;
    GaiaBoundingBox boundingBox;
}
