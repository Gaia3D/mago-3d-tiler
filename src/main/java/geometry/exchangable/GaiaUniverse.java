package geometry.exchangable;

import de.javagl.jgltf.impl.v2.Material;
import geometry.structure.GaiaMaterial;
import geometry.structure.GaiaScene;
import geometry.structure.GaiaTexture;
import geometry.types.TextureType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import util.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Set of scenes
 */
@Slf4j
@Setter
@Getter
public class GaiaUniverse {
    private List<GaiaScene> scenes;
    public GaiaUniverse() {
        this.scenes = new ArrayList<>();
    }

    public List<GaiaSet> getGaiaSets() {
        List<GaiaSet> gaiaSets = new ArrayList<>();
        for (GaiaScene scene : scenes) {
            GaiaSet gaiaSet = new GaiaSet(scene);
            gaiaSets.add(gaiaSet);
        }
        return gaiaSets;
    }

    public void filterTexture() {


        List<GaiaMaterial> gaiaMaterialList = new ArrayList<>();


        /*for (GaiaScene scene : scenes) {

            List<GaiaMaterial> materials = scene.getMaterials();
            for (GaiaMaterial material : materials) {
                if (material.getTextureType() == TextureType.DIFFUSE) {
                    gaiaMaterialList.add(material);
                }
            }

        }*/
    }

    public GaiaSet writeFiles(Path path) {
        GaiaBatcher gaiaBatcher = new GaiaBatcher();
        Path originalPath = null;
        List<GaiaSet> gaiaSets = new ArrayList<>();
        for (GaiaScene scene : scenes) {
            GaiaSet gaiaSet = new GaiaSet(scene);
            gaiaSets.add(gaiaSet);
            originalPath = scene.getOriginalPath().getParent();
        }
        GaiaSet gaiaSet = gaiaBatcher.batch(gaiaSets);
        List<String> texturePaths = new ArrayList<>();
        for (GaiaMaterial material : gaiaSet.getMaterials()) {
            LinkedHashMap<TextureType, List<GaiaTexture>> textures = material.getTextures();
            textures.forEach((textureType, gaiaTextures) -> {
                for (GaiaTexture gaiaTexture : gaiaTextures) {
                    String texturePath = gaiaTexture.getPath();
                    if (texturePath != null) {
                        texturePaths.add(texturePath);
                    }
                }
            });
        }

        Path imagesPath = path.resolve("images");
        File imagesDir = imagesPath.toFile();
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }
        Path finalOriginalPath = originalPath;
        texturePaths.stream().forEach(texturePath -> {
            Path originalTexturePath = finalOriginalPath.resolve(texturePath);
            Path destinationTexturePath = imagesPath.resolve(texturePath);
            FileUtils.copyPath(originalTexturePath, destinationTexturePath);
        });

        gaiaSet.writeFile(path);

        /*for (GaiaScene scene : scenes) {
            GaiaSet gaiaSet = new GaiaSet(scene);
            Path imagesPath = path.resolve("images");
            File imagesDir = imagesPath.toFile();
            Path originalPath = scene.getOriginalPath();
            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
            }
            List<String> texturePaths = scene.getAllTexturePaths();
            log.info("texturePaths: " + texturePaths);

            texturePaths.stream().forEach(texturePath -> {
                Path originalTexturePath = originalPath.getParent().resolve(texturePath);
                Path destinationTexturePath = imagesPath.resolve(texturePath);
                FileUtils.copyPath(originalTexturePath, destinationTexturePath);
            });
            gaiaSet.writeFile(path);
            gaiaSets.add(gaiaSet);
        }*/
        return gaiaSet;
    }
}
