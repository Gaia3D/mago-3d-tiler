package com.gaia3d.process;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProcessOptions {
    HELP("help", "h", "help", false, "print this message"),
    VERSION("version", "v", "version", false, "print version"),
    QUIET("quiet", "q", "quiet", false, "quiet mode"),
    LOG("log", "l", "log", true, "output log file path"),
    INPUT("input", "i", "input", true, "input file path"),
    OUTPUT("output", "o", "output", true, "output file path"),
    INPUT_TYPE("inputType", "it", "inputType", true, "input file type (kml, 3ds, obj, gltf, etc...)"),
    //OUTPUT_TYPE("outputType", "ot", "outputType", true, "output file type"),
    CRS("crs", "c", "crs", true,"Coordinate Reference Systems, only epsg code (4326, 3857, etc...)"),
    RECURSIVE("recursive", "r", "recursive", false, "deep directory exploration"),
    SWAP_YZ("swapYZ", "yz", "swapYZ", false, "Swap Vertex Axis YZ"),
    REVERSE_TEXCOORD("reverseTexCoord", "rt", "reverseTexCoord", false, "Reverse Texture Coordinate Y"),
    MULTI_THREAD("multiThread", "mt", "multiThread", false, "Multi Thread Mode"),

    //Experimental
    //AUTO_AXIS("autoAxis", "aa", "autoAxis", false, "[Experimental] auto axis"),
    //GENERATE_NORMALS("genNormals", "gn", "genNormals", false, "generate normals"),

    //SCALE("scale", "sc", "scale", false, "scale factor"),
    //STRICT("strict", "st", "strict", false, "strict mode"),

    DEBUG("debug", "d", "debug", false,"debug mode"),
    //IGNORE_TEXTURES("ignoreTextures", "itx","ignoreTextures", false, "ignore textures"),

    GLTF("gltf", "gltf", "gltf", false, "create gltf file"),
    GLB("glb", "glb", "glb", false, "create glb file"),
    MAX_COUNT("maxCount", "mx", "maxCount", true, "max count of nodes (Default: 256)"),
    MAX_LOD("maxLod", "xl", "maxLod", true, "max level of detail (Default: 3)"),
    MIN_LOD("minLod", "nl", "minLod", true, "min level of detail (Default: 0)");

    private final String longName;
    private final String shortName;
    private final String argName;
    private final boolean argRequired;
    private final String description;
}
