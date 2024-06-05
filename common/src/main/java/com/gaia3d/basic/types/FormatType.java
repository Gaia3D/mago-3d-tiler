package com.gaia3d.basic.types;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * Enum for the different types of formats that can be used in the application.
 * Each format has a corresponding extension.
 * The extension is used to determine the type of the file.
 * The extension is also used to determine the type of the file that is being downloaded.
 */
@Getter
@RequiredArgsConstructor
public enum FormatType {
    // 3D Formats
    KML("kml", "kml", false),
    GLTF("gltf", "glb", false),
    GLB("glb", "gltf", false),
    COLLADA("dae", "dae", true),
    MAX_3DS("3ds", "3ds",false),
    MAX_ASE("ase", "ase", false),
    FBX("fbx", "fbx", false),
    OBJ("obj","obj", false),
    IFC("ifc", "ifc",false),
    CITYGML("gml","xml", false),
    INDOORGML("gml","xml", false),
    MODO("lxo", "lxo", false),
    LWO("lwo", "lwo", false),
    LWS("lws", "lws", false),
    DirectX("x", "x", false),
    // 2D Formats,
    SHP("shp", "shp",false),
    GEOJSON("geojson", "json", false),
    //JSON("json", "", false),
    LAS("las", "laz", false),
    LAZ("laz", "las", false),
    // OUTPUT Formats
    B3DM("b3dm", "gltf", true),
    I3DM("i3dm", "gltf",true),
    PNTS("pnts", "gltf",true),
    TEMP("tmp", "tmp", false);

    private final String extension;
    private final String subExtension;
    private final boolean yUpAxis;

    public static FormatType fromExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return null;
        }
        return Arrays.stream(FormatType.values())
                .filter((type) -> {
                    boolean compareName = type.name().equalsIgnoreCase(extension);
                    boolean compareExtension = type.getExtension().equalsIgnoreCase(extension.toLowerCase());
                    boolean compareSubExtension = type.getSubExtension().equalsIgnoreCase(extension.toLowerCase());
                    return compareName || compareExtension || compareSubExtension;
                })
                .findFirst()
                .orElse(null);
    }
}
