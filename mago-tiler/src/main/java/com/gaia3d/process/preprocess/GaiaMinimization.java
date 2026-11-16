package com.gaia3d.process.preprocess;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.LevelOfDetail;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.TempPack;
import com.gaia3d.process.TempTextures;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
/**
 * Save only the essential information of the object as a file.
 */
public class GaiaMinimization implements PreProcess {
    private final List<LevelOfDetail> lodList = new ArrayList<>();

    @Override
    public TileInfo run(TileInfo tileInfo) {
        GaiaScene scene = tileInfo.getScene();
        minimizeGaiaScene(tileInfo, scene);
        return tileInfo;
    }

    private void minimizeGaiaScene(TileInfo tileInfo, GaiaScene scene) {
        if (scene != null) {
            GaiaSet tempSet = GaiaSet.fromGaiaScene(scene);

            if (lodList.isEmpty()) {
                GlobalOptions globalOptions = GlobalOptions.getInstance();
                int minimumLod = globalOptions.getMinLod();
                int maximumLod = globalOptions.getMaxLod();
                for (int index = minimumLod; index <= maximumLod; index++) {
                    LevelOfDetail lod = LevelOfDetail.getByLevel(index);
                    if (lod != LevelOfDetail.NONE) {
                        lodList.add(lod);
                    }
                }
            }

            Path tempRoot = tileInfo.getTempPath();
            tileInfo.setTempEntry(writeToPack(tempSet, tempRoot.resolve("pack"), tempRoot.resolve("images")));
            tileInfo.setTempPath(null);
            tempSet.clear();
            tempSet = null;
            scene.clear();
            scene = null;
        }
    }

    /**
     * Appends the set to the temp pack of this thread. The original textures are referenced where they are,
     * only their resized images for the LODs above 0 are written to the images folder.
     * @return the pack entry, or null when the set could not be written
     */
    private TempPack.Entry writeToPack(GaiaSet tempSet, Path packPath, Path imagesPath) {
        try {
            for (GaiaMaterial material : tempSet.getMaterials()) {
                GaiaTexture texture = TempTextures.reference(material);
                if (texture != null) {
                    TempTextures.writeLodImages(texture, imagesPath, lodList);
                }
            }
            return TempPack.of(packPath).write(outputStream -> {
                ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
                objectOutputStream.writeObject(tempSet);
                objectOutputStream.flush();
            });
        } catch (Exception e) {
            log.error("[ERROR] GaiaSet Write Error : ", e);
            return null;
        }
    }
}
