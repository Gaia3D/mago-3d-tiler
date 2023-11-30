package com.gaia3d.process.postprocess.instance;

import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.process.postprocess.TileModel;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import org.apache.commons.cli.CommandLine;

import java.util.List;

public class Instanced3DModel implements TileModel {
    private static final String MAGIC = "i3dm";
    private static final int VERSION = 1;
    private final GltfWriter gltfWriter;
    private final CommandLine command;

    public Instanced3DModel(CommandLine command) {
        this.gltfWriter = new GltfWriter();
        this.command = command;

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
