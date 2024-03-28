package com.gaia3d.process;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProcessOptions {
    // Default Options
    HELP("help", "h", "help", false, "Print Gelp"),
    VERSION("version", "v", "version", false, "Print Version Info"),
    QUIET("quiet", "q", "quiet", false, "Quiet mode/Silent mode"),

    // Path Options
    INPUT("input", "i", "input", true, "Input directory path"),
    OUTPUT("output", "o", "output", true, "Output directory file path"),
    INPUT_TYPE("inputType", "it", "inputType", true, "Input files type (kml, 3ds, fbx, obj, gltf, glb, las, laz, citygml, indoorgml, shp, geojson)(Default: kml)"),
    OUTPUT_TYPE("outputType", "ot", "outputType", true, "Output 3DTiles Type (b3dm, i3dm, pnts)(Default : b3dm)"),
    LOG("log", "l", "log", true, "Output log file path."),
    TERRAIN("terrain", "te", "terrain", true, "GeoTiff Terrain file path, 3D Object applied as clampToGround (Supports geotiff format)"),
    INSTANCE_FILE("instance", "if", "instance", true, "Instance file path for I3DM (Default: {OUTPUT}/instance.dae)"),
    RECURSIVE("recursive", "r", "recursive", false, "Tree directory deep navigation."),

    // Coordinate Options
    CRS("crs", "c", "crs", true,"Coordinate Reference Systems, EPSG Code(4326, 3857, 32652, 5186...)"),
    PROJ4("proj", "p", "proj", true, "Proj4 parameters (ex: +proj=tmerc +la...)"),

    // Execution Options
    MULTI_THREAD_COUNT("multiThreadCount", "mc", "multiThreadCount", true, "Multi-Thread count (Default: 4)"),

    // 3DTiles Options
    REFINE_ADD("refineAdd", "ra", "refineAdd", false, "Set 3D Tiles Refine 'ADD' mode"),
    MAX_COUNT("maxCount", "mx", "maxCount", true, "Maximum number of triangles per node."),
    MIN_LOD("minLod", "nl", "minLod", true, "min level of detail (Default: 0)"),
    MAX_LOD("maxLod", "xl", "maxLod", true, "Max Level of detail (Default: 3)"),
    MAX_POINTS("maxPoints", "mp", "maxPoints", true, "Limiting the maximum number of points in point cloud data. (Default: 65536)"),
    POINT_SCALE("pointScale", "ps", "pointScale", true, "Pointscloud geometryError scale setting (Default: 2)"),
    POINT_SKIP("pointSkip", "pk", "pointSkip", true, "Number of pointcloud omissions (ex: 1/4)(Default: 4)"),
    Y_UP_AXIS("yUpAxis", "ya", "yAxis", false, "Assign 3D root transformed matrix Y-UP axis"),
    ZERO_ORIGIN("zeroOrigin", "zo", "zeroOrigin", false, "[Experimental] fix 3d root transformed matrix origin to zero point."),
    IGNORE_TEXTURES("ignoreTextures", "igtx", "ignoreTextures", false,"Ignore diffuse textures. "),
    AUTO_UP_AXIS("autoUpAxis", "aa", "autoUpAxis", false, "Automatically Assign 3D Matrix Axes. If your 3D data up-axis is incorrect, try this option."),

    // Extrusion Options
    FLIP_COORDINATE("flipCoordinate", "fc", "flipCoordinate", false, "Flip x, y Coordinate (Default: false)"),
    NAME_COLUMN("nameColumn", "nc", "nameColumn", true, "Name column setting for extrusion model (Default: name)"),
    HEIGHT_COLUMN("heightColumn", "hc", "heightColumn", true, "Height column setting for extrusion model (Default: height)"),
    ALTITUDE_COLUMN("altitudeColumn", "ac", "altitudeColumn", true, "Altitude Column setting for extrusion model (Default: altitude)"),
    MINIMUM_HEIGHT("minimumHeight", "mh", "minimumHeight", true, "Minimum height value for extrusion model (Default: 1.0)"),
    ABSOLUTE_ALTITUDE("absoluteAltitude", "aa", "absoluteAltitude", true, "Absolute altitude value for extrusion model"),
    SKIRT_HEIGHT("skirtHeight", "sh", "skirtHeight", true, "Building Skirt height setting for extrusion model (Default: 4.0)"),

    DEBUG("debug", "d", "debug", false,"More detailed log output and stops on Multi-Thread bugs."),
    DEBUG_GLB("glb", "glb", "glb", false, "Create glb file with B3DM.");

    private final String longName;
    private final String shortName;
    private final String argName;
    private final boolean argRequired;
    private final String description;
}
