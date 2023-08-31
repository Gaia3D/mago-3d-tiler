package com.gaia3d.process.tileprocess.tile;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.basic.structure.GaiaScene;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Matrix4d;

import java.io.IOException;
import java.nio.file.Path;

@Getter
@Setter
@Builder
@Slf4j
public class TileInfo {
    private KmlInfo kmlInfo;
    private GaiaScene scene;
    private GaiaSet set;
    private String name;

    private Matrix4d transformMatrix;
    private GaiaBoundingBox boundingBox;
    private Path scenePath;
    private Path outputPath;
    private Path tempPath;

    private void init() {
        GaiaNode rootNode = this.scene.getNodes().get(0);
        this.transformMatrix = rootNode.getTransformMatrix();
        this.boundingBox = this.scene.getGaiaBoundingBox();
        this.scenePath = this.scene.getOriginalPath();
        this.name = rootNode.getName();

        this.tempPath = this.outputPath.resolve("temp");
        this.tempPath.toFile().mkdir();
        //this.tempPath = this.tempPath.resolve(scenePath.getFileName() + ".set");
    }

    public void minimize(int serial) {
        if (this.scene != null) {
            init();

            GaiaSet tempSet = new GaiaSet(this.scene);
            this.tempPath = tempSet.writeFile(this.tempPath, serial);

            tempSet.clear();
            tempSet = null;
            this.scene.clear();
            this.scene = null;
        } else {
            log.warn("[Warn] Can't minimize tile info because scene is null.");
        }
    }

    public void maximize() {
        if (this.set != null) {
            this.set.deleteTextures();
            this.set = null;
        }
        this.set = new GaiaSet(this.tempPath);
    }

    public void clear() {
        //this.kmlInfo = null;
        this.scene = null;
        this.set = null;
        //this.transformMatrix = null;
        //this.boundingBox = null;
        //this.scenePath = null;
        //this.outputPath = null;
        //this.tempPath = null;
    }

    public void deleteTemp() throws IOException {
        FileUtils.deleteDirectory(this.tempPath.getParent().toFile());
    }
}
