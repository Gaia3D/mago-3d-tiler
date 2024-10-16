package com.gaia3d.converter.loader;

import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.pointcloud.LasConverter;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.geotools.coverage.grid.GridCoverage2D;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads files from the input directory.
 *
 * @author znkim
 * @since 1.0.0
 */
@Slf4j
@RequiredArgsConstructor
public class PointCloudFileLoader implements FileLoader {
    private final LasConverter converter;

    public List<GaiaPointCloud> loadPointCloud(File input) {
        return converter.load(input);
    }

    public List<File> loadFiles() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        File inputFile = new File(globalOptions.getInputPath());
        boolean recursive = globalOptions.isRecursive();
        FormatType formatType = globalOptions.getInputFormat();
        String[] extensions = getExtensions(formatType);
        return (ArrayList<File>) FileUtils.listFiles(inputFile, extensions, recursive);
    }

    @Override
    public List<GridCoverage2D> loadGridCoverages(List<GridCoverage2D> coverages) {
        return null;
    }

    public List<TileInfo> loadTileInfo(File file) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        Path outputPath = new File(globalOptions.getOutputPath()).toPath();
        List<TileInfo> tileInfos = new ArrayList<>();
        file = new File(file.getParent(), file.getName());
        List<GaiaPointCloud> pointClouds = loadPointCloud(file);
        for (GaiaPointCloud pointCloud : pointClouds) {
            if (pointCloud == null) {
                log.error("Failed to load scene: {}", file.getAbsolutePath());
                return null;
            } else {
                TileInfo tileInfo = TileInfo.builder().pointCloud(pointCloud).outputPath(outputPath).build();
                tileInfos.add(tileInfo);
            }
        }
        return tileInfos;
    }
}
