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
    RECURSIVE("recursive", "r", "recursive", false, "deep directory exploration"),

    // 3D Options,
    INSTANCE_FILE("instance", "in", "instance", true, "instance file path. (Default: {OUTPUT}/instance.dae)"),

    //SWAP_YZ("swapYZ", "yz", "swapYZ", false, "swap vertices axis YZ"),
    REVERSE_TEXCOORD("reverseTexCoord", "rt", "reverseTexCoord", false, "texture y-axis coordinate reverse"),
    MULTI_THREAD("multiThread", "mt", "multiThread", false, "multi thread mode"),
    MULTI_THREAD_COUNT("multiThreadCount", "mc", "multiThreadCount", true, "multi thread count (Default: 8)"),
    PNG_TEXTURE("pngTexture", "pt", "pngTexture", false, "png texture mode"),
    Y_UP_AXIS("yUpAxis", "ya", "yAxis", false, "Assign 3D root transformed matrix Y-UP axis"),

    // 3D Tiles Options
    REFINE_ADD("refineAdd", "ra", "refineAdd", false, "refine add mode"),
    MAX_COUNT("maxCount", "mx", "maxCount", true, "max count of nodes (Default: 1024)"),
    MAX_LOD("maxLod", "xl", "maxLod", true, "max level of detail (Default: 3)"),
    MIN_LOD("minLod", "nl", "minLod", true, "min level of detail (Default: 0)"),
    MAX_POINTS("maxPoints", "mp", "maxPoints", true, "max points of pointcloud data (Default: 20000)"),
    POINT_SCALE("pointScale", "ps", "pointScale", true, "point scale setting (Default: 2)"),
    POINT_SKIP("pointSkip", "pk", "pointSkip", true, "point skip setting (Default: 4)"),

    // 2D Options
    FLIP_COORDINATE("flipCoordinate", "fc", "flipCoordinate", false, "flip x,y Coordinate. (Default: false)"),
    NAME_COLUMN("nameColumn", "nc", "nameColumn", true, "name column setting. (Default: name)"),
    HEIGHT_COLUMN("heightColumn", "hc", "heightColumn", true, "height column setting. (Default: height)"),
    ALTITUDE_COLUMN("altitudeColumn", "ac", "altitudeColumn", true, "altitude Column setting."),
    MINIMUM_HEIGHT("minimumHeight", "mh", "minimumHeight", true, "minimum height setting."),
    ABSOLUTE_ALTITUDE("absoluteAltitude", "aa", "absoluteAltitude", true, "absolute altitude mode."),
    SKIRT_HEIGHT("skirtHeight", "sh", "skirtHeight", true, "extrusion skirt height setting."),

    IGNORE_TEXTURES("ignoreTextures", "igtx", "ignoreTextures", false,"Ignore diffuse textures."),
    ZERO_ORIGIN("zeroOrigin", "zo", "zeroOrigin", false, "[Experimental] fix 3d root transformed matrix origin to zero point."),
    AUTO_UP_AXIS("autoUpAxis", "aa", "autoUpAxis", false, "automatically Assign 3D Matrix Axes"),
    TERRAIN("terrain", "te", "terrain", true, "terrain file path, 3D Object applied as clampToGround. Currently, we only support the geotiff extension."),

    DEBUG("debug", "d", "debug", false,"output more detailed log and stuck on a multi-thread bug."),
    DEBUG_GLTF("gltf", "gltf", "gltf", false, "create gltf with b3dm."),
    DEBUG_GLB("glb", "glb", "glb", false, "create glb file with b3dm.");

    private final String longName;
    private final String shortName;
    private final String argName;
    private final boolean argRequired;
    private final String description;
}
