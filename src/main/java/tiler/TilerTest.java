package tiler;

import geometry.DataLoader;
import geometry.GaiaScene;

public class TilerTest {
    public static void main(String[] args) {
        //GaiaScene scene = DataLoader.load("D:\\Gaia3d\\ws2_3ds\\a_bd001.3ds");
        //GaiaScene scene = FileUtil.sampleScene();

        String path = "C:\\data\\sample\\";

        GaiaScene scene = DataLoader.load(path + "a_bd001.3ds", "3ds");

        //GltfWriter.write(scene, "D:\\Gaia3d\\output.gltf");
        GltfWriter.writeGltf(scene, path + "znkimsGltf.gltf");
        GltfWriter.writeGlb(scene, path + "znkimGlb.glb");
    }
}
