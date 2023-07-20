package geometry.extension;

import converter.AssimpConverter;
import converter.Converter;
import geometry.structure.GaiaScene;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

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