package com.gaia3d.release.big;

import com.gaia3d.command.Configuration;
import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

@Slf4j
class I3dmBuildTest {
    private static final String INPUT_PATH = "D:/data/mago-3d-tiler/build-sample";
    private static final String OUTPUT_PATH = "E:/data/mago-server/output";

    @Test
    void instanced07Chim() {
        String path = "Chim";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                "-it", "shp",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/chim-sample-low.glb",
                "-terrain", "G:/workspace/dem05.tif",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced07jat() {
        String path = "Jat";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                "-it", "shp",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/jat-sample-low.glb",
                "-terrain", "G:/workspace/dem05.tif",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced07Hwal() {
        String path = "Hwal";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                "-it", "shp",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/hwal-sample-low.glb",
                "-terrain", "G:/workspace/dem05.tif",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced07Nak() {
        String path = "Nak";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                "-it", "shp",
                "-ot", "i3dm",
                "-refineAdd",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/nak-sample-low.glb",
                "-terrain", "G:/workspace/dem05.tif",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced07NakOffset() {
        String path = "Nak";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath() + "-offset",
                "-c", "5187",
                "-it", "shp",
                "-ot", "i3dm",
                "-instance", getInputPath("sample-tree").getAbsolutePath() + "/nak-sample-low.glb",
                "-terrain", "G:/workspace/dem05.tif",
                "-zOffset", "100.0",
                //"-xOffset", "100.0",
                //"-yOffset", "100.0",
                //"-debug"
        };
        execute(args);
    }

    @Test
    void instanced06Merger() throws IOException {
        Configuration.initConsoleLogger();

        String path = "개체목 수정";

        /*File files = getOutputPath(path);
        for (File file : files.listFiles()) {
            if (file.isDirectory()) {
                boolean hasDataFolder = false;
                for (File subFile : file.listFiles()) {
                    if (subFile.isDirectory() && subFile.getName().equals("data")) {
                        hasDataFolder = true;
                        break;
                    }
                }

                if (!hasDataFolder) {
                    log.info("Directory: {}", file.getName());

                    FileUtils.deleteDirectory(file);

                }
            }
        }*/

        String[] args = new String[] {
                "-i", getOutputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5179",
                "-merge",
                "-ot", "i3dm",
        };
        execute(args);
    }

    private void execute(String[] args) {
        Mago3DTilerMain.main(args);
    }

    private File getInputPath(String path) {
        return new File(INPUT_PATH, path);
    }

    private File getOutputPath(String path) {
        return new File(OUTPUT_PATH, path);
    }
}
