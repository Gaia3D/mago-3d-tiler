package com.gaia3d.basic.command;

import com.gaia3d.command.TilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class UnitTest {
        private static final String INPUT_PATH = "D:\\data\\unit-test\\";
        private static final String OUTPUT_PATH = "D:\\Result_mago3dTiler\\";

        @Test
        void test() {
            String[] args = new String[]{
                "-help"
            };
            TilerMain.main(args);
        }

    @Test
    void kmlComplicatedModels() {
        String path = "ComplicatedModels10";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "kml",
                "-o", OUTPUT_PATH + path,
                "-autoUpAxis",
                "-glb"
        };
        TilerMain.main(args);
    }

        @Test
        void kmlObj() {
            String path = "kml-obj";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "kml",
                    "-o", OUTPUT_PATH + path,
                    "-autoUpAxis",
            };
            TilerMain.main(args);
        }

        @Test
        void kmlFbx() {
            String path = "kml-fbx";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "kml",
                    "-o", OUTPUT_PATH + path,
                    "-autoUpAxis",
            };
            TilerMain.main(args);
        }

        @Test
        void kmlCollada() {
            String path = "kml-collada";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "kml",
                    "-o", OUTPUT_PATH + path,
                    "-autoUpAxis",
            };
            TilerMain.main(args);
        }

    @Test
    void kmlGltf() {
        String path = "kml-gltf";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "kml",
                "-o", OUTPUT_PATH + path,
                "-yUpAxis",
                "-refineAdd",
                "-multiThread",
                "-zeroOrigin",
        };
        TilerMain.main(args);
    }

    //@Test
    void kmlIfc() {
        String path = "kml-ifc";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "kml",
                "-o", OUTPUT_PATH + path,
                "-autoUpAxis",
        };
        TilerMain.main(args);
    }

    @Test
    void kml3ds() {
        String path = "kml-3ds";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "kml",
                "-o", OUTPUT_PATH + path,
                "-autoUpAxis",
                "-multiThread",
        };
        TilerMain.main(args);
    }

    //@Test
    @Test
    void kmlJeon() {
        String path = "kml-jeon";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "kml",
                "-o", OUTPUT_PATH + path,
                "-yUpAxis",
                "-refineAdd",
                "-multiThread",
        };
        TilerMain.main(args);
    }

        @Test
        void test3ds() {
            String path = "3ds";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "3ds",
                    "-o", OUTPUT_PATH + path,
                    "-crs", "5186",
                    "-autoUpAxis",
                    //"-gt", "D:/forTest/ws2_dem.tif",
                    "-multiThread",
            };
            TilerMain.main(args);
        }

        @Test
        void testKmlWith3ds() {
            String path = "kml-with-3ds";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "kml",
                    "-o", OUTPUT_PATH + path,
                    "-crs", "4326",
                    "-autoUpAxis",
                    "-maxCount", "1024",
                    "-minLod", "0",
                    "-maxLod", "3",
                    //"-reverseTexCoord",
                    "-multiThread",
            };
            TilerMain.main(args);
        }

        //@Test
        void testIfcWithKml() {
            String path = "ifc";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "kml",
                    "-o", OUTPUT_PATH + path,
                    "-crs", "4326",
                    "-autoUpAxis",
                    "-maxCount", "1024",
                    "-minLod", "0",
                    "-maxLod", "3",
                    "-multiThread",
            };
            TilerMain.main(args);
        }

        @Test
        void testCollada() {
            String path = "collada";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "kml",
                    "-o", OUTPUT_PATH + path,
                    "-crs", "4326",
                    "-autoUpAxis",
                    "-maxCount", "1024",
                    "-minLod", "0",
                    "-maxLod", "3",
                    "-multiThread",
            };
            TilerMain.main(args);
        }

        @Test
        void testKml() {
            String path = "kml";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "kml",
                    "-o", OUTPUT_PATH + path,
                    "-crs", "4326",
                    "-autoUpAxis",
                    "-maxCount", "1024",
                    "-minLod", "0",
                    "-maxLod", "3",
                    //"-multiThread",
            };
            TilerMain.main(args);
        }

        @Test
        void testCityGml() {
            String path = "citygml";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "gml",
                    "-o", OUTPUT_PATH + path,
                    "-crs", "4326",
                    "-flipCoordinate",
                    "-maxCount", "1024",
                    "-multiThread",
                    "-refineAdd",
            };
            TilerMain.main(args);
        }

        @Test
        void testShape() {
            String path = "shape";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "shp",
                    "-o", OUTPUT_PATH + path,
                    "-proj", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43",
                    "-maxCount", "4096",
                    "-multiThread",
                    "-refineAdd",
            };
            TilerMain.main(args);
        }

        @Test
        void testGeojson() {
            String path = "geojson";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "geojson",
                    "-o", OUTPUT_PATH + path,
                    "-crs", "5186",
                    //"-maxCount", "1024",
                    //"-debug",
                    "-gt", "D:/forTest/ws2_dem.tif",
                    "-refineAdd",
            };
            TilerMain.main(args);
        }

        @Test
        void testLas() {
            String path = "las";
            String[] args = new String[]{
                    "-i", INPUT_PATH + path,
                    "-it", "las",
                    "-o", OUTPUT_PATH + path,
                    "-maxCount", "1024",
                    "-multiThread",
                    "-proj", "+proj=utm +zone=52 +datum=WGS84 +units=m +no_defs",
            };
            TilerMain.main(args);
        }
}
