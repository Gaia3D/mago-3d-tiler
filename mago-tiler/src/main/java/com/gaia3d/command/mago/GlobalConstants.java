package com.gaia3d.command.mago;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

/**
 * Global options for Gaia3D Tiler.
 */
public class GlobalConstants {
    public static final String DEFAULT_TILES_VERSION = "1.1";
    public static final String DEFAULT_INPUT_FORMAT = "kml";
    public static final String DEFAULT_INSTANCE_FILE = "instance.dae";
    public static final int DEFAULT_MIN_LOD = 0;
    public static final int DEFAULT_MAX_LOD = 3;
    public static final int DEFAULT_MIN_GEOMETRIC_ERROR = 16;
    public static final int DEFAULT_MAX_GEOMETRIC_ERROR = Integer.MAX_VALUE;
    public static final int DEFAULT_MAX_TRIANGLES = 65536 * 8;
    public static final int DEFAULT_MAX_NODE_DEPTH = 32;
    public static final int DEFAULT_MAX_INSTANCE = 1024 * 64;
    public static final int DEFAULT_MAX_I3DM_FEATURE_COUNT = 1024;
    public static final int DEFAULT_MIN_I3DM_FEATURE_COUNT = 128;
    public static final int DEFAULT_POINT_PER_TILE = 300000;
    public static final int DEFAULT_POINT_RATIO = 100;
    public static final float POINTSCLOUD_HORIZONTAL_GRID = 500.0f; // in meters
    public static final float POINTSCLOUD_VERTICAL_GRID = 500.0f; // in meters
    public static final float POINTSCLOUD_HORIZONTAL_ARC = (1.0f / 60.0f / 60.0f) * 20.0f;
    public static final float POINTSCLOUD_VERTICAL_ARC = (1.0f / 60.0f / 60.0f) * 20.0f;
    public static final String DEFAULT_SOURCE_CRS_CODE = "3857";
    // The default target CRS is WGS 84 / ECEF (EPSG:4978)
    public static final String DEFAULT_TARGET_CRS_CODE = "4978";
    public static final CoordinateReferenceSystem DEFAULT_SOURCE_CRS = new CRSFactory().createFromName("EPSG:" + DEFAULT_SOURCE_CRS_CODE);
    public static final CoordinateReferenceSystem DEFAULT_TARGET_CRS = new CRSFactory().createFromName("EPSG:" + DEFAULT_TARGET_CRS_CODE);
    public static final String DEFAULT_HEIGHT_COLUMN = "height";
    public static final String DEFAULT_ALTITUDE_COLUMN = "altitude";
    public static final String DEFAULT_HEADING_COLUMN = "heading";
    public static final String DEFAULT_DIAMETER_COLUMN = "diameter";
    public static final String DEFAULT_SCALE_COLUMN = "scale";
    public static final String DEFAULT_DENSITY_COLUMN = "density";

    public static final double DEFAULT_ABSOLUTE_ALTITUDE = 0.0d;
    public static final double DEFAULT_MINIMUM_HEIGHT = 0.0d;
    public static final double DEFAULT_SKIRT_HEIGHT = 4.0d;
    public static final double DEFAULT_HEIGHT = 0.0d;
    public static final double DEFAULT_ALTITUDE = 0.0d;
    public static final double DEFAULT_SCALE = 1.0d;
    public static final double DEFAULT_DENSITY = 1.0d;
    public static final double DEFAULT_DIAMETER = 1.0d;
    public static final double DEFAULT_HEADING = 0.0d;

    public static final boolean DEFAULT_USE_QUANTIZATION = false;
    public static final int REALISTIC_LOD0_MAX_TEXTURE_SIZE = 1024;
    public static final int REALISTIC_MAX_TEXTURE_SIZE = 1024;
    public static final int REALISTIC_MIN_TEXTURE_SIZE = 32;
    public static final int REALISTIC_SCREEN_DEPTH_TEXTURE_SIZE = 256;
    public static final int REALISTIC_SCREEN_COLOR_TEXTURE_SIZE = 1024;
    public static final double REALISTIC_LEAF_TILE_SIZE = 25.0; // meters
    public static final int INSTANCE_POLYGON_CONTAINS_POINT_COUNTS = -1;
    public static final int RANDOM_SEED = 2620;
    public static final boolean MAKE_SKIRT = true;
}
