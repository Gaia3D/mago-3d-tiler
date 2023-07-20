package converter;

import geometry.structure.GaiaScene;

import java.io.File;
import java.nio.file.Path;

public interface Converter {
    GaiaScene load(String path);
    GaiaScene load(File file);
    GaiaScene load(Path path);
}
