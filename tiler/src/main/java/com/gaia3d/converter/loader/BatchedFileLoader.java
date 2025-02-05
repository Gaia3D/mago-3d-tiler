package com.gaia3d.converter.loader;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.Converter;
import com.gaia3d.converter.geometry.ExtrusionTempGenerator;
import com.gaia3d.converter.kml.AttributeReader;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;

import javax.media.jai.Interpolation;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads files from the input directory.
 */
@Slf4j
@RequiredArgsConstructor
public class BatchedFileLoader implements FileLoader {
    private final Converter converter;
    private final AttributeReader kmlReader;
    private final ExtrusionTempGenerator tempGenerator;

    public List<File> loadTemp(File tempPath, List<File> files) {
        return tempGenerator.generate(tempPath, files);
    }

    public List<GaiaScene> loadScene(File input) {
        return converter.load(input);
    }

    private GridCoverage2D loadGeoTiff(File file) {
        GridCoverage2D coverage = null;
        try {
            GeoTiffReader reader = new GeoTiffReader(file);
            Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            coverage = (GridCoverage2D) Operations.DEFAULT.interpolate(reader.read(null), interpolation);
            reader.dispose();
        } catch (Exception e) {
            log.debug("Failed to load GeoTiff file: {}", file.getAbsolutePath());
            throw new RuntimeException(e);
        }
        return coverage;
    }

    public List<GridCoverage2D> loadGridCoverages(List<GridCoverage2D> coverages) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        File geoTiffPath = new File(globalOptions.getTerrainPath());
        if (geoTiffPath.isFile()) {
            log.info("GeoTiff path is file. Loading only the GeoTiff file.");
            GridCoverage2D coverage = loadGeoTiff(geoTiffPath);
            coverages.add(coverage);
        } else if (geoTiffPath.isDirectory()) {
            log.info("GeoTiff path is directory. Loading all GeoTiff files in the directory.");
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
        KmlInfo kmlInfo = null;
        if (FormatType.KML == formatType) {
            List<KmlInfo> kmlInfos = kmlReader.readAll(file);
            if (kmlInfos != null) {
                for (KmlInfo info : kmlInfos) {
                    kmlInfo = info;
                    if (kmlInfo != null) {
                        assert file != null;
                        file = new File(file.getParent(), kmlInfo.getHref());
                        List<GaiaScene> scenes = loadScene(file);
                        for (GaiaScene scene : scenes) {
                            if (scene == null) {
                                log.error("Failed to load scene: {}", file.getAbsolutePath());
                                return null;
                            } else {
                                TileInfo tileInfo = TileInfo.builder().kmlInfo(kmlInfo).scene(scene).outputPath(outputPath).build();
                                tileInfos.add(tileInfo);
                            }
                        }
                    } else {
                        file = null;
                    }
                }
            } else {
                file = null;
            }
        } else {
            List<GaiaScene> scenes = loadScene(file);
            for (GaiaScene scene : scenes) {
                if (scene == null) {
                    log.error("Failed to load scene: {}", file.getAbsolutePath());
                    return null;
                } else {
                    TileInfo tileInfo = TileInfo.builder().scene(scene).outputPath(outputPath).build();
                    tileInfos.add(tileInfo);
                }
            }
        }
        return tileInfos;
    }

    /*private String[] getExtensions(FormatType formatType) {
        String[] extensions = new String[4];
        extensions[0] = formatType.getExtension().toLowerCase();
        extensions[1] = formatType.getExtension().toUpperCase();
        extensions[2] = formatType.getSubExtension().toLowerCase();
        extensions[3] = formatType.getSubExtension().toUpperCase();
        return extensions;
    }*/
}
