package com.gaia3d.process.output;

import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.command.mago.OutputArchive;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

@Tag("default")
class TilesetOutputTest {

    @TempDir
    Path tempDir;

    private String outputPath;
    private OutputArchive outputArchive;

    @BeforeEach
    void saveOptions() {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        outputPath = globalOptions.getOutputPath();
        outputArchive = globalOptions.getOutputArchive();
        globalOptions.setOutputPath(tempDir.toString());
    }

    @AfterEach
    void restoreOptions() {
        TilesetOutput.abort();
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setOutputPath(outputPath);
        globalOptions.setOutputArchive(outputArchive);
    }

    @Test
    void writesAfterArchiveAbortFailWithoutLooseFiles() throws Exception {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setOutputArchive(OutputArchive.THREE_TZ);
        TilesetOutput.open(globalOptions);
        TilesetOutput.write("data/R0.b3dm", new byte[16]);
        TilesetOutput.abort();

        assertThrows(IOException.class, () -> TilesetOutput.write("data/R1.b3dm", new byte[16]));
        assertThrows(IOException.class, () -> TilesetOutput.write("data/R2.b3dm", stream(16)));
        assertThrows(IOException.class, () -> TilesetOutput.writeJson("tileset.json", "{}"));
        assertFalse(TilesetOutput.get().exists("data/R0.b3dm"));
        assertFalse(Files.exists(tempDir.resolve("data")), "no loose files next to the archive");
        assertFalse(Files.exists(tempDir.resolve("tileset.json")));
        assertFalse(Files.exists(tempDir.resolve("tileset.3tz.part")));
        assertFalse(Files.exists(tempDir.resolve("tileset.3tz")));
    }

    @Test
    void writesAfterArchiveFinishFailWithoutLooseFiles() throws Exception {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setOutputArchive(OutputArchive.THREE_TZ);
        TilesetOutput.open(globalOptions);
        TilesetOutput.write("data/R0.b3dm", stream(32));
        TilesetOutput.writeJson("tileset.json", "{}");
        TilesetOutput.finish();

        assertThrows(IOException.class, () -> TilesetOutput.write("data/R1.b3dm", new byte[16]));
        TilesetOutput.abort();
        assertThrows(IOException.class, () -> TilesetOutput.write("data/R1.b3dm", new byte[16]));
        assertFalse(Files.exists(tempDir.resolve("data")));
        try (ZipFile zipFile = new ZipFile(tempDir.resolve("tileset.3tz").toFile())) {
            assertEquals(3, zipFile.size());
            assertEquals(32, zipFile.getEntry("data/R0.b3dm").getSize());
        }
    }

    @Test
    void failedWriteFailsTheRunAndDiscardsTheArchive() throws Exception {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setOutputArchive(OutputArchive.THREE_TZ);
        TilesetOutput.open(globalOptions);
        TilesetOutput.write("data/R0.b3dm", new byte[16]);
        assertFalse(TilesetOutput.hasFailed());
        // a content writer that only logs the error does not hide it from the pipeline
        try {
            TilesetOutput.write("data/nested.3tz", new byte[16]);
            fail("reserved name accepted");
        } catch (IOException e) {
            // logged and swallowed by a content writer
        }
        assertTrue(TilesetOutput.hasFailed());
        assertThrows(IOException.class, TilesetOutput::checkFailure);
        assertThrows(IOException.class, TilesetOutput::finish);
        assertFalse(Files.exists(tempDir.resolve("tileset.3tz.part")));
        assertFalse(Files.exists(tempDir.resolve("tileset.3tz")));

        // the next run starts clean
        TilesetOutput.open(globalOptions);
        assertFalse(TilesetOutput.hasFailed());
        TilesetOutput.abort();
    }

    @Test
    void folderModeWritesFilesAlsoAfterAnArchiveRun() throws Exception {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setOutputArchive(OutputArchive.THREE_TZ);
        TilesetOutput.open(globalOptions);
        TilesetOutput.abort();

        globalOptions.setOutputArchive(OutputArchive.NONE);
        TilesetOutput.write("data/R0.b3dm", stream(8));
        assertEquals(8, Files.size(tempDir.resolve("data/R0.b3dm")));

        TilesetOutput.open(globalOptions);
        TilesetOutput.writeJson("tileset.json", "{}");
        TilesetOutput.finish();
        TilesetOutput.write("data/R1.b3dm", new byte[4]);
        assertEquals("{}", Files.readString(tempDir.resolve("tileset.json"), StandardCharsets.UTF_8));
        assertEquals(4, Files.size(tempDir.resolve("data/R1.b3dm")));
    }

    private static ByteArrayOutputStream stream(int length) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.writeBytes(new byte[length]);
        return outputStream;
    }
}
