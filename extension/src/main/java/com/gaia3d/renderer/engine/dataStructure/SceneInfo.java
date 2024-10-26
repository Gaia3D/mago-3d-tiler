package com.gaia3d.renderer.engine.dataStructure;

import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector3d;

@Getter
@Setter
public class SceneInfo {
    private String scenePath;
    private Matrix4d transformMatrix;
}
