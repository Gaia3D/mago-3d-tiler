package geometry.exchangable;

import geometry.structure.GaiaMaterial;
import geometry.structure.GaiaTexture;
import geometry.types.TextureType;
import lombok.Getter;
import lombok.Setter;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Getter
@Setter
public class GaiaTextureCoordinator {
    List<GaiaMaterial> materials = new ArrayList<>();
    List<GaiaBufferDataSet> bufferDataSets = new ArrayList<>();

    public GaiaTextureCoordinator(List<GaiaMaterial> materials, List<GaiaBufferDataSet> bufferDataSets) {
        this.materials = materials;
        this.bufferDataSets = bufferDataSets;
    }

    public void batchTextures() {
        for (GaiaMaterial material : materials) {
            LinkedHashMap<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
            //TODO
        }
    }
}


