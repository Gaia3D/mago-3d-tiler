package com.gaia3d.command.mago;

/**
 * Container of the written tileset, chosen with --outputArchive.
 */
public enum OutputArchive {
    NONE, // a folder of files
    THREE_TZ; // a single 3D Tiles Archive (.3tz) file

    public static OutputArchive fromOption(String value) {
        if (value == null || value.isBlank() || value.equalsIgnoreCase("none")) {
            return NONE;
        }
        if (value.equalsIgnoreCase("3tz")) {
            return THREE_TZ;
        }
        throw new IllegalArgumentException("Invalid output archive: " + value + " (options: none, 3tz)");
    }
}
