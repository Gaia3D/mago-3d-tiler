
package com.gaia3d.modifier;

import com.gaia3d.command.Configuration;
import org.junit.jupiter.api.Test;

class TreeCreatorTest {

    static {
        Configuration.initConsoleLogger();
    }

    @Test
    void createTreeBillBoard() {
        TreeBillBoardParameters treeBillBoardParameters = new TreeBillBoardParameters();
        //treeBillBoardParameters.setVerticalRectanglesCount(2);
        //treeBillBoardParameters.setHorizontalRectanglesCount(4);

        //treeBillBoardParameters.setVerticalRectanglesCount(4);
        //treeBillBoardParameters.setHorizontalRectanglesCount(4);

        treeBillBoardParameters.setVerticalRectanglesCount(2);
        treeBillBoardParameters.setHorizontalRectanglesCount(8);

        String inputPath = "D:\\data\\korea-forest-service\\original.glb";
        String outputPath = "E:\\data\\mago-server\\output\\korea-forest-service\\";
        TreeCreator treeCreator = new TreeCreator();
        treeCreator.createTreeBillBoard(treeBillBoardParameters, inputPath, outputPath);
    }
}