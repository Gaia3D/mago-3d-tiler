package com.gaia3d.basic.command;

import com.gaia3d.command.TilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class LasTest {
    private static final String input = "D:\\temp\\las\\";
    private static final String output = "D:\\temp\\las\\output\\";

    @Test
    void convertShp1() {
        String suffix = "sample";
        convert(suffix);
    }

    void convert(String suffix) {
        String[] args = new String[]{
                "-input", input + suffix,
                "-inputType", "las",
                "-output", output + suffix,
                "-recursive",
                "-maxCount", "1024",
                "-minLod", "0",
                "-maxLod", "3",
                "-proj", "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs",
        };
        TilerMain.main(args);
    }
}