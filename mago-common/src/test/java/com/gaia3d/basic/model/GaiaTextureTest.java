package com.gaia3d.basic.model;

import com.gaia3d.basic.types.LevelOfDetail;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GaiaTextureTest {

    @TempDir
    Path tempDir;

    @Test
    @Tag("release")
    void reloadsRequestedLodImageWhenOriginalImageWasAlreadyLoaded() throws IOException {
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB), "png", tempDir.resolve("texture.png").toFile());
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", tempDir.resolve("1_texture.png").toFile());

        GaiaTexture texture = new GaiaTexture();
        texture.setParentPath(tempDir.toString());
        texture.setPath("texture.png");

        assertEquals(16, texture.getBufferedImage().getWidth());
        assertEquals(8, texture.getBufferedImage(LevelOfDetail.LOD1).getWidth());
        assertEquals(8, texture.getBufferedImage(LevelOfDetail.LOD1).getHeight());
    }
}
