package com.gaia3d.process.output;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Writes every tileset file as a normal file under the output directory.
 */
public class DirectoryTilesetSink implements TilesetSink {
    private final Path root;

    public DirectoryTilesetSink(Path root) {
        this.root = root;
    }

    @Override
    public void write(String path, byte[] data) throws IOException {
        Path target = root.resolve(TilesetSink.normalize(path));
        Path parent = target.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.write(target, data);
    }

    @Override
    public boolean exists(String path) {
        return Files.exists(root.resolve(TilesetSink.normalize(path)));
    }

    @Override
    public void finish() {
    }

    @Override
    public void abort() {
    }
}
