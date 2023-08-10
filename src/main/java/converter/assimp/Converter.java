package converter.assimp;

import basic.structure.GaiaScene;

import java.io.File;
import java.nio.file.Path;

/**
 * Interface for the converter.
 * @author znkim
 * @since 1.0.0
 * @see AssimpConverter
 */
public interface Converter {
    GaiaScene load(String path);
    GaiaScene load(File file);
    GaiaScene load(Path path);
}
