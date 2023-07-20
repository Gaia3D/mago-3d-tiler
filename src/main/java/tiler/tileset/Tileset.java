package tiler.tileset;

import lombok.Getter;
import lombok.Setter;
import tiler.BatchInfo;
import tiler.tileset.asset.Asset;
import tiler.tileset.node.Node;
import tiler.tileset.node.Properties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Tileset {
    private Asset asset;
    private double geometricError = 0.0d;
    private Node root;
    private Properties properties;

    public List<BatchInfo> findAllBatchInfo() {
        return root.findAllBatchInfo(new ArrayList<>());
    }
}
