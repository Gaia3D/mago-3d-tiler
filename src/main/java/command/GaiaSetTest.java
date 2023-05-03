package command;

import assimp.DataLoader;
import geometry.exchangable.GaiaSet;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaScene;

import java.io.File;

public class GaiaSetTest {
    public static void main(String[] arg) {
        //String inputPath = "C:\\data\\sample\\Data3D\\DC_library_del_3DS\\DC_library_del.3ds";
        //String outputPath = "C:\\data\\sample\\Data3D\\DC_library_del_3DS\\";

        //String inputPath = "D:\\Gaia3d\\Data3D\\DC_library_del_3DS\\DC_library_del.3ds";
        //String outputPath = "D:\\Gaia3d\\Data3D\\";

        String inputPath = "D:\\Gaia3d\\Data3D\\easyTest\\a_bd001.3ds";
        String outputPath = "D:\\Gaia3d\\Data3D\\output\\";

        CommandOption commandOption = new CommandOption();
        commandOption.setInputPath(new File(inputPath).toPath());
        commandOption.setOutputPath(new File(outputPath).toPath());





        GaiaScene scene1 = DataLoader.load(commandOption.getInputPath(), commandOption);

        GaiaUniverse gaiaUniverse = new GaiaUniverse();
        gaiaUniverse.getScenes().add(scene1);

        gaiaUniverse.writeFiles(commandOption.getOutputPath());
    }
}
