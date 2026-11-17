package com.gaia3d.process.output;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.UnaryOperator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Writes the tileset into one 3D Tiles Archive (3TZ) file, a zip file with a "@3dtilesIndex1@" hash index as its last entry.
 * See https://github.com/erikdahlstrom/3tz-specification (v1.3).
 * Callers hash and compress their entries in their own thread; a single writer thread appends them in arrival order.
 * The queue holds at most {@link #QUEUE_BYTE_LIMIT} bytes so the post-process threads wait when the disk is slower than tiling.
 * The archive is written as "{name}.part" and renamed when finished. A run that fails before the archive is complete deletes it,
 * a failed rename keeps the complete "{name}.part".
 */
@Slf4j
public class ArchiveTilesetSink implements TilesetSink {
    public static final String INDEX_ENTRY_NAME = "@3dtilesIndex1@";
    private static final long QUEUE_BYTE_LIMIT = 256L * 1024 * 1024;
    private static final int PERMIT_BYTES = 1024;
    private static final long WAIT_MILLIS = 200;
    // binary tile contents are already compact, they are stored so viewers can read them without inflating
    private static final Set<String> STORED_EXTENSIONS = Set.of("glb", "b3dm", "i3dm", "pnts", "cmpt", "ktx2", "png", "jpg", "jpeg", "webp");

    private static final int LOCAL_HEADER_SIGNATURE = 0x04034b50;
    private static final int CENTRAL_HEADER_SIGNATURE = 0x02014b50;
    private static final int END_SIGNATURE = 0x06054b50;
    private static final int ZIP64_END_SIGNATURE = 0x06064b50;
    private static final int ZIP64_LOCATOR_SIGNATURE = 0x07064b50;
    private static final int METHOD_STORED = 0;
    private static final int METHOD_DEFLATED = 8;
    private static final int VERSION_DEFAULT = 20;
    private static final int VERSION_ZIP64 = 45;
    private static final int FLAG_UTF8 = 1 << 11;
    private static final long ZIP64_LIMIT = 0xFFFFFFFFL;
    private static final int ZIP64_ENTRY_LIMIT = 0xFFFF;

    private static final Entry END = new Entry(null, null, 0, 0, 0, 0);

    private final Path archiveFile;
    private final Path partFile;
    private final OutputStream out;
    private final BlockingQueue<Entry> queue = new LinkedBlockingQueue<>();
    private final int queuePermits;
    private final Semaphore queueBudget;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final Set<String> names = ConcurrentHashMap.newKeySet();
    private final Thread writerThread;
    private final int dosTime;
    private final int dosDate;

    /* accessed by the writer thread only, and by finish() after it has stopped */
    private final Map<String, Record> records = new LinkedHashMap<>();
    private long position = 0;

    private volatile IOException failure;
    private volatile boolean closed = false;
    private volatile boolean finished = false;
    private volatile boolean aborted = false;
    // the part file holds a complete archive, abort() keeps it
    private volatile boolean complete = false;

    public ArchiveTilesetSink(Path archiveFile) throws IOException {
        this(archiveFile, QUEUE_BYTE_LIMIT);
    }

    ArchiveTilesetSink(Path archiveFile, long queueByteLimit) throws IOException {
        this(archiveFile, queueByteLimit, UnaryOperator.identity());
    }

    /**
     * @param wrapOutput wraps the part file stream, lets tests inject write failures
     */
    ArchiveTilesetSink(Path archiveFile, long queueByteLimit, UnaryOperator<OutputStream> wrapOutput) throws IOException {
        this.archiveFile = archiveFile;
        this.partFile = archiveFile.resolveSibling(archiveFile.getFileName() + ".part");
        Path parent = archiveFile.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        this.out = new BufferedOutputStream(wrapOutput.apply(Files.newOutputStream(partFile)), 1 << 20);
        this.queuePermits = (int) Math.max(1, Math.min(Integer.MAX_VALUE, queueByteLimit / PERMIT_BYTES));
        this.queueBudget = new Semaphore(queuePermits);

        LocalDateTime now = LocalDateTime.now();
        this.dosTime = (now.getHour() << 11) | (now.getMinute() << 5) | (now.getSecond() / 2);
        this.dosDate = (Math.max(0, now.getYear() - 1980) << 9) | (now.getMonthValue() << 5) | now.getDayOfMonth();

        this.writerThread = new Thread(this::writeLoop, "3tz-writer");
        this.writerThread.setDaemon(true);
        this.writerThread.start();
    }

    @Override
    public void write(String path, byte[] data) throws IOException {
        String name = TilesetSink.normalize(path);
        if (name.isEmpty() || name.equals(INDEX_ENTRY_NAME) || name.toLowerCase().contains(".3tz")) {
            throw new IOException("Invalid 3TZ entry name: " + path);
        }
        checkFailure();

        CRC32 crc32 = new CRC32();
        crc32.update(data);
        byte[] body = data;
        int method = METHOD_STORED;
        if (!STORED_EXTENSIONS.contains(extension(name))) {
            byte[] deflated = deflate(data);
            if (deflated.length < data.length) {
                body = deflated;
                method = METHOD_DEFLATED;
            }
        }
        int permits = (int) Math.min(queuePermits, Math.max(1, ((long) body.length + PERMIT_BYTES - 1) / PERMIT_BYTES));
        acquire(permits, name);
        lock.readLock().lock();
        try {
            if (closed || failure != null || !writerThread.isAlive()) {
                queueBudget.release(permits);
                checkFailure();
                throw new IOException("3TZ archive is already closed: " + name);
            }
            names.add(name);
            queue.add(new Entry(name, body, method, crc32.getValue(), data.length, permits));
        } finally {
            lock.readLock().unlock();
        }
    }

    // waits for queue space, but gives up when the archive failed or was closed, or the writer thread is gone
    private void acquire(int permits, String name) throws IOException {
        try {
            while (!queueBudget.tryAcquire(permits, WAIT_MILLIS, TimeUnit.MILLISECONDS)) {
                checkFailure();
                if (closed) {
                    throw new IOException("3TZ archive is already closed: " + name);
                }
                if (!writerThread.isAlive()) {
                    throw new IOException("3TZ writer thread has stopped: " + name);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted while waiting for the 3TZ writer: " + name);
        }
    }

    @Override
    public boolean exists(String path) {
        return names.contains(TilesetSink.normalize(path));
    }

    @Override
    public void finish() throws IOException {
        if (finished) {
            return;
        }
        if (aborted) {
            throw new IOException("3TZ archive was aborted: " + archiveFile);
        }
        if (complete) {
            throw new IOException("3TZ archive was written to " + partFile.toAbsolutePath() + " but not renamed to " + archiveFile.toAbsolutePath());
        }
        try {
            stopWriter();
            checkFailure();
            if (!queue.isEmpty()) {
                throw new IOException("3TZ writer stopped before writing every entry (" + queue.size() + " left).");
            }
            byte[] index = createIndex(records.values());
            CRC32 crc32 = new CRC32();
            crc32.update(index);
            Record indexRecord = writeLocalEntry(new Entry(INDEX_ENTRY_NAME, index, METHOD_STORED, crc32.getValue(), index.length, 0));

            List<Record> centralRecords = new ArrayList<>(records.values());
            centralRecords.add(indexRecord);
            long centralOffset = position;
            for (Record record : centralRecords) {
                byte[] header = centralHeader(record, dosTime, dosDate);
                out.write(header);
                position += header.length;
            }
            byte[] end = endRecords(centralRecords.size(), centralOffset, position - centralOffset, position);
            out.write(end);
            position += end.length;
            out.close();
        } catch (IOException | RuntimeException | Error e) {
            abort();
            throw e;
        }
        complete = true;
        try {
            moveToArchive();
        } catch (IOException e) {
            // the part file is a complete archive: keep it, it can be renamed once the old archive is released
            log.error("[ERROR][Output] The 3TZ archive is complete but could not replace {} ({}). It was kept as {}",
                    archiveFile.toAbsolutePath(), e.toString(), partFile.toAbsolutePath());
            throw new IOException("Failed to rename the finished 3TZ archive to " + archiveFile.toAbsolutePath()
                    + " (is the old archive open in another program?). The complete archive was kept as " + partFile.toAbsolutePath(), e);
        }
        finished = true;
        log.info("[Output] Wrote 3TZ archive {} ({} entries, {} bytes).", archiveFile.toAbsolutePath(), records.size(), Files.size(archiveFile));
    }

    @Override
    public void abort() {
        if (finished || aborted || complete) {
            return;
        }
        aborted = true;
        closed = true;
        setFailure(new IOException("3TZ archive was aborted."));
        // END stops the writer also when the interrupt is consumed by a blocked write
        queue.add(END);
        writerThread.interrupt();
        // wake callers waiting for queue space, they see the failure and stop
        queueBudget.release(queuePermits);
        try {
            writerThread.join(5000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        try {
            out.close();
        } catch (IOException e) {
            log.debug("[Output] Failed to close the 3TZ part file: {}", e.getMessage());
        }
        try {
            if (Files.deleteIfExists(partFile)) {
                log.info("[Output] Deleted unfinished 3TZ archive {}", partFile.toAbsolutePath());
            }
        } catch (IOException e) {
            log.warn("[WARN] Failed to delete unfinished 3TZ archive {}: {}", partFile.toAbsolutePath(), e.getMessage());
        }
    }

    private void stopWriter() throws IOException {
        lock.writeLock().lock();
        try {
            closed = true;
            queue.add(END);
        } finally {
            lock.writeLock().unlock();
        }
        try {
            writerThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            abort();
            throw new InterruptedIOException("Interrupted while finishing the 3TZ archive.");
        }
    }

    private void writeLoop() {
        try {
            while (true) {
                Entry entry;
                try {
                    entry = queue.take();
                } catch (InterruptedException e) {
                    setFailure(new InterruptedIOException("3TZ writer was interrupted."));
                    return;
                }
                if (entry == END) {
                    return;
                }
                try {
                    if (failure == null) {
                        Record record = writeLocalEntry(entry);
                        if (records.put(entry.name, record) != null) {
                            log.warn("[WARN][Output] '{}' was written twice, the 3TZ archive keeps the last one.", entry.name);
                        }
                    }
                } catch (IOException e) {
                    setFailure(e);
                } catch (Throwable e) {
                    // also errors such as OutOfMemoryError: the archive misses this entry, so it must not be finished
                    setFailure(new IOException("Failed to write 3TZ entry " + entry.name + ": " + e, e));
                } finally {
                    // keep draining after a failure so waiting callers are released, they get the failure on their next write
                    queueBudget.release(entry.permits);
                }
            }
        } catch (Throwable e) {
            setFailure(new IOException("3TZ writer thread failed: " + e, e));
            // nobody drains the queue anymore, wake every waiting caller
            queueBudget.release(queuePermits);
        }
    }

    private synchronized void setFailure(IOException exception) {
        if (failure == null) {
            failure = exception;
        }
    }

    private Record writeLocalEntry(Entry entry) throws IOException {
        // sizes and crc32 are always in the local header (no data descriptor), the 3TZ spec requires it
        byte[] nameBytes = entry.name.getBytes(StandardCharsets.UTF_8);
        int flags = isAscii(entry.name) ? 0 : FLAG_UTF8;
        long offset = position;
        ByteBuffer header = littleEndian(30 + nameBytes.length);
        header.putInt(LOCAL_HEADER_SIGNATURE);
        header.putShort((short) VERSION_DEFAULT);
        header.putShort((short) flags);
        header.putShort((short) entry.method);
        header.putShort((short) dosTime);
        header.putShort((short) dosDate);
        header.putInt((int) entry.crc);
        header.putInt(entry.body.length);
        header.putInt(entry.size);
        header.putShort((short) nameBytes.length);
        header.putShort((short) 0);
        header.put(nameBytes);
        out.write(header.array());
        out.write(entry.body);
        position += header.capacity() + entry.body.length;
        return new Record(nameBytes, flags, entry.method, entry.crc, entry.body.length, entry.size, offset);
    }

    /**
     * Central directory header of one entry, with a ZIP64 extra field when its local header offset does not fit 32 bits.
     */
    static byte[] centralHeader(Record record, int dosTime, int dosDate) {
        boolean zip64Offset = record.offset >= ZIP64_LIMIT;
        int extraLength = zip64Offset ? 12 : 0;
        ByteBuffer header = littleEndian(46 + record.nameBytes.length + extraLength);
        header.putInt(CENTRAL_HEADER_SIGNATURE);
        header.putShort((short) VERSION_ZIP64); // version made by
        header.putShort((short) (zip64Offset ? VERSION_ZIP64 : VERSION_DEFAULT));
        header.putShort((short) record.flags);
        header.putShort((short) record.method);
        header.putShort((short) dosTime);
        header.putShort((short) dosDate);
        header.putInt((int) record.crc);
        header.putInt(record.compressedSize);
        header.putInt(record.size);
        header.putShort((short) record.nameBytes.length);
        header.putShort((short) extraLength);
        header.putShort((short) 0); // comment length
        header.putShort((short) 0); // disk number
        header.putShort((short) 0); // internal attributes
        header.putInt(0); // external attributes
        header.putInt(zip64Offset ? (int) ZIP64_LIMIT : (int) record.offset);
        header.put(record.nameBytes);
        if (zip64Offset) {
            // ZIP64 extended information, only the field whose header value overflowed
            header.putShort((short) 0x0001);
            header.putShort((short) 8);
            header.putLong(record.offset);
        }
        return header.array();
    }

    /**
     * End of central directory record, preceded by the ZIP64 end record and locator when a count, size or offset overflows.
     *
     * @param position offset at which these records start, right after the central directory
     */
    static byte[] endRecords(int entryCount, long centralOffset, long centralSize, long position) {
        ByteBuffer records = littleEndian(56 + 20 + 22);
        boolean zip64 = entryCount >= ZIP64_ENTRY_LIMIT || centralOffset >= ZIP64_LIMIT || centralSize >= ZIP64_LIMIT;
        if (zip64) {
            long zip64EndOffset = position;
            ByteBuffer zip64End = records;
            zip64End.putInt(ZIP64_END_SIGNATURE);
            zip64End.putLong(44); // size of the remaining record
            zip64End.putShort((short) VERSION_ZIP64);
            zip64End.putShort((short) VERSION_ZIP64);
            zip64End.putInt(0); // disk number
            zip64End.putInt(0); // disk with the central directory
            zip64End.putLong(entryCount);
            zip64End.putLong(entryCount);
            zip64End.putLong(centralSize);
            zip64End.putLong(centralOffset);
            zip64End.putInt(ZIP64_LOCATOR_SIGNATURE);
            zip64End.putInt(0); // disk with the zip64 end record
            zip64End.putLong(zip64EndOffset);
            zip64End.putInt(1); // total disks
        }
        ByteBuffer end = records;
        end.putInt(END_SIGNATURE);
        end.putShort((short) 0);
        end.putShort((short) 0);
        end.putShort((short) Math.min(entryCount, ZIP64_ENTRY_LIMIT));
        end.putShort((short) Math.min(entryCount, ZIP64_ENTRY_LIMIT));
        end.putInt((int) Math.min(centralSize, ZIP64_LIMIT));
        end.putInt((int) Math.min(centralOffset, ZIP64_LIMIT));
        end.putShort((short) 0); // comment length
        return Arrays.copyOf(records.array(), records.position());
    }

    private void moveToArchive() throws IOException {
        try {
            Files.move(partFile, archiveFile, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(partFile, archiveFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private void checkFailure() throws IOException {
        IOException exception = failure;
        if (exception != null) {
            throw new IOException("3TZ archive writing failed: " + exception.getMessage(), exception);
        }
    }

    /**
     * Builds the "@3dtilesIndex1@" content: per entry the MD5 of its path (16 bytes) and the offset of its local file header
     * (uint64 little endian), sorted ascending by the MD5 read as two little endian uint64 values.
     */
    static byte[] createIndex(Collection<Record> records) {
        MessageDigest md5;
        try {
            md5 = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 is not available.", e);
        }
        List<byte[]> entries = new ArrayList<>(records.size());
        for (Record record : records) {
            ByteBuffer entry = littleEndian(24);
            entry.put(md5.digest(record.nameBytes));
            entry.putLong(record.offset);
            entries.add(entry.array());
        }
        entries.sort(ArchiveTilesetSink::compareIndexEntries);
        ByteBuffer index = ByteBuffer.allocate(entries.size() * 24);
        entries.forEach(index::put);
        return index.array();
    }

    static int compareIndexEntries(byte[] a, byte[] b) {
        ByteBuffer bufferA = ByteBuffer.wrap(a).order(ByteOrder.LITTLE_ENDIAN);
        ByteBuffer bufferB = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);
        int compare = Long.compareUnsigned(bufferA.getLong(0), bufferB.getLong(0));
        return compare != 0 ? compare : Long.compareUnsigned(bufferA.getLong(8), bufferB.getLong(8));
    }

    private static byte[] deflate(byte[] data) {
        Deflater deflater = new Deflater(Deflater.DEFAULT_COMPRESSION, true);
        try {
            deflater.setInput(data);
            deflater.finish();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream(Math.max(64, data.length / 4));
            byte[] buffer = new byte[64 * 1024];
            while (!deflater.finished()) {
                int length = deflater.deflate(buffer);
                outputStream.write(buffer, 0, length);
            }
            return outputStream.toByteArray();
        } finally {
            deflater.end();
        }
    }

    private static String extension(String name) {
        int slash = name.lastIndexOf('/');
        int dot = name.lastIndexOf('.');
        return dot > slash ? name.substring(dot + 1).toLowerCase() : "";
    }

    private static boolean isAscii(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) > 0x7F) {
                return false;
            }
        }
        return true;
    }

    private static ByteBuffer littleEndian(int capacity) {
        return ByteBuffer.allocate(capacity).order(ByteOrder.LITTLE_ENDIAN);
    }

    private record Entry(String name, byte[] body, int method, long crc, int size, int permits) {
    }

    record Record(byte[] nameBytes, int flags, int method, long crc, int compressedSize, int size, long offset) {
    }
}
