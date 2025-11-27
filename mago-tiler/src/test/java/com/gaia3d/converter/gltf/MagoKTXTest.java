package com.gaia3d.converter.gltf;

import com.gaia3d.command.LoggingConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class MagoKTXTest {

    @Test
    void loadPNGImageFromBufferedImage() {
        LoggingConfiguration.initConsoleLogger();

        String input = "D:\\workspace\\input\\pizza.jpg";
        String output = "D:\\workspace\\input\\pizza.ktx";

        MagoKTX magoKTX = new MagoKTX();
        magoKTX.start(input, output);

    }
}