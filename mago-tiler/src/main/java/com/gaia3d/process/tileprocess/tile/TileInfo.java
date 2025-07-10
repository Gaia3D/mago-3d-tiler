package com.gaia3d.process.tileprocess.tile;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.pointcloud.GaiaPointCloud;
import com.gaia3d.converter.kml.TileTransformInfo;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Matrix4d;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

@Getter
@Setter
@Builder
@Slf4j
public class TileInfo {
    @Builder.Default
    private int serial = -1;
    private String name;

    private GaiaScene scene;
    private GaiaSet set;
    private GaiaPointCloud pointCloud;

    private TileTransformInfo tileTransformInfo;
    private Matrix4d transformMatrix;
    private GaiaBoundingBox boundingBox;

    private Path scenePath;
    private Path outputPath;
    private Path tempPath; // tempPath lod 0

    private List<Path> tempPathLod; // tempPath lod 0, 1, 2, 3, 4, 5, etc (deprecated)
    private GaiaBoundingBox cartographicBBox;

    @Builder.Default
    private long triangleCount = 0;
    @Builder.Default
    private boolean isI3dm = false;

    private void init() {
        GaiaNode rootNode = this.scene.getNodes().get(0);
        this.name = rootNode.getName();
        this.transformMatrix = rootNode.getTransformMatrix();
        this.boundingBox = this.scene.getGaiaBoundingBox();
        this.scenePath = this.scene.getOriginalPath();

        this.outputPath = this.outputPath.resolve(this.name).resolve("temp");
        this.tempPath = this.outputPath.resolve("temp");
        File tempFile = this.tempPath.toFile();
        if (!tempFile.exists() && tempFile.mkdir()) {
            log.info("[Pre] Created temp directory in {}", this.tempPath);
        }
    }

    /**
     * Load the minimized scene file and create a GaiaSet object.
     */
    public void maximize() {
        if (this.tempPath == null) {
            return;
        }
        File tempFile = this.tempPath.toFile();
        if (!tempFile.isFile()) {
            return;
        }
        if (this.set != null) {
            this.set.deleteTextures();
            this.set = null;
        }
        try {
            this.set = GaiaSet.readFile(this.tempPath);
        } catch (IOException e) {
            log.error("[ERROR] Failed to read the temp file: {}", this.tempPath);
        }
    }

    public void clear() {
        this.scene = null;
        this.set = null;
        pointCloud = null;
        tileTransformInfo = null;
        transformMatrix = null;
        boundingBox = null;
        scenePath = null;
        outputPath = null;
        tempPath = null;
        tempPathLod = null;
        cartographicBBox = null;

    }

    public void deleteTemp() throws IOException {
        if (this.tempPath != null) {
            File file = this.tempPath.toFile();
            File parent = file.getParentFile();
            if (file.isFile()) {
                if (parent.isDirectory()) {
                    log.info("[Delete][temp] {}", parent);
                    FileUtils.deleteDirectory(parent);
                    return;
                }
            } else if (file.isDirectory()) {
                log.info("[Delete][temp] {}", file);
                FileUtils.deleteDirectory(file);
                return;
            }
            log.warn("[WARN] Can not delete temp files: {}", file);
        }
    }

    public TileInfo clone() {
        return TileInfo.builder()
                .serial(this.serial)
                .scene(this.scene)
                .set(this.set)
                .pointCloud(this.pointCloud)
                .name(this.name)
                .tileTransformInfo(this.tileTransformInfo)
                .transformMatrix(this.transformMatrix)
                .boundingBox(this.boundingBox)
                .scenePath(this.scenePath)
                .outputPath(this.outputPath)
                .tempPath(this.tempPath)
                .tempPathLod(this.tempPathLod)
                .cartographicBBox(this.cartographicBBox)
                .triangleCount(this.triangleCount)
                .isI3dm(this.isI3dm)
                .build();
    }

    public void setGaiaSet(GaiaSet o) {
        this.set = o;
    }

    public void updateSceneInfo() {
        GaiaScene scene = this.scene;
        if (scene != null) {
            GaiaBoundingBox boundingBox = scene.updateBoundingBox();
            GaiaNode rootNode = scene.getNodes().get(0);
            this.transformMatrix = rootNode.getTransformMatrix();
            this.boundingBox = this.scene.getGaiaBoundingBox();
        } else {
            log.warn("[WARN] Scene is null, cannot update scene info.");
        }
    }
}
