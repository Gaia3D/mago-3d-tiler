package com.gaia3d.basic.model;

import com.gaia3d.basic.model.structure.MaterialStructure;
import com.gaia3d.basic.types.TextureType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector4d;

import java.io.Serializable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A class that represents a material of a Gaia object.
 * It contains the diffuse color, ambient color, specular color, shininess, id, name and textures.
 * The textures are stored in a LinkedHashMap.
 * The key is the texture type and the value is the list of textures.
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaMaterial extends MaterialStructure implements Serializable {
    private int id = -1;
    private String name = "no_name";

    private Vector4d diffuseColor = new Vector4d(1.0, 1.0, 1.0, 1.0);
    private Vector4d ambientColor = new Vector4d(1.0, 1.0, 1.0, 1.0);
    private Vector4d specularColor = new Vector4d(1.0, 1.0, 1.0, 1.0);
    private float shininess = 0.0f;
    private boolean isRepeat = false;
    private boolean isBlend = false;
    private boolean isOpaque = true;

    public boolean isOpaqueMaterial() {
        this.isOpaque = true;

        // 1rst check textures.
        int texCount = textures.size();
        for (Map.Entry<TextureType, List<GaiaTexture>> entry : textures.entrySet()) {
            List<GaiaTexture> gaiaTextures = entry.getValue();
            for (GaiaTexture gaiaTexture : gaiaTextures) {
                if (gaiaTexture != null) {
                    String texPath = gaiaTexture.getPath();
                    int indicePunto = texPath.lastIndexOf('.');
                    String extension = texPath.substring(indicePunto + 1);
                    if (extension.equals("png") || extension.equals("PNG")) {
                        this.isOpaque = false;
                        break;
                    }
                }
            }
        }

        // if there are no textures, then check the diffuse color.
        if (texCount == 0) {
            if (diffuseColor.w < 1.0) {
                this.isOpaque = false;
            }
        }
        if (diffuseColor.w < 1.0) {
            this.isOpaque = false;
            this.isBlend = true;
        }

        return this.isOpaque;
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

    public GaiaMaterial clone() {
        GaiaMaterial newMaterial = new GaiaMaterial();
        newMaterial.setDiffuseColor(new Vector4d(this.diffuseColor));
        newMaterial.setAmbientColor(new Vector4d(this.ambientColor));
        newMaterial.setSpecularColor(new Vector4d(this.specularColor));
        newMaterial.setShininess(this.shininess);
        newMaterial.setId(this.id);
        newMaterial.setName(this.name);
        for (Map.Entry<TextureType, List<GaiaTexture>> entry : this.textures.entrySet()) {
            TextureType textureType = entry.getKey();
            List<GaiaTexture> gaiaTextures = entry.getValue();
            List<GaiaTexture> newGaiaTextures = new ArrayList<>();
            for (GaiaTexture gaiaTexture : gaiaTextures) {
                newGaiaTextures.add(gaiaTexture.clone());
            }
            newMaterial.textures.put(textureType, newGaiaTextures);
        }
        return newMaterial;
    }

    public boolean hasTextures() {
        for (Map.Entry<TextureType, List<GaiaTexture>> entry : textures.entrySet()) {
            List<GaiaTexture> gaiaTextures = entry.getValue();
            for (GaiaTexture gaiaTexture : gaiaTextures) {
                if (gaiaTexture != null) {
                    return true;
                }
            }
        }

        return false;
    }

    public void saveTextures(Path imagesTempDir) {
        for (Map.Entry<TextureType, List<GaiaTexture>> entry : textures.entrySet()) {
            List<GaiaTexture> gaiaTextures = entry.getValue();
            for (GaiaTexture gaiaTexture : gaiaTextures) {
                if (gaiaTexture != null) {
                    gaiaTexture.saveImage(String.valueOf(imagesTempDir));
                }
            }
        }
    }
}
