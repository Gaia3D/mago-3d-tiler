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

        // color texture.
        colorTextureId = GL30.glGenTextures();
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, colorTextureId);

        GL30.glEnable(GL30.GL_TEXTURE_2D);
        GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGBA, fboWidth, fboHeight, 0, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, (ByteBuffer) null);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_NEAREST);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_NEAREST);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D, colorTextureId, 0);

        // depth render buffer.
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

    public BufferedImage getBufferedImage(int bufferedImageType) {
        int format = GL30.GL_RGBA;

        if (bufferedImageType == BufferedImage.TYPE_INT_RGB) {
            format = GL30.GL_RGB;
        } else if (bufferedImageType == BufferedImage.TYPE_INT_ARGB) {
            format = GL30.GL_RGBA;
        }
        ByteBuffer byteBuffer = this.readPixels(format);
        byteBuffer.rewind();

        int fboWidth = this.getFboWidth();
        int fboHeight = this.getFboHeight();

        BufferedImage image = new BufferedImage(fboWidth, fboHeight, bufferedImageType);

        int[] pixels = new int[fboWidth * fboHeight];

        byte[] bufferArray = new byte[byteBuffer.remaining()];
        byteBuffer.get(bufferArray);

        int index = 0;
        for (int y = 0; y < fboHeight; y++) {
            for (int x = 0; x < fboWidth; x++) {
                int r = bufferArray[index++] & 0xFF;
                int g = bufferArray[index++] & 0xFF;
                int b = bufferArray[index++] & 0xFF;
                int a = (bufferedImageType == BufferedImage.TYPE_INT_ARGB) ? (bufferArray[index++] & 0xFF) : 255;

                int color = (a << 24) | (r << 16) | (g << 8) | b;
                pixels[(fboHeight - y - 1) * fboWidth + x] = color;
            }
        }

        // Set the pixels of the image in one command.***
        image.setRGB(0, 0, fboWidth, fboHeight, pixels, 0, fboWidth);

        return image;
    }

    public void cleanup() {
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

        // update the size of the FBO.***
        this.fboWidth = newWidth;
        this.fboHeight = newHeight;

        // Delete the color texture and the depth render buffer
        GL30.glDeleteTextures(colorTextureId);
        GL30.glDeleteRenderbuffers(depthRenderBufferId);

        // Create the new color texture
        colorTextureId = GL30.glGenTextures();
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, colorTextureId);

        GL30.glEnable(GL30.GL_TEXTURE_2D);
        GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGBA, fboWidth, fboHeight, 0, GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, 0);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_NEAREST);
        GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_NEAREST);
        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL30.GL_TEXTURE_2D, colorTextureId, 0);

        // Create the new depth render buffer
        depthRenderBufferId = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRenderBufferId);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT, fboWidth, fboHeight);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthRenderBufferId);

        // Verify if the framebuffer is complete
        if (GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER) != GL30.GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Error: Framebuffer is not complete after resizing!");
        }

        // Unbind the framebuffer
        unbind();
    }
}
