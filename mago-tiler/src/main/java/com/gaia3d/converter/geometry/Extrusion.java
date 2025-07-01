package com.gaia3d.converter.geometry;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joml.Vector3d;

import java.util.List;

@AllArgsConstructor
@Getter
@Deprecated
public class Extrusion {
    private final List<GaiaTriangle> triangles;
    private final List<Vector3d> positions;
}
