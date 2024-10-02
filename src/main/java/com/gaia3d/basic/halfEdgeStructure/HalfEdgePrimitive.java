package com.gaia3d.basic.halfEdgeStructure;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class HalfEdgePrimitive {
    @Setter
    @Getter
    private Integer accessorIndices = -1;
    @Setter
    @Getter
    private Integer materialIndex = -1;
    @Setter
    @Getter
    private List<HalfEdgeSurface> surfaces = new ArrayList<>();
    @Setter
    @Getter
    private List<HalfEdgeVertex> vertices = new ArrayList<>(); // vertices of all surfaces.***
}
