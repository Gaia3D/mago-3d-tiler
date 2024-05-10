package com.gaia3d.basic.geometry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joml.Vector4d;

@Getter
@AllArgsConstructor
public enum Classification {
    WALL("Wall", new Vector4d(0.9, 0.9, 0.9, 1.0)),
    ROOF("Roof", new Vector4d(1.0, 0.3, 0.3, 1.0)),
    DOOR("Door", new Vector4d(1.3, 0.3, 1.0, 1.0)),
    WINDOW("Window", new Vector4d(0.5, 0.7, 1.0, 1.0)),
    CEILING("Ceiling", new Vector4d(0.5, 0.5, 0.5, 1.0)),
    FLOOR("Floor", new Vector4d(0.5, 0.5, 0.5, 1.0)),
    STAIRS("Stairs", new Vector4d(0.5, 0.5, 0.5, 1.0)),
    UNKNOWN("Unknown", new Vector4d(0.5, 0.5, 0.5, 1.0));

    private final String value;
    private final Vector4d color;
}
