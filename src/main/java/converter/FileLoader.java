package converter;

import basic.structure.GaiaScene;
import basic.types.FormatType;
import converter.assimp.AssimpConverter;
import converter.assimp.Converter;
import converter.kml.KmlInfo;
import converter.kml.KmlReader;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import process.ProcessOptions;
import process.tileprocess.tile.TileInfo;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class FileLoader {
    private final Converter converter;
    private final KmlReader kmlReader;
    private final CommandLine command;

    public FileLoader(CommandLine command) {
        this.command = command;
        try {
            this.kmlReader = new KmlReader();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
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
        String inputExtension = command.getOptionValue("inputType");
        boolean recursive = command.hasOption("recursive");
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
