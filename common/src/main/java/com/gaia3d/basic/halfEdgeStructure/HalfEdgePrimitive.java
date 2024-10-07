package com.gaia3d.basic.halfEdgeStructure;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter

public class HalfEdgePrimitive {

    private Integer accessorIndices = -1;
    private Integer materialIndex = -1;
    private List<HalfEdgeSurface> surfaces = new ArrayList<>();
    private List<HalfEdgeVertex> vertices = new ArrayList<>(); // vertices of all surfaces.***

    public void doTrianglesReduction()
    {
        for (HalfEdgeSurface surface : surfaces)
        {
            surface.doTrianglesReduction();
        }

        // Remake vertices.***
        vertices.clear();
        for (HalfEdgeSurface surface : surfaces)
        {
            this.vertices.addAll(surface.getVertices());
        }
    }
}
