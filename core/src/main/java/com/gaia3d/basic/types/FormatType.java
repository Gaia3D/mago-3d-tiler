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
    // 3D Formats
    FBX("fbx", true),
    GLTF("gltf", true),
    GLB("glb", true),
    KML("kml", false),
    COLLADA("dae", false),
    MAX_3DS("3ds", false),
    MAX_ASE("ase", false),
    OBJ("obj", false),
    IFC("ifc", false),
    CITY_GML("gml", false),
    MODO("lxo", false),
    LWO("lwo", false),
    LWS("lws", false),
    DirectX("x", false),
    // 2D Formats,
    SHP("shp", false),
    GEOJSON("geojson", false),
    JSON("json", false),
    LAS("las", false),
    LAZ("laz", false),
    // OUTPUT Formats
    B3DM("b3dm", true),
    I3DM("i3dm", true),
    PNTS("pnts", true),
    TEMP("mgb", false);

    final String extension;
    final boolean yUpAxis;

    FormatType(String extension, boolean yUpAxis) {
        this.extension = extension;
        this.yUpAxis = yUpAxis;
    }

    public static FormatType fromExtension(String extension) {
        if (extension == null) {
            return null;
        }
        return Arrays.stream(FormatType.values())
                .filter(type -> type.getExtension().equals(extension.toLowerCase()))
                .findFirst()
                .orElse(null);
    }
}
