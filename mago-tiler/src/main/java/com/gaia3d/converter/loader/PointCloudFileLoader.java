package com.gaia3d.converter.loader;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.pointcloud.GaiaLasPoint;
import com.gaia3d.converter.pointcloud.GaiaPointCloud;
import com.gaia3d.converter.pointcloud.LasConverter;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.geotools.coverage.grid.GridCoverage2D;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Loads files from the input directory.
 */
@Slf4j
@RequiredArgsConstructor
public class PointCloudFileLoader implements FileLoader {
    private final LasConverter converter;

    public List<File> loadTemp(File tempPath, List<File> files) {
        int fileCount = files.size();
        int index = 0;
        for (File file : files) {
            log.info("[Load][{}/{}] Loading point cloud file: {}", ++index, fileCount, file.getAbsolutePath());
            converter.convert(file);
            log.info("[Load][{}/{}] Finished loading point cloud file: {}", index, fileCount, file.getAbsolutePath());
        }
        converter.close();

        converter.createShuffle();
        return converter.getBucketFiles();
    }

    public GaiaPointCloud createGaiaPointCloud(File input, File tempFile) {
        return converter.readTempFileToGaiaPointCloud(input, tempFile);
    }

    public List<GaiaLasPoint> loadPointCloud(File input) {
        return converter.readTempFile(input);
    }

    public List<File> loadFiles() {
        return loadFileDefault();
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

        File tempDir = new File(globalOptions.getTempPath());
        if (!tempDir.exists()) {
            boolean created = tempDir.mkdirs();
            if (!created) {
                log.error("[ERROR] :Failed to create temp directory: {}", tempDir.getAbsolutePath());
                return null;
            }
        }

        File tempFile = new File(tempDir, UUID.randomUUID() + ".tmp");

        GaiaPointCloud pointCloud = createGaiaPointCloud(file, tempFile);

        /*List<GaiaLasPoint> points = loadPointCloud(file);
        GaiaPointCloud pointCloud = new GaiaPointCloud();
        pointCloud.setLasPoints(points);
        pointCloud.setPointCount(points.size());
        pointCloud.computeBoundingBox();
        pointCloud.minimize(tempFile);
        points.clear();*/

        if (pointCloud.getPointCount() < 1) {
            log.error("[ERROR] :Failed to load scene: {}", file.getAbsolutePath());
            return null;
        } else {
            TileInfo tileInfo = TileInfo.builder()
                    .pointCloud(pointCloud)
                    .outputPath(outputPath)
                    .build();
            tileInfos.add(tileInfo);
        }
        return tileInfos;
    }
}
