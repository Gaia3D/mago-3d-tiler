package com.gaia3d.renderable;

import com.gaia3d.basic.structure.GaiaMaterial;
import com.gaia3d.basic.types.AttributeType;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.Map;
@Getter
@Setter
public class RenderablePrimitive {
    int id;
    String guid;
    Map<AttributeType, RenderableBuffer> mapAttribTypeRenderableBuffer;
    GaiaMaterial material;

    public RenderablePrimitive() {
        id = -1;
        guid = "no_guid";
        mapAttribTypeRenderableBuffer = new HashMap<>();
        material = null;
    }

    public void setAttribTypeRenderableBuffer(AttributeType attribType, RenderableBuffer renderableBuffer) {
        mapAttribTypeRenderableBuffer.put(attribType, renderableBuffer);
    }
}
