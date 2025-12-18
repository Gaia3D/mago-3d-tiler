package com.gaia3d.converter.loader;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.pointcloud.GaiaLasPoint;
import com.gaia3d.converter.pointcloud.GaiaPointCloud;
import com.gaia3d.converter.pointcloud.LasConverter;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.eclipse.imagen.Interpolation;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.Interpolator2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;

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

    @Override
    public List<GridCoverage2D> loadGridCoverages(File geoTiffPath, List<GridCoverage2D> coverages) {
        if (geoTiffPath.isFile()) {
            log.info("GeoTiff path is file. Loading only the GeoTiff file.");
            log.info(" - Loading GeoTiff file: {}", geoTiffPath.getAbsolutePath());
            GridCoverage2D coverage = loadGeoTiff(geoTiffPath);
            Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
            GridCoverage2D interpolatedCoverage = Interpolator2D.create(coverage, interpolation);
            coverages.add(interpolatedCoverage);
        } else if (geoTiffPath.isDirectory()) {
            log.info("GeoTiff path is directory. Loading all GeoTiff files in the directory.");
            File[] files = FileUtils.listFiles(geoTiffPath, new String[]{"tif", "tiff"}, true).toArray(new File[0]);
            for (File file : files) {
                log.info(" - Loading GeoTiff file: {}", file.getAbsolutePath());
                GridCoverage2D coverage = loadGeoTiff(file);
                Interpolation interpolation = Interpolation.getInstance(Interpolation.INTERP_BILINEAR);
                GridCoverage2D interpolatedCoverage = Interpolator2D.create(coverage, interpolation);
                coverages.add(interpolatedCoverage);
            }
        } else {
            throw new RuntimeException("GeoTiff path is neither a file nor a directory.");
        }
        return coverages;
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
