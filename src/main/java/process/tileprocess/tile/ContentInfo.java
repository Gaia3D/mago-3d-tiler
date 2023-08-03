package process.tileprocess.tile;

import basic.geometry.GaiaBoundingBox;
import basic.exchangable.GaiaSet;
import basic.exchangable.GaiaUniverse;
import basic.structure.GaiaMaterial;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ContentInfo {
    private String nodeCode;
    private LevelOfDetail lod;
    private List<TileInfo> tileInfos;
    private GaiaUniverse universe;
    private GaiaBoundingBox boundingBox;
    private GaiaSet batchedSet;

    public void deleteTexture() {
        this.universe.getScenes().forEach(gaiaScene -> {
            gaiaScene.getMaterials().forEach(GaiaMaterial::deleteTextures);
        });
    }
}
