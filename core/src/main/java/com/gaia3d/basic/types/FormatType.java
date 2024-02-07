package com.gaia3d.basic.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

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
@RequiredArgsConstructor
public enum FormatType {
    // 3D Formats
    KML("kml", false),
    GLTF("gltf", true),
    GLB("glb", true),
    COLLADA("dae", true),
    MAX_3DS("3ds", false),
    MAX_ASE("ase", false),
    FBX("fbx", true),
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

    private final String extension;
    private final boolean yUpAxis;

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
