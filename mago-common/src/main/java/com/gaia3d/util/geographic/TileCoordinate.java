package com.gaia3d.util.geographic;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TileCoordinate {
    public int level;
    public int x;
    public int y;

    @Override
    public String toString() {
        return "[L:" + level + ", X:" + x + ", Y:" + y + "]";
    }
}
