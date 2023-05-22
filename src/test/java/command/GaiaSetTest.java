package command;

import assimp.AssimpConverter;
import geometry.exchangable.GaiaBatcher;
import geometry.exchangable.GaiaSet;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaScene;
import gltf.GltfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import renderable.GaiaSetObject;
import renderable.RenderableObject;
import viewer.OpenGlViwer;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
class GaiaSetTest {
    //private static final String FILE_NAME = "DC_library_del";
    //private static final String INPUT_PATH = "C:\\data\\sample\\Data3D\\DC_library_del_3DS\\DC_library_del.3ds";
    private static final String FILE_NAME = "a_bd001";
    //private static final String INPUT_PATH = "C:\\data\\sample\\a_bd001.3ds";



    private static final String INPUT_PATH = "C:\\data\\plasma-test\\ws2-3ds\\";
    private static final String OUTPUT_PATH = "C:\\data\\plasma-test\\output\\";

    private static final AssimpConverter assimpConverter = new AssimpConverter(null);

    @Test
    @DisplayName("convert GaiaSet to GaiaScene")
    void convertGaiaSetToGaiaScene() {
        writeGaiaSet();
        readGaiaSet();
    }

    @Test
    void writeGaiaSet() {
        GaiaScene scene = assimpConverter.load(new File(INPUT_PATH).toPath(), null);
        GaiaUniverse gaiaUniverse = new GaiaUniverse();
        gaiaUniverse.getScenes().add(scene);
        gaiaUniverse.writeFiles(new File(OUTPUT_PATH).toPath());
    }

    @Test
    @Disabled
    GaiaSet readGaiaSet() {
        String readMgbPath = OUTPUT_PATH + FILE_NAME + ".mgb";
        GaiaSet gaiaSet = new GaiaSet();
        gaiaSet.readFile(new File(readMgbPath).toPath());
        return gaiaSet;
    }

    @Test
    GaiaSet convertSceneToSet() {
        GaiaScene scene = assimpConverter.load(new File(INPUT_PATH).toPath(), null);
        GaiaSet gaiaSet = new GaiaSet(scene);
        return gaiaSet;
    }

    @Test
    void convertSetToScene() {
        this.writeGaiaSet();
        GaiaSet set = this.readGaiaSet();
        GaiaScene scene = new GaiaScene(set);
        GltfWriter.writeGlb(scene, OUTPUT_PATH + FILE_NAME + ".glb");
    }

    @Test
    void render() {
        this.writeGaiaSet();
        GaiaSet gaiaSet1 = this.readGaiaSet();
        OpenGlViwer openGlViwer = new OpenGlViwer(500, 500);
        List<RenderableObject> renderableObjectList = openGlViwer.getRenderableObjects();
        GaiaSetObject gaiaSetObject = new GaiaSetObject(gaiaSet1);
        renderableObjectList.add(gaiaSetObject);
        openGlViwer.run();
    }

    @Test
    void renderMultFiles() {
        //convertMultFiles();

        File outputFile = new File(OUTPUT_PATH);
        List<GaiaSet> gaiaSets = readFiles(new ArrayList<>(), outputFile);

        OpenGlViwer openGlViwer = new OpenGlViwer(500, 500);
        /*List<RenderableObject> renderableObjects = gaiaSets.stream().map((gaiaSet) -> {
            GaiaSetObject gaiaSetObject = new GaiaSetObject(gaiaSet);
            return gaiaSetObject;
        }).collect(Collectors.toList());
        openGlViwer.setRenderableObjects((ArrayList<RenderableObject>) renderableObjects);*/

        GaiaBatcher gaiaBatcher = new GaiaBatcher();
        GaiaSet batched = gaiaBatcher.batch(gaiaSets);
        List<RenderableObject> renderableObjectList = openGlViwer.getRenderableObjects();
        GaiaSetObject gaiaSetObject = new GaiaSetObject(batched);
        renderableObjectList.add(gaiaSetObject);

        openGlViwer.run();
    }

    @Test
    void batch() {
        Configurator.initLogger();

        List<GaiaSet> gaiaSets =  readFiles(new ArrayList<>(), new File(OUTPUT_PATH));

        GaiaBatcher gaiaBatcher = new GaiaBatcher();
        GaiaSet batched = gaiaBatcher.batch(gaiaSets);
        log.info("done");
    }

    @Test
    //@Disabled
    void convertMultFiles() {
        GaiaUniverse gaiaUniverse = new GaiaUniverse();
        File inputFile = new File(INPUT_PATH);
        convertFiles(gaiaUniverse, inputFile);
        GaiaSet gaiaSets = gaiaUniverse.writeFiles(new File(OUTPUT_PATH).toPath());
        //File outputFile = new File(OUTPUT_PATH);
        //List<GaiaSet> gaiaSets = readFiles(new ArrayList<>(), outputFile);
    }


    @Test
    @Disabled("다중노드 싱글오브젝트 배칭 테스트")
    void singleObjectMultiNodeTest() {
        Configurator.initLogger();
        GaiaUniverse gaiaUniverse = new GaiaUniverse();

        String FILE_NAME = "DC_library_del.3ds";
        String INPUT_PATH = "C:\\data\\sample\\DC_library_del_3DS\\DC_library_del.3ds";
        String OUTPUT_PATH = "C:\\data\\plasma-test\\output\\";

        File inputFile = new File(INPUT_PATH);
        File outputFile = new File(OUTPUT_PATH);
        Path outputPath = outputFile.toPath().resolve("GaiaBatchedProject" + ".mgb");

        convertFiles(gaiaUniverse, inputFile);
        GaiaSet gaiaSets = gaiaUniverse.writeFiles(outputFile.toPath());

        //gaiaSets = new GaiaSet();
        //gaiaSets.readFile(outputPath);

        GaiaScene scene = new GaiaScene(gaiaSets);
        GltfWriter.writeGlb(scene, OUTPUT_PATH + "GaiaBatchedProject" + ".glb");
    }

    @Test
    @Disabled("싱글노드 다중오브젝트 배칭 테스트")
    void multiObjectSingleNodeTest() {
        Configurator.initLogger();
        GaiaUniverse gaiaUniverse = new GaiaUniverse();
        File inputFile = new File(INPUT_PATH);
        File outputFile = new File(OUTPUT_PATH);
        Path outputPath = outputFile.toPath().resolve("GaiaBatchedProject" + ".mgb");

        convertFiles(gaiaUniverse, inputFile);
        GaiaSet gaiaSets = gaiaUniverse.writeFiles(outputFile.toPath());

        //gaiaSets = new GaiaSet();
        //gaiaSets.readFile(outputPath);
        GaiaScene scene = new GaiaScene(gaiaSets);
        GltfWriter.writeGlb(scene, OUTPUT_PATH + "GaiaBatchedProject" + ".glb");
    }

    @Test
    void renderTest1312f() {
        Configurator.initLogger();

        OpenGlViwer openGlViwer = new OpenGlViwer(500, 500);
        List<RenderableObject> renderableObjectList = openGlViwer.getRenderableObjects();


        GaiaUniverse gaiaUniverse = new GaiaUniverse();
        File inputFile = new File(INPUT_PATH);
        File outputFile = new File(OUTPUT_PATH);
        Path outputPath = outputFile.toPath().resolve("GaiaBatchedProject" + ".mgb");

        convertFiles(gaiaUniverse, inputFile);
        GaiaSet writedGaiaSets = gaiaUniverse.writeFiles(outputFile.toPath());
        //GaiaScene scene = new GaiaScene(writedGaiaSets);

        GaiaSet readedGaiaSet = new GaiaSet();
        readedGaiaSet.readFile(outputPath);

        GaiaScene scene = new GaiaScene(readedGaiaSet);
        GltfWriter.writeGlb(scene, OUTPUT_PATH + FILE_NAME + ".glb");

        GaiaSetObject gaiaSetObject = new GaiaSetObject(readedGaiaSet);
        renderableObjectList.add(gaiaSetObject);
        openGlViwer.run();
    }


    GaiaUniverse convertFiles(GaiaUniverse gaiaUniverse, File inputFile) {
        if (inputFile.isFile() && inputFile.getName().endsWith(".3ds")) {
            GaiaScene scene = assimpConverter.load(inputFile.toPath(), null);
            gaiaUniverse.getScenes().add(scene);
        } else if (inputFile.isDirectory()){
            for (File child : inputFile.listFiles()) {
                if (gaiaUniverse.getGaiaSets().size() <= 100) {
                    convertFiles(gaiaUniverse, child);
                }
            }
        }
        return gaiaUniverse;
    }

    List<GaiaSet> readFiles(List<GaiaSet> gaiaSets, File outputFile) {
        if (outputFile.isFile() && outputFile.getName().endsWith(".mgb")) {
            GaiaSet gaiaSet = new GaiaSet();
            gaiaSet.readFile(outputFile.toPath());
            gaiaSets.add(gaiaSet);
        } else if (outputFile.isDirectory()){
            for (File child : outputFile.listFiles()) {
                if (gaiaSets.size() <= 100) {
                    readFiles(gaiaSets, child);
                }
            }
        }
        return gaiaSets;
    }
}
