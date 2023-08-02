package tiler;

import lombok.Builder;
import tiler.tileset.Tileset;

import java.nio.file.Path;
import java.util.List;

public interface Tiler {
    Tileset tile(List<TileInfo> tileInfos);
    public void writeTileset(Path path, Tileset tileset);
}
