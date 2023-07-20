package geometry.extension;

import assimp.AssimpConverter;
import geometry.structure.GaiaScene;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class GeometryOptimizerTest {

    @Test
    void optimize() {
        AssimpConverter assimpConverter = new AssimpConverter(null);
        GaiaScene gaiaScene = assimpConverter.load(new File("src\\test\\resources\\test.obj"), "3ds");

        ArrayList<GaiaScene> gaiaScenes = new ArrayList<>();
        gaiaScenes.add(gaiaScene);

        GeometryOptimizer geometryOptimizer = new GeometryOptimizer();
        geometryOptimizer.optimize(gaiaScenes);

    }
}