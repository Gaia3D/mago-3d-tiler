package com.gaia3d.converter.pointcloud;

import com.gaia3d.util.geographic.TileCoordinate;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class FileHandlePool implements Closeable {
    private static final int COARSE_LEVEL = 13;
    private final Path rootDir;
    private final int maxOpenFiles;
    private final int bufferSize;

    private final Map<Integer, Handle> handles = new HashMap<>();
    private final LinkedHashMap<Integer, Handle> openHandles;

    private static class Handle {
        final int bucketId;
        final Path path;
        OutputStream out;

        Handle(int bucketId, Path path) {
            this.bucketId = bucketId;
            this.path = path;
        }

        boolean isOpen() {
            return out != null;
        }
    }

    public FileHandlePool(Path rootDir, int maxOpenFiles) throws IOException {
        this(rootDir, maxOpenFiles, 4 * 1024 * 1024); // default 4MB buffer
    }

    public FileHandlePool(Path rootDir, int maxOpenFiles, int bufferSize) throws IOException {
        this.rootDir = rootDir;
        this.maxOpenFiles = Math.max(1, maxOpenFiles);
        this.bufferSize = Math.max(4 * 1024 * 1024, bufferSize);

        if (!Files.exists(rootDir)) {
            Files.createDirectories(rootDir);
        }

        this.openHandles = new LinkedHashMap<>(16, 0.75f, true);
    }

    public synchronized OutputStream getOutputStream(int bucketId) throws IOException {
        Handle handle = handles.get(bucketId);
        if (handle == null) {
            Path path = bucketPath(bucketId);
            handle = new Handle(bucketId, path);
            handles.put(bucketId, handle);
        }

        if (!handle.isOpen()) {
            ensureCapacityForNewHandle();
            if (!Files.exists(handle.path.getParent())) {
                Files.createDirectories(handle.path.getParent());
            }
            OutputStream fos = new FileOutputStream(handle.path.toFile(), true);
            handle.out = new BufferedOutputStream(fos, bufferSize);
            openHandles.put(bucketId, handle);
        } else {
            openHandles.get(bucketId);
        }

        return handle.out;
    }

    private void ensureCapacityForNewHandle() throws IOException {
        while (openHandles.size() >= maxOpenFiles) {
            Map.Entry<Integer, Handle> eldest = openHandles.entrySet().iterator().next();
            Handle handleToClose = eldest.getValue();
            closeHandle(handleToClose);
            openHandles.remove(handleToClose.bucketId);
        }
    }

    private void closeHandle(Handle handle) throws IOException {
        if (handle.out != null) {
            try {
                handle.out.flush();
            } finally {
                handle.out.close();
                handle.out = null;
            }
        }
    }

    private TileCoordinate bucketIdToTileCoordinate(int bucketId) {
        int x = bucketId & 0xFFFF;
        int y = (bucketId >> 16) & 0xFFFF;
        return new TileCoordinate(COARSE_LEVEL, x, y);
    }

    private Path bucketPath(int bucketId) {
        TileCoordinate tile = bucketIdToTileCoordinate(bucketId);

        //String fileName = String.format("bucket_%06d.bin", bucketId);
        String fileName = String.format("bucket.bin");
        String filePath = String.format("%d/%d/%d/" + fileName, tile.level, tile.x, tile.y);
        return rootDir.resolve(filePath);
    }

    @Override
    public synchronized void close() throws IOException {
        IOException first = null;

        for (Handle handle : handles.values()) {
            if (handle.isOpen()) {
                try {
                    handle.out.flush();
                } catch (IOException e) {
                    if (first == null) first = e;
                }
                try {
                    handle.out.close();
                } catch (IOException e) {
                    if (first == null) first = e;
                } finally {
                    handle.out = null;
                }
            }
        }

        openHandles.clear();

        if (first != null) {
            throw first;
        }
    }
}