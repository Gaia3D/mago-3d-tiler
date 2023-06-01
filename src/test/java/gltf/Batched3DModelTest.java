package gltf;

import assimp.AssimpConverter;
import command.Configurator;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaScene;
import geometry.types.FormatType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class Batched3DModelTest {
    private static final AssimpConverter assimpConverter = new AssimpConverter(null);
    private static final String RESULT = "GaiaBatchedProject";
    private static final String INPUT_PATH = "C:\\data\\plasma-test\\ws2-3ds\\";
    private static final String OUTPUT_PATH = "C:\\data\\plasma-test\\output\\";
    private static final int TEST_COUNT = 100;

    @Test
    void write() {
        Configurator.initLogger();
        File input = new File(INPUT_PATH);
        File output = new File(OUTPUT_PATH);
        Path inputPath = input.toPath();
        Path outputPath = output.toPath();

        GaiaUniverse universe = new GaiaUniverse(inputPath, outputPath);
        readOriginFiles(universe, FormatType.MAX_3DS);

        Batched3DModel batched3DModel = new Batched3DModel(universe);
        batched3DModel.write();

        log.info("done");
    }

    public void readOriginFiles(GaiaUniverse gaiaUniverse, FormatType formatType) {
        File inputPath = gaiaUniverse.getInputRoot().toFile();
        readTree(gaiaUniverse, inputPath, formatType);
    }
    private void readTree(GaiaUniverse gaiaUniverse, File inputFile, FormatType formatType) {
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
    }
}