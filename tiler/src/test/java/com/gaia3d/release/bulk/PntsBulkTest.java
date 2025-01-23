package com.gaia3d.release.bulk;

import com.gaia3d.command.mago.Mago3DTilerMain;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

@Slf4j
class PntsBulkTest {
    private static final String INPUT_PATH = "D:/data/mago-3d-tiler/release-sample";
    private static final String OUTPUT_PATH = "D:/data/mago-server/output";

    @Test
    void pointcloud00() {
        String originalPath = "G:\\(2024)\\(2024) 프로젝트 문서 및 파일\\(울주군DT) 부산진구_04_LAS(1.4_RGBN)\\변환결과\\BUSAN_JINGU_SPLIT";
        String path = "BULK-P00-hwangyonggak-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "32652",
        };
        execute(args);
    }

    @Test
    void pointcloud01() {
        String path = "P01-sejong-bridge-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5186"
        };
        execute(args);
    }

    @Test
    void pointcloud02() {
        String originalPath = "G:\\(2024)\\(2024) 프로젝트 문서 및 파일\\(울주군DT) 부산진구_04_LAS(1.4_RGBN)\\변환결과\\BUSAN_JINGU_SPLIT\\";
        String path = "BULK-P02-busan-jingu-las";
        String[] args = new String[] {
                "-i", originalPath,
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "5187",
                "-pointRatio", "25",
        };
        execute(args);
    }

    @Test
    void pointcloud03() {
        String originalPath = "G:\\(2024)\\(2024) 프로젝트 문서 및 파일\\(태국) 2024 구축데이터\\(Tailland) PointCloud\\Tile\\";
        String path = "BULK-P03-thai-khonkaen-las";
        String[] args = new String[] {
                "-i", getInputPath(path).getAbsolutePath(),
                "-o", getOutputPath(path).getAbsolutePath(),
                "-c", "32648",
                "-pointRatio", "25",
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
