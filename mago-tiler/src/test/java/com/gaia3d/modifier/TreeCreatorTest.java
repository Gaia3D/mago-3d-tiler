package com.gaia3d.modifier;

import com.gaia3d.command.LoggingConfiguration;
import org.junit.jupiter.api.Test;

import java.io.File;

class TreeCreatorTest {

    static {
        LoggingConfiguration.initConsoleLogger();
    }

    @Test
    void createTreeBillBoard() {
        TreeBillBoardParameters treeBillBoardParameters = new TreeBillBoardParameters();
        treeBillBoardParameters.setVerticalRectanglesCount(2);
        treeBillBoardParameters.setHorizontalRectanglesCount(1);

        treeBillBoardParameters.setVerticalRectanglesCount(4);
        treeBillBoardParameters.setHorizontalRectanglesCount(3);

        String inputPath = "D:\\data\\korea-forest-service\\original.glb";
        String outputPath = "E:\\data\\mago-server\\output\\BillboardCreation\\";
        File outputDir = new File(outputPath);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new RuntimeException("Output directory creation failed");
        }

        TreeCreator treeCreator = new TreeCreator();
        treeCreator.createTreeBillBoard(treeBillBoardParameters, inputPath, outputPath);
    }
}