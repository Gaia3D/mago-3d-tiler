package command;

import assimp.DataLoader;
import geometry.structure.GaiaScene;
import gltf.GltfWriter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import util.FileUtils;

import java.io.File;

class CommandTest {
    @Test
    @DisplayName("excute")
    void excute() {
        File thePath = new File("C:\\data\\sample\\Data3D\\gangbuk_cultur_del_3DS\\gangbuk_cultur_del.3ds");
        convert(thePath, "3ds", "C:\\data\\sample");
    }

    @Test
    @DisplayName("convert single Files")
    void convertSingleFile() {
        String[] args = {
                "-input=C:\\data\\sample\\Data3D\\Edumuseum_del_150417_02_3DS\\Edumuseum_del_150417_02.3ds",
                "-inputType=3ds",
                "-output=C:\\data\\sample\\Data3D\\Edumuseum_del_150417_02_3DS\\",
                "-outputType=gltf"
        };
        Command command = new Command();
        command.excute(args);
    }

    @Test
    @DisplayName("convert multi File")
    void convertMultiFiles() {
        String[] args = {
                "-input=C:\\data\\sample\\Data3D\\",
                "-inputType=3ds",
                "-output=C:\\data\\sample\\Data3D\\",
                "-outputType=gltf"
        };
        Command command = new Command();
        command.excute(args);
    }

    public void convert(File path, String extension, String outputPath) {
        if (path.isFile() && FileUtils.getExtension(path.getName()).equals(extension)) {
            String fileName = FileUtils.getFileNameWithoutExtension(path.getName());
            GaiaScene scene = DataLoader.load(path.getAbsolutePath(), extension, new CommandOption());

            File output = new File(outputPath, fileName + ".glb");
            System.out.println(path.getAbsolutePath() + " -> " + output.getAbsolutePath());
            GltfWriter.writeGlb(scene, output.getAbsolutePath());
        }
        if (path.isDirectory()) {
            for (String childPath : path.list()) {
                File child = new File(path, childPath);
                convert(child, extension, outputPath);
            }
        }
    }
}