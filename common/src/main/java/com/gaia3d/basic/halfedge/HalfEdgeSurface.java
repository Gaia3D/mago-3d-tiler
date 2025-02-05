package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctree;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.util.FileUtils;
import com.gaia3d.util.ImageUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

import static java.lang.Double.isNaN;

@Slf4j
@Setter
@Getter
public class HalfEdgeSurface implements Serializable {
    // auxiliary variables.***
    Map<AttributeType, HalfEdgeRenderableBuffer> mapAttribTypeRenderableBuffer; // GL attributes.***
    private List<HalfEdge> halfEdges = new ArrayList<>();
    private List<HalfEdgeVertex> vertices = new ArrayList<>();
    private List<HalfEdgeFace> faces = new ArrayList<>();
    private GaiaBoundingBox boundingBox = null;
    private boolean dirty = true;

    public void setTwins() {
        Map<HalfEdgeVertex, List<HalfEdge>> mapVertexOutingHEdges = new HashMap<>();
        Map<HalfEdgeVertex, List<HalfEdge>> mapVertexIncomingHEdges = new HashMap<>();

        for (HalfEdge halfEdge : halfEdges) {
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            HalfEdgeVertex endVertex = halfEdge.getEndVertex();

            if (startVertex != null) {
                List<HalfEdge> outingEdges = mapVertexOutingHEdges.computeIfAbsent(startVertex, k -> new ArrayList<>());
                outingEdges.add(halfEdge);
            }

            if (endVertex != null) {
                List<HalfEdge> incomingEdges = mapVertexIncomingHEdges.computeIfAbsent(endVertex, k -> new ArrayList<>());
                incomingEdges.add(halfEdge);
            }
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

                if (outingEdge == null) {
                    continue;
                }
                for (int k = 0; k < incomingEdgesCount; k++) {
                    HalfEdge incomingEdge = incomingEdges.get(k);
                    if (incomingEdge == null) {
                        continue;
                    }
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
    }

    public void setTwins_original() {
        Map<HalfEdgeVertex, List<HalfEdge>> mapVertexOutingHEdges = new HashMap<>();
        Map<HalfEdgeVertex, List<HalfEdge>> mapVertexIncomingHEdges = new HashMap<>();

        for (HalfEdge halfEdge : halfEdges) {
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            HalfEdgeVertex endVertex = halfEdge.getEndVertex();

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

    public void calculatePlaneNormals() {
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            face.calculatePlaneNormal();
        }
    }

    public void calculateNormals() {
        Map<HalfEdgeVertex, List<HalfEdgeFace>> vertexAllFacesMap = this.getMapVertexAllFaces(null);
        Set<HalfEdgeVertex> vertexSet = vertexAllFacesMap.keySet();
        for (HalfEdgeVertex vertex : vertexSet) {
            List<HalfEdgeFace> faces = vertexAllFacesMap.get(vertex);
            if (faces == null || faces.isEmpty()) {
                continue;
            }
            Vector3d normal = vertex.getNormal();
            if (normal == null) {
                normal = new Vector3d();
            }
            normal.set(0, 0, 0);
            for (HalfEdgeFace face : faces) {
                Vector3d faceNormal = face.calculatePlaneNormal();
                if (isNaN(faceNormal.x) || isNaN(faceNormal.y) || isNaN(faceNormal.z)) {
                    faceNormal.set(0, 0, 1);
                }
                normal.add(faceNormal);
            }
            normal.normalize();
            if (isNaN(normal.x) || isNaN(normal.y) || isNaN(normal.z)) {
                normal.set(0, 0, 1);
            }
            vertex.setNormal(normal);
        }
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

    public Map<HalfEdge, Vector3d> getMapHalfEdgeToDirection(Map<HalfEdge, Vector3d> resultMapHalfEdgeToDirection) {
        if (resultMapHalfEdgeToDirection == null) {
            resultMapHalfEdgeToDirection = new HashMap<>();
        }
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            Vector3d direction = halfEdge.getVector(null);
            direction.normalize();
            resultMapHalfEdgeToDirection.put(halfEdge, direction);
        }
        return resultMapHalfEdgeToDirection;
    }

    public Map<Vector3d, List<HalfEdgeVertex>> getMapPositionToVertices(Map<Vector3d, List<HalfEdgeVertex>> resultMapPositionToVertices) {
        if (resultMapPositionToVertices == null) {
            resultMapPositionToVertices = new HashMap<>();
        }

        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector3d position = vertex.getPosition();
            List<HalfEdgeVertex> vertices = resultMapPositionToVertices.computeIfAbsent(position, k -> new ArrayList<>());
            vertices.add(vertex);
        }

        return resultMapPositionToVertices;
    }

    public Map<HalfEdgeVertex, List<HalfEdgeVertex>> getMapVertexToSamePosVertices(Map<HalfEdgeVertex, List<HalfEdgeVertex>> resultMapVertexToSamePosVertices) {
        if (resultMapVertexToSamePosVertices == null) {
            resultMapVertexToSamePosVertices = new HashMap<>();
        }

        HalfEdgeOctree octree = new HalfEdgeOctree(null);
        List<HalfEdgeVertex> verticesCopy = new ArrayList<>(vertices);
        octree.setVertices(verticesCopy);
        octree.calculateSize();
        octree.setMaxDepth(10);
        octree.setMinBoxSize(1.0);
        octree.makeTreeByMinVertexCount(20);

        List<HalfEdgeOctree> nodesWithContents = new ArrayList<>();
        octree.extractOctreesWithContents(nodesWithContents);

        int nodesWithContentsCount = nodesWithContents.size();
        log.info("nodesWithContentsCount = " + nodesWithContentsCount);
        for (int i = 0; i < nodesWithContentsCount; i++) {
            HalfEdgeOctree node = nodesWithContents.get(i);
            List<HalfEdgeVertex> vertices = node.getVertices();
            int verticesCount = vertices.size();
            for (int j = 0; j < verticesCount; j++) {
                HalfEdgeVertex vertex = vertices.get(j);
                List<HalfEdgeVertex> samePosVertices = resultMapVertexToSamePosVertices.computeIfAbsent(vertex, k -> new ArrayList<>());
                samePosVertices.add(vertex);
            }

            for (int j = 0; j < verticesCount; j++) {
                HalfEdgeVertex vertex = vertices.get(j);

                // find the samePosVertices of the vertex in the map.***
                // loop the keys of the map.***
                for (HalfEdgeVertex vertex2 : vertices) {
                    if (vertex == vertex2) {
                        continue;
                    }
                    Vector3d pos = vertex.getPosition();
                    Vector3d pos2 = vertex2.getPosition();
                    if (pos.distance(pos2) < 0.001) {
                        List<HalfEdgeVertex> samePosVertices = resultMapVertexToSamePosVertices.get(vertex2);
                        samePosVertices.add(vertex);

                        List<HalfEdgeVertex> samePosVertices2 = resultMapVertexToSamePosVertices.get(vertex);
                        samePosVertices2.add(vertex2);
                    }
                }
            }
        }

        return resultMapVertexToSamePosVertices;
    }

    public Map<HalfEdgeFace, List<HalfEdge>> getMapFaceToHalfEdges(Map<HalfEdgeFace, List<HalfEdge>> resultMapFaceToHalfEdges) {
        if (resultMapFaceToHalfEdges == null) {
            resultMapFaceToHalfEdges = new HashMap<>();
        }

        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            HalfEdgeFace face = halfEdge.getFace();
            List<HalfEdge> halfEdges = resultMapFaceToHalfEdges.computeIfAbsent(face, k -> new ArrayList<>());
            halfEdges.add(halfEdge);
        }

        return resultMapFaceToHalfEdges;
    }

    public void doTrianglesReduction_new(double maxDiffAngDeg, double frontierMaxDiffAngDeg, double hedgeMinLength, double maxAspectRatio) {
        // 1rst, find possible halfEdges to remove.***
        // Reasons to remove a halfEdge:
        // 1. The halfEdge is very short. (small length).
        // 2. All triangles around the startVertex has a similar normal.
        //----------------------------------------------------------------
        int originalFacesCount = faces.size();
        int originalHalfEdgesCount = halfEdges.size();
        int originalVerticesCount = vertices.size();

        log.info("halfEdgesCount = " + originalHalfEdgesCount);
        int counterAux = 0;
        int hedgesCollapsedCount = 0;

        Map<HalfEdge, Vector3d> mapHalfEdgeToInitialDirection = this.getMapHalfEdgeToDirection(null);
        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap = this.getMapVertexAllOutingEdges(null);
        Map<HalfEdgeFace, List<HalfEdge>> mapFaceToHalfEdges = this.getMapFaceToHalfEdges(null);
        Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices = this.getMapVertexToSamePosVertices(null);

        // classify vertices.***
        List<List<HalfEdgeFace>> weldedFacesGroups = getWeldedFacesGroups(null);
        int weldedFacesGroupsCount = weldedFacesGroups.size();
        for (int i = 0; i < weldedFacesGroupsCount; i++) {
            List<HalfEdgeFace> weldedFacesGroup = weldedFacesGroups.get(i);
            for (HalfEdgeFace face : weldedFacesGroup) {
                List<HalfEdgeVertex> vertices = face.getVertices(null);
                for (HalfEdgeVertex vertex : vertices) {
                    vertex.setClassifyId(i);
                }
            }
        }
        // end classify vertices.---

//        // set classifying ids for all HEdges.***
//        int hedgesCount = halfEdges.size();
//        for (int i = 0; i < hedgesCount; i++) {
//            HalfEdge halfEdge = halfEdges.get(i);
//            halfEdge.setClassifyId(0);
//        }

        calculatePlaneNormals();


        Collections.shuffle(halfEdges);

        double smallHedgeSize = 1.8;

        boolean finished = false;
        int maxIterations = 50;
        int iteration = 0;
        while (!finished && iteration < maxIterations) {
            boolean collapsed = false;
            int facesCount = faces.size();
            for (int i = 0; i < facesCount; i++) {
                HalfEdgeFace face = faces.get(i);
                if (face.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }

//                if (face.isDegenerated()) {
//                    continue;
//                }

                List<HalfEdge> halfEdges = face.getHalfEdgesLoop(null);
                int halfEdgesCount = halfEdges.size();
                for (int j = 0; j < halfEdgesCount; j++) {
                    HalfEdge halfEdge = halfEdges.get(j);
                    if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                        continue;
                    }
                    if (halfEdge.isDegeneratedByPointers()) {
                        continue;
                    }
//                    if (halfEdge.isApplauseEdge()) {
//                        //continue;
//                    }

                    HalfEdgeVertex startVertex = halfEdge.getStartVertex();

                    PositionType positionType = PositionType.INTERIOR;
                    List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(startVertex);
                    int outingEdgesCount = outingEdges.size();
                    for (int k = 0; k < outingEdgesCount; k++) {
                        HalfEdge outingEdge = outingEdges.get(k);
                        if (!outingEdge.hasTwin()) {
                            positionType = PositionType.BOUNDARY_EDGE;
                            break;
                        }
                    }

                    if (halfEdge.hasTwin() && positionType == PositionType.BOUNDARY_EDGE) {
                        continue;
                    }

                    boolean testDebug = false;
//                    if (halfEdge.isApplauseEdge()) {
//                        //continue;
//                    }

                    if (halfEdge.hasTwin() && positionType == PositionType.INTERIOR) {
                        if (collapseHalfEdge(halfEdge, i, vertexAllOutingEdgesMap, mapVertexToSamePosVertices, maxDiffAngDeg, frontierMaxDiffAngDeg, hedgeMinLength, maxAspectRatio, smallHedgeSize, testDebug)) {
                            hedgesCollapsedCount += 6;
                            counterAux++;
                            collapsed = true;
                        }
                    } else if (!halfEdge.hasTwin() && positionType == PositionType.BOUNDARY_EDGE) {
                        if (collapseFrontierHalfEdge(halfEdge, i, vertexAllOutingEdgesMap, mapHalfEdgeToInitialDirection, mapVertexToSamePosVertices, maxDiffAngDeg, frontierMaxDiffAngDeg, hedgeMinLength, maxAspectRatio, smallHedgeSize, testDebug)) {
                            hedgesCollapsedCount += 3;
                            counterAux++;
                            collapsed = true;
                        }
                    }
                    if (counterAux >= 2000) {
                        counterAux = 0;
                        log.info("halfEdges deleted = " + hedgesCollapsedCount);
                    }
                }

            }

            iteration++;

            if (!collapsed) {
                finished = true;
            }
        }

        log.info("*** TOTAL HALFEDGES DELETED = " + hedgesCollapsedCount);
        log.info("total iteration = " + iteration);

        // delete objects that status is DELETED.***
        deleteDegeneratedFaces(mapFaceToHalfEdges);
        deleteNoUsedVertices();
        this.removeDeletedObjects();

        int finalFacesCount = faces.size();
        int finalHalfEdgesCount = halfEdges.size();
        int finalVerticesCount = vertices.size();

        int facesCountDiff = originalFacesCount - finalFacesCount;
        int halfEdgesCountDiff = originalHalfEdgesCount - finalHalfEdgesCount;
        int verticesCountDiff = originalVerticesCount - finalVerticesCount;

        log.info("faces % deleted = " + (facesCountDiff * 100.0) / originalFacesCount);
        log.info("halfEdges % deleted = " + (halfEdgesCountDiff * 100.0) / originalHalfEdgesCount);
        log.info("vertices % deleted = " + (verticesCountDiff * 100.0) / originalVerticesCount);
    }

    public void doTrianglesReduction(DecimateParameters decimateParameters) {
        // 1rst, find possible halfEdges to remove.***
        // Reasons to remove a halfEdge:
        // 1. The halfEdge is very short. (small length).
        // 2. All triangles around the startVertex has a similar normal.
        //----------------------------------------------------------------
        double maxDiffAngDeg = decimateParameters.getMaxDiffAngDegrees();
        double frontierMaxDiffAngDeg = decimateParameters.getFrontierMaxDiffAngDeg();
        double hedgeMinLength = decimateParameters.getHedgeMinLength();
        double maxAspectRatio = decimateParameters.getMaxAspectRatio();

        int originalFacesCount = faces.size();
        int originalHalfEdgesCount = halfEdges.size();
        int originalVerticesCount = vertices.size();

        log.info("halfEdgesCount = " + originalHalfEdgesCount);
        int counterAux = 0;
        int hedgesCollapsedCount = 0;
        int frontierHedgesCollapsedCount = 0;
        int hedgesCollapsedInOneIteration = 0;
        int frontierHedgesCollapsedInOneIteration = 0;


        double hedgeMinLengthCurrent = hedgeMinLength;

        Collections.shuffle(halfEdges);

        boolean finished = false;
        int maxIterations = 20;
        int iteration = 0;

        Map<HalfEdge, Vector3d> mapHalfEdgeToInitialDirection = new HashMap<>();
        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap = new HashMap<>();
        Map<HalfEdgeFace, List<HalfEdge>> mapFaceToHalfEdges = new HashMap<>();
        Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices = new HashMap<>();

        List<List<HalfEdgeFace>> weldedFacesGroups = new ArrayList<>();

        mapHalfEdgeToInitialDirection = this.getMapHalfEdgeToDirection(mapHalfEdgeToInitialDirection);

        // classify vertices.***
        weldedFacesGroups = getWeldedFacesGroups(weldedFacesGroups);
        int weldedFacesGroupsCount = weldedFacesGroups.size();
        for (int i = 0; i < weldedFacesGroupsCount; i++) {
            List<HalfEdgeFace> weldedFacesGroup = weldedFacesGroups.get(i);
            for (HalfEdgeFace face : weldedFacesGroup) {
                List<HalfEdgeVertex> vertices = face.getVertices(null);
                for (HalfEdgeVertex vertex : vertices) {
                    vertex.setClassifyId(i);
                }
            }
        }
        // end classify vertices.---
        double smallHedgeSize = 1.8;

        while (!finished && iteration < maxIterations) {

            // clear maps.***
            vertexAllOutingEdgesMap.clear();
            mapFaceToHalfEdges.clear();
            mapVertexToSamePosVertices.clear();

            vertexAllOutingEdgesMap = this.getMapVertexAllOutingEdges(vertexAllOutingEdgesMap);
            mapFaceToHalfEdges = this.getMapFaceToHalfEdges(mapFaceToHalfEdges);
            mapVertexToSamePosVertices = this.getMapVertexToSamePosVertices(mapVertexToSamePosVertices);

            boolean collapsed = false;
            hedgesCollapsedInOneIteration = 0;
            frontierHedgesCollapsedInOneIteration = 0;
            int halfEdgesCount = halfEdges.size();
            for (int i = 0; i < halfEdgesCount; i++) {
                HalfEdge halfEdge = halfEdges.get(i);
                if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }

                if (halfEdge.isDegeneratedByPointers()) {
                    continue;
                }


                HalfEdgeVertex startVertex = halfEdge.getStartVertex();

                PositionType positionType = PositionType.INTERIOR;
                List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(startVertex);
                int outingEdgesCount = outingEdges.size();
                for (int j = 0; j < outingEdgesCount; j++) {
                    HalfEdge outingEdge = outingEdges.get(j);
                    if (!outingEdge.hasTwin()) {
                        positionType = PositionType.BOUNDARY_EDGE;
                        break;
                    }
                }

                if (halfEdge.hasTwin() && positionType == PositionType.BOUNDARY_EDGE) {
                    continue;
                }

                boolean testDebug = false;
                if (halfEdge.isApplauseEdge()) {
                    //continue;
                }

                if (halfEdge.hasTwin() && positionType == PositionType.INTERIOR) {
                    if (collapseHalfEdge(halfEdge, i, vertexAllOutingEdgesMap, mapVertexToSamePosVertices, maxDiffAngDeg, frontierMaxDiffAngDeg, hedgeMinLengthCurrent, maxAspectRatio, smallHedgeSize, testDebug)) {
                        hedgesCollapsedCount += 1;
                        hedgesCollapsedInOneIteration += 1;
                        counterAux++;
                        collapsed = true;
                    }
                } else if (!halfEdge.hasTwin() && positionType == PositionType.BOUNDARY_EDGE) {
                    if (collapseFrontierHalfEdge(halfEdge, i, vertexAllOutingEdgesMap, mapHalfEdgeToInitialDirection, mapVertexToSamePosVertices, maxDiffAngDeg, frontierMaxDiffAngDeg, hedgeMinLengthCurrent, maxAspectRatio, smallHedgeSize, testDebug)) {
                        frontierHedgesCollapsedCount += 1;
                        frontierHedgesCollapsedInOneIteration += 1;
                        counterAux++;
                        collapsed = true;
                    }
                }
                if (counterAux >= 4000) {
                    counterAux = 0;
                    log.info("iteration = " + iteration + " halfEdges deleted = " + hedgesCollapsedCount);
                    log.info("iteration = " + iteration + " frontierHedges deleted = " + frontierHedgesCollapsedCount);
                }
            }

            if (hedgesCollapsedInOneIteration + frontierHedgesCollapsedInOneIteration < 0) {
                finished = true;
            }

            if (!collapsed) {
                finished = true;
            }

            log.info("iteration = " + iteration + ", hedgesCollapsedInOneIteration = " + hedgesCollapsedInOneIteration);
            log.info("iteration = " + iteration + ", frontierHedgesCollapsedInOneIteration = " + frontierHedgesCollapsedInOneIteration);

            iteration++;

            // delete objects that status is DELETED.***
            deleteDegeneratedFaces(mapFaceToHalfEdges);
            deleteNoUsedVertices();
            this.removeDeletedObjects();
        }

        log.info("*** TOTAL HALFEDGES DELETED = " + hedgesCollapsedCount);

        int finalFacesCount = faces.size();
        int finalHalfEdgesCount = halfEdges.size();
        int finalVerticesCount = vertices.size();

        int facesCountDiff = originalFacesCount - finalFacesCount;
        int halfEdgesCountDiff = originalHalfEdgesCount - finalHalfEdgesCount;
        int verticesCountDiff = originalVerticesCount - finalVerticesCount;

        log.info("faces % deleted = " + (facesCountDiff * 100.0) / originalFacesCount);
        log.info("halfEdges % deleted = " + (halfEdgesCountDiff * 100.0) / originalHalfEdgesCount);
        log.info("vertices % deleted = " + (verticesCountDiff * 100.0) / originalVerticesCount);
    }

    public List<HalfEdge> getHalfEdgesSortedByLength(List<HalfEdge> resultHalfEdgesSortedByLength) {
        if (resultHalfEdgesSortedByLength == null) {
            resultHalfEdgesSortedByLength = new ArrayList<>();
        }

        resultHalfEdgesSortedByLength.clear();
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            resultHalfEdgesSortedByLength.add(halfEdge);
        }

        resultHalfEdgesSortedByLength.sort((o1, o2) -> {
            double length1 = o1.getSquaredLength();
            double length2 = o2.getSquaredLength();
            if (length1 < length2) {
                return -1;
            } else if (length1 > length2) {
                return 1;
            }
            return 0;
        });

        return resultHalfEdgesSortedByLength;
    }

    public void doTrianglesReductionOneIteration(DecimateParameters decimateParameters) {
        // 1rst, find possible halfEdges to remove.***
        // Reasons to remove a halfEdge:
        // 1. The halfEdge is very short. (small length).
        // 2. All triangles around the startVertex has a similar normal.
        //----------------------------------------------------------------
        int originalFacesCount = faces.size();
        int originalHalfEdgesCount = halfEdges.size();
        int originalVerticesCount = vertices.size();

        log.info("halfEdgesCount = " + originalHalfEdgesCount);
        int counterAux = 0;
        int hedgesCollapsedCount = 0;
        int frontierHedgesCollapsedCount = 0;
        int hedgesCollapsedInOneIteration = 0;
        int frontierHedgesCollapsedInOneIteration = 0;

        double maxDiffAngDeg = decimateParameters.getMaxDiffAngDegrees();
        double frontierMaxDiffAngDeg = decimateParameters.getFrontierMaxDiffAngDeg();
        double hedgeMinLength = decimateParameters.getHedgeMinLength();
        double maxAspectRatio = decimateParameters.getMaxAspectRatio();

        double hedgeMinLengthCurrent = hedgeMinLength;

        Collections.shuffle(halfEdges);

        boolean finished = false;
        int maxIterations = decimateParameters.getIterationsCount();
        int iteration = 0;

        Map<HalfEdge, Vector3d> mapHalfEdgeToInitialDirection = new HashMap<>();
        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap = new HashMap<>();
        Map<HalfEdgeFace, List<HalfEdge>> mapFaceToHalfEdges = new HashMap<>();
        Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices = new HashMap<>();

        List<List<HalfEdgeFace>> weldedFacesGroups = new ArrayList<>();

        mapHalfEdgeToInitialDirection = this.getMapHalfEdgeToDirection(mapHalfEdgeToInitialDirection);

        // classify vertices.***
        weldedFacesGroups = getWeldedFacesGroups(weldedFacesGroups);
        int weldedFacesGroupsCount = weldedFacesGroups.size();
        for (int i = 0; i < weldedFacesGroupsCount; i++) {
            List<HalfEdgeFace> weldedFacesGroup = weldedFacesGroups.get(i);
            for (HalfEdgeFace face : weldedFacesGroup) {
                List<HalfEdgeVertex> vertices = face.getVertices(null);
                for (HalfEdgeVertex vertex : vertices) {
                    vertex.setClassifyId(i);
                }
            }
        }
        // end classify vertices.---

        List<HalfEdge> resultHalfEdgesSortedByLength = new ArrayList<>();
        //resultHalfEdgesSortedByLength = this.getHalfEdgesSortedByLength(resultHalfEdgesSortedByLength);

        double smallHedgeSize = decimateParameters.getSmallHedgeSize();

        while (!finished && iteration < maxIterations) {

            resultHalfEdgesSortedByLength.clear();
            resultHalfEdgesSortedByLength = this.getHalfEdgesSortedByLength(resultHalfEdgesSortedByLength);
            int halfEdgesCount = resultHalfEdgesSortedByLength.size();

//            double minLength = resultHalfEdgesSortedByLength.get(0).getLength();
//            double maxLength = resultHalfEdgesSortedByLength.get(halfEdgesCount - 1).getLength();
//            hedgeMinLengthCurrent = (maxLength - minLength) * 0.1;

            // classify halfEdges.***
            int hedgesCount = resultHalfEdgesSortedByLength.size();
            for (int i = 0; i < hedgesCount; i++) {
                HalfEdge halfEdge = resultHalfEdgesSortedByLength.get(i);
                halfEdge.setClassifyId(0);
            }

            // clear maps.***
            vertexAllOutingEdgesMap.clear();
            mapFaceToHalfEdges.clear();
            mapVertexToSamePosVertices.clear();

            vertexAllOutingEdgesMap = this.getMapVertexAllOutingEdges(vertexAllOutingEdgesMap);
            mapFaceToHalfEdges = this.getMapFaceToHalfEdges(mapFaceToHalfEdges);
            mapVertexToSamePosVertices = this.getMapVertexToSamePosVertices(mapVertexToSamePosVertices);

            boolean collapsed = false;
            hedgesCollapsedInOneIteration = 0;
            frontierHedgesCollapsedInOneIteration = 0;

            for (int i = 0; i < halfEdgesCount; i++) {
                HalfEdge halfEdge = resultHalfEdgesSortedByLength.get(i);
                if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }

                if (halfEdge.isDegeneratedByPointers()) {
                    continue;
                }

                if (halfEdge.getClassifyId() == 1) {
                    continue;
                }


                HalfEdgeVertex startVertex = halfEdge.getStartVertex();

                PositionType positionType = PositionType.INTERIOR;
                List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(startVertex);
                int outingEdgesCount = outingEdges.size();
                for (int j = 0; j < outingEdgesCount; j++) {
                    HalfEdge outingEdge = outingEdges.get(j);
                    if (!outingEdge.hasTwin()) {
                        positionType = PositionType.BOUNDARY_EDGE;
                        break;
                    }
                }

                if (halfEdge.hasTwin() && positionType == PositionType.BOUNDARY_EDGE) {
                    continue;
                }

                boolean testDebug = false;
//                if (halfEdge.isApplauseEdge()) {
//                    //continue;
//                }

                if (halfEdge.hasTwin() && positionType == PositionType.INTERIOR) {
                    if (collapseHalfEdge(halfEdge, i, vertexAllOutingEdgesMap, mapVertexToSamePosVertices, maxDiffAngDeg, frontierMaxDiffAngDeg, hedgeMinLengthCurrent, maxAspectRatio, smallHedgeSize, testDebug)) {
                        hedgesCollapsedCount += 1;
                        hedgesCollapsedInOneIteration += 1;
                        counterAux++;
                        collapsed = true;
                    }
                } else if (!halfEdge.hasTwin() && positionType == PositionType.BOUNDARY_EDGE) {
                    if (collapseFrontierHalfEdge(halfEdge, i, vertexAllOutingEdgesMap, mapHalfEdgeToInitialDirection, mapVertexToSamePosVertices, maxDiffAngDeg, frontierMaxDiffAngDeg, hedgeMinLengthCurrent, maxAspectRatio, smallHedgeSize, testDebug)) {
                        frontierHedgesCollapsedCount += 1;
                        frontierHedgesCollapsedInOneIteration += 1;
                        counterAux++;
                        collapsed = true;
                    }
                }
//                if (counterAux >= 4000) {
//                    counterAux = 0;
//                    log.info("iteration = " + iteration + " halfEdges deleted = " + hedgesCollapsedCount);
//                    log.info("iteration = " + iteration + " frontierHedges deleted = " + frontierHedgesCollapsedCount);
//                }

//                if(hedgesCollapsedCount + frontierHedgesCollapsedCount >= maxCollapseCount)
//                {
//                    finished = true;
//                    break;
//                }
            }


            if (hedgesCollapsedInOneIteration + frontierHedgesCollapsedInOneIteration < 0) {
                finished = true;
            }

            if (!collapsed) {
                finished = true;
            }

            log.info("iteration = " + iteration + ", hedgesCollapsedInOneIteration = " + hedgesCollapsedInOneIteration);
            log.info("iteration = " + iteration + ", frontierHedgesCollapsedInOneIteration = " + frontierHedgesCollapsedInOneIteration);

            iteration++;

            // delete objects that status is DELETED.***
            deleteDegeneratedFaces(mapFaceToHalfEdges);
            deleteNoUsedVertices();
            this.removeDeletedObjects();
            this.weldVertices(1e-4, false, false, false, false);

        }


//        boolean checkTexCoord = false;
//        boolean checkNormal = false;
//        boolean checkColor = false;
//        boolean checkBatchId = false;
//        double error = 1e-4;
//        this.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
//        this.deleteNoUsedVertices();
//        this.removeDeletedObjects();

        dirty = true;

        log.info("*** TOTAL HALFEDGES DELETED = " + hedgesCollapsedCount);

        int finalFacesCount = faces.size();
        int finalHalfEdgesCount = halfEdges.size();
        int finalVerticesCount = vertices.size();

        int facesCountDiff = originalFacesCount - finalFacesCount;
        int halfEdgesCountDiff = originalHalfEdgesCount - finalHalfEdgesCount;
        int verticesCountDiff = originalVerticesCount - finalVerticesCount;

        log.info("faces % deleted = " + (facesCountDiff * 100.0) / originalFacesCount);
        log.info("halfEdges % deleted = " + (halfEdgesCountDiff * 100.0) / originalHalfEdgesCount);
        log.info("vertices % deleted = " + (verticesCountDiff * 100.0) / originalVerticesCount);
    }

    private int deleteDegeneratedFaces(Map<HalfEdgeFace, List<HalfEdge>> mapFaceToHalfEdges) {
        int facesCount = faces.size();
        int deletedCount = 0;
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face.isDegenerated()) {
                face.setStatus(ObjectStatus.DELETED);
                List<HalfEdge> halfEdges = mapFaceToHalfEdges.get(face);
                for (HalfEdge halfEdge : halfEdges) {
                    halfEdge.setStatus(ObjectStatus.DELETED);
                }

                deletedCount++;
            }
        }

        return deletedCount;
    }

    private int deleteNoUsedVertices() {
        //*****************************************************************************************
        // Sometimes, there are no used vertices.***
        // The no used vertices must be deleted (vertex indices of the faces will be modified!).***
        //*****************************************************************************************
        Map<HalfEdgeVertex, HalfEdgeVertex> mapUsedVertices = new HashMap<>();
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            mapUsedVertices.put(startVertex, startVertex);
        }

        int deletedCount = 0;
        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            if (mapUsedVertices.get(vertex) == null) {
                vertex.setStatus(ObjectStatus.DELETED);
                deletedCount++;
            }
        }

        return deletedCount;
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
    }

    public boolean checkVertices() {
        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            if (vertex.getOutingHalfEdge() == null) {
                log.error("HalfEdgeSurface.checkVertices() : vertex.getOutingHalfEdge() == null.");
                return false;
            }
            if (vertex.getOutingHalfEdge().getStatus() == ObjectStatus.DELETED) {
                log.error("HalfEdgeSurface.checkVertices() : vertex.getOutingHalfEdge() is deleted!.");
                return false;
            }

//            PositionType positionType = vertex.getPositionType();
//            if(positionType == PositionType.INTERIOR)
//            {
//                List<HalfEdge> outingEdges = vertex.getOutingHalfEdges(null);
//                if(outingEdges == null)
//                {
//                    log.error("HalfEdgeSurface.checkVertices() : outingEdges == null.");
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
                    log.error("HalfEdgeSurface.checkEqualHEdges() : hedgesList.size() == " + hedgesList.size());
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
                log.error("HalfEdgeSurface.checkFaces() : face.getHalfEdge() == null.");
                return false;
            }
            if (face.getHalfEdge().getStatus() == ObjectStatus.DELETED) {
                log.error("HalfEdgeSurface.checkFaces() : face.getHalfEdge() is deleted!.");
                return false;
            }

            List<HalfEdge> halfEdgesLoop = new ArrayList<>();
            halfEdgesLoop = face.getHalfEdgesLoop(halfEdgesLoop);
            int halfEdgesLoopCount = halfEdgesLoop.size();
            for (int j = 0; j < halfEdgesLoopCount; j++) {
                HalfEdge halfEdge = halfEdgesLoop.get(j);
                if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                    log.error("HalfEdgeSurface.checkFaces() : halfEdge is deleted!.");
                    return false;
                }
                HalfEdgeVertex startVertex = halfEdge.getStartVertex();
                if (startVertex == null) {
                    log.error("HalfEdgeSurface.checkFaces() : startVertex == null.");
                    return false;
                }
                if (startVertex.getStatus() == ObjectStatus.DELETED) {
                    log.error("HalfEdgeSurface.checkFaces() : startVertex is deleted!.");
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
        }
        return true;
    }

    public boolean collapseFrontierHalfEdge(HalfEdge halfEdge, int iteration, Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap, Map<HalfEdge, Vector3d> mapHalfEdgeToInitialDirection, Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices, double maxDiffAngDeg, double frontierMaxDiffAngDeg, double hedgeMinLength, double maxAspectRatio, double smallHedgeSize, boolean testDebug) {

        HalfEdgeVertex startVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();
        Vector3d startPosition = startVertex.getPosition();
        Vector3d endPosition = endVertex.getPosition();


        List<HalfEdgeVertex> samePosVertices = mapVertexToSamePosVertices.get(startVertex);
        List<HalfEdge> outingEdgesOfSamePosVertices = new ArrayList<>();

        boolean isFrontierWithOtherFrontier = false;

        int samePosVertexCount = samePosVertices.size();
        for (int i = 0; i < samePosVertexCount; i++) {
            HalfEdgeVertex vertex = samePosVertices.get(i);
            List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(vertex);
            outingEdgesOfSamePosVertices.addAll(outingEdges);
        }

        //*****************************************************************************************
        // Note : if a hedge length < hedgeMinLength, then sure collapse because is very short.***
        //*****************************************************************************************

        // check if collapse.**************************************************************************************************
        // In frontier halfEdges, must check the another frontier halfEdges that uses the startVertex.***
        int outingEdgesOfStartVertexCount = outingEdgesOfSamePosVertices.size();
        if (outingEdgesOfStartVertexCount < 2) {
            return false;
        }

        for (int i = 0; i < outingEdgesOfStartVertexCount; i++) {
            HalfEdge outingEdge = outingEdgesOfSamePosVertices.get(i);
            if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            List<HalfEdge> outingLoop = outingEdge.getLoop(null);
            int outingLoopCount = outingLoop.size();
            for (int j = 0; j < outingLoopCount; j++) {
                HalfEdge outingEdge2 = outingLoop.get(j);
                if (outingEdge2.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }

                if (outingEdge2 == halfEdge) {
                    continue;
                }

                if (outingEdge2.isDegeneratedByPointers()) {
                    continue;
                }

                if (!outingEdge2.hasTwin()) {
                    // this is a frontier halfEdge.***
                    HalfEdgeVertex startVertex2 = outingEdge2.getStartVertex();
                    Vector3d startPosition2 = startVertex2.getPosition();

                    // check the angle before and after collapse.***
                    Vector3d v1 = mapHalfEdgeToInitialDirection.get(outingEdge2);
                    v1 = outingEdge2.getVector(v1);
                    v1.normalize();
                    if (isNaN(v1.x) || isNaN(v1.y) || isNaN(v1.z)) {
                        continue;
                    }

                    Vector3d v2 = new Vector3d(endPosition.x - startPosition2.x, endPosition.y - startPosition2.y, endPosition.z - startPosition2.z);
                    v2.normalize();

                    if (isNaN(v2.x) || isNaN(v2.y) || isNaN(v2.z)) {
                        continue;
                    }

                    double angRad = Math.acos(v1.dot(v2));
                    double angDeg = Math.toDegrees(angRad);

                    if (angDeg > frontierMaxDiffAngDeg) {
                        return false;
                    }
                }
            }
        }

        if (halfEdge.getLength() > hedgeMinLength) {
            if (!decideIfCollapseCheckingFaces(halfEdge, vertexAllOutingEdgesMap, mapVertexToSamePosVertices, maxDiffAngDeg, maxAspectRatio, smallHedgeSize)) {
                return false;
            }
        }
        // end check if collapse.------------------------------------------------------------------------------------------

        int endVertexClassifyId = endVertex.getClassifyId();
        boolean isCollapsed = false;

        List<HalfEdge> outingEdgesOfEndVertex = vertexAllOutingEdgesMap.get(endVertex);
        List<HalfEdgeVertex> listVertexSamePosition = mapVertexToSamePosVertices.get(startVertex);
        int samePositionVerticesCount = listVertexSamePosition.size();
        for (int i = 0; i < samePositionVerticesCount; i++) {
            HalfEdgeVertex vertex = listVertexSamePosition.get(i);
            List<HalfEdge> outingEdgesOfVertex = vertexAllOutingEdgesMap.get(vertex);
            int outingEdgesOfVertexCount = outingEdgesOfVertex.size();

            for (int j = 0; j < outingEdgesOfVertexCount; j++) {
                HalfEdge outingEdge = outingEdgesOfVertex.get(j);
                HalfEdgeVertex startVertex2 = outingEdge.getStartVertex();
                int startVertex2ClassifyId = startVertex2.getClassifyId();
                if (startVertex2ClassifyId == endVertexClassifyId) {
                    outingEdge.setStartVertex(endVertex);
                    outingEdge.setClassifyId(1);
                    outingEdgesOfEndVertex.add(outingEdge);
                    isCollapsed = true;
                } else {
                    // must find another endVertex that has the same classifyId.***
                    List<HalfEdgeVertex> listVertexEndPos = mapVertexToSamePosVertices.get(endVertex);
                    boolean isFound = false;
                    int listVertexEndPosCount = listVertexEndPos.size();
                    for (int k = 0; k < listVertexEndPosCount; k++) {
                        HalfEdgeVertex endVertex2 = listVertexEndPos.get(k);
                        int endVertex2ClassifyId = endVertex2.getClassifyId();
                        if (endVertex2ClassifyId == startVertex2ClassifyId) {
                            outingEdge.setStartVertex(endVertex2);
                            outingEdge.setClassifyId(1);
                            List<HalfEdge> outingEdgesOfEndVertex2 = vertexAllOutingEdgesMap.get(endVertex2);
                            outingEdgesOfEndVertex2.add(outingEdge);
                            isCollapsed = true;
                            isFound = true;
                            break;
                        }
                    }

//                    if(!isFound)
//                    {
//                        for(int k = 0; k < listVertexEndPosCount; k++)
//                        {
//                            HalfEdgeVertex endVertex2 = listVertexEndPos.get(k);
//                            //int endVertex2ClassifyId = endVertex2.getClassifyId();
//                            //if(endVertex2ClassifyId == startVertex2ClassifyId)
//                            {
//                                outingEdge.setStartVertex(endVertex2);
//                                List<HalfEdge> outingEdgesOfEndVertex2 = vertexAllOutingEdgesMap.get(endVertex2);
//                                outingEdgesOfEndVertex2.add(outingEdge);
//                                isCollapsed = true;
//                                isFound = true;
//                                break;
//                            }
//                        }
//                    }
                }
            }

            outingEdgesOfVertex.clear();
        }


        return isCollapsed;
    }

    private void setStartVertexToHalfEdge(HalfEdge hedge, HalfEdgeVertex startVertex, Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices) {

    }

    private HalfEdgeVertex getVertexWithClassifyId(List<HalfEdgeVertex> listVertices, int classifyId) {
        int listVerticesCount = listVertices.size();
        for (int i = 0; i < listVerticesCount; i++) {
            HalfEdgeVertex vertex = listVertices.get(i);
            if (vertex.getClassifyId() == classifyId) {
                return vertex;
            }
        }

        return null;
    }

    public boolean collapseHalfEdge(HalfEdge halfEdge, int iteration, Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap, Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices, double maxDiffAngDeg, double frontierMaxDiffAngDeg, double hedgeMinLength, double maxAspectRatio, double smallHedgeSize, boolean testDebug) {
        // When collapse a halfEdge, we delete the face, the twin's face, the twin & the startVertex.***
        // When deleting a face, must delete all halfEdges of the face.***
        // must find all halfEdges that startVertex is the deletingVertex, and set as startVertex the endVertex of the deletingHalfEdge.***

        HalfEdgeVertex startVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();


        // check if collapse.**************************************************************************************************
        if (halfEdge.getLength() > hedgeMinLength) {
            if (!decideIfCollapseCheckingFaces(halfEdge, vertexAllOutingEdgesMap, mapVertexToSamePosVertices, maxDiffAngDeg, maxAspectRatio, smallHedgeSize)) {
                return false;
            }
        }
        // end check if collapse.------------------------------------------------------------------------------------------

        int endVertexClassifyId = endVertex.getClassifyId();

        boolean isCollapsed = false;

        List<HalfEdge> outingEdgesOfEndVertex = vertexAllOutingEdgesMap.get(endVertex);
        List<HalfEdgeVertex> listVertexSamePosition = mapVertexToSamePosVertices.get(startVertex);

        if (listVertexSamePosition == null) {
            log.error("HalfEdgeSurface.collapseHalfEdge() : listVertexSamePosition == null.");
            return false;
        }

        List<HalfEdge> outingEdgesOfVertex = null;

        int samePositionVerticesCount = listVertexSamePosition.size();
        for (int i = 0; i < samePositionVerticesCount; i++) {
            HalfEdgeVertex vertex = listVertexSamePosition.get(i);
            outingEdgesOfVertex = vertexAllOutingEdgesMap.get(vertex);
            if (outingEdgesOfVertex == null) {
                log.error("HalfEdgeSurface.collapseHalfEdge() : outingEdgesOfVertex == null.");
                continue;
            }

            int outingEdgesOfVertexCount = outingEdgesOfVertex.size();
            //for (HalfEdge outingEdge : outingEdgesOfVertex) {
            // do not use the iterator because the list is modified.
            for (int gg = 0; gg < outingEdgesOfVertexCount; gg++) {
                HalfEdge outingEdge = outingEdgesOfVertex.get(gg);
                //log.info("HalfEdgeSurface.collapseHalfEdge() : outingEdge = " + count++);

                if (outingEdge == null) {
                    log.error("HalfEdgeSurface.collapseHalfEdge() : outingEdge == null.");
                    continue;
                }
                HalfEdgeVertex startVertex2 = outingEdge.getStartVertex();
                int startVertex2ClassifyId = startVertex2.getClassifyId();
                if (startVertex2ClassifyId == endVertexClassifyId) {
                    outingEdge.setStartVertex(endVertex);
                    outingEdge.setClassifyId(1);
                    outingEdgesOfEndVertex.add(outingEdge);
                    isCollapsed = true;
                } else {
                    // must find another endVertex that has the same classifyId.***
                    List<HalfEdgeVertex> listVertexEndPos = mapVertexToSamePosVertices.get(endVertex);
                    int listVertexEndPosCount = listVertexEndPos.size();
                    for (int k = 0; k < listVertexEndPosCount; k++) {
                        HalfEdgeVertex endVertex2 = listVertexEndPos.get(k);
                        int endVertex2ClassifyId = endVertex2.getClassifyId();
                        if (endVertex2ClassifyId == startVertex2ClassifyId) {
                            outingEdge.setStartVertex(endVertex2);
                            outingEdge.setClassifyId(1);
                            List<HalfEdge> outingEdgesOfEndVertex2 = vertexAllOutingEdgesMap.get(endVertex2);
                            outingEdgesOfEndVertex2.add(outingEdge);
                            isCollapsed = true;
                            break;
                        }
                    }
                }
            }

            //outingEdgesOfVertex.clear();
        }

//        halfEdge.setStatus(ObjectStatus.DELETED);
//        HalfEdge twin = halfEdge.getTwin();
//        if (twin != null) {
//            twin.setStatus(ObjectStatus.DELETED);
//        }

        return isCollapsed;
    }

    public boolean collapseFace(HalfEdgeFace face) {
        List<HalfEdge> halfEdgesLoop = face.getHalfEdgesLoop(null);
        int halfEdgesLoopCount = halfEdgesLoop.size();
        for (int i = 0; i < halfEdgesLoopCount; i++) {
            HalfEdge halfEdge = halfEdgesLoop.get(i);
            halfEdge.setStatus(ObjectStatus.DELETED);
        }

        face.setStatus(ObjectStatus.DELETED);

        return true;
    }

    public boolean decideIfCollapseCheckingFaces(HalfEdge halfEdge, Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap, Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices, double maxDiffAngDeg, double maxAspectRatio, double smallHedgeSize) {

        HalfEdgeVertex deletingVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();
        HalfEdge twin = halfEdge.getTwin();

        List<HalfEdgeVertex> samePosVertices = mapVertexToSamePosVertices.get(deletingVertex);
        List<HalfEdge> outingEdgesOfSamePosVertices = new ArrayList<>();

        int samePosVertexCount = samePosVertices.size();
        for (int i = 0; i < samePosVertexCount; i++) {
            HalfEdgeVertex vertex = samePosVertices.get(i);
            List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(vertex);
            outingEdgesOfSamePosVertices.addAll(outingEdges);
        }

        //List<HalfEdge> outingEdgesOfDeletingVertex = vertexAllOutingEdgesMap.get(deletingVertex);

        int outingEdgesOfDeletingVertexCount2 = outingEdgesOfSamePosVertices.size();
        int normalNullsCount = 0;
        for (int i = 0; i < outingEdgesOfDeletingVertexCount2; i++) {
            HalfEdge outingEdge = outingEdgesOfSamePosVertices.get(i);
            if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (twin != null) {
                if (outingEdge == twin.getNext()) {
                    continue;
                }
            }

            if (outingEdge == halfEdge) {
                continue;
            }

            if (outingEdge.isDegeneratedByPointers()) {
                continue;
            }

            HalfEdgeFace faceA = outingEdge.getFace();
//            if(faceA.isDegenerated())
//            {
//                continue;
//            }

            List<HalfEdgeVertex> verticesA = faceA.getVertices(null);

            // TODO
            double areaA = HalfEdgeUtils.calculateArea(verticesA.get(0), verticesA.get(1), verticesA.get(2));
            if (areaA < 0.01) {
                // is a small triangle, so continue.***
                continue;
            }

            Vector3d normalA = HalfEdgeUtils.calculateNormalAsConvex(verticesA, null);
            if (normalA == null) {
                continue;
            }

            List<HalfEdgeVertex> verticesB = new ArrayList<>();

            int verticesACount = verticesA.size();
            for (int j = 0; j < verticesACount; j++) {
                HalfEdgeVertex vertexA = verticesA.get(j);
                if (vertexA == deletingVertex) {
                    verticesB.add(endVertex);
                } else {
                    verticesB.add(vertexA);
                }
            }

            double areaB = HalfEdgeUtils.calculateArea(verticesB.get(0), verticesB.get(1), verticesB.get(2));
            if (areaB < 0.01) {
                // is a small triangle, so continue.***
                continue;
            }

            Vector3d normalB = HalfEdgeUtils.calculateNormalAsConvex(verticesB, null);

            if (normalB == null) {
                normalNullsCount++;
                continue;
            }

            double aspectRatio = HalfEdgeUtils.calculateAspectRatioAsTriangle(verticesB.get(0), verticesB.get(1), verticesB.get(2));
            if (aspectRatio > maxAspectRatio) {
                return false;
            }

            // for hedges with length less than 1.5m, apply a factor to the angle.***
            double hedgeLength = halfEdge.getLength();
            double angFactor = 1.0;
            if (hedgeLength < smallHedgeSize) {
                angFactor = Math.min(hedgeLength, smallHedgeSize);
                angFactor /= smallHedgeSize;
                angFactor *= angFactor;
            }

            double angDeg = Math.toDegrees(HalfEdgeUtils.calculateAngleBetweenNormals(normalA, normalB));
            if (angDeg * angFactor > maxDiffAngDeg) {
                // if hedgeLength is small, then collapse.***
                return false;
            }
        }

        return true;
    }


    public void setTwinsBetweenHalfEdges(List<HalfEdge> halfEdges) {
        // This function sets the twins between the halfEdges
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
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
        if (vertices == null || vertices.isEmpty()) {
            return resultBBox;
        }

        GaiaBoundingBox myBBox = new GaiaBoundingBox();
        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector3d position = vertex.getPosition();
            if (position != null) {
                myBBox.addPoint(position);
            }
        }

        if (resultBBox == null) {
            resultBBox = myBBox;
        } else {
            resultBBox.addBoundingBox(myBBox);
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
            cutByPlaneXY(planePosition, error);
        } else if (planeType == PlaneType.XZ) {
            cutByPlaneXZ(planePosition, error);
        } else if (planeType == PlaneType.YZ) {
            cutByPlaneYZ(planePosition, error);
        }

        removeDeletedObjects();
    }

    private void cutByPlaneXY(Vector3d planePosition, double error) {
        // find halfEdges that are cut by the plane.***

        int hedgesCutCount = 0;
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeVertex intersectionVertex = new HalfEdgeVertex();
            if (hedge.getIntersectionByPlane(PlaneType.XY, planePosition, intersectionVertex, error)) {
                splitHalfEdge(hedge, intersectionVertex);
                hedgesCount = halfEdges.size();

                hedgesCutCount++;
                if (hedgesCutCount % 200 == 0) {
                    log.info("[Tile][PhotoRealistic][cut][cutByPlaneXY] hedgesCount = " + hedgesCount + " , hedgesCutCount = " + hedgesCutCount + " , currIdx = " + i);
                }
            }
        }
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
                if (hedgesCutCount % 200 == 0) {
                    log.info("[Tile][PhotoRealistic][cut][cutByPlaneXZ] hedgesCount = " + hedgesCount + " , hedgesCutCount = " + hedgesCutCount + " , currIdx = " + i);
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
                if (hedgesCutCount % 200 == 0) {
                    log.info("[Tile][PhotoRealistic][cut][cutByPlaneYZ] hedgesCount = " + hedgesCount + " , hedgesCutCount = " + hedgesCutCount + " , currIdx = " + i);
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
                log.error("HalfEdgeSurface.checkHalfEdgesFaces() : hedge.getFace() == null.");
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
            // log.error("HalfEdgeSurface.checkTwins() : startVertex != twinEndVertex || endVertex != twinStartVertex.");
//                    return false;
//                }
//            }

            if (twin != null && twin.getStatus() != ObjectStatus.DELETED && twin.getTwin() != hedge) {
                log.error("HalfEdgeSurface.checkTwins() : twin.getTwin() != hedge.");
                return false;
            }
        }

        return true;
    }

    public boolean TEST_checkHalfEdgeLength() {
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            double length = hedge.getLength();
            if (length > 10.0) {
                return false;
            }
        }
        return true;
    }

    public boolean TEST_checkIntersectionVertex() {
        int vertexCount = vertices.size();
        for (int i = 0; i < vertexCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getNote() != null && vertex.getNote().equals("intersectionVertex")) {
                double z = vertex.getPosition().z;
                if (z > 1000.0) {

                } else {

                }
            }
        }

        return true;
    }

    public boolean TEST_checkStartVertexPosition() {
        int hedgesCount = halfEdges.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge hedge = halfEdges.get(i);
            if (hedge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeVertex startVertex = hedge.getStartVertex();
            if (startVertex.getPosition() == null) {
                return false;
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

        //TEST_checkHalfEdgeLength();


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
                    face.setClassifyId(1);
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
                    face.setClassifyId(1);
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
                    face.setClassifyId(1);
                }
            }
        }
    }

    public void deleteFacesWithClassifyId(int classifyId) {
        // must delete the faces, halfEdges, vertices.***
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face == null) {
                log.error("HalfEdgeSurface.deleteFacesWithClassifyId() : face == null.");
                continue;
            }
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (face.getClassifyId() == classifyId) {
                face.setStatus(ObjectStatus.DELETED);
                HalfEdge halfEdge = face.getHalfEdge();
                List<HalfEdge> halfEdgesLoop = new ArrayList<>();
                halfEdgesLoop = halfEdge.getLoop(halfEdgesLoop);
                int hedgesLoopCount = halfEdgesLoop.size();
                for (int j = 0; j < hedgesLoopCount; j++) {
                    HalfEdge halfEdgeLoop = halfEdgesLoop.get(j);
                    halfEdgeLoop.setStatus(ObjectStatus.DELETED);
                }
            }
        }

        removeDeletedObjects();

        // check no used vertices.***
        List<HalfEdgeVertex> noUsedVertices = new ArrayList<>();
        if (existNoUsedVertices(noUsedVertices)) {
            int noUsedVerticesCount = noUsedVertices.size();
            for (int i = 0; i < noUsedVerticesCount; i++) {
                HalfEdgeVertex vertex = noUsedVertices.get(i);
                vertex.setStatus(ObjectStatus.DELETED);
            }

            removeDeletedObjects();
        }

        setObjectIdsInList();
    }

    public void deleteFacesWithNoClassifyId(int classifyId) {
        // must delete the faces, halfEdges, vertices.***
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face == null) {
                log.error("HalfEdgeSurface.deleteFacesWithClassifyId() : face == null.");
                continue;
            }
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (face.getClassifyId() != classifyId) {
                face.setStatus(ObjectStatus.DELETED);
                HalfEdge halfEdge = face.getHalfEdge();
                List<HalfEdge> halfEdgesLoop = new ArrayList<>();
                halfEdgesLoop = halfEdge.getLoop(halfEdgesLoop);
                int hedgesLoopCount = halfEdgesLoop.size();
                for (int j = 0; j < hedgesLoopCount; j++) {
                    HalfEdge halfEdgeLoop = halfEdgesLoop.get(j);
                    halfEdgeLoop.setStatus(ObjectStatus.DELETED);
                }
            }
        }

        removeDeletedObjects();

        // check no used vertices.***
        List<HalfEdgeVertex> noUsedVertices = new ArrayList<>();
        if (existNoUsedVertices(noUsedVertices)) {
            int noUsedVerticesCount = noUsedVertices.size();
            for (int i = 0; i < noUsedVerticesCount; i++) {
                HalfEdgeVertex vertex = noUsedVertices.get(i);
                vertex.setStatus(ObjectStatus.DELETED);
            }

            removeDeletedObjects();
        }

        setObjectIdsInList();
    }

    public void deleteFacesWithClassifyId_old(int classifyId) {
        // must delete the faces, halfEdges, vertices.***
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeFace face = halfEdge.getFace();
            if (face != null && face.getClassifyId() == classifyId) {
                face.setStatus(ObjectStatus.DELETED);
                halfEdge.setStatus(ObjectStatus.DELETED);
            }
        }

        removeDeletedObjects();

        // check no used vertices.***
        List<HalfEdgeVertex> noUsedVertices = new ArrayList<>();
        if (existNoUsedVertices(noUsedVertices)) {
            int noUsedVerticesCount = noUsedVertices.size();
            for (int i = 0; i < noUsedVerticesCount; i++) {
                HalfEdgeVertex vertex = noUsedVertices.get(i);
                vertex.setStatus(ObjectStatus.DELETED);
            }

            removeDeletedObjects();
            log.info("HalfEdgeSurface.deleteFacesWithClassifyId() : existNoUsedVertices() == true.");
        }

        setObjectIdsInList();
    }

    private void postReadFile() {
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
            if (startVertexIndex >= 0) {
                HalfEdgeVertex startVertex = vertices.get(startVertexIndex);
                hedge.setStartVertex(startVertex);
            }

            // set the face.***
            int faceIndex = hedge.getFaceId();
            if (faceIndex >= 0) {
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
            for (HalfEdgeFace face : faces) {
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

    public boolean existNoUsedVertices(List<HalfEdgeVertex> noUsedVertices) {
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

        noUsedVertices.clear();

        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (!vertexMap.containsKey(vertex)) {
                noUsedVertices.add(vertex);
            }
        }

        return !noUsedVertices.isEmpty();
    }

    public HalfEdgeSurface cloneByClassifyId(int classifyId) {
        List<HalfEdgeFace> faces = new ArrayList<>();
        int facesCount = this.faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = this.faces.get(i);
            if (face.getClassifyId() == classifyId) {
                faces.add(face);
            }
        }

        if(faces.isEmpty()) {
            return null;
        }

        HalfEdgeSurface cloneSurface = new HalfEdgeSurface();

        this.setObjectIdsInList();

        // 1rst, copy vertices.***
        Map<HalfEdgeVertex, HalfEdgeVertex> mapOriginalToCloneVertex = new HashMap<>();
        List<HalfEdgeVertex> faceVertexList = new ArrayList<>();
        HalfEdgeUtils.getVerticesOfFaces(faces, faceVertexList);
        int verticesCount = faceVertexList.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = faceVertexList.get(i);
            HalfEdgeVertex cloneVertex = new HalfEdgeVertex();
            cloneVertex.copyFrom(vertex);
            cloneSurface.vertices.add(cloneVertex);

            mapOriginalToCloneVertex.put(vertex, cloneVertex);
        }

        // copy faces.***
        Map<HalfEdgeFace, HalfEdgeFace> mapOriginalToCloneFace = new HashMap<>();
        facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            HalfEdgeFace cloneFace = new HalfEdgeFace();
            cloneFace.copyFrom(face);
            cloneSurface.faces.add(cloneFace);

            mapOriginalToCloneFace.put(face, cloneFace);
        }

        // copy halfEdges.***
        Map<HalfEdge, HalfEdge> mapOriginalToCloneHalfEdge = new HashMap<>();
        List<HalfEdge> halfEdgesOfFaces = HalfEdgeUtils.getHalfEdgesOfFaces(faces, null);
        int halfEdgesCount = halfEdgesOfFaces.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdgesOfFaces.get(i);
            HalfEdge cloneHalfEdge = new HalfEdge();
            mapOriginalToCloneHalfEdge.put(halfEdge, cloneHalfEdge);
        }
        if(mapOriginalToCloneHalfEdge.isEmpty()) {
            return null;
        }

        halfEdgesCount = halfEdgesOfFaces.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdgesOfFaces.get(i);
            HalfEdge cloneHalfEdge = mapOriginalToCloneHalfEdge.get(halfEdge);

            // startVertex
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            HalfEdgeVertex cloneStartVertex = mapOriginalToCloneVertex.get(startVertex);
            cloneHalfEdge.setStartVertex(cloneStartVertex);
            cloneStartVertex.setOutingHalfEdge(cloneHalfEdge);

            // next
            HalfEdge next = halfEdge.getNext();
            HalfEdge cloneNext = mapOriginalToCloneHalfEdge.get(next);
            cloneHalfEdge.setNext(cloneNext);

            // twin
            HalfEdge twin = halfEdge.getTwin();
            HalfEdge cloneTwin = mapOriginalToCloneHalfEdge.get(twin);
            cloneHalfEdge.setTwin(cloneTwin);

            // face
            HalfEdgeFace face = halfEdge.getFace();
            HalfEdgeFace cloneFace = mapOriginalToCloneFace.get(face);
            cloneHalfEdge.setFace(cloneFace);
            cloneFace.setHalfEdge(cloneHalfEdge);

            cloneSurface.halfEdges.add(cloneHalfEdge);
        }

        cloneSurface.setTwins();

        return cloneSurface;
    }

    public HalfEdgeSurface clone() {
        HalfEdgeSurface cloneSurface = new HalfEdgeSurface();

        this.setObjectIdsInList();

        // 1rst, copy vertices.***
        Map<HalfEdgeVertex, HalfEdgeVertex> mapOriginalToCloneVertex = new HashMap<>();
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            HalfEdgeVertex cloneVertex = new HalfEdgeVertex();
            cloneVertex.copyFrom(vertex);
            cloneSurface.vertices.add(cloneVertex);

            mapOriginalToCloneVertex.put(vertex, cloneVertex);
        }

        // copy faces.***
        Map<HalfEdgeFace, HalfEdgeFace> mapOriginalToCloneFace = new HashMap<>();
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            HalfEdgeFace cloneFace = new HalfEdgeFace();
            cloneFace.copyFrom(face);
            cloneSurface.faces.add(cloneFace);

            mapOriginalToCloneFace.put(face, cloneFace);
        }

        // copy halfEdges.***
        Map<HalfEdge, HalfEdge> mapOriginalToCloneHalfEdge = new HashMap<>();
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            HalfEdge cloneHalfEdge = new HalfEdge();
            mapOriginalToCloneHalfEdge.put(halfEdge, cloneHalfEdge);
        }

        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            HalfEdge cloneHalfEdge = mapOriginalToCloneHalfEdge.get(halfEdge);

            // startVertex
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            HalfEdgeVertex cloneStartVertex = mapOriginalToCloneVertex.get(startVertex);
            cloneHalfEdge.setStartVertex(cloneStartVertex);
            cloneStartVertex.setOutingHalfEdge(cloneHalfEdge);

            // next
            HalfEdge next = halfEdge.getNext();
            HalfEdge cloneNext = mapOriginalToCloneHalfEdge.get(next);
            cloneHalfEdge.setNext(cloneNext);

            // twin
            HalfEdge twin = halfEdge.getTwin();
            HalfEdge cloneTwin = mapOriginalToCloneHalfEdge.get(twin);
            cloneHalfEdge.setTwin(cloneTwin);

            // face
            HalfEdgeFace face = halfEdge.getFace();
            HalfEdgeFace cloneFace = mapOriginalToCloneFace.get(face);
            cloneHalfEdge.setFace(cloneFace);
            cloneFace.setHalfEdge(cloneHalfEdge);

            cloneSurface.halfEdges.add(cloneHalfEdge);

            mapOriginalToCloneHalfEdge.put(halfEdge, cloneHalfEdge);
        }

        return cloneSurface;
    }

    public void scissorTextures(GaiaMaterial material) {
        // Provisionally scissor only the "DiffuseTexture".***
        if (material == null) {
            return;
        }

        Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
        List<GaiaTexture> diffuseTextures = textures.get(TextureType.DIFFUSE);
        if (diffuseTextures == null || diffuseTextures.isEmpty()) {
            return;
        }

        // load the image.***
        boolean existPngTextures = false;
        GaiaTexture texture = diffuseTextures.get(0);
        if (texture.getPath().endsWith(".png") || texture.getPath().endsWith(".PNG")) {
            existPngTextures = true;
        }

        if (texture.getBufferedImage() == null) {
            // here loads the image.***
            return;
        }
        int texWidth = texture.getWidth();
        int texHeight = texture.getHeight();

        // must find welded face-groups (faces group that are not connected with other faces).***
        List<List<HalfEdgeFace>> resultWeldedFacesGroups = new ArrayList<>();
        getWeldedFacesGroups(resultWeldedFacesGroups);

        // now, for each faceGroup, create a scissorData.***
        // there are 2 types of scissorData :
        // 1- more width than height.
        // 2- more height than width.
        Map<GaiaTextureScissorData, List<HalfEdgeFace>> scissorDataToFaceGroupMap = new HashMap<>();
        List<GaiaTextureScissorData> textureScissorDatasWidth = new ArrayList<>();
        List<GaiaTextureScissorData> textureScissorDatasHeight = new ArrayList<>();
        int weldedFacesGroupsCount = resultWeldedFacesGroups.size();

        GaiaRectangle texCoordBRect = new GaiaRectangle();
        GaiaRectangle groupTexCoordBRect = new GaiaRectangle();
        List<HalfEdgeVertex> faceVertices = new ArrayList<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> groupVertexMap = new HashMap<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> visitedVertexMap = new HashMap<>();

        boolean invertTexCoordY = false;
        for (int i = 0; i < weldedFacesGroupsCount; i++) {
            List<HalfEdgeFace> weldedFacesGroup = resultWeldedFacesGroups.get(i);
            int weldedFacesCount = weldedFacesGroup.size();
            for (int j = 0; j < weldedFacesCount; j++) {
                HalfEdgeFace face = weldedFacesGroup.get(j);
                texCoordBRect = face.getTexCoordBoundingRectangle(texCoordBRect, invertTexCoordY);

                if (j == 0) {
                    groupTexCoordBRect.copyFrom(texCoordBRect);
                } else {
                    groupTexCoordBRect.addBoundingRectangle(texCoordBRect);
                }
            }

            // check if must translate to positive quadrant.***
            if (groupTexCoordBRect.getMinX() < 0.0 || groupTexCoordBRect.getMinX() > 0.0 || groupTexCoordBRect.getMinY() < 0.0 || groupTexCoordBRect.getMinY() > 0.0) {
                double texCoordOriginX = groupTexCoordBRect.getMinX();
                double texCoordOriginY = groupTexCoordBRect.getMinY();
                double offsetX = 0.0;
                double offsetY = 0.0;
                if (texCoordOriginX < 0.0 || texCoordOriginX > 1.0) {
                    offsetX = Math.floor(texCoordOriginX);
                }

                if (texCoordOriginY < 0.0 || texCoordOriginY > 1.0) {
                    offsetY = Math.floor(texCoordOriginY);
                }

                // must translate to positive quadrant.***
                int facesCount = weldedFacesGroup.size();
                for (int j = 0; j < facesCount; j++) {
                    HalfEdgeFace face = weldedFacesGroup.get(j);
                    faceVertices.clear();
                    faceVertices = face.getVertices(faceVertices);
                    int verticesCount = faceVertices.size();
                    for (int k = 0; k < verticesCount; k++) {
                        HalfEdgeVertex vertex = faceVertices.get(k);
                        if (visitedVertexMap.containsKey(vertex)) {
                            continue;
                        }
                        Vector2d texCoord = vertex.getTexcoords();
                        texCoord.x -= offsetX;
                        texCoord.y -= offsetY;
                        visitedVertexMap.put(vertex, vertex);
                    }
                }
            }

            // create a new GaiaTextureScissorData.***
            GaiaTextureScissorData textureScissorData = new GaiaTextureScissorData();
            double minPixelPosX = groupTexCoordBRect.getMinX() * texWidth;
            double minPixelPosY = groupTexCoordBRect.getMinY() * texHeight;
            double maxPixelPosX = groupTexCoordBRect.getMaxX() * texWidth;
            double maxPixelPosY = groupTexCoordBRect.getMaxY() * texHeight;
            GaiaRectangle pixelRect = new GaiaRectangle(minPixelPosX, minPixelPosY, maxPixelPosX, maxPixelPosY);
            textureScissorData.setCurrentBoundary(pixelRect);
            double width = groupTexCoordBRect.getWidthInt();
            double height = groupTexCoordBRect.getHeightInt();

            if (width == 0 || height == 0) {
                continue;
            }

            if (width > height) {
                textureScissorDatasWidth.add(textureScissorData);
            } else {
                textureScissorDatasHeight.add(textureScissorData);
            }

            scissorDataToFaceGroupMap.put(textureScissorData, weldedFacesGroup);
        }

        // Now, sort the textureScissorDatas by xLength & yLength (big to small).***
        textureScissorDatasWidth = textureScissorDatasWidth.stream().sorted(Comparator.comparing(textureScissorData -> textureScissorData.getCurrentBoundary().getWidthInt())).collect(Collectors.toList());
        Collections.reverse(textureScissorDatasWidth);
        textureScissorDatasHeight = textureScissorDatasHeight.stream().sorted(Comparator.comparing(textureScissorData -> textureScissorData.getCurrentBoundary().getHeightInt())).collect(Collectors.toList());
        Collections.reverse(textureScissorDatasHeight);

        // make a unique textureScissorData, alternating width & height.***
        int textureScissorDatasWidthCount = textureScissorDatasWidth.size();
        int textureScissorDatasHeightCount = textureScissorDatasHeight.size();

        List<GaiaTextureScissorData> textureScissorDatas = new ArrayList<>();
        int maxCount = Math.max(textureScissorDatasWidthCount, textureScissorDatasHeightCount);
        for (int i = 0; i < maxCount; i++) {
            if (i < textureScissorDatasWidthCount) {
                textureScissorDatas.add(textureScissorDatasWidth.get(i));
            }

            if (i < textureScissorDatasHeightCount) {
                textureScissorDatas.add(textureScissorDatasHeight.get(i));
            }
        }

        // do texture atlas process.***
        doTextureAtlasProcess(textureScissorDatas);

        // recalculate texCoords for each faceGroup.***************************************************************************************************
        // TODO : must recalculate the texCoords for each faceGroup. is not necessary to recalculate all texCoords.***
        int maxWidth = getMaxWidth(textureScissorDatas);
        int maxHeight = getMaxHeight(textureScissorDatas);
        if (maxWidth == 0 || maxHeight == 0) {
            log.warn("HalfEdgeSurface.scissorTextures() : maxWidth == 0 || maxHeight == 0.");
            return;
        }

        visitedVertexMap.clear();


        int textureScissorDatasCount = textureScissorDatas.size();
        for (int i = 0; i < textureScissorDatasCount; i++) {
            GaiaTextureScissorData textureScissorData = textureScissorDatas.get(i);
            List<HalfEdgeFace> faceGroup = scissorDataToFaceGroupMap.get(textureScissorData);
            GaiaRectangle currentBoundary = textureScissorData.getCurrentBoundary();
            GaiaRectangle batchedBoundary = textureScissorData.getBatchedBoundary();

            // obtain all vertex of the faceGroup.***


            groupVertexMap.clear();
            int facesCount = faceGroup.size();
            for (int j = 0; j < facesCount; j++) {
                HalfEdgeFace face = faceGroup.get(j);
                faceVertices.clear();
                faceVertices = face.getVertices(faceVertices);
                int verticesCount = faceVertices.size();
                for (int k = 0; k < verticesCount; k++) {
                    HalfEdgeVertex vertex = faceVertices.get(k);
                    groupVertexMap.put(vertex, vertex);
                }
            }

            // now, calculate the vertex list from the map.***
            List<HalfEdgeVertex> vertexList = new ArrayList<>(groupVertexMap.values());

            int verticesCount = vertexList.size();
            for (int k = 0; k < verticesCount; k++) {
                HalfEdgeVertex vertex = vertexList.get(k);

                if (visitedVertexMap.containsKey(vertex)) {
                    int hola = 0;
                }
                visitedVertexMap.put(vertex, vertex);

                Vector2d texCoord = vertex.getTexcoords();
                double x = texCoord.x;
                double y = texCoord.y;

                double pixelX = x * texWidth;
                double pixelY = y * texHeight;

                // transform the texCoords to texCoordRelToCurrentBoundary.***
                double xRel = (pixelX - currentBoundary.getMinX()) / currentBoundary.getWidthInt();
                double yRel = (pixelY - currentBoundary.getMinY()) / currentBoundary.getHeightInt();

                // transform the texCoordRelToCurrentBoundary to atlasBoundary using batchedBoundary.***
                double xAtlas = (batchedBoundary.getMinX() + xRel * batchedBoundary.getWidthInt()) / maxWidth;
                double yAtlas = (batchedBoundary.getMinY() + yRel * batchedBoundary.getHeightInt()) / maxHeight;

                texCoord.set(xAtlas, yAtlas);
                vertex.setTexcoords(texCoord);
            }

        }


        int imageType = existPngTextures ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;

        GaiaTexture textureAtlas = new GaiaTexture();
        log.info("[Tile][PhotoRealistic][Atlas] Atlas maxWidth : " + maxWidth + " , maxHeight : " + maxHeight);
        textureAtlas.createImage(maxWidth, maxHeight, imageType);
        // fill the textureAtlas with fuchia color.***
        Color fuchiaColor = new Color(255, 255, 0);
        Graphics2D g2d = textureAtlas.getBufferedImage().createGraphics();
        g2d.setColor(fuchiaColor);
        g2d.fillRect(0, 0, maxWidth, maxHeight);
        g2d.dispose();

        //BufferedImage clampedBufferedImage = ImageUtils.clampBackGroundColor(textureAtlas.getBufferedImage(), fuchiaColor, 1, 20);
        //textureAtlas.setBufferedImage(clampedBufferedImage);

        // draw the images into textureAtlas.***
        g2d = textureAtlas.getBufferedImage().createGraphics();
        textureScissorDatasCount = textureScissorDatas.size();
        for (int i = 0; i < textureScissorDatasCount; i++) {
            GaiaTextureScissorData textureScissorData = textureScissorDatas.get(i);
            GaiaRectangle currentBoundary = textureScissorData.getCurrentBoundary();
            GaiaRectangle batchedBoundary = textureScissorData.getBatchedBoundary();
            GaiaRectangle originBoundary = textureScissorData.getOriginBoundary();

            // 1 - read from "texture" the currentBoundary.***
            // 2 - write into "textureAtlas" the batchedBoundary.***
            BufferedImage image = texture.getBufferedImage();
            int subImageMinX = (int) currentBoundary.getMinX();
            int subImageMinY = (int) currentBoundary.getMinY();

            int subImageW = Math.max(currentBoundary.getWidthInt(), 1);
            int subImageH = Math.max(currentBoundary.getHeightInt(), 1);

            int testImageWidth = image.getWidth();
            int testImageHeight = image.getHeight();
            if (subImageMinX + subImageW > testImageWidth) {
                subImageW = testImageWidth - subImageMinX;
            }

            if (subImageMinY + subImageH > testImageHeight) {
                subImageH = testImageHeight - subImageMinY;
            }

            if (subImageMinX < 0 || subImageMinX > testImageWidth || subImageMinY < 0 || subImageMinY > testImageHeight) {
                continue;
            }

            BufferedImage subImage = image.getSubimage(subImageMinX, subImageMinY, subImageW, subImageH);

            // test random color for each splitImage.***************************************************************************************************
//            Color randomColor = new Color((float) Math.random(), (float) Math.random(), (float) Math.random(), 0.8f);
//            BufferedImage randomColoredImage = new BufferedImage(subImage.getWidth(), subImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
//            Graphics2D randomGraphics = randomColoredImage.createGraphics();
//            randomGraphics.setColor(randomColor);
//            randomGraphics.fillRect(0, 0, subImage.getWidth(), subImage.getHeight());
//            randomGraphics.dispose();
//            g2d.drawImage(randomColoredImage, (int) batchedBoundary.getMinX(), (int) batchedBoundary.getMinY(), null); // test code.***

            g2d.drawImage(subImage, (int) batchedBoundary.getMinX(), (int) batchedBoundary.getMinY(), null); // original code.***

        }
        g2d.dispose();

        // write the textureAtlas into a file.***
        String imageParentPath = texture.getParentPath();
        String texturePath = texture.getPath();
        File textureFile = new File(texturePath);
        String textureRawName = textureFile.getName();
        int lastDotIndex = textureRawName.lastIndexOf(".");
        String[] textureRawNameParts = textureRawName.split("\\.");
        String textureImageExtension = textureRawNameParts[textureRawNameParts.length - 1];

        // TODO : test
        textureImageExtension = "png";

        String textureAtlasName = textureRawNameParts[0] + "_atlas_image" + "." + textureImageExtension;

        //String textureRawName = texturePath.substring(texturePath.lastIndexOf(File.separator) + 1);
        //String textureImageExtension = textureRawName.substring(textureRawName.lastIndexOf("."));
        //String textureAtlasName = textureRawName.substring(0, textureRawName.lastIndexOf(".")) + "_atlas" + textureImageExtension;

        String textureAtlasPath = imageParentPath + File.separator + textureAtlasName;
        //textureAtlas.setBufferedImage(ImageUtils.clampBackGroundColor(textureAtlas.getBufferedImage(), new Color(255, 0, 255), 1, 200));
        textureAtlas.saveImage(textureAtlasPath);

        // change the diffuseTexture path.***
        diffuseTextures.get(0).setPath(textureAtlasName);

        // delete the original texture.***
        String textureToDeletePath = imageParentPath + File.separator + texturePath;
        File textureToDelete = new File(textureToDeletePath);
        if (textureToDelete.exists()) {
            textureToDelete.delete();
        }
    }

    private void doTextureAtlasProcess(List<GaiaTextureScissorData> textureScissorDates) {
        //*********************************************************************
        // here calculates the batchedBoundaries of each textureScissorData.***
        //*********************************************************************

//        GaiaRectangle beforeMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
//        int existentSplitDatasCount = currProcessScissorDates.size();
//        for (int i = 0; i < existentSplitDatasCount; i++) {
//            GaiaTextureScissorData existentSplitData = currProcessScissorDates.get(i);
//            GaiaRectangle batchedBoundary = existentSplitData.getBatchedBoundary();
//            if (i == 0) {
//                beforeMosaicRectangle.copyFrom(batchedBoundary);
//            } else {
//                beforeMosaicRectangle.addBoundingRectangle(batchedBoundary);
//            }
//            listRectangles.add(batchedBoundary);
//        }

        GaiaRectangle beforeMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
        List<GaiaRectangle> listRectangles = new ArrayList<>();

        TreeMap<Double, List<GaiaRectangle>> mapMaxXrectangles = new TreeMap<>();

        Vector2d bestPosition = new Vector2d();
        List<GaiaTextureScissorData> currProcessScissorDates = new ArrayList<>();
        int textureScissorDatasCount = textureScissorDates.size();
        log.info("[Tile][PhotoRealistic][Atlas] doTextureAtlasProcess() : textureScissorDatasCount = " + textureScissorDatasCount);

        for (int i = 0; i < textureScissorDatasCount; i++) {
            GaiaTextureScissorData textureScissorData = textureScissorDates.get(i);
            GaiaRectangle originBoundary = textureScissorData.getOriginBoundary();

            GaiaRectangle batchedBoundary = null;
            if (i == 0) {
                // the 1rst textureScissorData.***
                batchedBoundary = new GaiaRectangle(0.0, 0.0, originBoundary.getWidthInt(), originBoundary.getHeightInt());
                textureScissorData.setBatchedBoundary(batchedBoundary);
                beforeMosaicRectangle.copyFrom(batchedBoundary);
            } else {
                // 1rst, find the best position for image into atlas.***
                bestPosition = this.getBestPositionMosaicInAtlas(currProcessScissorDates, textureScissorData, bestPosition, beforeMosaicRectangle, listRectangles, mapMaxXrectangles);
                batchedBoundary = new GaiaRectangle(bestPosition.x, bestPosition.y, bestPosition.x + originBoundary.getWidthInt(), bestPosition.y + originBoundary.getHeightInt());
                textureScissorData.setBatchedBoundary(batchedBoundary);
                beforeMosaicRectangle.addBoundingRectangle(batchedBoundary);
            }
            listRectangles.add(batchedBoundary);
            currProcessScissorDates.add(textureScissorData);

            // map.************************************************************************************************************************
            double maxX = batchedBoundary.getMaxX();

            List<GaiaRectangle> listRectanglesMaxX = mapMaxXrectangles.computeIfAbsent(maxX, k -> new ArrayList<>());
            listRectanglesMaxX.add(batchedBoundary);
        }
    }

    private int getMaxWidth(List<GaiaTextureScissorData> compareImages) {
        int result = compareImages.stream().mapToInt(textureScissorData -> (int) textureScissorData.getBatchedBoundary().getMaxX()).max().orElse(0);
        return result;
    }

    private int getMaxHeight(List<GaiaTextureScissorData> compareImages) {
        int result = compareImages.stream().mapToInt(textureScissorData -> (int) textureScissorData.getBatchedBoundary().getMaxY()).max().orElse(0);
        return result;
    }

    private Vector2d getBestPositionMosaicInAtlas(List<GaiaTextureScissorData> currProcessScissorDates, GaiaTextureScissorData scissorDataToPutInMosaic, Vector2d resultVec, GaiaRectangle beforeMosaicRectangle, List<GaiaRectangle> listRectangles, TreeMap<Double, List<GaiaRectangle>> map_maxXrectangles) {
        if (resultVec == null) {
            resultVec = new Vector2d();
        }

        double currPosX, currPosY;
        double candidatePosX = 0.0, candidatePosY = 0.0;
        double currMosaicPerimeter, candidateMosaicPerimeter;
        candidateMosaicPerimeter = -1.0;
        double error = 1.0 - 1e-6;

        // Now, try to find the best positions to put our rectangle.***
        int existentSplitDatasCount = currProcessScissorDates.size();
        for (int i = 0; i < existentSplitDatasCount; i++) {
            GaiaTextureScissorData existentSplitData = currProcessScissorDates.get(i);
            GaiaRectangle currRect = existentSplitData.getBatchedBoundary();

            // for each existent rectangles, there are 2 possibles positions: leftUp & rightDown.***
            // in this 2 possibles positions we put our leftDownCorner of rectangle of "splitData_toPutInMosaic".***

            // If in some of two positions our rectangle intersects with any other rectangle, then discard.***
            // If no intersects with others rectangles, then calculate the mosaic-perimeter.
            // We choose the minor perimeter of the mosaic.***

            double width = scissorDataToPutInMosaic.getOriginBoundary().getWidthInt();
            double height = scissorDataToPutInMosaic.getOriginBoundary().getHeightInt();

            // 1- leftUp corner.***
            currPosX = currRect.getMinX();
            currPosY = currRect.getMaxY();

            // setup our rectangle.***
            if (scissorDataToPutInMosaic.getBatchedBoundary() == null) {
                scissorDataToPutInMosaic.setBatchedBoundary(new GaiaRectangle(0.0, 0.0, 0.0, 0.0));
            }
            scissorDataToPutInMosaic.getBatchedBoundary().setMinX(currPosX);
            scissorDataToPutInMosaic.getBatchedBoundary().setMinY(currPosY);
            scissorDataToPutInMosaic.getBatchedBoundary().setMaxX(currPosX + width);
            scissorDataToPutInMosaic.getBatchedBoundary().setMaxY(currPosY + height);

            // put our rectangle into mosaic & check that no intersects with another rectangles.***
            if (!this.intersectsRectangleAtlasingProcess(listRectangles, scissorDataToPutInMosaic.getBatchedBoundary(), map_maxXrectangles)) {
                GaiaRectangle afterMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
                afterMosaicRectangle.copyFrom(beforeMosaicRectangle);
                afterMosaicRectangle.addBoundingRectangle(scissorDataToPutInMosaic.getBatchedBoundary());

                // calculate the perimeter of the mosaic.***
                if (candidateMosaicPerimeter < 0.0) {
                    candidateMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    candidatePosX = currPosX;
                    candidatePosY = currPosY;
                } else {
                    currMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    if (candidateMosaicPerimeter >= currMosaicPerimeter * error) {
                        candidateMosaicPerimeter = currMosaicPerimeter;
                        candidatePosX = currPosX;
                        candidatePosY = currPosY;
                        break; // test delete.*****************************
                    }
                }
            }

            // 2- rightDown corner.***
            currPosX = currRect.getMaxX();
            currPosY = currRect.getMinY();

            // setup our rectangle.***
            scissorDataToPutInMosaic.getBatchedBoundary().setMinX(currPosX);
            scissorDataToPutInMosaic.getBatchedBoundary().setMinY(currPosY);
            scissorDataToPutInMosaic.getBatchedBoundary().setMaxX(currPosX + width);
            scissorDataToPutInMosaic.getBatchedBoundary().setMaxY(currPosY + height);

            // put our rectangle into mosaic & check that no intersects with another rectangles.***
            if (!this.intersectsRectangleAtlasingProcess(listRectangles, scissorDataToPutInMosaic.getBatchedBoundary(), map_maxXrectangles)) {
                GaiaRectangle afterMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
                afterMosaicRectangle.copyFrom(beforeMosaicRectangle);
                afterMosaicRectangle.addBoundingRectangle(scissorDataToPutInMosaic.getBatchedBoundary());

                // calculate the perimeter of the mosaic.***
                if (candidateMosaicPerimeter < 0.0) {
                    candidateMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    candidatePosX = currPosX;
                    candidatePosY = currPosY;
                } else {
                    currMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    if (candidateMosaicPerimeter >= currMosaicPerimeter * error) {
                        candidateMosaicPerimeter = currMosaicPerimeter;
                        candidatePosX = currPosX;
                        candidatePosY = currPosY;
                        break; // test delete.*****************************
                    }
                }
            }
        }

        resultVec.set(candidatePosX, candidatePosY);

        return resultVec;
    }


    private boolean intersectsRectangleAtlasingProcess(List<GaiaRectangle> listRectangles, GaiaRectangle rectangle, TreeMap<Double, List<GaiaRectangle>> map_maxXrectangles) {
        // this function returns true if the rectangle intersects with any existent rectangle of the listRectangles.***
        boolean intersects = false;
        double error = 10E-5;

        double currRectMinX = rectangle.getMinX();

        // check with map_maxXrectangles all rectangles that have maxX > currRectMinX.***
        for (Map.Entry<Double, List<GaiaRectangle>> entry : map_maxXrectangles.tailMap(currRectMinX).entrySet()) {
            List<GaiaRectangle> existentRectangles = entry.getValue();

            int existentRectanglesCount = existentRectangles.size();
            for (int i = 0; i < existentRectanglesCount; i++) {
                GaiaRectangle existentRectangle = existentRectangles.get(i);
                if (existentRectangle == rectangle) {
                    continue;
                }
                if (existentRectangle.intersects(rectangle, error)) {
                    return true;
                }
            }
        }


//        for (GaiaRectangle existentRectangle : listRectangles) {
//            if (existentRectangle == rectangle) {
//                continue;
//            }
//            if (existentRectangle.intersects(rectangle, error)) {
//                intersects = true;
//                break;
//            }
//        }
        return intersects;
    }

    public boolean getWeldedFacesWithFace(HalfEdgeFace face, List<HalfEdgeFace> resultWeldedFaces, Map<HalfEdgeFace, HalfEdgeFace> mapVisitedFaces) {
        List<HalfEdgeFace> weldedFacesAux = new ArrayList<>();
        List<HalfEdgeFace> faces = new ArrayList<>();
        faces.add(face);
        //mapVisitedFaces.put(face, face);
        boolean finished = false;
        int counter = 0;
        while (!finished)// && counter < 10000000)
        {
            List<HalfEdgeFace> newAddedfaces = new ArrayList<>();
            int facesCount = faces.size();
            for (int i = 0; i < facesCount; i++) {
                HalfEdgeFace currFace = faces.get(i);
                if (currFace.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }

                if (mapVisitedFaces.containsKey(currFace)) {
                    continue;
                }

                resultWeldedFaces.add(currFace);
                mapVisitedFaces.put(currFace, currFace);
                weldedFacesAux.clear();
                currFace.getWeldedFaces(weldedFacesAux, mapVisitedFaces);
                newAddedfaces.addAll(weldedFacesAux);
            }

            if (newAddedfaces.isEmpty()) {
                finished = true;
            } else {
                faces.clear();
                faces.addAll(newAddedfaces);
            }

            counter++;
        }


        return true;
    }

    public List<List<HalfEdgeFace>> getWeldedFacesGroups(List<List<HalfEdgeFace>> resultWeldedFacesGroups) {
        if (resultWeldedFacesGroups == null) {
            resultWeldedFacesGroups = new ArrayList<>();
        }

        Map<HalfEdgeVertex, List<HalfEdgeFace>> vertexFacesMap = getMapVertexAllFaces(null);
        Map<HalfEdgeFace, HalfEdgeFace> mapVisitedFaces = new HashMap<>();
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (mapVisitedFaces.containsKey(face)) {
                continue;
            }

            List<HalfEdgeFace> weldedFaces = new ArrayList<>();
            this.getWeldedFacesWithFace(face, weldedFaces, mapVisitedFaces);
            resultWeldedFacesGroups.add(weldedFaces);
        }

        return resultWeldedFacesGroups;
    }

    public List<List<HalfEdgeFace>> getWeldedFacesGroupsRecursive(List<List<HalfEdgeFace>> resultWeldedFacesGroups) {
        if (resultWeldedFacesGroups == null) {
            resultWeldedFacesGroups = new ArrayList<>();
        }

        Map<HalfEdgeVertex, List<HalfEdgeFace>> vertexFacesMap = getMapVertexAllFaces(null);
        Map<HalfEdgeFace, HalfEdgeFace> mapVisitedFaces = new HashMap<>();
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (mapVisitedFaces.containsKey(face)) {
                continue;
            }

            List<HalfEdgeFace> weldedFaces = new ArrayList<>();
            face.getWeldedFacesRecursive(weldedFaces, mapVisitedFaces);
            resultWeldedFacesGroups.add(weldedFaces);
        }

        return resultWeldedFacesGroups;
    }

    public int getTrianglesCount() {
        int hedgesCount = halfEdges.size();
        int trianglesCount = hedgesCount / 3; // provisionally.***
        return trianglesCount;
    }

    public void setBoxTexCoordsXY(GaiaBoundingBox box) {
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector3d position = vertex.getPosition();
            Vector2d texCoord = new Vector2d();
            double relPosX = (position.x - box.getMinX()) / box.getSizeX();
            double relPosY = (position.y - box.getMinY()) / box.getSizeY();

            texCoord.set(relPosX, 1.0 - relPosY);
            vertex.setTexcoords(texCoord);
        }
    }

    public void changeOutingHEdgesOfVertexIfHEdgeIsDeleted() {
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdge outgoingHEdge = vertex.getOutingHalfEdge();
            if (outgoingHEdge.getStatus() == ObjectStatus.DELETED) {
                vertex.changeOutingHalfEdge();
            }
        }
    }

    private void getWeldableVertexMap(Map<HalfEdgeVertex, HalfEdgeVertex> mapVertexToVertexMaster, List<HalfEdgeVertex> vertices, double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        Map<HalfEdgeVertex, HalfEdgeVertex> visitedMap = new HashMap<>();
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (visitedMap.containsKey(vertex)) {
                continue;
            }

            mapVertexToVertexMaster.put(vertex, vertex);

            for (int j = i + 1; j < verticesCount; j++) {
                HalfEdgeVertex vertex2 = vertices.get(j);
                if (visitedMap.containsKey(vertex2)) {
                    continue;
                }
                if (vertex.isWeldable(vertex2, error, checkTexCoord, checkNormal, checkColor, checkBatchId)) {
                    mapVertexToVertexMaster.put(vertex2, vertex);

                    visitedMap.put(vertex, vertex);
                    visitedMap.put(vertex2, vertex2);
                }
            }
        }
    }

    private List<HalfEdge> getTwinablesByPosition(double error, HalfEdge halfEdge, Map<HalfEdgeVertex, HalfEdgeVertex> mapVertexToVertexMaster, Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexMasterToVertices, List<HalfEdge> resultTwinables) {
        //*******************************************************************
        // There are 2 ways to get twinables : by pointers or by position.***
        //*******************************************************************
        if (resultTwinables == null) {
            resultTwinables = new ArrayList<>();
        }

        HalfEdgeVertex startVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();

        HalfEdgeVertex startVertexMaster = mapVertexToVertexMaster.get(startVertex);
        HalfEdgeVertex endVertexMaster = mapVertexToVertexMaster.get(endVertex);

        //List<HalfEdgeVertex> startVertices = mapVertexMasterToVertices.get(startVertexMaster);
        List<HalfEdgeVertex> endVertices = mapVertexMasterToVertices.get(endVertexMaster);

        // now, find halfEdges that has startVertex in endVertex list. and endVertex in startVertex list.***
        int endVertexCount = endVertices.size();
        for (int i = 0; i < endVertexCount; i++) {
            HalfEdgeVertex vertex = endVertices.get(i);
            HalfEdge outingHalfEdge = vertex.getOutingHalfEdge();

            if (outingHalfEdge == halfEdge) {
                // impossible.***
                continue;
            }

            if (outingHalfEdge.hasTwin()) {
                continue;
            }

            if (outingHalfEdge == null) {
                log.error("error : HalfEdgeSurface.getTwinablesByPosition() : outingHalfEdge is null.");
            }

            HalfEdgeVertex endVertex2 = outingHalfEdge.getEndVertex();
            HalfEdgeVertex vertexMaster = mapVertexToVertexMaster.get(endVertex2);

            if (vertexMaster == startVertexMaster) {
                resultTwinables.add(outingHalfEdge);
            }
        }

        return resultTwinables;
    }

    private GaiaSurface getGaiaSurface(List<GaiaVertex> resultGaiaVertices) {
        // 1rst, make maps.***
        GaiaSurface provisionalSurface = new GaiaSurface();
        Map<HalfEdgeVertex, GaiaVertex> mapHalfEdgeVertexToGaiaVertex = new HashMap<>();
        Map<GaiaVertex, Integer> mapGaiaVertexToIndex = new HashMap<>();

        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            if (vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            GaiaVertex provisionalVertex = vertex.toGaiaVertex();
            resultGaiaVertices.add(provisionalVertex);
            mapHalfEdgeVertexToGaiaVertex.put(vertex, provisionalVertex);
            mapGaiaVertexToIndex.put(provisionalVertex, i);
        }

        // now, make the provisional faces.***
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace halfEdgeFace = faces.get(i);
            if (halfEdgeFace == null) {
                continue;
            }

            if (halfEdgeFace.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (halfEdgeFace.isDegenerated()) {
                continue;
            }

            GaiaFace gaiaFace = new GaiaFace();

            List<HalfEdgeVertex> halfEdgeVertices = halfEdgeFace.getVertices(null);
            int faceVerticesCount = halfEdgeVertices.size();
            int[] indices = new int[faceVerticesCount];
            int indicesCount = 0;
            for (int j = 0; j < faceVerticesCount; j++) {
                HalfEdgeVertex halfEdgeVertex = halfEdgeVertices.get(j);
                GaiaVertex gaiaVertex = mapHalfEdgeVertexToGaiaVertex.get(halfEdgeVertex);
                if (gaiaVertex == null) {
                    continue;
                }
                indices[j] = mapGaiaVertexToIndex.get(gaiaVertex);
                indicesCount++;
            }

            if (indicesCount > 2) {
                gaiaFace.setIndices(indices);
                provisionalSurface.getFaces().add(gaiaFace);
            } else {
                gaiaFace = null;
            }
        }

        return provisionalSurface;
    }

    public void weldVertices(double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        // make a provisional GaiaSurface.***
        List<GaiaVertex> gaiaVertices = new ArrayList<>();
        GaiaSurface gaiaSurface = this.getGaiaSurface(gaiaVertices);

        HalfEdgeUtils.weldVerticesGaiaSurface(gaiaSurface, gaiaVertices, error, checkTexCoord, checkNormal, checkColor, checkBatchId);

        // Now, delete the halfEdge objects.***
        this.deleteObjects();

        // now, make halfEdgeSurface from the provisionalSurface.***
        Map<GaiaVertex, HalfEdgeVertex> mapGaiaVertexToHalfEdgeVertex = new HashMap<>();

        // faces.***
        List<GaiaFace> gaiaFaces = gaiaSurface.getFaces();
        int facesCount = gaiaFaces.size();
        for (int i = 0; i < facesCount; i++) {
            GaiaFace gaiaFace = gaiaFaces.get(i);
            if (gaiaFace == null) {
                log.error("gaiaFace == null.***");
                continue;
            }
            List<GaiaFace> gaiaTriangleFaces = new HalfEdgeUtils().getGaiaTriangleFacesFromGaiaFace(gaiaFace);
            int triangleFacesCount = gaiaTriangleFaces.size();
            for (int j = 0; j < triangleFacesCount; j++) {
                GaiaFace gaiaTriangleFace = gaiaTriangleFaces.get(j);
                if (gaiaTriangleFace == null) {
                    continue;
                }
                HalfEdgeFace halfEdgeFace = HalfEdgeUtils.halfEdgeFaceFromGaiaFace(gaiaTriangleFace, gaiaVertices, this, mapGaiaVertexToHalfEdgeVertex);
                this.getFaces().add(halfEdgeFace);
            }
        }

        List<HalfEdgeVertex> halfEdgeVertices = new ArrayList<>(mapGaiaVertexToHalfEdgeVertex.values());
        this.getVertices().addAll(halfEdgeVertices);

        // set twins.***
        this.setTwins();
        this.checkSandClockFaces();

        // finally delete gaiaSurface.***
        gaiaSurface.clear();
    }

    public void translate(Vector3d translation) {
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector3d position = vertex.getPosition();
            position.add(translation);
        }
    }

    public int[] getIndices() {
        Map<HalfEdgeVertex, Integer> vertexIndexMap = new HashMap<>();
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            vertexIndexMap.put(vertex, i);
        }

        int facesCount = faces.size();
        int[] indices = new int[facesCount * 3];
        int index = 0;
        List<HalfEdgeVertex> faceVertices = new ArrayList<>();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            faceVertices.clear();
            faceVertices = face.getVertices(faceVertices);
            for (int j = 0; j < 3; j++) {
                HalfEdgeVertex vertex = faceVertices.get(j);
                int vertexIndex = vertexIndexMap.get(vertex);
                indices[index++] = vertexIndex;
            }
        }

        return indices;
    }

    public boolean getDirty() {
        return dirty;
    }

    public void joinSurface(HalfEdgeSurface newSurface) {
        this.vertices.addAll(newSurface.getVertices());
        this.faces.addAll(newSurface.getFaces());
        this.halfEdges.addAll(newSurface.getHalfEdges());
        this.dirty = true;
    }

    public void splitFacesByBestPlanesToProject() {
        // make faceGroups by classifyId & bestPlaneType.***
        Map<Integer, Map<PlaneType, List<HalfEdgeFace>>> mapFaceGroupByClassifyIdAndPlaneType = new HashMap<>();
        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            PlaneType bestPlaneType = face.calculateBestPlaneToProject();
            if (bestPlaneType == PlaneType.XYNEG) {
                bestPlaneType = PlaneType.XY;
                face.setBestPlaneToProject(bestPlaneType);
            }
            int ClassifyId = face.getClassifyId();

            Map<PlaneType, List<HalfEdgeFace>> mapFaceGroupByPlaneType = mapFaceGroupByClassifyIdAndPlaneType.computeIfAbsent(ClassifyId, k -> new HashMap<>());
            List<HalfEdgeFace> faceGroup = mapFaceGroupByPlaneType.computeIfAbsent(bestPlaneType, k -> new ArrayList<>());
            faceGroup.add(face);
        }

        // for each faceGroups make a surface.***
        HalfEdgeSurface newSurfaceMaster = new HalfEdgeSurface();
        boolean checkClassifyId = true;
        boolean checkBestPlaneToProject = true;
        for (Map<PlaneType, List<HalfEdgeFace>> mapFaceGroupByPlaneType : mapFaceGroupByClassifyIdAndPlaneType.values()) {
            for (List<HalfEdgeFace> faceGroup : mapFaceGroupByPlaneType.values()) {
                HalfEdgeSurface newSurface = HalfEdgeCutter.createHalfEdgeSurfaceByFacesCopy(faceGroup, checkClassifyId, checkBestPlaneToProject);
                newSurfaceMaster.joinSurface(newSurface);
            }
        }

        this.deleteObjects();
        this.joinSurface(newSurfaceMaster);
    }

    public Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> getMapClassifyIdToCameraDirectionTypeToFaces(Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapFaceGroupByClassifyIdAndObliqueCamDirType) {
        if (mapFaceGroupByClassifyIdAndObliqueCamDirType == null) {
            mapFaceGroupByClassifyIdAndObliqueCamDirType = new HashMap<>();
        }

        int facesCount = faces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            CameraDirectionType bestObliqueCameraDirectionType = face.getCameraDirectionType();
            int ClassifyId = face.getClassifyId();

            Map<CameraDirectionType, List<HalfEdgeFace>> mapFaceGroupByPlaneType = mapFaceGroupByClassifyIdAndObliqueCamDirType.computeIfAbsent(ClassifyId, k -> new HashMap<>());
            List<HalfEdgeFace> faceGroup = mapFaceGroupByPlaneType.computeIfAbsent(bestObliqueCameraDirectionType, k -> new ArrayList<>());
            faceGroup.add(face);
        }

        return mapFaceGroupByClassifyIdAndObliqueCamDirType;
    }

    public void splitFacesByBestObliqueCameraDirectionToProject() {
        // make faceGroups by classifyId & bestObliqueCameraDirectionType.***
        Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapFaceGroupByClassifyIdAndObliqueCamDirType = this.getMapClassifyIdToCameraDirectionTypeToFaces(null);

        // for each faceGroups make a surface.***
        HalfEdgeSurface newSurfaceMaster = new HalfEdgeSurface();
        boolean checkClassifyId = true;
        boolean checkBestPlaneToProject = true;
        for (Map<CameraDirectionType, List<HalfEdgeFace>> mapFaceGroupByPlaneType : mapFaceGroupByClassifyIdAndObliqueCamDirType.values()) {
            for (List<HalfEdgeFace> faceGroup : mapFaceGroupByPlaneType.values()) {
                HalfEdgeSurface newSurface = HalfEdgeCutter.createHalfEdgeSurfaceByFacesCopy(faceGroup, checkClassifyId, checkBestPlaneToProject);
                newSurfaceMaster.joinSurface(newSurface);
            }
        }

        this.deleteObjects();
        this.joinSurface(newSurfaceMaster);
    }

    public void getWestEastSouthNorthVertices(GaiaBoundingBox bbox, List<HalfEdgeVertex> westVertices, List<HalfEdgeVertex> eastVertices, List<HalfEdgeVertex> southVertices, List<HalfEdgeVertex> northVertices, double error) {
        int verticesCount = vertices.size();
        double west = bbox.getMinX();
        double east = bbox.getMaxX();
        double south = bbox.getMinY();
        double north = bbox.getMaxY();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector3d position = vertex.getPosition();
            if (Math.abs(position.x - west) < error) {
                westVertices.add(vertex);
            } else if (Math.abs(position.x - east) < error) {
                eastVertices.add(vertex);
            } else if (Math.abs(position.y - south) < error) {
                southVertices.add(vertex);
            } else if (Math.abs(position.y - north) < error) {
                northVertices.add(vertex);
            }
        }
    }


}