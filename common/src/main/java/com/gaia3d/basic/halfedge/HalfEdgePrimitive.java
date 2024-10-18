package com.gaia3d.basic.halfedge;

import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class HalfEdgePrimitive {
    private Integer accessorIndices = -1;
    private Integer materialIndex = -1;
    private List<HalfEdgeSurface> surfaces = new ArrayList<>();
    private List<HalfEdgeVertex> vertices = new ArrayList<>(); // vertices of all surfaces.***

    public void doTrianglesReduction() {
        for (HalfEdgeSurface surface : surfaces) {
            surface.doTrianglesReduction();
        }

        // Remake vertices.***
        vertices.clear();
        for (HalfEdgeSurface surface : surfaces) {
            this.vertices.addAll(surface.getVertices());
        }
    }

    public void deleteObjects() {
        for (HalfEdgeSurface surface : surfaces) {
            surface.deleteObjects();
        }
        surfaces.clear();
        vertices.clear();
    }

    public void checkSandClockFaces() {
        for (HalfEdgeSurface surface : surfaces) {
            surface.checkSandClockFaces();
        }
    }

    public void transformPoints(Matrix4d finalMatrix) {
        for (HalfEdgeSurface surface : surfaces) {
            surface.transformPoints(finalMatrix);
        }
    }
}
