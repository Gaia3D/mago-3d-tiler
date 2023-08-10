package basic.extension;

import converter.assimp.AssimpConverter;
import converter.assimp.Converter;
import basic.structure.GaiaScene;
import org.junit.jupiter.api.Test;
import process.preprocess.GeometryOptimizer;

import java.io.File;
import java.util.ArrayList;

class GeometryOptimizerTest {

    @Test
    void optimize() {
        Converter assimpConverter = new AssimpConverter(null);
        GaiaScene gaiaScene = assimpConverter.load(new File("src\\test\\resources\\test.obj"));

        ArrayList<GaiaScene> gaiaScenes = new ArrayList<>();
        gaiaScenes.add(gaiaScene);

        GeometryOptimizer geometryOptimizer = new GeometryOptimizer();
        geometryOptimizer.optimize(gaiaScenes);

    }
}