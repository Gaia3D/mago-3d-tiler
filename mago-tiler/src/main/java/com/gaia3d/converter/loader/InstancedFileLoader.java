package com.gaia3d.converter.loader;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.assimp.validation.GaiaSceneRepair;
import com.gaia3d.converter.assimp.validation.GaiaSceneValidationReport;
import com.gaia3d.converter.assimp.validation.GaiaSceneValidationReportCollector;
import com.gaia3d.converter.assimp.validation.GaiaSceneValidator;
import com.gaia3d.converter.kml.AttributeReader;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.imagen.Interpolation;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.Interpolator2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads files from the input directory.
 */
@Slf4j
@RequiredArgsConstructor
public class InstancedFileLoader implements FileLoader {
    private final Converter converter;
    private final AttributeReader attributeReader;
    private final InstancedTempGenerator tempGenerator;

    private final GaiaSceneValidator sceneValidator = new GaiaSceneValidator();
    private final GaiaSceneRepair sceneRepair = new GaiaSceneRepair();

    /* For instanced model */
    private File instanceFile = null;
    private GaiaScene instanceScene = null;

    public List<File> loadTemp(File tempPath, List<File> files) {
        return tempGenerator.generate(tempPath, files);
    }

    public List<GaiaScene> loadScene(File input) {
        List<GaiaScene> scenes = converter.load(input);
        GaiaSceneValidationReport report = sceneValidator.validate(input, scenes);
        if (report.hasIssues()) {
            log.warn("[WARN] Validation issues: {}", report.toIssuesSummaryString());
            GlobalOptions globalOptions = GlobalOptions.getInstance();
            if (globalOptions.isValidationReport()) {
                GaiaSceneValidationReportCollector.getInstance().collect(report, new File(globalOptions.getTempPath()));
            }
            sceneRepair.repair(report, scenes);
            GaiaSceneValidationReport afterRepair = sceneValidator.validate(input, scenes);
            if (afterRepair.hasIssues()) {
                log.debug("[WARN] Unresolved issues after repair in {}: {}", input.getName(), afterRepair.toDetailString());
            } else {
                log.debug("[INFO] All issues resolved after repair: {}", input.getName());
            }
        }
        return scenes;
    }

    private GridCoverage2D loadGeoTiff(File file) {
        GridCoverage2D coverage = null;
        try {
            GeoTiffReader reader = new GeoTiffReader(file);
            Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            GeneralParameterValue[] params = null;
            coverage = (GridCoverage2D) Operations.DEFAULT.interpolate(reader.read(params), interpolation);
            reader.dispose();
        } catch (Exception e) {
            log.debug("Failed to load GeoTiff file: {}", file.getAbsolutePath());
            throw new RuntimeException(e);
        }
        return coverage;
    }

    @Override
    public List<GridCoverage2D> loadGridCoverages(File geoTiffPath, List<GridCoverage2D> coverages) {
        if (geoTiffPath.isFile()) {
            log.info("GeoTiff path is file. Loading only the GeoTiff file.");
            log.info(" - Loading GeoTiff file: {}", geoTiffPath.getAbsolutePath());
            GridCoverage2D coverage = loadGeoTiff(geoTiffPath);
            //Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            //GridCoverage2D interpolatedCoverage = Interpolator2D.create(coverage, interpolation);
            coverages.add(coverage);
        } else if (geoTiffPath.isDirectory()) {
            log.info("GeoTiff path is directory. Loading all GeoTiff files in the directory.");
            File[] files = FileUtils.listFiles(geoTiffPath, new String[]{"tif", "tiff"}, true).toArray(new File[0]);
            for (File file : files) {
                log.info(" - Loading GeoTiff file: {}", file.getAbsolutePath());
                GridCoverage2D coverage = loadGeoTiff(file);
                //Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
                //GridCoverage2D interpolatedCoverage = Interpolator2D.create(coverage, interpolation);
                coverages.add(coverage);
            }
        } else {
            throw new RuntimeException("GeoTiff path is neither a file nor a directory.");
        }
        return coverages;
    }

    @Override
    public List<File> loadFiles() {
        return loadFileDefault();
    }

    @Override
    public List<TileInfo> loadTileInfo(File file) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        Path outputPath = new File(globalOptions.getOutputPath()).toPath();
        FormatType formatType = globalOptions.getInputFormat();
        List<TileInfo> tileInfos = new ArrayList<>();

        if (InstancedTempFileHelper.isTempFile(file)) {
            ensureInstanceScene(file, formatType);
            List<TileTransformInfo> tileTransformInfos = InstancedTempFileHelper.read(file);
            for (TileTransformInfo tileTransformInfo : tileTransformInfos) {
                TileInfo tileInfo = TileInfo.builder()
                        .scene(instanceScene)
                        .tileTransformInfo(tileTransformInfo)
                        .isI3dm(true)
                        .outputPath(outputPath)
                        .build();
                tileInfos.add(tileInfo);
            }
            return tileInfos;
        }

        if (FormatType.KML == formatType) {
            List<TileTransformInfo> tileTransformInfos = attributeReader.readAll(file);
            if (tileTransformInfos != null) {
                for (TileTransformInfo tileTransformInfo : tileTransformInfos) {
                    if (instanceFile == null || instanceScene == null) {
                        instanceFile = new File(file.getParent(), tileTransformInfo.getHref());
                        List<GaiaScene> scenes = loadScene(instanceFile);
                        for (GaiaScene scene : scenes) {
                            if (instanceScene == null) {
                                instanceScene = scene;
                            }
                        }
                    }
                    TileInfo tileInfo = TileInfo.builder()
                            .isI3dm(true)
                            .tileTransformInfo(tileTransformInfo)
                            .scene(instanceScene)
                            .outputPath(outputPath)
                            .build();
                    tileInfos.add(tileInfo);
                }
            }
        } else {
            ensureInstanceScene(file, formatType);
            // geojson, shape type
            List<TileTransformInfo> tileTransformInfos = attributeReader.readAll(file);
            if (tileTransformInfos != null) {
                for (TileTransformInfo tileTransformInfo : tileTransformInfos) {
                    TileInfo tileInfo = TileInfo.builder()
                            .scene(instanceScene)
                            .tileTransformInfo(tileTransformInfo)
                            .isI3dm(true)
                            .outputPath(outputPath)
                            .build();
                    tileInfos.add(tileInfo);
                }
            }
        }
        return tileInfos;
    }

    private void ensureInstanceScene(File file, FormatType formatType) {
        if (instanceScene != null) {
            return;
        }

        if (FormatType.KML == formatType) {
            return;
        }

        File meshData = new File(GlobalOptions.getInstance().getInstancePath());
        List<GaiaScene> scenes = loadScene(meshData);
        for (GaiaScene scene : scenes) {
            if (instanceScene == null) {
                instanceScene = scene;
            }
        }
        if (instanceScene == null) {
            throw new RuntimeException("Failed to load instanced scene: " + file.getAbsolutePath());
        }
    }
}
