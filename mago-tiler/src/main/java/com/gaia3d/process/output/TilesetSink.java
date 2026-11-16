package com.gaia3d.process.output;

import java.io.IOException;

/**
 * Destination of the tileset files (tileset.json, subtrees, tile contents).
 * Paths are relative to the tileset root and use '/' separators, e.g. "data/R0.b3dm".
 * Implementations must be safe to call from several threads.
 */
public interface TilesetSink {
    void write(String path, byte[] data) throws IOException;

    boolean exists(String path);

    /**
     * Completes the output. Nothing may be written afterwards.
     */
    void finish() throws IOException;

    /**
     * Discards an unfinished output, e.g. after a failed or stopped run.
     */
    void abort();

    static String normalize(String path) {
        String normalized = path.replace('\\', '/');
        while (normalized.startsWith("/") || normalized.startsWith("./")) {
            normalized = normalized.substring(normalized.startsWith("/") ? 1 : 2);
        }
        return normalized;
    }
}
