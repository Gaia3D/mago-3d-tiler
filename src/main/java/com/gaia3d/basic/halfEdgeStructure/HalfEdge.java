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

    public String note = null;

    public HalfEdge() {

    }

    public void setStartVertex(HalfEdgeVertex startVertex) {
        this.startVertex = startVertex;
        if(startVertex != null) {
            startVertex.setOutingHalfEdge(this);
        }
    }

    public boolean setTwin(HalfEdge twin) {
        if(twin != null && this.isTwineableByPointers(twin)) {
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
        if(this.twin == null)
        {
            return false;
        }
        else {
            if(this.twin.getStatus() == ObjectStatus.DELETED) {
                this.twin.setTwin(null);
                this.twin = null;
                return false;
            }
        }
        return true;
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

    public boolean isTwin(HalfEdge halfEdge) {
        if(halfEdge == null || halfEdge.twin == null) {
            return false;
        }

        if(this.twin == null) {
            return false;
        }

        return halfEdge.twin == this && this.twin == halfEdge;
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

    public HalfEdge getPrev() {
        HalfEdge prev = this;
        while (prev.next != this) {
            prev = prev.next;
            if(prev == null) {
                return null;
            }
        }
        return prev;
    }

    public boolean isDegenerated()
    {
        HalfEdgeVertex startVertex = this.getStartVertex();
        HalfEdgeVertex endVertex = this.getEndVertex();

        if(startVertex == endVertex)
        {
            return true;
        }

        return false;
    }

    public void breakRelations()
    {
        if(this.startVertex != null)
        {
            this.startVertex.setOutingHalfEdge(null);
            this.startVertex = null;
        }

        if(this.face != null)
        {
            this.face.setHalfEdge(null);
            this.face = null;
        }

        if(this.next != null)
        {
            this.next = null;
        }

        if(this.twin != null)
        {
            this.twin.twin = null;
            this.twin = null;
        }
    }

    public void setItselfAsOutingHalfEdgeToTheStartVertex()
    {
        if(this.startVertex != null)
        {
            this.startVertex.setOutingHalfEdge(this);
        }
    }

    public boolean isApplauseEdge() {
        if(this.twin == null) {
            return false;
        }

        HalfEdgeFace face1 = this.face;
        HalfEdgeFace face2 = this.twin.face;

        if(face1 == null || face2 == null) {
            return false;
        }

        return face1.isApplauseFace(face2);
    }
}
