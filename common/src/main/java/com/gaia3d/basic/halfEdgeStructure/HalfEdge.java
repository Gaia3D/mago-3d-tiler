package com.gaia3d.basic.halfEdgeStructure;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
public class HalfEdge {
    private HalfEdge twin = null;
    private HalfEdge next = null;
    private HalfEdgeVertex startVertex = null;
    private HalfEdgeFace face = null;
    private ObjectStatus status = ObjectStatus.ACTIVE;

    public boolean setTwin(HalfEdge twin) {
        if(this.isTwineableByPointers(twin)) {
            this.twin = twin;
            twin.twin = this;
            return true;
        }
        return false;
    }

    public void untwin() {
        if(twin != null) {
            twin.twin = null;
            twin = null;
        }
    }

    public boolean hasTwin() {
        return twin != null;
    }

    public HalfEdgeVertex getEndVertex() {
        if(next == null) {
            return null;
        }
        return next.getStartVertex();
    }

    public boolean isTwineableByPointers(HalfEdge twin) {
        HalfEdgeVertex thisStartVertex = this.getStartVertex();
        HalfEdgeVertex thisEndVertex = this.getEndVertex();
        HalfEdgeVertex twinStartVertex = twin.getStartVertex();
        HalfEdgeVertex twinEndVertex = twin.getEndVertex();

        if(thisStartVertex == twinEndVertex && thisEndVertex == twinStartVertex) {
            return true;
        }
        return false;
    }

    public double getSquaredLength() {
        if(startVertex == null || next == null) {
            return -1;
        }
        return startVertex.getPosition().distanceSquared(next.getStartVertex().getPosition());
    }

    public double getLength() {
        return Math.sqrt(getSquaredLength());
    }

    public List<HalfEdge> getLoop(List<HalfEdge> resultHalfEdgesLoop)
    {
        if (resultHalfEdgesLoop == null)
        {
            resultHalfEdgesLoop = new ArrayList<>();
        }
        resultHalfEdgesLoop.add(this);
        HalfEdge nextHalfEdge = this.next;
        while (nextHalfEdge != null && nextHalfEdge != this)
        {
            resultHalfEdgesLoop.add(nextHalfEdge);
            nextHalfEdge = nextHalfEdge.next;
        }
        return resultHalfEdgesLoop;
    }
}
