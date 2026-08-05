package com.gaia3d.converter;

import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.temp.GaiaSceneTempGroup;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface for the converter.
 */
public interface Converter {
    List<GaiaScene> load(String path);

    List<GaiaScene> load(File file);

    List<GaiaScene> load(Path path);

    List<GaiaSceneTempGroup> convertTemp(File input, File output);
}

