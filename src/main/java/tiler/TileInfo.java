package tiler;

import command.KmlInfo;
import geometry.structure.GaiaScene;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class TileInfo {
    private KmlInfo kmlInfo;
    private GaiaScene scene;
}
