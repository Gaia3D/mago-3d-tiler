package com.gaia3d.process.postprocess.instance;

import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.process.postprocess.TileModel;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;

import java.util.List;

public class Instanced3DModel implements TileModel {
    private static final String MAGIC = "i3dm";
    private static final int VERSION = 1;
    private final GltfWriter gltfWriter;

    public Instanced3DModel() {
        this.gltfWriter = new GltfWriter();
        int featureTableJSONByteLength;
        int batchTableJSONByteLength;
        String featureTableJson;
        String batchTableJson;
    }

    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        List<TileInfo> tileInfos = contentInfo.getTileInfos();


        return null;
    }
}
