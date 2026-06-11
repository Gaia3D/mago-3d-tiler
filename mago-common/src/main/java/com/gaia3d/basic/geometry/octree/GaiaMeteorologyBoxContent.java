package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaPlane;
import com.gaia3d.basic.geometry.entities.GaiaTriangle;
import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaScene;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.joml.Vector4d;

@Slf4j
@Getter
@Setter
@Deprecated
public class GaiaMeteorologyBoxContent implements GeometryContent {
    // This class is used by GaiaOctree to store the face data.
    private GaiaBoundingBox boundingBox = null;
    private Vector3d centerPoint = null;
    private Vector4d primaryColor = null;

    // Temperature, humidity, wind speed, etc. can be added as needed.
    // TODO implements here
    private float temperature;
    private float humidity;

    @Override
    public GaiaBoundingBox getBoundingBox() {
        return boundingBox;
    }

    @Override
    public Vector3d getCenterPoint() {
        if (centerPoint == null) {
            if (boundingBox == null) {
                getBoundingBox();
            }
            centerPoint = boundingBox.getCenter();
        }
        return centerPoint;
    }
}
