package com.gaia3d.basic.halfedge;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter

public class HalfEdgeCollapseData {
    public String note = "";
    // This class contains the data that is implicated to collapse an edge.
    private HalfEdge halfEdgeA;
    private HalfEdge halfEdgeB;
    private HalfEdgeVertex startVertexA;
    private HalfEdgeVertex startVertexB;
    private List<HalfEdge> halfEdgesLoopA; // here is included the halfEdgeA
    private List<HalfEdge> halfEdgesLoopB; // here is included the halfEdgeB
    private List<HalfEdge> halfEdgesAExterior;
    private List<HalfEdge> halfEdgesBExterior;
    private HalfEdgeFace faceA;
    private HalfEdgeFace faceB;

    public boolean checkIfAreEqualHedgesInInteriorAndExterior() {
        if (halfEdgesLoopA == null || halfEdgesAExterior == null) {
            return false;
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////
        int halfEdgesLoopASize = halfEdgesLoopA.size();
        int halfEdgesLoopBSize = halfEdgesLoopB.size();
        int halfEdgesAExteriorSize = halfEdgesAExterior.size();
        int halfEdgesBExteriorSize = halfEdgesBExterior.size();

        // check exteriorA with interiorB.***
        for (int i = 0; i < halfEdgesAExteriorSize; i++) {
            HalfEdge halfEdgeAExt = halfEdgesAExterior.get(i);
            if (halfEdgeAExt == null) {
                note = "halfEdgeAExt == null";
                return false;
            }

            for (int j = 0; j < halfEdgesLoopBSize; j++) {
                HalfEdge halfEdgeLoopB = halfEdgesLoopB.get(j);
                if (halfEdgeLoopB == null) {
                    note = "halfEdgeLoopB == null";
                    return false;
                }

                if (halfEdgeAExt == halfEdgeLoopB) {
                    note = "halfEdgeAExt == halfEdgeLoopB";
                    return true;
                }
            }
        }

        // check exteriorB with interiorA.***
        for (int i = 0; i < halfEdgesBExteriorSize; i++) {
            HalfEdge halfEdgeBExt = halfEdgesBExterior.get(i);
            if (halfEdgeBExt == null) {
                note = "halfEdgeBExt == null";
                return false;
            }

            for (int j = 0; j < halfEdgesLoopASize; j++) {
                HalfEdge halfEdgeLoopA = halfEdgesLoopA.get(j);
                if (halfEdgeLoopA == null) {
                    note = "halfEdgeLoopA == null";
                    return false;
                }

                if (halfEdgeBExt == halfEdgeLoopA) {
                    note = "halfEdgeBExt == halfEdgeLoopA";
                    return true;
                }
            }
        }

        //////////////////////////////////////////////////////////////////////////////////////////////////

        note = "all noraml";
        return false;
    }

    public boolean checkStartVerticesOfExteriorHedges() {
        if (halfEdgesAExterior == null || halfEdgesBExterior == null) {
            return false;
        }

        int halfEdgesAExteriorSize = halfEdgesAExterior.size();
        int halfEdgesBExteriorSize = halfEdgesBExterior.size();

        for (int i = 0; i < halfEdgesAExteriorSize; i++) {
            HalfEdge halfEdgeAExt = halfEdgesAExterior.get(i);
            if (halfEdgeAExt == null) {
                return false;
            }

            if (halfEdgeAExt.getStartVertex() == null) {
                return false;
            }
        }

        for (int i = 0; i < halfEdgesBExteriorSize; i++) {
            HalfEdge halfEdgeBExt = halfEdgesBExterior.get(i);
            if (halfEdgeBExt == null) {
                return false;
            }

            if (halfEdgeBExt.getStartVertex() == null) {
                return false;
            }
        }
        return true;
    }

    public boolean check() {
        if (halfEdgesLoopA == null) {
            note = "halfEdgesLoopA == null";
            return false;
        }

        if (!this.checkStartVerticesOfExteriorHedges()) {
            note = "checkStartVerticesOfExteriorHedges is false";
            return false;
        }

        if (this.checkIfAreEqualHedgesInInteriorAndExterior()) {
            note = "checkIfAreEqualHedgesInInteriorAndExterior is true";
            return false;
        }

        int halfEdgesLoopASize = halfEdgesLoopA.size();
        for (int i = 0; i < halfEdgesLoopASize; i++) {
            HalfEdge halfEdgeLoopA = halfEdgesLoopA.get(i);
            if (halfEdgeLoopA == null) {
                note = "halfEdgeLoopA == null";
                return false;
            }

//            if(halfEdgeLoopA.getStatus() == ObjectStatus.DELETED)
//            {
//                note = "halfEdgeLoopA is deleted";
//                return false;
//            }
        }
        //////////////////////////////////////////////////////////////////////////////////////////////
        if (halfEdgesAExterior == null) {
            note = "halfEdgesAExterior == null";
            return false;
        }

        int halfEdgesAExteriorSize = halfEdgesAExterior.size();
        for (int i = 0; i < halfEdgesAExteriorSize; i++) {
            HalfEdge halfEdgeAExt = halfEdgesAExterior.get(i);
            if (halfEdgeAExt == null) {
                note = "halfEdgeAExt == null";
                return false;
            }

            if (halfEdgeAExt.getStatus() == ObjectStatus.DELETED) {
                note = "halfEdgeAExt is deleted";
                return false;
            }
        }


        return true;
    }
}
