package assimp;

import geometry.structure.GaiaScene;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.net.URISyntaxException;

import static org.junit.jupiter.api.Assertions.*;

class AssimpConverterTest {

    private static final String INPUT_PATH = "../sample/";

    @Test
    void load() throws URISyntaxException {
        AssimpConverter converter = new AssimpConverter(null);
        GaiaScene scene = converter.load(getAbsolutePath(INPUT_PATH) + "a_bd001.3ds", "3ds");
        assertNotNull(scene);
    }

    private String getAbsolutePath(String classPath) throws URISyntaxException {
        File file = new File(getClass().getResource(classPath).toURI());
        assert(file != null);
        return file.getAbsolutePath() + File.separator;
    }
}