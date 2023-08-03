package process.tileprocess;

import process.tileprocess.tile.tileset.Tileset;

public interface Tiler extends TileProcess {
    public void writeTileset(Tileset tileset);
}
