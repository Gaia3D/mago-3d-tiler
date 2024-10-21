package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaSegment;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.opengis.geometry.BoundingBox;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.*;

@Slf4j
@Setter
@Getter
public class HalfEdgeSurface implements Serializable {
    private List<HalfEdge> halfEdges = new ArrayList<>();
    private List<HalfEdgeVertex> vertices = new ArrayList<>();
    private List<HalfEdgeFace> faces = new ArrayList<>();
    private GaiaBoundingBox boundingBox = null;

    public void setTwins() {
        Map<HalfEdgeVertex, List<HalfEdge>> mapVertexOutingHEdges = new HashMap<>();
        Map<HalfEdgeVertex, List<HalfEdge>> mapVertexIncomingHEdges = new HashMap<>();

        for (HalfEdge halfEdge : halfEdges) {
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            HalfEdgeVertex endVertex = halfEdge.getEndVertex();

            if (startVertex == endVertex) {
                int hola = 0;
            }
            List<HalfEdge> outingEdges = mapVertexOutingHEdges.computeIfAbsent(startVertex, k -> new ArrayList<>());
            outingEdges.add(halfEdge);

            List<HalfEdge> incomingEdges = mapVertexIncomingHEdges.computeIfAbsent(endVertex, k -> new ArrayList<>());
            incomingEdges.add(halfEdge);
        }

        // make twinables lists.***
        Map<HalfEdge, List<HalfEdge>> mapHalfEdgeTwinables = new HashMap<>();
        Map<HalfEdge, HalfEdge> mapRemovedHalfEdges = new HashMap<>();
        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            List<HalfEdge> outingEdges = mapVertexOutingHEdges.get(vertex);
            List<HalfEdge> incomingEdges = mapVertexIncomingHEdges.get(vertex);

            if (outingEdges == null || incomingEdges == null) {
                continue;
            }

            int outingEdgesCount = outingEdges.size();
            int incomingEdgesCount = incomingEdges.size();
            for (int j = 0; j < outingEdgesCount; j++) {
                HalfEdge outingEdge = outingEdges.get(j);

//                if(mapRemovedHalfEdges.get(outingEdge) != null)
//                {
//                    continue;
//                }

                for (int k = 0; k < incomingEdgesCount; k++) {
                    HalfEdge incomingEdge = incomingEdges.get(k);

//                    if(mapRemovedHalfEdges.get(incomingEdge) != null)
//                    {
//                        continue;
//                    }

                    if (incomingEdge.isTwineableByPointers(outingEdge)) {
                        List<HalfEdge> twinables = mapHalfEdgeTwinables.computeIfAbsent(outingEdge, k2 -> new ArrayList<>());
//                        if(!twinables.isEmpty())
//                        {
//                            mapRemovedHalfEdges.put(incomingEdge, incomingEdge);
//                        }
//                        else
                        {
                            twinables.add(incomingEdge);
                        }

                    }
                }
            }
        }

        // now set twins.***
        Set<HalfEdge> halfEdgesSet2 = mapHalfEdgeTwinables.keySet();
        for (HalfEdge halfEdge : halfEdgesSet2) {
            if (halfEdge.hasTwin()) {
                continue;
            }
            List<HalfEdge> twinables = mapHalfEdgeTwinables.get(halfEdge);
            for (int i = 0; i < twinables.size(); i++) {
                HalfEdge twinable = twinables.get(i);
                if (twinable.hasTwin()) {
                    continue;
                }
                if (halfEdge.setTwin(twinable)) {
                    int hola = 0;
                    break;
                }
            }
        }

        // now collect hedges that has not twin.***
        List<HalfEdge> singleHalfEdges = new ArrayList<>();
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (!hedge.hasTwin()) {
                singleHalfEdges.add(hedge);
                singleHalfEdges.add(hedge.getNext());
            } else {
                hedge.setItselfAsOutingHalfEdgeToTheStartVertex();
            }
        }

        // now manage the removed halfEdges.************************************************************************
        List<HalfEdge> removedHalfEdges = singleHalfEdges;
        Map<HalfEdgeVertex, HalfEdgeVertex> mapVertexToNewVertex = new HashMap<>();
        int removedHalfEdgesCount = removedHalfEdges.size();
        for (int i = 0; i < removedHalfEdgesCount; i++) {
            HalfEdge removedHalfEdge = removedHalfEdges.get(i);
            HalfEdgeVertex startVertex = removedHalfEdge.getStartVertex();
            List<HalfEdge> outingEdgesByMap = mapVertexOutingHEdges.get(startVertex);
            List<HalfEdge> outingEdgesByVertex = startVertex.getOutingHalfEdges(null);

            int hola = 0;
        }

        // now manage the removed halfEdges.************************************************************************
//        List<HalfEdge> removedHalfEdges = singleHalfEdges;
//        Map<HalfEdgeVertex, HalfEdgeVertex> mapVertexToNewVertex = new HashMap<>();
//        int removedHalfEdgesCount = removedHalfEdges.size();
//        for (int i = 0; i < removedHalfEdgesCount; i++)
//        {
//            HalfEdge removedHalfEdge = removedHalfEdges.get(i);
//            HalfEdgeVertex startVertex = removedHalfEdge.getStartVertex();
//            if(mapVertexToNewVertex.get(startVertex) == null)
//            {
//                HalfEdgeVertex newVertex = new HalfEdgeVertex();
//                newVertex.copyFrom(startVertex);
//                newVertex.setOutingHalfEdge(removedHalfEdge);
//                newVertex.note = "newVertex";
//                vertices.add(newVertex);
//                mapVertexToNewVertex.put(startVertex, newVertex);
//            }
//
//            HalfEdgeVertex newVertex = mapVertexToNewVertex.get(startVertex);
//            removedHalfEdge.setStartVertex(newVertex);
//            newVertex.setOutingHalfEdge(removedHalfEdge);
//            removedHalfEdge.note = "removedHalfEdge";
//
//            int hola = 0;
//        }
//
//        this.setTwinsBetweenHalfEdges(removedHalfEdges);

        int hola = 0;
    }

    public void setTwins_original() {
        Map<HalfEdgeVertex, List<HalfEdge>> mapVertexOutingHEdges = new HashMap<>();
        Map<HalfEdgeVertex, List<HalfEdge>> mapVertexIncomingHEdges = new HashMap<>();

        for (HalfEdge halfEdge : halfEdges) {
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            HalfEdgeVertex endVertex = halfEdge.getEndVertex();

            if (startVertex == endVertex) {
                int hola = 0;
            }
            List<HalfEdge> outingEdges = mapVertexOutingHEdges.computeIfAbsent(startVertex, k -> new ArrayList<>());
            outingEdges.add(halfEdge);

            List<HalfEdge> incomingEdges = mapVertexIncomingHEdges.computeIfAbsent(endVertex, k -> new ArrayList<>());
            incomingEdges.add(halfEdge);
        }

        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            List<HalfEdge> outingEdges = mapVertexOutingHEdges.get(vertex);
            List<HalfEdge> incomingEdges = mapVertexIncomingHEdges.get(vertex);

            if (outingEdges == null || incomingEdges == null) {
                continue;
            }

            int outingEdgesCount = outingEdges.size();
            int incomingEdgesCount = incomingEdges.size();
            for (int j = 0; j < outingEdgesCount; j++) {
                HalfEdge outingEdge = outingEdges.get(j);

                if (outingEdge.hasTwin()) {
                    continue;
                }
                for (int k = 0; k < incomingEdgesCount; k++) {
                    HalfEdge incomingEdge = incomingEdges.get(k);

                    if (incomingEdge.hasTwin()) {
                        continue;
                    }
                    if (outingEdge.setTwin(incomingEdge)) {
                        break;
                    }
                }
            }

        }

        int hola = 0;
    }

    public boolean TEST_addRandomPositionToVertices() {
        int vertexCount = vertices.size();
        double offset = 2.0;
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            PositionType positionType = vertex.getPositionType();
            if (positionType != PositionType.INTERIOR) {
                if (vertex.getPosition() != null) {
                    //Vector3d randomOffset = new Vector3d(Math.random() * offset, Math.random() * offset, Math.random() * offset);
                    Vector3d randomOffset = new Vector3d(0.0, 0.0, 40.0);
                    vertex.getPosition().add(randomOffset);
                }
            }

        }

        return true;
    }

    public void deleteObjects() {
        // delete halfEdges.***
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            halfEdge.breakRelations();
        }
        halfEdges.clear();

        // delete faces.***
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            face.breakRelations();
        }
        faces.clear();

        // delete vertices.***
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            vertex.deleteObjects();
        }
        vertices.clear();
    }


    public void doTrianglesReduction() {
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
        for (HalfEdge halfEdge : halfEdges) {
            double squaredLength = halfEdge.getSquaredLength();
            List<HalfEdge> halfEdges = mapHalfEdgesOrderedBySquaredLength.computeIfAbsent(squaredLength, k -> new ArrayList<>());
            halfEdges.add(halfEdge);
            averageSquaredLength += squaredLength;
        }
        averageSquaredLength /= halfEdges.size();
        double averageLength = Math.sqrt(averageSquaredLength);

        double minSquaredLength = averageSquaredLength * 10.0;
        List<List<HalfEdge>> orderedHalfEdgesList = new ArrayList<>(mapHalfEdgesOrderedBySquaredLength.values());
        List<HalfEdge> orderedHalfEdges = new ArrayList<>();

        int orderedHalfEdgesListCount = orderedHalfEdgesList.size();
        for (int i = 0; i < orderedHalfEdgesListCount; i++) {
            List<HalfEdge> halfEdges = orderedHalfEdgesList.get(i);
            orderedHalfEdges.addAll(halfEdges);
        }
        int halfEdgesCount = orderedHalfEdges.size();
        System.out.println("halfEdgesCount = " + halfEdgesCount);
        int counterAux = 0;
        int hedgesCollapsedCount = 0;

        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = orderedHalfEdges.get(i);
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (halfEdge.isDegenerated()) {
                int hola = 0;
            }

            if (!halfEdge.hasTwin()) {
                // this is frontier halfEdge.***
//                if(this.collapseFrontierHalfEdge(halfEdge, i, false))
//                {
//                    hedgesCollapsedCount+= 3;
//                    counterAux++;
//
//                    if(!this.checkVertices())
//                    {
//                        int hola = 0;
////                        setItselfAsOutingHalfEdgeToTheStartVertex();
////                        if(!this.checkVertices())
////                        {
////                            int hola2 = 0;
////                        }
//                    }
//                }

                continue;
            }

            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            PositionType positionType = startVertex.getPositionType();
            if (positionType != PositionType.INTERIOR) {
                continue;
            }

            if (startVertex.getPosition() == null) {
                int hola = 0;
            }


            if (halfEdge.getSquaredLength() < minSquaredLength) {
                boolean testDebug = false;
                if (halfEdge.isApplauseEdge()) {
                    continue;
                }
//                if(!this.checkFaces())
//                {
//                    int hola = 0;
//                }
//                if(!this.checkVertices()) {
//                    int hola = 0;
//                }
                if (collapseHalfEdge(halfEdge, i, testDebug)) {
                    hedgesCollapsedCount += 6;
                    counterAux++;

//                    if(!this.checkVertices()) {
//                        int hola = 0;
//                    }
//
//                    if(!this.checkFaces())
//                    {
//                        int hola = 0;
//                    }
                }
                if (counterAux >= 2000) {
                    counterAux = 0;
                    System.out.println("halfEdges deleted = " + hedgesCollapsedCount);
                }
            }
//            else {
//                break;  // the halfEdges are ordered by squaredLength.***
//            }


        }

        System.out.println("*** TOTAL HALFEDGES DELETED = " + hedgesCollapsedCount);

        // delete objects that status is DELETED.***
        this.removeDeletedObjects();

        if (!this.checkVertices()) {
            int hola = 0;
//            setItselfAsOutingHalfEdgeToTheStartVertex();
//            if(!this.checkVertices())
//            {
//                int hola2 = 0;
//            }
        }

        if (!this.checkFaces()) {
            int hola = 0;
        }

        // Finally check the halfEdges.***
        if (!this.TEST_check()) {
            int hola = 0;
            setItselfAsOutingHalfEdgeToTheStartVertex();
            if (!this.TEST_check()) {
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

    public void removeDeletedObjects() {
        // delete objects that status is DELETED.***
        // delete halfEdges that status is DELETED.***
        int halfEdgesCount = this.halfEdges.size();
        List<HalfEdge> copyHalfEdges = new ArrayList<>(this.halfEdges);
        this.halfEdges.clear();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = copyHalfEdges.get(i);
            if (halfEdge.getStatus() != ObjectStatus.DELETED) {
                this.halfEdges.add(halfEdge);
            } else {
                halfEdge.breakRelations();
            }
        }
        copyHalfEdges.clear();

        // delete vertices that status is DELETED.***
        int verticesCount = this.vertices.size();
        List<HalfEdgeVertex> copyVertices = new ArrayList<>(this.vertices);
        this.vertices.clear();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = copyVertices.get(i);
            if (vertex.getStatus() != ObjectStatus.DELETED) {
                this.vertices.add(vertex);
            } else {
                vertex.deleteObjects();
            }
        }
        copyVertices.clear();

        // delete faces that status is DELETED.***
        int facesCount = this.faces.size();
        List<HalfEdgeFace> copyFaces = new ArrayList<>(this.faces);
        this.faces.clear();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = copyFaces.get(i);
            if (face.getStatus() != ObjectStatus.DELETED) {
                this.faces.add(face);
            } else {
                face.breakRelations();
            }
        }
        copyFaces.clear();
    }

    public void setObjectIdsInList() {
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            halfEdge.setId(i);
        }

        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            vertex.setId(i);
        }

        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            face.setId(i);
        }
    }

    public void setItselfAsOutingHalfEdgeToTheStartVertex() {
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            halfEdge.setItselfAsOutingHalfEdgeToTheStartVertex();
        }

    }

    private Map<HalfEdgeVertex, List<HalfEdge>> checkVertexAllOutingEdgesMap(Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap) {
        Set<HalfEdgeVertex> vertexSet = vertexAllOutingEdgesMap.keySet();
        List<HalfEdge> outingHEdgesByVertex = new ArrayList<>();
        Map<HalfEdgeVertex, List<HalfEdge>> newVertexAllOutingEdgesMap2 = new HashMap<>();
        for (HalfEdgeVertex vertex : vertexSet) {
            List<HalfEdge> outingEdgesByMap = vertexAllOutingEdgesMap.get(vertex);
            if (outingEdgesByMap == null || outingEdgesByMap.isEmpty()) {
                //log.error("HalfEdgeSurface.makeVertexAllOutingEdgesMap() : outingEdges == null.");
                continue;
            }
            int outingEdgesByMapCount = outingEdgesByMap.size();
            outingHEdgesByVertex.clear();
            outingHEdgesByVertex = vertex.getOutingHalfEdges(outingHEdgesByVertex);
            int outingEdgesByVertexCount = outingHEdgesByVertex.size();

            if (outingEdgesByMapCount != outingEdgesByVertexCount) {
                // make a map of outingEdgesByVertex.***
                Map<HalfEdge, HalfEdge> outingEdgesByVertexMap = new HashMap<>();
                for (int i = 0; i < outingEdgesByVertexCount; i++) {
                    HalfEdge hedge = outingHEdgesByVertex.get(i);
                    if (hedge.getStatus() == ObjectStatus.DELETED) {
                        continue;
                    }
                    if (hedge.getStartVertex().getStatus() == ObjectStatus.DELETED) {
                        continue;
                    }
                    outingEdgesByVertexMap.put(hedge, hedge);
                }

                // separate in 2 hedgesList.***
                List<HalfEdge> outingEdgesByMap2 = new ArrayList<>();
                List<HalfEdge> outingEdgesByVertex2 = new ArrayList<>();
                for (int i = 0; i < outingEdgesByMapCount; i++) {
                    HalfEdge hedge = outingEdgesByMap.get(i);
                    // check if the hedge is in the outingEdgesByVertexMap.***
                    if (outingEdgesByVertexMap.get(hedge) == null) {
                        outingEdgesByMap2.add(hedge);
                    } else {
                        outingEdgesByVertex2.add(hedge);
                    }
                }

                // now, for outingEdgesByMap2, change the vertex for a new vertex.***
                HalfEdgeVertex newVertex = new HalfEdgeVertex();
                this.getVertices().add(newVertex);
                newVertex.copyFrom(vertex);
                for (int i = 0; i < outingEdgesByMap2.size(); i++) {
                    HalfEdge hedge = outingEdgesByMap2.get(i);
                    hedge.setStartVertex(newVertex);
                    newVertex.setOutingHalfEdge(hedge);
                }

                newVertexAllOutingEdgesMap2.put(newVertex, outingEdgesByMap2);

            }
        }

        return newVertexAllOutingEdgesMap2;
    }

    public Map<HalfEdgeVertex, List<HalfEdgeFace>> getMapVertexAllFaces(Map<HalfEdgeVertex, List<HalfEdgeFace>> resultVertexAllFacesMap) {
        if (resultVertexAllFacesMap == null) {
            resultVertexAllFacesMap = new HashMap<>();
        }

        for (HalfEdge halfEdge : halfEdges) {
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            if (startVertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeFace face = halfEdge.getFace();
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            List<HalfEdgeFace> faces = resultVertexAllFacesMap.computeIfAbsent(startVertex, k -> new ArrayList<>());
            faces.add(face);
        }

        return resultVertexAllFacesMap;
    }

    public Map<HalfEdgeVertex, List<HalfEdge>> getMapVertexAllOutingEdges(Map<HalfEdgeVertex, List<HalfEdge>> resultVertexAllOutingEdgesMap) {
        if (resultVertexAllOutingEdgesMap == null) {
            resultVertexAllOutingEdgesMap = new HashMap<>();
        }

        for (HalfEdge halfEdge : halfEdges) {
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            if (startVertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            List<HalfEdge> edges = resultVertexAllOutingEdgesMap.computeIfAbsent(startVertex, k -> new ArrayList<>());
            edges.add(halfEdge);
        }

        return resultVertexAllOutingEdgesMap;
    }

    public void checkSandClockFaces() {
        // This function returns a map of all halfEdges that startVertex is the key.***
        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap = this.getMapVertexAllOutingEdges(null);

        // Now, for each outingHEdgesList, check if they are connected.***
        int maxIterations = vertexAllOutingEdgesMap.size() * 10;
        int iteration = 0;
        boolean finished = false;
        while (!finished && iteration < maxIterations) {
            finished = true;
            vertexAllOutingEdgesMap = checkVertexAllOutingEdgesMap(vertexAllOutingEdgesMap);
            if (!vertexAllOutingEdgesMap.isEmpty()) {
                finished = false;
            } else {
                break;
            }
            iteration++;
        }

        int hola = 0;

    }

    public boolean checkVertices() {
        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            if (vertex.getOutingHalfEdge() == null) {
                System.out.println("HalfEdgeSurface.checkVertices() : vertex.getOutingHalfEdge() == null.");
                return false;
            }
            if (vertex.getOutingHalfEdge().getStatus() == ObjectStatus.DELETED) {
                System.out.println("HalfEdgeSurface.checkVertices() : vertex.getOutingHalfEdge() is deleted!.");
                return false;
            }

//            PositionType positionType = vertex.getPositionType();
//            if(positionType == PositionType.INTERIOR)
//            {
//                List<HalfEdge> outingEdges = vertex.getOutingHalfEdges(null);
//                if(outingEdges == null)
//                {
//                    System.out.println("HalfEdgeSurface.checkVertices() : outingEdges == null.");
//                    return false;
//                }
//
//                int outingEdgesCount = outingEdges.size();
//                if(outingEdgesCount < 3)
//                {
//                    HalfEdge hedge = outingEdges.get(0);
//                    HalfEdge twin = hedge.getTwin();
//                    HalfEdgeFace face = hedge.getFace();
//                    HalfEdgeFace twinFace = twin.getFace();
//                }
//            }
        }
        return true;
    }

    public boolean TEST_checkEqualHEdges() {
        Map<HalfEdgeVertex, Map<HalfEdgeVertex, List<HalfEdge>>> mapStartVertexToEndVertexToHedgesList = new HashMap<>();
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeVertex startVertex = hedge.getStartVertex();
            HalfEdgeVertex endVertex = hedge.getEndVertex();
            Map<HalfEdgeVertex, List<HalfEdge>> mapEndVertexToHedgesList = mapStartVertexToEndVertexToHedgesList.computeIfAbsent(startVertex, k -> new HashMap<>());
            List<HalfEdge> hedgesList = mapEndVertexToHedgesList.computeIfAbsent(endVertex, k -> new ArrayList<>());
            hedgesList.add(hedge);
        }

        Map<HalfEdgeVertex, List<HalfEdgeFace>> mapVertexAllFaces = this.getMapVertexAllFaces(null);

        // now, check if exist some list that has more than one halfEdge.***
        Set<HalfEdgeVertex> startVertexSet = mapStartVertexToEndVertexToHedgesList.keySet();
        for (HalfEdgeVertex startVertex : startVertexSet) {
            Map<HalfEdgeVertex, List<HalfEdge>> mapEndVertexToHedgesList = mapStartVertexToEndVertexToHedgesList.get(startVertex);
            Set<HalfEdgeVertex> endVertexSet = mapEndVertexToHedgesList.keySet();
            for (HalfEdgeVertex endVertex : endVertexSet) {
                List<HalfEdge> hedgesList = mapEndVertexToHedgesList.get(endVertex);
                if (hedgesList.size() > 1) {
                    System.out.println("HalfEdgeSurface.checkEqualHEdges() : hedgesList.size() > 1.");
                    int equalHEdgesCount = hedgesList.size();
                    List<HalfEdge> startVertexOutingHEdges = startVertex.getOutingHalfEdges(null);
                    int startVertexOutingHEdgesCount = startVertexOutingHEdges.size();
                    List<HalfEdge> endVertexOutingHEdges = endVertex.getOutingHalfEdges(null);
                    int endVertexOutingHEdgesCount = endVertexOutingHEdges.size();
                    List<HalfEdge> startVertexIncomingHEdges = startVertex.getIncomingHalfEdges(null);
                    int startVertexIncomingHEdgesCount = startVertexIncomingHEdges.size();
                    List<HalfEdge> endVertexIncomingHEdges = endVertex.getIncomingHalfEdges(null);
                    int endVertexIncomingHEdgesCount = endVertexIncomingHEdges.size();

                    List<HalfEdgeFace> startVertexFaces = startVertex.getFaces(null);
                    List<HalfEdgeFace> endVertexFaces = endVertex.getFaces(null);

                    List<HalfEdgeFace> startVertexFaces2 = mapVertexAllFaces.get(startVertex);
                    List<HalfEdgeFace> endVertexFaces2 = mapVertexAllFaces.get(endVertex);

                    int hola = 0;

                    return true;
                }
            }
        }
        return false;
    }

    public boolean checkFaces() {
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            if (face.getHalfEdge() == null) {
                System.out.println("HalfEdgeSurface.checkFaces() : face.getHalfEdge() == null.");
                return false;
            }
            if (face.getHalfEdge().getStatus() == ObjectStatus.DELETED) {
                System.out.println("HalfEdgeSurface.checkFaces() : face.getHalfEdge() is deleted!.");
                return false;
            }

            List<HalfEdge> halfEdgesLoop = new ArrayList<>();
            halfEdgesLoop = face.getHalfEdgesLoop(halfEdgesLoop);
            int halfEdgesLoopCount = halfEdgesLoop.size();
            for (int j = 0; j < halfEdgesLoopCount; j++) {
                HalfEdge halfEdge = halfEdgesLoop.get(j);
                if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                    System.out.println("HalfEdgeSurface.checkFaces() : halfEdge is deleted!.");
                    return false;
                }
                HalfEdgeVertex startVertex = halfEdge.getStartVertex();
                if (startVertex == null) {
                    System.out.println("HalfEdgeSurface.checkFaces() : startVertex == null.");
                    return false;
                }
                if (startVertex.getStatus() == ObjectStatus.DELETED) {
                    System.out.println("HalfEdgeSurface.checkFaces() : startVertex is deleted!.");
                    return false;
                }
            }
        }
        return true;
    }

    private boolean TEST_check() {
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            if (hedge.isDegenerated()) {
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

    private HalfEdgeCollapseData getHalfEdgeCollapsingData(HalfEdge halfEdge, HalfEdgeCollapseData resultHalfEdgeCollapseData) {
        if (resultHalfEdgeCollapseData == null) {
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
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedgeA = halfEdgesLoopA.get(i);
            if (hedgeA == halfEdgeA) {
                continue;
            }
            if (hedgeA.getStatus() == ObjectStatus.DELETED) {
                int hola = 0;
            }
            HalfEdge twin = hedgeA.getTwin();
            if (twin != null && twin.getStatus() != ObjectStatus.DELETED) {
                halfEdgesAExterior.add(twin);
            }
        }
        resultHalfEdgeCollapseData.setHalfEdgesAExterior(halfEdgesAExterior);
        resultHalfEdgeCollapseData.setFaceA(halfEdgeA.getFace());

        // HalfEdge B.*********************************************************************
        HalfEdge halfEdgeB = halfEdgeA.getTwin();
        if (halfEdgeB == null) {
            return resultHalfEdgeCollapseData;
        }

        resultHalfEdgeCollapseData.setHalfEdgeB(halfEdgeB);
        resultHalfEdgeCollapseData.setStartVertexB(halfEdgeB.getStartVertex());

        List<HalfEdge> halfEdgesLoopB = new ArrayList<>();
        halfEdgesLoopB = halfEdgeB.getLoop(halfEdgesLoopB);
        resultHalfEdgeCollapseData.setHalfEdgesLoopB(halfEdgesLoopB);

        List<HalfEdge> halfEdgesBExterior = new ArrayList<>();
        hedgesCount = halfEdgesLoopB.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedgeB = halfEdgesLoopB.get(i);
            if (hedgeB == halfEdgeB) {
                continue;
            }
            if (hedgeB.getStatus() == ObjectStatus.DELETED) {
                int hola = 0;
            }
            HalfEdge twin2 = hedgeB.getTwin();
            if (twin2 != null && twin2.getStatus() != ObjectStatus.DELETED) {
                halfEdgesBExterior.add(twin2);
            }
        }
        resultHalfEdgeCollapseData.setHalfEdgesBExterior(halfEdgesBExterior);
        resultHalfEdgeCollapseData.setFaceB(halfEdgeB.getFace());

        return resultHalfEdgeCollapseData;
    }

    public boolean collapseFrontierHalfEdge(HalfEdge halfEdge, int iteration, boolean testDebug) {
        // In this case, must find prevFrontierHalfEdge.***
        HalfEdgeVertex startVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();

        List<HalfEdge> incomingEdges = startVertex.getIncomingHalfEdges(null);
        if (incomingEdges == null) {
            int hola = 0;
        }
        HalfEdge prevFrontierHalfEdge = null;
        int incomingEdgesCount = incomingEdges.size();
        for (int i = 0; i < incomingEdgesCount; i++) {
            HalfEdge incomingEdge = incomingEdges.get(i);
            if (!incomingEdge.hasTwin()) {
                prevFrontierHalfEdge = incomingEdge;
                break;
            }
        }

        if (prevFrontierHalfEdge == null) {
            return false;
        }

        // Calculate the angle between the prevFrontierHalfEdge & the newPrevFrontierHalfEdge.***
        HalfEdgeVertex prevFrontierStartVertex = prevFrontierHalfEdge.getStartVertex();
        GaiaSegment segmentPrevHEdge = new GaiaSegment(prevFrontierStartVertex.getPosition(), startVertex.getPosition());
        if (!segmentPrevHEdge.check()) {
            int hola = 0;
        }
        GaiaSegment segmentNewPrevHEdge = new GaiaSegment(prevFrontierStartVertex.getPosition(), endVertex.getPosition());
        if (!segmentNewPrevHEdge.check()) {
            int hola = 0;
        }
        double angRad = segmentPrevHEdge.angRadToSegment(segmentNewPrevHEdge);
        double angDeg = Math.toDegrees(angRad);

        if (angDeg > 5.0) {
            return false;
        }

        HalfEdgeCollapseData halfEdgeCollapseData = getHalfEdgeCollapsingData(halfEdge, null);


        HalfEdgeVertex deletingVertex = halfEdgeCollapseData.getStartVertexA();
        List<HalfEdge> deletingHalfEdgesLoopA = halfEdgeCollapseData.getHalfEdgesLoopA();
        HalfEdgeFace deletingFace = halfEdge.getFace();
        List<HalfEdge> outingEdgesOfDeletingVertex = deletingVertex.getOutingHalfEdges(null);
        List<HalfEdge> outingEdgesOfEndVertex = halfEdge.getEndVertex().getOutingHalfEdges(null);

        if (outingEdgesOfEndVertex.size() == 1) {
            // we are deleting an isolated face.***
            return false;
        }

        for (HalfEdge outingEdge : outingEdgesOfDeletingVertex) {
            if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (outingEdge == halfEdge) {
                continue;
            }

            HalfEdgeVertex startVertexTest = outingEdge.getStartVertex();
            HalfEdgeVertex endVertexTest = outingEdge.getEndVertex();

            if (startVertexTest == startVertex && endVertexTest == endVertex) {
                int hola = 0;
            }
        }


        if (deletingVertex == endVertex) {
            int hola = 0;
        }

        if (endVertex.getStatus() == ObjectStatus.DELETED) {
            int hola = 0;
        }

        // Here there are no twin data.***
        //*********************************************************************************
        // 1- Delete the 2 faces, the 2 halfEdges, the 2 halfEdgesLoop, the startVertex.***
        //*********************************************************************************
        // delete the 1 faces.***
        deletingFace.setStatus(ObjectStatus.DELETED);

        // Delete the 1 halfEdgesLoop.***
        List<HalfEdgeVertex> vertexThatMustChangeOutingHalfEdge = new ArrayList<>();

        // Side A.**************************************************************************
        int counterAux = 0;
        for (HalfEdge deletingHalfEdgeA : deletingHalfEdgesLoopA) {
            deletingHalfEdgeA.setStatus(ObjectStatus.DELETED);
            HalfEdgeVertex startVertexA = deletingHalfEdgeA.getStartVertex();
            if (startVertexA != null)// && startVertex.getOutingHalfEdge() == deletingHalfEdgeA)
            {
                vertexThatMustChangeOutingHalfEdge.add(startVertexA);
                startVertexA.note = "mustChange-outingHalfEdge_DIRECT" + counterAux + "_ITER: " + iteration;
                startVertexA.setOutingHalfEdge(null);
            }

            deletingHalfEdgeA.note = "deleted-in-collapseHalfEdge_DIRECT" + counterAux + "_ITER: " + iteration;
            deletingHalfEdgeA.breakRelations();
            counterAux++;
        }


        if (endVertex.getStatus() == ObjectStatus.DELETED) {
            int hola = 0;
        }

        // delete the startVertex.***
        deletingVertex.setStatus(ObjectStatus.DELETED);
        deletingVertex.deleteObjects();
        deletingVertex.note = "deleted-in-collapseHalfEdge" + "_ITER: " + iteration;


        //**************************************************************************************
        // 2- Set the endVertex to halfEdges that lost the startVertex.***
        //**************************************************************************************

        if (outingEdgesOfDeletingVertex == null) {
            System.out.println("HalfEdgeSurface.collapseHalfEdge() : outingEdgesOfDeletingVertex == null.");
            return false;
        }

        for (HalfEdge outingEdge : outingEdgesOfDeletingVertex) {
            if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeVertex startVertexTest = outingEdge.getStartVertex();
            HalfEdgeVertex endVertexTest = outingEdge.getEndVertex();
            Vector3d startPosition = startVertexTest.getPosition();
            Vector3d endPosition = endVertexTest.getPosition();

            if (endVertex.getStatus() == ObjectStatus.DELETED) {
                int hola = 0;
            }

            if (outingEdge.isDegenerated()) {
                int hola = 0;
            }
            HalfEdgeVertex currEndVertex = outingEdge.getEndVertex();
            if (currEndVertex == endVertex) {
                int hola = 0;
            }
            outingEdge.setStartVertex(endVertex);
            if (outingEdge.isDegenerated()) {
                int hola = 0;
            }
            outingEdge.note = "Reasigned StartVertex-in-collapseHalfEdge" + "_ITER: " + iteration;
            endVertex.setOutingHalfEdge(outingEdge);
        }

        // for all outingHedges, take the HEdgeLoop and set HEdge.setItselfAsOutingEdge for his-startVertex.***
        for (HalfEdge outingEdge : outingEdgesOfDeletingVertex) {
            if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            List<HalfEdge> outingEdgesLoop = new ArrayList<>();
            outingEdgesLoop = outingEdge.getLoop(outingEdgesLoop);
            int outingEdgesLoopCount = outingEdgesLoop.size();
            for (int i = 0; i < outingEdgesLoopCount; i++) {
                HalfEdge outingEdgeLoop = outingEdgesLoop.get(i);
                if (outingEdgeLoop.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }
                outingEdgeLoop.setItselfAsOutingHalfEdgeToTheStartVertex();
            }
        }

        // for all outingEdges of the endVertex, take the HEdgeLoop and set HEdge.setItselfAsOutingEdge for his-startVertex.***
        for (HalfEdge outingEdge : outingEdgesOfEndVertex) {
            if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            List<HalfEdge> outingEdgesLoop = new ArrayList<>();
            outingEdgesLoop = outingEdge.getLoop(outingEdgesLoop);
            int outingEdgesLoopCount = outingEdgesLoop.size();
            for (int i = 0; i < outingEdgesLoopCount; i++) {
                HalfEdge outingEdgeLoop = outingEdgesLoop.get(i);
                if (outingEdgeLoop.getStatus() == ObjectStatus.DELETED) {
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
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdgeAExterior = halfEdgesAExterior.get(i);
            HalfEdgeVertex startVertexA = halfEdgeAExterior.getStartVertex();
            if (startVertexA == null) {
                int hola = 0;
            }
            startVertexA.setOutingHalfEdge(halfEdgeAExterior);
        }

        //**************************************************************************************
        // 3- Set twins between the halfEdges stored in keepFutureTwineablesHalfEdges.***
        //**************************************************************************************
        setTwinsBetweenHalfEdges(halfEdgeCollapseData.getHalfEdgesAExterior());


        return true;
    }

    public boolean collapseHalfEdge(HalfEdge halfEdge, int iteration, boolean testDebug) {
        // When collapse a halfEdge, we delete the face, the twin's face, the twin & the startVertex.***
        // When deleting a face, must delete all halfEdges of the face.***
        // must find all halfEdges that startVertex is the deletingVertex, and set as startVertex the endVertex of the deletingHalfEdge.***
        HalfEdgeCollapseData halfEdgeCollapseData = getHalfEdgeCollapsingData(halfEdge, null);

        //this.checkVertices();

        if (!halfEdgeCollapseData.check()) {
            int hola = 0;
            return false;
        }
        HalfEdgeVertex deletingVertex = halfEdgeCollapseData.getStartVertexA();
        List<HalfEdge> deletingHalfEdgesLoopA = halfEdgeCollapseData.getHalfEdgesLoopA();


        // twin data.***
        HalfEdge twin = halfEdgeCollapseData.getHalfEdgeB();
        List<HalfEdge> deletingTwinHalfEdgesLoopB = halfEdgeCollapseData.getHalfEdgesLoopB();

        List<HalfEdge> outingEdgesOfDeletingVertex = deletingVertex.getOutingHalfEdges(null);
        List<HalfEdge> outingEdgesOfEndVertex = halfEdge.getEndVertex().getOutingHalfEdges(null);

        // check if outingHedge.endVertex == endVertex.***
        int outingEdgesOfDeletingVertexCount = outingEdgesOfDeletingVertex.size();
        for (int i = 0; i < outingEdgesOfDeletingVertexCount; i++) {
            HalfEdge outingEdge = outingEdgesOfDeletingVertex.get(i);
            if (outingEdge != halfEdge) {
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

        if (deletingVertex == endVertex) {
            int hola = 0;
        }

        if (endVertex.getStatus() == ObjectStatus.DELETED) {
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
        for (HalfEdge deletingHalfEdgeA : deletingHalfEdgesLoopA) {
            deletingHalfEdgeA.setStatus(ObjectStatus.DELETED);
            HalfEdgeVertex startVertex = deletingHalfEdgeA.getStartVertex();
            if (startVertex != null)// && startVertex.getOutingHalfEdge() == deletingHalfEdgeA)
            {
                vertexThatMustChangeOutingHalfEdge.add(startVertex);
                startVertex.note = "mustChange-outingHalfEdge_DIRECT" + counterAux + "_ITER: " + iteration;
                startVertex.setOutingHalfEdge(null);
            }

            deletingHalfEdgeA.note = "deleted-in-collapseHalfEdge_DIRECT" + counterAux + "_ITER: " + iteration;
            deletingHalfEdgeA.breakRelations();
            counterAux++;
        }

        if (endVertex.getStatus() == ObjectStatus.DELETED) {
            int hola = 0;
        }

        // Side B.***************************************************************************
        counterAux = 0;
        for (HalfEdge deletingTwinHalfEdgeB : deletingTwinHalfEdgesLoopB) {
            deletingTwinHalfEdgeB.setStatus(ObjectStatus.DELETED);
            HalfEdgeVertex startVertex = deletingTwinHalfEdgeB.getStartVertex();
            if (startVertex != null)// && startVertex.getOutingHalfEdge() == deletingTwinHalfEdgeB)
            {
                vertexThatMustChangeOutingHalfEdge.add(startVertex);
                startVertex.note = "mustChange-outingHalfEdge_TWIN" + counterAux + "_ITER: " + iteration;
                startVertex.setOutingHalfEdge(null);
            }

            deletingTwinHalfEdgeB.note = "deleted-in-collapseHalfEdge_TWIN" + counterAux + "_ITER: " + iteration;
            deletingTwinHalfEdgeB.breakRelations();
        }

        // delete the startVertex.***
        deletingVertex.setStatus(ObjectStatus.DELETED);
        deletingVertex.deleteObjects();
        deletingVertex.note = "deleted-in-collapseHalfEdge" + "_ITER: " + iteration;


        //**************************************************************************************
        // 2- Set the endVertex to halfEdges that lost the startVertex.***
        //**************************************************************************************

        if (outingEdgesOfDeletingVertex == null) {
            System.out.println("HalfEdgeSurface.collapseHalfEdge() : outingEdgesOfDeletingVertex == null.");
            return false;
        }

        for (HalfEdge outingEdge : outingEdgesOfDeletingVertex) {
            if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeVertex startVertexTest = outingEdge.getStartVertex();
            HalfEdgeVertex endVertexTest = outingEdge.getEndVertex();
            Vector3d startPosition = startVertexTest.getPosition();
            Vector3d endPosition = endVertexTest.getPosition();

            if (endVertex.getStatus() == ObjectStatus.DELETED) {
                int hola = 0;
            }

            if (outingEdge.isDegenerated()) {
                int hola = 0;
            }
            HalfEdgeVertex currEndVertex = outingEdge.getEndVertex();
            if (currEndVertex == endVertex) {
                int hola = 0;
            }
            outingEdge.setStartVertex(endVertex);
            if (outingEdge.isDegenerated()) {
                int hola = 0;
            }
            outingEdge.note = "Reasigned StartVertex-in-collapseHalfEdge" + "_ITER: " + iteration;
            endVertex.setOutingHalfEdge(outingEdge);
        }

        // for all outingHedges, take the HEdgeLoop and set HEdge.setItselfAsOutingEdge for his-startVertex.***
        for (HalfEdge outingEdge : outingEdgesOfDeletingVertex) {
            if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            List<HalfEdge> outingEdgesLoop = new ArrayList<>();
            outingEdgesLoop = outingEdge.getLoop(outingEdgesLoop);
            int outingEdgesLoopCount = outingEdgesLoop.size();
            for (int i = 0; i < outingEdgesLoopCount; i++) {
                HalfEdge outingEdgeLoop = outingEdgesLoop.get(i);
                if (outingEdgeLoop.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }
                outingEdgeLoop.setItselfAsOutingHalfEdgeToTheStartVertex();
            }
        }

        // for all outingEdges of the endVertex, take the HEdgeLoop and set HEdge.setItselfAsOutingEdge for his-startVertex.***
        for (HalfEdge outingEdge : outingEdgesOfEndVertex) {
            if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            List<HalfEdge> outingEdgesLoop = new ArrayList<>();
            outingEdgesLoop = outingEdge.getLoop(outingEdgesLoop);
            int outingEdgesLoopCount = outingEdgesLoop.size();
            for (int i = 0; i < outingEdgesLoopCount; i++) {
                HalfEdge outingEdgeLoop = outingEdgesLoop.get(i);
                if (outingEdgeLoop.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }
                outingEdgeLoop.setItselfAsOutingHalfEdgeToTheStartVertex();
            }
        }

        List<HalfEdge> halfEdgesAExterior = halfEdgeCollapseData.getHalfEdgesAExterior();
        List<HalfEdge> halfEdgesBExterior = halfEdgeCollapseData.getHalfEdgesBExterior();

        int halfEdgesCount = halfEdgesAExterior.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdgeAExterior = halfEdgesAExterior.get(i);
            HalfEdgeVertex startVertex = halfEdgeAExterior.getStartVertex();
            if (startVertex == null) {
                int hola = 0;
            }
            startVertex.setOutingHalfEdge(halfEdgeAExterior);
        }


        halfEdgesCount = halfEdgesBExterior.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdgeBExterior = halfEdgesBExterior.get(i);
            HalfEdgeVertex startVertex = halfEdgeBExterior.getStartVertex();
            if (startVertex == null) {
                int hola = 0;
            }
            startVertex.setOutingHalfEdge(halfEdgeBExterior);
        }

        //**************************************************************************************
        // 3- Set twins between the halfEdges stored in keepFutureTwineablesHalfEdges.***
        //**************************************************************************************
        setTwinsBetweenHalfEdges(halfEdgeCollapseData.getHalfEdgesAExterior());
        setTwinsBetweenHalfEdges(halfEdgeCollapseData.getHalfEdgesBExterior());

        return true;
    }


    public void setTwinsBetweenHalfEdges(List<HalfEdge> halfEdges) {
        // This function sets the twins between the halfEdges
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            if (halfEdge == null) {
                int hola = 0;
            }
            if (halfEdge.getStatus() == ObjectStatus.DELETED || halfEdge.hasTwin()) {
                continue;
            }

            for (int j = i + 1; j < halfEdgesCount; j++) {
                HalfEdge halfEdge2 = halfEdges.get(j);
                if (halfEdge2.getStatus() == ObjectStatus.DELETED || halfEdge2.hasTwin()) {
                    continue;
                }

                if (halfEdge.setTwin(halfEdge2)) {
                    break;
                }
            }
        }
    }

    public void transformPoints(Matrix4d finalMatrix) {
        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector3d position = vertex.getPosition();
            if (position != null) {
                Vector3d transformedPosition = new Vector3d();
                finalMatrix.transformPosition(position, transformedPosition);
                vertex.setPosition(transformedPosition);
            }
        }
    }

    public GaiaBoundingBox calculateBoundingBox(GaiaBoundingBox resultBBox) {
        if (resultBBox == null) {
            resultBBox = new GaiaBoundingBox();
        }
        resultBBox.setInit(true);
        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector3d position = vertex.getPosition();
            if (position != null) {
                resultBBox.addPoint(position);
            }
        }
        return resultBBox;
    }

    public GaiaBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            boundingBox = calculateBoundingBox(null);
        }
        return boundingBox;
    }

    public void cutByPlane(PlaneType planeType, Vector3d planePosition, double error) {
        if (planeType == PlaneType.XY) {
            //cutByPlaneXY(planePosition);
        } else if (planeType == PlaneType.XZ) {
            cutByPlaneXZ(planePosition, error);
        } else if (planeType == PlaneType.YZ) {
            cutByPlaneYZ(planePosition, error);
        }

        removeDeletedObjects();
    }

    private void cutByPlaneXZ(Vector3d planePosition, double error) {
        // find halfEdges that are cut by the plane.***

        int hedgesCount = halfEdges.size();
        int hedgesCutCount = 0;
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeVertex intersectionVertex = new HalfEdgeVertex();
            if (hedge.getIntersectionByPlane(PlaneType.XZ, planePosition, intersectionVertex, error)) {
                splitHalfEdge(hedge, intersectionVertex);
                hedgesCount = halfEdges.size();

                hedgesCutCount++;
                if(hedgesCutCount%10 == 0)
                {
                    log.info("hedgesCount = " + hedgesCount + " , hedgesCutCount = " + hedgesCutCount + " , currIdx = " + i);
                }
            }
        }

    }

    private void cutByPlaneYZ(Vector3d planePosition, double error) {
        // find halfEdges that are cut by the plane.***

        int hedgesCount = halfEdges.size();
        int hedgesCutCount = 0;
        int cutCount = 0;
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeVertex intersectionVertex = new HalfEdgeVertex();
            if (hedge.getIntersectionByPlane(PlaneType.YZ, planePosition, intersectionVertex, error)) {
                splitHalfEdge(hedge, intersectionVertex);
                hedgesCount = halfEdges.size();
                hedgesCutCount++;
                if(hedgesCutCount%10 == 0)
                {
                    log.info("hedgesCount = " + hedgesCount + " , hedgesCutCount = " + hedgesCutCount + " , currIdx = " + i);
                }
            }
        }
    }

    public boolean checkHalfEdgesFaces() {
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            if (hedge.getFace() == null) {
                System.out.println("HalfEdgeSurface.checkHalfEdgesFaces() : hedge.getFace() == null.");
                return false;
            }
        }

        return true;
    }

    public boolean checkTwins() {
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdge twin = hedge.getTwin();
//            if(twin != null && twin.getStatus() != ObjectStatus.DELETED)
//            {
//                HalfEdgeVertex startVertex = hedge.getStartVertex();
//                HalfEdgeVertex endVertex = hedge.getEndVertex();
//
//                HalfEdgeVertex twinStartVertex = twin.getStartVertex();
//                HalfEdgeVertex twinEndVertex = twin.getEndVertex();
//
//                if(startVertex != twinEndVertex || endVertex != twinStartVertex) {
//                    System.out.println("HalfEdgeSurface.checkTwins() : startVertex != twinEndVertex || endVertex != twinStartVertex.");
//                    return false;
//                }
//            }

            if (twin != null && twin.getStatus() != ObjectStatus.DELETED && twin.getTwin() != hedge) {
                System.out.println("HalfEdgeSurface.checkTwins() : twin.getTwin() != hedge.");
                return false;
            }
        }

        return true;
    }

    public boolean TEST_checkHalfEdgeLength()
    {
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++)
        {
            HalfEdge hedge = halfEdges.get(i);
            if(hedge.getStatus() == ObjectStatus.DELETED)
            {
                continue;
            }
            double length = hedge.getLength();
            if(length >10.0)
            {
                return false;
            }
        }
        return true;
    }

    public boolean TEST_checkIntersectionVertex()
    {
        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getNote() != null && vertex.getNote().equals("intersectionVertex")) {
                double z = vertex.getPosition().z;
                if(z > 1000.0) {
                    int hola = 0;
                }
                else {
                    int hola = 0;
                }
            }
        }

        return true;
    }

    private void splitHalfEdge(HalfEdge halfEdge, HalfEdgeVertex intersectionVertex) {
        // When split a halfEdge, must split the face too.***
        // If exist twin, must split the twin and twin's face too.***
        //TEST_checkHalfEdgeLength();
        HalfEdge twin = halfEdge.getTwin();

        HalfEdgeVertex startVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();


        if (twin != null && twin.getStatus() != ObjectStatus.DELETED) {

            intersectionVertex.setNote("intersectionVertex");

            //intersectionVertex.getPosition().add(0.0, 0.0, 10.0); // test.***
            this.getVertices().add(intersectionVertex);

            if (twin != null && twin.getStatus() != ObjectStatus.DELETED) {
                HalfEdgeFace twinsFace = twin.getFace();
                if (twinsFace == null) {
                    int hola = 0;
                }
                int hola = 0;
            }

            // must split the twin too.***
            HalfEdgeFace faceA = halfEdge.getFace();
            HalfEdgeFace faceB = twin.getFace();

            if (faceA.getStatus() == ObjectStatus.DELETED || faceB.getStatus() == ObjectStatus.DELETED) {
                int hola = 0;
            }

            faceA.setStatus(ObjectStatus.DELETED);
            faceA.setNote("faceA_deleted");
            faceB.setStatus(ObjectStatus.DELETED);
            faceB.setNote("faceB_deleted");

            List<HalfEdge> halfEdgesLoopA = new ArrayList<>();
            halfEdgesLoopA = halfEdge.getLoop(halfEdgesLoopA);

            List<HalfEdge> halfEdgesLoopB = new ArrayList<>();
            halfEdgesLoopB = twin.getLoop(halfEdgesLoopB);

            int hedgesACount = halfEdgesLoopA.size();
            int hedgesBCount = halfEdgesLoopB.size();


            // Initial situation.***************************************************************************************
            //                                               oppositeVertexA
            //                                                    / \
            //                                                 /       \
            //                                              /             \
            //                                           /                   \
            //                 exteriorHEdgeA2        /                         \   exteriorHEdgeA1
            //                                     /                               \
            //                                  /             faceA                   \
            //                               /                                           \
            //                            /                                                 \
            //                         /                    halfEdge--->                       \
            //             startV   *-------------------------------------------------------------*  endV
            //                         \                    <---twin                           /
            //                            \                                                 /
            //                               \                                           /
            //                                  \            faceB                    /
            //                                     \                               /
            //                  exteriorHEdgeB1       \                         /   exteriorHEdgeB2
            //                                           \                   /
            //                                              \             /
            //                                                 \       /
            //                                                    \ /
            //                                               oppositeVertexB


            // Final situation.*****************************************************************************************
            //                                               oppositeVertexA
            //                                                    /|\
            //                                                 /   |   \
            //                                              /      |      \
            //                                           /         |         \
            //                     exteriorHEdgeA2    /            |            \   exteriorHEdgeA1
            //                                     /               |               \
            //                                  /          faceA   |    faceC         \
            //                               /                     |                     \
            //                            /                        |                        \
            //                         /        halfEdge--->       |     newHalfEdgeC1--->     \
            //             startV   *------------------------------*------------------------------*  endV  (in the center there are intersectionVertex)
            //                         \      <--->twin            |   <--->newHalfEdgeD1      /
            //                            \                        |                        /
            //                               \                     |                     /
            //                                  \          faceB   |    faceD         /
            //                                     \               |               /
            //                                        \            |            /
            //                      exteriorHEdgeB1      \         |         /   exteriorHEdgeB2
            //                                              \      |      /
            //                                                 \   |   /
            //                                                    \|/
            //                                               oppositeVertexB

            // Find oppositeVertexA and oppositeVertexB.***
            HalfEdgeVertex oppositeVertexA = halfEdge.getPrev().getStartVertex();
            HalfEdgeVertex oppositeVertexB = twin.getPrev().getStartVertex();

            HalfEdge exteriorHEdgeA1 = halfEdge.getNext().getTwin();
            HalfEdge exteriorHEdgeA2 = halfEdge.getPrev().getTwin();
            HalfEdge exteriorHEdgeB1 = twin.getNext().getTwin();
            // test.***
            if (exteriorHEdgeB1 != null) {
                HalfEdgeVertex extB1StartVertex = exteriorHEdgeB1.getStartVertex();
                HalfEdgeVertex extB1EndVertex = exteriorHEdgeB1.getEndVertex();
                if (extB1StartVertex != oppositeVertexB || extB1EndVertex != startVertex) {
                    int hola = 0;
                }
                // end test.---
            }
            HalfEdge exteriorHEdgeB2 = twin.getPrev().getTwin();

            // Face A.********************************
            // In this face use the halfEdge.***
            HalfEdgeFace newFaceA = new HalfEdgeFace();
            newFaceA.setNote("newFaceA");
            HalfEdge newHalfEdgeA1 = new HalfEdge();
            HalfEdge newHalfEdgeA2 = new HalfEdge();
            HalfEdge newHalfEdgeA3 = new HalfEdge();
            this.getHalfEdges().add(newHalfEdgeA1);
            this.getHalfEdges().add(newHalfEdgeA2);
            this.getHalfEdges().add(newHalfEdgeA3);
            this.getFaces().add(newFaceA);

            newHalfEdgeA1.setNext(newHalfEdgeA2);
            newHalfEdgeA2.setNext(newHalfEdgeA3);
            newHalfEdgeA3.setNext(newHalfEdgeA1);

            newHalfEdgeA1.setFace(newFaceA);
            newHalfEdgeA2.setFace(newFaceA);
            newHalfEdgeA3.setFace(newFaceA);

            newHalfEdgeA1.setStartVertex(startVertex);
            newHalfEdgeA2.setStartVertex(intersectionVertex);
            newHalfEdgeA3.setStartVertex(oppositeVertexA);

            newFaceA.setHalfEdge(newHalfEdgeA1);
            intersectionVertex.setOutingHalfEdge(newHalfEdgeA2);
            oppositeVertexA.setOutingHalfEdge(newHalfEdgeA3);


            // Face B.********************************
            // In this face use the twin.***
            HalfEdgeFace newFaceB = new HalfEdgeFace();
            newFaceB.setNote("newFaceB");
            HalfEdge newHalfEdgeB1 = new HalfEdge();
            HalfEdge newHalfEdgeB2 = new HalfEdge();
            HalfEdge newHalfEdgeB3 = new HalfEdge();
            this.getHalfEdges().add(newHalfEdgeB1);
            this.getHalfEdges().add(newHalfEdgeB2);
            this.getHalfEdges().add(newHalfEdgeB3);
            this.getFaces().add(newFaceB);

            newHalfEdgeB1.setNext(newHalfEdgeB2);
            newHalfEdgeB2.setNext(newHalfEdgeB3);
            newHalfEdgeB3.setNext(newHalfEdgeB1);

            newHalfEdgeB1.setFace(newFaceB);
            newHalfEdgeB2.setFace(newFaceB);
            newHalfEdgeB3.setFace(newFaceB);

            newHalfEdgeB1.setStartVertex(intersectionVertex);
            newHalfEdgeB2.setStartVertex(startVertex);
            newHalfEdgeB3.setStartVertex(oppositeVertexB);

            newFaceB.setHalfEdge(newHalfEdgeB1);
            intersectionVertex.setOutingHalfEdge(newHalfEdgeB1);
            oppositeVertexB.setOutingHalfEdge(newHalfEdgeB3);


            // Face C.********************************
            // In this face use the newHalfEdgeC.***
            HalfEdgeFace newFaceC = new HalfEdgeFace();
            newFaceC.setNote("newFaceC");
            HalfEdge newHalfEdgeC1 = new HalfEdge();
            HalfEdge newHalfEdgeC2 = new HalfEdge();
            HalfEdge newHalfEdgeC3 = new HalfEdge();
            this.getHalfEdges().add(newHalfEdgeC1);
            this.getHalfEdges().add(newHalfEdgeC2);
            this.getHalfEdges().add(newHalfEdgeC3);
            this.getFaces().add(newFaceC);

            newHalfEdgeC1.setNext(newHalfEdgeC2);
            newHalfEdgeC2.setNext(newHalfEdgeC3);
            newHalfEdgeC3.setNext(newHalfEdgeC1);

            newHalfEdgeC1.setFace(newFaceC);
            newHalfEdgeC2.setFace(newFaceC);
            newHalfEdgeC3.setFace(newFaceC);

            newHalfEdgeC1.setStartVertex(intersectionVertex);
            newHalfEdgeC2.setStartVertex(endVertex);
            newHalfEdgeC3.setStartVertex(oppositeVertexA);

            newFaceC.setHalfEdge(newHalfEdgeC1);
            intersectionVertex.setOutingHalfEdge(newHalfEdgeC1);
            oppositeVertexA.setOutingHalfEdge(newHalfEdgeC3);


            // Face D.********************************
            // In this face use the newHalfEdgeD.***
            HalfEdgeFace newFaceD = new HalfEdgeFace();
            newFaceD.setNote("newFaceD");
            HalfEdge newHalfEdgeD1 = new HalfEdge();
            HalfEdge newHalfEdgeD2 = new HalfEdge();
            HalfEdge newHalfEdgeD3 = new HalfEdge();
            this.getHalfEdges().add(newHalfEdgeD1);
            this.getHalfEdges().add(newHalfEdgeD2);
            this.getHalfEdges().add(newHalfEdgeD3);
            this.getFaces().add(newFaceD);

            newHalfEdgeD1.setNext(newHalfEdgeD2);
            newHalfEdgeD2.setNext(newHalfEdgeD3);
            newHalfEdgeD3.setNext(newHalfEdgeD1);

            newHalfEdgeD1.setFace(newFaceD);
            newHalfEdgeD2.setFace(newFaceD);
            newHalfEdgeD3.setFace(newFaceD);

            newHalfEdgeD1.setStartVertex(endVertex);
            newHalfEdgeD2.setStartVertex(intersectionVertex);
            newHalfEdgeD3.setStartVertex(oppositeVertexB);

            newFaceD.setHalfEdge(newHalfEdgeD1);
            intersectionVertex.setOutingHalfEdge(newHalfEdgeD2);
            oppositeVertexB.setOutingHalfEdge(newHalfEdgeD3);

            // Now set twins.***
            if (!newHalfEdgeA1.setTwin(newHalfEdgeB1)) {
                int hola = 0;
            }

            if (!newHalfEdgeA2.setTwin(newHalfEdgeC3)) {
                int hola = 0;
            }


            if (exteriorHEdgeA2 != null) {
                HalfEdge currTwinOfExteriorA2 = exteriorHEdgeA2.getTwin();
                if (!newHalfEdgeA3.setTwin(exteriorHEdgeA2)) {
                    int hola = 0;
                }

                if (currTwinOfExteriorA2 != null) {
                    currTwinOfExteriorA2.setTwin(null);
                }
            }


            if (exteriorHEdgeB1 != null) {
                HalfEdge currTwinOfExteriorB1 = exteriorHEdgeB1.getTwin();
                if (!newHalfEdgeB2.setTwin(exteriorHEdgeB1)) {
                    int hola = 0;
                }

                if (currTwinOfExteriorB1 != null) {
                    currTwinOfExteriorB1.setTwin(null);
                }
            }


            if (!newHalfEdgeB3.setTwin(newHalfEdgeD2)) {
                int hola = 0;
            }

            if (!newHalfEdgeC1.setTwin(newHalfEdgeD1)) {
                int hola = 0;
            }

            if (exteriorHEdgeA1 != null) {
                HalfEdge currTwinOfExteriorA1 = exteriorHEdgeA1.getTwin();
                if (!newHalfEdgeC2.setTwin(exteriorHEdgeA1)) {
                    int hola = 0;
                }

                if (currTwinOfExteriorA1 != null) {
                    currTwinOfExteriorA1.setTwin(null);
                }
            }

            if (exteriorHEdgeB2 != null) {
                HalfEdge currTwinOfExteriorB2 = exteriorHEdgeB2.getTwin();
                if (!newHalfEdgeD3.setTwin(exteriorHEdgeB2)) {
                    int hola = 0;
                }

                if (currTwinOfExteriorB2 != null) {
                    currTwinOfExteriorB2.setTwin(null);
                }
            }


            // finally break the relations of the halfEdgesLoopA.***
            for (int i = 0; i < hedgesACount; i++) {
                HalfEdge hedgeA = halfEdgesLoopA.get(i);
                //if (hedgeA != halfEdge)
                {
                    hedgeA.setStatus(ObjectStatus.DELETED);
                    hedgeA.breakRelations();

                }
            }


            // finally break the relations of the halfEdgesLoopB.***
            for (int i = 0; i < hedgesBCount; i++) {
                HalfEdge hedgeB = halfEdgesLoopB.get(i);
                //if (hedgeB != twin)
                {
                    hedgeB.setStatus(ObjectStatus.DELETED);
                    hedgeB.breakRelations();
                }
            }

        } else {
            intersectionVertex.setNote("intersectionVertex");
            //intersectionVertex.getPosition().add(0.0, 0.0, 10.0); // test.***
            this.getVertices().add(intersectionVertex);

            HalfEdgeFace faceA = halfEdge.getFace();

            faceA.setStatus(ObjectStatus.DELETED);

            List<HalfEdge> halfEdgesLoopA = new ArrayList<>();
            halfEdgesLoopA = halfEdge.getLoop(halfEdgesLoopA);

            int hedgesACount = halfEdgesLoopA.size();


            // Initial situation.***************************************************************************************
            //                                               oppositeVertexA
            //                                                    / \
            //                                                 /       \
            //                                              /             \
            //                                           /                   \
            //                 exteriorHEdgeA2        /                         \   exteriorHEdgeA1
            //                                     /                               \
            //                                  /             faceA                   \
            //                               /                                           \
            //                            /                                                 \
            //                         /                    halfEdge--->                       \
            //             startV   *-------------------------------------------------------------*  endV


            // Final situation.*****************************************************************************************
            //                                               oppositeVertexA
            //                                                    /|\
            //                                                 /   |   \
            //                                              /      |      \
            //                                           /         |         \
            //                     exteriorHEdgeA2    /            |            \   exteriorHEdgeA1
            //                                     /               |               \
            //                                  /          faceA   |    faceC         \
            //                               /                     |                     \
            //                            /                        |                        \
            //                         /        halfEdge--->       |     newHalfEdgeC--->      \
            //             startV   *------------------------------*------------------------------*  endV  (in the center there are intersectionVertex)


            // Find oppositeVertexA and oppositeVertexB.***
            HalfEdgeVertex oppositeVertexA = halfEdge.getPrev().getStartVertex();

            HalfEdge exteriorHEdgeA1 = halfEdge.getNext().getTwin();
            HalfEdge exteriorHEdgeA2 = halfEdge.getPrev().getTwin();

            // Face A.********************************
            // In this face use the halfEdge.***
            HalfEdgeFace newFaceA = new HalfEdgeFace();
            HalfEdge newHalfEdgeA1 = new HalfEdge();
            HalfEdge newHalfEdgeA2 = new HalfEdge();
            HalfEdge newHalfEdgeA3 = new HalfEdge();
            this.getHalfEdges().add(newHalfEdgeA1);
            this.getHalfEdges().add(newHalfEdgeA2);
            this.getHalfEdges().add(newHalfEdgeA3);
            this.getFaces().add(newFaceA);

            newHalfEdgeA1.setNext(newHalfEdgeA2);
            newHalfEdgeA2.setNext(newHalfEdgeA3);
            newHalfEdgeA3.setNext(newHalfEdgeA1);

            newHalfEdgeA1.setFace(newFaceA);
            newHalfEdgeA2.setFace(newFaceA);
            newHalfEdgeA3.setFace(newFaceA);

            newHalfEdgeA1.setStartVertex(startVertex); // is redundant.***
            newHalfEdgeA2.setStartVertex(intersectionVertex);
            newHalfEdgeA3.setStartVertex(oppositeVertexA);

            newFaceA.setHalfEdge(newHalfEdgeA1);
            intersectionVertex.setOutingHalfEdge(newHalfEdgeA2);
            oppositeVertexA.setOutingHalfEdge(newHalfEdgeA3);


            // Face C.********************************
            // In this face use the newHalfEdgeC.***
            HalfEdgeFace newFaceC = new HalfEdgeFace();
            HalfEdge newHalfEdgeC1 = new HalfEdge();
            HalfEdge newHalfEdgeC2 = new HalfEdge();
            HalfEdge newHalfEdgeC3 = new HalfEdge();
            this.getHalfEdges().add(newHalfEdgeC1);
            this.getHalfEdges().add(newHalfEdgeC2);
            this.getHalfEdges().add(newHalfEdgeC3);
            this.getFaces().add(newFaceC);

            newHalfEdgeC1.setNext(newHalfEdgeC2);
            newHalfEdgeC2.setNext(newHalfEdgeC3);
            newHalfEdgeC3.setNext(newHalfEdgeC1);

            newHalfEdgeC1.setFace(newFaceC);
            newHalfEdgeC2.setFace(newFaceC);
            newHalfEdgeC3.setFace(newFaceC);

            newHalfEdgeC1.setStartVertex(intersectionVertex);
            newHalfEdgeC2.setStartVertex(endVertex);
            newHalfEdgeC3.setStartVertex(oppositeVertexA);

            newFaceC.setHalfEdge(newHalfEdgeC1);
            intersectionVertex.setOutingHalfEdge(newHalfEdgeC1);
            oppositeVertexA.setOutingHalfEdge(newHalfEdgeC3);


            // Now set twins.***
            if (!newHalfEdgeA2.setTwin(newHalfEdgeC3)) {
                int hola = 0;
            }
            if (exteriorHEdgeA2 != null) {
                HalfEdge currTwinOfExteriorA2 = exteriorHEdgeA2.getTwin();
                if (!newHalfEdgeA3.setTwin(exteriorHEdgeA2)) {
                    int hola = 0;
                }

                if (currTwinOfExteriorA2 != null) {
                    currTwinOfExteriorA2.setTwin(null);
                }
            }

            if (exteriorHEdgeA1 != null) {
                HalfEdge currTwinOfExteriorA1 = exteriorHEdgeA1.getTwin();
                if (!newHalfEdgeC2.setTwin(exteriorHEdgeA1)) {
                    int hola = 0;
                }

                if (currTwinOfExteriorA1 != null) {
                    currTwinOfExteriorA1.setTwin(null);
                }
            }

            // finally break the relations of the halfEdgesLoopA.***
            for (int i = 0; i < hedgesACount; i++) {
                HalfEdge hedgeA = halfEdgesLoopA.get(i);
                //if (hedgeA != halfEdge) {
                    hedgeA.setStatus(ObjectStatus.DELETED);
                    hedgeA.breakRelations();
                }
            }

            // int hola = 0;

            TEST_checkHalfEdgeLength();



    }


    public void classifyFacesIdByPlane(PlaneType planeType, Vector3d planePosition) {
        int facesCount = faces.size();
        Vector3d barycenter = new Vector3d();
        if (planeType == PlaneType.XY) {
            for (int i = 0; i < facesCount; i++) {
                HalfEdgeFace face = faces.get(i);
                if (face.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }

                barycenter = face.getBarycenter(barycenter);
                double z = barycenter.z;
                if (z > planePosition.z) {
                    face.setClassifyId(2);
                } else if (z < planePosition.z) {
                    face.setClassifyId(1);
                } else {
                    face.setClassifyId(0);
                }
            }
        } else if (planeType == PlaneType.XZ) {
            for (int i = 0; i < facesCount; i++) {
                HalfEdgeFace face = faces.get(i);
                if (face.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }

                barycenter = face.getBarycenter(barycenter);
                double y = barycenter.y;
                if (y > planePosition.y) {
                    face.setClassifyId(2);
                } else if (y < planePosition.y) {
                    face.setClassifyId(1);
                } else {
                    face.setClassifyId(0);
                }
            }
        } else if (planeType == PlaneType.YZ) {
            for (int i = 0; i < facesCount; i++) {
                HalfEdgeFace face = faces.get(i);
                if (face.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }

                barycenter = face.getBarycenter(barycenter);
                double x = barycenter.x;
                if (x > planePosition.x) {
                    face.setClassifyId(2);
                } else if (x < planePosition.x) {
                    face.setClassifyId(1);
                } else {
                    face.setClassifyId(0);
                }
            }
        }
    }

    private void postReadFile()
    {
        // set the twins & others.***
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            // set the twin.***
            int twinIndex = hedge.getTwinId();
            if (twinIndex >= 0) {
                HalfEdge twin = halfEdges.get(twinIndex);
                hedge.setTwin(twin);
            }

            // set the next.***
            int nextIndex = hedge.getNextId();
            if (nextIndex >= 0) {
                HalfEdge next = halfEdges.get(nextIndex);
                hedge.setNext(next);
            }

            // set the startVertex.***
            int startVertexIndex = hedge.getStartVertexId();
            if(startVertexIndex >= 0)
            {
                HalfEdgeVertex startVertex = vertices.get(startVertexIndex);
                hedge.setStartVertex(startVertex);
            }

            // set the face.***
            int faceIndex = hedge.getFaceId();
            if(faceIndex >= 0)
            {
                HalfEdgeFace face = faces.get(faceIndex);
                hedge.setFace(face);
            }
        }

        // set the faces.***
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            int halfEdgeIndex = face.getHalfEdgeId();
            if (halfEdgeIndex >= 0) {
                HalfEdge halfEdge = halfEdges.get(halfEdgeIndex);
                face.setHalfEdge(halfEdge);
            }
        }

        // set the startVertex.***
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            int outingHalfEdgeIndex = vertex.getOutingHalfEdgeId();
            if (outingHalfEdgeIndex >= 0) {
                HalfEdge outingHalfEdge = halfEdges.get(outingHalfEdgeIndex);
                vertex.setOutingHalfEdge(outingHalfEdge);
            }
        }
    }

    public void writeFile(ObjectOutputStream outputStream) {
        /*
        private List<HalfEdge> halfEdges = new ArrayList<>();
        private List<HalfEdgeVertex> vertices = new ArrayList<>();
        private List<HalfEdgeFace> faces = new ArrayList<>();
        private GaiaBoundingBox boundingBox = null;
         */

        this.setObjectIdsInList();

        try {
            // vertices
            outputStream.writeInt(vertices.size());
            for (HalfEdgeVertex vertex : vertices) {
                vertex.writeFile(outputStream);
            }

            // faces
            outputStream.writeInt(faces.size());
            for(HalfEdgeFace face : faces) {
                face.writeFile(outputStream);
            }

            outputStream.writeObject(boundingBox);

            outputStream.writeInt(halfEdges.size());
            int counter = 0;
            for (HalfEdge halfEdge : halfEdges) {
                halfEdge.writeFile(outputStream);
            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readFile(ObjectInputStream inputStream) {
        try {
            // vertices
            int verticesCount = inputStream.readInt();
            for (int i = 0; i < verticesCount; i++) {
                HalfEdgeVertex vertex = new HalfEdgeVertex();
                vertex.readFile(inputStream);
                vertices.add(vertex);
            }
            // faces
            int facesCount = inputStream.readInt();
            for (int i = 0; i < facesCount; i++) {
                HalfEdgeFace face = new HalfEdgeFace();
                face.readFile(inputStream);
                faces.add(face);
            }
            boundingBox = (GaiaBoundingBox) inputStream.readObject();

            int halfEdgesCount = inputStream.readInt();
            for (int i = 0; i < halfEdgesCount; i++) {
                HalfEdge halfEdge = new HalfEdge();
                halfEdge.readFile(inputStream);
                halfEdges.add(halfEdge);
            }

        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        postReadFile();
    }

    public boolean existNoUsedVertices() {
        // check if there are no used vertices.***
        Map<HalfEdgeVertex, HalfEdgeVertex> vertexMap = new HashMap<>();
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeVertex startVertex = hedge.getStartVertex();
            vertexMap.put(startVertex, startVertex);
        }

        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (!vertexMap.containsKey(vertex)) {
                System.out.println("HalfEdgeSurface.existNoUsedVertices() : halfEdgeSurface has vertex that is not used.");
                return true;
            }
        }

        return false;
    }
}