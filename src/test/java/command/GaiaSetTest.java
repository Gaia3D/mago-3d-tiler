package command;

import assimp.AssimpConverter;
import geometry.batch.Batcher;
import geometry.exchangable.GaiaSet;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaScene;
import geometry.types.FormatType;
import gltf.GltfWriter;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import renderable.GaiaSetObject;
import renderable.RenderableObject;
import tiler.LevelOfDetail;
import viewer.OpenGlViwer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
class GaiaSetTest {
    private static final AssimpConverter assimpConverter = new AssimpConverter(null);
    private static final String RESULT = "GaiaBatchedProject";
    private static final String INPUT_PATH = "C:\\data\\plasma-test\\ws2-3ds\\";
    private static final String OUTPUT_PATH = "C:\\data\\plasma-test\\output\\";
    private static final int TEST_COUNT = 1000;

    @Test
    public void read() {
        Configurator.initLogger();
        File input = new File(INPUT_PATH);
        File output = new File(OUTPUT_PATH);
        Path inputPath = input.toPath();
        Path outputPath = output.toPath();
        GaiaUniverse universe = new GaiaUniverse(RESULT, inputPath, outputPath);
        readOriginFiles(universe, FormatType.MAX_3DS);
        universe.convertGaiaSet();
        Batcher batcher = new Batcher(universe, null, LevelOfDetail.LOD4);
        GaiaSet set = batcher.batch();
        writeGlb(set);
    }

    @Test
    public void readTemp() {
        Configurator.initLogger();
        GaiaSet set = readTempFile();
    }

    @Test
    public void renderTemp() {
        Configurator.initLogger();
        GaiaSet set = readTempFile();
        List<GaiaSet> sets = new ArrayList<>();
        sets.add(set);
        render(sets);
    }

    @Test
    public void renderFiles(){
        Configurator.initLogger();
        File input = new File(OUTPUT_PATH);
        Path inputPath = input.toPath();
        List<GaiaSet> sets = GaiaSet.readFiles(inputPath);
        render(sets);
    }

    private GaiaSet readTempFile() {
        String tempFileName = RESULT + "." + FormatType.TEMP.getExtension();
        File input = new File(OUTPUT_PATH, tempFileName);
        Path inputPath = input.toPath();
        return new GaiaSet(inputPath);
    }
    private void writeGlb(GaiaSet set) {
        GaiaScene scene = new GaiaScene(set);
        String outputFileName = RESULT + "." + FormatType.GLB.getExtension();
        File ouputFile = new File(OUTPUT_PATH, outputFileName);
        GltfWriter.writeGlb(scene, ouputFile);
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
    private void render(List<GaiaSet> sets) {
        OpenGlViwer openGlViwer = new OpenGlViwer(500, 500);
        List<RenderableObject> renderableObjects = openGlViwer.getRenderableObjects();
        List<RenderableObject> gaiaSetObejcts = sets.stream()
                .map(GaiaSetObject::new)
                .collect(Collectors.toList());
        renderableObjects.addAll(gaiaSetObejcts);
        openGlViwer.run();
    }
    private List<GaiaSet> readTemps(List<GaiaSet> gaiaSets, File outputFile) {
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
                    readTemps(gaiaSets, child);
                }
            }
        }
        return gaiaSets;
    }
}
