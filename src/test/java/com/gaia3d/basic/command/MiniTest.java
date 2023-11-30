package com.gaia3d.basic.command;

import com.gaia3d.command.TilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class MiniTest {
        private static final String INPUT_PATH = "D:\\forTest\\";
        private static final String OUTPUT_PATH = "D:\\forTest\\output\\";

        @Test
        void testBuilding_sample() {
            String path = "building_sample";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "shp",
                    "-o", OUTPUT_PATH + path,
                    "-crs", "5181",
                    //"-proj", "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs",
                    //"-swapYZ",
                    "-maxCount", "16384",
                    "-refineAdd",
                    "-minimumHeight", "3",
                    "-heightColumn", "HEIGHT",
                    //"-glb",
                    "-multiThread",
            };
            TilerMain.main(args);
        }
        
        @Test
        void testBuilding() {
            String path = "building";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "shp",
                    "-o", OUTPUT_PATH + path,
                    "-crs", "5181",
                    //"-proj", "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs",
                    //"-swapYZ",
                    "-maxCount", "16384",
                    "-refineAdd",
                    "-minimumHeight", "3",
                    "-heightColumn", "HEIGHT",
                    //"-glb",
                    "-multiThread",
            };
            TilerMain.main(args);
        }

        @Test
        void testSeoul() {
            String path = "seoul";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "shp",
                    "-o", OUTPUT_PATH + path,
                    //"-crs", "5174",
                    "-proj", "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs",
                    //"-swapYZ",
                    "-maxCount", "8192",
                    "-refineAdd",
                    "-minimumHeight", "3",
                    "-heightColumn", "HEIGHT",
                    //"-glb",
                    "-multiThread",
            };
            TilerMain.main(args);
        }

    @Test
        void testIncheon() {
            String path = "incheon";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "shp",
                    "-o", OUTPUT_PATH + path,
                    //"-crs", "5174",
                    "-proj", "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs",
                    //"-swapYZ",
                    "-maxCount", "8192",
                    "-refineAdd",
                    "-minimumHeight", "3",
                    "-heightColumn", "HEIGHT",
                    //"-glb",
                    "-multiThread",
            };
            TilerMain.main(args);
        }

        @Test
        void testGyeonggi() {
            String path = "gyeonggi";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "shp",
                    "-o", OUTPUT_PATH + path,
                    //"-crs", "5174",
                    "-proj", "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=500000 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs",
                    //"-swapYZ",
                    "-maxCount", "8192",
                    "-refineAdd",
                    "-minimumHeight", "3",
                    "-heightColumn", "HEIGHT",
                    //"-glb",
                    "-multiThread",
            };
            TilerMain.main(args);
        }


}
