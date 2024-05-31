package com.gaia3d.converter.geometry.pipe;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PipeElbowType {
    //0 = unknown, 1 = straight, 2 = toroidal, 3 = spherical.***
    UNKNOWN(0),
    STRAIGHT(1),
    TOROIDAL(2),
    SPHERICAL(3);

    private final int value;

}
