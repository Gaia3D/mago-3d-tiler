package com.gaia3d.basic.pipe;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PipeType {
    UNKNOWN(0),
    CIRCULAR(1),
    RECTANGULAR(2),
    OVAL(3),
    IRREGULAR(4);

    private final int value;

    public static PipeType fromValue(int value) {
        for (PipeType type : PipeType.values()) {
            if (type.getValue() == value) {
                return type;
            }
        }
        return UNKNOWN;
    }
}
