package command;

import assimp.DataLoader;
import geometry.exchangable.GaiaSet;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaScene;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.File;

class GaiaSetTest {
    @Test
    @DisplayName("convert GaiaSet to GaiaScene")
    void convertGaiaSetToGaiaScene(String[] arg) {
        String inputPath = "C:\\data\\sample\\Data3D\\DC_library_del_3DS\\DC_library_del.3ds";
        //String inputPath = "C:\\data\\sample\\a_bd001.3ds";

        String outputPath = "C:\\data\\sample\\output\\";

        CommandOption commandOption = new CommandOption();
        commandOption.setInputPath(new File(inputPath).toPath());
        commandOption.setOutputPath(new File(outputPath).toPath());

        GaiaScene scene1 = DataLoader.load(commandOption.getInputPath(), commandOption);
        GaiaUniverse gaiaUniverse = new GaiaUniverse();
        gaiaUniverse.getScenes().add(scene1);

        gaiaUniverse.writeFiles(commandOption.getOutputPath());


        String readMgbPath = "C:\\data\\sample\\output\\DC_library_del.mgb";
        GaiaSet gaiaSet = new GaiaSet();
        gaiaSet.readFile(new File(readMgbPath).toPath());

        GaiaScene scene2 = new GaiaScene(gaiaSet);
        System.out.println("test");
    }
}
