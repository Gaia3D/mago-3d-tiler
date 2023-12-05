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
    FBX("fbx"),
    KML("kml"),
    MAX_3DS("3ds"),
    MAX_ASE("ase"),
    OBJ("obj"),
    COLLADA("dae"),
    IFC("ifc"),
    GLTF("gltf"),
    GLB("glb"),
    CITY_GML("gml"),
    MODO("lxo"),
    LWO("lwo"),
    LWS("lws"),
    DirectX("x"),
    // 2D Formats,
    SHP("shp"),
    GEOJSON("geojson"),
    JSON("json"),
    LAS("las"),
    LAZ("laz"),
    // OUTPUT Formats
    B3DM("b3dm"),
    I3DM("i3dm"),
    PNTS("pnts"),
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
