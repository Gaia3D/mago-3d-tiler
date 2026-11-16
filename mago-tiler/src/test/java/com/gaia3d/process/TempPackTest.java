package com.gaia3d.process;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.LevelOfDetail;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.converter.pointcloud.GaiaLasPoint;
import com.gaia3d.converter.pointcloud.GaiaPointCloud;
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
