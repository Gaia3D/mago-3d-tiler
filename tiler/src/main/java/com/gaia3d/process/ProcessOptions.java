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
    LEAVE_TEMP("leaveTemp", "lt", "leaveTemp", false, "Leave temporary files"),
    MERGE("merge", "m", "merge", false, "Merge tileset.json files"),

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
    X_OFFSET("xOffset", "xo", "xOffset", true, "X Offset value for coordinate transformation"),
    Y_OFFSET("yOffset", "yo", "yOffset", true, "Y Offset value for coordinate transformation"),
    Z_OFFSET("zOffset", "zo", "zOffset", true, "Z Offset value for coordinate transformation"),

    // Execution Options
    MULTI_THREAD_COUNT("multiThreadCount", "mc", "multiThreadCount", true, "set Multi-Thread count"),

    // 3DTiles Options
    REFINE_ADD("refineAdd", "ra", "refineAdd", false, "Set 3D Tiles Refine 'ADD' mode"),
    MAX_COUNT("maxCount", "mx", "maxCount", true, "Maximum number of triangles per node."),
    MIN_LOD("minLod", "nl", "minLod", true, "min level of detail (Default: 0)"),
    MAX_LOD("maxLod", "xl", "maxLod", true, "Max Level of detail (Default: 3)"),
    MIN_GEOMETRIC_ERROR("minGeometricError", "ng", "minGeometricError", true, "Minimum geometric error (Default: 16.0)"),
    MAX_GEOMETRIC_ERROR("maxGeometricError", "mg", "maxGeometricError", true, "Maximum geometric error (Default: Integer max value)"),
    MAX_POINTS("maxPoints", "mp", "maxPoints", true, "Maximum number of points per a tile (Default: 100000)"),
    POINT_RATIO("pointRatio", "pcr", "pointRatio", true, "Percentage of points from original data (Default: 50)"),
    POINT_PRECISION("sourcePrecision", "sp", "sourcePrecision", false, "Create pointscloud tile with original precision. (Slow)"),
    POINT_FORCE_4BYTE_RGB("force4ByteRGB", "f4", "force4ByteRGB", false, "Force 4Byte RGB for pointscloud tile."),

    ROTATE_X_AXIS("rotateXAxis", "rx", "rotateXAxis", true, "Rotate the X-Axis in degrees (Unit: degrees)(Default: 0.0)"),

    SWAP_UP_AXIS("swapUpAxis", "su", "swapUpAxis", false, "Rotate the matrix -90 degrees about the X-axis. (Default: false)"),
    FLIP_UP_AXIS("flipUpAxis", "ru", "flipUpAxis", false, "Rotate the matrix 180 degrees about the X-axis. (Default: false)"),

    IGNORE_TEXTURES("ignoreTextures", "igtx", "ignoreTextures", false,"Ignore diffuse textures. "),
    AUTO_UP_AXIS("autoUpAxis", "aa", "autoUpAxis", false, "Automatically Assign 3D Matrix Axes. If your 3D data up-axis is incorrect, try this option."),

    // Extrusion Options
    FLIP_COORDINATE("flipCoordinate", "fc", "flipCoordinate", false, "Flip x, y coordinate for 2D Original Data. (Default: false)"),
    NAME_COLUMN("nameColumn", "nc", "nameColumn", true, "Name column setting for extrusion model (Default Column: name)"),
    HEIGHT_COLUMN("heightColumn", "hc", "heightColumn", true, "Height column setting for extrusion model ((Default Column: height)"),
    ALTITUDE_COLUMN("altitudeColumn", "ac", "altitudeColumn", true, "Altitude Column setting for extrusion model ((Default Column: altitude)"),
    HEADING_COLUMN("headingColumn", "hd", "headingColumn", true, "Heading column setting for I3DM converting (Default Column: heading)"),
    DIAMETER_COLUMN("diameterColumn", "dc", "diameterColumn", true, "Diameter column setting for extrusion model, Specify a length unit for Diameter in millimeters(mm) (Default Column: diameter)"),

    MINIMUM_HEIGHT("minimumHeight", "mh", "minimumHeight", true, "Minimum height value for extrusion model (Default: 1.0)"),
    ABSOLUTE_ALTITUDE("absoluteAltitude", "aa", "absoluteAltitude", true, "Absolute altitude value for extrusion model"),
    SKIRT_HEIGHT("skirtHeight", "sh", "skirtHeight", true, "Building Skirt height setting for extrusion model (Default: 4.0)"),

    DEBUG("debug", "d", "debug", false,"More detailed log output and stops on Multi-Thread bugs."),
    DEBUG_GLB("glb", "glb", "glb", false, "Create glb file with B3DM."),

    LARGE_MESH("largeMesh", "lm", "largeMesh", false, "[Experimental] Large Mesh Splitting Mode (Default: false)"),
    VOXEL_LOD("voxelLod", "vl", "voxelLod", false, "[Experimental] Voxel Level Of Detail setting for i3dm (Default: false)"),
    PHOTOREALISTIC("photorealistic", "pr", "photorealistic", false, "[Experimental] Photorealistic mode for b3dm (Default: false)"),
    ZERO_ORIGIN("zeroOrigin", "zo", "zeroOrigin", false, "[Experimental] fix 3d root transformed matrix origin to zero point.");

    private final String longName;
    private final String shortName;
    private final String argName;
    private final boolean argRequired;
    private final String description;
}
