package com.gaia3d.process.output;

import com.gaia3d.command.mago.GlobalConstants;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.command.mago.OutputArchive;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Holds the sink that receives the tileset files of the current run (--outputArchive).
 * Without an opened sink the files go to the output directory, as before.
 */
@Slf4j
public final class TilesetOutput {
    private static volatile TilesetSink current;

    private TilesetOutput() {
    }

    public static synchronized TilesetSink open(GlobalOptions globalOptions) throws IOException {
        if (current != null) {
            current.abort();
        }
        Path outputPath = Path.of(globalOptions.getOutputPath());
        if (globalOptions.getOutputArchive() == OutputArchive.THREE_TZ) {
            Path archiveFile = outputPath.resolve(GlobalConstants.DEFAULT_ARCHIVE_FILE);
            log.info("[Output] Writing the tileset into the 3TZ archive {}", archiveFile.toAbsolutePath());
            current = new ArchiveTilesetSink(archiveFile);
        } else {
            current = new DirectoryTilesetSink(outputPath);
        }
        return current;
    }

    public static TilesetSink get() {
        TilesetSink sink = current;
        if (sink != null) {
            return sink;
        }
        return new DirectoryTilesetSink(Path.of(GlobalOptions.getInstance().getOutputPath()));
    }

    public static void write(String path, byte[] data) throws IOException {
        get().write(path, data);
    }

    public static void writeJson(String path, String json) throws IOException {
        get().write(path, json.getBytes(StandardCharsets.UTF_8));
    }

    public static synchronized void finish() throws IOException {
        TilesetSink sink = current;
        if (sink == null) {
            return;
        }
        try {
            sink.finish();
        } finally {
            current = null;
        }
    }

    // not synchronized: also called from the shutdown hook while finish() may hold the lock
    public static void abort() {
        TilesetSink sink = current;
        current = null;
        if (sink != null) {
            sink.abort();
        }
    }
}
