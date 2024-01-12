package com.gaia3d.basic.structure;

import com.gaia3d.basic.types.TextureType;
import com.gaia3d.util.io.BigEndianDataInputStream;
import com.gaia3d.util.io.BigEndianDataOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector4d;

import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Path;
import java.util.*;

/**
 * A class that represents a material of a Gaia object.
 * It contains the diffuse color, ambient color, specular color, shininess, id, name and textures.
 * The textures are stored in a LinkedHashMap.
 * The key is the texture type and the value is the list of textures.
 * @ author znkim
 * @ since 1.0.0
 * @ see <a href="https://en.wikipedia.org/wiki/Texture_mapping">Texture mapping</a>
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaMaterial implements Serializable {
    private Vector4d diffuseColor = new Vector4d(1.0, 1.0, 1.0, 1.0);
    private Vector4d ambientColor = new Vector4d(1.0, 1.0, 1.0, 1.0);
    private Vector4d specularColor = new Vector4d(1.0, 1.0, 1.0, 1.0);
    private float shininess = 0.0f;

    private int id = -1;
    private String name = "no_name";
    private Map<TextureType, List<GaiaTexture>> textures = new WeakHashMap<>();
    private boolean isRepeat = false;

    public boolean isOpaqueMaterial()
    {
        boolean isOpaque = true;

        // 1rst check textures.***
        int texCount = textures.size();
        for (Map.Entry<TextureType, List<GaiaTexture>> entry : textures.entrySet()) {
            List<GaiaTexture> gaiaTextures = entry.getValue();
            for (GaiaTexture gaiaTexture : gaiaTextures) {
                if (gaiaTexture != null) {
                    String texPath = gaiaTexture.getPath();
                    int indicePunto = texPath.lastIndexOf('.');
                    String extension = texPath.substring(indicePunto + 1);
                    if(extension.equals("png") || extension.equals("PNG")) {
                        isOpaque = false;
                        break;
                    }
                }
            }
        }

        // if there are no textures, then check the diffuse color.***
        if (texCount == 0) {
            if (diffuseColor.w < 1.0) {
                isOpaque = false;
            }
        }

        return isOpaque;
    }

    public static boolean areEqualMaterials(GaiaMaterial materialA, GaiaMaterial materialB, float scaleFactor) {
        // This function determines if two materials are equal.
        if (materialA == null && materialB == null) {
            return true;
        } else if (materialA == null || materialB == null) {
            return false;
        }

        if (materialA == materialB) {
            return true;
        }

        Map<TextureType, List<GaiaTexture>> textureMapA = materialA.getTextures();

        Set<TextureType> keys = textureMapA.keySet();
        boolean hasTexture = false;
        boolean hasTextureAreEquals = true;
        for (TextureType key : keys) {
            List<GaiaTexture> listTexturesA = textureMapA.get(key);
            List<GaiaTexture> listTexturesB = materialB.getTextures().get(key);
            if (listTexturesA == null && listTexturesB == null) {
                continue;
            } else if (listTexturesA == null || listTexturesB == null) {
                hasTextureAreEquals = false;
            }
            if (listTexturesA.size() != listTexturesB.size()) {
                hasTextureAreEquals = false;
            }
            for (int i = 0; i < listTexturesA.size() && i < listTexturesB.size(); i++) {
                GaiaTexture textureA = listTexturesA.get(i);
                GaiaTexture textureB = listTexturesB.get(i);
                hasTexture = true;

                // check if the fullPath of the textures are equal.***
                String fullPathA = textureA.getFullPath();
                String fullPathB = textureB.getFullPath();

                if(fullPathA.equals(fullPathB)) {
                    hasTextureAreEquals = true;
                }
                else if (!textureA.isEqualTexture(textureB, scaleFactor)) {
                    hasTextureAreEquals = false;
                }
            }
        }

        if (!hasTexture) {
            Vector4d colorA = materialA.getDiffuseColor();
            Vector4d colorB = materialB.getDiffuseColor();
            return colorA.equals(colorB);
        }
        return hasTextureAreEquals;
    }

    public void write(BigEndianDataOutputStream stream) throws IOException {
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

    public void read(BigEndianDataInputStream stream, Path parentPath) throws IOException {
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

    public void deleteTextures() {
        for (Map.Entry<TextureType, List<GaiaTexture>> entry : textures.entrySet()) {
            List<GaiaTexture> gaiaTextures = entry.getValue();
            for (GaiaTexture gaiaTexture : gaiaTextures) {
                if (gaiaTexture != null) {
                    gaiaTexture.deleteObjects();
                }
            }
        }
    }

    public void clear() {
        if (textures != null) {
            textures.forEach((key, value) -> {
                value.forEach(GaiaTexture::clear);
            });
        }
        this.diffuseColor = null;
        this.ambientColor = null;
        this.specularColor = null;
        this.textures = null;
    }
}
