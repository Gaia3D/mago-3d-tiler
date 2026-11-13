package com.gaia3d.converter.gltf;

import com.gaia3d.util.ImageResizer;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.libc.LibCStdlib;
import org.lwjgl.util.ktx.KTX;
import org.lwjgl.util.ktx.ktxBasisParams;
import org.lwjgl.util.ktx.ktxTexture;
import org.lwjgl.util.ktx.ktxTexture2;
import org.lwjgl.util.ktx.ktxTextureCreateInfo;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Encodes images as KTX2 / Basis Universal ETC1S with a full mip chain, for glTF KHR_texture_basisu. Viewers transcode
 * ETC1S to a GPU block format (BC1/BC3, ETC, ASTC), about an eighth of the RGBA memory a JPEG or PNG takes once decoded.
 */
@Slf4j
public class Ktx2TextureEncoder implements Function<BufferedImage, byte[]> {
    private static final int VK_FORMAT_R8G8B8_SRGB = 29;
    private static final int VK_FORMAT_R8G8B8A8_SRGB = 43;

    private final int qualityLevel; // ETC1S quality 1..255
    private final int compressionLevel; // ETC1S effort 0..5
    private final int threadCount;

    /**
     * @param threadCount encoder threads per image; tiles are written by several threads already
     */
    public Ktx2TextureEncoder(int qualityLevel, int compressionLevel, int threadCount) {
        this.qualityLevel = qualityLevel;
        this.compressionLevel = compressionLevel;
        this.threadCount = Math.max(1, threadCount);
    }

    /**
     * The LWJGL libktx for Windows is built with MSVC 2022 17.10+, whose std::mutex crashes the process when an older
     * msvcp140.dll is loaded first; the JDK loads the copy in its own bin directory, older than 14.40 in early JDK 21
     * builds. Other platforms are unaffected.
     */
    public static boolean isRuntimeSupported() {
        if (!System.getProperty("os.name", "").toLowerCase().contains("win")) {
            return true;
        }
        Path runtime = Path.of(System.getProperty("java.home"), "bin", "msvcp140.dll");
        if (!Files.isRegularFile(runtime)) {
            return true; // the system copy is used
        }
        int[] version = readFileVersion(runtime);
        return version == null || version[0] > 14 || (version[0] == 14 && version[1] >= 40);
    }

    /**
     * Major and minor file version from the VS_FIXEDFILEINFO of a PE file, null when not found.
     */
    static int[] readFileVersion(Path file) {
        try {
            byte[] bytes = Files.readAllBytes(file);
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.LITTLE_ENDIAN);
            for (int i = 0; i + 16 <= bytes.length; i += 4) {
                if (buffer.getInt(i) == 0xFEEF04BD) {
                    int versionMs = buffer.getInt(i + 8);
                    return new int[]{versionMs >>> 16, versionMs & 0xFFFF};
                }
            }
        } catch (IOException e) {
            log.debug("Unable to read the file version of {}", file, e);
        }
        return null;
    }

    /**
     * @return the KTX2 file bytes, or null when encoding fails so the caller keeps the original format
     */
    @Override
    public byte[] apply(BufferedImage image) {
        try {
            return encode(image);
        } catch (RuntimeException | LinkageError e) {
            log.warn("[WARN] KTX2 encoding failed, keeping the original image format: {}", e.getMessage());
            return null;
        }
    }

    private byte[] encode(BufferedImage image) {
        // block-compressed formats work on 4x4 blocks; a whole-texture stretch keeps normalized UVs valid
        int width = alignToBlock(image.getWidth());
        int height = alignToBlock(image.getHeight());
        if (width != image.getWidth() || height != image.getHeight()) {
            image = ImageResizer.resizeMultiStepSmart(image, width, height);
        }
        boolean hasAlpha = image.getColorModel().hasAlpha();
        int channels = hasAlpha ? 4 : 3;
        List<BufferedImage> levels = getMipLevels(image);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            ktxTextureCreateInfo createInfo = ktxTextureCreateInfo.calloc(stack)
                    .vkFormat(hasAlpha ? VK_FORMAT_R8G8B8A8_SRGB : VK_FORMAT_R8G8B8_SRGB)
                    .baseWidth(width)
                    .baseHeight(height)
                    .baseDepth(1)
                    .numDimensions(2)
                    .numLevels(levels.size())
                    .numLayers(1)
                    .numFaces(1)
                    .isArray(false)
                    .generateMipmaps(false);
            PointerBuffer texturePointer = stack.mallocPointer(1);
            check(KTX.ktxTexture2_Create(createInfo, KTX.KTX_TEXTURE_CREATE_ALLOC_STORAGE, texturePointer), "create");
            ktxTexture2 texture2 = ktxTexture2.create(texturePointer.get(0));
            ktxTexture texture = ktxTexture.create(texturePointer.get(0));
            try {
                for (int level = 0; level < levels.size(); level++) {
                    ByteBuffer pixels = toPixelBytes(levels.get(level), channels);
                    try {
                        check(KTX.ktxTexture_SetImageFromMemory(texture, level, 0, 0, pixels), "set level " + level);
                    } finally {
                        MemoryUtil.memFree(pixels);
                    }
                }

                ktxBasisParams params = ktxBasisParams.calloc(stack)
                        .structSize(ktxBasisParams.SIZEOF)
                        .uastc(false)
                        .threadCount(threadCount)
                        .compressionLevel(compressionLevel)
                        .qualityLevel(qualityLevel);
                check(KTX.ktxTexture2_CompressBasisEx(texture2, params), "compress");

                PointerBuffer data = stack.mallocPointer(1);
                PointerBuffer size = stack.mallocPointer(1);
                check(KTX.ktxTexture_WriteToMemory(texture, data, size), "write");
                try {
                    byte[] bytes = new byte[(int) size.get(0)];
                    MemoryUtil.memByteBuffer(data.get(0), bytes.length).get(bytes);
                    return bytes;
                } finally {
                    LibCStdlib.nfree(data.get(0));
                }
            } finally {
                KTX.ktxTexture_Destroy(texture);
            }
        }
    }

    static int alignToBlock(int size) {
        return Math.max(4, (size + 3) / 4 * 4);
    }

    /**
     * Base image and every halved level down to 1x1.
     */
    static List<BufferedImage> getMipLevels(BufferedImage image) {
        List<BufferedImage> levels = new ArrayList<>();
        levels.add(image);
        BufferedImage current = image;
        while (current.getWidth() > 1 || current.getHeight() > 1) {
            int width = Math.max(1, current.getWidth() / 2);
            int height = Math.max(1, current.getHeight() / 2);
            current = ImageResizer.resizeMultiStepSmart(current, width, height);
            levels.add(current);
        }
        return levels;
    }

    static ByteBuffer toPixelBytes(BufferedImage image, int channels) {
        int width = image.getWidth();
        int height = image.getHeight();
        int[] argb = image.getRGB(0, 0, width, height, null, 0, width);
        ByteBuffer buffer = MemoryUtil.memAlloc(width * height * channels);
        for (int pixel : argb) {
            buffer.put((byte) (pixel >> 16)).put((byte) (pixel >> 8)).put((byte) pixel);
            if (channels == 4) {
                buffer.put((byte) (pixel >>> 24));
            }
        }
        return buffer.flip();
    }

    private static void check(int result, String step) {
        if (result != KTX.KTX_SUCCESS) {
            throw new IllegalStateException("ktx " + step + ": " + KTX.ktxErrorString(result));
        }
    }
}
