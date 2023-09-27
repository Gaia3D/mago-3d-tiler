package com.gaia3d.basic.command;

import com.gaia3d.command.Configurator;
import com.gaia3d.converter.TriangleFileLoader;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class OptimizeTest {

    @Test
    void optTest() {
        String path = "F:\\workspace\\";
        String[] args = new String[]{
                "-i", path + "optimize",
                "-it", "kml",
                "-o", path +  "optimize-test",
                "-c", "",
                "-yz",
                "-mx", "65536",
                "-nl", "0",
                "-xl", "3",
                "-refineAdd",
                "-mt",
                //"-glb",
                //"-debug"
        };

        Configurator.initConsoleLogger();
        Options options = Configurator.createOptions();
        CommandLineParser parser = new DefaultParser();
        CommandLine command;
        try {
            command = parser.parse(options, args);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        //Runtime.getRuntime().gc();
        long usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println((usedMemory / 1000 / 1000) + " mega bytes");

        log.info("Start loading files.");
        TriangleFileLoader fileLoader = new TriangleFileLoader(command, new AssimpConverter(null));
        List<File> fileList = fileLoader.loadFiles();
        log.info("End loading files.");

        usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println((usedMemory / 1000 / 1000) + " mega bytes");

        List<TileInfo> tileInfos = new ArrayList<>();
        log.info("Start loading tile infos.");
        int count = 0;
        for (File file : fileList) {
            List<TileInfo> tileInfo = fileLoader.loadTileInfo(file);
            for (TileInfo info : tileInfo) {
                if (info != null) {
                    tileInfos.add(info);
                }
            }
            //log.info("loaded tile info: {}/{}", count++, fileList.size());
        }
        log.info("End loading tile infos.");

        usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println((usedMemory / 1000 / 1000) + " mega bytes");

        log.info("Start minimizing tile infos.");
        int serial = 0;
        for (TileInfo tileInfo : tileInfos) {
            tileInfo.minimize(serial++);
        }
        log.info("End minimizing tile infos.");

        usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println((usedMemory / 1000 / 1000) + " mega bytes");

        log.info("Start maximizing tile infos.");
        for (TileInfo tileInfo : tileInfos) {
            tileInfo.maximize();
        }
        log.info("End maximizing tile infos.");

        usedMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println((usedMemory / 1000 / 1000) + " mega bytes");
    }
}
