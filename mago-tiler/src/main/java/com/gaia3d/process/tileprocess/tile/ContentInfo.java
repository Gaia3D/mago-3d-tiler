package com.gaia3d.process.tileprocess.tile;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;

import java.util.List;

@Slf4j
@Getter
@Setter
public class ContentInfo {
    private String name;
    private String nodeCode;
    private LevelOfDetail lod;
    private List<TileInfo> tileInfos;
    private List<TileInfo> tempTileInfos;
    private List<TileInfo> remainTileInfos;
    private GaiaBoundingBox boundingBox;
    private GaiaSet batchedSet;
    private Matrix4d transformMatrix;

    public void deleteTexture() {
        for (TileInfo tileInfo : tileInfos) {
            GaiaSet set = tileInfo.getSet();
            if (set != null) {
                set.deleteTextures();
            }
        }
        if (tempTileInfos == null) {
            return;
        }
        if (remainTileInfos == null) {
            return;
        }
        for (TileInfo tileInfo : remainTileInfos) {
            GaiaSet set = tileInfo.getSet();
            if (set != null) {
                set.deleteTextures();
            }
        }
    }

    public void clear() {
        this.name = null;
        this.boundingBox = null;
        this.batchedSet = null;
        if (this.tileInfos != null) {
            this.tileInfos.forEach(TileInfo::clear);
            this.tileInfos = null;
        }
        if (this.tempTileInfos != null) {
            this.tempTileInfos.forEach(TileInfo::clear);
            this.tempTileInfos = null;
        }
        if (this.remainTileInfos != null) {
            this.remainTileInfos.forEach(TileInfo::clear);
            this.remainTileInfos = null;
        }
    }
}
