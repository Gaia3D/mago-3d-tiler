package com.gaia3d.basic.halfEdgeStructure;

import com.gaia3d.basic.geometry.entities.GaiaSegment;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

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

    public boolean TEST_addRandomPositionToVertices()
    {
        int vertexCount = vertices.size();
        double offset = 2.0;
        for (int i = 0; i < vertexCount; i++)
        {
            HalfEdgeVertex vertex = vertices.get(i);
            PositionType positionType = vertex.getPositionType();
            if(positionType != PositionType.INTERIOR)
            {
                if(vertex.getPosition() != null)
                {
                    //Vector3d randomOffset = new Vector3d(Math.random() * offset, Math.random() * offset, Math.random() * offset);
                    Vector3d randomOffset = new Vector3d(0.0, 0.0, 40.0);
                    vertex.getPosition().add(randomOffset);
                }
            }

        }

        return true;
    }

    public void deleteObjects()
    {
        // delete halfEdges.***
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++)
        {
            HalfEdge halfEdge = halfEdges.get(i);
            halfEdge.breakRelations();
        }
        halfEdges.clear();

        // delete faces.***
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++)
        {
            HalfEdgeFace face = faces.get(i);
            face.breakRelations();
        }
        faces.clear();

        // delete vertices.***
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++)
        {
            HalfEdgeVertex vertex = vertices.get(i);
            vertex.deleteObjects();
        }
        vertices.clear();
    }


    public void doTrianglesReduction()
    {
//        if(this.TEST_addRandomPositionToVertices())
//        {
//            return;
//        }

        // 1rst, find possible halfEdges to remove.***
        // Reasons to remove a halfEdge:
        // 1. The halfEdge is very short. (small length).
        // 2. All triangles around the startVertex has a similar normal.
        //----------------------------------------------------------------
        int originalFacesCount = faces.size();
        int originalHalfEdgesCount = halfEdges.size();
        int originalVerticesCount = vertices.size();

        // Make a map ordered by squaredLength.***
        TreeMap<Double, List<HalfEdge>> mapHalfEdgesOrderedBySquaredLength = new TreeMap<>();
        double averageSquaredLength = 0.0;
        for (HalfEdge halfEdge : halfEdges)
        {
            double squaredLength = halfEdge.getSquaredLength();
            List<HalfEdge> halfEdges = mapHalfEdgesOrderedBySquaredLength.computeIfAbsent(squaredLength, k -> new ArrayList<>());
            halfEdges.add(halfEdge);
            averageSquaredLength += squaredLength;
        }
        averageSquaredLength /= halfEdges.size();
        double averageLength = Math.sqrt(averageSquaredLength);

        double minSquaredLength = averageSquaredLength * 100.0;
        List<List<HalfEdge>> orderedHalfEdgesList = new ArrayList<>(mapHalfEdgesOrderedBySquaredLength.values());
        List<HalfEdge> orderedHalfEdges = new ArrayList<>();

        int orderedHalfEdgesListCount = orderedHalfEdgesList.size();
        for (int i = 0; i < orderedHalfEdgesListCount; i++)
        {
            List<HalfEdge> halfEdges = orderedHalfEdgesList.get(i);
            orderedHalfEdges.addAll(halfEdges);
        }
        int halfEdgesCount = orderedHalfEdges.size();
        System.out.println("halfEdgesCount = " + halfEdgesCount);
        int counterAux = 0;
        int hedgesCollapsedCount = 0;

        for (int i = 0; i < halfEdgesCount; i++)
        {
            HalfEdge halfEdge = orderedHalfEdges.get(i);
            if(halfEdge.getStatus() == ObjectStatus.DELETED)
            {
                continue;
            }

            if(halfEdge.isDegenerated())
            {
                int hola = 0;
            }

            if(!halfEdge.hasTwin())
            {
                // this is frontier halfEdge.***
                if(this.collapseFrontierHalfEdge(halfEdge, i, false))
                {
                    hedgesCollapsedCount++;
                    counterAux++;
                }
                else
                {
                    int hola = 0;
                }

//                if(!this.checkVertices())
//                {
//                    int hola = 0;
//                    setItselfAsOutingHalfEdgeToTheStartVertex();
//                    if(!this.checkVertices())
//                    {
//                        int hola2 = 0;
//                    }
//                }

                continue;
            }

            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            PositionType positionType = startVertex.getPositionType();
            if(positionType != PositionType.INTERIOR)
            {
                continue;
            }


            //if(halfEdge.getSquaredLength() < minSquaredLength)
            {
                boolean testDebug = false;
                if(collapseHalfEdge(halfEdge, i, testDebug))
                {
                    hedgesCollapsedCount++;
                    counterAux++;
                }
                else
                {
                    int hola = 0;
                }
                //this.check(); // test debug.***

                if(counterAux >= 2000)
                {
                    counterAux = 0;
                    System.out.println("halfEdges deleted = " + hedgesCollapsedCount);
                }
            }
//            else {
//                break;  // the halfEdges are ordered by squaredLength.***
//            }

            //if(!this.checkVertices())
            {
                int hola = 0;
//                setItselfAsOutingHalfEdgeToTheStartVertex();
//                if(!this.checkVertices())
//                {
//                    int hola2 = 0;
//                }
            }
        }

        System.out.println("*** TOTAL HALFEDGES DELETED = " + hedgesCollapsedCount);

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
            else
            {
                halfEdge.breakRelations();
            }
        }
        copyHalfEdges.clear();

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
            else {
                vertex.deleteObjects();
            }
        }
        copyVertices.clear();

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
            else
            {
                face.breakRelations();
            }
        }
        copyFaces.clear();

        // Finally check the halfEdges.***
        if(!this.check())
        {
            int hola = 0;
            setItselfAsOutingHalfEdgeToTheStartVertex();
            if(!this.check())
            {
                int hola2 = 0;
            }
        }

        int finalFacesCount = faces.size();
        int finalHalfEdgesCount = halfEdges.size();
        int finalVerticesCount = vertices.size();

        int facesCountDiff = originalFacesCount - finalFacesCount;
        int halfEdgesCountDiff = originalHalfEdgesCount - finalHalfEdgesCount;
        int verticesCountDiff = originalVerticesCount - finalVerticesCount;

        int hola = 0;
    }

    public void setItselfAsOutingHalfEdgeToTheStartVertex()
    {
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++)
        {
            HalfEdge halfEdge = halfEdges.get(i);
            if(halfEdge.getStatus() == ObjectStatus.DELETED)
            {
                continue;
            }
            halfEdge.setItselfAsOutingHalfEdgeToTheStartVertex();
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

    public  boolean checkVertices()
    {
        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++)
        {
            HalfEdgeVertex vertex = vertices.get(i);
            if(vertex.getStatus() == ObjectStatus.DELETED)
            {
                continue;
            }
            if(vertex.getOutingHalfEdge() == null)
            {
                System.out.println("HalfEdgeSurface.checkVertices() : vertex.getOutingHalfEdge() == null.");
                return false;
            }
            if(vertex.getOutingHalfEdge().getStatus() == ObjectStatus.DELETED)
            {
                System.out.println("HalfEdgeSurface.checkVertices() : vertex.getOutingHalfEdge() is deleted!.");
                return false;
            }
        }
        return true;
    }

    private boolean check()
    {
//        int vertexCount = vertices.size();
//        for (int i = 0; i < vertexCount; i++)
//        {
//            HalfEdgeVertex vertex = vertices.get(i);
//            if(vertex.getStatus() != ObjectStatus.DELETED)
//            {
//                if(vertex.getOutingHalfEdge().getStatus() == ObjectStatus.DELETED)
//                {
//                    int hola = 0;
//                }
//            }
//        }

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

//        hedgesCount = halfEdges.size();
//        for (int i = 0; i < hedgesCount; i++)
//        {
//            HalfEdge hedge = halfEdges.get(i);
//            ObjectStatus status = hedge.getStatus();
//            List<HalfEdge> hedgesLoop = new ArrayList<>();
//
//            hedgesLoop = hedge.getLoop(hedgesLoop);
//            int hedgeLoopCount = hedgesLoop.size();
//            for (int j = 0; j < hedgeLoopCount; j++)
//            {
//                HalfEdge hedgeLoop = hedgesLoop.get(j);
//                if(hedgeLoop.getStatus() != status)
//                {
//                    int hola = 0;
//                }
//
//                if(hedgeLoop.getStartVertex() == null)
//                {
//                    int hola = 0;
//                }
//
//                if(status != ObjectStatus.DELETED)
//                {
//                    if(hedgeLoop.getPrev() == null)
//                    {
//                        int hola = 0;
//                    }
//                    if(hedgeLoop.getStartVertex().getStatus() == ObjectStatus.DELETED)
//                    {
//                        int hola = 0;
//                    }
//                }
//            }
//
//        }
        return true;
    }

    private HalfEdgeCollapseData getHalfEdgeCollapsingData(HalfEdge halfEdge, HalfEdgeCollapseData resultHalfEdgeCollapseData)
    {
        if(resultHalfEdgeCollapseData == null)
        {
            resultHalfEdgeCollapseData = new HalfEdgeCollapseData();
        }

        // HalfEdge A.*********************************************************************
        HalfEdge halfEdgeA = halfEdge;
        resultHalfEdgeCollapseData.setHalfEdgeA(halfEdgeA);
        resultHalfEdgeCollapseData.setStartVertexA(halfEdgeA.getStartVertex());

        List<HalfEdge> halfEdgesLoopA = new ArrayList<>();
        halfEdgesLoopA = halfEdgeA.getLoop(halfEdgesLoopA);
        resultHalfEdgeCollapseData.setHalfEdgesLoopA(halfEdgesLoopA);

        List<HalfEdge> halfEdgesAExterior = new ArrayList<>();
        int hedgesCount = halfEdgesLoopA.size();
        for (int i = 0; i < hedgesCount; i++)
        {
            HalfEdge hedgeA = halfEdgesLoopA.get(i);
            if(hedgeA == halfEdgeA)
            {
                continue;
            }
            if(hedgeA.getStatus() == ObjectStatus.DELETED)
            {
                int hola = 0;
            }
            HalfEdge twin = hedgeA.getTwin();
            if(twin != null && twin.getStatus() != ObjectStatus.DELETED)
            {
                halfEdgesAExterior.add(twin);
            }
        }
        resultHalfEdgeCollapseData.setHalfEdgesAExterior(halfEdgesAExterior);
        resultHalfEdgeCollapseData.setFaceA(halfEdgeA.getFace());

        // HalfEdge B.*********************************************************************
        HalfEdge halfEdgeB = halfEdgeA.getTwin();
        if(halfEdgeB == null)
        {
            return resultHalfEdgeCollapseData;
        }

        resultHalfEdgeCollapseData.setHalfEdgeB(halfEdgeB);
        resultHalfEdgeCollapseData.setStartVertexB(halfEdgeB.getStartVertex());

        List<HalfEdge> halfEdgesLoopB = new ArrayList<>();
        halfEdgesLoopB = halfEdgeB.getLoop(halfEdgesLoopB);
        resultHalfEdgeCollapseData.setHalfEdgesLoopB(halfEdgesLoopB);

        List<HalfEdge> halfEdgesBExterior = new ArrayList<>();
        hedgesCount = halfEdgesLoopB.size();
        for (int i = 0; i < hedgesCount; i++)
        {
            HalfEdge hedgeB = halfEdgesLoopB.get(i);
            if(hedgeB == halfEdgeB)
            {
                continue;
            }
            if(hedgeB.getStatus() == ObjectStatus.DELETED)
            {
                int hola = 0;
            }
            HalfEdge twin2 = hedgeB.getTwin();
            if(twin2 != null && twin2.getStatus() != ObjectStatus.DELETED)
            {
                halfEdgesBExterior.add(twin2);
            }
        }
        resultHalfEdgeCollapseData.setHalfEdgesBExterior(halfEdgesBExterior);
        resultHalfEdgeCollapseData.setFaceB(halfEdgeB.getFace());

        return resultHalfEdgeCollapseData;
    }

    public boolean collapseFrontierHalfEdge(HalfEdge halfEdge, int iteration, boolean testDebug)
    {
        // In this case, must find prevFrontierHalfEdge.***
        HalfEdgeVertex startVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();

        List<HalfEdge> incomingEdges = startVertex.getIncomingHalfEdges(null);
        if(incomingEdges == null)
        {
            int hola = 0;
        }
        HalfEdge prevFrontierHalfEdge = null;
        int incomingEdgesCount = incomingEdges.size();
        for (int i = 0; i < incomingEdgesCount; i++)
        {
            HalfEdge incomingEdge = incomingEdges.get(i);
            if(!incomingEdge.hasTwin())
            {
                prevFrontierHalfEdge = incomingEdge;
                break;
            }
        }

        if(prevFrontierHalfEdge == null)
        {
            return false;
        }

        // Calculate the angle between the prevFrontierHalfEdge & the newPrevFrontierHalfEdge.***
        HalfEdgeVertex prevFrontierStartVertex = prevFrontierHalfEdge.getStartVertex();
        GaiaSegment segmentPrevHEdge = new GaiaSegment(prevFrontierStartVertex.getPosition(), startVertex.getPosition());
        GaiaSegment segmentNewPrevHEdge = new GaiaSegment(prevFrontierStartVertex.getPosition(), endVertex.getPosition());
        double angRad = segmentPrevHEdge.angRadToSegment(segmentNewPrevHEdge);
        double angDeg = Math.toDegrees(angRad);

        if(angDeg > 5.0)
        {
            return false;
        }

        HalfEdgeCollapseData halfEdgeCollapseData = getHalfEdgeCollapsingData(halfEdge, null);


        HalfEdgeVertex deletingVertex = halfEdgeCollapseData.getStartVertexA();
        List<HalfEdge> deletingHalfEdgesLoopA = halfEdgeCollapseData.getHalfEdgesLoopA();
        HalfEdgeFace deletingFace = halfEdge.getFace();
        List<HalfEdge> outingEdgesOfDeletingVertex = deletingVertex.getOutingHalfEdges(null);
        List<HalfEdge> outingEdgesOfEndVertex = halfEdge.getEndVertex().getOutingHalfEdges(null);


        if(deletingVertex == endVertex)
        {
            int hola = 0;
        }

        if(endVertex.getStatus() == ObjectStatus.DELETED)
        {
            int hola = 0;
        }

        // Here there are no twin data.***
        //*********************************************************************************
        // 1- Delete the 2 faces, the 2 halfEdges, the 2 halfEdgesLoop, the startVertex.***
        //*********************************************************************************
        // delete the 2 faces.***
        deletingFace.setStatus(ObjectStatus.DELETED);

        // Delete the 2 halfEdgesLoop.***
        List<HalfEdge> keepFutureTwineablesHalfEdges = new ArrayList<>(); // keep here the halfEdges that can be twined in the future.***
        List<HalfEdgeVertex> vertexThatMustChangeOutingHalfEdge = new ArrayList<>();
        //this.check();

        // Side A.**************************************************************************
        int counterAux = 0;
        for(HalfEdge deletingHalfEdgeA : deletingHalfEdgesLoopA)
        {
            deletingHalfEdgeA.setStatus(ObjectStatus.DELETED);
            HalfEdgeVertex startVertexA = deletingHalfEdgeA.getStartVertex();
            if(startVertexA != null)// && startVertex.getOutingHalfEdge() == deletingHalfEdgeA)
            {
                vertexThatMustChangeOutingHalfEdge.add(startVertexA);
                startVertexA.note = "mustChange-outingHalfEdge_DIRECT" + counterAux + "_ITER: " + iteration;
                startVertexA.setOutingHalfEdge(null);
            }

            deletingHalfEdgeA.note = "deleted-in-collapseHalfEdge_DIRECT" + counterAux+ "_ITER: " + iteration;
            deletingHalfEdgeA.breakRelations();
            counterAux++;
        }


        if(endVertex.getStatus() == ObjectStatus.DELETED)
        {
            int hola = 0;
        }

        // delete the startVertex.***
        deletingVertex.setStatus(ObjectStatus.DELETED);
        deletingVertex.deleteObjects();
        deletingVertex.note = "deleted-in-collapseHalfEdge"+ "_ITER: " + iteration;


        //**************************************************************************************
        // 2- Set the endVertex to halfEdges that lost the startVertex.***
        //**************************************************************************************

//        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap = new HashMap<>();
//        vertexAllOutingEdgesMap = getVertexAllOutingEdgesMap(vertexAllOutingEdgesMap);
//        List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(deletingVertex);

        if(outingEdgesOfDeletingVertex == null)
        {
            System.out.println("HalfEdgeSurface.collapseHalfEdge() : outingEdgesOfDeletingVertex == null.");
            return false;
        }

        for(HalfEdge outingEdge : outingEdgesOfDeletingVertex)
        {

            if(outingEdge.getStatus() == ObjectStatus.DELETED)
            {
                continue;
            }

            HalfEdgeVertex startVertexTest = outingEdge.getStartVertex();
            HalfEdgeVertex endVertexTest = outingEdge.getEndVertex();
            Vector3d startPosition = startVertexTest.getPosition();
            Vector3d endPosition = endVertexTest.getPosition();

            if(endVertex.getStatus() == ObjectStatus.DELETED)
            {
                int hola = 0;
            }

            if(outingEdge.isDegenerated())
            {
                int hola = 0;
            }
            HalfEdgeVertex currEndVertex = outingEdge.getEndVertex();
            if(currEndVertex == endVertex)
            {
                int hola = 0;
            }
            outingEdge.setStartVertex(endVertex);
            if(outingEdge.isDegenerated())
            {
                int hola = 0;
            }
            outingEdge.note = "Reasigned StartVertex-in-collapseHalfEdge"+ "_ITER: " + iteration;
            endVertex.setOutingHalfEdge(outingEdge);
        }

        // for all outingHedges, take the HEdgeLoop and set HEdge.setItselfAsOutingEdge for his-startVertex.***
        for(HalfEdge outingEdge : outingEdgesOfDeletingVertex)
        {
            if(outingEdge.getStatus() == ObjectStatus.DELETED)
            {
                continue;
            }
            List<HalfEdge> outingEdgesLoop = new ArrayList<>();
            outingEdgesLoop = outingEdge.getLoop(outingEdgesLoop);
            int outingEdgesLoopCount = outingEdgesLoop.size();
            for (int i = 0; i < outingEdgesLoopCount; i++)
            {
                HalfEdge outingEdgeLoop = outingEdgesLoop.get(i);
                if(outingEdgeLoop.getStatus() == ObjectStatus.DELETED)
                {
                    continue;
                }
                outingEdgeLoop.setItselfAsOutingHalfEdgeToTheStartVertex();
            }
        }

        // for all outingEdges of the endVertex, take the HEdgeLoop and set HEdge.setItselfAsOutingEdge for his-startVertex.***
        for(HalfEdge outingEdge : outingEdgesOfEndVertex)
        {
            if(outingEdge.getStatus() == ObjectStatus.DELETED)
            {
                continue;
            }
            List<HalfEdge> outingEdgesLoop = new ArrayList<>();
            outingEdgesLoop = outingEdge.getLoop(outingEdgesLoop);
            int outingEdgesLoopCount = outingEdgesLoop.size();
            for (int i = 0; i < outingEdgesLoopCount; i++)
            {
                HalfEdge outingEdgeLoop = outingEdgesLoop.get(i);
                if(outingEdgeLoop.getStatus() == ObjectStatus.DELETED)
                {
                    continue;
                }
                outingEdgeLoop.setItselfAsOutingHalfEdgeToTheStartVertex();
            }
        }

//        if(!this.checkVertices())
//        {
//            int hola = 0;
////            setItselfAsOutingHalfEdgeToTheStartVertex();
////            if(!this.checkVertices())
////            {
////                int hola2 = 0;
////            }
//        }

        List<HalfEdge> halfEdgesAExterior = halfEdgeCollapseData.getHalfEdgesAExterior();

        int halfEdgesCount = halfEdgesAExterior.size();
        for (int i = 0; i < halfEdgesCount; i++)
        {
            HalfEdge halfEdgeAExterior = halfEdgesAExterior.get(i);
            HalfEdgeVertex startVertexA = halfEdgeAExterior.getStartVertex();
            if(startVertexA == null)
            {
                int hola = 0;
            }
            startVertexA.setOutingHalfEdge(halfEdgeAExterior);
        }

        //**************************************************************************************
        // 3- Set twins between the halfEdges stored in keepFutureTwineablesHalfEdges.***
        //**************************************************************************************
        setTwinsBetweenHalfEdges(halfEdgeCollapseData.getHalfEdgesAExterior());

        //this.checkVertices();

        return true;
    }

    public boolean collapseHalfEdge(HalfEdge halfEdge, int iteration, boolean testDebug)
    {
        // When collapse a halfEdge, we delete the face, the twin's face, the twin & the startVertex.***
        // When deleting a face, must delete all halfEdges of the face.***
        // must find all halfEdges that startVertex is the deletingVertex, and set as startVertex the endVertex of the deletingHalfEdge.***
        HalfEdgeCollapseData halfEdgeCollapseData = getHalfEdgeCollapsingData(halfEdge, null);

        //this.checkVertices();

        if(!halfEdgeCollapseData.check())
        {
            int hola = 0;
            return false;
        }
        HalfEdgeVertex deletingVertex = halfEdgeCollapseData.getStartVertexA();
        List<HalfEdge> deletingHalfEdgesLoopA = halfEdgeCollapseData.getHalfEdgesLoopA();

        //check();

        // twin data.***
        HalfEdge twin = halfEdgeCollapseData.getHalfEdgeB();
        List<HalfEdge> deletingTwinHalfEdgesLoopB = halfEdgeCollapseData.getHalfEdgesLoopB();

        List<HalfEdge> outingEdgesOfDeletingVertex = deletingVertex.getOutingHalfEdges(null);
        List<HalfEdge> outingEdgesOfEndVertex = halfEdge.getEndVertex().getOutingHalfEdges(null);

        // check if outingHedge.endVertex == endVertex.***
        int outingEdgesOfDeletingVertexCount = outingEdgesOfDeletingVertex.size();
        for (int i = 0; i < outingEdgesOfDeletingVertexCount; i++)
        {
            HalfEdge outingEdge = outingEdgesOfDeletingVertex.get(i);
            if(outingEdge != halfEdge) {
                if (outingEdge.getEndVertex() == halfEdge.getEndVertex()) {
                    int hola = 0;
                    return false;
                }
            }
        }

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

        //this.checkVertices();

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
        for(HalfEdge deletingHalfEdgeA : deletingHalfEdgesLoopA)
        {
            deletingHalfEdgeA.setStatus(ObjectStatus.DELETED);
            HalfEdgeVertex startVertex = deletingHalfEdgeA.getStartVertex();
            if(startVertex != null)// && startVertex.getOutingHalfEdge() == deletingHalfEdgeA)
            {
                vertexThatMustChangeOutingHalfEdge.add(startVertex);
                startVertex.note = "mustChange-outingHalfEdge_DIRECT" + counterAux + "_ITER: " + iteration;
                startVertex.setOutingHalfEdge(null);
            }

            deletingHalfEdgeA.note = "deleted-in-collapseHalfEdge_DIRECT" + counterAux+ "_ITER: " + iteration;
            deletingHalfEdgeA.breakRelations();
            counterAux++;
        }



//        if(!halfEdgeCollapseData.check())
//        {
//            int hola = 0;
//        }

        if(endVertex.getStatus() == ObjectStatus.DELETED)
        {
            int hola = 0;
        }

        // Side B.***************************************************************************
        counterAux = 0;
        for(HalfEdge deletingTwinHalfEdgeB : deletingTwinHalfEdgesLoopB)
        {
            deletingTwinHalfEdgeB.setStatus(ObjectStatus.DELETED);
            HalfEdgeVertex startVertex = deletingTwinHalfEdgeB.getStartVertex();
            if(startVertex != null)// && startVertex.getOutingHalfEdge() == deletingTwinHalfEdgeB)
            {
                vertexThatMustChangeOutingHalfEdge.add(startVertex);
                startVertex.note = "mustChange-outingHalfEdge_TWIN" + counterAux+ "_ITER: " + iteration;
                startVertex.setOutingHalfEdge(null);
            }

            deletingTwinHalfEdgeB.note = "deleted-in-collapseHalfEdge_TWIN" + counterAux+ "_ITER: " + iteration;
            deletingTwinHalfEdgeB.breakRelations();
        }

//        if(!halfEdgeCollapseData.check())
//        {
//            int hola = 0;
//        }

        // delete the startVertex.***
        deletingVertex.setStatus(ObjectStatus.DELETED);
        deletingVertex.deleteObjects();
        deletingVertex.note = "deleted-in-collapseHalfEdge"+ "_ITER: " + iteration;

//        if(!halfEdgeCollapseData.check())
//        {
//            int hola = 0;
//        }

        //**************************************************************************************
        // 2- Set the endVertex to halfEdges that lost the startVertex.***
        //**************************************************************************************

//        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap = new HashMap<>();
//        vertexAllOutingEdgesMap = getVertexAllOutingEdgesMap(vertexAllOutingEdgesMap);
//        List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(deletingVertex);

        if(outingEdgesOfDeletingVertex == null)
        {
            System.out.println("HalfEdgeSurface.collapseHalfEdge() : outingEdgesOfDeletingVertex == null.");
            return false;
        }

        for(HalfEdge outingEdge : outingEdgesOfDeletingVertex)
        {

            if(outingEdge.getStatus() == ObjectStatus.DELETED)
            {
                continue;
            }

            HalfEdgeVertex startVertexTest = outingEdge.getStartVertex();
            HalfEdgeVertex endVertexTest = outingEdge.getEndVertex();
            Vector3d startPosition = startVertexTest.getPosition();
            Vector3d endPosition = endVertexTest.getPosition();

            if(endVertex.getStatus() == ObjectStatus.DELETED)
            {
                int hola = 0;
            }

            if(outingEdge.isDegenerated())
            {
                int hola = 0;
            }
            HalfEdgeVertex currEndVertex = outingEdge.getEndVertex();
            if(currEndVertex == endVertex)
            {
                int hola = 0;
            }
            outingEdge.setStartVertex(endVertex);
            if(outingEdge.isDegenerated())
            {
                int hola = 0;
            }
            outingEdge.note = "Reasigned StartVertex-in-collapseHalfEdge"+ "_ITER: " + iteration;
            endVertex.setOutingHalfEdge(outingEdge);
        }

        // for all outingHedges, take the HEdgeLoop and set HEdge.setItselfAsOutingEdge for his-startVertex.***
        for(HalfEdge outingEdge : outingEdgesOfDeletingVertex)
        {
            if(outingEdge.getStatus() == ObjectStatus.DELETED)
            {
                continue;
            }
            List<HalfEdge> outingEdgesLoop = new ArrayList<>();
            outingEdgesLoop = outingEdge.getLoop(outingEdgesLoop);
            int outingEdgesLoopCount = outingEdgesLoop.size();
            for (int i = 0; i < outingEdgesLoopCount; i++)
            {
                HalfEdge outingEdgeLoop = outingEdgesLoop.get(i);
                if(outingEdgeLoop.getStatus() == ObjectStatus.DELETED)
                {
                    continue;
                }
                outingEdgeLoop.setItselfAsOutingHalfEdgeToTheStartVertex();
            }
        }

        // for all outingEdges of the endVertex, take the HEdgeLoop and set HEdge.setItselfAsOutingEdge for his-startVertex.***
        for(HalfEdge outingEdge : outingEdgesOfEndVertex)
        {
            if(outingEdge.getStatus() == ObjectStatus.DELETED)
            {
                continue;
            }
            List<HalfEdge> outingEdgesLoop = new ArrayList<>();
            outingEdgesLoop = outingEdge.getLoop(outingEdgesLoop);
            int outingEdgesLoopCount = outingEdgesLoop.size();
            for (int i = 0; i < outingEdgesLoopCount; i++)
            {
                HalfEdge outingEdgeLoop = outingEdgesLoop.get(i);
                if(outingEdgeLoop.getStatus() == ObjectStatus.DELETED)
                {
                    continue;
                }
                outingEdgeLoop.setItselfAsOutingHalfEdgeToTheStartVertex();
            }
        }



//        if(!halfEdgeCollapseData.check())
//        {
//            int hola = 0;
//        }

        //this.checkVertices();

        List<HalfEdge> halfEdgesAExterior = halfEdgeCollapseData.getHalfEdgesAExterior();
        List<HalfEdge> halfEdgesBExterior = halfEdgeCollapseData.getHalfEdgesBExterior();

        int halfEdgesCount = halfEdgesAExterior.size();
        for (int i = 0; i < halfEdgesCount; i++)
        {
            HalfEdge halfEdgeAExterior = halfEdgesAExterior.get(i);
            HalfEdgeVertex startVertex = halfEdgeAExterior.getStartVertex();
            if(startVertex == null)
            {
                int hola = 0;
            }
            startVertex.setOutingHalfEdge(halfEdgeAExterior);
        }


        halfEdgesCount = halfEdgesBExterior.size();
        for (int i = 0; i < halfEdgesCount; i++)
        {
            HalfEdge halfEdgeBExterior = halfEdgesBExterior.get(i);
            HalfEdgeVertex startVertex = halfEdgeBExterior.getStartVertex();
            if(startVertex == null)
            {
                int hola = 0;
            }
            startVertex.setOutingHalfEdge(halfEdgeBExterior);
        }

        //this.checkVertices();

//        if(!halfEdgeCollapseData.check())
//        {
//            int hola = 0;
//        }

        //**************************************************************************************
        // 3- Set twins between the halfEdges stored in keepFutureTwineablesHalfEdges.***
        //**************************************************************************************
        setTwinsBetweenHalfEdges(halfEdgeCollapseData.getHalfEdgesAExterior());
        setTwinsBetweenHalfEdges(halfEdgeCollapseData.getHalfEdgesBExterior());

//        if(!halfEdgeCollapseData.check())
//        {
//            int hola = 0;
//        }

        //this.checkVertices();

        //check();

        return true;
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
