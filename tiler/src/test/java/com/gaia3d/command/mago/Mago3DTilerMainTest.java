package com.gaia3d.command.mago;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class Mago3DTilerMainTest {
    /*
        ┌┬┐┌─┐┌─┐┌─┐  ┌┬┐┬┬  ┌─┐┬─┐
        │││├─┤│ ┬│ │───│ ││  ├┤ ├┬┘
        ┴ ┴┴ ┴└─┘└─┘   ┴ ┴┴─┘└─┘┴└─
        ----------------------------------------
        usage: Gaia3D Tiler
         -aa,--autoUpAxis               [Experimental] automatically Assign 3D
                                        Matrix Axes
         -ac,--altitudeColumn <arg>     altitude Column setting.
         -c,--crs <arg>                 Coordinate Reference Systems, only epsg
                                        code (4326, 3857, etc...)
         -d,--debug                     debug mode
         -dad,--debugAllDrawing         debug all drawing
         -dit,--debugIgnoreTextures     debug ignore textures
         -fc,--flipCoordinate           flip x,y Coordinate.
         -glb,--glb                     create glb file.
         -gltf,--gltf                   create gltf file.
         -gt,--geoTiff <arg>            [Experimental] geoTiff file path, 3D
                                        Object applied as clampToGround.
         -h,--help                      print this message
         -hc,--heightColumn <arg>       height column setting. (Default: height)
         -i,--input <arg>               input file path
         -it,--inputType <arg>          input file type (kml, 3ds, obj, gltf,
                                        etc...)
         -l,--log <arg>                 output log file path
         -mc,--multiThreadCount <arg>   multi thread count (Default: 8)
         -mh,--minimumHeight <arg>      minimum height setting.
         -mp,--maxPoints <arg>          max points of pointcloud data (Default:
                                        20000)
         -mt,--multiThread              multi thread mode
         -mx,--maxCount <arg>           max count of nodes (Default: 1024)
         -nc,--nameColumn <arg>         name column setting. (Default: name)
         -nl,--minLod <arg>             min level of detail (Default: 0)
         -o,--output <arg>              output file path
         -ot,--outputType <arg>         output file type
         -p,--proj <arg>                proj4 parameters (ex: +proj=tmerc +la...)
         -pt,--pngTexture               png texture mode
         -q,--quiet                     quiet mode
         -r,--recursive                 deep directory exploration
         -ra,--refineAdd                refine addd mode
         -rt,--reverseTexCoord          texture y-axis coordinate reverse
         -te,--terrain <arg>            [Experimental] terrain file path, 3D
                                        Object applied as clampToGround.
         -v,--version                   print version
         -xl,--maxLod <arg>             max level of detail (Default: 3)
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