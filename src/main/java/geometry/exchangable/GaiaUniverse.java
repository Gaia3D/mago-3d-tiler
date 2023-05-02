package geometry.exchangable;

import geometry.structure.GaiaScene;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
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
            gaiaSet.writeFile(path);
        }
    }
}
