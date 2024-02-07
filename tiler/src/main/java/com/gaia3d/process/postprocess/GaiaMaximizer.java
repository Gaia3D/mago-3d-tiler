package com.gaia3d.process.postprocess;

import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@AllArgsConstructor
public class GaiaMaximizer implements PostProcess {
    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        contentInfo.getTileInfos().forEach(TileInfo::maximize);
        return contentInfo;
    }
}
