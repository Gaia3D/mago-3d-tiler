package com.gaia3d.process;

import com.gaia3d.basic.types.FormatType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

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
        Files.write(input.resolve("a.obj"), new byte[1024]);
        Files.write(Files.createDirectories(input.resolve("images")).resolve("a.jpg"), new byte[2048]);
        assertEquals(3072, DiskSpaceChecker.sizeOf(input.toFile()));

        File temp = dir.resolve("out").resolve("temp").toFile(); // not created yet
        assertDoesNotThrow(() -> DiskSpaceChecker.check(temp, dir.resolve("out").toFile(), input.toFile(), FormatType.OBJ, FormatType.B3DM, false));
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
