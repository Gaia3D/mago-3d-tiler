package com.gaia3d.process;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.LevelOfDetail;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.pointcloud.GaiaLasPoint;
import com.gaia3d.converter.pointcloud.GaiaPointCloud;
import com.gaia3d.process.preprocess.GaiaMinimization;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Tag("default")
class TempPackTest {

    @AfterEach
    void closePacks() {
        TempPack.closeAll();
    }

    @Test
    void readsBackEntriesWrittenByManyThreads(@TempDir Path dir) throws Exception {
        TempPack pack = TempPack.of(dir.resolve("pack"));
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<TempPack.Entry>> futures = new ArrayList<>();
        for (int i = 0; i < 200; i++) {
            byte[] bytes = bytesOf(i);
            futures.add(executor.submit(() -> pack.write(outputStream -> outputStream.write(bytes))));
        }
        executor.shutdown();

        for (int i = 0; i < futures.size(); i++) {
            TempPack.Entry entry = futures.get(i).get();
            try (InputStream inputStream = pack.read(entry)) {
                assertArrayEquals(bytesOf(i), inputStream.readAllBytes());
            }
        }
        try (Stream<Path> files = Files.list(dir.resolve("pack"))) {
            long packCount = files.count();
            assertTrue(packCount >= 1 && packCount <= 4, "one pack per writer thread: " + packCount);
        }
    }

    @Test
    void dropsThePartialEntryOfAFailedWrite(@TempDir Path dir) throws IOException {
        TempPack pack = TempPack.of(dir.resolve("pack"));
        TempPack.Entry first = pack.write(outputStream -> outputStream.write(bytesOf(1)));
        assertThrows(IOException.class, () -> pack.write(outputStream -> {
            outputStream.write(new byte[100_000]);
            throw new IOException("failed");
        }));
        TempPack.Entry second = pack.write(outputStream -> outputStream.write(bytesOf(2)));

        assertEquals(first.offset() + first.length(), second.offset());
        try (InputStream inputStream = pack.read(second)) {
            assertArrayEquals(bytesOf(2), inputStream.readAllBytes());
        }
    }

    @Test
    void closeAllReleasesThePackFiles(@TempDir Path dir) throws IOException {
        Path packDirectory = dir.resolve("temp").resolve("pack");
        TempPack.of(packDirectory).write(outputStream -> outputStream.write(bytesOf(3)));
        TempPack.closeAll();
        FileUtils.deleteDirectory(dir.resolve("temp").toFile());
        assertFalse(Files.exists(packDirectory));
    }

    @Test
    void setReferencesTheOriginalTextureAndWritesOnlyResizedImages(@TempDir Path dir) throws IOException {
        // the texture lives in a subfolder of the model and is referenced relatively
        Path model = Files.createDirectories(dir.resolve("model"));
        Path texture = Files.createDirectories(model.resolve("textures")).resolve("wall.png");
        ImageIO.write(new BufferedImage(200, 100, BufferedImage.TYPE_INT_ARGB), "png", texture.toFile());
        Path temp = dir.resolve("temp");
        Path images = temp.resolve("images");

        GaiaSet set = createSet(model, "textures/wall.png");
        TempTextures.writeLodImages(TempTextures.reference(set.getMaterials().getFirst()), images, List.of(LevelOfDetail.LOD0, LevelOfDetail.LOD1, LevelOfDetail.LOD2));
        TempPack.Entry entry = TempPack.of(temp.resolve("pack")).write(outputStream -> {
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(set);
            objectOutputStream.flush();
        });
        TileInfo tileInfo = TileInfo.builder().tempEntry(entry).build();
        tileInfo.maximize();

        GaiaTexture read = tileInfo.getSet().getMaterials().getFirst().getTextures().get(TextureType.DIFFUSE).getFirst();
        assertEquals(texture.toFile().getCanonicalPath(), new File(read.getFullPath()).getCanonicalPath());
        List<String> written;
        try (Stream<Path> files = Files.walk(images)) {
            written = files.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).sorted().toList();
        }
        assertEquals(List.of("1_wall.png", "2_wall.png"), written); // no copy of the original

        TempTextures.loadLodImage(read, images, LevelOfDetail.LOD1);
        assertEquals(100, read.getBufferedImage(LevelOfDetail.LOD1).getWidth()); // the written image, a resize on the fly gives a power of two
        assertEquals(200, read.getBufferedImage(LevelOfDetail.LOD0).getWidth());
    }

    @Test
    void sameImageNamesInDifferentFoldersDoNotCollide(@TempDir Path dir) throws IOException {
        Path a = Files.createDirectories(dir.resolve("a"));
        Path b = Files.createDirectories(dir.resolve("b"));
        ImageIO.write(new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB), "png", a.resolve("tex.png").toFile());
        ImageIO.write(new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB), "png", b.resolve("tex.png").toFile());
        Path images = dir.resolve("temp").resolve("images");
        List<LevelOfDetail> lods = List.of(LevelOfDetail.LOD0, LevelOfDetail.LOD1);

        GaiaTexture textureA = TempTextures.reference(createSet(a, "tex.png").getMaterials().getFirst());
        GaiaTexture textureB = TempTextures.reference(createSet(b, "tex.png").getMaterials().getFirst());
        TempTextures.writeLodImages(textureA, images, lods);
        TempTextures.writeLodImages(textureB, images, lods);
        TempTextures.loadLodImage(textureA, images, LevelOfDetail.LOD1);
        TempTextures.loadLodImage(textureB, images, LevelOfDetail.LOD1);
        assertEquals(128, textureA.getWidth());
        assertEquals(64, textureB.getWidth());
    }

    @Test
    void pointCloudNodeRoundTripsThroughThePack(@TempDir Path dir) {
        TempPack pack = TempPack.of(dir.resolve("pack"));
        List<GaiaLasPoint> points = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            points.add(GaiaLasPoint.builder().x(i).y(i * 2.0).z(-i).r((byte) i).intensity((char) i).classification((short) 2).build());
        }
        GaiaPointCloud node = new GaiaPointCloud();
        node.setLasPoints(points);
        node.minimize(pack);
        GaiaPointCloud empty = new GaiaPointCloud();
        empty.setLasPoints(new ArrayList<>());
        empty.minimize(pack);
        assertNull(node.getLasPoints());

        node.maximize(true);
        empty.maximize(true);
        assertEquals(1000, node.getPointCount());
        assertEquals(999.0, node.getLasPoints().get(999).getX());
        assertEquals(1998.0, node.getLasPoints().get(999).getY());
        assertEquals((short) 2, node.getLasPoints().get(5).getClassification());
        assertTrue(empty.getLasPoints().isEmpty());
    }

    @Test
    void ofAfterCloseAllGivesANewOpenPack(@TempDir Path dir) throws IOException {
        Path packDirectory = dir.resolve("pack");
        TempPack first = TempPack.of(packDirectory);
        TempPack.Entry entry = first.write(outputStream -> outputStream.write(bytesOf(4)));
        TempPack.closeAll();
        assertThrows(IOException.class, () -> first.write(outputStream -> outputStream.write(bytesOf(5))));

        TempPack second = TempPack.of(packDirectory);
        assertNotSame(first, second);
        try (InputStream inputStream = second.read(entry)) {
            assertArrayEquals(bytesOf(4), inputStream.readAllBytes());
        }
        second.write(outputStream -> outputStream.write(bytesOf(6)));
        assertSame(second, TempPack.of(packDirectory));
        TempPack.closeAll();
        FileUtils.deleteDirectory(packDirectory.toFile()); // closed by the second call
        assertFalse(Files.exists(packDirectory));
    }

    @Test
    void indexedPackListsItsEntries(@TempDir Path dir) throws IOException {
        Path packDirectory = dir.resolve("pack");
        TempPack.Entry first;
        TempPack.Entry second;
        try (TempPack pack = new TempPack(packDirectory, true)) {
            first = pack.write("id-1", "a,b.3ds", outputStream -> outputStream.write(bytesOf(1)));
            second = pack.write("id-2", null, outputStream -> outputStream.write(bytesOf(2)));
        }
        List<String> lines = Files.readAllLines(packDirectory.resolve(TempPack.INDEX_FILE));
        assertEquals(List.of("id,name,pack,offset,length",
                "id-1,\"a,b.3ds\"," + first.pack().getFileName() + ",0," + first.length(),
                "id-2,," + second.pack().getFileName() + "," + second.offset() + "," + second.length()), lines);
    }

    @Test
    void twoThreadsWritingTheSameLodImage(@TempDir Path dir) throws Exception {
        Path model = Files.createDirectories(dir.resolve("model"));
        ImageIO.write(new BufferedImage(512, 256, BufferedImage.TYPE_INT_RGB), "jpg", model.resolve("wall.jpg").toFile());
        List<LevelOfDetail> lods = List.of(LevelOfDetail.LOD0, LevelOfDetail.LOD1, LevelOfDetail.LOD2, LevelOfDetail.LOD3);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 10; round++) {
                Path images = dir.resolve("images-" + round);
                GaiaTexture textureA = TempTextures.reference(createSet(model, "wall.jpg").getMaterials().getFirst());
                GaiaTexture textureB = TempTextures.reference(createSet(model, "wall.jpg").getMaterials().getFirst());
                CountDownLatch start = new CountDownLatch(1);
                Future<?> a = executor.submit(() -> {
                    start.await();
                    TempTextures.writeLodImages(textureA, images, lods);
                    return null;
                });
                Future<?> b = executor.submit(() -> {
                    start.await();
                    TempTextures.writeLodImages(textureB, images, lods);
                    return null;
                });
                start.countDown();
                a.get();
                b.get();
                for (LevelOfDetail lod : lods.subList(1, lods.size())) {
                    BufferedImage written = ImageIO.read(TempTextures.lodImage(images, textureA, lod).toFile());
                    assertNotNull(written, "complete image for " + lod);
                    assertEquals((int) Math.round(512 * lod.getTextureScale()), written.getWidth());
                }
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void failedLodImagesKeepTheSet(@TempDir Path dir) throws IOException {
        Path model = Files.createDirectories(dir.resolve("model"));
        Path image = model.resolve("wall.png");
        ImageIO.write(new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB), "png", image.toFile());
        Path temp = Files.createDirectories(dir.resolve("temp"));
        Files.writeString(temp.resolve("images"), "a file where the images folder should be");

        // writeLodImages logs instead of throwing
        GaiaTexture texture = TempTextures.reference(createSet(model, "wall.png").getMaterials().getFirst());
        assertDoesNotThrow(() -> TempTextures.writeLodImages(texture, temp.resolve("images"), List.of(LevelOfDetail.LOD0, LevelOfDetail.LOD1)));

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        int minLod = globalOptions.getMinLod();
        int maxLod = globalOptions.getMaxLod();
        try {
            globalOptions.setMinLod(0);
            globalOptions.setMaxLod(2);
            TileInfo tileInfo = minimize(createSet(model, "wall.png"), temp);
            assertNotNull(tileInfo.getTempEntry());
            tileInfo.maximize();
            GaiaTexture read = tileInfo.getSet().getMaterials().getFirst().getTextures().get(TextureType.DIFFUSE).getFirst();
            assertEquals(image.toFile().getCanonicalPath(), new File(read.getFullPath()).getCanonicalPath());
        } finally {
            globalOptions.setMinLod(minLod);
            globalOptions.setMaxLod(maxLod);
        }
    }

    @Test
    void missingTextureIsRemovedAndTheSetKept(@TempDir Path dir) {
        Path model = dir.resolve("model");
        GaiaSet set = createSet(model, "missing.png");
        assertNull(TempTextures.reference(set.getMaterials().getFirst()));
        assertTrue(set.getMaterials().getFirst().getTextures().get(TextureType.DIFFUSE).isEmpty());

        TileInfo tileInfo = minimize(createSet(model, "missing.png"), dir.resolve("temp"));
        assertNotNull(tileInfo.getTempEntry());
        tileInfo.maximize();
        assertFalse(tileInfo.getSet().getMaterials().getFirst().hasTextures());
    }

    @Test
    void embeddedTexturesAreCopiedPerSet(@TempDir Path dir) throws IOException {
        // the loader extracts the images of every model of a folder to the same embedded_textures folder
        Path model = Files.createDirectories(dir.resolve("model"));
        Path embedded = Files.createDirectories(model.resolve("embedded_textures")).resolve("wall.png");
        Path images = dir.resolve("temp").resolve("images");
        List<LevelOfDetail> lods = List.of(LevelOfDetail.LOD0, LevelOfDetail.LOD1);

        ImageIO.write(new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB), "png", embedded.toFile());
        GaiaSet setA = createSet(model.resolve("embedded_textures"), "wall.png");
        GaiaTexture textureA = TempTextures.referenceTextures(setA, images).getFirst();
        TempTextures.writeLodImages(textureA, images, lods);

        ImageIO.write(new BufferedImage(128, 128, BufferedImage.TYPE_INT_RGB), "png", embedded.toFile()); // the next model
        GaiaSet setB = createSet(model.resolve("embedded_textures"), "wall.png");
        GaiaTexture textureB = TempTextures.referenceTextures(setB, images).getFirst();
        TempTextures.writeLodImages(textureB, images, lods);

        assertTrue(Path.of(textureA.getFullPath()).startsWith(images.toAbsolutePath().normalize()));
        assertNotEquals(textureA.getFullPath(), textureB.getFullPath());
        assertEquals(256, ImageIO.read(new File(textureA.getFullPath())).getWidth());
        assertEquals(128, ImageIO.read(new File(textureB.getFullPath())).getWidth());
        TempTextures.loadLodImage(textureA, images, LevelOfDetail.LOD1);
        TempTextures.loadLodImage(textureB, images, LevelOfDetail.LOD1);
        assertEquals(128, textureA.getWidth());
        assertEquals(64, textureB.getWidth());

        // an input image is still referenced where it is
        Path input = model.resolve("roof.png");
        ImageIO.write(new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB), "png", input.toFile());
        GaiaTexture inputTexture = TempTextures.referenceTextures(createSet(model, "roof.png"), images).getFirst();
        assertEquals(input.toFile().getCanonicalPath(), new File(inputTexture.getFullPath()).getCanonicalPath());
    }

    private static TileInfo minimize(GaiaSet set, Path temp) {
        GaiaScene scene = new GaiaScene(set);
        scene.setOriginalPath(temp.resolve("model.3ds"));
        TileInfo tileInfo = TileInfo.builder().scene(scene).tempPath(temp).build();
        new GaiaMinimization().run(tileInfo);
        return tileInfo;
    }

    private static GaiaSet createSet(Path parent, String texturePath) {
        GaiaTexture texture = new GaiaTexture();
        texture.setType(TextureType.DIFFUSE);
        texture.setParentPath(parent.toString());
        texture.setPath(texturePath);
        GaiaMaterial material = new GaiaMaterial();
        material.getTextures().put(TextureType.DIFFUSE, new ArrayList<>(List.of(texture)));
        GaiaSet set = new GaiaSet();
        set.setBufferDataList(new ArrayList<>());
        set.setMaterials(new ArrayList<>(List.of(material)));
        set.setAttribute(new GaiaAttribute());
        set.setProjectName("model");
        return set;
    }

    private static byte[] bytesOf(int seed) {
        byte[] bytes = new byte[1000 + seed * 37];
        new Random(seed).nextBytes(bytes);
        return bytes;
    }
}
