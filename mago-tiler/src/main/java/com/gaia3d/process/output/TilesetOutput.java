package com.gaia3d.process.output;

import com.gaia3d.command.mago.GlobalConstants;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.command.mago.OutputArchive;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

/**
 * Holds the sink that receives the tileset files of the current run (--outputArchive).
 * Without an opened sink the files go to the output directory, as before.
 * Once an archive was opened, later writes never fall back to the directory: after finish() or abort() they fail.
 * The first failed write is kept so the pipeline can stop the run, see {@link #checkFailure()}.
 */
@Slf4j
public final class TilesetOutput {
    private static final TilesetSink CLOSED = new ClosedTilesetSink();
    private static volatile TilesetSink current;
    private static volatile IOException failure;

    private TilesetOutput() {
    }

    public static synchronized TilesetSink open(GlobalOptions globalOptions) throws IOException {
        TilesetSink previous = current;
        if (previous != null) {
            previous.abort();
        }
        failure = null;
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
        if (sink != null && (sink != CLOSED || GlobalOptions.getInstance().getOutputArchive() != OutputArchive.NONE)) {
            return sink;
        }
        return new DirectoryTilesetSink(Path.of(GlobalOptions.getInstance().getOutputPath()));
    }

    public static void write(String path, byte[] data) throws IOException {
        try {
            get().write(path, data);
        } catch (IOException e) {
            throw recordFailure(e);
        }
    }

    /**
     * Writes the bytes of the stream, in folder mode straight from its buffer without copying it.
     */
    public static void write(String path, ByteArrayOutputStream data) throws IOException {
        try {
            get().write(path, data);
        } catch (IOException e) {
            throw recordFailure(e);
        }
    }

    public static void writeJson(String path, String json) throws IOException {
        write(path, json.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * True after a tileset file could not be written, the run is going to fail.
     */
    public static boolean hasFailed() {
        return failure != null;
    }

    /**
     * Throws the first write failure of this run, also when the content writer that saw it only logged it.
     */
    public static void checkFailure() throws IOException {
        IOException exception = failure;
        if (exception != null) {
            throw new IOException("Failed to write the tileset: " + exception.getMessage(), exception);
        }
    }

    private static synchronized IOException recordFailure(IOException exception) {
        if (failure == null) {
            failure = exception;
        }
        return exception;
    }

    public static synchronized void finish() throws IOException {
        TilesetSink sink = current;
        if (sink == null || sink == CLOSED) {
            return;
        }
        try {
            checkFailure();
            sink.finish();
        } catch (IOException e) {
            sink.abort();
            throw e;
        } finally {
            current = sink instanceof DirectoryTilesetSink ? null : CLOSED;
        }
    }

    // not synchronized: also called from the shutdown hook while finish() may hold the lock
    public static void abort() {
        TilesetSink sink = current;
        if (sink == null || sink == CLOSED) {
            return;
        }
        current = sink instanceof DirectoryTilesetSink ? null : CLOSED;
        sink.abort();
    }

    /**
     * Takes the place of a finished or aborted archive: late writes, e.g. from worker threads still running after Ctrl+C, fail
     * instead of creating loose files next to the archive.
     */
    private static final class ClosedTilesetSink implements TilesetSink {
        @Override
        public void write(String path, byte[] data) throws IOException {
            throw new IOException("The tileset output is already closed: " + path);
        }

        @Override
        public void write(String path, ByteArrayOutputStream data) throws IOException {
            throw new IOException("The tileset output is already closed: " + path);
        }

        @Override
        public boolean exists(String path) {
            return false;
        }

        @Override
        public void finish() {
        }

        @Override
        public void abort() {
        }
    }
}
