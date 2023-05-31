package geometry.exchangable;

import geometry.batch.GaiaBatcher;
import geometry.structure.GaiaMaterial;
import geometry.structure.GaiaScene;
import geometry.structure.GaiaTexture;
import geometry.types.TextureType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import util.ImageUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Getter
public class GaiaUniverse {
    private final Path inputRoot;
    private final Path outputRoot;
    private final List<GaiaScene> scenes;
    private final List<GaiaSet> sets;

    public GaiaUniverse(File inputRoot, File outputRoot) {
        if (!(inputRoot.isDirectory() && outputRoot.isDirectory())) {
            throw new NullPointerException();
        }
        this.inputRoot = inputRoot.toPath();
        this.outputRoot = outputRoot.toPath();
        this.scenes = new ArrayList<>();
        this.sets = new ArrayList<>();
    }

    public GaiaUniverse(Path inputRoot, Path outputRoot) {
        if (!(inputRoot.toFile().isDirectory() && outputRoot.toFile().isDirectory())) {
            throw new NullPointerException();
        }
        this.inputRoot = inputRoot;
        this.outputRoot = outputRoot;
        this.scenes = new ArrayList<>();
        this.sets = new ArrayList<>();
    }
    public List<GaiaSet> getGaiaSets() {
        List<GaiaSet> gaiaSets = new ArrayList<>();
        for (GaiaScene scene : scenes) {
            GaiaSet gaiaSet = new GaiaSet(scene);
            gaiaSets.add(gaiaSet);
        }
        return gaiaSets;
    }
    public void convertGaiaSet() {
        List<GaiaSet> sets = this.getScenes().stream()
                .map(GaiaSet::new)
                .collect(Collectors.toList());
        this.sets.removeAll(new ArrayList());
        this.sets.addAll(sets);
    }
    public GaiaSet write() {
        Path imagesPath = this.outputRoot.resolve("images");

        if (this.sets.size() < 0) {
            convertGaiaSet();
        }
        List<GaiaSet> gaiaSets = this.sets;
        GaiaBatcher gaiaBatcher = new GaiaBatcher();

        GaiaSet result = gaiaBatcher.batch(gaiaSets, imagesPath);

        List<String> texturePaths = new ArrayList<>();
        for (GaiaMaterial material : result.getMaterials()) {
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
        texturePaths.forEach(texturePath -> {
            Path originalTexturePath = this.inputRoot.resolve(texturePath);
            Path destinationTexturePath = imagesPath.resolve(texturePath);
            ImageUtils.copyAndResize(originalTexturePath, destinationTexturePath);
        });

        result.writeFile(this.outputRoot);
        return result;
    }

    public GaiaSet writeFiles() {
        Path imagesPath = this.outputRoot.resolve("images");
        Path originalPath = null;

        GaiaBatcher gaiaBatcher = new GaiaBatcher();
        List<GaiaSet> gaiaSets = new ArrayList<>();
        for (GaiaScene scene : scenes) {
            GaiaSet gaiaSet = new GaiaSet(scene);
            gaiaSets.add(gaiaSet);
            originalPath = scene.getOriginalPath().getParent();
        }

        GaiaSet gaiaSet = gaiaBatcher.batch(gaiaSets, imagesPath);

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

        Path finalOriginalPath = originalPath;
        texturePaths.forEach(texturePath -> {
            assert finalOriginalPath != null;
            Path originalTexturePath = finalOriginalPath.resolve(texturePath);
            Path destinationTexturePath = imagesPath.resolve(texturePath);
            ImageUtils.copyAndResize(originalTexturePath, destinationTexturePath);
        });

        gaiaSet.writeFile(this.outputRoot);
        return gaiaSet;
    }
}
