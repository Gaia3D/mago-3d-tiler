package com.gaia3d.engine;

import com.gaia3d.util.ImageResizer;
import com.gaia3d.util.ImageUtils;
import org.lwjgl.opengl.GL20;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.nio.ByteBuffer;

import static java.awt.image.BufferedImage.*;
import static org.lwjgl.opengl.GL11.*;

public class RenderableTexturesUtils {

    static public int createGlTextureFromByteArray(byte[] byteArray, int width, int height, int glFormat,
                                                   int minFilter, int magFilter, int wrapS, int wrapT) {

        ByteBuffer buffer = ByteBuffer.allocateDirect(byteArray.length);
        buffer.put(byteArray);
        buffer.flip(); // buffer.flip() = buffer.position(0); // returns the buffer to the start of the data.


        //GL20.glEnable(GL20.GL_TEXTURE_2D);

        int textureId = GL20.glGenTextures();

        GL20.glActiveTexture(GL20.GL_TEXTURE0);
        GL20.glBindTexture(GL20.GL_TEXTURE_2D, textureId);
        //GL20.glPixelStorei(GL20.GL_UNPACK_ALIGNMENT, 1);

        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MIN_FILTER, minFilter); // GL_LINEAR
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_MAG_FILTER, magFilter);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_S, wrapS);
        GL20.glTexParameteri(GL20.GL_TEXTURE_2D, GL20.GL_TEXTURE_WRAP_T, wrapT);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                glFormat, GL_UNSIGNED_BYTE, buffer);

        return textureId;
    }
    static public int createGlTextureFromBufferedImage(BufferedImage bufferedImage, int minFilter, int magFilter, int wrapS, int wrapT) {
        int width = bufferedImage.getWidth();
        int height = bufferedImage.getHeight();

        // resize image to nearest power of two.***
        int resizeWidth = width;
        int resizeHeight = height;
        resizeWidth = ImageUtils.getNearestPowerOfTwo(resizeWidth);
        resizeHeight = ImageUtils.getNearestPowerOfTwo(resizeHeight);
        width = resizeWidth;
        height = resizeHeight;
        ImageResizer imageResizer = new ImageResizer();
        bufferedImage = imageResizer.resizeImageGraphic2D(bufferedImage, resizeWidth, resizeHeight);
        int format = bufferedImage.getType();
        // end resize image to nearest power of two.***

        //format :
        // 1 = TYPE_INT_RGB,
        // 2 = TYPE_INT_ARGB,
        // 3 = TYPE_INT_ARGB_PRE,
        // 4 = TYPE_INT_BGR,
        // 5 = TYPE_3BYTE_BGR,
        // 6 = TYPE_4BYTE_ABGR,
        // TYPE_4BYTE_ABGR_PRE,
        // TYPE_BYTE_GRAY,
        // TYPE_BYTE_BINARY,
        // TYPE_BYTE_INDEXED,
        // TYPE_USHORT_GRAY,
        // TYPE_USHORT_565_RGB,
        // TYPE_USHORT_555_RGB,
        // TYPE_CUSTOM
        //int format = diffuseTexture.getFormat();
        int glFormat = -1; // GL_RGB, GL_RGBA, etc.
        if(format == TYPE_INT_RGB)
        {
            glFormat = GL_RGB;
        }
        else if(format == TYPE_INT_ARGB)
        {
            glFormat = GL_RGBA;
        }
        else if(format == TYPE_3BYTE_BGR)
        {
            glFormat = GL_RGB;
        }
        else if(format == TYPE_4BYTE_ABGR)
        {
            glFormat = GL_RGBA;
        }
        else
        {
            int hola = 0;
        }



        byte[] rgbaByteArray = ((DataBufferByte) bufferedImage.getRaster().getDataBuffer()).getData();
        int textureId = createGlTextureFromByteArray(rgbaByteArray, width, height, glFormat, minFilter, magFilter, wrapS, wrapT);

        return textureId;
    }
}
