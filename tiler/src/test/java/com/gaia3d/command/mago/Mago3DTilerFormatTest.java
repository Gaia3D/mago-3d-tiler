package com.gaia3d.command.mago;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.joml.Random;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class Mago3DTilerFormatTest {

    private static final String INPUT_PATH = "D:\\DT\\";
    private static final String OUTPUT_PATH = "C:\\Workspaces\\GitSources\\mago-viewer\\data\\tilesets\\DT\\";

    @Test
    void THREEDS() {
        String path = "3DS";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-inputType", "3ds",
                "-crs", "5186",
                //"-gltf",
                //"-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void DAE() {
        String path = "DAE";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-inputType", "dae",
                "-crs", "5186",
                //"-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void FBX() {
        String path = "FBX";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-inputType", "fbx",
                "-crs", "5186",
                "-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void CityGML() {
        String path = "CityGML";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-inputType", "gml",
                "-crs", "5186",
                //"-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void GLTF() {
        String path = "GLTF";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-inputType", "gltf",
                "-crs", "5186",
                //"-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void IFC() {
        String path = "IFC";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-inputType", "ifc",
                "-crs", "5186",
                //"-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void OBJ() {
        String path = "OBJ";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-inputType", "obj",
                "-crs", "5186",
                //"-glb"
                //"-autoUpAxis",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void CITYGML() {
        String path = "CITYGML";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-inputType", "citygml",
                "-crs", "5186",
                "-glb",
                //"-glb"
                //"-autoUpAxis",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void LAS() {
        String path = "LAS";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-inputType", "las",
                "-skipPoints", "256",
                "-crs", "5186",
        };
        Mago3DTilerMain.main(args);
    }

    @Test
    void INDOORGML() {
        String path = "INDOORGML";
        File input = new File(INPUT_PATH, path);
        File output = new File(OUTPUT_PATH, path);
        FileUtils.deleteQuietly(output);
        String[] args = {
                "-input", input.getAbsolutePath(),
                "-output", output.getAbsolutePath(),
                "-inputType", "indoorgml",
                "-crs", "5186",
                "-glb",
                "-debug"
        };
        Mago3DTilerMain.main(args);
    }

}