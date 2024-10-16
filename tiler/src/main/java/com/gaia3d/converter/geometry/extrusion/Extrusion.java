package com.gaia3d.converter.geometry.extrusion;

import com.gaia3d.converter.geometry.GaiaTriangle;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joml.Vector3d;

import java.util.List;

@AllArgsConstructor
@Getter
public class Extrusion {
    private final List<GaiaTriangle> triangles;
    private final List<Vector3d> positions;
}
