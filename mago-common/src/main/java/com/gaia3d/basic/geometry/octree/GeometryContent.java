package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaTriangle;
import org.joml.Vector3d;

public interface GeometryContent {

    GaiaBoundingBox getBoundingBox();

    Vector3d getCenterPoint();

    //GaiaTriangle getTriangle();
}
