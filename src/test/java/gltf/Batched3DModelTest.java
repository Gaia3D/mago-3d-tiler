package gltf;

import assimp.AssimpConverter;
import command.Configurator;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaScene;
import geometry.types.FormatType;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import tiler.LevelOfDetail;
import tiler.TileInfo;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class Batched3DModelTest {
    private static final AssimpConverter assimpConverter = new AssimpConverter(null);
    private static final String RESULT = "GaiaBatchedProject";
    private static final String INPUT_PATH = "C:\\data\\plasma-test\\ws2-3ds\\";
    private static final String OUTPUT_PATH = "C:\\data\\plasma-test\\output\\";
    private static final int TEST_COUNT = 3000;

    @Test
    void write() {
        Configurator.initLogger();
        File input = new File(INPUT_PATH);
        File output = new File(OUTPUT_PATH);
        Path inputPath = input.toPath();
        Path outputPath = output.toPath();

        GaiaUniverse universe = new GaiaUniverse(RESULT, inputPath, outputPath);
        readOriginFiles(universe, FormatType.MAX_3DS);

        TileInfo tileInfo = new TileInfo();
        tileInfo.setUniverse(universe);

        Batched3DModel batched3DModel = new Batched3DModel(tileInfo, LevelOfDetail.LOD4, null);
        batched3DModel.write("test");

        log.info("done");
    }

    public void readOriginFiles(GaiaUniverse gaiaUniverse, FormatType formatType) {
        File parent = gaiaUniverse.getInputRoot().toFile();
        if (parent.isFile()) {
            GaiaScene scene = assimpConverter.load(parent.toPath(), formatType.getExtension());
            gaiaUniverse.getScenes().add(scene);
        } else if (parent.isDirectory()){
            String[] extensions = new String[] {formatType.getExtension()};
            for (File child : FileUtils.listFiles(parent, extensions, true)) {
                GaiaScene scene = assimpConverter.load(child.toPath(), formatType.getExtension());
                gaiaUniverse.getScenes().add(scene);
            }
        }
    }
    /*private void readTree(GaiaUniverse gaiaUniverse, File inputFile, FormatType formatType) {

        if (inputFile.isFile() && inputFile.getName().endsWith("." + formatType.getExtension())) {
            GaiaScene scene = assimpConverter.load(inputFile.toPath(), formatType.getExtension());
            gaiaUniverse.getScenes().add(scene);
        } else if (inputFile.isDirectory()){
            for (File child : inputFile.listFiles()) {
                if (gaiaUniverse.getGaiaSets().size() <= TEST_COUNT) {
                    readTree(gaiaUniverse, child, formatType);
                }
            }
        }
    }*/

}