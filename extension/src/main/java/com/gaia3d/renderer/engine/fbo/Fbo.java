package com.gaia3d.renderer.engine.fbo;

// http://www.java2s.com/example/java-api/org/lwjgl/opengl/gl30/glgenframebuffers-0-0.html

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.opengl.GL30;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.glPixelStorei;
import static org.lwjgl.opengl.GL11C.GL_PACK_ALIGNMENT;
import static org.lwjgl.opengl.GL11C.GL_UNPACK_ALIGNMENT;

@Getter
@Setter
public class Fbo {
    private int fboId;
    private int colorTextureId;
    private int depthRenderBufferId;
    private String name;
    private int fboWidth;
    private int fboHeight;

    public Fbo(String name, int fboWidth, int fboHeight) {
        this.name = name;
        this.fboWidth = fboWidth;
        this.fboHeight = fboHeight;

        fboId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);

        // color texture.***
        colorTextureId = GL30.glGenTextures();
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, colorTextureId);

        GL30.glEnable(GL30.GL_TEXTURE_2D);
        GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGBA, fboWidth, fboHeight, 0, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, 0);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_NEAREST);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_NEAREST);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D, colorTextureId, 0);

        // depth render buffer.***
        depthRenderBufferId = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRenderBufferId);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT, fboWidth, fboHeight);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthRenderBufferId);

        unbind();
    }

    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
    }

    public void unbind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public ByteBuffer readPixels(int format) {
        glPixelStorei(GL_PACK_ALIGNMENT, 1);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);

        ByteBuffer pixels = ByteBuffer.allocateDirect(fboWidth * fboHeight * 4);
        GL30.glReadPixels(0, 0, fboWidth, fboHeight, format, GL30.GL_UNSIGNED_BYTE, pixels);
        return pixels;
    }

    public BufferedImage getBufferedImage(int bufferedImageType)
    {
        int format = GL30.GL_RGBA;

        if(bufferedImageType == BufferedImage.TYPE_INT_RGB)
        {
            format = GL30.GL_RGB;
        }
        else if(bufferedImageType == BufferedImage.TYPE_INT_ARGB)
        {
            format = GL30.GL_RGBA;
        }
        ByteBuffer byteBuffer = this.readPixels(format);
        byteBuffer.rewind();

        int fboWidth = this.getFboWidth();
        int fboHeight = this.getFboHeight();

        BufferedImage image = new BufferedImage(fboWidth, fboHeight, bufferedImageType);

        for (int y = 0; y < fboHeight; y++) {
            for (int x = 0; x < fboWidth; x++) {
                if(bufferedImageType == BufferedImage.TYPE_INT_ARGB)
                {
                    int r = byteBuffer.get() & 0xFF; // Rojo
                    int g = byteBuffer.get() & 0xFF; // Verde
                    int b = byteBuffer.get() & 0xFF; // Azul
                    int a = byteBuffer.get() & 0xFF; // Alpha

                    int color = (a << 24) | (r << 16) | (g << 8) | b; // Formato ARGB
                    image.setRGB(x, fboHeight - y - 1, color);
                }
                else if(bufferedImageType == BufferedImage.TYPE_INT_RGB)
                {
                    int r = byteBuffer.get() & 0xFF; // Rojo
                    int g = byteBuffer.get() & 0xFF; // Verde
                    int b = byteBuffer.get() & 0xFF; // Azul

                    int color = (r << 16) | (g << 8) | b; // Formato RGB
                    image.setRGB(x, fboHeight - y - 1, color);
                }

            }
        }

        return image;
    }

    public void cleanup()
    {
        GL30.glDeleteTextures(colorTextureId);

        // Eliminar el render buffer de profundidad
        GL30.glDeleteRenderbuffers(depthRenderBufferId);

        // Eliminar el framebuffer
        GL30.glDeleteFramebuffers(fboId);
    }

    public void resize(int newWidth, int newHeight) {
        // 1rst, check if the existent Fbo has the same size.***
        if (newWidth == fboWidth && newHeight == fboHeight) {
            return;
        }

        // Actualizar los valores de ancho y alto
        this.fboWidth = newWidth;
        this.fboHeight = newHeight;

        // Primero, eliminar las texturas y buffers existentes
        GL30.glDeleteTextures(colorTextureId);
        GL30.glDeleteRenderbuffers(depthRenderBufferId);

        // Crear la nueva textura de color
        colorTextureId = GL30.glGenTextures();
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, colorTextureId);

        GL30.glEnable(GL30.GL_TEXTURE_2D);
        GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGBA, fboWidth, fboHeight, 0, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, 0);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_NEAREST);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_NEAREST);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D, colorTextureId, 0);

        // Crear el nuevo render buffer de profundidad
        depthRenderBufferId = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRenderBufferId);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT, fboWidth, fboHeight);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthRenderBufferId);

        // Verificar si el FBO está completo
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Error: Framebuffer is not complete after resizing!");
        }

        // Desvincular el framebuffer
        unbind();
    }
}
