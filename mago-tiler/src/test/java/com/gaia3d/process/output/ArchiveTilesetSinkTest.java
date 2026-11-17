package com.gaia3d.process.output;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.junit.jupiter.api.Assertions.*;

@Tag("default")
class ArchiveTilesetSinkTest {

    @TempDir
    Path tempDir;

    @Test
    void writesZipWithIndexAsLastEntry() throws Exception {
        Path archive = tempDir.resolve("tileset.3tz");
        Map<String, byte[]> expected = new LinkedHashMap<>();
        expected.put("tileset.json", "{\"asset\":{\"version\":\"1.1\"},\"root\":{\"geometricError\":1}}".repeat(20).getBytes(StandardCharsets.UTF_8));
        expected.put("data/R0.b3dm", randomBytes(1, 200_000));
        expected.put("data/R0/1/0/0.glb", randomBytes(2, 5_000));
        expected.put("subtrees/R/0/0/0.json", "{\"buffers\":[]}".getBytes(StandardCharsets.UTF_8));
        expected.put("subtrees/R/0/0/0.bin", new byte[64]);
        expected.put("data/한글.pnts", randomBytes(3, 100));

        ArchiveTilesetSink sink = new ArchiveTilesetSink(archive);
        for (Map.Entry<String, byte[]> entry : expected.entrySet()) {
            sink.write(entry.getKey(), entry.getValue());
        }
        assertTrue(sink.exists("data/R0.b3dm"));
        assertTrue(sink.exists("\\data\\R0.b3dm"));
        assertFalse(sink.exists("data/R1.b3dm"));
        sink.finish();

        assertTrue(Files.exists(archive));
        assertFalse(Files.exists(tempDir.resolve("tileset.3tz.part")));
        try (ZipFile zipFile = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            List<? extends ZipEntry> entries = Collections.list(zipFile.entries());
            assertEquals(expected.size() + 1, entries.size());
            ZipEntry last = entries.getLast();
            assertEquals(ArchiveTilesetSink.INDEX_ENTRY_NAME, last.getName());
            assertEquals(ZipEntry.STORED, last.getMethod());
            assertNull(last.getComment());
            for (Map.Entry<String, byte[]> entry : expected.entrySet()) {
                ZipEntry zipEntry = zipFile.getEntry(entry.getKey());
                assertNotNull(zipEntry, entry.getKey());
                assertArrayEquals(entry.getValue(), read(zipFile, zipEntry), entry.getKey());
            }
            assertEquals(ZipEntry.STORED, zipFile.getEntry("data/R0.b3dm").getMethod());
            assertEquals(ZipEntry.STORED, zipFile.getEntry("data/R0/1/0/0.glb").getMethod());
            assertEquals(ZipEntry.DEFLATED, zipFile.getEntry("tileset.json").getMethod());
        }
        assertIndex(archive, expected.keySet().stream().toList());
    }

    @Test
    void indexIsSortedByMd5AsLittleEndianUint64Pair() throws Exception {
        Path archive = tempDir.resolve("sorted.3tz");
        List<String> names = new ArrayList<>();
        ArchiveTilesetSink sink = new ArchiveTilesetSink(archive);
        for (int i = 0; i < 500; i++) {
            String name = "data/R" + i + ".b3dm";
            names.add(name);
            sink.write(name, randomBytes(i, 10 + i));
        }
        sink.finish();
        assertIndex(archive, names);
    }

    @Test
    void parallelWritesWithSmallQueueKeepEveryEntry() throws Exception {
        Path archive = tempDir.resolve("parallel.3tz");
        // a 4 KB queue makes the callers wait for the writer thread
        ArchiveTilesetSink sink = new ArchiveTilesetSink(archive, 4096);
        Map<String, byte[]> expected = new HashMap<>();
        ExecutorService executorService = Executors.newFixedThreadPool(8);
        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            String name = "data/" + (i % 10) + "/R" + i + ".glb";
            byte[] data = randomBytes(i, 1000 + i * 13);
            synchronized (expected) {
                expected.put(name, data);
            }
            futures.add(executorService.submit(() -> {
                sink.write(name, data);
                return null;
            }));
        }
        for (Future<?> future : futures) {
            future.get();
        }
        executorService.shutdown();
        sink.finish();

        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            assertEquals(expected.size() + 1, zipFile.size());
            for (Map.Entry<String, byte[]> entry : expected.entrySet()) {
                assertArrayEquals(entry.getValue(), read(zipFile, zipFile.getEntry(entry.getKey())), entry.getKey());
            }
        }
        assertIndex(archive, new ArrayList<>(expected.keySet()));
    }

    @Test
    void writesZip64EndRecordForMoreThan65535Entries() throws Exception {
        Path archive = tempDir.resolve("many.3tz");
        int count = 70_000;
        ArchiveTilesetSink sink = new ArchiveTilesetSink(archive);
        for (int i = 0; i < count; i++) {
            sink.write("data/" + (i / 1000) + "/" + i + ".pnts", new byte[]{(byte) i, (byte) (i >> 8), 1});
        }
        sink.finish();

        byte[] bytes = Files.readAllBytes(archive);
        ByteBuffer end = ByteBuffer.wrap(bytes, bytes.length - 22, 22).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0x06054b50, end.getInt());
        end.getInt();
        assertEquals(0xFFFF, Short.toUnsignedInt(end.getShort()));
        ByteBuffer locator = ByteBuffer.wrap(bytes, bytes.length - 42, 20).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0x07064b50, locator.getInt());
        locator.getInt();
        long zip64EndOffset = locator.getLong();
        ByteBuffer zip64End = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0x06064b50, zip64End.getInt((int) zip64EndOffset));
        assertEquals(count + 1, zip64End.getLong((int) zip64EndOffset + 32));

        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            assertEquals(count + 1, zipFile.size());
            assertArrayEquals(new byte[]{(byte) 69_999, (byte) (69_999 >> 8), 1}, read(zipFile, zipFile.getEntry("data/69/69999.pnts")));
        }
        assertEquals(count * 24, readIndex(bytes).length);
    }

    @Test
    void duplicatePathKeepsLastContent() throws Exception {
        Path archive = tempDir.resolve("duplicate.3tz");
        ArchiveTilesetSink sink = new ArchiveTilesetSink(archive);
        sink.write("tileset.json", "{\"a\":1}".getBytes(StandardCharsets.UTF_8));
        sink.write("tileset.json", "{\"a\":2}".getBytes(StandardCharsets.UTF_8));
        sink.finish();
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            assertEquals(2, zipFile.size());
            assertEquals("{\"a\":2}", new String(read(zipFile, zipFile.getEntry("tileset.json")), StandardCharsets.UTF_8));
        }
        assertIndex(archive, List.of("tileset.json"));
    }

    @Test
    void abortDeletesPartFile() throws Exception {
        Path archive = tempDir.resolve("aborted.3tz");
        ArchiveTilesetSink sink = new ArchiveTilesetSink(archive);
        sink.write("tileset.json", "{}".getBytes(StandardCharsets.UTF_8));
        assertTrue(Files.exists(tempDir.resolve("aborted.3tz.part")));
        sink.abort();
        assertFalse(Files.exists(tempDir.resolve("aborted.3tz.part")));
        assertFalse(Files.exists(archive));
        assertThrows(IOException.class, () -> sink.write("data/R0.b3dm", new byte[8]));
        assertThrows(IOException.class, sink::finish);
    }

    @Test
    void rejectsReservedNames() throws Exception {
        ArchiveTilesetSink sink = new ArchiveTilesetSink(tempDir.resolve("names.3tz"));
        assertThrows(IOException.class, () -> sink.write(ArchiveTilesetSink.INDEX_ENTRY_NAME, new byte[1]));
        assertThrows(IOException.class, () -> sink.write("nested.3tz", new byte[1]));
        sink.abort();
    }

    @Test
    void directorySinkWritesNestedFiles() throws Exception {
        DirectoryTilesetSink sink = new DirectoryTilesetSink(tempDir);
        byte[] data = randomBytes(7, 32);
        sink.write("data/R0/1/0/0.glb", data);
        assertTrue(sink.exists("data/R0/1/0/0.glb"));
        assertArrayEquals(data, Files.readAllBytes(tempDir.resolve("data/R0/1/0/0.glb")));
    }

    @Test
    void koreanEntryNameSetsUtf8FlagAndIndexHashesUtf8Bytes() throws Exception {
        Path archive = tempDir.resolve("korean.3tz");
        String korean = "data/영등포구/건물_01.b3dm";
        String ascii = "data/R0.b3dm";
        byte[] data = randomBytes(11, 300);
        ArchiveTilesetSink sink = new ArchiveTilesetSink(archive);
        sink.write(ascii, randomBytes(12, 40));
        sink.write(korean, data);
        sink.finish();

        byte[] bytes = Files.readAllBytes(archive);
        Map<String, Long> offsets = indexOffsets(bytes);
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        String koreanHash = hex(md5.digest(korean.getBytes(StandardCharsets.UTF_8)));
        assertTrue(offsets.containsKey(koreanHash), "index holds the MD5 of the UTF-8 name");
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int koreanOffset = offsets.get(koreanHash).intValue();
        assertEquals(1 << 11, buffer.getShort(koreanOffset + 6) & (1 << 11), "local header UTF-8 flag");
        byte[] nameBytes = korean.getBytes(StandardCharsets.UTF_8);
        assertEquals(nameBytes.length, Short.toUnsignedInt(buffer.getShort(koreanOffset + 26)));
        assertArrayEquals(nameBytes, Arrays.copyOfRange(bytes, koreanOffset + 30, koreanOffset + 30 + nameBytes.length));
        int asciiOffset = offsets.get(hex(md5.digest(ascii.getBytes(StandardCharsets.UTF_8)))).intValue();
        assertEquals(0, buffer.getShort(asciiOffset + 6) & (1 << 11), "ASCII names keep the flag clear");

        // central directory header of the Korean entry carries the flag as well
        int central = indexOf(bytes, nameBytes, koreanOffset + 30 + nameBytes.length) - 46;
        assertEquals(0x02014b50, buffer.getInt(central));
        assertEquals(1 << 11, buffer.getShort(central + 8) & (1 << 11), "central header UTF-8 flag");

        try (ZipFile zipFile = new ZipFile(archive.toFile(), StandardCharsets.UTF_8)) {
            assertArrayEquals(data, read(zipFile, zipFile.getEntry(korean)));
        }
        assertIndex(archive, List.of(ascii, korean));
    }

    @Test
    void zip64RecordsForOffsetsAbove4Gb() {
        long offset = 5L * 1024 * 1024 * 1024 + 123;
        byte[] name = "data/R0.b3dm".getBytes(StandardCharsets.UTF_8);
        ArchiveTilesetSink.Record record = new ArchiveTilesetSink.Record(name, 0, 0, 0x12345678L, 1000, 1000, offset);

        ByteBuffer central = ByteBuffer.wrap(ArchiveTilesetSink.centralHeader(record, 0, 0)).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(46 + name.length + 12, central.capacity());
        assertEquals(0x02014b50, central.getInt(0));
        assertEquals(45, central.getShort(6), "version needed to extract is 4.5 for ZIP64");
        assertEquals(12, central.getShort(30), "extra field length");
        assertEquals(0xFFFFFFFF, central.getInt(42), "offset field is the ZIP64 sentinel");
        assertEquals(1000, central.getInt(20), "sizes that fit stay in the header");
        assertEquals(0x0001, central.getShort(46 + name.length));
        assertEquals(8, central.getShort(46 + name.length + 2), "only the offset is in the ZIP64 extra field");
        assertEquals(offset, central.getLong(46 + name.length + 4));

        ArchiveTilesetSink.Record small = new ArchiveTilesetSink.Record(name, 0, 0, 0, 10, 10, 0xFFFFFFFEL);
        ByteBuffer smallCentral = ByteBuffer.wrap(ArchiveTilesetSink.centralHeader(small, 0, 0)).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(46 + name.length, smallCentral.capacity());
        assertEquals(0xFFFFFFFE, smallCentral.getInt(42));

        long centralOffset = offset + 2000;
        long centralSize = 500;
        long endPosition = centralOffset + centralSize;
        ByteBuffer end = ByteBuffer.wrap(ArchiveTilesetSink.endRecords(3, centralOffset, centralSize, endPosition)).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(56 + 20 + 22, end.capacity());
        assertEquals(0x06064b50, end.getInt(0));
        assertEquals(44, end.getLong(4));
        assertEquals(3, end.getLong(24));
        assertEquals(3, end.getLong(32));
        assertEquals(centralSize, end.getLong(40));
        assertEquals(centralOffset, end.getLong(48));
        assertEquals(0x07064b50, end.getInt(56));
        assertEquals(endPosition, end.getLong(64), "locator points at the ZIP64 end record");
        assertEquals(1, end.getInt(72));
        assertEquals(0x06054b50, end.getInt(76));
        assertEquals(3, end.getShort(76 + 10));
        assertEquals(centralSize, end.getInt(76 + 12));
        assertEquals(0xFFFFFFFF, end.getInt(76 + 16), "EOCD offset is the ZIP64 sentinel");

        assertEquals(22, ArchiveTilesetSink.endRecords(3, 1000, 200, 1200).length, "no ZIP64 records below the limits");

        byte[] index = ArchiveTilesetSink.createIndex(List.of(record));
        assertEquals(24, index.length);
        assertEquals(offset, ByteBuffer.wrap(index).order(ByteOrder.LITTLE_ENDIAN).getLong(16), "index offset is a uint64");
    }

    @Test
    void entryLargerThanQueueBudget() throws Exception {
        Path archive = tempDir.resolve("large.3tz");
        ArchiveTilesetSink sink = new ArchiveTilesetSink(archive, 4096);
        byte[] large = randomBytes(21, 3 * 1024 * 1024);
        byte[] json = "{\"a\":1}".repeat(100_000).getBytes(StandardCharsets.UTF_8);
        sink.write("data/large.glb", large);
        sink.write("data/small.pnts", randomBytes(22, 10));
        sink.write("tileset.json", json);
        sink.finish();
        try (ZipFile zipFile = new ZipFile(archive.toFile())) {
            assertArrayEquals(large, read(zipFile, zipFile.getEntry("data/large.glb")));
            assertArrayEquals(json, read(zipFile, zipFile.getEntry("tileset.json")));
        }
        assertIndex(archive, List.of("data/large.glb", "data/small.pnts", "tileset.json"));
    }

    @Test
    void writerErrorFailsFinishAndDeletesPart() throws Exception {
        Path archive = tempDir.resolve("error.3tz");
        // entries bigger than the 1 MB write buffer reach the file stream at once, where the error is thrown
        ArchiveTilesetSink sink = new ArchiveTilesetSink(archive, 64L * 1024 * 1024, out -> new FailingOutputStream(out, null));
        sink.write("data/R0.glb", randomBytes(31, 2 * 1024 * 1024));
        IOException exception = assertThrows(IOException.class, sink::finish);
        assertTrue(hasCause(exception, OutOfMemoryError.class), "the writer error is the cause: " + exception);
        assertFalse(Files.exists(tempDir.resolve("error.3tz.part")));
        assertFalse(Files.exists(archive));
        assertThrows(IOException.class, () -> sink.write("data/R1.glb", new byte[8]));
    }

    @Test
    void writerErrorReleasesBlockedProducer() throws Exception {
        Path archive = tempDir.resolve("blocked.3tz");
        CountDownLatch release = new CountDownLatch(1);
        ArchiveTilesetSink sink = new ArchiveTilesetSink(archive, 4096, out -> new FailingOutputStream(out, release));
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            // takes the whole queue budget, the writer blocks while writing it
            sink.write("data/R0.glb", randomBytes(41, 2 * 1024 * 1024));
            Future<?> blocked = executorService.submit(() -> {
                sink.write("data/R1.glb", randomBytes(42, 2 * 1024 * 1024));
                return null;
            });
            Thread.sleep(500);
            assertFalse(blocked.isDone(), "the producer waits for queue space");
            release.countDown();
            ExecutionException exception = assertThrows(ExecutionException.class, () -> blocked.get(10, TimeUnit.SECONDS));
            assertInstanceOf(IOException.class, exception.getCause());
            assertThrows(IOException.class, sink::finish);
            assertFalse(Files.exists(tempDir.resolve("blocked.3tz.part")));
        } finally {
            release.countDown();
            executorService.shutdownNow();
        }
    }

    @Test
    void abortReleasesBlockedProducer() throws Exception {
        Path archive = tempDir.resolve("abort-blocked.3tz");
        CountDownLatch release = new CountDownLatch(1);
        ArchiveTilesetSink sink = new ArchiveTilesetSink(archive, 4096, out -> new FailingOutputStream(out, release));
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        try {
            sink.write("data/R0.glb", randomBytes(51, 2 * 1024 * 1024));
            Future<?> blocked = executorService.submit(() -> {
                sink.write("data/R1.glb", randomBytes(52, 100));
                return null;
            });
            Thread.sleep(500);
            assertFalse(blocked.isDone());
            sink.abort();
            ExecutionException exception = assertThrows(ExecutionException.class, () -> blocked.get(10, TimeUnit.SECONDS));
            assertInstanceOf(IOException.class, exception.getCause());
            assertFalse(Files.exists(tempDir.resolve("abort-blocked.3tz.part")));
            assertFalse(Files.exists(archive));
        } finally {
            release.countDown();
            executorService.shutdownNow();
        }
    }

    @Test
    void writesAfterFinishThrow() throws Exception {
        Path archive = tempDir.resolve("finished.3tz");
        ArchiveTilesetSink sink = new ArchiveTilesetSink(archive);
        sink.write("tileset.json", "{}".getBytes(StandardCharsets.UTF_8));
        sink.finish();
        assertThrows(IOException.class, () -> sink.write("data/R0.b3dm", new byte[8]));
        sink.finish();
        sink.abort();
        assertTrue(Files.exists(archive), "abort after finish keeps the archive");
    }

    @Test
    void failedRenameKeepsCompletePart() throws Exception {
        Path archive = tempDir.resolve("locked.3tz");
        // a non-empty directory with the archive name cannot be replaced, like an archive held open by a viewer on Windows
        Files.createDirectories(archive.resolve("child"));
        ArchiveTilesetSink sink = new ArchiveTilesetSink(archive);
        byte[] data = randomBytes(61, 1000);
        sink.write("data/R0.b3dm", data);
        IOException exception = assertThrows(IOException.class, sink::finish);
        Path part = tempDir.resolve("locked.3tz.part");
        assertTrue(exception.getMessage().contains(part.toAbsolutePath().toString()), exception.getMessage());
        sink.abort();
        assertTrue(Files.exists(part), "the complete archive is kept");
        try (ZipFile zipFile = new ZipFile(part.toFile())) {
            assertArrayEquals(data, read(zipFile, zipFile.getEntry("data/R0.b3dm")));
            assertNotNull(zipFile.getEntry(ArchiveTilesetSink.INDEX_ENTRY_NAME));
        }
        assertIndex(part, List.of("data/R0.b3dm"));
        assertThrows(IOException.class, () -> sink.write("data/R1.b3dm", new byte[8]));
        assertThrows(IOException.class, sink::finish);
    }

    /**
     * Throws an OutOfMemoryError on the first large write. With a latch it first blocks until the latch is released,
     * an interrupt (abort) ends the wait with an InterruptedIOException.
     */
    private static final class FailingOutputStream extends FilterOutputStream {
        private final CountDownLatch release;
        private boolean failed = false;

        FailingOutputStream(OutputStream out, CountDownLatch release) {
            super(out);
            this.release = release;
        }

        @Override
        public void write(byte[] b, int off, int len) throws IOException {
            if (!failed && len > 1024 * 1024) {
                failed = true;
                if (release != null) {
                    try {
                        release.await();
                    } catch (InterruptedException e) {
                        throw new InterruptedIOException("interrupted");
                    }
                }
                throw new OutOfMemoryError("simulated");
            }
            out.write(b, off, len);
        }
    }

    private static boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            if (type.isInstance(cause)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Long> indexOffsets(byte[] bytes) throws IOException {
        byte[] index = readIndex(bytes);
        Map<String, Long> offsets = new HashMap<>();
        ByteBuffer buffer = ByteBuffer.wrap(index).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < index.length; i += 24) {
            offsets.put(hex(Arrays.copyOfRange(index, i, i + 16)), buffer.getLong(i + 16));
        }
        return offsets;
    }

    private static int indexOf(byte[] bytes, byte[] pattern, int from) {
        for (int i = from; i <= bytes.length - pattern.length; i++) {
            if (Arrays.equals(bytes, i, i + pattern.length, pattern, 0, pattern.length)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Checks the "@3dtilesIndex1@" entry against the 3TZ spec: one 24 byte record (MD5 of the path, uint64 LE offset of the local
     * file header) per file, ascending by the MD5 read as two LE uint64, each offset pointing at the local header of that path.
     */
    private static void assertIndex(Path archive, List<String> names) throws Exception {
        byte[] bytes = Files.readAllBytes(archive);
        byte[] index = readIndex(bytes);
        assertEquals(names.size() * 24, index.length);

        Map<String, String> nameByHash = new HashMap<>();
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        for (String name : names) {
            nameByHash.put(hex(md5.digest(name.getBytes(StandardCharsets.UTF_8))), name);
        }
        ByteBuffer buffer = ByteBuffer.wrap(index).order(ByteOrder.LITTLE_ENDIAN);
        BigInteger previous = BigInteger.valueOf(-1);
        for (int i = 0; i < names.size(); i++) {
            byte[] hash = Arrays.copyOfRange(index, i * 24, i * 24 + 16);
            BigInteger key = unsigned(buffer.getLong(i * 24)).shiftLeft(64).or(unsigned(buffer.getLong(i * 24 + 8)));
            assertTrue(key.compareTo(previous) > 0, "index is not sorted at " + i);
            previous = key;

            String name = nameByHash.get(hex(hash));
            assertNotNull(name, "unknown hash at " + i);
            long offset = buffer.getLong(i * 24 + 16);
            ByteBuffer header = ByteBuffer.wrap(bytes, (int) offset, 30).order(ByteOrder.LITTLE_ENDIAN);
            assertEquals(0x04034b50, header.getInt(), name);
            header.getShort();
            assertEquals(0, header.getShort() & 0x08, "no data descriptor: " + name);
            header.position(header.position() + 2 + 4 + 4);
            assertTrue(header.getInt() > 0, "compressed size is set: " + name);
            header.getInt();
            int nameLength = Short.toUnsignedInt(header.getShort());
            assertEquals(name, new String(bytes, (int) offset + 30, nameLength, StandardCharsets.UTF_8));
        }
    }

    private static byte[] readIndex(byte[] bytes) throws IOException {
        // the last central directory header is the index: find it from the end, then read its stored data
        int position = bytes.length - 46;
        while (position >= 0 && ByteBuffer.wrap(bytes, position, 4).order(ByteOrder.LITTLE_ENDIAN).getInt() != 0x02014b50) {
            position--;
        }
        ByteBuffer central = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, central.getShort(position + 10), "index is stored");
        int size = central.getInt(position + 20);
        int nameLength = Short.toUnsignedInt(central.getShort(position + 28));
        assertEquals(ArchiveTilesetSink.INDEX_ENTRY_NAME, new String(bytes, position + 46, nameLength, StandardCharsets.UTF_8));
        long offset = Integer.toUnsignedLong(central.getInt(position + 42));
        int localNameLength = Short.toUnsignedInt(central.getShort((int) offset + 26));
        int localExtraLength = Short.toUnsignedInt(central.getShort((int) offset + 28));
        int start = (int) offset + 30 + localNameLength + localExtraLength;
        return Arrays.copyOfRange(bytes, start, start + size);
    }

    private static BigInteger unsigned(long value) {
        return new BigInteger(Long.toUnsignedString(value));
    }

    private static String hex(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private static byte[] read(ZipFile zipFile, ZipEntry entry) throws IOException {
        try (InputStream inputStream = zipFile.getInputStream(entry)) {
            return inputStream.readAllBytes();
        }
    }

    private static byte[] randomBytes(long seed, int length) {
        byte[] bytes = new byte[length];
        new Random(seed).nextBytes(bytes);
        return bytes;
    }
}
