package com.gaia3d.engine.fbo;

import lombok.Getter;
import lombok.Setter;
import org.lwjgl.opengl.GL30;

// http://www.java2s.com/example/java-api/org/lwjgl/opengl/gl30/glgenframebuffers-0-0.html

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
}
