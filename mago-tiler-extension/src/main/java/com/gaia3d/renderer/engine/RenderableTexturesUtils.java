package com.gaia3d.renderer.engine;

import com.gaia3d.util.ImageResizer;
import com.gaia3d.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.opengl.GL30;

import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;
import java.nio.ByteBuffer;

import static java.awt.image.BufferedImage.*;
import static org.lwjgl.opengl.GL11.*;

@Slf4j
public class RenderableTexturesUtils {
    public static int createGlTextureFromByteArray(byte[] byteArray, int width, int height, int glFormat, int minFilter, int magFilter, int wrapS, int wrapT) {
        ByteBuffer buffer = ByteBuffer.allocateDirect(byteArray.length);
        buffer.put(byteArray);
        buffer.flip();
        int textureId = GL30.glGenTextures();

        GL30.glActiveTexture(GL30.GL_TEXTURE0);
        // get currently bound texture
        int boundTex = GL30.glGetInteger(GL30.GL_TEXTURE_BINDING_2D);

        GL30.glBindTexture(GL30.GL_TEXTURE_2D, textureId);

        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, minFilter); // GL_LINEAR
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, magFilter);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, wrapS);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, wrapT);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, glFormat, GL_UNSIGNED_BYTE, buffer);

        // restore previously bound texture
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, boundTex);
        return textureId;
    }

    public static int createGlTextureFromBufferedImage(BufferedImage bufferedImage, int minFilter, int magFilter, int wrapS, int wrapT, boolean resizeToPowerOf2) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        // resize image to nearest power of two
        if (resizeToPowerOf2) {
            log.debug("Resizing image to nearest power of two...");
            log.debug("Original image size: {}x{}", width, height);
            int resizeWidth = width;
            int resizeHeight = height;
            resizeWidth = ImageUtils.getNearestPowerOfTwo(resizeWidth);
            resizeHeight = ImageUtils.getNearestPowerOfTwo(resizeHeight);

            if (resizeWidth != width || resizeHeight != height) {
                width = resizeWidth;
                height = resizeHeight;
                ImageResizer imageResizer = new ImageResizer();
                bufferedImage = imageResizer.resizeImageGraphic2D(bufferedImage, resizeWidth, resizeHeight, true);
                log.debug("Resized image size: {}x{}", resizeWidth, resizeHeight);
            }
        }
        int format = bufferedImage.getType();
        // end resize image to nearest power of two

        // BufferedImage format :
        // TYPE_INT_RGB,
        // TYPE_INT_ARGB,
        // TYPE_INT_ARGB_PRE,
        // TYPE_INT_BGR,
        // TYPE_3BYTE_BGR,
        // TYPE_4BYTE_ABGR,
        // TYPE_4BYTE_ABGR_PRE,
        // TYPE_BYTE_GRAY,
        // TYPE_BYTE_BINARY,
        // TYPE_BYTE_INDEXED,
        // TYPE_USHORT_GRAY,
        // TYPE_USHORT_565_RGB,
        // TYPE_USHORT_555_RGB,
        // TYPE_CUSTOM

        int glFormat = -1; // GL_RGB, GL_RGBA, etc.
        if (format == TYPE_INT_RGB) {
            glFormat = GL_RGB;
        } else if (format == TYPE_INT_ARGB) {
            glFormat = GL_RGBA;
        } else if (format == TYPE_3BYTE_BGR) {
            glFormat = GL_RGB;
        } else if (format == TYPE_4BYTE_ABGR) {
            glFormat = GL_RGBA;
        }

        byte[] rgbaByteArray = null;

        // check if the data is DataBufferInt or DataBufferByte
        DataBuffer dataBuffer = bufferedImage.getRaster().getDataBuffer();
        if (dataBuffer instanceof DataBufferInt) {
            // DataBufferInt
            int[] intArray = ((DataBufferInt) dataBuffer).getData();
            int intArrayLength = intArray.length;
            if (format == TYPE_INT_RGB) {
                rgbaByteArray = new byte[intArray.length * 3];

                for (int i = 0; i < intArray.length; i++) {
                    int value = intArray[i];
                    rgbaByteArray[i * 3] = (byte) ((value >> 16) & 0xFF);  // Red
                    rgbaByteArray[i * 3 + 1] = (byte) ((value >> 8) & 0xFF); // Green
                    rgbaByteArray[i * 3 + 2] = (byte) (value & 0xFF);        // Blue
                }
            } else if (format == TYPE_INT_ARGB) {
                // DataBufferInt
                rgbaByteArray = new byte[intArray.length * 4];

                for (int i = 0; i < intArray.length; i++) {
                    int value = intArray[i];
                    rgbaByteArray[i * 4] = (byte) ((value >> 24) & 0xFF);  // Alpha
                    rgbaByteArray[i * 4 + 1] = (byte) ((value >> 16) & 0xFF); // Red
                    rgbaByteArray[i * 4 + 2] = (byte) ((value >> 8) & 0xFF);        // Green
                    rgbaByteArray[i * 4 + 3] = (byte) (value & 0xFF); // Blue
                }
            }
        } else {
            // DataBufferByte
            rgbaByteArray = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
        }


        if (format == TYPE_INT_ARGB) {
            // change byte order
            byte temp;
            for (int i = 0; i < rgbaByteArray.length; i += 4) {
                temp = rgbaByteArray[i];
                rgbaByteArray[i] = rgbaByteArray[i + 1];
                rgbaByteArray[i + 1] = rgbaByteArray[i + 2];
                rgbaByteArray[i + 2] = rgbaByteArray[i + 3];
                rgbaByteArray[i + 3] = temp;
            }
        } else if (format == TYPE_4BYTE_ABGR) {
            // change byte order
            byte temp;
            for (int i = 0; i < rgbaByteArray.length; i += 4) {
                temp = rgbaByteArray[i];
                rgbaByteArray[i] = rgbaByteArray[i + 3];
                rgbaByteArray[i + 3] = temp;
                temp = rgbaByteArray[i + 1];
                rgbaByteArray[i + 1] = rgbaByteArray[i + 2];
                rgbaByteArray[i + 2] = temp;
            }
        } else if (format == TYPE_3BYTE_BGR) {
            // change byte order
            byte temp;
            for (int i = 0; i < rgbaByteArray.length; i += 3) {
                temp = rgbaByteArray[i];
                rgbaByteArray[i] = rgbaByteArray[i + 2];
                rgbaByteArray[i + 2] = temp;
            }
        }
        int textureId = createGlTextureFromByteArray(rgbaByteArray, width, height, glFormat, minFilter, magFilter, wrapS, wrapT);

        return textureId;
    }


}
