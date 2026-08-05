package com.gaia3d.basic.geometry.entities;

import com.gaia3d.basic.halfedge.PlaneType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Slf4j
@Setter
@Getter
public class GaiaAAPlane {
    // Axis Aligned Plane
    private PlaneType planeType = null;
    private Vector3d point = null;

    public GaiaAAPlane() {
        this.planeType = PlaneType.XY;
        this.point = new Vector3d();
    }
}
