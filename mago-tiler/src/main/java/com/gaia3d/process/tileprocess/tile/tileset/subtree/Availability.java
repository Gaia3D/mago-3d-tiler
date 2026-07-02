package com.gaia3d.process.tileprocess.tile.tileset.subtree;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Availability {
    private Integer constant;
    private Integer bitstream;
    private Integer availableCount;

    public static Availability constant(int value) {
        Availability availability = new Availability();
        availability.setConstant(value);
        return availability;
    }

    public static Availability bitstream(int bufferView, int availableCount) {
        Availability availability = new Availability();
        availability.setBitstream(bufferView);
        availability.setAvailableCount(availableCount);
        return availability;
    }
}
