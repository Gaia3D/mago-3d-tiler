package com.gaia3d.basic.pipe;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PipeElbowType {
    UNKNOWN(0),
    STRAIGHT(1),
    TOROIDAL(2),
    SPHERICAL(3);

    private final int value;
}
