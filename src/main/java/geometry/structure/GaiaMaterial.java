package geometry.structure;

import geometry.types.TextureType;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector4d;
import util.BinaryUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaMaterial {
    private Vector4d diffuseColor = new Vector4d(1.0, 1.0, 1.0, 1.0);
    private Vector4d ambientColor = new Vector4d(1.0, 1.0, 1.0, 1.0);
    private Vector4d specularColor = new Vector4d(1.0, 1.0, 1.0, 1.0);
    private float shininess = 0.0f;

    private int id = -1;
    private String name = "no_name";
    private LinkedHashMap<TextureType, List<GaiaTexture>> textures = new LinkedHashMap<>();

    public void write(DataOutputStream stream) throws IOException {
        BinaryUtils.writeInt(stream, id);
        BinaryUtils.writeText(stream, name);
        BinaryUtils.writeVector4(stream, diffuseColor);
        BinaryUtils.writeVector4(stream, ambientColor);
        BinaryUtils.writeVector4(stream, specularColor);
        BinaryUtils.writeFloat(stream, shininess);
        BinaryUtils.writeInt(stream, textures.size());
        for (Map.Entry<TextureType, List<GaiaTexture>> entry : textures.entrySet()) {
            TextureType gaiaMaterialType = entry.getKey();
            List<GaiaTexture> gaiaTextures = entry.getValue();

            BinaryUtils.writeByte(stream, gaiaMaterialType.getValue());
            BinaryUtils.writeInt(stream, gaiaTextures.size());
            for (GaiaTexture gaiaTexture : gaiaTextures) {
                boolean isExist = gaiaTexture != null;
                BinaryUtils.writeBoolean(stream, isExist);
                if (isExist) {
                    gaiaTexture.write(stream);
                }
            }
        }
    }

    public void read(DataInputStream stream, Path parentPath) throws IOException {
        this.setId(BinaryUtils.readInt(stream));
        this.setName(BinaryUtils.readText(stream));
        this.setDiffuseColor(BinaryUtils.readVector4(stream));
        this.setAmbientColor(BinaryUtils.readVector4(stream));
        this.setSpecularColor(BinaryUtils.readVector4(stream));
        this.setShininess(BinaryUtils.readFloat(stream));
        int texturesSize = BinaryUtils.readInt(stream);

        for (int i = 0; i < texturesSize; i++) {
            List<GaiaTexture> gaiaTextures = new ArrayList<>();
            byte textureType = BinaryUtils.readByte(stream);
            int gaiaTexturesSize = BinaryUtils.readInt(stream);
            TextureType gaiaMaterialType = TextureType.fromValue(textureType);

            //int gaiaTexturesSize = BinaryUtils.readInt(stream);

            for (int j = 0; j < gaiaTexturesSize; j++) {
                boolean isExist = BinaryUtils.readBoolean(stream);
                if (isExist) {
                    GaiaTexture gaiaTexture = new GaiaTexture();
                    gaiaTexture.setParentPath(parentPath);
                    gaiaTexture.read(stream);
                    gaiaTextures.add(gaiaTexture);
                }
            }
            this.textures.put(gaiaMaterialType, gaiaTextures);
        }
    }
}
