package com.gaia3d.process;

import com.gaia3d.basic.types.FormatType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("default")
class DiskSpaceCheckerTest {
    private static final long GB = 1L << 30;

    @Test
    void stopsOnlyBelowTheMinimum() {
        DiskSpaceChecker.Need need = new DiskSpaceChecker.Need();
        need.minimum = 10 * GB;
        need.expected = 40 * GB;
        need.dirs.add("temp T:\\temp");

        assertThrows(IllegalStateException.class, () -> DiskSpaceChecker.check(10 * GB, 10 * GB, need));
        assertDoesNotThrow(() -> DiskSpaceChecker.check(11 * GB + 1, 10 * GB, need)); // warns
        assertDoesNotThrow(() -> DiskSpaceChecker.check(100 * GB, 10 * GB, need));
    }

    @Test
    void passesOnSmallInput(@TempDir Path dir) throws IOException {
        Path input = Files.createDirectories(dir.resolve("in"));
        File obj = Files.write(input.resolve("a.obj"), new byte[1024]).toFile();
        File temp = dir.resolve("out").resolve("temp").toFile(); // not created yet
        assertDoesNotThrow(() -> DiskSpaceChecker.check(temp, dir.resolve("out").toFile(), List.of(obj), FormatType.OBJ, FormatType.B3DM, false));
    }

    @Test
    void countsSelectedFilesWithTheirSideFilesAndTextures(@TempDir Path dir) throws IOException {
        Path input = Files.createDirectories(dir.resolve("in"));
        File obj = Files.write(input.resolve("a.obj"), new byte[1000]).toFile();
        Files.write(input.resolve("a.mtl"), new byte[100]);
        Files.write(Files.createDirectories(input.resolve("textures")).resolve("a.jpg"), new byte[2000]);
        File shp = Files.write(input.resolve("roads.shp"), new byte[300]).toFile();
        Files.write(input.resolve("roads.dbf"), new byte[30]);
        // not converted: another dataset, and the output inside the input folder
        Files.write(input.resolve("scan.las"), new byte[1_000_000]);
        Path output = Files.createDirectories(input.resolve("output"));
        Files.write(Files.createDirectories(output.resolve("temp")).resolve("atlas.png"), new byte[500_000]);

        assertEquals(3100, DiskSpaceChecker.sizeOf(List.of(obj), true, List.of(output.resolve("temp").toFile(), output.toFile())));
        assertEquals(1100, DiskSpaceChecker.sizeOf(List.of(obj), false, List.of(output.toFile())));
        assertEquals(330, DiskSpaceChecker.sizeOf(List.of(shp), false, List.of(output.toFile())));
    }

    @Test
    void choosesRatioByInputAndOutput() {
        assertEquals(DiskSpaceChecker.Ratio.POINT_CLOUD, DiskSpaceChecker.Ratio.of(FormatType.LAZ, FormatType.PNTS, false));
        assertEquals(DiskSpaceChecker.Ratio.PHOTOGRAMMETRY, DiskSpaceChecker.Ratio.of(FormatType.OBJ, FormatType.B3DM, true));
        assertEquals(DiskSpaceChecker.Ratio.INSTANCE, DiskSpaceChecker.Ratio.of(FormatType.KML, FormatType.I3DM, false));
        assertEquals(DiskSpaceChecker.Ratio.VECTOR, DiskSpaceChecker.Ratio.of(FormatType.GEO_PACKAGE, FormatType.B3DM, false));
        assertEquals(DiskSpaceChecker.Ratio.MESH, DiskSpaceChecker.Ratio.of(FormatType.MAX_3DS, FormatType.B3DM, false));
    }
}
