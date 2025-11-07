package com.gaia3d.basic.geometry.octree;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GaiaOctreeIndex {
    // children indices
    // down                         up
    // +---------+---------+        +---------+---------+
    // |         |         |        |         |         |
    // |    3    |    2    |        |    7    |    6    |
    // |         |         |        |         |         |
    // +---------+---------+        +---------+---------+
    // |         |         |        |         |         |
    // |    0    |    1    |        |    4    |    5    |
    // |         |         |        |         |         |
    // +---------+---------+        +---------+---------+

    UNDEFINED(-1),
    LEFT_FRONT_BOTTOM(0),
    RIGHT_FRONT_BOTTOM(1),
    RIGHT_REAR_BOTTOM(2),
    LEFT_REAR_BOTTOM(3),
    LEFT_FRONT_TOP(4),
    RIGHT_FRONT_TOP(5),
    RIGHT_REAR_TOP(6),
    LEFT_REAR_TOP(7);

    private final int index;

    public static GaiaOctreeIndex fromIndex(int index) {
        for (GaiaOctreeIndex value : values()) {
            if (value.getIndex() == index) {
                return value;
            }
        }
        throw new IllegalArgumentException("Index " + index + " is not a valid index");
    }
}
