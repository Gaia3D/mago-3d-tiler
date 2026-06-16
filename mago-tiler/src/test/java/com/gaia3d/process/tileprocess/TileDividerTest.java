package com.gaia3d.process.tileprocess;

import com.gaia3d.command.LoggingConfiguration;
import com.gaia3d.command.mago.GlobalOptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@Tag("manual")
class TileDividerTest {

    static {
        LoggingConfiguration.initConsoleLogger();
    }

    @Test
    void divide() {
        String inputPath = "H:\\workspace\\mago-server\\output\\KimJinHun_babubabu\\Thailand_All\\tileset-copy.json";
        String outputPath = "H:\\workspace\\mago-server\\output\\KimJinHun_babubabu\\Thailand_All\\";

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setInputPath(inputPath);
        globalOptions.setOutputPath(outputPath);
        TileDivider tileDivider = new TileDivider();
        tileDivider.divide();
    }
}