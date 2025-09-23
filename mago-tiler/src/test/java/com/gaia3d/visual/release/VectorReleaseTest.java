package com.gaia3d.visual.release;

import com.gaia3d.visual.MagoTestConfig;
import org.junit.jupiter.api.Test;

public class VectorReleaseTest {

    @Test
    void shape() {
        testByFormat("shp");
    }

    @Test
    void geojson() {
        testByFormat("geojson");
    }

    @Test
    void geopackage() {
        testByFormat("gpkg");
    }

    private void testByFormat(String format) {
        String[] epsg = new String[] {
                //"4326",
                "3857",
                "5179",
                "5186",
                //"32652"
        };

        for (String code : epsg) {
            String path = "vector-release-sample/EPSG" + code;
            String[] args = new String[] {
                    "-i", MagoTestConfig.getInputPath(path).getAbsolutePath(),
                    "-o", MagoTestConfig.getOutputPath(path + format).getAbsolutePath(),
                    "-c", code,
                    "-it", format
            };
            MagoTestConfig.execute(args);
        }
    }
}
