package com.gaia3d.renderer.engine.fbo;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL30;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

@Getter
@Setter
public class FboMRT {
    private int fboId;
    private int[] colorTextureIds;
    private int depthRenderBufferId;
    private String name;
    private int fboWidth;
    private int fboHeight;

    public FboMRT(String name, int fboWidth, int fboHeight, int numColorAttachments) {
        this.fboWidth = fboWidth;
        this.fboHeight = fboHeight;

        fboId = GL30.glGenFramebuffers();
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);

        // Create color textures
        colorTextureIds = new int[numColorAttachments];
        for (int i = 0; i < numColorAttachments; i++) {
            colorTextureIds[i] = GL30.glGenTextures();
            GL30.glBindTexture(GL30.GL_TEXTURE_2D, colorTextureIds[i]);

            GL30.glTexImage2D(GL30.GL_TEXTURE_2D, 0, GL30.GL_RGBA, fboWidth, fboHeight, 0,
                    GL30.GL_RGBA, GL30.GL_UNSIGNED_BYTE, (ByteBuffer) null);

            GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_S, GL30.GL_CLAMP_TO_EDGE);
            GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_WRAP_T, GL30.GL_CLAMP_TO_EDGE);
            GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MIN_FILTER, GL30.GL_NEAREST);
            GL30.glTexParameteri(GL30.GL_TEXTURE_2D, GL30.GL_TEXTURE_MAG_FILTER, GL30.GL_NEAREST);

            // Attach texture to FBO
            GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0 + i,
                    GL30.GL_TEXTURE_2D, colorTextureIds[i], 0);
        }

        // Depth renderbuffer
        depthRenderBufferId = GL30.glGenRenderbuffers();
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, depthRenderBufferId);
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH_COMPONENT24, fboWidth, fboHeight);
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_RENDERBUFFER, depthRenderBufferId);

        // Specify the list of draw buffers
        IntBuffer drawBuffers = BufferUtils.createIntBuffer(numColorAttachments);
        for (int i = 0; i < numColorAttachments; i++) {
            drawBuffers.put(GL30.GL_COLOR_ATTACHMENT0 + i);
        }
        drawBuffers.flip();
        GL30.glDrawBuffers(drawBuffers);

        // Check FBO status
        int status = GL30.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Framebuffer not complete! Status = " + status);
        }

        unbind();
    }

    public void bind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fboId);
    }

    public void unbind() {
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
    }

    public int getColorTextureId(int index) {
        return colorTextureIds[index];
    }

    public ByteBuffer readPixels(int index, int format) {
        GL30.glPixelStorei(GL30.GL_PACK_ALIGNMENT, 1);
        GL30.glPixelStorei(GL30.GL_UNPACK_ALIGNMENT, 1);

        // Set the read buffer to the specified color attachment
        GL30.glReadBuffer(GL30.GL_COLOR_ATTACHMENT0 + index);

        ByteBuffer pixels = ByteBuffer.allocateDirect(fboWidth * fboHeight * 4);
        GL30.glReadPixels(0, 0, fboWidth, fboHeight, format, GL30.GL_UNSIGNED_BYTE, pixels);
        return pixels;
    }

    public byte[] getBytesArray(int index, int format) {
        ByteBuffer pixels = readPixels(index, format);
        pixels.rewind();
        byte[] bufferArray = new byte[pixels.remaining()];
        pixels.get(bufferArray);
        return bufferArray;
    }

    public BufferedImage getBufferedImage(int index, int bufferedImageType) {
        int format = GL30.GL_RGBA;

        if (bufferedImageType == BufferedImage.TYPE_INT_RGB) {
            format = GL30.GL_RGB;
        } else if (bufferedImageType == BufferedImage.TYPE_INT_ARGB) {
            format = GL30.GL_RGBA;
        }

        ByteBuffer byteBuffer = this.readPixels(index, format);
        byteBuffer.rewind();

        int fboWidth = this.getFboWidth();
        int fboHeight = this.getFboHeight();

        BufferedImage image = new BufferedImage(fboWidth, fboHeight, bufferedImageType);

        int[] pixels = new int[fboWidth * fboHeight];

        byte[] bufferArray = new byte[byteBuffer.remaining()];
        byteBuffer.get(bufferArray);

        int indexBuffer = 0;
        for (int y = 0; y < fboHeight; y++) {
            for (int x = 0; x < fboWidth; x++) {
                int r = bufferArray[indexBuffer++] & 0xFF;
                int g = bufferArray[indexBuffer++] & 0xFF;
                int b = bufferArray[indexBuffer++] & 0xFF;
                int a = (bufferedImageType == BufferedImage.TYPE_INT_ARGB) ? (bufferArray[indexBuffer++] & 0xFF) : 255;

                int color = (a << 24) | (r << 16) | (g << 8) | b;
                pixels[(fboHeight - y - 1) * fboWidth + x] = color;
            }
        }

        image.setRGB(0, 0, fboWidth, fboHeight, pixels, 0, fboWidth);
        return image;
    }

    public void cleanup() {
        for (int texId : colorTextureIds) {
            GL30.glDeleteTextures(texId);
        }

        GL30.glDeleteRenderbuffers(depthRenderBufferId);
        GL30.glDeleteFramebuffers(fboId);
    }

    public int getNumColorAttachments() {
        if (colorTextureIds == null) return 0;
        return colorTextureIds.length;
    }
}
