package com.gaia3d.converter.assimp;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.LoggingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Level;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.lwjgl.assimp.AIScene;
import org.lwjgl.assimp.Assimp;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("ALL")
@Slf4j
class AssimpConverterTest {

    @Test
    void loadTest() {
        File file = new File("D:\\data\\mago-3d-tiler\\temp-sample\\20250827-ld-IFC-extract\\val.ifc");
        AIScene aiScene = Assimp.aiImportFile(file.getAbsolutePath(), 0);
        assert aiScene != null;
    }

    /**
     * Stress test for loading 3D models.
     */
    @Test
    @Disabled
    void loadStressTest() {
        LoggingConfiguration.initConsoleLogger("%d{HH:mm:ss.SSS} [%t] %-5level %logger{36} - %msg%n");
        LoggingConfiguration.setLevel(Level.DEBUG);

        log.debug("Start loading files");

        AssimpConverterOptions options = AssimpConverterOptions.builder()
                .build();
        AssimpConverter assimpConverter = new AssimpConverter(options);
        File inputFolder = new File("D:\\data\\mago-tiler-data\\input\\seoul-set\\");
        List<File> files = (List<File>) FileUtils.listFiles(inputFolder, new String[]{"3ds", "3DS"}, true);
        int size = files.size();
        int count = 0;

        List<GaiaScene> gaiaAllScenes = new ArrayList<>();
        for (File file : files) {
            log.debug("[{}/{}] Start loading file : {}", count, size, file.getName());
            List<GaiaScene> gaiaScenes = assimpConverter.load(file);
            //gaiaAllScenes.addAll(gaiaScenes);
            gaiaScenes = null;
            log.debug("[{}/{}] End loading file : {}", count, size, file.getName());
            count++;
        }
        log.debug("End loading files");
    }


    @Test
    void load() {
        LoggingConfiguration.initConsoleLogger();
        ClassLoader classLoader = getClass().getClassLoader();
        File inputFolder = new File(classLoader.getResource("./sample-3ds").getFile());
        File inputFile = new File(inputFolder, "a_bd001.3ds");

        AssimpConverterOptions options = AssimpConverterOptions.builder()
                .build();
        AssimpConverter assimpConverter = new AssimpConverter(options);
        List<GaiaScene> gaiaScenes = assimpConverter.load(inputFile);
        GaiaScene gaiaScene = gaiaScenes.get(0);
        GaiaSet gaiaSet = GaiaSet.fromGaiaScene(gaiaScene);

        File outputPath = new File("D:/workbench");
        File outputFileA = new File(outputPath, "a.gaia");
        GaiaSet readA = readObject(outputFileA);

        log.debug("gaiaScenes : {}", gaiaSet);
        log.debug("gaiaScenes : {}", readA);
        log.debug("{}", gaiaSet.equals(readA));
    }

    private void writeObject(GaiaSet gaiaSet, File outputFile) {
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new FileOutputStream(outputFile))) {
            outputStream.writeObject(gaiaSet);
            outputStream.flush();
            outputStream.close();
        } catch (Exception e) {
            log.error("[ERROR] :", e);
        }
    }


    private GaiaSet readObject(File outputFile) {
        try (ObjectInputStream inputStream = new ObjectInputStream(new FileInputStream(outputFile))) {
            return (GaiaSet) inputStream.readObject();
        } catch (Exception e) {
            log.error("[ERROR] :", e);
        }
        return null;
    }
}