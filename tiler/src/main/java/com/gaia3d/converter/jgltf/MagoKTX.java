package com.gaia3d.converter.jgltf;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;

@Slf4j
public class MagoKTX {
    public void start(String inputPath, String outputPath) {
        BufferedImage image = null;
        try {
            image = ImageIO.read(new File(inputPath));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        byte[] pngData = loadFile(image);
        ByteBuffer imageData = loadPngImage(pngData);
        saveAsKtx(imageData, image.getWidth(), image.getHeight(), 4, outputPath);
    }

    private final static byte[] fileIdentifier = {
            (byte) 0xAB, (byte) 0x4B, (byte) 0x54, (byte) 0x58, (byte) 0x20, (byte) 0x31, (byte) 0x31, (byte) 0xBB, (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
    };

    // KTX Save
    private void saveAsKtx(ByteBuffer imageData, int imageWidth, int imageHeight, int channels, String ktxFilePath) {
        try (DataOutputStream os = new DataOutputStream(new FileOutputStream(ktxFilePath))) {
            byte[] bytes = imageData.array();

            // KTX Header
            os.write(fileIdentifier);  // KTX Magic Number
            os.writeInt(0x04030201);  // Endianess - bit integer
            os.writeInt(5121);  // GlType - UnsignedByte
            os.writeInt(1);  // GlTypeSize
            os.writeInt(6408);  // GlFormat - RGBA
            os.writeInt(0x04030201);  // GlInternalFormat
            os.writeInt(6408);  // GlBaseInternalFormat

            os.writeInt(imageWidth);  // PixelWidth
            os.writeInt(imageHeight);  // PixelHeight
            os.writeInt(0);  // PixelDepth

            int numberOfArrayElements = bytes.length;
            os.writeInt(numberOfArrayElements);  // NumberOfArrayElements
            int numberOfFaces = 1;
            os.writeInt(numberOfFaces);  // NumberOfFaces

            int numberOfMipmapLevels = 1;
            os.writeInt(numberOfMipmapLevels);  // NumberOfMipmapLevels

            String keyValues = "KTXorientation\u0000S=r,T=u\u0000";
            int bytesOfKeyValueData = keyValues.length() + 4;
            int padding = 3 - (bytesOfKeyValueData + 3) % 4;
            bytesOfKeyValueData += padding;

            os.writeInt(bytesOfKeyValueData);
            os.writeInt(bytesOfKeyValueData - 4 - padding);
            os.writeBytes(keyValues);
            this.pad(padding, os);

            for (int mipLevel = 0; mipLevel < numberOfMipmapLevels; ++mipLevel) {
                int width = Math.max(1, imageWidth >> mipLevel);
                int height = Math.max(1, imageHeight >> mipLevel);
                int imageSize = width * height;
                os.writeInt(imageSize);
                os.write(imageData.array());

                /*padding = 3 - (imageSize + 3) % 4;
                this.pad(padding, out);
                offset += imageSize;*/
            }
            os.flush();
            os.close();
            log.info("KTX file successfully saved: " + ktxFilePath);
        } catch (IOException e) {
            log.error("[ERROR] :Failed to save KTX file", e);
        }
    }

    private void pad(int padding, DataOutput out) throws IOException {
        for (int i = 0; i < padding; ++i) {
            out.write(0);
        }
    }

    // load PNG file
    private byte[] loadFile(BufferedImage bufferedImage) {
        int[] values = bufferedImage.getData().getPixels(0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), new int[bufferedImage.getWidth() * bufferedImage.getHeight() * 4]);
        byte[] bytes = new byte[values.length];
        for (int i = 0; i < values.length; i++) {
            bytes[i] = (byte) values[i];
        }
        return bytes;
    }

    // load PNG image
    private ByteBuffer loadPngImage(byte[] pngData) {
        return ByteBuffer.wrap(pngData);  // 임시 구현
    }
}