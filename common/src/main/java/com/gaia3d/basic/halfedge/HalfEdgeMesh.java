package com.gaia3d.basic.halfedge;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

public class HalfEdgeMesh {
    @Setter
    @Getter
    private List<HalfEdgePrimitive> primitives = new ArrayList<>();

    public void doTrianglesReduction() {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.doTrianglesReduction();
        }
    }

    public void deleteObjects() {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.deleteObjects();
        }
        primitives.clear();
    }

    public void checkSandClockFaces() {
        for (HalfEdgePrimitive primitive : primitives) {
            primitive.checkSandClockFaces();
        }
    }
}
