package com.gaia3d.renderable;

import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.structure.GaiaMaterial;
import com.gaia3d.basic.structure.GaiaPrimitive;
import com.gaia3d.basic.types.AttributeType;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
@Getter
@Setter
public class RenderablePrimitive extends RenderableObject{
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
}
