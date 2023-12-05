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
    OUTPUT_TYPE("outputType", "ot", "outputType", true, "output file type"),
    CRS("crs", "c", "crs", true,"Coordinate Reference Systems, only epsg code (4326, 3857, etc...)"),
    PROJ4("proj", "p", "proj", true, "proj4 parameters (ex: +proj=tmerc +la...)"),

    // 3D Options
    RECURSIVE("recursive", "r", "recursive", false, "deep directory exploration"),
    //SWAP_YZ("swapYZ", "yz", "swapYZ", false, "swap vertices axis YZ"),
    REVERSE_TEXCOORD("reverseTexCoord", "rt", "reverseTexCoord", false, "texture y-axis coordinate reverse"),
    MULTI_THREAD("multiThread", "mt", "multiThread", false, "multi thread mode"),
    MULTI_THREAD_COUNT("multiThreadCount", "mc", "multiThreadCount", true, "multi thread count (Default: 8)"),
    PNG_TEXTURE("pngTexture", "pt", "pngTexture", false, "png texture mode"),
    Y_UP_AXIS("yUpAxis", "ya", "yAxis", false, "Assign 3D root transformed matrix Y-UP axis"),

    // 3D Tiles Options
    REFINE_ADD("refineAdd", "ra", "refineAdd", false, "refine addd mode"),
    MAX_COUNT("maxCount", "mx", "maxCount", true, "max count of nodes (Default: 1024)"),
    MAX_LOD("maxLod", "xl", "maxLod", true, "max level of detail (Default: 3)"),
    MIN_LOD("minLod", "nl", "minLod", true, "min level of detail (Default: 0)"),
    MAX_POINTS("maxPoints", "mp", "maxPoints", true, "max points of pointcloud data (Default: 20000)"),

    // 2D Options
    FLIP_COORDINATE("flipCoordinate", "fc", "flipCoordinate", false, "flip x,y Coordinate."),
    NAME_COLUMN("nameColumn", "nc", "nameColumn", true, "name column setting. (Default: name)"),
    HEIGHT_COLUMN("heightColumn", "hc", "heightColumn", true, "height column setting. (Default: height)"),
    ALTITUDE_COLUMN("altitudeColumn", "ac", "altitudeColumn", true, "altitude Column setting."),
    MINIMUM_HEIGHT("minimumHeight", "mh", "minimumHeight", true, "minimum height setting."),
    ABSOLUTE_ALTITUDE("absoluteAltitude", "aa", "absoluteAltitude", true, "absolute altitude mode."),

    //Experimental,
    ZERO_ORIGIN("zeroOrigin", "zo", "zeroOrigin", false, "[Experimental] fix 3d root transformed matrix origin to zero point."),
    AUTO_UP_AXIS("autoUpAxis", "aa", "autoUpAxis", false, "[Experimental] automatically Assign 3D Matrix Axes"),
    //Z_UP_AXIS("zAxis", "ya", "zAxis", false, "[Experimental] Assign 3D root transformed matrix Z-UP axis"),
    //GENERATE_NORMALS("genNormals", "gn", "genNormals", false, "generate normals"),
    //SCALE("scale", "sc", "scale", false, "scale factor"),
    //STRICT("strict", "st", "strict", false, "strict mode"),

    DEBUG("debug", "d", "debug", false,"debug mode"),
    DEBUG_ALL_DRAWING("debugAllDrawing", "dad", "debugAllDrawing", false,"debug all drawing"),
    DEBUG_IGNORE_TEXTURES("debugIgnoreTextures", "dit", "debugIgnoreTextures", false,"debug ignore textures"),
    DEBUG_GLTF("gltf", "gltf", "gltf", false, "create gltf file."),
    DEBUG_GLB("glb", "glb", "glb", false, "create glb file.");

    private final String longName;
    private final String shortName;
    private final String argName;
    private final boolean argRequired;
    private final String description;
}
