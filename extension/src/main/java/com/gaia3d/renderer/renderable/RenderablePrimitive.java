package com.gaia3d.renderer.renderable;

import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.types.AttributeType;
import lombok.Getter;
import lombok.Setter;
import org.lwjgl.opengl.GL20;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class RenderablePrimitive  extends RenderableObject{
    int id;
    String guid;
    Map<AttributeType, RenderableBuffer> mapAttribTypeRenderableBuffer;
    GaiaMaterial material;
    GaiaPrimitive originalGaiaPrimitive;
    GaiaBufferDataSet originalBufferDataSet;

    public RenderablePrimitive()
    {
        id = -1;
        guid = "no_guid";
        mapAttribTypeRenderableBuffer = new HashMap<>();
        material = null;
        status = -1; // 0 = interior, 1 = exterior, -1 = unknown.***
        colorCode = -1; // 36-bit RGBA color.***
    }

    public void setAttribTypeRenderableBuffer(AttributeType attribType, RenderableBuffer renderableBuffer)
    {
        mapAttribTypeRenderableBuffer.put(attribType, renderableBuffer);
    }

    public void deleteGLBuffers() {
        for (RenderableBuffer renderableBuffer : mapAttribTypeRenderableBuffer.values())
        {
            int vboId = renderableBuffer.getVboId();
            if(vboId != -1)
            {
                GL20.glDeleteBuffers(vboId);
                renderableBuffer.setVboId(-1);
            }
        }

        // remove all elements from map.
        mapAttribTypeRenderableBuffer.clear();
        material = null;
        originalGaiaPrimitive = null;
        originalBufferDataSet = null;

    }
}
