package command;

import assimp.DataLoader;
import geometry.exchangable.GaiaSet;
import geometry.structure.GaiaScene;

public class GaiaSetTest {
    public static void main(String[] arg) {
        String path = "C:\\Datas\\3dsample\\DC_library_del_3DS\\";
        GaiaScene scene = DataLoader.load(path);
        GaiaSet gaiaSet = scene.toGaiaSet();
    }
}
