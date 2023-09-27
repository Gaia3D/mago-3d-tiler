package com.gaia3d.converter;

import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.converter.kml.FastKmlReader;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.converter.pointcloud.LasConverter;
import com.gaia3d.process.ProcessOptions;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;

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
public class PointCloudFileLoader implements FileLoader {
    private final LasConverter converter;
    private final CommandLine command;

    public PointCloudFileLoader(CommandLine command, LasConverter converter) {
        this.command = command;
        this.converter = converter;
    }

    public List<GaiaPointCloud> loadPointCloud(File input) {
        return converter.load(input);
    }

    public List<File> loadFiles() {
        File inputFile = new File(command.getOptionValue(ProcessOptions.INPUT.getArgName()));
        String inputExtension = command.getOptionValue(ProcessOptions.INPUT_TYPE.getArgName());
        boolean recursive = command.hasOption(ProcessOptions.RECURSIVE.getArgName());
        FormatType formatType = FormatType.fromExtension(inputExtension);
        String[] extensions = getExtensions(formatType);
        return (ArrayList<File>) FileUtils.listFiles(inputFile, extensions, recursive);
    }

    public List<TileInfo> loadTileInfo(File file) {
        Path outputPath = new File(command.getOptionValue(ProcessOptions.OUTPUT.getArgName())).toPath();
        List<TileInfo> tileInfos = new ArrayList<>();
        file = new File(file.getParent(), file.getName());
        List<GaiaPointCloud> pointClouds = loadPointCloud(file);
        for (GaiaPointCloud pointCloud : pointClouds) {
            if (pointCloud == null) {
                log.error("Failed to load scene: {}", file.getAbsolutePath());
                return null;
            } else {
                TileInfo tileInfo = TileInfo.builder()
                        .pointCloud(pointCloud)
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
