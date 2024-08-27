package com.gaia3d.basic.halfEdgeStructure;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter

public class HalfEdgeFace {
    private HalfEdge halfEdge = null;
    private ObjectStatus status = ObjectStatus.ACTIVE;
}
