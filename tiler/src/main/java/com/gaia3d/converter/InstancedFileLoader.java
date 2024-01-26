package com.gaia3d.converter;

import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.FastKmlReader;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
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
public class InstancedFileLoader implements FileLoader {
    private final Converter converter;
    private final FastKmlReader kmlReader;

    /* For instanced model */
    private File instanceFile = null;
    private GaiaScene instanceScene = null;

    public InstancedFileLoader(Converter converter) {
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
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        File inputFile = new File(globalOptions.getInputPath());
        String inputExtension = globalOptions.getInputFormat();
        boolean recursive = globalOptions.isRecursive();
        FormatType formatType = FormatType.fromExtension(inputExtension);
        String[] extensions = getExtensions(formatType);
        return (List<File>) FileUtils.listFiles(inputFile, extensions, recursive);
    }

    @Override
    public List<TileInfo> loadTileInfo(File file) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        Path outputPath = new File(globalOptions.getOutputPath()).toPath();
        String inputExtension = globalOptions.getInputFormat();
        FormatType formatType = FormatType.fromExtension(inputExtension);
        List<TileInfo> tileInfos = new ArrayList<>();

        KmlInfo kmlInfo = null;
        if (FormatType.KML == formatType) {
            kmlInfo = kmlReader.read(file);
            if (kmlInfo != null) {
                // I3DM reads a 3D file only once.
                if (instanceFile == null || instanceScene == null) {
                    instanceFile = new File(file.getParent(), kmlInfo.getHref());
                    List<GaiaScene> scenes = loadScene(instanceFile);
                    for (GaiaScene scene : scenes) {
                        if (instanceScene == null) {
                            instanceScene = scene;
                        }
                    }
                }
                TileInfo tileInfo = TileInfo.builder()
                        .kmlInfo(kmlInfo)
                        .scene(instanceScene)
                        .outputPath(outputPath)
                        .build();
                tileInfos.add(tileInfo);
            }
        }
        return tileInfos;
    }

    private String[] getExtensions(FormatType formatType) {
        return new String[]{formatType.getExtension().toLowerCase(), formatType.getExtension().toUpperCase()};
    }
}