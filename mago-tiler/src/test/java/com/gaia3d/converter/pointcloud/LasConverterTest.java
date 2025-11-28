package com.gaia3d.converter.pointcloud;

import com.gaia3d.command.LoggingConfiguration;
import com.gaia3d.converter.pointcloud.shuffler.BasicShuffler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.locationtech.proj4j.CRSFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

@Slf4j
@Tag("unit")
class LasConverterTest {

    static {
        LoggingConfiguration.initConsoleLogger();
    }

    @Test
    void convert() {
        File inputFile = new File("D:\\data\\mago-3d-tiler\\release-sample\\P07-cube-points");
        //File inputFile = new File("D:\\data\\mago-3d-tiler\\release-sample\\P02-busan-jingu-las");
        if (!inputFile.exists()) {
            log.warn("Input file does not exist: {}", inputFile.getAbsolutePath());
            return;
        }

        File tempDir = new File("C:\\temp\\");
        if (!tempDir.exists() && !tempDir.mkdirs()) {
            log.warn("Failed to create temp directory: {}", tempDir.getAbsolutePath());
            return;
        } else {
            try {
                FileUtils.deleteDirectory(tempDir);
                if (!tempDir.mkdirs()) {
                    log.warn("Failed to create temp directory: {}", tempDir.getAbsolutePath());
                    return;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        Path tempPath = tempDir.toPath();

        BasicShuffler shuffler = new BasicShuffler();

        CRSFactory crsFactory = new CRSFactory();
        LasConverterOptions options = LasConverterOptions.builder()
                .sourceCrs(crsFactory.createFromName("EPSG:5186"))
                .tempDirectory(tempPath)
                .build();
        LasConverter converter = new LasConverter(options);

        for (File file : Objects.requireNonNull(inputFile.listFiles())) {
            log.info("Converting file: {}", file.getAbsolutePath());
            converter.convert(file);
            log.info("Finished processing file: {}", file.getAbsolutePath());
        }
        converter.close();

        /*log.info("Creating voxel data...");
        converter.createVoxel();
        log.info("Voxel data creation completed.");*/

        log.info("Creating shuffled data...");
        converter.createShuffle();
        log.info("Shuffled data creation completed.");
    }
}