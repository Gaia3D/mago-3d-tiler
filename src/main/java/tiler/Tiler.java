package tiler;

import lombok.Builder;
import tiler.tileset.Tileset;

public interface Tiler {
    Tileset tile();
}
