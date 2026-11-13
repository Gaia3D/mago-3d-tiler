package com.gaia3d.command.mago;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Tag("default")
class CompressionOptionTest {

    @Test
    void parsesMeshCompression() {
        assertEquals(MeshCompression.NONE, MeshCompression.fromOption(null));
        assertEquals(MeshCompression.NONE, MeshCompression.fromOption("none"));
        assertEquals(MeshCompression.QUANTIZE, MeshCompression.fromOption("Quantize"));
        assertEquals(MeshCompression.DRACO, MeshCompression.fromOption("draco"));
        assertThrows(IllegalArgumentException.class, () -> MeshCompression.fromOption("meshopt"));
    }

    @Test
    void parsesTextureCompression() {
        assertEquals(TextureCompression.NONE, TextureCompression.fromOption(""));
        assertEquals(TextureCompression.KTX2, TextureCompression.fromOption("KTX2"));
        assertThrows(IllegalArgumentException.class, () -> TextureCompression.fromOption("webp"));
    }
}
