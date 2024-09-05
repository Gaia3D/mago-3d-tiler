package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaFace;
import com.gaia3d.basic.structure.GaiaPrimitive;
import com.gaia3d.basic.structure.GaiaScene;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.joml.Vector4d;

@Slf4j
@Setter
@Getter

public class GaiaFaceData {
    //**********************************************************
    // This class is used by GaiaOctree to store the face data.
    //**********************************************************
    private GaiaScene sceneParent = null;
    private GaiaPrimitive primitiveParent = null;
    private GaiaFace face = null;
    private GaiaBoundingBox boundingBox = null;
    private Vector3d centerPoint = null;
    private Vector4d averageColor = null; // Average color of the face.***

    public GaiaBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            if(primitiveParent != null) {
                boundingBox = face.getBoundingBox(primitiveParent.getVertices(), new GaiaBoundingBox());
            } else {
                log.error("[Error][getBoundingBox] : primitiveParent is null.");
            }
        }
        return boundingBox;
    }

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
