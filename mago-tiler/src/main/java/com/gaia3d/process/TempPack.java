package com.gaia3d.process;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Append-only pack files for intermediate data, instead of one temp file per object.
 * Every writer thread appends to its own pack file, an entry remembers where its bytes are and is read back with
 * positional reads. Entries are never removed: the temp directory is deleted as a whole after the run.
 */
@Slf4j
public class TempPack implements Closeable {
    private static final Map<Path, TempPack> PACKS = new ConcurrentHashMap<>();
    private static final int BUFFER_SIZE = 64 * 1024;

    private final Path directory;
    private final AtomicInteger packCount = new AtomicInteger();
    private final Map<Thread, Path> writerPacks = new ConcurrentHashMap<>();
    private final Map<Path, FileChannel> channels = new ConcurrentHashMap<>();
    private volatile boolean closed = false;

    public record Entry(Path pack, long offset, long length) {}

    @FunctionalInterface
    public interface Writer {
        void write(OutputStream outputStream) throws IOException;
    }

    public TempPack(Path directory) {
        this.directory = directory;
    }

    /**
     * The shared pack of a directory, created on first use. {@link #closeAll()} closes it.
     */
    public static TempPack of(Path directory) {
        return PACKS.computeIfAbsent(directory.toAbsolutePath().normalize(), TempPack::new);
    }

    /**
     * Closes every shared pack, so that the temp directory can be deleted (open files can not be deleted on Windows).
     */
    public static void closeAll() {
        PACKS.values().forEach(TempPack::close);
        PACKS.clear();
    }

    /**
     * Appends the bytes written by the writer to the pack file of the calling thread.
     */
    public Entry write(Writer writer) throws IOException {
        Path pack = writerPacks.computeIfAbsent(Thread.currentThread(), thread -> directory.resolve("pack-" + packCount.getAndIncrement() + ".bin"));
        FileChannel channel = channel(pack);
        long offset = channel.size();
        channel.position(offset);
        try {
            OutputStream outputStream = new BufferedOutputStream(new ChannelOutputStream(channel), BUFFER_SIZE);
            writer.write(outputStream);
            outputStream.flush();
        } catch (IOException | RuntimeException e) {
            // drop the partial entry
            if (channel.isOpen()) {
                channel.truncate(offset);
            }
            throw e;
        }
        return new Entry(pack, offset, channel.position() - offset);
    }

    /**
     * Stream over the bytes of an entry.
     */
    public InputStream read(Entry entry) throws IOException {
        return new BufferedInputStream(new ChannelInputStream(entry), BUFFER_SIZE);
    }

    // reopens a channel that was closed by an interrupted thread, FileChannel closes itself for everyone in that case
    private FileChannel channel(Path pack) throws IOException {
        FileChannel channel = channels.get(pack);
        if (channel != null && channel.isOpen()) {
            return channel;
        }
        synchronized (this) {
            if (closed) {
                throw new ClosedChannelException();
            }
            channel = channels.get(pack);
            if (channel == null || !channel.isOpen()) {
                Files.createDirectories(directory);
                channel = FileChannel.open(pack, StandardOpenOption.CREATE, StandardOpenOption.READ, StandardOpenOption.WRITE);
                channels.put(pack, channel);
            }
            return channel;
        }
    }

    @Override
    public synchronized void close() {
        closed = true;
        for (FileChannel channel : channels.values()) {
            try {
                channel.close();
            } catch (IOException e) {
                log.warn("[WARN] Failed to close temp pack: {}", e.getMessage());
            }
        }
        channels.clear();
        writerPacks.clear();
    }

    private static class ChannelOutputStream extends OutputStream {
        private final FileChannel channel;

        ChannelOutputStream(FileChannel channel) {
            this.channel = channel;
        }

        @Override
        public void write(int b) throws IOException {
            write(new byte[]{(byte) b}, 0, 1);
        }

        @Override
        public void write(byte[] bytes, int offset, int length) throws IOException {
            ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, length);
            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }
        }

        @Override
        public void close() {
            // the channel stays open for the next entries
        }
    }

    private class ChannelInputStream extends InputStream {
        private final Entry entry;
        private long position;

        ChannelInputStream(Entry entry) {
            this.entry = entry;
            this.position = entry.offset();
        }

        @Override
        public int read() throws IOException {
            byte[] single = new byte[1];
            return read(single, 0, 1) == -1 ? -1 : single[0] & 0xFF;
        }

        @Override
        public int read(byte[] bytes, int offset, int length) throws IOException {
            long remaining = entry.offset() + entry.length() - position;
            if (remaining <= 0) {
                return -1;
            }
            ByteBuffer buffer = ByteBuffer.wrap(bytes, offset, (int) Math.min(length, remaining));
            int read = channel(entry.pack()).read(buffer, position);
            if (read < 0) {
                throw new EOFException("Unexpected end of temp pack: " + entry.pack());
            }
            position += read;
            return read;
        }
    }
}
