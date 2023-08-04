package converter;

import basic.structure.GaiaScene;
import converter.kml.KmlInfo;
import converter.kml.KmlReader;
import converter.assimp.AssimpConverter;
import converter.assimp.Converter;
import basic.types.FormatType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import process.ProcessOptions;
import process.tileprocess.tile.TileInfo;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
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

    /*public List<TileInfo> loadTileInfos() {
        File inputFile = new File(command.getOptionValue(ProcessOptions.INPUT.getArgName()));
        //File outputFile = new File(command.getOptionValue(ProcessOptions.OUTPUT.getArgName()));
        //String crs = command.getOptionValue("crs");
        String inputExtension = command.getOptionValue("inputType");
        boolean recursive = command.hasOption("recursive");
        FormatType formatType = FormatType.fromExtension(inputExtension);

        log.info("Start loading tile infos.");
        List<File> fileList = loadFiles();
        //fileList = fileList.subList(0, 300000); // FOR TEST
        log.info("Total {} files.", fileList.size());

        int count = 0;
        List<TileInfo> tileInfos = new ArrayList<>();
        int size = fileList.size();
        for (File child : fileList) {
            count++;
            log.info("[{}/{}] load tile info: {}", count, size, child);
            TileInfo tileInfo = loadTileInfo(child, formatType);
            if (tileInfo != null) {
                tileInfo.minimize();
                tileInfos.add(tileInfo);
            }
        }
        return tileInfos;
    }*/

    public TileInfo loadTileInfo(File file) {
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
                    return TileInfo.builder().kmlInfo(kmlInfo).scene(scene).build();
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
                return TileInfo.builder().scene(scene).build();
            }
        }
        return null;
    }

    private String[] getExtensions(FormatType formatType) {
        return new String[]{formatType.getExtension().toLowerCase(), formatType.getExtension().toUpperCase()};
    }
}
