package command;

import assimp.AssimpConverter;
import geometry.exchangable.GaiaSet;
import geometry.types.FormatType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import renderable.GaiaSetObject;
import renderable.RenderableObject;
import viewer.OpenGlViwer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
class GaiaSetMgbTest {
    private static final String FILE_NAME = "a_bd001";
    private static final String INPUT_PATH = "C:\\data\\plasma-test\\ws2-3ds\\";
    private static final String OUTPUT_PATH = "C:\\data\\plasma-test\\output\\";
    private static final AssimpConverter assimpConverter = new AssimpConverter(null);

    @Test
    void renderMgb() {
        Configurator.initLogger();
        OpenGlViwer openGlViwer = new OpenGlViwer(500, 500);
        List<RenderableObject> renderableObjectList = openGlViwer.getRenderableObjects();

        File output = new File(OUTPUT_PATH);
        Path outputFile = output.toPath().resolve("GaiaBatchedProject" + "." + FormatType.TEMP.getExtension());

        GaiaSet gaiaSet = new GaiaSet();
        gaiaSet.readFile(outputFile);
        renderableObjectList.add(new GaiaSetObject(gaiaSet));

        openGlViwer.run();
    }

    @Test
    void renderMgbs() {
        Configurator.initLogger();
        OpenGlViwer openGlViwer = new OpenGlViwer(500, 500);
        List<RenderableObject> renderableObjectList = openGlViwer.getRenderableObjects();

        List<GaiaSet> readed = readMgb(null, new File(OUTPUT_PATH));
        List<RenderableObject> gaiaSets = readed.stream().map((gaiaSet) -> {
            return new GaiaSetObject(gaiaSet);
        }).collect(Collectors.toList());
        renderableObjectList.addAll(gaiaSets);

        openGlViwer.run();
    }

    private List<GaiaSet> readMgb(List<GaiaSet> gaiaSets, File outputFile) {
        if (gaiaSets == null) {
            gaiaSets = new ArrayList<>();
        }
        if (outputFile.isFile() && outputFile.getName().endsWith("." + FormatType.TEMP.getExtension())) {
            GaiaSet gaiaSet = new GaiaSet();
            gaiaSet.readFile(outputFile.toPath());
            gaiaSets.add(gaiaSet);
        } else if (outputFile.isDirectory()){
            for (File child : outputFile.listFiles()) {
                if (gaiaSets.size() <= 100) {
                    readMgb(gaiaSets, child);
                }
            }
        }
        return gaiaSets;
    }
}
