package com.gaia3d.basic.command;

import com.gaia3d.command.TilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

@Slf4j
class TempTest {
    private static final String input = "F:\\temp\\";
    private static final String output = "F:\\temp\\output\\";

    /*@Test
    void convertKorea() {
        String suffix = "south_korea";
        convert(suffix);
    }*/

    @Test
    void convertShp1() {
        String suffix = "busan";
        convert(suffix);
    }

    @Test
    void convertShp2() {
        String suffix = "deagu";
        convert(suffix);
    }

    @Test
    void convertShp3() {
        String suffix = "gwangju";
        convert(suffix);
    }



    void convert(String suffix) {
        String[] args = new String[]{
                "-input", input + suffix,
                "-inputType", "shp",
                "-output", output + suffix,
                "-recursive",
                "-maxCount", "16384",
                "-minLod", "0",
                "-maxLod", "3",
                "-proj", "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 +x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs +towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43",
                "-multiThread",
                "-refineAdd",
                "-crs", "5174",
                "-gltf",
                "-debug",
                "-heightColumn", "HEIGHT"
        };
        TilerMain.main(args);
    }
}