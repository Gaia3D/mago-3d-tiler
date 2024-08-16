package com.gaia3d.converter.assimp;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.command.Configurator;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class AssimpConverterTest {

    @Test
    void load() {
        Configurator.initConsoleLogger();

        ClassLoader classLoader = getClass().getClassLoader();
        File inputFolder = new File(classLoader.getResource("./sample-3ds").getFile());
        File inputFile = new File(inputFolder, "a_bd001.3ds");

        AssimpConverter assimpConverter = new AssimpConverter();
        List<GaiaScene> gaiaScenes = assimpConverter.load(inputFile);
        GaiaScene gaiaScene = gaiaScenes.get(0);
        GaiaSet gaiaSet = GaiaSet.fromGaiaScene(gaiaScene);

        File outputPath = new File("D:/workbench");
        File outputFileA = new File(outputPath, "a.gaia");
        //writeObject(gaiaSet, outputFileA);

        //File outputFileB = new File(outputPath, "b.gaia");
        //Path path = Paths.get(outputFileB.getAbsolutePath());
        //gaiaSet.writeFile(path, 0, gaiaSet.getAttribute()) ;


        GaiaSet readA = readObject(outputFileA);
        //GaiaSet readB = GaiaSet.fromGaiaScene(path);

        log.debug("gaiaScenes : {}", gaiaSet);
        log.debug("gaiaScenes : {}", readA);

        log.debug(gaiaSet.equals(readA) + "");
    }

    void writeObject(GaiaSet gaiaSet, File outputFile) {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
            outputStream.writeObject(gaiaSet);
        } catch (Exception e) {
            log.error("Error : {}", e.getMessage());
        }
    }

    GaiaSet readObject(File outputFile) {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(outputFile))) {
            GaiaSet gaiaSet = (GaiaSet) inputStream.readObject();
            return gaiaSet;
        } catch (Exception e) {
            log.error("Error : {}", e.getMessage());
        }
        return null;
    }
}