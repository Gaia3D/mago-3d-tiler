package com.gaia3d.renderable;

import com.gaia3d.basic.types.AccessorType;
import com.gaia3d.basic.types.AttributeType;
import lombok.Setter;

public class RenderableBuffer {
    AttributeType attributeType;
    int elementsCount = -1;
    byte glDimension;
    int glType;
    int glTarget;

    @Setter
    int vboId = -1;

    public RenderableBuffer(AttributeType attributeType, int elementsCount, byte glDimension, int glType, int glTarget) {
        this.attributeType = attributeType;
        this.elementsCount = elementsCount;
        this.glDimension = glDimension;
        this.glType = glType;
        this.glTarget = glTarget;
    }

}
