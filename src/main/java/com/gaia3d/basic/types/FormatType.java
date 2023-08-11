package com.gaia3d.basic.types;

import lombok.Getter;

import java.util.Arrays;

/**
 * Enum for the different types of formats that can be used in the application.
 * Each format has a corresponding extension.
 * The extension is used to determine the type of the file.
 * The extension is also used to determine the type of the file that is being downloaded.
 * @Author znkim
 * @Since 1.0.1
 * @See GaiaSet
 */
@Getter
public enum FormatType {
    KML("kml"),
    MAX_3DS("3ds"),
    OBJ("obj"),
    COLLADA("dae"),
    IFC("ifc"),
    GLTF("gltf"),
    GLB("glb"),
    B3DM("b3dm"),
    I3DM("i3dm"),
    TEMP("mgb");

    final String extension;

    FormatType(String extension) {
        this.extension = extension;
    }

    public static FormatType fromExtension(String extension) {
        return Arrays.stream(FormatType.values())
                .filter(type -> type.getExtension().equals(extension.toLowerCase()))
                .findFirst()
                .orElse(null);
    }
}
