package com.gaia3d.basic.exchangable;

import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector3d;

@Getter
@Setter
public class SceneInfo {
    private String scenePath;
    private Matrix4d transformMatrix;
    private Vector3d scenePosLC;
}

