package com.gaia3d.basic.command;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
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
            Mago3DTilerMain.main(args);
        }

    @Test
    void kmlComplicatedModels() {
        String path = "ComplicatedModels10";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "kml",
                "-o", OUTPUT_PATH + path,
                "-autoUpAxis",
                //"-glb",
                "-mc", "1" // multithreadsCount.
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testHanamKyosan3ds() {
        String path = "Data_HanamKyosan\\건물";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "3ds",
                "-o", OUTPUT_PATH + path,
                "-autoUpAxis",
                "-crs", "5174",
                "-mc", "4" // multithreadsCount.
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testHanamKyosanRoads3ds() {
        String path = "Data_HanamKyosan\\교량";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "3ds",
                "-o", OUTPUT_PATH + path,
                "-autoUpAxis",
                "-crs", "5174"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testLH_gs3ds() {
        String path = "LH_Data\\gs-3ds";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "3ds",
                "-o", OUTPUT_PATH + path,
                "-autoUpAxis",
                "-crs", "5174",
                "-mc", "1"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testLH_icgy3ds() {
        String path = "LH_Data\\icgy-3ds";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "3ds",
                "-o", OUTPUT_PATH + path,
                "-autoUpAxis",
                "-crs", "5186",
                "-mc", "1"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testLH_ws13ds() {
        String path = "LH_Data\\ws1-3ds";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "3ds",
                "-o", OUTPUT_PATH + path,
                "-autoUpAxis",
                "-crs", "5186",
                "-mc", "1"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testLH_ws23ds() {
        String path = "LH_Data\\ws2-3ds";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "3ds",
                "-o", OUTPUT_PATH + path,
                "-autoUpAxis",
                "-crs", "5186",
                "-mc", "1"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testPoleSouthSejongBase() {
            // "+proj=tmerc +x_0=0 +y_0=0 +ellps=WGS84 +datum=WGS84 +units=m +no_defs +lon_0=-58.789189 +lat_0=-62.223259" // pole south
        // "+proj=tmerc +x_0=0 +y_0=0 +ellps=WGS84 +datum=WGS84 +units=m +no_defs +lon_0=126.980125 +lat_0=37.521169" // seoul
        String path = "PoleSouthIFC_5";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "ifc",
                "-o", OUTPUT_PATH + path,
                "-proj", "+proj=tmerc +x_0=0 +y_0=0 +ellps=WGS84 +datum=WGS84 +units=m +no_defs +lon_0=-58.789189 +lat_0=-62.223259",
                "-refineAdd",
                "-glb",
                "-mc", "1",
                "-r"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testIFC_DuplexApartment() {
        // "+proj=tmerc +x_0=0 +y_0=0 +ellps=WGS84 +datum=WGS84 +units=m +no_defs +lon_0=126.980125 +lat_0=37.521169" // seoul
        // Duplex Apartment
        // Medical-Dental Clinic
        String path = "IFC 2x3\\Duplex Apartment";
        String[] args = new String[]{
                "-i", INPUT_PATH + path,
                "-it", "ifc",
                "-o", OUTPUT_PATH + path,
                "-proj", "+proj=tmerc +x_0=0 +y_0=0 +ellps=WGS84 +datum=WGS84 +units=m +no_defs +lon_0=126.980125 +lat_0=37.521169",
                "-refineAdd",
                "-glb",
                "-mc", "1",
                "-r"
        };
        Mago3DTilerMain.main(args);
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
            Mago3DTilerMain.main(args);
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
            Mago3DTilerMain.main(args);
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
            Mago3DTilerMain.main(args);
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
                //"-multiThread",
                "-zeroOrigin",
        };
        Mago3DTilerMain.main(args);
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
        Mago3DTilerMain.main(args);
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
        Mago3DTilerMain.main(args);
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
        Mago3DTilerMain.main(args);
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
            Mago3DTilerMain.main(args);
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
            Mago3DTilerMain.main(args);
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
            Mago3DTilerMain.main(args);
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
                    //"-multiThread",
            };
            Mago3DTilerMain.main(args);
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
            Mago3DTilerMain.main(args);
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
            Mago3DTilerMain.main(args);
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
            Mago3DTilerMain.main(args);
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
            Mago3DTilerMain.main(args);
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
            Mago3DTilerMain.main(args);
        }
}
