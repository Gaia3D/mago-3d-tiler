package com.gaia3d.basic.exchangable;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
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
    private GaiaBoundingBox sceneBoundingBox;
}

