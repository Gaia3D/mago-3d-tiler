package geometry.structure;

import geometry.types.TextureType;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector4d;
import util.BinaryUtils;

import java.io.*;
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

            BinaryUtils.writeText(stream, gaiaMaterialType.toString());
            BinaryUtils.writeInt(stream, gaiaTextures.size());
            for (GaiaTexture gaiaTexture : gaiaTextures) {
                boolean isExist = gaiaTexture != null;
                BinaryUtils.writeBoolean(stream, isExist);
                gaiaTexture.write(stream);
            }
        }
    }
}
