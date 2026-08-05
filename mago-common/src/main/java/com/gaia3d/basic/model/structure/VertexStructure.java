package com.gaia3d.basic.model.structure;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2d;
import org.joml.Vector3d;

@Getter
@Setter
public abstract class VertexStructure {
    protected Vector3d position;
    protected Vector3d normal;
    protected Vector2d texcoords;
    protected byte[] color;
    protected float batchId;
}
