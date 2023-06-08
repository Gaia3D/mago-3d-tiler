package tiler.tileset;

import lombok.Getter;
import lombok.Setter;
import tiler.tileset.asset.Asset;
import tiler.tileset.node.Node;
import tiler.tileset.node.Properties;

@Getter
@Setter
public class Tileset {
    private Asset asset;
    private float geometricError = 0.0f;
    private Node root;
    private Properties properties;
}
