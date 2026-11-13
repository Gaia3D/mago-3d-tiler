package com.gaia3d.command.mago;

/**
 * Texture encoding of the glTF content, chosen with --textureCompression.
 */
public enum TextureCompression {
    NONE, // the source format, JPEG or PNG
    KTX2; // KTX2 Basis Universal ETC1S with mipmaps, KHR_texture_basisu

    public static TextureCompression fromOption(String value) {
        if (value == null || value.isBlank()) {
            return NONE;
        }
        for (TextureCompression compression : values()) {
            if (compression.name().equalsIgnoreCase(value)) {
                return compression;
            }
        }
        throw new IllegalArgumentException("Invalid texture compression: " + value + " (options: none, ktx2)");
    }
}
