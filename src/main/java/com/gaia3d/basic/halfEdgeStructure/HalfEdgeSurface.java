package com.gaia3d.basic.halfEdgeStructure;

import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Setter
@Getter

public class HalfEdgeSurface {

    private List<HalfEdge> halfEdges = new ArrayList<>();
    private List<HalfEdgeVertex> vertices = new ArrayList<>();
    private List<HalfEdgeFace> faces = new ArrayList<>();

    public void setTwins() {
        Map<HalfEdgeVertex, List<HalfEdge>> mapVertexOutingHEdges = new HashMap<>();
        Map<HalfEdgeVertex, List<HalfEdge>> mapVertexIncomingHEdges = new HashMap<>();

        for (HalfEdge halfEdge : halfEdges)
        {
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            HalfEdgeVertex endVertex = halfEdge.getEndVertex();
            List<HalfEdge> outingEdges = mapVertexOutingHEdges.computeIfAbsent(startVertex, k -> new ArrayList<>());
            outingEdges.add(halfEdge);

            List<HalfEdge> incomingEdges = mapVertexIncomingHEdges.computeIfAbsent(endVertex, k -> new ArrayList<>());
            incomingEdges.add(halfEdge);
        }

        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++)
        {
            HalfEdgeVertex vertex = vertices.get(i);
            List<HalfEdge> outingEdges = mapVertexOutingHEdges.get(vertex);
            List<HalfEdge> incomingEdges = mapVertexIncomingHEdges.get(vertex);

            if (outingEdges == null || incomingEdges == null)
            {
                continue;
            }

            int outingEdgesCount = outingEdges.size();
            int incomingEdgesCount = incomingEdges.size();
            for (int j = 0; j < outingEdgesCount; j++)
            {
                HalfEdge outingEdge = outingEdges.get(j);
                if(outingEdge.hasTwin())
                {
                    continue;
                }
                for (int k = 0; k < incomingEdgesCount; k++)
                {
                    HalfEdge incomingEdge = incomingEdges.get(k);
                    if(incomingEdge.hasTwin())
                    {
                        continue;
                    }
                    if (outingEdge.setTwin(incomingEdge))
                    {
                        break;
                    }
                }
            }
        }

        int hola = 0;
    }

    public void doTrianglesReduction()
    {
        // 1rst, find possible halfEdges to remove.***
        // Reasons to remove a halfEdge:
        // 1. The halfEdge is very short. (small length).
        // 2. All triangles around the startVertex has a similar normal.
        //----------------------------------------------------------------

        // Make a map ordered by squaredLength.***
        TreeMap<Double, HalfEdge> mapHalfEdgesOrderedBySquaredLength = new TreeMap<>();
        for (HalfEdge halfEdge : halfEdges)
        {
            double squaredLength = halfEdge.getSquaredLength();
            mapHalfEdgesOrderedBySquaredLength.put(squaredLength, halfEdge);
        }
    }

    public Map getVertexAllOutingEdgesMap(Map<HalfEdgeVertex, List<HalfEdge>> vertexEdgesMap)
    {
        // This function returns a map of all halfEdges that startVertex is the key.***
        if(vertexEdgesMap == null)
        {
            vertexEdgesMap = new HashMap<>();
        }

        for (HalfEdge halfEdge : halfEdges)
        {
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            List<HalfEdge> edges = vertexEdgesMap.get(startVertex);
            if (edges == null)
            {
                edges = new ArrayList<>();
                vertexEdgesMap.put(startVertex, edges);
            }
            edges.add(halfEdge);
        }
        return vertexEdgesMap;
    }

    public void collapseHalfEdge(HalfEdge halfEdge)
    {
        // When collapse a halfEdge, we delete the face, the twin's face, the twin & the startVertex.***
        // When deleting a face, must delete all halfEdges of the face.***
        // must find all halfEdges that startVertex is the deletingVertex, and set as startVertex the endVertex of the deletingHalfEdge.***
        HalfEdge twin = halfEdge.getTwin();
        HalfEdgeVertex deletingVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();
        HalfEdgeFace deletingFace = halfEdge.getFace();
        HalfEdgeFace deletingTwinFace = twin.getFace();

        //*********************************************************************************
        // 1- Delete the 2 faces, the 2 halfEdges, the 2 halfEdgesLoop, the startVertex.***
        //*********************************************************************************
        // delete the 2 faces.***
        deletingFace.setStatus(ObjectStatus.DELETED);
        deletingTwinFace.setStatus(ObjectStatus.DELETED);

        // delete the 2 halfEdges.***
        halfEdge.untwin();
        halfEdge.setStatus(ObjectStatus.DELETED);
        twin.untwin();
        twin.setStatus(ObjectStatus.DELETED);

        // Delete the 2 halfEdgesLoop.***
        List<HalfEdge> keepFutureTwineablesHalfEdges = new ArrayList<>(); // keep here the halfEdges that can be twined in the future.***
        List<HalfEdge> deletingHalfEdgesLoop = new ArrayList<>();
        deletingHalfEdgesLoop = halfEdge.getLoop(deletingHalfEdgesLoop);
        for(HalfEdge deletingHalfEdge : deletingHalfEdgesLoop)
        {
            deletingHalfEdge.untwin();
            deletingHalfEdge.setStatus(ObjectStatus.DELETED);
            keepFutureTwineablesHalfEdges.add(deletingHalfEdge.getTwin());
        }

        List<HalfEdge> deletingTwinHalfEdgesLoop = new ArrayList<>();
        deletingTwinHalfEdgesLoop = twin.getLoop(deletingTwinHalfEdgesLoop);
        for(HalfEdge deletingTwinHalfEdge : deletingTwinHalfEdgesLoop)
        {
            deletingTwinHalfEdge.untwin();
            deletingTwinHalfEdge.setStatus(ObjectStatus.DELETED);
            keepFutureTwineablesHalfEdges.add(deletingTwinHalfEdge.getTwin());
        }

        // delete the startVertex.***
        deletingVertex.setStatus(ObjectStatus.DELETED);

        //**************************************************************************************
        // 2- Set the endVertex to halfEdges that lost the startVertex.***
        //**************************************************************************************
        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap = new HashMap<>();
        vertexAllOutingEdgesMap = getVertexAllOutingEdgesMap(vertexAllOutingEdgesMap);
        List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(deletingVertex);
        if(outingEdges != null)
        {
            for(HalfEdge outingEdge : outingEdges)
            {
                if(keepFutureTwineablesHalfEdges.contains(outingEdge))
                {
                    outingEdge.setStartVertex(endVertex);
                    endVertex.setOutingHalfEdge(outingEdge);
                }
            }
        }

        //**************************************************************************************
        // 3- Set twins between the halfEdges stored in keepFutureTwineablesHalfEdges.***
        //**************************************************************************************
        setTwinsBetweenHalfEdges(keepFutureTwineablesHalfEdges);
    }

    public void setTwinsBetweenHalfEdges(List<HalfEdge> halfEdges) {
        // This function sets the twins between the halfEdges
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++)
        {
            HalfEdge halfEdge = halfEdges.get(i);
            if(halfEdge.getStatus() == ObjectStatus.DELETED || halfEdge.hasTwin())
            {
                continue;
            }

            for (int j = i + 1; j < halfEdgesCount; j++)
            {
                HalfEdge halfEdge2 = halfEdges.get(j);
                if(halfEdge2.getStatus() == ObjectStatus.DELETED || halfEdge2.hasTwin())
                {
                    continue;
                }

                if (halfEdge.setTwin(halfEdge2))
                {
                    break;
                }
            }
        }
    }
}
