package com.gaia3d.basic.halfEdgeStructure;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

public class HalfEdgePrimitive {
    @Setter
    @Getter
    private List<HalfEdgeSurface> surfaces = new ArrayList<>();
}
