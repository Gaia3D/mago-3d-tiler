package com.gaia3d.basic.halfEdgeStructure;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter

public class HalfEdgeFace {
    private HalfEdge halfEdge = null;
    private ObjectStatus status = ObjectStatus.ACTIVE;

    public List<HalfEdge> getHalfEdgesLoop(List<HalfEdge> resultHalfEdgesLoop) {
        if(this.halfEdge == null) {
            return resultHalfEdgesLoop;
        }

        if(resultHalfEdgesLoop == null) {
            resultHalfEdgesLoop = new ArrayList<>();
        }

        return this.halfEdge.getLoop(resultHalfEdgesLoop);
    }

    public List<HalfEdgeVertex> getVertices(List<HalfEdgeVertex> resultVertices) {
        if(this.halfEdge == null) {
            return resultVertices;
        }

        if(resultVertices == null) {
            resultVertices = new ArrayList<>();
        }

        List<HalfEdge> halfEdgesLoop = this.halfEdge.getLoop(null);
        for(HalfEdge halfEdge : halfEdgesLoop) {
            resultVertices.add(halfEdge.getStartVertex());
        }

        return resultVertices;
    }
}
