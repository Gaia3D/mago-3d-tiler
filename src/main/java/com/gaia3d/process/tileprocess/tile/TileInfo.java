package com.gaia3d.process.tileprocess.tile;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.converter.kml.KmlInfo;
import com.gaia3d.basic.structure.GaiaScene;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Matrix4d;

import java.io.File;
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
    private GaiaPointCloud pointCloud;
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
        if (!this.tempPath.toFile().mkdir()) {
            log.warn("[Warn] Can't create temp directory.");
        }
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
        this.scene = null;
        this.set = null;
    }

    public void deleteTemp() throws IOException {
        if (this.tempPath != null) {
            File file = this.tempPath.toFile();
            File parent = file.getParentFile();

            log.info("[DeleteTemp] {}", file);
            if (parent.isDirectory()) {
                FileUtils.deleteDirectory(parent);
            } else if (file.isFile()) {
                FileUtils.delete(file);
            } else if (file.isDirectory()) {
                FileUtils.deleteDirectory(file);
            } else {
                log.warn("[Warn] Can't delete temp file because it is not file or directory.");
            }
        }
    }
}
