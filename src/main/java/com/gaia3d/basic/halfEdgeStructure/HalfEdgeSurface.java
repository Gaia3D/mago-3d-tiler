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

            if(startVertex == endVertex)
            {
                int hola = 0;
            }
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
                    if(outingEdge == incomingEdge)
                    {
                        int hola = 0;
                    }

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
        double averageSquaredLength = 0.0;
        for (HalfEdge halfEdge : halfEdges)
        {
            double squaredLength = halfEdge.getSquaredLength();
            mapHalfEdgesOrderedBySquaredLength.put(squaredLength, halfEdge);
            averageSquaredLength += squaredLength;
        }
        averageSquaredLength /= halfEdges.size();
        double averageLength = Math.sqrt(averageSquaredLength);

        double minSquaredLength = averageSquaredLength * 100.0; // 50% of the squared average length.***
        List<HalfEdge> orderedHalfEdges = new ArrayList<>(mapHalfEdgesOrderedBySquaredLength.values());
        int halfEdgesCount = orderedHalfEdges.size();
        System.out.println("halfEdgesCount = " + halfEdgesCount);
        int edgesDeletedCount = 0;
        int counterAux = 0;
        for (int i = 0; i < halfEdgesCount; i++)
        {
            HalfEdge halfEdge = orderedHalfEdges.get(i);
            if(halfEdge.getStatus() == ObjectStatus.DELETED)
            {
                continue;
            }

            if(!halfEdge.hasTwin())
            {
                continue;
            }

            if(halfEdge.isDegenerated())
            {
                //halfEdge.setStatus(ObjectStatus.DELETED);
                //continue;
            }

            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            PositionType positionType = startVertex.getPositionType();
            if(positionType != PositionType.INTERIOR)
            {
                continue;
            }

            if(halfEdge.getStartVertex().getPosition() == null)
            {
                int hola = 0;
            }

            if(halfEdge.getEndVertex().getPosition() == null)
            {
                int hola = 0;
            }

            if(halfEdge.getNext().getStartVertex().getPosition() == null)
            {
                int hola = 0;
            }

            if(halfEdge.getNext().getEndVertex().getPosition() == null)
            {
                int hola = 0;
            }

            if(halfEdge.getSquaredLength() < minSquaredLength)
            {
                boolean testDebug = false;
                if(i == 60610)
                {
                    int hola = 0;
                    testDebug = true;
                }
                collapseHalfEdge(halfEdge, testDebug);
                //this.check(); // test debug.***
                edgesDeletedCount++;
                counterAux++;

                if(counterAux >= 2000)
                {
                    counterAux = 0;
                    System.out.println("halfEdges deleted = " + edgesDeletedCount);
                }
            }
            else {
                break;  // the halfEdges are ordered by squaredLength.***
            }
        }

        // delete objects that status is DELETED.***
        // delete halfEdges that status is DELETED.***
        halfEdgesCount = this.halfEdges.size();
        List<HalfEdge> copyHalfEdges = new ArrayList<>(this.halfEdges);
        this.halfEdges.clear();
        for (int i = 0; i < halfEdgesCount; i++)
        {
            HalfEdge halfEdge = copyHalfEdges.get(i);
            if(halfEdge.getStatus() != ObjectStatus.DELETED)
            {
                this.halfEdges.add(halfEdge);
            }
        }

        // delete vertices that status is DELETED.***
        int verticesCount = this.vertices.size();
        List<HalfEdgeVertex> copyVertices = new ArrayList<>(this.vertices);
        this.vertices.clear();
        for (int i = 0; i < verticesCount; i++)
        {
            HalfEdgeVertex vertex = copyVertices.get(i);
            if(vertex.getStatus() != ObjectStatus.DELETED)
            {
                this.vertices.add(vertex);
            }
        }

        // delete faces that status is DELETED.***
        int facesCount = this.faces.size();
        List<HalfEdgeFace> copyFaces = new ArrayList<>(this.faces);
        this.faces.clear();
        for (int i = 0; i < facesCount; i++)
        {
            HalfEdgeFace face = copyFaces.get(i);
            if(face.getStatus() != ObjectStatus.DELETED)
            {
                this.faces.add(face);
            }
        }

        // Finally check the halfEdges.***
        this.check();

        int hola = 0;
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
            if(halfEdge.getStatus() == ObjectStatus.DELETED)
            {
                int hola = 0;
                continue;
            }
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            if(startVertex.getStatus() == ObjectStatus.DELETED)
            {
                int hola = 0;
                continue;
            }
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

    private boolean check()
    {
        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++)
        {
            HalfEdgeVertex vertex = vertices.get(i);
            if(vertex.getStatus() != ObjectStatus.DELETED)
            {
                if(vertex.getOutingHalfEdge().getStatus() == ObjectStatus.DELETED)
                {
                    int hola = 0;
                }
            }
        }

        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++)
        {
            HalfEdge hedge = halfEdges.get(i);
            if(hedge.getStatus() == ObjectStatus.DELETED)
            {
                continue;
            }
            if(hedge.isDegenerated())
            {
                int hola = 0;
            }


        }

        hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++)
        {
            HalfEdge hedge = halfEdges.get(i);
            ObjectStatus status = hedge.getStatus();
            List<HalfEdge> hedgesLoop = new ArrayList<>();

            hedgesLoop = hedge.getLoop(hedgesLoop);
            int hedgeLoopCount = hedgesLoop.size();
            for (int j = 0; j < hedgeLoopCount; j++)
            {
                HalfEdge hedgeLoop = hedgesLoop.get(j);
                if(hedgeLoop.getStatus() != status)
                {
                    int hola = 0;
                }

                if(hedgeLoop.getStartVertex() == null)
                {
                    int hola = 0;
                }

                if(status != ObjectStatus.DELETED)
                {
                    if(hedgeLoop.getPrev() == null)
                    {
                        int hola = 0;
                    }
                    if(hedgeLoop.getStartVertex().getStatus() == ObjectStatus.DELETED)
                    {
                        int hola = 0;
                    }
                }
            }

        }
        return true;
    }

    private HalfEdgeCollapseData getHalfEdgeCollapsingData(HalfEdge halfEdge, HalfEdgeCollapseData resultHalfEdgeCollapseData)
    {
        if(resultHalfEdgeCollapseData == null)
        {
            resultHalfEdgeCollapseData = new HalfEdgeCollapseData();
        }

        // HalfEdge A.***
        resultHalfEdgeCollapseData.setHalfEdgeA(halfEdge);
        resultHalfEdgeCollapseData.setStartVertexA(halfEdge.getStartVertex());

        List<HalfEdge> halfEdgesLoopA = new ArrayList<>();
        halfEdgesLoopA = halfEdge.getLoop(halfEdgesLoopA);
        resultHalfEdgeCollapseData.setHalfEdgesLoopA(halfEdgesLoopA);

        List<HalfEdge> halfEdgesATwin = new ArrayList<>();
        int hedgesCount = halfEdgesLoopA.size();
        for (int i = 0; i < hedgesCount; i++)
        {
            HalfEdge hedge = halfEdgesLoopA.get(i);
            HalfEdge twin = hedge.getTwin();
            if(twin != null && twin != hedge)
            {
                halfEdgesATwin.add(twin);
            }
        }
        resultHalfEdgeCollapseData.setHalfEdgesATwin(halfEdgesATwin);

        resultHalfEdgeCollapseData.setFaceA(halfEdge.getFace());

        // HalfEdge B.***
        HalfEdge twin = halfEdge.getTwin();
        if(twin == null)
        {
            return resultHalfEdgeCollapseData;
        }

        resultHalfEdgeCollapseData.setHalfEdgeB(twin);
        resultHalfEdgeCollapseData.setStartVertexB(twin.getStartVertex());

        List<HalfEdge> halfEdgesLoopB = new ArrayList<>();
        halfEdgesLoopB = twin.getLoop(halfEdgesLoopB);
        resultHalfEdgeCollapseData.setHalfEdgesLoopB(halfEdgesLoopB);

        List<HalfEdge> halfEdgesBTwin = new ArrayList<>();
        hedgesCount = halfEdgesLoopB.size();
        for (int i = 0; i < hedgesCount; i++)
        {
            HalfEdge hedge = halfEdgesLoopB.get(i);
            HalfEdge twin2 = hedge.getTwin();
            if(twin2 != null && twin2 != hedge)
            {
                halfEdgesBTwin.add(twin2);
            }
        }
        resultHalfEdgeCollapseData.setHalfEdgesBTwin(halfEdgesBTwin);

        resultHalfEdgeCollapseData.setFaceB(twin.getFace());

        return resultHalfEdgeCollapseData;
    }

    public void collapseHalfEdge(HalfEdge halfEdge, boolean testDebug)
    {
        // When collapse a halfEdge, we delete the face, the twin's face, the twin & the startVertex.***
        // When deleting a face, must delete all halfEdges of the face.***
        // must find all halfEdges that startVertex is the deletingVertex, and set as startVertex the endVertex of the deletingHalfEdge.***
        HalfEdgeCollapseData halfEdgeCollapseData = getHalfEdgeCollapsingData(halfEdge, null);
        HalfEdgeVertex deletingVertex = halfEdgeCollapseData.getStartVertexA();
        List<HalfEdge> deletingHalfEdgesLoop = halfEdgeCollapseData.getHalfEdgesLoopA();

        // twin data.***
        HalfEdge twin = halfEdgeCollapseData.getHalfEdgeB();
        List<HalfEdge> deletingTwinHalfEdgesLoop = halfEdgeCollapseData.getHalfEdgesLoopB();

        List<HalfEdge> outingEdgesOfDeletingVertex = deletingVertex.getOutingHalfEdges(null);

        // check code.*****************************************************************************************
//        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap = new HashMap<>();
//        vertexAllOutingEdgesMap = getVertexAllOutingEdgesMap(vertexAllOutingEdgesMap);
//        List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(deletingVertex);
//
//        if(outingEdges.size() != outingEdgesOfDeletingVertex.size())
//        {
//            int hola = 0;
//        }
        // End check code.--------------------------------------------------------------------------------------

        HalfEdgeVertex endVertex = halfEdge.getEndVertex();
        HalfEdgeFace deletingFace = halfEdge.getFace();
        HalfEdgeFace deletingTwinFace = twin.getFace();

        if(deletingVertex == endVertex)
        {
            int hola = 0;
        }

        if(endVertex.getStatus() == ObjectStatus.DELETED)
        {
            int hola = 0;
        }

        //*********************************************************************************
        // 1- Delete the 2 faces, the 2 halfEdges, the 2 halfEdgesLoop, the startVertex.***
        //*********************************************************************************
        // delete the 2 faces.***
        deletingFace.setStatus(ObjectStatus.DELETED);
        deletingTwinFace.setStatus(ObjectStatus.DELETED);

        // Delete the 2 halfEdgesLoop.***
        List<HalfEdge> keepFutureTwineablesHalfEdges = new ArrayList<>(); // keep here the halfEdges that can be twined in the future.***
        List<HalfEdgeVertex> vertexThatMustChangeOutingHalfEdge = new ArrayList<>();
        //this.check();

        // Side A.**************************************************************************
        int counterAux = 0;
        for(HalfEdge deletingHalfEdge : deletingHalfEdgesLoop)
        {
            deletingHalfEdge.setStatus(ObjectStatus.DELETED);
            HalfEdgeVertex startVertex = deletingHalfEdge.getStartVertex();
            if(startVertex != null && startVertex.getOutingHalfEdge() == deletingHalfEdge)
            {
                vertexThatMustChangeOutingHalfEdge.add(startVertex);
                startVertex.note = "mustChange-outingHalfEdge_DIRECT";
                //this.check();
                startVertex.changeOutingHalfEdge();
                //this.check();
            }

            deletingHalfEdge.note = "deleted-in-collapseHalfEdge_DIRECT";
            HalfEdge deletingTwin = deletingHalfEdge.getTwin();
            if(deletingTwin != null)
            {
                keepFutureTwineablesHalfEdges.add(deletingHalfEdge.getTwin());
            }
            //this.check();
            deletingHalfEdge.untwin();
            //this.check();
            //deletingHalfEdge.setStartVertex(null);
            //deletingHalfEdge.setNext(null);
            //this.check();

            counterAux++;
        }

        if(endVertex.getStatus() == ObjectStatus.DELETED)
        {
            int hola = 0;
        }

        // Side B.***************************************************************************
        for(HalfEdge deletingTwinHalfEdge : deletingTwinHalfEdgesLoop)
        {
            deletingTwinHalfEdge.setStatus(ObjectStatus.DELETED);
            HalfEdgeVertex startVertex = deletingTwinHalfEdge.getStartVertex();
            if(startVertex != null && startVertex.getOutingHalfEdge() == deletingTwinHalfEdge)
            {
                vertexThatMustChangeOutingHalfEdge.add(startVertex);
                startVertex.note = "mustChange-outingHalfEdge_TWIN";
                startVertex.changeOutingHalfEdge();
                //this.check();
            }

            deletingTwinHalfEdge.note = "deleted-in-collapseHalfEdge_TWIN";
            HalfEdge deletingTwinHalfEdgeTwin = deletingTwinHalfEdge.getTwin();
            if(deletingTwinHalfEdgeTwin != null)
            {
                keepFutureTwineablesHalfEdges.add(deletingTwinHalfEdge.getTwin());
            }
            deletingTwinHalfEdge.untwin();
            //deletingTwinHalfEdge.setStartVertex(null);
            //deletingTwinHalfEdge.setNext(null);
        }

        if(endVertex.getStatus() == ObjectStatus.DELETED)
        {
            int hola = 0;
        }


        // delete the 2 halfEdges.***
        halfEdge.untwin();
        halfEdge.setStatus(ObjectStatus.DELETED);
        twin.untwin();
        twin.setStatus(ObjectStatus.DELETED);

        if(endVertex.getStatus() == ObjectStatus.DELETED)
        {
            int hola = 0;
        }

        // delete the startVertex.***
        deletingVertex.setStatus(ObjectStatus.DELETED);
        //deletingVertex.deleteObjects();
        deletingVertex.note = "deleted-in-collapseHalfEdge";

        if(endVertex.getStatus() == ObjectStatus.DELETED)
        {
            int hola = 0;
        }

        //**************************************************************************************
        // 2- Set the endVertex to halfEdges that lost the startVertex.***
        //**************************************************************************************

//        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap = new HashMap<>();
//        vertexAllOutingEdgesMap = getVertexAllOutingEdgesMap(vertexAllOutingEdgesMap);
//        List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(deletingVertex);


        if(outingEdgesOfDeletingVertex != null)
        {
            for(HalfEdge outingEdge : outingEdgesOfDeletingVertex)
            {
                if(outingEdge.getStatus() == ObjectStatus.DELETED)
                {
                    continue;
                }
                if(endVertex.getStatus() == ObjectStatus.DELETED)
                {
                    int hola = 0;
                }

                if(outingEdge.isDegenerated())
                {
                    int hola = 0;
                }
                outingEdge.setStartVertex(endVertex);
                if(outingEdge.isDegenerated())
                {
                    int hola = 0;
                }
                outingEdge.note = "Reasigned StartVertex-in-collapseHalfEdge";
                endVertex.setOutingHalfEdge(outingEdge);
            }
        }



        //**************************************************************************************
        // 3- Set twins between the halfEdges stored in keepFutureTwineablesHalfEdges.***
        //**************************************************************************************
        setTwinsBetweenHalfEdges(keepFutureTwineablesHalfEdges);
    }

    public void collapseHalfEdge_original(HalfEdge halfEdge, boolean testDebug)
    {
        // When collapse a halfEdge, we delete the face, the twin's face, the twin & the startVertex.***
        // When deleting a face, must delete all halfEdges of the face.***
        // must find all halfEdges that startVertex is the deletingVertex, and set as startVertex the endVertex of the deletingHalfEdge.***
        HalfEdge twin = halfEdge.getTwin();
        HalfEdgeVertex deletingVertex = halfEdge.getStartVertex();

        List<HalfEdge> deletingHalfEdgesLoop = new ArrayList<>();
        deletingHalfEdgesLoop = halfEdge.getLoop(deletingHalfEdgesLoop);

        List<HalfEdge> deletingTwinHalfEdgesLoop = new ArrayList<>();
        deletingTwinHalfEdgesLoop = twin.getLoop(deletingTwinHalfEdgesLoop);

        List<HalfEdge> outingEdgesOfDeletingVertex = deletingVertex.getOutingHalfEdges(null);

        // check code.*****************************************************************************************
//        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap = new HashMap<>();
//        vertexAllOutingEdgesMap = getVertexAllOutingEdgesMap(vertexAllOutingEdgesMap);
//        List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(deletingVertex);
//
//        if(outingEdges.size() != outingEdgesOfDeletingVertex.size())
//        {
//            int hola = 0;
//        }
        // End check code.--------------------------------------------------------------------------------------

        HalfEdgeVertex endVertex = halfEdge.getEndVertex();
        HalfEdgeFace deletingFace = halfEdge.getFace();
        HalfEdgeFace deletingTwinFace = twin.getFace();

        if(deletingVertex == endVertex)
        {
            int hola = 0;
        }

        if(endVertex.getStatus() == ObjectStatus.DELETED)
        {
            int hola = 0;
        }

        //*********************************************************************************
        // 1- Delete the 2 faces, the 2 halfEdges, the 2 halfEdgesLoop, the startVertex.***
        //*********************************************************************************
        // delete the 2 faces.***
        deletingFace.setStatus(ObjectStatus.DELETED);
        deletingTwinFace.setStatus(ObjectStatus.DELETED);

        // Delete the 2 halfEdgesLoop.***
        List<HalfEdge> keepFutureTwineablesHalfEdges = new ArrayList<>(); // keep here the halfEdges that can be twined in the future.***
        List<HalfEdgeVertex> vertexThatMustChangeOutingHalfEdge = new ArrayList<>();
        //this.check();

        // Side A.**************************************************************************
        int counterAux = 0;
        for(HalfEdge deletingHalfEdge : deletingHalfEdgesLoop)
        {
            deletingHalfEdge.setStatus(ObjectStatus.DELETED);
            HalfEdgeVertex startVertex = deletingHalfEdge.getStartVertex();
            if(startVertex != null && startVertex.getOutingHalfEdge() == deletingHalfEdge)
            {
                vertexThatMustChangeOutingHalfEdge.add(startVertex);
                startVertex.note = "mustChange-outingHalfEdge_DIRECT";
                //this.check();
                startVertex.changeOutingHalfEdge();
                //this.check();
            }

            deletingHalfEdge.note = "deleted-in-collapseHalfEdge_DIRECT";
            HalfEdge deletingTwin = deletingHalfEdge.getTwin();
            if(deletingTwin != null)
            {
                keepFutureTwineablesHalfEdges.add(deletingHalfEdge.getTwin());
            }
            //this.check();
            deletingHalfEdge.untwin();
            //this.check();
            //deletingHalfEdge.setStartVertex(null);
            //deletingHalfEdge.setNext(null);
            //this.check();

            counterAux++;
        }

        if(endVertex.getStatus() == ObjectStatus.DELETED)
        {
            int hola = 0;
        }

        // Side B.***************************************************************************
        for(HalfEdge deletingTwinHalfEdge : deletingTwinHalfEdgesLoop)
        {
            deletingTwinHalfEdge.setStatus(ObjectStatus.DELETED);
            HalfEdgeVertex startVertex = deletingTwinHalfEdge.getStartVertex();
            if(startVertex != null && startVertex.getOutingHalfEdge() == deletingTwinHalfEdge)
            {
                vertexThatMustChangeOutingHalfEdge.add(startVertex);
                startVertex.note = "mustChange-outingHalfEdge_TWIN";
                startVertex.changeOutingHalfEdge();
                //this.check();
            }

            deletingTwinHalfEdge.note = "deleted-in-collapseHalfEdge_TWIN";
            HalfEdge deletingTwinHalfEdgeTwin = deletingTwinHalfEdge.getTwin();
            if(deletingTwinHalfEdgeTwin != null)
            {
                keepFutureTwineablesHalfEdges.add(deletingTwinHalfEdge.getTwin());
            }
            deletingTwinHalfEdge.untwin();
            //deletingTwinHalfEdge.setStartVertex(null);
            //deletingTwinHalfEdge.setNext(null);
        }

        if(endVertex.getStatus() == ObjectStatus.DELETED)
        {
            int hola = 0;
        }


        // delete the 2 halfEdges.***
        halfEdge.untwin();
        halfEdge.setStatus(ObjectStatus.DELETED);
        twin.untwin();
        twin.setStatus(ObjectStatus.DELETED);

        if(endVertex.getStatus() == ObjectStatus.DELETED)
        {
            int hola = 0;
        }

        // delete the startVertex.***
        deletingVertex.setStatus(ObjectStatus.DELETED);
        //deletingVertex.deleteObjects();
        deletingVertex.note = "deleted-in-collapseHalfEdge";

        if(endVertex.getStatus() == ObjectStatus.DELETED)
        {
            int hola = 0;
        }

        //**************************************************************************************
        // 2- Set the endVertex to halfEdges that lost the startVertex.***
        //**************************************************************************************

//        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap = new HashMap<>();
//        vertexAllOutingEdgesMap = getVertexAllOutingEdgesMap(vertexAllOutingEdgesMap);
//        List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(deletingVertex);


        if(outingEdgesOfDeletingVertex != null)
        {
            for(HalfEdge outingEdge : outingEdgesOfDeletingVertex)
            {
                if(outingEdge.getStatus() == ObjectStatus.DELETED)
                {
                    continue;
                }
                if(endVertex.getStatus() == ObjectStatus.DELETED)
                {
                    int hola = 0;
                }

                if(outingEdge.isDegenerated())
                {
                    int hola = 0;
                }
                outingEdge.setStartVertex(endVertex);
                if(outingEdge.isDegenerated())
                {
                    int hola = 0;
                }
                outingEdge.note = "Reasigned StartVertex-in-collapseHalfEdge";
                endVertex.setOutingHalfEdge(outingEdge);
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
            if(halfEdge == null)
            {
                int hola = 0;
            }
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
