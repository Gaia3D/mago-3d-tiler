package com.gaia3d.process.preprocess.normalization;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.converter.kml.TileTransformInfo;
import com.gaia3d.converter.pointcloud.GaiaPointCloud;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

@Slf4j
public class TilingDataReference {
    private int id;

    /* source data */
    private GaiaScene scene;
    private GaiaPointCloud pointCloud;
    private Path sourcePath;

    /* temporary data */
    private GaiaSet tempSet;
    private Path tempPath;

    /* spatial info */
    private TileTransformInfo tileTransformInfo;
    private GaiaBoundingBox localBBox;
    private GaiaBoundingBox cartographicBBox;

    public void minimize(Path parentPath) {
        GaiaSet tempSet = GaiaSet.fromGaiaScene(scene);
        Path tempPath = tempSet.writeFile(parentPath, id);
        this.tempSet = tempSet;
        this.tempPath = tempPath;

        tempSet = null;
        scene.clear();
        scene = null;
    }

    public void maximize() {
        if (!isTempFileExists()) {
            return;
        }

        this.tempSet.deleteTextures();
        this.tempSet = null;
        try {
            this.tempSet = GaiaSet.readFile(this.tempPath);
        } catch (IOException e) {
            log.error("[ERROR] Failed to read the temp file: {}", this.tempPath);
        }
    }

    private boolean isTempFileExists() {
        if (this.tempPath == null) {
            log.warn("[WARN] Temp path is null, cannot check existence.");
            return false;
        }
        File tempFile = this.tempPath.toFile();
        return tempFile.isFile();
    }
}
