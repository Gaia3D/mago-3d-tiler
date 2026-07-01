package com.gaia3d.basic.exchangable;

import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.LevelOfDetail;
import com.gaia3d.basic.types.TextureType;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GaiaSetTest {

    @TempDir
    Path tempDir;

    @Test
    @Tag("release")
    void writeFileWithLodKeepsSameNamedTexturesSeparate() throws IOException {
        UUID firstId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Path firstSource = tempDir.resolve("first");
        Path secondSource = tempDir.resolve("second");
        java.nio.file.Files.createDirectories(firstSource);
        java.nio.file.Files.createDirectories(secondSource);
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "png", firstSource.resolve("texture.png").toFile());
        ImageIO.write(new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB), "png", secondSource.resolve("texture.png").toFile());

        Path firstTempFile = gaiaSet(firstId, firstSource).writeFileWithLod(tempDir, 1, List.of(LevelOfDetail.LOD0, LevelOfDetail.LOD1));
        gaiaSet(secondId, secondSource).writeFileWithLod(tempDir, 2, List.of(LevelOfDetail.LOD0, LevelOfDetail.LOD1));

        Path images = tempDir.resolve("Project").resolve("0").resolve("images");
        assertTrue(java.nio.file.Files.exists(images.resolve(firstId + "_0_texture.png")));
        assertTrue(java.nio.file.Files.exists(images.resolve("1_" + firstId + "_0_texture.png")));
        assertTrue(java.nio.file.Files.exists(images.resolve(secondId + "_0_texture.png")));
        assertTrue(java.nio.file.Files.exists(images.resolve("1_" + secondId + "_0_texture.png")));

        GaiaSet reloaded = GaiaSet.readFile(firstTempFile);
        GaiaTexture texture = reloaded.getMaterials().getFirst().getTextures().get(TextureType.DIFFUSE).getFirst();
        assertEquals(firstId + "_0_texture.png", texture.getPath());
    }

    @Test
    @Tag("release")
    void writeFileWithLodRewritesExistingTextureWithWrongSize() throws IOException {
        UUID identifier = UUID.fromString("00000000-0000-0000-0000-000000000003");
        Path source = tempDir.resolve("source");
        java.nio.file.Files.createDirectories(source);
        ImageIO.write(new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB), "png", source.resolve("texture.png").toFile());

        Path images = tempDir.resolve("Project").resolve("0").resolve("images");
        java.nio.file.Files.createDirectories(images);
        Path staleTexture = images.resolve(identifier + "_0_texture.png");
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "png", staleTexture.toFile());

        gaiaSet(identifier, source).writeFileWithLod(tempDir, 1, List.of(LevelOfDetail.LOD0));

        BufferedImage rewritten = ImageIO.read(staleTexture.toFile());
        assertEquals(32, rewritten.getWidth());
        assertEquals(32, rewritten.getHeight());
    }

    private GaiaSet gaiaSet(UUID identifier, Path textureParentPath) {
        GaiaAttribute attribute = new GaiaAttribute();
        attribute.setIdentifier(identifier);

        GaiaTexture texture = new GaiaTexture();
        texture.setParentPath(textureParentPath.toString());
        texture.setPath("texture.png");
        texture.setType(TextureType.DIFFUSE);

        GaiaMaterial material = new GaiaMaterial();
        material.setId(0);
        material.getTextures().put(TextureType.DIFFUSE, List.of(texture));

        GaiaSet set = new GaiaSet();
        set.setProjectName("Project");
        set.setAttribute(attribute);
        set.setMaterials(List.of(material));
        set.setBufferDataList(List.of());
        return set;
    }
}
