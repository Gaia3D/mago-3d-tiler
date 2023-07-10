package tiler;

import command.Configurator;
import geometry.types.FormatType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;

@Slf4j
class Gaia3DTilerTest {
    private static final String INPUT_PATH = "../sample-external/";
    private static final String OUTPUT_PATH = "../output/";

    @Test
    void createRoot() throws URISyntaxException {
        Configurator.initLogger();

        File input = new File(getAbsolutePath(INPUT_PATH) + "3d-tiles-ws2");
        File output = new File(getAbsolutePath(OUTPUT_PATH) + "3d-tiles-ws2");
        output.mkdir();
        Path inputPath = input.toPath();
        Path outputPath = output.toPath();

        CRSFactory factory = new CRSFactory();
        CoordinateReferenceSystem source = factory.createFromName("EPSG:5186");
        Gaia3DTiler tiler = new Gaia3DTiler(inputPath, outputPath, FormatType.MAX_3DS,source, null);
        tiler.execute();
    }

    private String getAbsolutePath(String classPath) throws URISyntaxException {
        File file = new File(getClass().getResource(classPath).toURI());
        return file.getAbsolutePath() + File.separator;
    }
}