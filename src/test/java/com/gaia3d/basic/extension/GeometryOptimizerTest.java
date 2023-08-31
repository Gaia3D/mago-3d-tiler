package com.gaia3d.basic.extension;

import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.Converter;
import com.gaia3d.basic.structure.GaiaScene;
import org.junit.jupiter.api.Test;
import com.gaia3d.process.preprocess.GeometryOptimizer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

class GeometryOptimizerTest {

    @Test
    void optimize() {
        Converter assimpConverter = new AssimpConverter(null);
        List<GaiaScene> gaiaScene = assimpConverter.load(new File("src\\test\\resources\\test.obj"));

        List<GaiaScene> gaiaScenes = new ArrayList<>();
        gaiaScenes.addAll(gaiaScene);

        GeometryOptimizer geometryOptimizer = new GeometryOptimizer();
        geometryOptimizer.optimize(gaiaScenes);

    }
}