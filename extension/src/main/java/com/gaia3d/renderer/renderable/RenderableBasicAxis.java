package com.gaia3d.renderer.renderable;

import com.gaia3d.basic.types.AttributeType;
import lombok.Getter;
import org.lwjgl.opengl.GL20;

import java.util.HashMap;
import java.util.Map;

@Getter
public class RenderableBasicAxis {
    private float axisLength;
    private Map<AttributeType, RenderableBuffer> mapAttribTypeRenderableBuffer;

    public RenderableBasicAxis() {
        axisLength = 100.0f;
        mapAttribTypeRenderableBuffer = new HashMap<>();
        init();
    }

    public void setAttribTypeRenderableBuffer(AttributeType attribType, RenderableBuffer renderableBuffer) {
        mapAttribTypeRenderableBuffer.put(attribType, renderableBuffer);
    }

    public void init() {
        AttributeType attribType = AttributeType.POSITION;
        int elemsCount = 6;
        byte glDimension = 3;
        int glType = GL20.GL_FLOAT;
        int glTarget = GL20.GL_ARRAY_BUFFER;

        // xyz axis.***
        RenderableBuffer axisBuffer = new RenderableBuffer(attribType, elemsCount, glDimension, glType, glTarget);
        float[] axisPositions = new float[]{0.0f, 0.0f, 0.0f, axisLength, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, axisLength, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, axisLength};
        int[] vboId = new int[1];
        GL20.glGenBuffers(vboId);

        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId[0]);
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, axisPositions, GL20.GL_STATIC_DRAW);
        axisBuffer.setVboId(vboId[0]);

        setAttribTypeRenderableBuffer(attribType, axisBuffer);

        // colors.***
        attribType = AttributeType.COLOR;
        elemsCount = 6;
        glDimension = 4; // r,g,b,a.***
        glType = GL20.GL_FLOAT;
        glTarget = GL20.GL_ARRAY_BUFFER;

        RenderableBuffer colorBuffer = new RenderableBuffer(attribType, elemsCount, glDimension, glType, glTarget);
        float[] colors = new float[]{1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 1.0f};
        vboId = new int[1];
        GL20.glGenBuffers(vboId);

        GL20.glBindBuffer(GL20.GL_ARRAY_BUFFER, vboId[0]);
        GL20.glBufferData(GL20.GL_ARRAY_BUFFER, colors, GL20.GL_STATIC_DRAW);
        colorBuffer.setVboId(vboId[0]);

        setAttribTypeRenderableBuffer(attribType, colorBuffer);
    }
}
