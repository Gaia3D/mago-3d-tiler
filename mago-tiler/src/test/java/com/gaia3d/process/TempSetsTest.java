package com.gaia3d.process;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.TextureType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@Tag("default")
class TempSetsTest {

    @AfterEach
    void clear() {
        TempPack.closeAll();
        TempSets.clear();
    }

    @Test
    void packedSetKeepsTheOriginalTexturePathAndCopiesNothing(@TempDir Path dir) throws IOException {
        Path model = Files.createDirectories(dir.resolve("model"));
        Path image = Files.createDirectories(model.resolve("textures")).resolve("wall.jpg");
        ImageIO.write(new BufferedImage(64, 32, BufferedImage.TYPE_INT_RGB), "jpg", image.toFile());
        Path temp = dir.resolve("temp");

        GaiaSet set = createSet(model.toString(), "textures/wall.jpg");
        TempTextures.reference(set.getMaterials().getFirst());
        Path name = temp.resolve("sets").resolve("model.tmp");
        assertEquals(name, TempSets.write(set, temp.resolve("pack"), name));

        assertFalse(Files.exists(name), "the set lives in the pack, not in a file of its own");
        assertTrue(TempSets.exists(name));
        GaiaTexture read = diffuse(TempSets.read(name));
        assertEquals(image.toFile().getCanonicalPath(), new File(read.getFullPath()).getCanonicalPath());
        assertNotNull(read.getBufferedImage());
        assertEquals(List.of("pack-0.bin"), filesIn(temp), "no copy of the texture in temp");
    }

    @Test
    void inputImageIsReferencedAndLoaderImageIsCopied(@TempDir Path dir) throws IOException {
        Path model = Files.createDirectories(dir.resolve("model"));
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", model.resolve("input.png").toFile());
        Path embedded = Files.createDirectories(model.resolve("embedded_textures"));
        ImageIO.write(new BufferedImage(16, 4, BufferedImage.TYPE_INT_RGB), "png", embedded.resolve("0.png").toFile());
        GaiaSet set = createSet(model.toString(), "input.png");
        GaiaMaterial embeddedMaterial = createSet(model.toString(), "embedded_textures/0.png").getMaterials().getFirst();
        set.getMaterials().add(embeddedMaterial);
        set.getMaterials().add(createSet(model.toString(), "missing.png").getMaterials().getFirst());
        Path images = dir.resolve("temp").resolve("images");

        TempTextures.referenceTextures(set, images);

        assertEquals(model.resolve("input.png").toFile().getCanonicalPath(), new File(diffuse(set).getFullPath()).getCanonicalPath());
        GaiaTexture copied = embeddedMaterial.getTextures().get(TextureType.DIFFUSE).getFirst();
        assertTrue(copied.getFullPath().startsWith(images.toString()), copied.getFullPath());
        assertEquals("0.png", copied.getPath());
        // a later model overwrites the shared loader image, the copy keeps the image of this model
        ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", embedded.resolve("0.png").toFile());
        assertEquals(new Dimension(16, 4), TempTextures.imageSize(Path.of(copied.getFullPath())));
        assertEquals(List.of("0.png"), filesIn(images));
        // the texture of the missing image is removed, the material and the geometry are kept
        assertTrue(set.getMaterials().get(2).getTextures().get(TextureType.DIFFUSE).isEmpty());
    }

    @Test
    void writeFileDoesNotReplaceAnExistingFile(@TempDir Path dir) throws IOException {
        Path file = Files.writeString(dir.resolve("existing.tmp"), "keep");
        assertThrows(IOException.class, () -> TempSets.writeFile(createSet("p", "a.png"), file));
        assertEquals("keep", Files.readString(file));
    }

    @Test
    void newImagePathIsNotRewritten(@TempDir Path dir) throws IOException {
        // a written image (atlas) next to the set, read back as it was written, not as <dir of the set>/images/<name>
        Path cutImages = Files.createDirectories(dir.resolve("cutTemp").resolve("lod0").resolve("id").resolve("images"));
        GaiaSet set = createSet(cutImages.toString(), "wall_atlas_image.png");
        Path name = dir.resolve("cutTemp").resolve("lod0").resolve("id.tmp");
        TempSets.write(set, dir.resolve("pack"), name);

        GaiaTexture read = diffuse(TempSets.read(name));
        assertEquals(cutImages.toString(), read.getParentPath());
        assertEquals("wall_atlas_image.png", read.getPath());
    }

    @Test
    void readsBackSetsWrittenByManyThreads(@TempDir Path dir) throws Exception {
        Path pack = dir.resolve("pack");
        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Future<Path>> futures = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            int index = i;
            futures.add(executor.submit(() -> {
                GaiaSet set = createSet("parent" + index, "image" + index + ".png");
                set.setProjectName("set" + index);
                return TempSets.write(set, pack, dir.resolve("sets").resolve(index + ".tmp"));
            }));
        }
        executor.shutdown();
        for (int i = 0; i < futures.size(); i++) {
            GaiaSet read = TempSets.read(futures.get(i).get());
            assertEquals("set" + i, read.getProjectName());
            assertEquals("image" + i + ".png", diffuse(read).getPath());
        }
        // the name is resolved the same way however it is spelled
        assertEquals("set7", TempSets.read(dir.resolve("sets").resolve("x").resolve("..").resolve("7.tmp")).getProjectName());
    }

    @Test
    void fileRoundTripKeepsTexturePaths(@TempDir Path dir) throws IOException {
        Path file = dir.resolve("sets").resolve("one.tmp");
        TempSets.writeFile(createSet("C:\\data\\model", "a.png"), file);

        assertTrue(Files.isRegularFile(file));
        assertTrue(TempSets.exists(file));
        GaiaTexture read = diffuse(TempSets.read(file));
        assertEquals("C:\\data\\model", read.getParentPath());
        assertEquals("a.png", read.getPath());
        assertFalse(Files.exists(dir.resolve("sets").resolve("images")));
    }

    @Test
    void missingSetFailsAndIsForgottenOnClear(@TempDir Path dir) throws IOException {
        Path name = dir.resolve("gone.tmp");
        assertFalse(TempSets.exists(name));
        assertThrows(IOException.class, () -> TempSets.read(name));

        TempSets.write(createSet("p", "a.png"), dir.resolve("pack"), name);
        assertTrue(TempSets.exists(name));
        TempSets.clear();
        assertFalse(TempSets.exists(name));
    }

    @Test
    void copiesTheSavedImagePathsToTheClonedMaterialsOfTheSet(@TempDir Path dir) {
        // the cut writers save the images of the half-edge materials, the set holds clones made before the save
        GaiaSet source = createSet(null, "wall_atlas_image.png");
        GaiaSet set = new GaiaSet();
        set.setMaterials(new ArrayList<>(List.of(source.getMaterials().getFirst().clone())));
        GaiaTexture saved = diffuse(source);
        saved.setParentPath(dir.resolve("images").toString());
        saved.setWidth(512);

        TempSets.copyTexturePaths(source.getMaterials(), set);
        assertEquals(dir.resolve("images").toString(), diffuse(set).getParentPath());
        assertEquals("wall_atlas_image.png", diffuse(set).getPath());
        assertEquals(512, diffuse(set).getWidth());
    }

    @Test
    void textureWithoutParentPointsToTheImagesFolder(@TempDir Path dir) {
        GaiaSet set = createSet(null, "sub/missing.png");
        GaiaTexture kept = new GaiaTexture();
        kept.setParentPath("C:\\data");
        kept.setPath("kept.png");
        set.getMaterials().getFirst().getTextures().get(TextureType.DIFFUSE).add(kept);

        TempSets.resolveMissingParents(set, dir.resolve("images"));
        assertEquals(dir.resolve("images").toString(), diffuse(set).getParentPath());
        assertEquals("missing.png", diffuse(set).getPath());
        assertEquals("C:\\data", kept.getParentPath());
    }

    @Test
    void imageSizeReadsTheHeader(@TempDir Path dir) throws IOException {
        Path image = dir.resolve("a.png");
        ImageIO.write(new BufferedImage(300, 20, BufferedImage.TYPE_INT_ARGB), "png", image.toFile());
        assertEquals(new Dimension(300, 20), TempTextures.imageSize(image));
        assertNull(TempTextures.imageSize(dir.resolve("missing.png")));
        Path notAnImage = Files.writeString(dir.resolve("b.png"), "not an image");
        assertNull(TempTextures.imageSize(notAnImage));
    }

    private static GaiaTexture diffuse(GaiaSet set) {
        return set.getMaterials().getFirst().getTextures().get(TextureType.DIFFUSE).getFirst();
    }

    private static List<String> filesIn(Path directory) throws IOException {
        try (Stream<Path> files = Files.walk(directory)) {
            return files.filter(Files::isRegularFile).map(path -> path.getFileName().toString()).sorted().toList();
        }
    }

    private static GaiaSet createSet(String parent, String texturePath) {
        GaiaTexture texture = new GaiaTexture();
        texture.setType(TextureType.DIFFUSE);
        texture.setParentPath(parent);
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
}
