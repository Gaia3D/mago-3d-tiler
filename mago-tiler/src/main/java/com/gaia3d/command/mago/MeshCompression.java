package com.gaia3d.command.mago;

/**
 * Geometry compression of the glTF content, chosen with --meshCompression.
 */
public enum MeshCompression {
    NONE,
    QUANTIZE, // KHR_mesh_quantization
    DRACO; // KHR_draco_mesh_compression

    public static MeshCompression fromOption(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        for (MeshCompression compression : values()) {
            if (compression.name().equalsIgnoreCase(value)) {
                return compression;
            }
        }
        throw new IllegalArgumentException("Invalid mesh compression: " + value + " (options: none, quantize, draco)");
    }
}
