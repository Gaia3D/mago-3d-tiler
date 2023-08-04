package process.tileprocess.tile;

import basic.geometry.GaiaBoundingBox;
import basic.exchangable.GaiaSet;
import basic.exchangable.GaiaUniverse;
import basic.structure.GaiaMaterial;
import basic.structure.GaiaScene;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ContentInfo {
    private String name;
    private String nodeCode;
    private LevelOfDetail lod;
    private List<TileInfo> tileInfos;
    //private GaiaUniverse universe;
    private GaiaBoundingBox boundingBox;
    private GaiaSet batchedSet;

    public void deleteTexture() {
        for (TileInfo tileInfo : tileInfos) {
            GaiaScene scene = tileInfo.getScene();
            List<GaiaMaterial> materials = scene.getMaterials();
            materials.forEach(GaiaMaterial::deleteTextures);
        }
    }

    public void clear() {
        this.name = null;
        this.boundingBox = null;
        this.batchedSet = null;
        this.tileInfos = null;
    }
}
