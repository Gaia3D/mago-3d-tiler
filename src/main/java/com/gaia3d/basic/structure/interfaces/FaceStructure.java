package com.gaia3d.basic.structure.interfaces;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector3d;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class FaceStructure {
    protected int[] indices;
    protected Vector3d faceNormal = new Vector3d();
}
