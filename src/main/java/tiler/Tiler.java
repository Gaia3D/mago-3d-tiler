package tiler;

import geometry.DataLoader;
import geometry.GaiaScene;

public class Tiler {
    public static void main(String[] args) {
        GaiaScene scene = DataLoader.load("C:\\data\\sample\\a_bd001_d.dae");
        GltfWriter.write(scene, "C:\\data\\cesium-ion-converted\\output.gltf");
    }
}
