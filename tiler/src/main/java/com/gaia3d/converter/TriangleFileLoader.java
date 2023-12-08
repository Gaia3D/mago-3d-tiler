package com.gaia3d.converter;

import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.converter.kml.FastKmlReader;
import com.gaia3d.converter.kml.KmlInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import com.gaia3d.process.ProcessOptions;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.geotiff.GeoTiffReader;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads files from the input directory.
 * @author znkim
 * @since 1.0.0
 */
@Slf4j
public class TriangleFileLoader implements FileLoader {
    private final Converter converter;
    private final FastKmlReader kmlReader;
    private final CommandLine command;

    public TriangleFileLoader(CommandLine command, Converter converter) {
        this.command = command;
        this.kmlReader = new FastKmlReader();
        this.converter = converter;
    }

    public List<GaiaScene> loadScene(File input) {
        return converter.load(input);
    }

    private GridCoverage2D loadGeoTiff(File file) {
        GridCoverage2D coverage = null;
        try {
            GeoTiffReader reader = new GeoTiffReader(file);
            coverage = reader.read(null);
            reader.dispose();
        } catch (Exception e) {
            log.debug("Failed to load GeoTiff file: {}", file.getAbsolutePath());
            throw new RuntimeException(e);
        }
        return coverage;
    }

    public List<GridCoverage2D> loadGridCoverages(List<GridCoverage2D> coverages) {
        File geoTiffPath = new File(command.getOptionValue(ProcessOptions.GEO_TIFF.getArgName()));
        if (geoTiffPath.isFile()) {
            log.info("GeoTiff path is file. Loading only the GeoTiff file.");
            GridCoverage2D coverage = loadGeoTiff(geoTiffPath);
            coverages.add(coverage);
        } else if (geoTiffPath.isDirectory()) {
            log.info("GeoTiff path is directory. Loading all GeoTiff files in the directory.");
        }
        return coverages;
    }

    public List<File> loadFiles() {
        File inputFile = new File(command.getOptionValue(ProcessOptions.INPUT.getArgName()));
        String inputExtension = command.getOptionValue(ProcessOptions.INPUT_TYPE.getArgName());
        boolean recursive = command.hasOption(ProcessOptions.RECURSIVE.getArgName());
        FormatType formatType = FormatType.fromExtension(inputExtension);
        String[] extensions = getExtensions(formatType);
        return (List<File>) FileUtils.listFiles(inputFile, extensions, recursive);
    }

    public List<TileInfo> loadTileInfo(File file) {
        Path outputPath = new File(command.getOptionValue(ProcessOptions.OUTPUT.getArgName())).toPath();
        String inputExtension = command.getOptionValue("inputType");
        FormatType formatType = FormatType.fromExtension(inputExtension);

        List<TileInfo> tileInfos = new ArrayList<>();
        KmlInfo kmlInfo = null;
        if (FormatType.KML == formatType) {
            kmlInfo = kmlReader.read(file);
            if (kmlInfo != null) {
                file = new File(file.getParent(), kmlInfo.getHref());
                List<GaiaScene> scenes = loadScene(file);
                for (GaiaScene scene : scenes) {
                    if (scene == null) {
                        log.error("Failed to load scene: {}", file.getAbsolutePath());
                        return null;
                    } else {
                        TileInfo tileInfo = TileInfo.builder()
                                .kmlInfo(kmlInfo)
                                .scene(scene)
                                .outputPath(outputPath)
                                .build();
                        tileInfos.add(tileInfo);
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
                    TileInfo tileInfo = TileInfo.builder()
                            .scene(scene)
                            .outputPath(outputPath)
                            .build();
                    tileInfos.add(tileInfo);
                }
            }
        }
        return tileInfos;
    }

    private String[] getExtensions(FormatType formatType) {
        return new String[]{formatType.getExtension().toLowerCase(), formatType.getExtension().toUpperCase()};
    }
}
