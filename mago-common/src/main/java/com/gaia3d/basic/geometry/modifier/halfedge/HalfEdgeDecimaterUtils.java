package com.gaia3d.basic.geometry.modifier.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.octree.GaiaOctree;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctreeVertices;
import com.gaia3d.basic.halfedge.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.*;

@Slf4j

public class HalfEdgeDecimaterUtils {
    public static Map<HalfEdge, Vector3d> getMapHalfEdgeToDirection(Map<HalfEdge, Vector3d> resultMapHalfEdgeToDirection, List<HalfEdge> halfEdges) {
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

    public static List<HalfEdge> getHalfEdgesSortedByLength(List<HalfEdge> resultHalfEdgesSortedByLength, List<HalfEdge> halfEdges) {
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

    public static Map<HalfEdgeVertex, List<HalfEdge>> getMapVertexAllOutingEdges(
            Map<HalfEdgeVertex, List<HalfEdge>> resultVertexAllOutingEdgesMap,
            List<HalfEdgeVertex> halfEdgeVertices,
            HalfEdgeSurface surface) {

        if (resultVertexAllOutingEdgesMap == null) {
            resultVertexAllOutingEdgesMap = new HashMap<>();
        }

        for(HalfEdgeVertex vertex: halfEdgeVertices) {
            resultVertexAllOutingEdgesMap.computeIfAbsent(vertex, k -> new ArrayList<>());
        }

        List<HalfEdge> halfEdges = surface.getHalfEdges();
        for (HalfEdge halfEdge : halfEdges) {
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            if(startVertex == null){
                continue;
            }
            if (startVertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            // check if exist key.
            List<HalfEdge> edges = resultVertexAllOutingEdgesMap.get(startVertex);
            if(edges == null){
                continue;
            }
            edges.add(halfEdge);
        }

        return resultVertexAllOutingEdgesMap;
    }

    public static Map<HalfEdgeVertex, List<HalfEdge>> getMapVertexAllOutingEdges(
            Map<HalfEdgeVertex, List<HalfEdge>> resultVertexAllOutingEdgesMap, List<HalfEdge> halfEdges) {

        if (resultVertexAllOutingEdgesMap == null) {
            resultVertexAllOutingEdgesMap = new HashMap<>();
        }

        for (HalfEdge halfEdge : halfEdges) {
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            if(startVertex == null){
                continue;
            }
            if (startVertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            List<HalfEdge> edges = resultVertexAllOutingEdgesMap.computeIfAbsent(startVertex, k -> new ArrayList<>());
            edges.add(halfEdge);
        }

        return resultVertexAllOutingEdgesMap;
    }

    @SuppressWarnings("unchecked")
    public static List<HalfEdge>[] getOutgoingEdgesByVertexIdExact(
            List<HalfEdge> halfEdges,
            int verticesCount
    ) {
        if (verticesCount <= 0) {
            return (List<HalfEdge>[]) new List<?>[0];
        }

        List<HalfEdge>[] outgoingEdgesByVertexId =
                (List<HalfEdge>[]) new List<?>[verticesCount];

        if (halfEdges == null || halfEdges.isEmpty()) {
            return outgoingEdgesByVertexId;
        }

        for (HalfEdge halfEdge : halfEdges) {
            if (halfEdge == null
                    || halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeVertex startVertex = halfEdge.getStartVertex();

            if (startVertex == null
                    || startVertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            int vertexId = startVertex.getId();

            if (vertexId < 0 || vertexId >= verticesCount) {
                continue;
            }

            List<HalfEdge> outgoingEdges =
                    outgoingEdgesByVertexId[vertexId];

            if (outgoingEdges == null) {
                /*
                 * En una malla triangular normal, una capacidad
                 * inicial de 6 suele ser una buena aproximación.
                 */
                outgoingEdges = new ArrayList<>(6);
                outgoingEdgesByVertexId[vertexId] = outgoingEdges;
            }

            outgoingEdges.add(halfEdge);
        }

        return outgoingEdgesByVertexId;
    }

    @SuppressWarnings("unchecked")
    public static List<HalfEdge>[] getOutgoingEdgesByVertexIdExact_old(
            List<HalfEdge> halfEdges,
            int verticesCount
    ) {
        if (verticesCount <= 0) {
            return new List[0];
        }

        int[] outgoingEdgeCounts = new int[verticesCount];

        /*
         * Primera pasada: contar.
         */
        for (HalfEdge halfEdge : halfEdges) {
            if (halfEdge == null
                    || halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeVertex vertex = halfEdge.getStartVertex();

            if (vertex == null
                    || vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            int vertexId = vertex.getId();

            if (vertexId >= 0 && vertexId < verticesCount) {
                outgoingEdgeCounts[vertexId]++;
            }
        }

        List<HalfEdge>[] result =
                (List<HalfEdge>[]) new List<?>[verticesCount];

        /*
         * Crear las listas con capacidad exacta.
         */
        for (int vertexId = 0; vertexId < verticesCount; vertexId++) {
            int count = outgoingEdgeCounts[vertexId];

            if (count > 0) {
                result[vertexId] = new ArrayList<>(count);
            }
        }

        /*
         * Segunda pasada: rellenar.
         */
        for (HalfEdge halfEdge : halfEdges) {
            if (halfEdge == null
                    || halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeVertex vertex = halfEdge.getStartVertex();

            if (vertex == null
                    || vertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            int vertexId = vertex.getId();

            if (vertexId >= 0 && vertexId < verticesCount) {
                result[vertexId].add(halfEdge);
            }
        }

        return result;
    }

    public static Map<HalfEdgeFace, List<HalfEdge>> getMapFaceToHalfEdges(Map<HalfEdgeFace, List<HalfEdge>> resultMapFaceToHalfEdges, List<HalfEdge> halfEdges) {
        if (resultMapFaceToHalfEdges == null) {
            resultMapFaceToHalfEdges = new HashMap<>();
        }

        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            HalfEdgeFace face = halfEdge.getFace();
            List<HalfEdge> currHalfEdges = resultMapFaceToHalfEdges.computeIfAbsent(face, k -> new ArrayList<>());
            currHalfEdges.add(halfEdge);
        }

        return resultMapFaceToHalfEdges;
    }

    public static Map<HalfEdgeVertex, List<HalfEdgeVertex>> getMapVertexToSamePosVertices(Map<HalfEdgeVertex, List<HalfEdgeVertex>> resultMapVertexToSamePosVertices,
                                                                                          List<HalfEdgeVertex> vertices, boolean checkTexCoords) {
        if (resultMapVertexToSamePosVertices == null) {
            resultMapVertexToSamePosVertices = new HashMap<>();
        }

//        HalfEdgeOctreeFaces octree = new HalfEdgeOctreeFaces(null);
//        List<HalfEdgeVertex> verticesCopy = new ArrayList<>(vertices);
//        octree.setVertices(verticesCopy);
//        octree.calculateSize();
//        octree.setMaxDepth(10);
//        octree.setMinBoxSize(1.0);
//        octree.makeTreeByMinVertexCount(20);

        // new*******************
        GaiaBoundingBox bbox = new GaiaBoundingBox();
        for (HalfEdgeVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            bbox.addPoint(position);
        }
        HalfEdgeOctreeVertices octreeVertices = new HalfEdgeOctreeVertices(null, bbox);
        octreeVertices.addContents(vertices);
        octreeVertices.setLimitDepth(10);
        octreeVertices.setLimitBoxSize(1.0);
        octreeVertices.setLimitVertexCount(20);
        octreeVertices.makeTree();
        List<GaiaOctree<HalfEdgeVertex>> nodesWithContents = octreeVertices.extractOctreesWithContents();
        // end new*******************

        int nodesWithContentsCount = nodesWithContents.size();
        for (int i = 0; i < nodesWithContentsCount; i++) {
            HalfEdgeOctreeVertices node = (HalfEdgeOctreeVertices) nodesWithContents.get(i);
            List<HalfEdgeVertex> currVertices = node.getContents();
            int verticesCount = currVertices.size();
            for (int j = 0; j < verticesCount; j++) {
                HalfEdgeVertex vertex = currVertices.get(j);
                List<HalfEdgeVertex> samePosVertices = resultMapVertexToSamePosVertices.computeIfAbsent(vertex, k -> new ArrayList<>());
                samePosVertices.add(vertex);
            }

            for (int j = 0; j < verticesCount; j++) {
                HalfEdgeVertex vertex = currVertices.get(j);

                // find the samePosVertices of the vertex in the map
                // loop the keys of the map
                for (HalfEdgeVertex vertex2 : currVertices) {
                    if (vertex == vertex2) {
                        continue;
                    }

                    if (checkTexCoords) {
                        // check texCoords
                        Vector2d texCoord = vertex.getTexcoords();
                        Vector2d texCoord2 = vertex2.getTexcoords();
                        if (texCoord != null && texCoord2 != null) {
                            if (texCoord.distance(texCoord2) > 0.0001) {
                                continue;
                            }
                        }
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

    public static void getFacesImplicatedWithHalfEdge(HalfEdge hedge,
                                                      List<HalfEdgeFace> resultFacesA,
                                                      List<HalfEdgeFace> resultFacesB,
                                                      Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap) {
        HalfEdgeVertex startVertex = hedge.getStartVertex();
        HalfEdgeVertex endVertex = hedge.getEndVertex();
        HalfEdge twin = hedge.getTwin();

        // faces A.
        List<HalfEdge> outingEdgesOfStartVertex = vertexAllOutingEdgesMap.get(startVertex);
        for (HalfEdge outingEdge : outingEdgesOfStartVertex) {
            if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (outingEdge.isDegeneratedByPointers()) {
                continue;
            }

            HalfEdgeFace faceA = outingEdge.getFace();
            resultFacesA.add(faceA);
        }

        // Faces B.
        List<HalfEdge> outingEdgesOfEndVertex = vertexAllOutingEdgesMap.get(endVertex);
        for(HalfEdge outingEdge : outingEdgesOfEndVertex){
            if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (outingEdge.isDegeneratedByPointers()) {
                continue;
            }

            HalfEdgeFace faceB = outingEdge.getFace();
            resultFacesB.add(faceB);
        }
    }

    public static void getFacesImplicatedWithHalfEdge_v2(HalfEdge hedge,
                                                      List<HalfEdgeFace> resultFacesA,
                                                      List<HalfEdgeFace> resultFacesB,
                                                         List<HalfEdge>[] outgoingEdgesByVertexId) {
        HalfEdgeVertex startVertex = hedge.getStartVertex();
        HalfEdgeVertex endVertex = hedge.getEndVertex();
        HalfEdge twin = hedge.getTwin();

        // faces A.
        List<HalfEdge> outingEdgesOfStartVertex = outgoingEdgesByVertexId[startVertex.getId()];
        for (HalfEdge outingEdge : outingEdgesOfStartVertex) {
            if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (outingEdge.isDegeneratedByPointers()) {
                continue;
            }

            HalfEdgeFace faceA = outingEdge.getFace();
            resultFacesA.add(faceA);
        }

        // Faces B.
        List<HalfEdge> outingEdgesOfEndVertex = outgoingEdgesByVertexId[endVertex.getId()];
        for(HalfEdge outingEdge : outingEdgesOfEndVertex){
            if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (outingEdge.isDegeneratedByPointers()) {
                continue;
            }

            HalfEdgeFace faceB = outingEdge.getFace();
            resultFacesB.add(faceB);
        }
    }

    public static boolean decideIfCollapseCheckingFacesAdvanced(HalfEdge halfEdge,
                                                                 Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap,
                                                                 Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices,
                                                                 double maxDiffAngDeg,
                                                                 double maxAspectRatio,
                                                                 double smallHedgeSize) {

        HalfEdgeVertex deletingVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();
        HalfEdge twin = halfEdge.getTwin();
        Vector3d collapseHedgeDirection = halfEdge.getVector(null);
        collapseHedgeDirection.normalize();

        HalfEdgeFace deletingFaceA = halfEdge.getFace();
        HalfEdgeFace deletingFaceB = null;

        if(twin != null){
            deletingFaceB = twin.getFace();
        }

        List<HalfEdgeFace> facesA = new ArrayList<>();
        List<HalfEdgeFace> facesB = new ArrayList<>();

        getFacesImplicatedWithHalfEdge(halfEdge, facesA, facesB, vertexAllOutingEdgesMap);
        int facesACount = facesA.size();
        for (int i = 0; i < facesACount; i++) {
            HalfEdgeFace faceA = facesA.get(i);
            if(faceA == deletingFaceA || faceA == deletingFaceB){
                continue;
            }
            Vector3d normalA = faceA.getNormal();
            if (normalA == null) {
                List<HalfEdgeVertex> verticesA = faceA.getVertices(null);
                normalA = HalfEdgeUtils.calculateNormalAsConvex(verticesA, null);
                if (normalA == null) {
                    continue;
                }

                faceA.setNormal(normalA);
            }

            double dotProd = Math.abs(collapseHedgeDirection.dot(normalA));
            double limitDotProd = 0.6;
            // arccos(0.9) = 25.84 deg
            // arccos(0.8) = 36.87 deg
            // arccos(0.75) = 41.41 deg
            // arccos(0.70) = 45.57 deg
            // arccos(0.60) = 53.13 deg
            double hedgeLength = halfEdge.getLength();
//            if (hedgeLength < smallHedgeSize) {
//                limitDotProd = 0.8; // relax a little.
//            }
            if (dotProd > limitDotProd) {
                return false;
            }

            // simulate transformedFaceA.
            List<HalfEdgeVertex> verticesA = faceA.getVertices(null);
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

            double aspectRatio = HalfEdgeUtils.calculateAspectRatioAsTriangle(verticesB.get(0), verticesB.get(1), verticesB.get(2));
            if (aspectRatio > maxAspectRatio) {
                return false;
            }

            Vector3d normalB = HalfEdgeUtils.calculateNormalAsConvex(verticesB, null);

            if (normalB == null) {
                continue;
            }

            // analyze the angle between normalA and normalB.
            double angFactor = 1.0;
            if (hedgeLength < smallHedgeSize) {
                angFactor = hedgeLength / smallHedgeSize;
                //angFactor *= angFactor;
            }

            double angDeg = Math.toDegrees(HalfEdgeUtils.calculateAngleBetweenNormals(normalA, normalB));
            if (angDeg * angFactor > maxDiffAngDeg) {
                return false;
            }
        }

        return true;
    }

    public static boolean decideIfCollapseCheckingFacesAdvanced_v2(HalfEdge halfEdge,
                                                                   List<HalfEdge>[] outgoingEdgesByVertexId,
                                                                Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices,
                                                                double maxDiffAngDeg,
                                                                double maxAspectRatio,
                                                                double smallHedgeSize) {

        HalfEdgeVertex deletingVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();
        HalfEdge twin = halfEdge.getTwin();
        Vector3d collapseHedgeDirection = halfEdge.getVector(null);
        collapseHedgeDirection.normalize();

        HalfEdgeFace deletingFaceA = halfEdge.getFace();
        HalfEdgeFace deletingFaceB = null;

        if(twin != null){
            deletingFaceB = twin.getFace();
        }

        List<HalfEdgeFace> facesA = new ArrayList<>();
        List<HalfEdgeFace> facesB = new ArrayList<>();

        getFacesImplicatedWithHalfEdge_v2(halfEdge, facesA, facesB, outgoingEdgesByVertexId);
        int facesACount = facesA.size();
        for (int i = 0; i < facesACount; i++) {
            HalfEdgeFace faceA = facesA.get(i);
            if(faceA == deletingFaceA || faceA == deletingFaceB){
                continue;
            }
            Vector3d normalA = faceA.getNormal();
            if (normalA == null) {
                List<HalfEdgeVertex> verticesA = faceA.getVertices(null);
                normalA = HalfEdgeUtils.calculateNormalAsConvex(verticesA, null);
                if (normalA == null) {
                    continue;
                }

                faceA.setNormal(normalA);
            }

            double dotProd = Math.abs(collapseHedgeDirection.dot(normalA));
            double limitDotProd = 0.6;
            // arccos(0.9) = 25.84 deg
            // arccos(0.8) = 36.87 deg
            // arccos(0.75) = 41.41 deg
            // arccos(0.70) = 45.57 deg
            // arccos(0.60) = 53.13 deg
            double hedgeLength = halfEdge.getLength();
//            if (hedgeLength < smallHedgeSize) {
//                limitDotProd = 0.8; // relax a little.
//            }
            if (dotProd > limitDotProd) {
                return false;
            }

            // simulate transformedFaceA.
            List<HalfEdgeVertex> verticesA = faceA.getVertices(null);
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

            double aspectRatio = HalfEdgeUtils.calculateAspectRatioAsTriangle(verticesB.get(0), verticesB.get(1), verticesB.get(2));
            if (aspectRatio > maxAspectRatio) {
                return false;
            }

            Vector3d normalB = HalfEdgeUtils.calculateNormalAsConvex(verticesB, null);

            if (normalB == null) {
                continue;
            }

            // analyze the angle between normalA and normalB.
            double angFactor = 1.0;
            if (hedgeLength < smallHedgeSize) {
                angFactor = hedgeLength / smallHedgeSize;
                //angFactor *= angFactor;
            }

            double angDeg = Math.toDegrees(HalfEdgeUtils.calculateAngleBetweenNormals(normalA, normalB));
            if (angDeg * angFactor > maxDiffAngDeg) {
                return false;
            }
        }

        return true;
    }

    public static boolean decideIfCollapseCheckingFacesOnlySmallTriangles(HalfEdge halfEdge,
                                                                          Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap,
                                                                          Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices,
                                                                          double maxDiffAngDeg, double maxAspectRatio, double smallHedgeSize,
                                                                          double smallTriangleMinSize) {

        HalfEdgeVertex deletingVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();
        HalfEdge twin = halfEdge.getTwin();
        Vector3d collapseHedgeDirection = halfEdge.getVector(null);
        collapseHedgeDirection.normalize();

        List<HalfEdgeVertex> samePosVertices = mapVertexToSamePosVertices.get(deletingVertex);
        List<HalfEdge> outingEdgesOfSamePosVertices = new ArrayList<>();

        boolean allFacesAAreSmall = true;
        int samePosVertexCount = samePosVertices.size();
        for (int i = 0; i < samePosVertexCount; i++) {
            HalfEdgeVertex vertex = samePosVertices.get(i);
            List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(vertex);
            outingEdgesOfSamePosVertices.addAll(outingEdges);
        }

        // check if all faces are small triangles.***
        int outingEdgesOfDeletingVertexCount2 = outingEdgesOfSamePosVertices.size();
        for (int i = 0; i < outingEdgesOfDeletingVertexCount2; i++) {
            HalfEdge outingEdge = outingEdgesOfSamePosVertices.get(i);
            if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            HalfEdgeFace faceA = outingEdge.getFace();
            if (faceA.getId() != 10) {
                allFacesAAreSmall = false;
                break;
            }
        }

        if (allFacesAAreSmall) {
            return true;
        }
        // end check if all faces are small triangles.***

        // apply a roughness threshold to skip the check if all faces are small triangles. If the roughness is greater than 0.5, then skip the check.
//        double roughness = deletingVertex.getRoughness();
//        if (roughness > 0.5) {
//            return true;
//        }

        int normalNullsCount = 0;
        double totalAreaA = 0.0;
        double totalAreaB = 0.0;
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

//            double dotButterFly = getButterFlyDotProdForHalfEdge(outingEdge);
//            if (dotButterFly < -0.3) {
//                // acos(0.3) = 72.54 deg
//                return false;
//
////                // calculate the angle between collapseHedgeDirection and the outingEdge.
////                Vector3d outingVector = outingEdge.getVector(null);
////                outingVector.normalize();
////                double dotBetweenHEdges = collapseHedgeDirection.dot(outingVector);
////                if (Math.abs(dotBetweenHEdges) < 0.95) { // acos(0.9) = 25.84 deg
////                    //dotButterFly = getButterFlyDotProdForHalfEdge(outingEdge);
////                    return false;
////                }
//            }

            HalfEdgeFace faceA = outingEdge.getFace();
//            if (faceA.isDegenerated())
//            {
//                continue;
//            }

            List<HalfEdgeVertex> verticesA = faceA.getVertices(null);
            if (HalfEdgeUtils.getLongestEdgeLength(verticesA.get(0), verticesA.get(1), verticesA.get(2)) > smallTriangleMinSize) {
                // not a small triangle
                return false;
            }

            int faceAId = faceA.getId();
            double areaA = HalfEdgeUtils.calculateArea(verticesA.get(0), verticesA.get(1), verticesA.get(2));
//            if (faceAId != 10 && areaA > 0.8) {
//                // not a small triangle
//                return false;
//            }


            Vector3d normalA = faceA.getNormal();
            if (normalA == null) {
                normalA = HalfEdgeUtils.calculateNormalAsConvex(verticesA, null);
                if (normalA == null) {
                    continue;
                }

                faceA.setNormal(normalA);
            }

            // if the abs(dotProd) between collapseHedgeDirection and normalA is near to 1.0, then continue
//            double dotProd = Math.abs(collapseHedgeDirection.dot(normalA));
//            double limitDotProd = 0.9; // 0.75 is ok, 0.9 is more restrict
//            // arccos(0.9) = 25.84 deg
//            // arccos(0.75) = 41.41 deg
//            if (dotProd > limitDotProd) {
//                return false;
//            }

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

            // check if both triangles are small
//            if (HalfEdgeUtils.getLongestEdgeLength(verticesB.get(0), verticesB.get(1), verticesB.get(2)) > smallTriangleMinSize) {
//                // not a small triangle
//                return false;
//            }

            Vector3d normalB = HalfEdgeUtils.calculateNormalAsConvex(verticesB, null);

            if (normalB == null) {
                normalNullsCount++;
                continue;
            }

            double aspectRatio = HalfEdgeUtils.calculateAspectRatioAsTriangle(verticesB.get(0), verticesB.get(1), verticesB.get(2));
            if (aspectRatio > maxAspectRatio) {
                return false;
            }

            // for hedges with length less than 1.5m, apply a factor to the angle
            double hedgeLength = halfEdge.getLength();
            double angFactor = 1.0;
            if (hedgeLength < smallHedgeSize) {
                angFactor = Math.min(hedgeLength, smallHedgeSize);
                angFactor /= smallHedgeSize;
                angFactor *= angFactor;
            }

            FaceType faceAType = faceA.getFaceType();
            double angDeg = Math.toDegrees(HalfEdgeUtils.calculateAngleBetweenNormals(normalA, normalB));
            if (faceAType == FaceType.SKIRT) {
                // if the face is a skirt, then the angle must be less than 90 degrees
                if (angDeg * angFactor > maxDiffAngDeg * 0.3) {
                    return false;
                }
            } else {
                // if the face is not a skirt, then the angle must be less than maxDiffAngDeg
                if (angDeg * angFactor > maxDiffAngDeg) {
                    return false;
                }
            }

//            totalAreaA += areaA;
//            totalAreaB += areaB;
        }

//        double areaRatio = Math.abs(totalAreaA - totalAreaB) / totalAreaA;
//        if (Math.abs(totalAreaA - totalAreaB) / totalAreaA > 0.01) {
//            log.info("[HalfEdgeSurface][decideIfCollapseCheckingFaces] Area ratio too high :  areaRatio = " + areaRatio + "_= *** =_._= *** =_._= *** =_.");
//            return false;
//        }

        return true;
    }

    public static void calculateVerticesRoughness(HalfEdgeSurface halfEdgeSurface) {

        List<HalfEdgeVertex> vertices = halfEdgeSurface.getVertices();
        if (vertices == null || vertices.isEmpty()) return;

        //final double clusterDotThreshold = 0.95; // ~18°
        final double clusterDotThreshold = 0.90; // ~25.84°
        final int maxSafety = 100;

        for (HalfEdgeVertex v : vertices) {

            HalfEdge startEdge = v.getOutingHalfEdge();
            if (startEdge == null) {
                v.setRoughness(0.0f);
                continue;
            }

            // ============================================
            // 🔁 1. recoger normales del 1-ring
            // ============================================
            List<Vector3d> normals = new ArrayList<>();

            HalfEdge edge = startEdge;
            int safety = 0;

            do {
                if (edge == null) break;

                HalfEdgeFace face = edge.getFace();
                if (face != null) {

                    Vector3d n = face.getNormal();

                    if (n == null || n.lengthSquared() == 0) {
                        List<HalfEdgeVertex> verts = face.getVertices(null);
                        n = HalfEdgeUtils.calculateNormalAsConvex(verts, null);

                        if (n != null && n.lengthSquared() > 0) {
                            n.normalize();
                            face.setNormal(n);
                        }
                    }

                    if (n != null && n.lengthSquared() > 0) {
                        normals.add(n);
                    }
                }

                HalfEdge twin = edge.getTwin();
                if (twin == null) break;

                edge = twin.getNext();

                safety++;
                if (safety > maxSafety) break;

            } while (edge != startEdge);

            int nCount = normals.size();
            if (nCount < 2) {
                v.setRoughness(0.0f);
                v.setClassifyId(1);
                continue;
            }

            // ============================================
            // 📉 2. varianza de normales
            // ============================================
            Vector3d avgNormal = new Vector3d();

            for (Vector3d n : normals) {
                avgNormal.add(n);
            }
            avgNormal.normalize();

            double variance = 0.0;

            for (Vector3d n : normals) {
                double dot = avgNormal.dot(n);
                dot = Math.max(-1.0, Math.min(1.0, dot));
                variance += (1.0 - dot);
            }

            variance /= nCount;

            // ============================================
            // 🧠 3. clustering de normales
            // ============================================
            List<Vector3d> clusters = new ArrayList<>();

            for (Vector3d n : normals) {

                boolean found = false;

                for (Vector3d c : clusters) {
                    if (n.dot(c) > clusterDotThreshold) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    clusters.add(new Vector3d(n));
                }
            }

            int clusterCount = clusters.size();

            // ============================================
            // 📏 4. factor de escala (opcional pero útil)
            // ============================================
            double avgLen = 0.0;
            double maxLen = 0.0;
            int lenCount = 0;

            edge = startEdge;
            safety = 0;

            do {
                if (edge == null) break;

                if (edge.getStatus() != ObjectStatus.DELETED) {
                    double len = edge.getLength();
                    avgLen += len;
                    maxLen = Math.max(maxLen, len);
                    lenCount++;
                }

                HalfEdge twin = edge.getTwin();
                if (twin == null) break;

                edge = twin.getNext();

                safety++;
                if (safety > maxSafety) break;

            } while (edge != startEdge);

            double scaleFactor = 1.0;
            if (lenCount > 0) {
                avgLen /= lenCount;
                if (avgLen > 0.0) {
                    scaleFactor = maxLen / avgLen;
                }
            }

            // ============================================
            // 🎯 5. RUGOSIDAD FINAL (CLAVE)
            // ============================================

            double roughness;

            if (clusterCount <= 2) {
                // 👉 plano / escalera / esquina
                roughness = variance * 0.5;
            }
            else if (clusterCount <= 4) {
                // 👉 algo complejo pero estructurado
                roughness = variance;
            }
            else {
                // 👉 ruido real (césped)
                roughness = variance * scaleFactor * 1.5;
            }

            v.setRoughness((float) roughness);
            v.setClassifyId(clusterCount);
        }
    }

    //public static void smoothRoughness(HalfEdgeSurface surface, int iterations) {

    public static void smoothRoughness(HalfEdgeSurface surface, int iterations) {

        if (iterations <= 0) return;

        final int maxSafety = 100;

        for (int it = 0; it < iterations; it++) {

            Map<HalfEdgeVertex, Float> newValues = new HashMap<>();

            for (HalfEdgeVertex v : surface.getVertices()) {

                if (v.getStatus() == ObjectStatus.DELETED) continue;

                HalfEdge start = v.getOutingHalfEdge();
                if (start == null) {
                    newValues.put(v, v.getRoughness());
                    continue;
                }

                // ============================
                // 🔥 acumuladores con pesos
                // ============================
                double weightedSum = 0.0;
                double weightSum = 0.0;

                // 👉 incluir el propio vértice (MUY importante)
                double selfWeight = 1.0;
                weightedSum += v.getRoughness() * selfWeight;
                weightSum += selfWeight;

                HalfEdge edge = start;
                int safety = 0;

                do {
                    if (edge == null) break;

                    HalfEdge twin = edge.getTwin();
                    if (twin == null) break;

                    HalfEdgeVertex v2 = twin.getStartVertex();

                    if (v2 != null && v2.getStatus() != ObjectStatus.DELETED) {

                        double dist = v.getPosition().distance(v2.getPosition());

                        // evitar división por 0
                        double w = 1.0 / (dist + 1e-6);

                        weightedSum += v2.getRoughness() * w;
                        weightSum += w;
                    }

                    edge = twin.getNext();

                    safety++;
                    if (safety > maxSafety) break;

                } while (edge != start);

                float smoothed = (float) (weightedSum / weightSum);

                newValues.put(v, smoothed);
            }

            // aplicar resultados
            for (Map.Entry<HalfEdgeVertex, Float> entry : newValues.entrySet()) {
                entry.getKey().setRoughness(entry.getValue());
            }
        }
    }

    public static List<HalfEdgeVertex> getNeighbors(HalfEdgeVertex v) {

        List<HalfEdgeVertex> neighbors = new ArrayList<>();

        HalfEdge start = v.getOutingHalfEdge();
        if (start == null) return neighbors;

        HalfEdge edge = start;
        int safety = 0;

        do {
            HalfEdge twin = edge.getTwin();
            if (twin == null) break;

            HalfEdgeVertex v2 = twin.getStartVertex();
            if (v2 != null) {
                neighbors.add(v2);
            }

            edge = twin.getNext();

            safety++;
            if (safety > 100) break;

        } while (edge != start);

        return neighbors;
    }

    public static List<List<HalfEdgeVertex>> buildRegions(HalfEdgeSurface surface, float roughnessTolerance) {

        List<List<HalfEdgeVertex>> regions = new ArrayList<>();
        Set<HalfEdgeVertex> visited = new HashSet<>();

        for (HalfEdgeVertex v : surface.getVertices()) {

            if (visited.contains(v)) continue;
            if (v.getStatus() == ObjectStatus.DELETED) continue;

            List<HalfEdgeVertex> region = new ArrayList<>();
            Queue<HalfEdgeVertex> queue = new LinkedList<>();

            queue.add(v);
            visited.add(v);

            float baseRoughness = v.getRoughness();

            while (!queue.isEmpty()) {

                HalfEdgeVertex current = queue.poll();
                region.add(current);

                for (HalfEdgeVertex n : getNeighbors(current)) {

                    if (visited.contains(n)) continue;
                    if (n.getStatus() == ObjectStatus.DELETED) continue;

                    double diff = Math.abs(n.getRoughness() - baseRoughness);

                    if (diff < roughnessTolerance) {
                        visited.add(n);
                        queue.add(n);
                    }
                }
            }

            regions.add(region);
        }

        return regions;
    }

    public static void classifyRegions(List<List<HalfEdgeVertex>> regions) {

        int BIG_REGION = 200;       // ajustar según mesh
        double ROUGH_THRESHOLD = 0.12;

        for (List<HalfEdgeVertex> region : regions) {

            double avgRoughness = 0.0;

            for (HalfEdgeVertex v : region) {
                avgRoughness += v.getRoughness();
            }

            avgRoughness /= region.size();

            boolean isGrass =
                    (region.size() > BIG_REGION) &&
                            (avgRoughness > ROUGH_THRESHOLD);

            int classifyId = isGrass ? 1 : 0;

            for (HalfEdgeVertex v : region) {
                v.setClassifyId(classifyId);
            }
        }
    }
}
