package com.gaia3d.converter.gltf;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@Tag("default")
class Ktx2TextureEncoderTest {
    private static final byte[] KTX2_IDENTIFIER = {(byte) 0xAB, 'K', 'T', 'X', ' ', '2', '0', (byte) 0xBB, '\r', '\n', 0x1A, '\n'};

    @Test
    void encodesEtc1sWithAFullMipChain() {
        assumeTrue(Ktx2TextureEncoder.isRuntimeSupported(), "libktx needs msvcp140 14.40+ on Windows");
        BufferedImage image = new BufferedImage(64, 32, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 64; x++) {
            for (int y = 0; y < 32; y++) {
                image.setRGB(x, y, (x * 4) << 16 | (y * 8) << 8 | 0x40);
            }
        }
        byte[] ktx2 = new Ktx2TextureEncoder(128, 1, 2).apply(image);
        assertNotNull(ktx2);
        assertArrayEquals(KTX2_IDENTIFIER, Arrays.copyOf(ktx2, 12));
        ByteBuffer header = ByteBuffer.wrap(ktx2).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, header.getInt(12)); // vkFormat undefined: Basis Universal payload
        assertEquals(64, header.getInt(20));
        assertEquals(32, header.getInt(24));
        assertEquals(7, header.getInt(40)); // 64x32 .. 1x1
        assertEquals(1, header.getInt(44)); // supercompression BasisLZ (ETC1S)
    }

    @Test
    void readsTheJdkRuntimeVersionOnWindows() {
        java.nio.file.Path runtime = java.nio.file.Path.of(System.getProperty("java.home"), "bin", "msvcp140.dll");
        assumeTrue(java.nio.file.Files.isRegularFile(runtime));
        int[] version = Ktx2TextureEncoder.readFileVersion(runtime);
        assertNotNull(version);
        assertEquals(14, version[0]);
    }

    @Test
    void alignsSizesToCompressionBlocks() {
        assertEquals(4, Ktx2TextureEncoder.alignToBlock(1));
        assertEquals(1024, Ktx2TextureEncoder.alignToBlock(1024));
        assertEquals(1028, Ktx2TextureEncoder.alignToBlock(1025));
    }
}
