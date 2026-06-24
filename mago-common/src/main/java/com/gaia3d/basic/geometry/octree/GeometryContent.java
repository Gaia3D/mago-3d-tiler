package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import org.joml.Vector3d;

public interface GeometryContent {

    GaiaBoundingBox getBoundingBox();

    Vector3d getCenterPoint();

}
