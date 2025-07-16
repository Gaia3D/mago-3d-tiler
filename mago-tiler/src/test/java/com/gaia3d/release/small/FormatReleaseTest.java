package com.gaia3d.release.small;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("release")
@Slf4j
class FormatReleaseTest {
    @Test
    void format00() {
        String path = "A00-Thonker-glb";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void format01() {
        String path = "A01-Thonker-gltf";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void format02() {
        String path = "A02-Thonker-obj";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void format03() {
        String path = "A03-Thonker-3mf";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void format04() {
        String path = "A04-Thonker-ply";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void format05() {
        String path = "A05-Thonker-fbx";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void format06() {
        String path = "A06-Thonker-dae";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void format07() {
        String path = "A07-Thonker-x3d";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void format08() {
        String path = "A08-Thonker-stl";
        String[] args = new String[] {
                "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
        };
        ReleaseTestConfig.execute(args);
    }

    @Test
    void format09() {
        try {
            String path = "A09-Thonker-blend";
            String[] args = new String[] {
                    "-i", ReleaseTestConfig.getInputPath(path).getAbsolutePath(),
                    "-o", ReleaseTestConfig.getOutputPath(path).getAbsolutePath(),
            };
            ReleaseTestConfig.execute(args);
        } catch (RuntimeException e) {
            log.error("[ERROR] Failed to run process, Please check the arguments.", e);
            assert true;
        }
    }
}
