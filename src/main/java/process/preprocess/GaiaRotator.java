package process.preprocess;

import basic.structure.GaiaNode;
import basic.structure.GaiaScene;
import converter.kml.KmlInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import process.tileprocess.tile.TileInfo;

@Slf4j
@AllArgsConstructor
public class GaiaRotator implements PreProcess {

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene gaiaScene = tileInfo.getScene();
        GaiaNode rootNode = gaiaScene.getNodes().get(0);
        Matrix4d transfrom = rootNode.getTransformMatrix();
        //transfrom.scale(kmlInfo.getScaleX(), kmlInfo.getScaleY(), kmlInfo.getScaleZ());
        transfrom.rotateX(Math.toRadians(90));
        rootNode.setTransformMatrix(transfrom);
        tileInfo.setTransformMatrix(transfrom);
        gaiaScene.getBoundingBox();
        return tileInfo;
    }
}
