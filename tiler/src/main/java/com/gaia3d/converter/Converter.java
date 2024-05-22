package com.gaia3d.converter;

import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.converter.assimp.AssimpConverter;

import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Interface for the converter.
 * @author znkim
 * @since 1.0.0
 * @see AssimpConverter
 */
public interface Converter {
    List<GaiaScene> load(String path);
    List<GaiaScene> load(File file);
    List<GaiaScene> load(Path path);
}

