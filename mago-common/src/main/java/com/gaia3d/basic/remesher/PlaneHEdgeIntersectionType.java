package com.gaia3d.basic.remesher;

public enum PlaneHEdgeIntersectionType {
    // intersection type between a plane and a half-edge
    NONE,
    START_VERTEX,
    END_VERTEX,
    INNER_INTERSECTION,
    COPLANAR_EDGE
}
