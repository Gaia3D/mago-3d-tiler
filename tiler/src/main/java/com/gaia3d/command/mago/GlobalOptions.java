package com.gaia3d.command.mago;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.process.ProcessOptions;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;

/**
 * Global options for Gaia3D Tiler.
 */
@Setter
@Getter
@NoArgsConstructor
@Slf4j
public class GlobalOptions {
    /* singleton */
    private static final GlobalOptions instance = new GlobalOptions();

    private static final String DEFAULT_INPUT_FORMAT = "kml";
    private static final String DEFAULT_INSTANCE_FILE = "instance.dae";
    private static final int DEFAULT_MIN_LOD = 0;
    private static final int DEFAULT_MAX_LOD = 3;
    private static final int DEFAULT_MIN_GEOMETRIC_ERROR = 16;
    private static final int DEFAULT_MAX_GEOMETRIC_ERROR = Integer.MAX_VALUE;

    private static final int DEFAULT_MAX_TRIANGLES = 65536 * 8;
    private static final int DEFAULT_MAX_NODE_DEPTH = 32;
    private static final int DEFAULT_MAX_INSTANCE = 512;

    private static final int DEFAULT_POINT_LIMIT = 65536;
    private static final int DEFAULT_POINT_SCALE = 2;
    private static final int DEFAULT_POINT_SKIP = 4;

    private static final String DEFAULT_CRS = "3857"; // 4326 -> 3857
    private static final String DEFAULT_NAME_COLUMN = "name";
    private static final String DEFAULT_HEIGHT_COLUMN = "height";
    private static final String DEFAULT_ALTITUDE_COLUMN = "altitude";
    private static final String DEFAULT_DIAMETER_COLUMN = "diameter";
    private static final double DEFAULT_ABSOLUTE_ALTITUDE = 0.0d;
    private static final double DEFAULT_MINIMUM_HEIGHT = 1.0d;
    private static final double DEFAULT_SKIRT_HEIGHT = 4.0d;
    private static final boolean DEFAULT_DEBUG_LOD = false;

    private String version; // version flag
    private String javaVersionInfo; // java version flag
    private String programInfo; // program info flag

    private long startTime = 0;
    private long endTime = 0;
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

    // point limit per tile
    private int pointLimit;

    // point scale
    private int pointScale;
    // skip points value
    private int pointSkip;

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
    // Debug Level of Detail
    private boolean debugLod = false;

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

    private boolean refineAdd = false; // 3dTiles refine option ADD fix flag
    private boolean flipCoordinate = false; // flip coordinate flag for 2D Data
    private boolean zeroOrigin = false; // data origin to zero point flag
    private boolean ignoreTextures = false; // ignore textures flag

    // [Experimental] 3D Data Options
    private boolean largeMesh = false; // [Experimental] large mesh splitting mode flag
    private boolean voxelLod = false; // [Experimental] voxel level of detail flag
    private boolean photorealistic = false; // [Experimental] photorealistic mode flag

    /* 2D Data Column Options */
    private String nameColumn;
    private String heightColumn;
    private String altitudeColumn;
    private String diameterColumn;
    private double absoluteAltitude;
    private double minimumHeight;
    private double skirtHeight;

    public static GlobalOptions getInstance() {
        if (instance.javaVersionInfo == null) {
            initVersionInfo();
        }
        return instance;
    }

    public static void init(CommandLine command) throws IOException {
        File input = new File(command.getOptionValue(ProcessOptions.INPUT.getArgName()));
        File output = new File(command.getOptionValue(ProcessOptions.OUTPUT.getArgName()));

        if (command.hasOption(ProcessOptions.INPUT.getArgName())) {
            instance.setInputPath(command.getOptionValue(ProcessOptions.INPUT.getArgName()));
            OptionsCorrector.checkExistInputPath(input);
        } else {
            throw new IllegalArgumentException("Please enter the value of the input argument.");
        }

        if (command.hasOption(ProcessOptions.OUTPUT.getArgName())) {
            instance.setOutputPath(command.getOptionValue(ProcessOptions.OUTPUT.getArgName()));
            OptionsCorrector.checkExistOutput(output);
        } else {
            throw new IllegalArgumentException("Please enter the value of the output argument.");
        }

        boolean isRecursive;
        if (command.hasOption(ProcessOptions.RECURSIVE.getArgName())) {
            isRecursive = true;
        } else {
            isRecursive = OptionsCorrector.isRecursive(input);
        }
        instance.setRecursive(isRecursive);

        FormatType inputFormat;
        String inputType = command.hasOption(ProcessOptions.INPUT_TYPE.getArgName()) ? command.getOptionValue(ProcessOptions.INPUT_TYPE.getArgName()) : null;
        if (inputType == null || StringUtils.isEmpty(inputType)) {
            inputFormat = OptionsCorrector.findInputFormatType(new File(instance.getInputPath()), isRecursive);
        } else {
            inputFormat = FormatType.fromExtension(inputType);
        }
        inputFormat = inputFormat == null ? FormatType.fromExtension(DEFAULT_INPUT_FORMAT) : inputFormat;
        instance.setInputFormat(inputFormat);

        FormatType outputFormat;
        String outputType = command.hasOption(ProcessOptions.OUTPUT_TYPE.getArgName()) ? command.getOptionValue(ProcessOptions.OUTPUT_TYPE.getArgName()) : null;
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

        instance.setLogPath(command.hasOption(ProcessOptions.LOG.getArgName()) ? command.getOptionValue(ProcessOptions.LOG.getArgName()) : null);

        if (command.hasOption(ProcessOptions.TERRAIN.getArgName())) {
            instance.setTerrainPath(command.getOptionValue(ProcessOptions.TERRAIN.getArgName()));
            OptionsCorrector.checkExistInputPath(new File(instance.getTerrainPath()));
        }

        if (command.hasOption(ProcessOptions.INSTANCE_FILE.getArgName())) {
            instance.setInstancePath(command.getOptionValue(ProcessOptions.INSTANCE_FILE.getArgName()));
            OptionsCorrector.checkExistInputPath(new File(instance.getInstancePath()));
        } else {
            String instancePath = instance.getInputPath() + File.separator + DEFAULT_INSTANCE_FILE;
            instance.setInstancePath(instancePath);
        }

        if (command.hasOption(ProcessOptions.PROJ4.getArgName())) {
            instance.setProj(command.hasOption(ProcessOptions.PROJ4.getArgName()) ? command.getOptionValue(ProcessOptions.PROJ4.getArgName()) : null);
            CoordinateReferenceSystem crs = null;
            if (instance.getProj() != null && !instance.getProj().isEmpty()) {
                crs = new CRSFactory().createFromParameters("CUSTOM_CRS_PROJ", instance.getProj());
            }
            instance.setCrs(crs);
        }

        CRSFactory factory = new CRSFactory();
        if (command.hasOption(ProcessOptions.CRS.getArgName()) || command.hasOption(ProcessOptions.PROJ4.getArgName())) {
            String crsString = command.getOptionValue(ProcessOptions.CRS.getArgName());
            String proj = command.getOptionValue(ProcessOptions.PROJ4.getArgName());
            CoordinateReferenceSystem source = null;

            if (proj != null && !proj.isEmpty()) {
                source = factory.createFromParameters("CUSTOM_CRS_PROJ", proj);
            } else if (crsString != null && !crsString.isEmpty()) {
                source = factory.createFromName("EPSG:" + crsString);
            } else {
                source = factory.createFromName("EPSG:" + DEFAULT_CRS);
            }
            instance.setCrs(source);
        } else {
            CoordinateReferenceSystem source = factory.createFromName("EPSG:" + DEFAULT_CRS);

            // GeoJSON Default CRS
            if (instance.getInputFormat().equals(FormatType.GEOJSON)) {
                source = factory.createFromName("EPSG:4326");
            }
            instance.setCrs(source);
        }

        /* 3D Data Options */
        instance.setMinLod(command.hasOption(ProcessOptions.MIN_LOD.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MIN_LOD.getArgName())) : DEFAULT_MIN_LOD);
        instance.setMaxLod(command.hasOption(ProcessOptions.MAX_LOD.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_LOD.getArgName())) : DEFAULT_MAX_LOD);
        instance.setMinGeometricError(command.hasOption(ProcessOptions.MIN_GEOMETRIC_ERROR.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MIN_GEOMETRIC_ERROR.getArgName())) : DEFAULT_MIN_GEOMETRIC_ERROR);
        instance.setMaxGeometricError(command.hasOption(ProcessOptions.MAX_GEOMETRIC_ERROR.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_GEOMETRIC_ERROR.getArgName())) : DEFAULT_MAX_GEOMETRIC_ERROR);
        instance.setIgnoreTextures(command.hasOption(ProcessOptions.IGNORE_TEXTURES.getArgName()));
        instance.setMaxTriangles(DEFAULT_MAX_TRIANGLES);
        instance.setMaxInstance(DEFAULT_MAX_INSTANCE);
        instance.setMaxNodeDepth(DEFAULT_MAX_NODE_DEPTH);
        instance.setLargeMesh(command.hasOption(ProcessOptions.LARGE_MESH.getArgName()));
        instance.setVoxelLod(command.hasOption(ProcessOptions.VOXEL_LOD.getArgName()));
        instance.setPhotorealistic(command.hasOption(ProcessOptions.PHOTOREALISTIC.getArgName()));

        /* Point Cloud Options */
        instance.setPointLimit(command.hasOption(ProcessOptions.MAX_POINTS.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_POINTS.getArgName())) : DEFAULT_POINT_LIMIT);
        instance.setPointScale(command.hasOption(ProcessOptions.POINT_SCALE.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.POINT_SCALE.getArgName())) : DEFAULT_POINT_SCALE);
        instance.setPointSkip(command.hasOption(ProcessOptions.POINT_SKIP.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.POINT_SKIP.getArgName())) : DEFAULT_POINT_SKIP);

        /* 2D Data Column Options */
        instance.setNameColumn(command.hasOption(ProcessOptions.NAME_COLUMN.getArgName()) ? command.getOptionValue(ProcessOptions.NAME_COLUMN.getArgName()) : DEFAULT_NAME_COLUMN);
        instance.setHeightColumn(command.hasOption(ProcessOptions.HEIGHT_COLUMN.getArgName()) ? command.getOptionValue(ProcessOptions.HEIGHT_COLUMN.getArgName()) : DEFAULT_HEIGHT_COLUMN);
        instance.setAltitudeColumn(command.hasOption(ProcessOptions.ALTITUDE_COLUMN.getArgName()) ? command.getOptionValue(ProcessOptions.ALTITUDE_COLUMN.getArgName()) : DEFAULT_ALTITUDE_COLUMN);
        instance.setDiameterColumn(command.hasOption(ProcessOptions.DIAMETER_COLUMN.getArgName()) ? command.getOptionValue(ProcessOptions.DIAMETER_COLUMN.getArgName()) : DEFAULT_DIAMETER_COLUMN);
        instance.setAbsoluteAltitude(command.hasOption(ProcessOptions.ABSOLUTE_ALTITUDE.getArgName()) ? Double.parseDouble(command.getOptionValue(ProcessOptions.ABSOLUTE_ALTITUDE.getArgName())) : DEFAULT_ABSOLUTE_ALTITUDE);
        instance.setMinimumHeight(command.hasOption(ProcessOptions.MINIMUM_HEIGHT.getArgName()) ? Double.parseDouble(command.getOptionValue(ProcessOptions.MINIMUM_HEIGHT.getArgName())) : DEFAULT_MINIMUM_HEIGHT);
        instance.setSkirtHeight(command.hasOption(ProcessOptions.SKIRT_HEIGHT.getArgName()) ? Double.parseDouble(command.getOptionValue(ProcessOptions.SKIRT_HEIGHT.getArgName())) : DEFAULT_SKIRT_HEIGHT);

        instance.setDebug(command.hasOption(ProcessOptions.DEBUG.getArgName()));
        instance.setDebugLod(DEFAULT_DEBUG_LOD);

        boolean isSwapUpAxis = true;
        boolean isFlipUpAxis = false;
        boolean isRefineAdd = false;

        if (command.hasOption(ProcessOptions.FLIP_UP_AXIS.getArgName())) {
            isFlipUpAxis = true;
        }
        if (command.hasOption(ProcessOptions.REFINE_ADD.getArgName())) {
            isRefineAdd = true;
        }

        // force setting
        if (instance.getInputFormat().equals(FormatType.GEOJSON) || instance.getInputFormat().equals(FormatType.SHP) || instance.getInputFormat().equals(FormatType.CITYGML) || instance.getInputFormat().equals(FormatType.INDOORGML)) {
            isSwapUpAxis = false;
            isFlipUpAxis = false;
            isRefineAdd = true;
        }

        if (command.hasOption(ProcessOptions.SWAP_UP_AXIS.getArgName())) {
            isSwapUpAxis = !isSwapUpAxis;
        }

        instance.setSwapUpAxis(isSwapUpAxis);
        instance.setFlipUpAxis(isFlipUpAxis);
        instance.setRefineAdd(isRefineAdd);
        instance.setGlb(command.hasOption(ProcessOptions.DEBUG_GLB.getArgName()));
        instance.setFlipCoordinate(command.hasOption(ProcessOptions.FLIP_COORDINATE.getArgName()));

        if (command.hasOption(ProcessOptions.MULTI_THREAD_COUNT.getArgName())) {
            instance.setMultiThreadCount(Byte.parseByte(command.getOptionValue(ProcessOptions.MULTI_THREAD_COUNT.getArgName())));
        } else {
            int processorCount = Runtime.getRuntime().availableProcessors();
            int threadCount = processorCount > 1 ? processorCount / 2 : 1;
            instance.setMultiThreadCount((byte) threadCount);
        }

        instance.setZeroOrigin(command.hasOption(ProcessOptions.ZERO_ORIGIN.getArgName()));
        instance.setAutoUpAxis(command.hasOption(ProcessOptions.AUTO_UP_AXIS.getArgName()));

        instance.printDebugOptions();
    }

    private static void initVersionInfo() {
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String javaVersionInfo = "JAVA Version : " + javaVersion + " (" + javaVendor + ") ";
        String version = Mago3DTilerMain.class.getPackage().getImplementationVersion();
        String title = Mago3DTilerMain.class.getPackage().getImplementationTitle();
        String vendor = Mago3DTilerMain.class.getPackage().getImplementationVendor();
        version = version == null ? "dev" : version;
        title = title == null ? "3d-tiler" : title;
        vendor = vendor == null ? "Gaia3D, Inc." : vendor;
        String programInfo = title + "(" + version + ") by " + vendor;

        instance.setStartTime(System.currentTimeMillis());
        instance.setProgramInfo(programInfo);
        instance.setJavaVersionInfo(javaVersionInfo);
    }

    public void printDebugOptions() {
        if (!debug) {
            return;
        }
        log.debug("========================================");
        log.debug("Input Path: {}", inputPath);
        log.debug("Output Path: {}", outputPath);
        log.debug("Input Format: {}", inputFormat);
        log.debug("Output Format: {}", outputFormat);
        log.debug("Terrain File Path: {}", terrainPath);
        log.debug("Instance File Path: {}", instancePath);
        log.debug("Log Path: {}", logPath);
        log.debug("Recursive Path Search: {}", recursive);

        log.debug("========================================");
        log.debug("Coordinate Reference System: {}", crs);
        log.debug("Proj4 Code: {}", proj);
        log.debug("Minimum LOD: {}", minLod);
        log.debug("Maximum LOD: {}", maxLod);
        log.debug("Minimum GeometricError: {}", minGeometricError);
        log.debug("Maximum GeometricError: {}", maxGeometricError);
        log.debug("PointCloud Point Limit: {}", pointLimit);
        log.debug("PointCloud Scale: {}", pointScale);
        log.debug("PointCloud Skip Interval: {}", pointSkip);
        log.debug("Debug Mode: {}", debug);
        log.debug("Debug LOD: {}", debugLod);
        log.debug("Debug GLB: {}", glb);
        log.debug("classicTransformMatrix: {}", classicTransformMatrix);
        log.debug("Multi-Thread Count: {}", multiThreadCount);

        // 3D Data Options
        log.debug("========================================");
        log.debug("Swap Up-Axis: {}", swapUpAxis);
        log.debug("Flip Up-Axis: {}", flipUpAxis);
        log.debug("RefineAdd: {}", refineAdd);
        log.debug("Flip Coordinate: {}", flipCoordinate);
        log.debug("Zero Origin: {}", zeroOrigin);
        log.debug("Auto Up-Axis: {}", autoUpAxis);
        log.debug("Ignore Textures: {}", ignoreTextures);
        log.debug("Max Triangles: {}", maxTriangles);
        log.debug("Max Instance Size: {}", maxInstance);
        log.debug("Max Node Depth: {}", maxNodeDepth);
        log.debug("LargeMesh: {}", largeMesh);
        log.debug("Voxel LOD: {}", voxelLod);
        log.debug("Photorealistic: {}", photorealistic);

        // 2D Data Column Options
        log.debug("========================================");
        log.debug("Name Column: {}", nameColumn);
        log.debug("Height Column: {}", heightColumn);
        log.debug("Altitude Column: {}", altitudeColumn);
        log.debug("Absolute Altitude: {}", absoluteAltitude);
        log.debug("Minimum Height: {}", minimumHeight);
        log.debug("Skirt Height: {}", skirtHeight);
        log.debug("========================================");
    }
}
