package com.gaia3d.process.tileprocess.tile.tileset.node;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@EqualsAndHashCode
public class Range {
    private double maximum;
    private double minimum;

    public Range(double min, double max) {
        minimum = min;
        maximum = max;
    }
}
