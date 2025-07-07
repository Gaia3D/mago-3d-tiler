package com.gaia3d.command.mago;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProcessOptions {
    // Default Options
    HELP("help", "h", false, "Print Help"),
    QUIET("quiet", "q", false, "Quiet mode/Silent mode"),
    LEAVE_TEMP("leaveTemp", "lt", false, "Leave temporary files"),
    MERGE("merge", "m", false, "Merge tileset.json files"),

    // Path Options
    INPUT("input", "i", true, "Input directory path"),
    OUTPUT("output", "o", true, "Output directory file path"),
    INPUT_TYPE("inputType", "it", true, "Input files type [kml, 3ds, fbx, obj, gltf/glb, las/laz, citygml, indoorgml, shp, geojson, gpkg]"),
    OUTPUT_TYPE("outputType", "ot", true, "Output 3DTiles Type [b3dm, i3dm, pnts]"),
    LOG("log", "l", true, "Output log file path."),
    RECURSIVE("recursive", "r", false, "Tree directory deep navigation."),

    // For Terrain
    TERRAIN("terrain", "te", true, "GeoTiff Terrain file path, 3D Object applied as clampToGround (Supports geotiff format)"),

    // For I3DM
    INSTANCE_FILE("instance", "if", true, "Instance file path for I3DM (Default: {OUTPUT}/instance.dae)"),

    // Coordinate Options
    CRS("crs", "c", true,"Coordinate Reference Systems, EPSG Code(4326, 3857, 32652, 5186...)"),
    PROJ4("proj", "p",  true, "Proj4 parameters (ex: +proj=tmerc +la...)"),
    X_OFFSET("xOffset", "xo", true, "X Offset value for coordinate transformation"),
    Y_OFFSET("yOffset", "yo", true, "Y Offset value for coordinate transformation"),
    Z_OFFSET("zOffset", "zo", true, "Z Offset value for coordinate transformation"),

    // Manual Coordinate Options
    LONGITUDE("longitude", "lon", true, "Longitude value for coordinate transformation. (The lon lat option must be used together)."),
    LATITUDE("latitude", "lat", true, "Latitude value for coordinate transformation. (The lon lat option must be used together)."),

    // Execution Options
    MULTI_THREAD_COUNT("multiThreadCount", "mc", true, "set Multi-Thread count"),

    // 3DTiles Options
    REFINE_ADD("refineAdd", "ra", false, "Set 3D Tiles Refine 'ADD' mode"),
    MAX_COUNT("maxCount", "mx", true, "Maximum number of triangles per node."),
    MIN_LOD("minLod", "nl", true, "min level of detail"),
    MAX_LOD("maxLod", "xl", true, "Max Level of detail"),
    MIN_GEOMETRIC_ERROR("minGeometricError", "ng", true, "Minimum geometric error"),
    MAX_GEOMETRIC_ERROR("maxGeometricError", "mg", true, "Maximum geometric error"),
    MAX_POINTS("maxPoints", "mp", true, "Maximum number of points per a tile"),

    // PointCloud Options
    POINT_RATIO("pointRatio", "pcr", true, "Percentage of points from original data"),
    POINT_PRECISION("sourcePrecision", "sp", false, "Create pointscloud tile with original precision. "),
    POINT_FORCE_4BYTE_RGB("force4ByteRGB", "f4", false, "Force 4Byte RGB for pointscloud tile."),

    // Mesh Options
    MESH_QUANTIZATION("quantize", "qt", false, "Quantize mesh to reduce glb size via \"KHR_mesh_quantization\" Extension"),
    ROTATE_X_AXIS("rotateXAxis", "rx", true, "Rotate the X-Axis in degrees"),

    // 2D Vector Options
    FLIP_COORDINATE("flipCoordinate", "fc", false, "Flip x, y coordinate for 2D Original Data."),
    NAME_COLUMN("nameColumn", "nc", true, "Name column setting for extrusion model"),
    HEIGHT_COLUMN("heightColumn", "hc", true, "Height column setting for extrusion model"),
    ALTITUDE_COLUMN("altitudeColumn", "ac", true, "Altitude Column setting for extrusion model"),

    // 2D Point Vector (I3DM) Options
    HEADING_COLUMN("headingColumn", "hd", true, "Heading column setting for I3DM converting"),
    SCALE_COLUMN("scaleColumn", "scl", true, "Scale column setting for I3DM converting"),
    DENSITY_COLUMN("densityColumn", "den", true, "Density column setting for I3DM polygon converting"),

    // 2D Line Vector  Options
    DIAMETER_COLUMN("diameterColumn", "dc", true, "Diameter column setting for pipe extrusion model, Specify a length unit for Diameter in millimeters(mm) (Default Column: diameter)"),

    MINIMUM_HEIGHT("minimumHeight", "mh", true, "Minimum height value for extrusion model"),
    ABSOLUTE_ALTITUDE("absoluteAltitude", "aa", true, "Absolute altitude value for extrusion model"),
    SKIRT_HEIGHT("skirtHeight", "sh", true, "Building Skirt height setting for extrusion model"),

    ATTRIBUTE_FILTER("attributeFilter", "af", true, "Attribute filter setting for extrusion model ex) \"classification=window,door;type=building\""),

    // debug Options
    DEBUG("debug", "d", false,"[DEBUG] More detailed log output and stops on Multi-Thread bugs."),
    DEBUG_GLB("glb", "glb", false, "[DEBUG] Create glb file with B3DM."),
    IGNORE_TEXTURES("ignoreTextures", "igtx", false,"[DEBUG] Ignore diffuse textures. "),

    // Experimental Options
    AUTO_UP_AXIS("autoUpAxis", "aa",  false, "Automatically Assign 3D Matrix Axes. If your 3D data up-axis is incorrect, try this option."),
    SWAP_UP_AXIS("swapUpAxis", "su",  false, "Rotate the matrix -90 degrees about the X-axis."),
    FLIP_UP_AXIS("flipUpAxis", "ru", false, "Rotate the matrix 180 degrees about the X-axis."),
    LARGE_MESH("largeMesh", "lm", false, "[Experimental] Large Mesh Splitting Mode)"),
    VOXEL_LOD("voxelLod", "vl", false, "[Experimental] Voxel Level Of Detail setting for i3dm"),
    PHOTOGRAMMETRY("photogrammetry", "pg", false, "[Experimental][GPU] generate b3dm for photogrammetry model"),
    ZERO_ORIGIN("zeroOrigin", "zo", false, "[Experimental] fix 3d root transformed matrix origin to zero point."),
    TILES_VERSION("tilesVersion", "tv", true, "[Experimental] 3DTiles Version [Default: 1.1][1.0, 1.1]"),;

    private final String longName;
    private final String shortName;
    private final boolean argRequired;
    private final String description;
}
