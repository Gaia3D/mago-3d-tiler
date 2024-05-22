package com.gaia3d.command.mago;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class Mago3DTilerMainTest {
    /*
        ┌┬┐┌─┐┌─┐┌─┐  -┐┌┬┐  ┌┬┐┬┬  ┌─┐┬─┐
        │││├─┤│ ┬│ │  -┤ ││   │ ││  ├┤ ├┬┘
        ┴ ┴┴ ┴└─┘└─┘  -┘-┴┘   ┴ ┴┴─┘└─┘┴└─
        3d-tiler(dev-version) by Gaia3D, Inc.
        ----------------------------------------
        usage: Gaia3D Tiler
         -aa,--absoluteAltitude <arg>   Absolute altitude value for extrusion
                                        model
         -ac,--altitudeColumn <arg>     Altitude Column setting for extrusion
                                        model (Default: altitude)
         -c,--crs <arg>                 Coordinate Reference Systems, EPSG
                                        Code(4326, 3857, 32652, 5186...)
         -d,--debug                     More detailed log output and stops on
                                        Multi-Thread bugs.
         -fc,--flipCoordinate           Flip x, y Coordinate (Default: false)
         -glb,--glb                     Create glb file with B3DM.
         -h,--help                      Print Gelp
         -hc,--heightColumn <arg>       Height column setting for extrusion model
                                        (Default: height)
         -i,--input <arg>               Input directory path
         -if,--instance <arg>           Instance file path for I3DM (Default:
                                        {OUTPUT}/instance.dae)
         -igtx,--ignoreTextures         Ignore diffuse textures.
         -it,--inputType <arg>          Input files type (kml, 3ds, fbx, obj,
                                        gltf, glb, las, laz, citygml, indoorgml,
                                        shp, geojson)(Default: kml)
         -l,--log <arg>                 Output log file path.
         -mc,--multiThreadCount <arg>   Multi-Thread count (Default: 4)
         -mh,--minimumHeight <arg>      Minimum height value for extrusion model
                                        (Default: 1.0)
         -mp,--maxPoints <arg>          Limiting the maximum number of points in
                                        point cloud data. (Default: 65536)
         -mx,--maxCount <arg>           Maximum number of triangles per node.
         -nc,--nameColumn <arg>         Name column setting for extrusion model
                                        (Default: name)
         -nl,--minLod <arg>             min level of detail (Default: 0)
         -o,--output <arg>              Output directory file path
         -ot,--outputType <arg>         Output 3DTiles Type (b3dm, i3dm,
                                        pnts)(Default : b3dm)
         -p,--proj <arg>                Proj4 parameters (ex: +proj=tmerc +la...)
         -pk,--pointSkip <arg>          Number of pointcloud omissions (ex:
                                        1/4)(Default: 4)
         -ps,--pointScale <arg>         Pointscloud geometryError scale setting
                                        (Default: 2)
         -q,--quiet                     Quiet mode/Silent mode
         -r,--recursive                 Tree directory deep navigation.
         -ra,--refineAdd                Set 3D Tiles Refine 'ADD' mode
         -sh,--skirtHeight <arg>        Building Skirt height setting for
                                        extrusion model (Default: 4.0)
         -te,--terrain <arg>            GeoTiff Terrain file path, 3D Object
                                        applied as clampToGround (Supports geotiff
                                        format)
         -v,--version                   Print Version Info
         -xl,--maxLod <arg>             Max Level of detail (Default: 3)
         -ya,--yUpAxis                  Assign 3D root transformed matrix Y-UP
                                        axis
         -zo,--zeroOrigin               [Experimental] fix 3d root transformed
                                        matrix origin to zero point.
     */
    @Test
    void help() {
        String args[] = {
                "-help",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void version() {
        String args[] = {
                "-version",
                "-help",
        };
        Mago3DTilerMain.main(args);
    }
    @Test
    void debug() {
        String args[] = {
                "-version",
                "-help",
        };
        Mago3DTilerMain.main(args);
    }
    @Test
    void noInput() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("sample-empty").getFile());
        String args[] = {
                "-outputPath", file.getAbsolutePath(),
        };

        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            log.debug(e.getMessage());
        }
    }
    @Test
    void noOutput() {
        ClassLoader classLoader = getClass().getClassLoader();
        File file = new File(classLoader.getResource("sample-empty").getFile());
        String args[] = {
                "-input", file.getAbsolutePath(),
                "-inputType", "kml",
        };

        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            log.error("Error : {}", e.getMessage());
            log.debug(e.getMessage());
        }
    }
    @Test
    void emptyConvert() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-empty").getFile());
        File output = new File(classLoader.getResource("./sample-empty").getFile());
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
        };
        Mago3DTilerMain.main(args);
    }
    @Test
    void defaultConvert() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-kml").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
        };
        Mago3DTilerMain.main(args);
    }
    @Test
    void multiThreadConvert() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-kml").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                //"-multiThread",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void multiThreadConvertErrorCase() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-kml-error-case").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());
        String args[] = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                //"-multiThread",
        };
        try {
            Mago3DTilerMain.main(args);
        } catch (Exception e) {
            log.debug("success test.");
            log.debug(e.getMessage());
        }
    }
}