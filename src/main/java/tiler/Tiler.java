package tiler;

import geometry.*;
import org.joml.Vector3d;
import org.joml.Vector4d;
import util.FileUtil;

public class Tiler {
    public static void main(String[] args) {
        String inputPath = null;
        String outputPath = null;

        for (String arg : args) {
            System.out.println("arg : " + arg);
            if (arg.contains("-input")) {
                inputPath = arg.substring(arg.indexOf("=") + 1);
                System.out.println("inputPath : " + inputPath);
            }
            if (arg.contains("-output")) {
                outputPath = arg.substring(arg.indexOf("=") + 1);
                System.out.println("outputPath : " + outputPath);
            }
        }

        if (inputPath == null) {
            System.err.println("inputPath is not defined.");
            return;
        }
        if (outputPath == null) {
            System.err.println("outputPath is not defined.");
            return;
        }

        GaiaScene scene = DataLoader.load(inputPath, null);
        GltfWriter.writeGltf(scene, outputPath);

        //GaiaScene scene = DataLoader.load("D:\\Gaia3d\\ws2_3ds\\a_bd001.3ds");
        //GaiaScene scene = DataLoader.load("C:\\data\\sample\\a_bd001.3ds", "3ds");
        //GaiaScene scene = FileUtil.sampleScene();
        //GltfWriter.write(scene, "D:\\Gaia3d\\output.gltf");
        //GltfWriter.writeGltf(scene, "C:\\data\\sample\\znkimsGltf.gltf");
        //GltfWriter.writeGlb(scene, "C:\\data\\sample\\znkimGlb.glb");
    }
}
