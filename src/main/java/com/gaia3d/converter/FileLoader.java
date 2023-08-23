package com.gaia3d.converter;

import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.assimp.Converter;
import com.gaia3d.converter.kml.FastKmlReader;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.converter.kml.KmlReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import com.gaia3d.process.ProcessOptions;
import com.gaia3d.process.tileprocess.tile.TileInfo;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;

/**
 * Loads files from the input directory.
 * @author znkim
 * @since 1.0.0
 */
@Slf4j
public class FileLoader {
    private final Converter converter;
    private final FastKmlReader kmlReader;
    private final CommandLine command;

    public FileLoader(CommandLine command) {
        this.command = command;
        this.kmlReader = new FastKmlReader();
        this.converter = new AssimpConverter(command);
    }

    public GaiaScene loadScene(Path input) {
        return converter.load(input);
    }

    public GaiaScene loadScene(File input) {
        return converter.load(input);
    }

    public List<File> loadFiles() {
        File inputFile = new File(command.getOptionValue(ProcessOptions.INPUT.getArgName()));
        String inputExtension = command.getOptionValue(ProcessOptions.INPUT_TYPE.getArgName());
        boolean recursive = command.hasOption(ProcessOptions.RECURSIVE.getArgName());
        FormatType formatType = FormatType.fromExtension(inputExtension);
        String[] extensions = getExtensions(formatType);
        return (List<File>) FileUtils.listFiles(inputFile, extensions, recursive);
    }

    public TileInfo loadTileInfo(File file) {
        Path outputPath = new File(command.getOptionValue(ProcessOptions.OUTPUT.getArgName())).toPath();
        String inputExtension = command.getOptionValue("inputType");
        FormatType formatType = FormatType.fromExtension(inputExtension);
        KmlInfo kmlInfo = null;
        if (FormatType.KML == formatType) {
            kmlInfo = kmlReader.read(file);
            if (kmlInfo != null) {
                file = new File(file.getParent(), kmlInfo.getHref());
                GaiaScene scene = loadScene(file);
                if (scene == null) {
                    log.error("Failed to load scene: {}", file);
                    return null;
                } else {
                    return TileInfo.builder()
                            .kmlInfo(kmlInfo)
                            .scene(scene)
                            .outputPath(outputPath)
                            .build();
                }
            } else {
                file = null;
            }
        } else {
            GaiaScene scene = loadScene(file);
            if (scene == null) {
                log.error("Failed to load scene: {}", file);
                return null;
            } else {
                return TileInfo.builder()
                        .scene(scene)
                        .outputPath(outputPath)
                        .build();
            }
        }
        return null;
    }

    private String[] getExtensions(FormatType formatType) {
        return new String[]{formatType.getExtension().toLowerCase(), formatType.getExtension().toUpperCase()};
    }
}
