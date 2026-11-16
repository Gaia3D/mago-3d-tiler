package com.gaia3d.process.output;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.InputStream;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
