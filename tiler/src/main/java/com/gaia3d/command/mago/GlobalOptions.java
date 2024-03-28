package com.gaia3d.command.mago;

import com.gaia3d.basic.exchangable.GaiaTextureArchive;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.process.ProcessOptions;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileExistsException;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import java.io.File;
import java.io.IOException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;

/**
 * Global options for Gaia3D Tiler.
 */
@Setter
@Getter
@NoArgsConstructor
@Slf4j
public class GlobalOptions {
    /* singleton */
    private static final GlobalOptions instance = new GlobalOptions(); // volatile?

    private static final String DEFAULT_INPUT_FORMAT = "kml";
    private static final String DEFAULT_INSTANCE_FILE = "instance.dae";
    private static final int DEFAULT_MIN_LOD = 0;
    private static final int DEFAULT_MAX_LOD = 3;
    private static final int DEFAULT_POINT_LIMIT = 65536;
    private static final int DEFAULT_POINT_SCALE = 2;
    private static final int DEFAULT_POINT_SKIP = 4;

    private static final byte DEFAULT_MULTI_THREAD_COUNT = 4;
    private static final String DEFAULT_CRS = "4326";
    private static final String DEFAULT_NAME_COLUMN = "name";
    private static final String DEFAULT_HEIGHT_COLUMN = "height";
    private static final String DEFAULT_ALTITUDE_COLUMN = "altitude";
    private static final double DEFAULT_ABSOLUTE_ALTITUDE = 0.0d;
    private static final double DEFAULT_MINIMUM_HEIGHT = 1.0d;
    private static final double DEFAULT_SKIRT_HEIGHT = 4.0d;
    private static final boolean DEFAULT_DEBUG_LOD = false;


    private String version; // version flag
    private String javaVersionInfo; // java version flag
    private String programInfo; // program info flag

    private GaiaTextureArchive textureArchive = new GaiaTextureArchive();

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

    private CoordinateReferenceSystem crs;
    private String proj; // default projection

    private int pointLimit; // point limit per tile
    private int pointScale; // point scale
    private int pointSkip; // skip points value

    private int nodeLimit; // node limit per tile
    private int minLod; // minimum level of detail
    private int maxLod; // maximum level of detail

    private boolean debug = false; // debug mode flag
    private boolean debugLod = false; // debug lod flag

    private boolean gltf = false; // gltf flag
    private boolean glb = false; // glb flag
    private boolean classicTransformMatrix = false; // classic transform matrix flag

    private byte multiThreadCount; // multi thread count

    /* 3D Data Options */
    private boolean recursive = false; // recursive flag
    private boolean yUpAxis = false; // y up axis flag
    private boolean refineAdd = false; // 3dTiles refine option ADD fix flag
    private boolean flipCoordinate = false; // flip coordinate flag for 2D Data
    private boolean zeroOrigin = false; // data origin to zero point flag
    private boolean autoUpAxis = false; // automatically assign 3D matrix axes flag
    private boolean ignoreTextures = false; // ignore textures flag

    /* 2D Data Column Options */
    private String nameColumn;
    private String heightColumn;
    private String altitudeColumn;
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
        if (command.hasOption(ProcessOptions.INPUT.getArgName())) {
            instance.setInputPath(command.getOptionValue(ProcessOptions.INPUT.getArgName()));
            validateInputPath(new File(instance.getInputPath()).toPath());
        } else {
            throw new IllegalArgumentException("Please enter the value of the input argument.");
        }

        if (command.hasOption(ProcessOptions.OUTPUT.getArgName())) {
            instance.setOutputPath(command.getOptionValue(ProcessOptions.OUTPUT.getArgName()));
            validateOutputPath(new File(instance.getOutputPath()).toPath());
        } else {
            throw new IllegalArgumentException("Please enter the value of the output argument.");
        }

        String inputType = command.hasOption(ProcessOptions.INPUT_TYPE.getArgName()) ? command.getOptionValue(ProcessOptions.INPUT_TYPE.getArgName()) : DEFAULT_INPUT_FORMAT;
        FormatType formatType = FormatType.fromExtension(inputType);
        if (formatType == null) {
            throw new IllegalArgumentException("Invalid input format: " + inputType);
        } else {
            instance.setInputFormat(formatType);
        }

        FormatType outputFormat = null;
        String outputType = command.hasOption(ProcessOptions.OUTPUT_TYPE.getArgName()) ? command.getOptionValue(ProcessOptions.OUTPUT_TYPE.getArgName()) : null;
        if (outputType == null) {
            outputFormat = inferOutputFormat(instance.getInputFormat());
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
            validateInputPath(new File(instance.getTerrainPath()).toPath());
        }

        if (command.hasOption(ProcessOptions.INSTANCE_FILE.getArgName())) {
            instance.setInstancePath(command.getOptionValue(ProcessOptions.INSTANCE_FILE.getArgName()));
            validateInputPath(new File(instance.getInstancePath()).toPath());
        } else {
            String instancePath = instance.getInputPath() + File.separator + DEFAULT_INSTANCE_FILE;
            instance.setInstancePath(instancePath);
            //validateInputPath(new File(instancePath).toPath());
        }

        if (command.hasOption(ProcessOptions.PROJ4.getArgName())) {
            instance.setProj(command.hasOption(ProcessOptions.PROJ4.getArgName()) ? command.getOptionValue(ProcessOptions.PROJ4.getArgName()) : null);
            CoordinateReferenceSystem crs = null;
            if (instance.getProj() != null && !instance.getProj().isEmpty()) {
                crs = new CRSFactory().createFromParameters("CUSTOM_CRS_PROJ", instance.getProj());
            }
            instance.setCrs(crs);
        }
        if (command.hasOption(ProcessOptions.CRS.getArgName()) || command.hasOption(ProcessOptions.PROJ4.getArgName())) {
            String crsString = command.getOptionValue(ProcessOptions.CRS.getArgName());
            String proj = command.getOptionValue(ProcessOptions.PROJ4.getArgName());
            CRSFactory factory = new CRSFactory();
            CoordinateReferenceSystem source = null;

            // proj code is first priority
            if (proj != null && !proj.isEmpty()) {
                source = factory.createFromParameters("CUSTOM_CRS_PROJ", proj);
            } else if (crsString != null && !crsString.isEmpty()) {
                source = factory.createFromName("EPSG:" + crsString);
            }
            instance.setCrs(source);
        }

        /* 3D Data Options */
        instance.setNodeLimit(command.hasOption(ProcessOptions.MAX_COUNT.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_COUNT.getArgName())) : -1);
        instance.setMinLod(command.hasOption(ProcessOptions.MIN_LOD.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MIN_LOD.getArgName())) : DEFAULT_MIN_LOD);
        instance.setMaxLod(command.hasOption(ProcessOptions.MAX_LOD.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_LOD.getArgName())) : DEFAULT_MAX_LOD);
        instance.setIgnoreTextures(command.hasOption(ProcessOptions.IGNORE_TEXTURES.getArgName()));

        /* Point Cloud Options */
        instance.setPointLimit(command.hasOption(ProcessOptions.MAX_POINTS.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.MAX_POINTS.getArgName())) : DEFAULT_POINT_LIMIT);
        instance.setPointScale(command.hasOption(ProcessOptions.POINT_SCALE.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.POINT_SCALE.getArgName())) : DEFAULT_POINT_SCALE);
        instance.setPointSkip(command.hasOption(ProcessOptions.POINT_SKIP.getArgName()) ? Integer.parseInt(command.getOptionValue(ProcessOptions.POINT_SKIP.getArgName())) : DEFAULT_POINT_SKIP);

        /* 2D Data Column Options */
        instance.setNameColumn(command.hasOption(ProcessOptions.NAME_COLUMN.getArgName()) ? command.getOptionValue(ProcessOptions.NAME_COLUMN.getArgName()) : DEFAULT_NAME_COLUMN);
        instance.setHeightColumn(command.hasOption(ProcessOptions.HEIGHT_COLUMN.getArgName()) ? command.getOptionValue(ProcessOptions.HEIGHT_COLUMN.getArgName()) : DEFAULT_HEIGHT_COLUMN);
        instance.setAltitudeColumn(command.hasOption(ProcessOptions.ALTITUDE_COLUMN.getArgName()) ? command.getOptionValue(ProcessOptions.ALTITUDE_COLUMN.getArgName()) : DEFAULT_ALTITUDE_COLUMN);
        instance.setAbsoluteAltitude(command.hasOption(ProcessOptions.ABSOLUTE_ALTITUDE.getArgName()) ? Double.parseDouble(command.getOptionValue(ProcessOptions.ABSOLUTE_ALTITUDE.getArgName())) : DEFAULT_ABSOLUTE_ALTITUDE);
        instance.setMinimumHeight(command.hasOption(ProcessOptions.MINIMUM_HEIGHT.getArgName()) ? Double.parseDouble(command.getOptionValue(ProcessOptions.MINIMUM_HEIGHT.getArgName())) : DEFAULT_MINIMUM_HEIGHT);
        instance.setSkirtHeight(command.hasOption(ProcessOptions.SKIRT_HEIGHT.getArgName()) ? Double.parseDouble(command.getOptionValue(ProcessOptions.SKIRT_HEIGHT.getArgName())) : DEFAULT_SKIRT_HEIGHT);

        instance.setDebug(command.hasOption(ProcessOptions.DEBUG.getArgName()));
        instance.setDebugLod(DEFAULT_DEBUG_LOD);

        instance.setYUpAxis(command.hasOption(ProcessOptions.Y_UP_AXIS.getArgName()));
        instance.setRecursive(command.hasOption(ProcessOptions.RECURSIVE.getArgName()));

        if (command.hasOption(ProcessOptions.REFINE_ADD.getArgName())) {
            instance.setRefineAdd(true);
        } else {
            if (instance.getInputFormat().equals(FormatType.GEOJSON) || instance.getInputFormat().equals(FormatType.SHP) || instance.getInputFormat().equals(FormatType.CITYGML) || instance.getInputFormat().equals(FormatType.INDOORGML)) {
                instance.setRefineAdd(true);
            }
        }
        instance.setGlb(command.hasOption(ProcessOptions.DEBUG_GLB.getArgName()));
        instance.setFlipCoordinate(command.hasOption(ProcessOptions.FLIP_COORDINATE.getArgName()));
        instance.setMultiThreadCount(command.hasOption(ProcessOptions.MULTI_THREAD_COUNT.getArgName()) ? Byte.parseByte(command.getOptionValue(ProcessOptions.MULTI_THREAD_COUNT.getArgName())) : DEFAULT_MULTI_THREAD_COUNT);

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
        version = version == null ? "dev-version" : version;
        title = title == null ? "3d-tiler" : title;
        vendor = vendor == null ? "Gaia3D, Inc." : vendor;
        String programInfo = title + "(" + version + ") by " + vendor;

        instance.setStartTime(System.currentTimeMillis());
        instance.setProgramInfo(programInfo);
        instance.setJavaVersionInfo(javaVersionInfo);
    }

    protected static void validateInputPath(Path path) throws IOException {
        File output = path.toFile();
        if (!output.exists()) {
            throw new FileExistsException(String.format("%s Path is not exist.", path));
        } else if (!output.canWrite()) {
            throw new IOException(String.format("%s path is not writable.", path));
        }
    }

    protected static void validateOutputPath(Path path) throws IOException {
        File output = path.toFile();
        if (!output.exists()) {
            boolean isSuccess = output.mkdirs();
            if (!isSuccess) {
                throw new FileExistsException(String.format("%s Path is not exist.", path));
            } else {
                log.info("Created new output directory: {}", path);
            }
        } else if (!output.isDirectory()) {
            throw new NotDirectoryException(String.format("%s Path is not directory.", path));
        } else if (!output.canWrite()) {
            throw new IOException(String.format("%s path is not writable.", path));
        }
    }

    private static FormatType inferOutputFormat(FormatType inputFormat) {
        if (FormatType.LAS == inputFormat || FormatType.LAZ == inputFormat) {
            return FormatType.PNTS;
        } else {
            return FormatType.B3DM;
        }
    }

    public void printDebugOptions() {
        if (!debug) {
            return;
        }

        log.debug("========================================");
        log.debug("inputPath: {}", inputPath);
        log.debug("outputPath: {}", outputPath);
        log.debug("inputFormat: {}", inputFormat);
        log.debug("outputFormat: {}", outputFormat);
        log.debug("terrainPath: {}", terrainPath);
        log.debug("instancePath: {}", instancePath);
        log.debug("logPath: {}", logPath);
        log.debug("crs: {}", crs);
        log.debug("proj: {}", proj);
        log.debug("tileCount: {}", tileCount);
        log.debug("fileCount: {}", fileCount);
        log.debug("pointLimit: {}", pointLimit);
        log.debug("pointScale: {}", pointScale);
        log.debug("pointSkip: {}", pointSkip);
        log.debug("nodeLimit: {}", nodeLimit);
        log.debug("minLod: {}", minLod);
        log.debug("maxLod: {}", maxLod);
        log.debug("debug: {}", debug);
        log.debug("debugLod: {}", debugLod);
        log.debug("glb: {}", glb);
        log.debug("classicTransformMatrix: {}", classicTransformMatrix);
        log.debug("multiThreadCount: {}", multiThreadCount);
        log.debug("recursive: {}", recursive);
        log.debug("yUpAxis: {}", yUpAxis);
        log.debug("refineAdd: {}", refineAdd);
        log.debug("flipCoordinate: {}", flipCoordinate);
        log.debug("zeroOrigin: {}", zeroOrigin);
        log.debug("autoUpAxis: {}", autoUpAxis);
        log.debug("ignoreTextures: {}", ignoreTextures);
        log.debug("nameColumn: {}", nameColumn);
        log.debug("heightColumn: {}", heightColumn);
        log.debug("altitudeColumn: {}", altitudeColumn);
        log.debug("absoluteAltitude: {}", absoluteAltitude);
        log.debug("minimumHeight: {}", minimumHeight);
        log.debug("skirtHeight: {}", skirtHeight);
        log.debug("========================================");
    }
}
