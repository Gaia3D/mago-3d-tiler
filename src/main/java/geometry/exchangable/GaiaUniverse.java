package geometry.exchangable;

import geometry.structure.GaiaScene;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import util.FileUtils;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.List;

/**
 * Set of scenes
 */
@Setter
@Getter
public class GaiaUniverse {
    private List<GaiaScene> scenes;
    public GaiaUniverse() {
        this.scenes = new ArrayList<>();
    }

    public void writeFiles(Path path) {
        for (GaiaScene scene : scenes) {
            GaiaSet gaiaSet = new GaiaSet(scene);
            Path imagesPath = path.resolve("images");
            File imagesDir = imagesPath.toFile();
            Path originalPath = scene.getOriginalPath();
            if (!imagesDir.exists()) {
                imagesDir.mkdirs();
            }
            List<String> texturePaths = scene.getAllTexturePaths();
            texturePaths.stream().forEach(texturePath -> {
                Path originalTexturePath = originalPath.getParent().resolve(texturePath);
                Path destinationTexturePath = imagesPath.resolve(texturePath);
                FileUtils.copyFile(originalTexturePath, destinationTexturePath);
            });
            gaiaSet.writeFile(path);
        }
    }
}
