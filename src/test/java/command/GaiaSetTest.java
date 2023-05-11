package command;

import assimp.DataLoader;
import geometry.exchangable.GaiaBuffer;
import geometry.exchangable.GaiaSet;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaScene;
import geometry.types.AttributeType;
import gltf.GltfWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import renderable.GaiaSetObject;
import renderable.RenderableObject;
import viewer.OpenGlViwer;

import java.io.File;
import java.util.List;

class GaiaSetTest {

    private static final String FILE_NAME = "DC_library_del";
    private static final String INPUT_PATH = "C:\\data\\sample\\Data3D\\DC_library_del_3DS\\DC_library_del.3ds";

    //private static final String FILE_NAME = "a_bd001";
    //private static final String INPUT_PATH = "C:\\data\\sample\\a_bd001.3ds";

    //private static final String FILE_NAME = "face";
    //private static final String INPUT_PATH = "C:\\data\\sample\\face.3ds";

    private static final String OUTPUT_PATH = "C:\\data\\sample\\output\\";

    @Test
    @DisplayName("convert GaiaSet to GaiaScene")
    void convertGaiaSetToGaiaScene() {
        writeGaiaSet();
        readGaiaSet();
    }

    @Test
    void writeGaiaSet() {
        //CommandOption commandOption = new CommandOption();
        //commandOption.setInputPath(new File(INPUT_PATH).toPath());
        //commandOption.setOutputPath(new File(OUTPUT_PATH).toPath());

        GaiaScene scene = DataLoader.load(new File(INPUT_PATH).toPath(), null);
        GaiaUniverse gaiaUniverse = new GaiaUniverse();
        gaiaUniverse.getScenes().add(scene);
        gaiaUniverse.writeFiles(new File(OUTPUT_PATH).toPath());
    }

    //@Test
    GaiaSet readGaiaSet() {
        String readMgbPath = OUTPUT_PATH + FILE_NAME + ".mgb";
        GaiaSet gaiaSet = new GaiaSet();
        gaiaSet.readFile(new File(readMgbPath).toPath());
        return gaiaSet;
    }

    @Test
    GaiaSet convertSceneToSet() {
        GaiaScene scene = DataLoader.load(new File(INPUT_PATH).toPath(), null);
        GaiaSet gaiaSet = new GaiaSet(scene);
        return gaiaSet;
    }

    @Test
    void convertSetToScene() {
        this.writeGaiaSet();
        GaiaSet set = this.readGaiaSet();
        GaiaScene scene = new GaiaScene(set);
        GltfWriter.writeGltf(scene, OUTPUT_PATH + FILE_NAME + ".gltf");
        //return scene;
    }

    @Test
    void render() {
        this.writeGaiaSet();
        GaiaSet gaiaSet1 = this.readGaiaSet();
        //GaiaSet gaiaSet2 = convertGaiaSet();

        /*GaiaBuffer buffer1 = (GaiaBuffer) gaiaSet1.getBufferDatas().get(0).getBuffers().get(AttributeType.NORMAL);
        GaiaBuffer buffer2 = (GaiaBuffer) gaiaSet2.getBufferDatas().get(0).getBuffers().get(AttributeType.NORMAL);

        float[] test1 = buffer1.getFloats();
        float[] test2 = buffer2.getFloats();

        for (int i = 0; i < test1.length; i++) {
            float value1 = test1[i];
            float value2 = test2[i];
            if (value1 == 6.9055E-41) {
                //System.out.println("test");
            }
            if (value1 != value2) {
                //System.out.println(value1);
                //System.out.println("value1 = " + value1);
                //System.out.println("value2 = " + value2);
                test1[i] = test2[i];
            }
        }*/

        OpenGlViwer openGlViwer = new OpenGlViwer();
        List<RenderableObject> renderableObjectList = openGlViwer.getRenderableObjects();
        GaiaSetObject gaiaSetObject = new GaiaSetObject(gaiaSet1);
        renderableObjectList.add(gaiaSetObject);
        openGlViwer.run();
    }
}
