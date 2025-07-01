package com.gaia3d.basic.geometry.tessellator;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.joml.Vector3d;

import java.util.List;

@AllArgsConstructor
@Getter
public class GaiaExtrusionSurface {
    private final List<Vector3d> vertices;
}