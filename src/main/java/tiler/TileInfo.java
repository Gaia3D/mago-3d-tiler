package tiler;

import command.KmlInfo;
import geometry.structure.GaiaScene;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TileInfo {
    private KmlInfo kmlInfo;
    private GaiaScene scene;
}
