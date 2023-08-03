package process.tileprocess;

import process.tileprocess.tile.TileInfo;
import process.tileprocess.tile.tileset.Tileset;

import java.util.List;

public interface TileProcess {
    Tileset run(List<TileInfo> tileInfo);

    public void writeTileset(Tileset tileset);
}
