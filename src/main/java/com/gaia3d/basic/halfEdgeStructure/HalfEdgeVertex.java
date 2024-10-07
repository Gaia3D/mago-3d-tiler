package com.gaia3d.basic.halfEdgeStructure;

import com.gaia3d.basic.structure.GaiaVertex;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class HalfEdgeVertex {
    private Vector2d texcoords;
    private Vector3d position;
    private Vector3d normal;
    private byte[] color;
    private float batchId;
    private HalfEdge outingHalfEdge = null;
    private ObjectStatus status = ObjectStatus.ACTIVE;
    private PositionType positionType = null;

    public String note = null;

    public HalfEdgeVertex(GaiaVertex vertex)
    {
        copyFromGaiaVertex(vertex);
    }

    public void deleteObjects()
    {
        this.texcoords = null;
        this.position = null;
        this.normal = null;
        this.color = null;
        this.batchId = 0;
        this.outingHalfEdge = null;
        this.positionType = null;
        this.status = ObjectStatus.DELETED;
    }

    public void copyFromGaiaVertex(GaiaVertex vertex)
    {
        Vector3d position = vertex.getPosition();
        Vector3d normal = vertex.getNormal();
        Vector2d texcoords = vertex.getTexcoords();
        byte[] color = vertex.getColor();
        float batchId = vertex.getBatchId();

        this.position = new Vector3d(position);

        if (normal != null)
        {
            this.normal = new Vector3d(normal);
        }

        if (texcoords != null)
        {
            this.texcoords = new Vector2d(texcoords);
        }

        if (color != null)
        {
            this.color = color.clone();
        }

        this.batchId = batchId;
    }

    public GaiaVertex toGaiaVertex() {
        GaiaVertex vertex = new GaiaVertex();
        vertex.setPosition(new Vector3d(position));

        if (normal != null)
        {
            vertex.setNormal(new Vector3d(normal));
        }

        if (texcoords != null)
        {
            vertex.setTexcoords(new Vector2d(texcoords));
        }

        if (color != null)
        {
            vertex.setColor(color.clone());
        }

        vertex.setBatchId(batchId);

        return vertex;
    }

    public List<HalfEdge> getOutingHalfEdges(List<HalfEdge> resultHalfEdges)
    {
        if(this.outingHalfEdge == null)
        {
            return resultHalfEdges;
        }

        if(this.outingHalfEdge.getStatus() == ObjectStatus.DELETED)
        {
            int hola = 0;
        }

        if(resultHalfEdges == null)
        {
            resultHalfEdges = new ArrayList<>();
        }

        boolean isInterior = true; // init as true.***
        HalfEdge currentEdge = this.outingHalfEdge;
        resultHalfEdges.add(currentEdge);
        HalfEdge currTwin = currentEdge.getTwin();
        if(currTwin == null)
        {
            isInterior = false;
        }
        else {
            HalfEdge nextOutingEdge = currTwin.getNext();
            while (nextOutingEdge != this.outingHalfEdge)
            {
                resultHalfEdges.add(nextOutingEdge);
                currTwin = nextOutingEdge.getTwin();
                if(currTwin == null)
                {
                    isInterior = false;
                    break;
                }
                nextOutingEdge = currTwin.getNext();
            }
        }

        if(!isInterior)
        {
            // search from incomingEdge.***
            HalfEdge incomingEdge = this.outingHalfEdge.getPrev();
            HalfEdge outingEdge = incomingEdge.getTwin();
            if(outingEdge == null)
            {
                return resultHalfEdges;
            }

            resultHalfEdges.add(outingEdge);

            HalfEdge prevEdge = outingEdge.getPrev();
            HalfEdge prevTwin = prevEdge.getTwin();
            while (prevTwin != null && prevTwin != outingEdge)
            {
                resultHalfEdges.add(prevTwin);
                prevEdge = prevTwin.getPrev();
                if(prevEdge == null)
                {
                    break;
                }
                prevTwin = prevEdge.getTwin();
            }
        }

        return resultHalfEdges;
    }

    public boolean changeOutingHalfEdge()
    {
        List<HalfEdge> outingEdges = this.getOutingHalfEdges(null);
        int edgesCount = outingEdges.size();
        for(int i=0; i<edgesCount; i++)
        {
            HalfEdge edge = outingEdges.get(i);
            if(edge.getStatus() != ObjectStatus.DELETED)
            {
                this.outingHalfEdge = edge;
                return true;
            }
        }

        return true;
    }

    public PositionType getPositionType() {
        if(this.positionType == null)
        {
            if(this.outingHalfEdge != null)
            {
                List<HalfEdge> outingEdges = this.getOutingHalfEdges(null);
                int edgesCount = outingEdges.size();
                for(int i=0; i<edgesCount; i++)
                {
                    HalfEdge edge = outingEdges.get(i);
                    if(edge.getTwin() == null)
                    {
                        this.positionType = PositionType.BOUNDARY_EDGE;
                        break;
                    }
                }

                if(this.positionType == null)
                {
                    this.positionType = PositionType.INTERIOR;
                }
            }
        }
        return positionType;
    }
}
