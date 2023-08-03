package converter;

import converter.kml.KmlInfo;
import converter.kml.KmlReader;
import converter.assimp.AssimpConverter;
import converter.assimp.Converter;
import basic.types.FormatType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;
import process.tileprocess.tile.TileInfo;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class FileLoader {
    public final Converter converter;
    public final KmlReader kmlReader;

    public FileLoader(CommandLine command) {
        try {
            this.kmlReader = new KmlReader();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e);
        }
        this.converter = new AssimpConverter(command);
    }

    public List<TileInfo> loadTileInfos(FormatType formatType, Path input, boolean recursive) {
        List<TileInfo> tileInfos = new ArrayList<>();
        String[] extensions = getExtensions(formatType);
        int count = 0;
        log.info("Start loading tile infos.");
        List<File> fileList = (List<File>) FileUtils.listFiles(input.toFile(), extensions, recursive);
        log.info("Total {} files.", fileList.size());
        //fileList = fileList.subList(0, 300000); // FOR TEST
        int size = fileList.size();
        for (File child : fileList) {
            count++;
            log.info("[{}/{}] load tile info: {}", count, size, child);
            TileInfo tileInfo = loadTileInfo(child, formatType);
            if (tileInfo != null) {
                tileInfos.add(tileInfo);
            }
        }
        return tileInfos;
    }

    private TileInfo loadTileInfo(File file, FormatType formatType) {
        KmlInfo kmlInfo = null;
        if (FormatType.KML == formatType) {
            kmlInfo = kmlReader.read(file);
            if (kmlInfo != null) {
                file = new File(file.getParent(), kmlInfo.getHref());
                return TileInfo.builder().kmlInfo(kmlInfo).scene(converter.load(file)).build();
            } else {
                file = null;
            }
        } else {
            return TileInfo.builder().scene(converter.load(file)).build();
        }
        return null;
    }

    private String[] getExtensions(FormatType formatType) {
        return new String[]{formatType.getExtension().toLowerCase(), formatType.getExtension().toUpperCase()};
    }
}
