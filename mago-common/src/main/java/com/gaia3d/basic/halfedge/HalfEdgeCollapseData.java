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

        int halfEdgesLoopASize = halfEdgesLoopA.size();
        int halfEdgesLoopBSize = halfEdgesLoopB.size();
        // check exteriorA with interiorB
        for (HalfEdge halfEdgeAExt : halfEdgesAExterior) {
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

        // check exteriorB with interiorA
        for (HalfEdge halfEdgeBExt : halfEdgesBExterior) {
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

        note = "all normal";
        return false;
    }

    public boolean checkStartVerticesOfExteriorHedges() {
        if (halfEdgesAExterior == null || halfEdgesBExterior == null) {
            return false;
        }

        for (HalfEdge halfEdgeAExt : halfEdgesAExterior) {
            if (halfEdgeAExt == null) {
                return false;
            }

            if (halfEdgeAExt.getStartVertex() == null) {
                return false;
            }
        }

        for (HalfEdge halfEdgeBExt : halfEdgesBExterior) {
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

        for (HalfEdge halfEdgeLoopA : halfEdgesLoopA) {
            if (halfEdgeLoopA == null) {
                note = "halfEdgeLoopA == null";
                return false;
            }
        }
        if (halfEdgesAExterior == null) {
            note = "halfEdgesAExterior == null";
            return false;
        }

        for (HalfEdge halfEdgeAExt : halfEdgesAExterior) {
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
