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
import java.util.List;

/**
 * Global options for Gaia3D Tiler.
 */
@Setter
@Getter
@NoArgsConstructor
@Slf4j
public class GlobalOptions {
    private static GlobalOptions instance = new GlobalOptions();
    private Reporter reporter;

    private boolean isParametric = false;

    private String tilesVersion;
    private String version;
    private String javaVersionInfo;
    private String programInfo;

    private long fileCount = 0;
    private long tileCount = 0;
    private long tilesetSize = 0;

    /* Required Options */
    private String inputPath;
    private String outputPath;

    /* Optional Options */
    private String logPath;
    private String terrainPath;
    private String instancePath;

    private FormatType inputFormat;
    private FormatType outputFormat;

    private CoordinateReferenceSystem sourceCrs;
    private CoordinateReferenceSystem targetCrs;
    private String proj;
    private Vector3d translateOffset;

    private boolean isSourcePrecision = false;
    private int maximumPointPerTile = 0; // Maximum number of points per a tile
    private int pointRatio = 0; // Percentage of points from original data
    private boolean force4ByteRGB = false; // Force 4Byte RGB for pointscloud tile

    private boolean useQuantization; // Use quantization via KHR_mesh_quantization

    /* Tiling Options */
    // Level of Detail
    private int minLod;
    private int maxLod;
    // Geometric Error
    private int minGeometricError;
    private int maxGeometricError;
    // Tile Options
    private int maxTriangles;
    private int maxInstance;
    private int maxNodeDepth;

    // Debug Mode
    private boolean debug = false;
    private boolean debugLod = false;
    private boolean isLeaveTemp = false;

    private boolean glb = false;
    private boolean classicTransformMatrix = false;
    private byte multiThreadCount = 1;

    /* 3D Data Options */
    private boolean recursive = false;
    private double rotateX = 0; // degrees

    private boolean refineAdd = false; // 3dTiles refine option ADD fix flag
    private boolean flipCoordinate = false; // flip coordinate flag for 2D Data
    private boolean ignoreTextures = false; // ignore textures flag

    // [Experimental] 3D Data Options
    private boolean isPhotogrammetry = false; // [Experimental] isPhotogrammetry mode flag
    private boolean isSplitByNode = false; // [Experimental] split by node flag

    /* 2D Data Column Options */
    private String heightColumn = null;
    private String altitudeColumn = null;
    private String headingColumn = null;
    private String diameterColumn = null;
    private String scaleColumn = null;
    private String densityColumn = null;

    private double absoluteAltitude = 0.0d;
    private double minimumHeight = 0.0d;
    private double skirtHeight = 0.0d;

    private List<AttributeFilter> attributeFilters = new ArrayList<>();

    public static void recreateInstance() {
        log.info("[INFO] Recreating GlobalOptions instance.");
        GlobalOptions.instance = new GlobalOptions();
    }

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
        String inputPath = command.getOptionValue(ProcessOptions.INPUT_PATH.getLongName());
        String outputPath = command.getOptionValue(ProcessOptions.OUTPUT_PATH.getLongName());
        if (inputPath == null || outputPath == null) {
            throw new IllegalArgumentException("Please enter the value of the input and output arguments.");
        }
        File input = new File(command.getOptionValue(ProcessOptions.INPUT_PATH.getLongName()));
        File output = new File(command.getOptionValue(ProcessOptions.OUTPUT_PATH.getLongName()));
        if (command.hasOption(ProcessOptions.INPUT_PATH.getLongName())) {
            instance.setInputPath(command.getOptionValue(ProcessOptions.INPUT_PATH.getLongName()));
            OptionsCorrector.checkExistInputPath(input);
        } else {
            throw new IllegalArgumentException("Please enter the value of the input argument.");
        }

        if (command.hasOption(ProcessOptions.OUTPUT_PATH.getLongName())) {
            instance.setOutputPath(command.getOptionValue(ProcessOptions.OUTPUT_PATH.getLongName()));
            OptionsCorrector.checkExistOutput(output);
        } else {
            throw new IllegalArgumentException("Please enter the value of the output argument.");
        }

        if (command.hasOption(ProcessOptions.TILES_VERSION.getLongName())) {
            String tilesVersion = command.getOptionValue(ProcessOptions.TILES_VERSION.getLongName());
            instance.setTilesVersion(tilesVersion);
        } else {
            instance.setTilesVersion(GlobalConstants.DEFAULT_TILES_VERSION);
        }

        boolean isRecursive;
        if (command.hasOption(ProcessOptions.RECURSIVE.getLongName())) {
            isRecursive = true;
        } else {
            isRecursive = OptionsCorrector.isRecursive(input);
        }
        instance.setRecursive(isRecursive);
        instance.setLogPath(command.hasOption(ProcessOptions.LOG_PATH.getLongName()) ? command.getOptionValue(ProcessOptions.LOG_PATH.getLongName()) : null);

        if (!command.hasOption(ProcessOptions.MERGE.getLongName())) {
            FormatType inputFormat;
            String inputType = command.hasOption(ProcessOptions.INPUT_TYPE.getLongName()) ? command.getOptionValue(ProcessOptions.INPUT_TYPE.getLongName()) : null;
            if (inputType == null || StringUtils.isEmpty(inputType)) {
                inputFormat = OptionsCorrector.findInputFormatType(new File(instance.getInputPath()), isRecursive);
            } else {
                inputFormat = FormatType.fromExtension(inputType);
            }
            inputFormat = inputFormat == null ? FormatType.fromExtension(GlobalConstants.DEFAULT_INPUT_FORMAT) : inputFormat;
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

        if (command.hasOption(ProcessOptions.TERRAIN_PATH.getLongName())) {
            instance.setTerrainPath(command.getOptionValue(ProcessOptions.TERRAIN_PATH.getLongName()));
            OptionsCorrector.checkExistInputPath(new File(instance.getTerrainPath()));
        } else {
            instance.setTerrainPath(null);
            log.info("[Info] Terrain path is not set. No terrain data will be used.");
        }

        if (command.hasOption(ProcessOptions.INSTANCE_PATH.getLongName())) {
            instance.setInstancePath(command.getOptionValue(ProcessOptions.INSTANCE_PATH.getLongName()));
            OptionsCorrector.checkExistInputPath(new File(instance.getInstancePath()));
        } else {
            String instancePath = instance.getInputPath() + File.separator + GlobalConstants.DEFAULT_INSTANCE_FILE;
            instance.setInstancePath(instancePath);
        }

        if (command.hasOption(ProcessOptions.PROJ4.getLongName())) {
            instance.setProj(command.hasOption(ProcessOptions.PROJ4.getLongName()) ? command.getOptionValue(ProcessOptions.PROJ4.getLongName()) : null);
            CoordinateReferenceSystem sourceCrs = null;
            if (instance.getProj() != null && !instance.getProj().isEmpty()) {
                sourceCrs = new CRSFactory().createFromParameters("CUSTOM_CRS_PROJ", instance.getProj());
            }
            instance.setSourceCrs(sourceCrs);
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
            CoordinateReferenceSystem sourceCrs = null;

            if (proj != null && !proj.isEmpty()) {
                sourceCrs = factory.createFromParameters("CUSTOM_CRS_PROJ", proj);
            } else if (crsString != null && !crsString.isEmpty()) {
                sourceCrs = factory.createFromName("EPSG:" + crsString);
            } else {
                sourceCrs = GlobalConstants.DEFAULT_SOURCE_CRS;
            }
            instance.setSourceCrs(sourceCrs);
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
            CoordinateReferenceSystem sourceCrs = factory.createFromParameters("CUSTOM_CRS_PROJ", proj);
            instance.setSourceCrs(sourceCrs);
            log.info("Custom CRS: {}", proj);
        } else {
            CoordinateReferenceSystem sourceCrs = GlobalConstants.DEFAULT_SOURCE_CRS;
            // GeoJSON GlobalDefaultConst.DEFAULT CRS
            if (instance.getInputFormat().equals(FormatType.GEOJSON)) {
                sourceCrs = factory.createFromName("EPSG:4326");
            }
            instance.setSourceCrs(sourceCrs);
        }

        /* 3D Data Options */
        instance.setMinLod(command.hasOption(ProcessOptions.MIN_LOD.getLongName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MIN_LOD.getLongName())) : GlobalConstants.DEFAULT_MIN_LOD);
        instance.setMaxLod(command.hasOption(ProcessOptions.MAX_LOD.getLongName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_LOD.getLongName())) : GlobalConstants.DEFAULT_MAX_LOD);
        instance.setMinGeometricError(command.hasOption(ProcessOptions.MIN_GEOMETRIC_ERROR.getLongName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MIN_GEOMETRIC_ERROR.getLongName())) : GlobalConstants.DEFAULT_MIN_GEOMETRIC_ERROR);
        instance.setMaxGeometricError(command.hasOption(ProcessOptions.MAX_GEOMETRIC_ERROR.getLongName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_GEOMETRIC_ERROR.getLongName())) : GlobalConstants.DEFAULT_MAX_GEOMETRIC_ERROR);
        instance.setIgnoreTextures(command.hasOption(ProcessOptions.IGNORE_TEXTURES.getLongName()));
        instance.setMaxTriangles(GlobalConstants.DEFAULT_MAX_TRIANGLES);
        instance.setMaxInstance(GlobalConstants.DEFAULT_MAX_INSTANCE);
        instance.setMaxNodeDepth(GlobalConstants.DEFAULT_MAX_NODE_DEPTH);
        //instance.setLargeMesh(command.hasOption(ProcessOptions.LARGE_MESH.getLongName()));
        //instance.setVoxelLod(command.hasOption(ProcessOptions.VOXEL_LOD.getLongName()));
        instance.setPhotogrammetry(command.hasOption(ProcessOptions.PHOTOGRAMMETRY.getLongName()));
        instance.setLeaveTemp(command.hasOption(ProcessOptions.LEAVE_TEMP.getLongName()));
        instance.setUseQuantization(command.hasOption(ProcessOptions.MESH_QUANTIZATION.getLongName()) || GlobalConstants.DEFAULT_USE_QUANTIZATION);

        /* Point Cloud Options */
        instance.setMaximumPointPerTile(command.hasOption(ProcessOptions.MAX_POINTS.getLongName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_POINTS.getLongName())) : GlobalConstants.DEFAULT_POINT_PER_TILE);
        instance.setPointRatio(command.hasOption(ProcessOptions.POINT_RATIO.getLongName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.POINT_RATIO.getLongName())) : GlobalConstants.DEFAULT_POINT_RATIO);
        instance.setForce4ByteRGB(command.hasOption(ProcessOptions.POINT_FORCE_4BYTE_RGB.getLongName()));

        /* 2D Data Column Options */
        instance.setHeightColumn(command.hasOption(ProcessOptions.HEIGHT_COLUMN.getLongName()) ? command.getOptionValue(ProcessOptions.HEIGHT_COLUMN.getLongName()) : GlobalConstants.DEFAULT_HEIGHT_COLUMN);
        instance.setAltitudeColumn(command.hasOption(ProcessOptions.ALTITUDE_COLUMN.getLongName()) ? command.getOptionValue(ProcessOptions.ALTITUDE_COLUMN.getLongName()) : GlobalConstants.DEFAULT_ALTITUDE_COLUMN);
        instance.setHeadingColumn(command.hasOption(ProcessOptions.HEADING_COLUMN.getLongName()) ? command.getOptionValue(ProcessOptions.HEADING_COLUMN.getLongName()) : GlobalConstants.DEFAULT_HEADING_COLUMN);
        instance.setDiameterColumn(command.hasOption(ProcessOptions.DIAMETER_COLUMN.getLongName()) ? command.getOptionValue(ProcessOptions.DIAMETER_COLUMN.getLongName()) : GlobalConstants.DEFAULT_DIAMETER_COLUMN);
        instance.setScaleColumn(command.hasOption(ProcessOptions.SCALE_COLUMN.getLongName()) ? command.getOptionValue(ProcessOptions.SCALE_COLUMN.getLongName()) : GlobalConstants.DEFAULT_SCALE_COLUMN);
        instance.setDensityColumn(command.hasOption(ProcessOptions.DENSITY_COLUMN.getLongName()) ? command.getOptionValue(ProcessOptions.DENSITY_COLUMN.getLongName()) : GlobalConstants.DEFAULT_DENSITY_COLUMN);

        instance.setAbsoluteAltitude(command.hasOption(ProcessOptions.ABSOLUTE_ALTITUDE.getLongName()) ? Double.parseDouble(command.getOptionValue(ProcessOptions.ABSOLUTE_ALTITUDE.getLongName())) : GlobalConstants.DEFAULT_ABSOLUTE_ALTITUDE);
        instance.setMinimumHeight(command.hasOption(ProcessOptions.MINIMUM_HEIGHT.getLongName()) ? Double.parseDouble(command.getOptionValue(ProcessOptions.MINIMUM_HEIGHT.getLongName())) : GlobalConstants.DEFAULT_MINIMUM_HEIGHT);
        instance.setSkirtHeight(command.hasOption(ProcessOptions.SKIRT_HEIGHT.getLongName()) ? Double.parseDouble(command.getOptionValue(ProcessOptions.SKIRT_HEIGHT.getLongName())) : GlobalConstants.DEFAULT_SKIRT_HEIGHT);

        instance.setSplitByNode(command.hasOption(ProcessOptions.SPLIT_BY_NODE.getLongName()));

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
        boolean isRefineAdd = false;
        if (command.hasOption(ProcessOptions.REFINE_ADD.getLongName())) {
            isRefineAdd = true;
        }

        double rotateXAxis = command.hasOption(ProcessOptions.ROTATE_X_AXIS.getLongName()) ? Double.parseDouble(command.getOptionValue(ProcessOptions.ROTATE_X_AXIS.getLongName())) : 0;

        // force setting
        FormatType inputFormat = instance.getInputFormat();
        FormatType outputFormat = instance.getOutputFormat();

        boolean isParametric = inputFormat.equals(FormatType.GEOJSON) || inputFormat.equals(FormatType.SHP) || inputFormat.equals(FormatType.CITYGML) || inputFormat.equals(FormatType.INDOORGML) || inputFormat.equals(FormatType.GEO_PACKAGE);
        instance.setParametric(isParametric);

        if (isParametric) {
            if (outputFormat.equals(FormatType.B3DM)) {
                isRefineAdd = true;
            }
        }
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
        log.info("3DTiles Version: {}", tilesVersion);
        log.info("Input Path: {}", inputPath);
        log.info("Output Path: {}", outputPath);
        log.info("Input Format: {}", inputFormat);
        log.info("Output Format: {}", outputFormat);
        log.info("Terrain File Path: {}", terrainPath);
        log.info("Instance File Path: {}", instancePath);
        log.info("Log File Path: {}", logPath);
        log.info("Recursive Path Search: {}", recursive);
        log.info("Source Coordinate Reference System: {}", sourceCrs);
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
        log.info("RefineAdd: {}", refineAdd);
        log.info("Flip Coordinate: {}", flipCoordinate);
        log.info("Ignore Textures: {}", ignoreTextures);
        log.info("Max Triangles: {}", maxTriangles);
        log.info("Max Instance Size: {}", maxInstance);
        log.info("Max Node Depth: {}", maxNodeDepth);
        log.info("isPhotogrammetry: {}", isPhotogrammetry);
        Mago3DTilerMain.drawLine();
        log.info("PointCloud Ratio: {}", pointRatio);
        log.info("Point Cloud Horizontal Grid: {}", GlobalConstants.POINTSCLOUD_HORIZONTAL_GRID);
        log.info("Point Cloud Vertical Grid: {}", GlobalConstants.POINTSCLOUD_VERTICAL_GRID);
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
