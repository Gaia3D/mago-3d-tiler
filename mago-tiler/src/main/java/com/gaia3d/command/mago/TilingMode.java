package com.gaia3d.command.mago;

public enum TilingMode {
    EXPLICIT,
    IMPLICIT;

    public static TilingMode fromOption(String value) {
        if (value == null || value.isBlank()) {
            return EXPLICIT;
        }
        for (TilingMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid tiling mode: " + value + " (options: explicit, implicit)");
    }
}
