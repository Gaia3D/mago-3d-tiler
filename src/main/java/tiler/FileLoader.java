package tiler;

import command.KmlInfo;
import command.KmlReader;
import converter.AssimpConverter;
import converter.Converter;
import geometry.structure.GaiaScene;
import geometry.types.FormatType;
import lombok.AllArgsConstructor;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class FileLoader {
    public final Converter converter;

    public FileLoader(CommandLine command) {
        converter = new AssimpConverter(command);
    }

    public List<TileInfo> loadTileInfos(FormatType formatType, Path input, boolean recursive) {
        List<TileInfo> tileInfos = new ArrayList<>();
        String[] extensions = getExtensions(formatType);
        for (File child : FileUtils.listFiles(input.toFile(), extensions, recursive)) {
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
            kmlInfo = KmlReader.read(file);
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
