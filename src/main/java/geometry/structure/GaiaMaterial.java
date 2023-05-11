package geometry.structure;

import geometry.types.TextureType;
import io.LittleEndianDataInputStream;
import io.LittleEndianDataOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector4d;

import java.io.IOException;
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

    public void write(LittleEndianDataOutputStream stream) throws IOException {
        stream.writeInt(id);
        stream.writeText(name);
        stream.writeVector4(diffuseColor);
        stream.writeVector4(ambientColor);
        stream.writeVector4(specularColor);
        stream.writeFloat(shininess);
        stream.writeInt(textures.size());
        for (Map.Entry<TextureType, List<GaiaTexture>> entry : textures.entrySet()) {
            TextureType gaiaMaterialType = entry.getKey();
            List<GaiaTexture> gaiaTextures = entry.getValue();

            stream.writeByte(gaiaMaterialType.getValue());
            stream.writeInt(gaiaTextures.size());
            for (GaiaTexture gaiaTexture : gaiaTextures) {
                boolean isExist = gaiaTexture != null;
                stream.writeBoolean(isExist);
                if (isExist) {
                    gaiaTexture.write(stream);
                }
            }
        }
    }

    public void read(LittleEndianDataInputStream stream, Path parentPath) throws IOException {
        this.setId(stream.readInt());
        this.setName(stream.readText());
        this.setDiffuseColor(stream.readVector4());
        this.setAmbientColor(stream.readVector4());
        this.setSpecularColor(stream.readVector4());
        this.setShininess(stream.readFloat());
        int texturesSize = stream.readInt();

        for (int i = 0; i < texturesSize; i++) {
            List<GaiaTexture> gaiaTextures = new ArrayList<>();
            byte textureType = stream.readByte();
            int gaiaTexturesSize = stream.readInt();
            TextureType gaiaMaterialType = TextureType.fromValue(textureType);

            //int gaiaTexturesSize = BinaryUtils.readInt(stream);

            for (int j = 0; j < gaiaTexturesSize; j++) {
                boolean isExist = stream.readBoolean();
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
