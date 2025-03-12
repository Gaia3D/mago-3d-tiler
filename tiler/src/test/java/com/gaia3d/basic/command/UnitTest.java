package com.gaia3d.basic.command;

import com.gaia3d.TilerExtensionModule;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.Configurator;
import com.gaia3d.command.mago.Mago3DTilerMain;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Deprecated
@Slf4j
class UnitTest {
        private static final String INPUT_PATH = "D:\\data\\unit-test\\";
        private static final String OUTPUT_PATH = "D:\\Result_mago3dTiler\\";

        @Test
        void sample() {
            Vector3d a = GlobeUtils.geographicToCartesianWgs84(new Vector3d(126.980125, 37.521169, 0));
            Vector3d b = GlobeUtils.cartesianToGeographicWgs84(new Vector3d(-3124798.1188322213,4127332.474398493,3713347.7041680403));
            Vector3d c = GlobeUtils.cartesianToGeographicWgs84(new Vector3d(-3075909.5, 4414039.0, 3414263.0));

            CRSFactory factory = new CRSFactory();
            CoordinateReferenceSystem crs = factory.createFromName("EPSG:5186");
            CoordinateReferenceSystem wgs84 = factory.createFromParameters("WGS84", "+proj=longlat +datum=WGS84 +no_defs");
            BasicCoordinateTransform transformer = new BasicCoordinateTransform(wgs84, crs);

            ProjCoordinate coordinate = new ProjCoordinate(b.x, b.y, b.z);
            ProjCoordinate transformedCoordinate = transformer.transform(coordinate, new ProjCoordinate());
        }


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
        String inputPath = "D:\\data\\unit-test\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "kml",
                "-o", outputPath + path,
                "-autoUpAxis",
                "-glb"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testHanamKyosan3ds() {
        String path = "Data_HanamKyosan\\건물";
        String inputPath = "D:\\data\\unit-test\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "3ds",
                "-o", outputPath + path,
                "-autoUpAxis",
                "-crs", "5174",
                "-minLod", "0", // test
                "-maxLod", "0", // test
                "-debug"
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
                "-crs", "5174"
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
                "-crs", "5186"
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
                "-crs", "5186"
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
                    "-autoUpAxis"
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
    void setUnderground() {
        String path = "case29-shp-wtl-pipe";
        String inputPath = "D:\\Mago3DTiler-UnitTest\\input\\";
        String outputPath = "C:\\Workspaces\\GitSources\\mago-viewer\\data\\tilesets\\";
        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "shp",
                "-o", OUTPUT_PATH + path,
                "-proj", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43",
        };
        Mago3DTilerMain.main(args);
    }


    @Test
    void testCityGml_SejongCity() {
        String path = "sejeong_citygml";
        String inputPath = "D:\\3D_DATA\\PROJECTS\\SejongCityGML_ParkJinWoo_20191101\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "gml",
                "-o", outputPath + path,
                "-crs", "3857",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test3ds_SejongCity() {
        String path = "SejongBuildings_3ds";
        String inputPath = "D:\\3D_DATA\\PROJECTS\\SejongCity\\SejongBuildings.vol1\\3ds\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "3ds",
                "-o", outputPath + path,
                "-crs", "5186",
        };
        Mago3DTilerMain.main(args);
    }

        @Test
        void testShape() {
            String inputPath  = "D:\\data\\unit-test\\";
            String outputPath = "D:\\Result_mago3dTiler\\";
            String path = "shape";
            String[] args = new String[]{
                    "-i", inputPath + path,
                    "-it", "shp",
                    "-o", outputPath + path,
                    "-proj", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43",
                    "-maxCount", "4096",
                    "-multiThread",
                    "-refineAdd",
                    "-d"
            };
            Mago3DTilerMain.main(args);
        }

    @Test
    void testShapePipe() {
        String path = "WTL_PIPE_LM-4dep.shp";
        String inputPath = "D:\\data\\unit-test\\2. 17년도 성과\\01.상수\\01. 상수관로_3DS\\shp\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "shp",
                "-o", outputPath + path,
                "-crs", "5186",
                "-maxCount", "4096",
                "-multiThreadCount", "1",
                "-refineAdd",
                "-glb",
                "-d"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testShapePipeUnderground() {
        String path = "SWL_PIPE_LM-4dep.shp";
        String inputPath = "D:\\data\\unit-test\\2. 17년도 성과\\02.하수\\01. 하수관로_3DS\\shp\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "shp",
                "-o", outputPath + path,
                "-crs", "5186",
                "-maxCount", "4096",
                "-multiThreadCount", "1",
                "-refineAdd",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testShapeCommunicationCables() {
        String path = "UFL_KPIP_LS-4dep.shp";
        String inputPath = "D:\\data\\unit-test\\2. 17년도 성과\\03.통신\\01. 통신선로_3DS\\shp\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "shp",
                "-o", outputPath + path,
                "-crs", "5186",
                "-maxCount", "4096",
                "-multiThreadCount", "1",
                "-refineAdd",
        };
        Mago3DTilerMain.main(args);
    }
    @Test
    void testShapeBasura() {
        String path = "RBL_PIPE_LM-4dep.shp";
        String inputPath = "D:\\data\\unit-test\\2. 17년도 성과\\04.쓰레기\\01. 쓰레기수송관로_3DS\\shp\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "shp",
                "-o", outputPath + path,
                "-crs", "5186",
                "-maxCount", "4096",
                "-multiThreadCount", "1",
                "-refineAdd",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testShapeCalefaction() {
        String path = "UFL_HPIP_LM-4dep.shp";
        String inputPath = "D:\\data\\unit-test\\2. 17년도 성과\\05.난방\\01. 열배관_3DS\\shp\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "shp",
                "-o", outputPath + path,
                "-crs", "5186",
                "-maxCount", "4096",
                "-multiThreadCount", "1",
                "-refineAdd",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testShapePipeLinesIndia() {
        String path = "water_pipelines.shp";
        String inputPath = "D:\\data\\GitHub_Issues_data\\water_pipelines\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "shp",
                "-o", outputPath + path,
                "-crs", "32643",
                "-maxCount", "4096",
                "-multiThreadCount", "1",
                "-refineAdd",
                "-debug",
                "-glb"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testShapePipeLinesIndia_10() {
        String path = "10_z.shp";
        String inputPath = "D:\\data\\GitHub_Issues_data\\10_z\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "shp",
                "-o", outputPath + path,
                "-crs", "32643",
                "-maxCount", "4096",
                "-multiThreadCount", "1",
                "-refineAdd",
                "-debug",
                "-glb"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testShapePipeLinesIndia_2() {
        String path = "selected_water_line.shp";
        String inputPath = "D:\\data\\GitHub_Issues_data\\selected_water_line\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "shp",
                "-o", outputPath + path,
                "-crs", "32643",
                "-maxCount", "4096",
                "-multiThreadCount", "1",
                "-refineAdd",
                "-debug",
                "-glb"
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

    @Test
    void testFbxHayashiSan() {
        String inputPath = "D:\\data\\mago3dtiler_TESTDATA\\workspace\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String path = "fbx";

        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "fbx",
                "-o", outputPath + path,
                "-crs", "6674",
                "-minLod", "0",
                "-maxLod", "0",
                "-multiThreadCount", "1",
                "-largeMesh",
                "-glb",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void testGoyangChangRung() {
        String inputPath = "D:\\data\\mago3dtiler_TESTDATA\\고양창릉데이터\\abs\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String path = "bridge";

        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "3ds",
                "-o", outputPath + path,
                "-crs", "5186",
                "-multiThreadCount", "1",
                "-rotateUpAxis",
                //"-largeMesh",
                "-debug",
                "-glb"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test_CityGML_RailWayGML() {
        String inputPath = "D:\\data\\mago3dtiler_TESTDATA\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String path = "RailWay";

        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "gml",
                "-o", outputPath + path,
                "-crs", "3857",
                "-multiThreadCount", "1",
                "-rotateUpAxis",
                //"-largeMesh",
                "-debug",
                "-glb"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test_CityGML_RailWayWithTextures() {
            // No tested yet
        String inputPath = "D:\\data\\CityGML\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String path = "RailWayWithTextures";

        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "gml",
                "-o", outputPath + path,
                "-crs", "5186",
                "-multiThreadCount", "1",
                "-rotateUpAxis",
                //"-largeMesh",
                "-debug",
                "-glb"
        };
        Mago3DTilerMain.main(args);
    }

    // -input "/input_path/i3dm"
    // -output "/output_path/i3dm"
    // -inputType "shp"
    // -outputType "i3dm"
    // -instance "/input_path/instance.gltf"
    @Test
    void test_I3dm_GoyangChangRung_trees() {
        String inputPath = "D:\\data\\mago3dtiler_TESTDATA\\고양창릉데이터\\abs\\tree_shp\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String path = "tree";

        String[] args = new String[]{
                "-i", inputPath,
                "-it", "shp",
                "-o", outputPath + path,
                "-crs", "5186",
                "-outputType", "i3dm",
                "-instance", inputPath + "tree-map.3ds",
                "-vl", "true"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test_I3dm_IncheonSemaphore() {
        String inputPath = "D:\\data\\mago3dtiler_TESTDATA\\incheonGyeyang_semaphore\\incheonSemaphore_shp\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String path = "incheonSemaphore_shp";

        String[] args = new String[]{
                "-i", inputPath,
                "-it", "shp",
                "-o", outputPath + path,
                "-crs", "5186",
                "-outputType", "i3dm",
                "-instance", inputPath + "lamp001.3ds",
                "-vl", "true"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test_geoJson_yeonHwa() {
        String inputPath = "D:\\data\\issues_data\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String path = "ThailandGeoJsonData";

        String[] args = new String[]{
                "-i", inputPath,
                "-it", "geojson",
                "-o", outputPath + path,
                "-crs", "4326",
                "-outputType", "b3dm",
                "-debug",
                "-glb",
                "-mh", "3.3"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test_geoJson_seoul() {
        // **********************************************************************************************************************************************************************************
        // crs 5174 = +proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43
        // **********************************************************************************************************************************************************************************
        String inputPath = "D:\\data\\issues_data\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String path = "allSeoulGeoJson";

        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "geojson",
                "-o", outputPath + path,
                "-proj", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43",
                "-outputType", "b3dm",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test_geoJson_wangsuk() {
        String inputPath = "D:\\data\\issues_data\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String path = "wangsukGeoJson";

        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "geojson",
                "-o", outputPath + path,
                "-crs", "4326",
                "-outputType", "b3dm",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test_IndiaSchool() {
        String inputPath = "D:\\data\\issues_data\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        String path = "GLB_india_school";

        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "glb",
                "-o", outputPath + path,
                "-crs", "3857",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test_RealisticMesh_Thailand() {
        String inputPath = "D:\\data\\mago3dtiler_TESTDATA\\";
        String outputPath = "D:\\Result_mago3dTiler\\";
        //String path = "Tile_+000_+000_+000";
        //String path = "splittedTile";
        String path = "RealisticMesh_Thailand";

        String[] args = new String[]{
                "-i", inputPath + path,
                "-it", "fbx",
                "-o", outputPath + path,
                "-crs", "2096",
                "-glb",
                "-pr", // photo realistic mesh
                "-minLod", "0",
                "-maxLod", "0",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }


    @Test
    void test_RealisticMesh_Thailand_Data_N_buildings() {
        String inputPath = "D:\\data\\mago3dtiler_TESTDATA\\RealisticMesh_Thailand_multiTiles\\OBJ25sqkm\\Data_16buildings\\";
        String outputPath = "D:\\data\\mago-server\\output\\ResultData_16buildings\\";

        String[] args = new String[]{
                "-i", inputPath,
                "-it", "obj",
                "-o", outputPath,
                "-crs", "32648",
                "-xOffset", "268943",
                "-yOffset", "1818915",
                "-pr"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test_RealisticMesh_LeeDongHun_Data_N_buildings() {
        String inputPath = "D:\\data\\mago3dtiler_TESTDATA\\leeDongHun_Data\\obj\\BANSONG\\";
        String outputPath = "D:\\data\\mago-server\\output\\leeDongHun_Data_OBJ_BANSONG\\";

        String[] args = new String[]{
                "-i", inputPath,
                "-it", "obj",
                "-o", outputPath,
                "-crs", "5187",
                "-pr",
                "-rx", "90",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test_RealisticMesh_LeeDongHun_Data_BANSONG() {
        String inputPath = "D:\\data\\mago3dtiler_TESTDATA\\leeDongHun_Data\\obj\\BANSONG\\";
        String outputPath = "D:\\data\\mago-server\\output\\leeDongHun_Data_OBJ_BANSONG\\";

        String[] args = new String[]{
                "-i", inputPath,
                "-it", "obj",
                "-o", outputPath,
                "-crs", "5187",
                "-pr",
                "-rx", "90",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test_RealisticMesh_LeeDongHun_Data_BANSONG_someBuildings() {
        String inputPath = "D:\\data\\mago3dtiler_TESTDATA\\leeDongHun_Data\\obj\\BANSONG_someBuildings\\";
        String outputPath = "D:\\data\\mago-server\\output\\leeDongHun_Data_OBJ_BANSONG_someBuildings\\";

        String[] args = new String[]{
                "-i", inputPath,
                "-it", "obj",
                "-o", outputPath,
                "-crs", "5187",
                "-pr",
                "-rx", "90",
                "-leaveTemp"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test_RealisticMesh_LeeDongHun_Data_SANGCHEON() {
        String inputPath = "D:\\data\\mago3dtiler_TESTDATA\\leeDongHun_Data\\obj\\SANGCHEON\\";
        String outputPath = "D:\\data\\mago-server\\output\\leeDongHun_Data_OBJ_SANGCHEON\\";

        String[] args = new String[]{
                "-i", inputPath,
                "-it", "obj",
                "-o", outputPath,
                "-crs", "5187",
                "-rx", "90",
                "-pr"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test_RealisticMesh_LeeDongHun_Data_SANGCHEON_someBuildings() {
        String inputPath = "D:\\data\\mago3dtiler_TESTDATA\\leeDongHun_Data\\obj\\SANGCHEON_someBuildings\\";
        String outputPath = "D:\\data\\mago-server\\output\\leeDongHun_Data_OBJ_SANGCHEON_someBuildings\\";

        String[] args = new String[]{
                "-i", inputPath,
                "-it", "obj",
                "-o", outputPath,
                "-crs", "5187",
                "-pr",
                "-rx", "90",
        };
        Mago3DTilerMain.main(args);
    }



    @Test
    void testCollada_SangGiDe() {
            // _1building _2buildings _3buildings _4buildings _5buildings _6buildings _someBuildings
        String inputPath = "D:\\data\\mago3dtiler_TESTDATA\\BB00-sangji-university\\";
        String outputPath = "D:\\data\\mago-server\\output\\BB00-sangji-university\\";

        String[] args = new String[]{
                "-i", inputPath,
                "-it", "dae",
                "-o", outputPath,
                "-proj", "+proj=tmerc +x_0=0 +y_0=0 +ellps=WGS84 +datum=WGS84 +units=m +no_defs +lon_0=127.9296192 +lat_0=37.3702212",
                "-pr",
                "-rx", "90",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void test_changeBackGroundColor() {
            Configurator.initConsoleLogger();
        String inputPath = "D:\\data\\mago-server\\output\\pinkTest.png";
        String outputPath = "D:\\data\\mago-server\\output\\pinkTest_result.jpg";

        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(inputPath));
            BufferedImage newImage = ImageUtils.clampBackGroundColor(image, new Color(255, 0, 255), 1, 50);
            ImageIO.write(newImage, "jpg", new File(outputPath));
            log.info("newImage: " + newImage);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
