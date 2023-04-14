package command;

import assimp.DataLoader;
import de.javagl.jgltf.impl.v2.Scene;
import geometry.structure.GaiaScene;
import tiler.GltfWriter;
import util.FileUtils;
import util.GeometryUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CommandTest {
    public static void main(String[] args) {
        String inputPath = "C:\\data\\sample\\face.3ds";
        GaiaScene scene = DataLoader.load(inputPath, "3ds");
        //GaiaScene scene = GeometryUtils.sampleScene();
        String outputPath = "C:\\data\\sample\\test.gltf";
        GltfWriter.writeGltf(scene, outputPath);
    }
}
