package com.gaia3d.basic.halfEdgeStructure;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter

public class HalfEdgeCollapseData {
    // This class contains the data that is implicated to collapse an edge.
    private HalfEdge halfEdgeA;
    private HalfEdge halfEdgeB;

    private HalfEdgeVertex startVertexA;
    private HalfEdgeVertex startVertexB;

    private List<HalfEdge> halfEdgesLoopA; // here is included the halfEdgeA
    private List<HalfEdge> halfEdgesLoopB; // here is included the halfEdgeB

    private List<HalfEdge> halfEdgesATwin;
    private List<HalfEdge> halfEdgesBTwin;

    private HalfEdgeFace faceA;
    private HalfEdgeFace faceB;
}
