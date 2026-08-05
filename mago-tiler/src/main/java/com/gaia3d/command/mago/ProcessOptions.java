package com.gaia3d.command.mago;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum ProcessOptions {
    /* Default Options */
    HELP("help", "h", false, false, "Print Help"),
    QUIET("quiet", "q", false, false, "Quiet mode/Silent mode"),
    LEAVE_TEMP("leaveTemp", "lt", false, false, "Leave temporary files"),
    MERGE("merge", "m", false, false, "Merge tileset.json files"),

    /* Path Options */
    INPUT_PATH("input", "i", true, true, "[Required] Input directory path"),
    OUTPUT_PATH("output", "o", true, true, "[Required] Output directory path"),
    TEMP_PATH("temp", "t", true, false, "Temporary directory path \n(default: {OUTPUT}/temp)"),
    INPUT_TYPE("inputType", "it", true, false, "Input files type \n(options: kml, 3ds, fbx, obj, gltf/glb, las/laz, citygml, indoorgml, shp, geojson, gpkg)"),
    OUTPUT_TYPE("outputType", "ot", true, false, "Output 3DTiles Type [b3dm, i3dm, pnts]"),
    LOG_PATH("log", "l", true, false, "Output log file path."),
    RECURSIVE("recursive", "r", false, false, "Tree directory deep navigation."),

    TERRAIN_PATH("terrain", "te", true, false, "Terrain path for clampToGround (GeoTIFF or a local Quantized Mesh layer.json)"),
    GEOID_PATH("geoid", "ge", true, false, "Geoid file path for height correction, \n(default: Ellipsoid)(options: Ellipsoid, EGM84, EGM96, EGM2008 or GeoTIFF File Path)"),
    INSTANCE_PATH("instance", "if", true, false, "Instance file path for I3DM \n(default: {OUTPUT}/instance.dae)"),

    MESH_QUANTIZATION("quantize", "qt", false, false, "Quantize glTF 3DMesh via \"KHR_mesh_quantization\" Extension"),
    TILES_VERSION("tilesVersion", "tv", true, false, "3DTiles Version \n(default: 1.1)(options: 1.0, 1.1)"),

    /* Coordinate Setting Options */
    CRS("crs", "c", true, false, "Set input data CRS (Coordinate Reference System) \n" +
            "(default: 3857)(options: 4326, 3857, 4978, 32652, 5186...) \n" +
            "(ECEF: 4978, WGS84: 4326, Web Mercator: 3857)"),
    PROJ4("proj", "p", true, false, "Set Proj4 parameters \n" +
            "(ex: +proj=tmerc +lat_0=...) \n" +
            "When this option is set, the 'crs' option is ignored."),
    X_OFFSET("xOffset", "xo", true, false, "X Offset value for coordinate transformation"),
    Y_OFFSET("yOffset", "yo", true, false, "Y Offset value for coordinate transformation"),
    Z_OFFSET("zOffset", "zo", true, false, "Z Offset value for coordinate transformation"),

    // Degree Coordinate Options
    LONGITUDE("longitude", "lon", true, false, "Longitude value for coordinate transformation. (The lon lat option must be used together)."),
    LATITUDE("latitude", "lat", true, false, "Latitude value for coordinate transformation. (The lon lat option must be used together)."),

    // Rotation Options
    ROTATE_X_AXIS("rotateXAxis", "rx", true, false, "Rotate the X-Axis in degrees"),

    /* Tiling Control Options */
    REFINE_ADD("refineAdd", "ra", false, false, "[Tileset] Set 3D Tiles Refine 'ADD' mode"),
    MAX_COUNT("maxCount", "mx", true, false, "[Tileset] Maximum number of triangles per node."),
    MIN_LOD("minLod", "nl", true, false, "[Tileset] min level of detail"),
    MAX_LOD("maxLod", "xl", true, false, "[Tileset] Max Level of detail"),
    MIN_GEOMETRIC_ERROR("minGeometricError", "ng", true, false, "[Tileset] Minimum geometric error"),
    MAX_GEOMETRIC_ERROR("maxGeometricError", "mg", true, false, "[Tileset] Maximum geometric error"),
    MAX_POINTS("maxPoints", "mp", true, false, "[Tileset] Maximum number of points per a tile"),

    // PointCloud Options
    POINT_RATIO("pointRatio", "pcr", true, false, "[PointCloud] Percentage of points from original data"),
    POINT_PRECISION("sourcePrecision", "sp", false, false, "[PointCloud] Create pointscloud tile with original precision. "),
    POINT_FORCE_4BYTE_RGB("force4ByteRGB", "f4", false, false, "[PointCloud] Force 4Byte RGB for pointscloud tile."),

    /* GIS Vector Generate Options */
    FLIP_COORDINATE("flipCoordinate", "fc", false, false, "[GISVector] Flip x, y coordinate for 2D Original Data."),
    ATTRIBUTE_FILTER("attributeFilter", "af", true, false, "[GISVector] Attribute filter setting for extrusion model (ex: \"classification=window,door;type=building\")"),
    // GIS Vector Column Options
    NAME_COLUMN("nameColumn", "nc", true, false, "[GISVector] Specify the column name for the feature name. \n (default: name)"),
    ALTITUDE_COLUMN("altitudeColumn", "ac", true, false, "[GISVector] Specify the column name for the altitude base height. \n (default: altitude)(units: meters)"),
    HEADING_COLUMN("headingColumn", "hd", true, false, "[GISVector][I3DM] Specify the column name for the heading rotation. \n (default: heading)(units: degrees)"),
    SCALE_COLUMN("scaleColumn", "scl", true, false, "[GISVector][I3DM] Specify the column name for the scale value. \n (default: scale)(units: meters)"),
    DENSITY_COLUMN("densityColumn", "den", true, false, "[GISVector][I3DM] Specify the column name for the density value. \n (default: density)"),
    DIAMETER_COLUMN("diameterColumn", "dc", true, false, "[GISVector][Pipe] Specify the column name for the pipe diameter value. \n (default: diameter)(units: millimeters)"),
    HEIGHT_COLUMN("heightColumn", "hc", true, false, "[GISVector][Extrusion] Specify the column name for the reference ceil level height. \n (default: height)(units: meters)"),
    // Height Options
    ABSOLUTE_ALTITUDE("absoluteAltitude", "aa", true, false, "[GISVector] Set absolute altitude value for all features (overrides altitude column)"),
    MINIMUM_HEIGHT("minimumHeight", "mh", true, false, "[GISVector][Extrusion] Set Building Minimum height \n(default: 0.0)(units: meters)"),
    SKIRT_HEIGHT("skirtHeight", "sh", true, false, "[GISVector][Extrusion] Set Building Skirt height \n(default: 4.0)(units: meters)"),

    /* Experimental Options */
    PHOTOGRAMMETRY("photogrammetry", "pg", false, false, "[Experimental] Generate b3dm with the compatibility-focused photogrammetry pipeline."),
    SPLIT_BY_NODE("splitByNode", "sbn", false, false, "[Experimental] Split tiles by nodes of scene."),
    CURVATURE_CORRECTION("curvatureCorrection", "cc", false, false, "[Experimental] Apply curvature correction for ellipsoid surface."),
    UPDATE_ROOT_TRANSFORM("updateRootTransform", "urt", true, false, "[Experimental] Add 16 comma-separated transform values to the root node of tileset.json."),
    TILING_MODE("tilingMode", "tm", true, false, "[Experimental] 3DTiles hierarchy mode \n(default: explicit)(options: explicit, implicit)"),
    IMPLICIT_SUBTREE_LEVELS("implicitSubtreeLevels", "isl", true, false, "[Experimental] Implicit tiling subtree levels \n(default: 4)"),

    /* Deprecated Options */
    MULTI_THREAD_COUNT("multiThreadCount", "mc", true, false, "[Deprecated] set thread count"),
    DEBUG_GLB("glb", "glb", false, false, "[Deprecated] Create glb file with B3DM."),
    IGNORE_TEXTURES("ignoreTextures", "igtx", false, false, "[Deprecated] Ignore diffuse textures. "),

    /* Validation Options */
    VALIDATION_REPORT("validationReport", "vr", false, false, "[Validation] Save per-file validation JSON reports to the temp directory and write a batch summary to the output directory."),
    /* Verbose Options */
    VERBOSE("verbose", "v", false, false, "[Verbose] Show debug-level logs without changing the log format or disabling async logging."),
    /* Debug Options */
    DEBUG("debug", "d", false, false, "[DEBUG] More detailed log output and stops on Multi-Thread bugs.");

    private final String longName;
    private final String shortName;
    private final boolean argValueRequired;
    private final boolean required;
    private final String description;
}
