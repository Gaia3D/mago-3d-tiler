package com.gaia3d.process.tileprocess.tile.tileset;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.gaia3d.converter.jgltf.extension.ExtensionConstant;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.tileset.asset.AssetV1;
import com.gaia3d.process.tileprocess.tile.tileset.extension.Extension3DTilesContentGltf;
import com.gaia3d.process.tileprocess.tile.tileset.node.Node;
import com.gaia3d.process.tileprocess.tile.tileset.node.Properties;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
public class TilesetV2 extends Tileset {
    private AssetV1 asset;
    @JsonInclude(JsonInclude.Include.USE_DEFAULTS)
    private double geometricError = 0.0d;
    private Node root;
    private Properties properties;

    private List<String> extensionsUsed = new ArrayList<>();
    private List<String> extensionsRequired = new ArrayList<>();
    private Map<String, Object> extensions = new HashMap<>();

    public TilesetV2() {
        this.asset = new AssetV1();
        this.asset.setVersion("1.1");

        String extensionName = ExtensionConstant.EX_3DTILES_CONTENT_GLTF.getExtensionName();
        this.extensionsUsed.add(extensionName);

        Extension3DTilesContentGltf extension3DTilesContentGltf = new Extension3DTilesContentGltf();
        this.extensions.put(extensionName, extension3DTilesContentGltf);
    }

    @JsonIgnore
    public List<ContentInfo> findAllContentInfo() {
        List <ContentInfo> contentInfos = new ArrayList<>();
        if (root != null) {
            contentInfos = root.findAllContentInfo(contentInfos);
        } else {
            log.warn("[WARN] root is null");
        }
        return contentInfos;
    }
}
