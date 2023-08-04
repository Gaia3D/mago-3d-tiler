package process.tileprocess.tile;

import basic.geometry.GaiaBoundingBox;
import basic.structure.GaiaNode;
import converter.FileLoader;
import converter.kml.KmlInfo;
import basic.structure.GaiaScene;
import de.javagl.jgltf.impl.v1.Scene;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.nio.file.Path;

@Getter
@Setter
@Builder
@Slf4j
public class TileInfo {
    private KmlInfo kmlInfo;
    private GaiaScene scene;

    private Matrix4d transformMatrix;
    private GaiaBoundingBox boundingBox;
    private Path scenePath;

    private void init() {
        GaiaNode rootNode = this.scene.getNodes().get(0);
        this.transformMatrix = rootNode.getTransformMatrix();
        this.boundingBox = this.scene.getBoundingBox();
        this.scenePath = this.scene.getOriginalPath();
    }

    public void minimize() {
        if (this.scene != null) {
            init();
            this.scene = null;
        } else {
            log.warn("[Warn] Can't minimize tile info because scene is null.");
        }
    }

    public void maximize(FileLoader fileLoader) {
        if (this.scene != null) {
            this.scene = null;
        }
        this.scene = fileLoader.loadScene(this.scenePath);
        GaiaNode rootNode = this.scene.getNodes().get(0);
        rootNode.setTransformMatrix(this.transformMatrix);
        this.boundingBox = this.scene.getBoundingBox();
        /*if (this.scene == null) {
            this.scene = fileLoader.loadScene(this.scenePath);
            GaiaNode rootNode = this.scene.getNodes().get(0);
            rootNode.setTransformMatrix(this.transformMatrix);
            this.boundingBox = this.scene.getBoundingBox();
            //this.scene.setOriginalPath(this.scenePath);
        } else {
            log.warn("[Warn] Can't maximize tile info because scene is not null.");
        }*/
    }

    private void reload(FileLoader fileLoader) {
        this.scene = fileLoader.loadScene(this.scenePath);
        //init();
    }
}
