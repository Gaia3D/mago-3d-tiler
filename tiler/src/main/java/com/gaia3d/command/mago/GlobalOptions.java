package com.gaia3d.command.mago;

import com.gaia3d.TilerExtensionModule;
import com.gaia3d.basic.exception.Reporter;
import com.gaia3d.basic.types.FormatType;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.joml.Vector3d;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Global options for Gaia3D Tiler.
 */
@Setter
@Getter
@NoArgsConstructor
@Slf4j
public class GlobalOptions {
    public static final String DEFAULT_INPUT_FORMAT = "kml";
    public static final String DEFAULT_INSTANCE_FILE = "instance.dae";
    public static final int DEFAULT_MIN_LOD = 0;
    public static final int DEFAULT_MAX_LOD = 3;
    public static final int DEFAULT_MIN_GEOMETRIC_ERROR = 16;
    public static final int DEFAULT_MAX_GEOMETRIC_ERROR = 1024;
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
    public static final String DEFAULT_CRS_CODE = "3857"; // 4326 -> 3857
    public static final CoordinateReferenceSystem DEFAULT_CRS = new CRSFactory().createFromName("EPSG:" + DEFAULT_CRS_CODE);
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
    /* singleton */
    private static final GlobalOptions instance = new GlobalOptions();
    private Reporter reporter;

    private String version; // version flag
    private String javaVersionInfo; // java version flag
    private String programInfo; // program info flag

    private long fileCount = 0;
    private long tileCount = 0;
    private long tilesetSize = 0;

    private String inputPath; // input file or dir path
    private String outputPath; // output dir path
    private String logPath; // log file path

    private String terrainPath; // terrain file path
    private String instancePath; // instance file path

    private FormatType inputFormat; // input file format
    private FormatType outputFormat; // output file format

    // projection options
    private CoordinateReferenceSystem crs; // default crs
    private String proj; // proj4 string
    private boolean cartesian = false;
    private Vector3d translateOffset; // origin offset

    private boolean isSourcePrecision = false;
    private int maximumPointPerTile = 0; // Maximum number of points per a tile
    private int pointRatio = 0; // Percentage of points from original data
    private boolean force4ByteRGB = false; // Force 4Byte RGB for pointscloud tile

    private boolean useQuantization; // Use quantization via KHR_mesh_quantization

    // Level of Detail
    private int minLod;
    private int maxLod;
    // Geometric Error
    private int minGeometricError;
    private int maxGeometricError;

    private int maxTriangles;
    private int maxInstance;
    private int maxNodeDepth;

    // Debug Mode
    private boolean debug = false;
    private boolean debugLod = false;
    private boolean isLeaveTemp = false;

    private boolean gltf = false;
    private boolean glb = false;
    private boolean classicTransformMatrix = false;

    private byte multiThreadCount;

    /* 3D Data Options */
    private boolean recursive = false; // recursive flag
    private boolean autoUpAxis = false; // automatically assign 3D matrix axes flag
    //private boolean rotateUpAxis = false; // y up axis flag

    private boolean swapUpAxis = false; // swap up axis flag
    private boolean flipUpAxis = false; // reverse up axis flag
    private double rotateX = 0; // degrees

    private boolean refineAdd = false; // 3dTiles refine option ADD fix flag
    private boolean flipCoordinate = false; // flip coordinate flag for 2D Data
    private boolean ignoreTextures = false; // ignore textures flag

    // [Experimental] 3D Data Options
    private boolean largeMesh = false; // [Experimental] large mesh splitting mode flag
    private boolean voxelLod = false; // [Experimental] voxel level of detail flag
    private boolean isPhotogrammetry = false; // [Experimental] isPhotogrammetry mode flag

    /* 2D Data Column Options */
    private String heightColumn;
    private String altitudeColumn;
    private String headingColumn;
    private String diameterColumn;
    private String scaleColumn;
    private String densityColumn;

    private double absoluteAltitude;
    private double minimumHeight;
    private double skirtHeight;

    private List<AttributeFilter> attributeFilters = new ArrayList<>();

    public static GlobalOptions getInstance() {
        if (instance.javaVersionInfo == null) {
            initVersionInfo();
            instance.reporter = new Reporter("", instance.version);
        }
        return instance;
    }

    public static void init(CommandLine command) throws IOException, RuntimeException {

        if (command == null) {
            throw new IllegalArgumentException("Command line argument is null.");
        }
        if (command.getOptions() == null || command.getOptions().length == 0) {
            throw new IllegalArgumentException("Command line argument is empty.");
        }
        String inputPath = command.getOptionValue(ProcessOptions.INPUT.getLongName());
        String outputPath = command.getOptionValue(ProcessOptions.OUTPUT.getLongName());
        if (inputPath == null || outputPath == null) {
            throw new IllegalArgumentException("Please enter the value of the input and output arguments.");
        }
        File input = new File(command.getOptionValue(ProcessOptions.INPUT.getLongName()));
        File output = new File(command.getOptionValue(ProcessOptions.OUTPUT.getLongName()));
        if (command.hasOption(ProcessOptions.INPUT.getLongName())) {
            instance.setInputPath(command.getOptionValue(ProcessOptions.INPUT.getLongName()));
            OptionsCorrector.checkExistInputPath(input);
        } else {
            throw new IllegalArgumentException("Please enter the value of the input argument.");
        }

        if (command.hasOption(ProcessOptions.OUTPUT.getLongName())) {
            instance.setOutputPath(command.getOptionValue(ProcessOptions.OUTPUT.getLongName()));
            OptionsCorrector.checkExistOutput(output);
        } else {
            throw new IllegalArgumentException("Please enter the value of the output argument.");
        }

        boolean isRecursive;
        if (command.hasOption(ProcessOptions.RECURSIVE.getLongName())) {
            isRecursive = true;
        } else {
            isRecursive = OptionsCorrector.isRecursive(input);
        }
        instance.setRecursive(isRecursive);
        instance.setLogPath(command.hasOption(ProcessOptions.LOG.getLongName()) ? command.getOptionValue(ProcessOptions.LOG.getLongName()) : null);

        if (!command.hasOption(ProcessOptions.MERGE.getLongName())) {
            FormatType inputFormat;
            String inputType = command.hasOption(ProcessOptions.INPUT_TYPE.getLongName()) ? command.getOptionValue(ProcessOptions.INPUT_TYPE.getLongName()) : null;
            if (inputType == null || StringUtils.isEmpty(inputType)) {
                inputFormat = OptionsCorrector.findInputFormatType(new File(instance.getInputPath()), isRecursive);
            } else {
                inputFormat = FormatType.fromExtension(inputType);
            }
            inputFormat = inputFormat == null ? FormatType.fromExtension(DEFAULT_INPUT_FORMAT) : inputFormat;
            instance.setInputFormat(inputFormat);

            FormatType outputFormat;
            String outputType = command.hasOption(ProcessOptions.OUTPUT_TYPE.getLongName()) ? command.getOptionValue(ProcessOptions.OUTPUT_TYPE.getLongName()) : null;
            if (outputType == null) {
                outputFormat = OptionsCorrector.findOutputFormatType(instance.getInputFormat());
            } else {
                outputFormat = FormatType.fromExtension(outputType);
            }
            if (outputFormat == null) {
                throw new IllegalArgumentException("Invalid output format: " + outputType);
            } else {
                instance.setOutputFormat(outputFormat);
            }
        } else {
            // Merge mode
            instance.setInputFormat(FormatType.TILESET);
            instance.setOutputFormat(FormatType.TILESET);
            if (command.hasOption(ProcessOptions.INPUT_TYPE.getLongName())) {
                log.warn("[WARN] Input type option is ignored in merge mode.");
            }
            if (command.hasOption(ProcessOptions.OUTPUT_TYPE.getLongName())) {
                log.warn("[WARN] Output type option is ignored in merge mode.");
            }
        }

        if (command.hasOption(ProcessOptions.TERRAIN.getLongName())) {
            instance.setTerrainPath(command.getOptionValue(ProcessOptions.TERRAIN.getLongName()));
            OptionsCorrector.checkExistInputPath(new File(instance.getTerrainPath()));
        }

        if (command.hasOption(ProcessOptions.INSTANCE_FILE.getLongName())) {
            instance.setInstancePath(command.getOptionValue(ProcessOptions.INSTANCE_FILE.getLongName()));
            OptionsCorrector.checkExistInputPath(new File(instance.getInstancePath()));
        } else {
            String instancePath = instance.getInputPath() + File.separator + DEFAULT_INSTANCE_FILE;
            instance.setInstancePath(instancePath);
        }

        instance.setCartesian(command.hasOption(ProcessOptions.CARTESIAN.getLongName()));
        if (command.hasOption(ProcessOptions.PROJ4.getLongName())) {
            instance.setProj(command.hasOption(ProcessOptions.PROJ4.getLongName()) ? command.getOptionValue(ProcessOptions.PROJ4.getLongName()) : null);
            CoordinateReferenceSystem crs = null;
            if (instance.getProj() != null && !instance.getProj().isEmpty()) {
                crs = new CRSFactory().createFromParameters("CUSTOM_CRS_PROJ", instance.getProj());
            }
            instance.setCrs(crs);
        }

        Vector3d translation = new Vector3d(0, 0, 0);
        if (command.hasOption(ProcessOptions.X_OFFSET.getLongName())) {
            translation.x = Double.parseDouble(command.getOptionValue(ProcessOptions.X_OFFSET.getLongName()));
        }
        if (command.hasOption(ProcessOptions.Y_OFFSET.getLongName())) {
            translation.y = Double.parseDouble(command.getOptionValue(ProcessOptions.Y_OFFSET.getLongName()));
        }
        if (command.hasOption(ProcessOptions.Z_OFFSET.getLongName())) {
            translation.z = Double.parseDouble(command.getOptionValue(ProcessOptions.Z_OFFSET.getLongName()));
        }
        instance.setTranslateOffset(translation);

        CRSFactory factory = new CRSFactory();
        if (command.hasOption(ProcessOptions.CRS.getLongName()) || command.hasOption(ProcessOptions.PROJ4.getLongName())) {
            String crsString = command.getOptionValue(ProcessOptions.CRS.getLongName());
            String proj = command.getOptionValue(ProcessOptions.PROJ4.getLongName());
            CoordinateReferenceSystem source = null;

            if (proj != null && !proj.isEmpty()) {
                source = factory.createFromParameters("CUSTOM_CRS_PROJ", proj);
            } else if (crsString != null && !crsString.isEmpty()) {
                source = factory.createFromName("EPSG:" + crsString);
            } else {
                source = DEFAULT_CRS;
            }
            instance.setCrs(source);
        } else if (command.hasOption(ProcessOptions.LONGITUDE.getLongName()) || command.hasOption(ProcessOptions.LATITUDE.getLongName())) {
            if (!command.hasOption(ProcessOptions.LONGITUDE.getLongName()) || !command.hasOption(ProcessOptions.LATITUDE.getLongName())) {
                log.error("[ERROR] Please enter the value of the longitude and latitude arguments.");
                log.error("[ERROR] The lon lat option must be used together.");
                throw new IllegalArgumentException("Please enter the value of the longitude and latitude arguments.");
            }
            double longitude = Double.parseDouble(command.getOptionValue(ProcessOptions.LONGITUDE.getLongName()));
            double latitude = Double.parseDouble(command.getOptionValue(ProcessOptions.LATITUDE.getLongName()));
            String proj = "+proj=tmerc +x_0=0 +y_0=0 +ellps=WGS84 +datum=WGS84 +units=m +no_defs +lon_0=" + longitude + " +lat_0=" + latitude;
            instance.setProj(proj);
            CoordinateReferenceSystem source = factory.createFromParameters("CUSTOM_CRS_PROJ", proj);
            instance.setCrs(source);
            log.info("Custom CRS: {}", proj);
        } else {
            CoordinateReferenceSystem source = DEFAULT_CRS;
            // GeoJSON Default CRS
            if (instance.getInputFormat().equals(FormatType.GEOJSON)) {
                source = factory.createFromName("EPSG:4326");
            }
            instance.setCrs(source);
        }

        /* 3D Data Options */
        instance.setMinLod(command.hasOption(ProcessOptions.MIN_LOD.getLongName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MIN_LOD.getLongName())) : DEFAULT_MIN_LOD);
        instance.setMaxLod(command.hasOption(ProcessOptions.MAX_LOD.getLongName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_LOD.getLongName())) : DEFAULT_MAX_LOD);
        instance.setMinGeometricError(command.hasOption(ProcessOptions.MIN_GEOMETRIC_ERROR.getLongName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MIN_GEOMETRIC_ERROR.getLongName())) : DEFAULT_MIN_GEOMETRIC_ERROR);
        instance.setMaxGeometricError(command.hasOption(ProcessOptions.MAX_GEOMETRIC_ERROR.getLongName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_GEOMETRIC_ERROR.getLongName())) : DEFAULT_MAX_GEOMETRIC_ERROR);
        instance.setIgnoreTextures(command.hasOption(ProcessOptions.IGNORE_TEXTURES.getLongName()));
        instance.setMaxTriangles(DEFAULT_MAX_TRIANGLES);
        instance.setMaxInstance(DEFAULT_MAX_INSTANCE);
        instance.setMaxNodeDepth(DEFAULT_MAX_NODE_DEPTH);
        instance.setLargeMesh(command.hasOption(ProcessOptions.LARGE_MESH.getLongName()));
        instance.setVoxelLod(command.hasOption(ProcessOptions.VOXEL_LOD.getLongName()));
        instance.setPhotogrammetry(command.hasOption(ProcessOptions.PHOTOGRAMMETRY.getLongName()));
        instance.setLeaveTemp(command.hasOption(ProcessOptions.LEAVE_TEMP.getLongName()));
        instance.setUseQuantization(command.hasOption(ProcessOptions.MESH_QUANTIZATION.getLongName()) || DEFAULT_USE_QUANTIZATION);

        /* Point Cloud Options */
        instance.setMaximumPointPerTile(command.hasOption(ProcessOptions.MAX_POINTS.getLongName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_POINTS.getLongName())) : DEFAULT_POINT_PER_TILE);
        instance.setPointRatio(command.hasOption(ProcessOptions.POINT_RATIO.getLongName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.POINT_RATIO.getLongName())) : DEFAULT_POINT_RATIO);
        instance.setForce4ByteRGB(command.hasOption(ProcessOptions.POINT_FORCE_4BYTE_RGB.getLongName()));

        /* 2D Data Column Options */
        instance.setHeightColumn(command.hasOption(ProcessOptions.HEIGHT_COLUMN.getLongName()) ? command.getOptionValue(ProcessOptions.HEIGHT_COLUMN.getLongName()) : DEFAULT_HEIGHT_COLUMN);
        instance.setAltitudeColumn(command.hasOption(ProcessOptions.ALTITUDE_COLUMN.getLongName()) ? command.getOptionValue(ProcessOptions.ALTITUDE_COLUMN.getLongName()) : DEFAULT_ALTITUDE_COLUMN);
        instance.setHeadingColumn(command.hasOption(ProcessOptions.HEADING_COLUMN.getLongName()) ? command.getOptionValue(ProcessOptions.HEADING_COLUMN.getLongName()) : DEFAULT_HEADING_COLUMN);
        instance.setDiameterColumn(command.hasOption(ProcessOptions.DIAMETER_COLUMN.getLongName()) ? command.getOptionValue(ProcessOptions.DIAMETER_COLUMN.getLongName()) : DEFAULT_DIAMETER_COLUMN);
        instance.setScaleColumn(command.hasOption(ProcessOptions.SCALE_COLUMN.getLongName()) ? command.getOptionValue(ProcessOptions.SCALE_COLUMN.getLongName()) : DEFAULT_SCALE_COLUMN);
        instance.setDensityColumn(command.hasOption(ProcessOptions.DENSITY_COLUMN.getLongName()) ? command.getOptionValue(ProcessOptions.DENSITY_COLUMN.getLongName()) : DEFAULT_DENSITY_COLUMN);

        instance.setAbsoluteAltitude(command.hasOption(ProcessOptions.ABSOLUTE_ALTITUDE.getLongName()) ? Double.parseDouble(command.getOptionValue(ProcessOptions.ABSOLUTE_ALTITUDE.getLongName())) : DEFAULT_ABSOLUTE_ALTITUDE);
        instance.setMinimumHeight(command.hasOption(ProcessOptions.MINIMUM_HEIGHT.getLongName()) ? Double.parseDouble(command.getOptionValue(ProcessOptions.MINIMUM_HEIGHT.getLongName())) : DEFAULT_MINIMUM_HEIGHT);
        instance.setSkirtHeight(command.hasOption(ProcessOptions.SKIRT_HEIGHT.getLongName()) ? Double.parseDouble(command.getOptionValue(ProcessOptions.SKIRT_HEIGHT.getLongName())) : DEFAULT_SKIRT_HEIGHT);

        // Attribute Filter ex) "classification=window,door;type=building"
        if (command.hasOption(ProcessOptions.ATTRIBUTE_FILTER.getLongName())) {
            List<AttributeFilter> attributeFilters = instance.getAttributeFilters();
            String[] filters = command.getOptionValue(ProcessOptions.ATTRIBUTE_FILTER.getLongName()).split(";");
            for (String filter : filters) {
                String[] keyValue = filter.split("=");
                if (keyValue.length == 2) {
                    for (String value : keyValue[1].split(",")) {
                        attributeFilters.add(new AttributeFilter(keyValue[0], value));
                        log.info("Attribute Filter: {}={}", keyValue[0], value);
                    }
                }
            }
        }

        instance.setDebug(command.hasOption(ProcessOptions.DEBUG.getLongName()));

        boolean isSwapUpAxis = false;
        boolean isFlipUpAxis = false;
        boolean isRefineAdd = false;

        if (command.hasOption(ProcessOptions.FLIP_UP_AXIS.getLongName())) {
            log.warn("[WARN] FLIP_UP_AXIS is Deprecated option: {}", ProcessOptions.FLIP_UP_AXIS.getLongName());
            log.warn("[WARN] Please use ROTATE_X_AXIS option instead of FLIP_UP_AXIS option.");
            isFlipUpAxis = true;
        }
        if (command.hasOption(ProcessOptions.SWAP_UP_AXIS.getLongName())) {
            log.warn("[WARN] SWAP_UP_AXIS is Deprecated option: {}", ProcessOptions.SWAP_UP_AXIS.getLongName());
            log.warn("[WARN] Please use ROTATE_X_AXIS option instead of SWAP_UP_AXIS option.");
            isSwapUpAxis = true;
        }
        if (command.hasOption(ProcessOptions.REFINE_ADD.getLongName())) {
            isRefineAdd = true;
        }

        double rotateXAxis = command.hasOption(ProcessOptions.ROTATE_X_AXIS.getLongName()) ? Double.parseDouble(command.getOptionValue(ProcessOptions.ROTATE_X_AXIS.getLongName())) : 0;

        // force setting
        if (instance.getInputFormat().equals(FormatType.GEOJSON) || instance.getInputFormat().equals(FormatType.SHP) || instance.getInputFormat().equals(FormatType.CITYGML) || instance.getInputFormat().equals(FormatType.INDOORGML) || instance.getInputFormat().equals(FormatType.GEO_PACKAGE)) {
            isSwapUpAxis = false;
            isFlipUpAxis = false;
            if (instance.getOutputFormat().equals(FormatType.B3DM)) {
                rotateXAxis = -90;
                isRefineAdd = true;
            }
        }

        instance.setSwapUpAxis(isSwapUpAxis);
        instance.setFlipUpAxis(isFlipUpAxis);
        instance.setRotateX(rotateXAxis);
        instance.setRefineAdd(isRefineAdd);
        instance.setGlb(command.hasOption(ProcessOptions.DEBUG_GLB.getLongName()));
        instance.setFlipCoordinate(command.hasOption(ProcessOptions.FLIP_COORDINATE.getLongName()));

        if (command.hasOption(ProcessOptions.MULTI_THREAD_COUNT.getLongName())) {
            instance.setMultiThreadCount(Byte.parseByte(command.getOptionValue(ProcessOptions.MULTI_THREAD_COUNT.getLongName())));
        } else {
            int processorCount = Runtime.getRuntime().availableProcessors();
            int threadCount = processorCount > 1 ? processorCount / 2 : 1;
            instance.setMultiThreadCount((byte) threadCount);
        }

        instance.setAutoUpAxis(command.hasOption(ProcessOptions.AUTO_UP_AXIS.getLongName()));
        instance.printDebugOptions();

        TilerExtensionModule extensionModule = new TilerExtensionModule();
        extensionModule.executePhotogrammetry(null, null);
        if (!extensionModule.isSupported() && instance.isPhotogrammetry()) {
            log.error("[ERROR] *** Extension Module is not supported ***");
            throw new IllegalArgumentException("Extension Module is not supported.");
        } else {
            instance.setUseQuantization(false);
        }
    }

    private static void initVersionInfo() {
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String javaVersionInfo = "JAVA Version : " + javaVersion + " (" + javaVendor + ") ";
        String version = Mago3DTilerMain.class.getPackage().getImplementationVersion();
        String title = Mago3DTilerMain.class.getPackage().getImplementationTitle();
        String vendor = Mago3DTilerMain.class.getPackage().getImplementationVendor();
        version = version == null ? "dev" : version;
        title = title == null ? "mago-3d-tiler" : title;
        vendor = vendor == null ? "Gaia3D, Inc." : vendor;
        String programInfo = title + "(" + version + ") by " + vendor;
        instance.setProgramInfo(programInfo);
        instance.setJavaVersionInfo(javaVersionInfo);
    }

    public void printDebugOptions() {
        Mago3DTilerMain.drawLine();
        log.info("Input Path: {}", inputPath);
        log.info("Output Path: {}", outputPath);
        log.info("Input Format: {}", inputFormat);
        log.info("Output Format: {}", outputFormat);
        log.info("Terrain File Path: {}", terrainPath);
        log.info("Instance File Path: {}", instancePath);
        log.info("Log Path: {}", logPath);
        log.info("Recursive Path Search: {}", recursive);
        log.info("Coordinate Reference System: {}", crs);
        log.info("Proj4 Code: {}", proj);
        log.info("Debug Mode: {}", debug);
        Mago3DTilerMain.drawLine();
        if (!debug) {
            return;
        }
        log.info("Leave Temp Files: {}", isLeaveTemp);
        log.info("Minimum LOD: {}", minLod);
        log.info("Maximum LOD: {}", maxLod);
        log.info("Minimum GeometricError: {}", minGeometricError);
        log.info("Maximum GeometricError: {}", maxGeometricError);
        log.info("Maximum number of points per a tile: {}", maximumPointPerTile);
        log.info("Source Precision: {}", isSourcePrecision);
        log.info("Debug LOD: {}", debugLod);
        log.info("Debug GLB: {}", glb);
        log.info("isClassicTransformMatrix: {}", classicTransformMatrix);
        log.info("Multi-Thread Count: {}", multiThreadCount);
        Mago3DTilerMain.drawLine();
        log.info("Mesh Quantization: {}", useQuantization);
        log.info("Rotate X-Axis: {}", rotateX);
        log.info("Swap Up-Axis: {}", swapUpAxis);
        log.info("Flip Up-Axis: {}", flipUpAxis);
        log.info("RefineAdd: {}", refineAdd);
        log.info("Flip Coordinate: {}", flipCoordinate);
        log.info("Auto Up-Axis: {}", autoUpAxis);
        log.info("Ignore Textures: {}", ignoreTextures);
        log.info("Max Triangles: {}", maxTriangles);
        log.info("Max Instance Size: {}", maxInstance);
        log.info("Max Node Depth: {}", maxNodeDepth);
        log.info("LargeMesh: {}", largeMesh);
        log.info("Voxel LOD: {}", voxelLod);
        log.info("isPhotogrammetry: {}", isPhotogrammetry);
        log.info("PointCloud Ratio: {}", pointRatio);
        log.info("Point Cloud Horizontal Grid: {}", POINTSCLOUD_HORIZONTAL_GRID);
        log.info("Point Cloud Vertical Grid: {}", POINTSCLOUD_VERTICAL_GRID);
        log.info("Force 4Byte RGB: {}", force4ByteRGB);
        Mago3DTilerMain.drawLine();
        log.info("Height Column: {}", heightColumn);
        log.info("Altitude Column: {}", altitudeColumn);
        log.info("Heading Column: {}", headingColumn);
        log.info("Diameter Column: {}", diameterColumn);
        log.info("Absolute Altitude: {}", absoluteAltitude);
        log.info("Minimum Height: {}", minimumHeight);
        log.info("Skirt Height: {}", skirtHeight);
        Mago3DTilerMain.drawLine();
    }

}
