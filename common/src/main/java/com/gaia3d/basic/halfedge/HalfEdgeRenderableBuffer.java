package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.types.AttributeType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class HalfEdgeRenderableBuffer {
    private AttributeType attributeType;
    private int elementsCount = -1;
    private byte glDimension;
    private int glType;
    private int glTarget;
    private int vboId = -1;

    public HalfEdgeRenderableBuffer(AttributeType attributeType, int elementsCount, byte glDimension, int glType, int glTarget) {
        this.attributeType = attributeType;
        this.elementsCount = elementsCount;
        this.glDimension = glDimension;
        this.glType = glType;
        this.glTarget = glTarget;
    }
}

