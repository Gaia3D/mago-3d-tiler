package com.gaia3d.basic.command;

import com.gaia3d.command.TilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class SampleTest {
        private static final String INPUT_PATH = "D:\\sample\\input\\";
        private static final String OUTPUT_PATH = "D:\\sample\\output\\";

        @Test
        void a() {
            String path = "gltf";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "kml",
                    "-o", OUTPUT_PATH + path,
                    //"-crs", "32652",
                    //"-swapYZ",
                    "-zeroOrigin",
                    "-maxCount", "1024",
                    "-glb",
                    //"-proj", "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs",
                    //"-multiThread",
            };
            TilerMain.main(args);
        }

    @Test
    void b() {
        String path = "glb";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "kml",
                "-o", OUTPUT_PATH + path,
                //"-crs", "32652",
                //"-swapYZ",
                "-autoUpAxis",
                "-zeroOrigin",
                "-maxCount", "1024",
                "-glb",
                //"-debug"
                //"-proj", "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs",
                //"-multiThread",
        };
        TilerMain.main(args);
    }
}
