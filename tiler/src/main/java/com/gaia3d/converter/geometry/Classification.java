package com.gaia3d.converter.geometry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joml.Vector4d;

@Getter
@AllArgsConstructor
public enum Classification {
    WALL("Wall", new Vector4d(0.5, 0.5, 0.5, 1.0)),
    ROOF("Roof", new Vector4d(1.0, 0.0, 0.0, 1.0)),
    DOOR("Door", new Vector4d(0.0, 0.0, 1.0, 1.0)),
    WINDOW("Window", new Vector4d(0.0, 1.0, 1.0, 0.5)),
    CEILING("Ceiling", new Vector4d(0.6, 0.6, 0.6, 1.0)),
    FLOOR("Floor", new Vector4d(0.2, 0.2, 0.2, 1.0)),
    STAIRS("Stairs", new Vector4d(0.4, 0.4, 0.4, 1.0)),
    GROUND("Ground", new Vector4d(0.0, 1.0, 0.0, 1.0)),
    WATER("Water", new Vector4d(0.0, 0.0, 1.0, 0.5)),
    UNKNOWN("Unknown", new Vector4d(0.5, 0.5, 0.5, 1.0));

    private final String value;
    private final Vector4d color;
}
