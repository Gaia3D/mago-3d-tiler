package com.gaia3d.converter.geometry.tessellator;

import com.gaia3d.basic.structure.GaiaVertex;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@AllArgsConstructor
@Getter
public class GaiaExtrusionSurface {
    //private final List<Integer> indices = new ArrayList<>();
    private final List<Vector3d> vertices;
}