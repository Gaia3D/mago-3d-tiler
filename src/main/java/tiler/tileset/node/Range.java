package tiler.tileset.node;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Range {
    private double maximum;
    private double minimum;
    public Range(double min, double max) {
        minimum = min;
        maximum = max;
    }
}
