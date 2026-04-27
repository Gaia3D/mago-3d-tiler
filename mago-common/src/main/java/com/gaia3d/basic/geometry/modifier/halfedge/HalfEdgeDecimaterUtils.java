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

    public static MapVertexAllOutingEdgesIndices getMapVertexAllOutingEdgesIndices(HalfEdgeSurface surface) {
        surface.setObjectIdsInList();

        // 1- count incidents.
        List<HalfEdgeVertex> vertices = surface.getVertices();
        int numVertices = vertices.size();
        int[] counts = new int[numVertices];

        List<HalfEdge> halfEdges = surface.getHalfEdges();

        for (HalfEdge halfEdge : halfEdges) {
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            if (startVertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            int vertexId = startVertex.getId();
            counts[vertexId]++;
        }

        // 2 - calculate offsets.
        int[] vertexOffsets = new int[numVertices + 1];
        for (int i = 0; i < numVertices; i++) {
            vertexOffsets[i + 1] = vertexOffsets[i] + counts[i];
        }

        // 3 - fill.
        int[] vertexOutingEdges = new int[vertexOffsets[numVertices]];
        int[] cursor = vertexOffsets.clone();
        for (HalfEdge halfEdge : halfEdges) {
            if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            if (startVertex.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            int vertexId = startVertex.getId();
            vertexOutingEdges[cursor[vertexId]++] = halfEdge.getId();
        }

        return new MapVertexAllOutingEdgesIndices(vertexOffsets, vertexOutingEdges);
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

    public static boolean decideIfCollapseCheckingFaces(HalfEdge halfEdge,
                                                        HalfEdgeSurface surface,
                                                        MapVertexAllOutingEdgesIndices mapVertexAllOutingEdgesIndices,
                                                        Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices,
                                                        double maxDiffAngDeg, double maxAspectRatio, double smallHedgeSize) {

        HalfEdgeVertex deletingVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();
        HalfEdge twin = halfEdge.getTwin();
        Vector3d collapseHedgeDirection = halfEdge.getVector(null);
        collapseHedgeDirection.normalize();

        List<HalfEdgeVertex> samePosVertices = mapVertexToSamePosVertices.get(deletingVertex);
        List<HalfEdge> outingEdgesOfSamePosVertices = new ArrayList<>();

        int samePosVertexCount = samePosVertices.size();
        for (int i = 0; i < samePosVertexCount; i++) {
            HalfEdgeVertex vertex = samePosVertices.get(i);
            //List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(vertex); // old.***
            int vertexId = vertex.getId();
            int vertexOutingEdgesCount = mapVertexAllOutingEdgesIndices.getEdgesCountOfVertex(vertexId);
            for(int j = 0; j < vertexOutingEdgesCount; j++) {
                int edgeIdx = mapVertexAllOutingEdgesIndices.getEdgeIndexOfVertex(vertexId, j);
                HalfEdge outingEdge = surface.getHalfEdges().get(edgeIdx);
                outingEdgesOfSamePosVertices.add(outingEdge);
            }
            //outingEdgesOfSamePosVertices.addAll(outingEdges); // old.***
        }

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

//            double dotButterFly = getButterFlyDotProdForHalfEdge(outingEdge);
//            if (dotButterFly < -0.7) {
//                // acos(0.7) = 45.57 deg
//
//                // calculate the angle between collapseHedgeDirection and the outingEdge.
//                Vector3d outingVector = outingEdge.getVector(null);
//                outingVector.normalize();
//                double dotBetweenHEdges = collapseHedgeDirection.dot(outingVector);
//                if (Math.abs(dotBetweenHEdges) < 0.95) { // acos(0.9) = 25.84 deg
//                    //dotButterFly = getButterFlyDotProdForHalfEdge(outingEdge);
//                    return false;
//                }
//            }

            HalfEdgeFace faceA = outingEdge.getFace();
//            if (faceA.isDegenerated())
//            {
//                continue;
//            }

            List<HalfEdgeVertex> verticesA = faceA.getVertices(null);

            // TODO
            double areaA = HalfEdgeUtils.calculateArea(verticesA.get(0), verticesA.get(1), verticesA.get(2));
//            if (areaA < 0.01) {
//                // is a small triangle, so continue
//                continue;
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
            double dotProd = Math.abs(collapseHedgeDirection.dot(normalA));
            double limitDotProd = 0.8; // 0.75 is ok, 0.9 is more restrict
            // arccos(0.9) = 25.84 deg
            // arcos(0.8) = 36.87 deg
            // arccos(0.75) = 41.41 deg
            if (dotProd > limitDotProd) {
                return false;
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
//            if (areaB < 0.01) {
//                // is a small triangle, so continue
//                continue;
//            }

            Vector3d normalB = HalfEdgeUtils.calculateNormalAsConvex(verticesB, null);

            if (normalB == null) {
                normalNullsCount++;
                continue;
            }

//            // Test**********************************************************
//            double dot = normalA.dot(normalB);
//            if (Math.abs(dot) < 0.342) {
//                return false;
//            }
//            // End test******************************************************

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
        }

        return true;
    }

    public static boolean decideIfCollapseCheckingFaces(HalfEdge halfEdge,
                                                        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap,
                                                        Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices,
                                                        double maxDiffAngDeg, double maxAspectRatio, double smallHedgeSize) {

        HalfEdgeVertex deletingVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();
        HalfEdge twin = halfEdge.getTwin();
        Vector3d collapseHedgeDirection = halfEdge.getVector(null);
        collapseHedgeDirection.normalize();

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

//            double dotButterFly = getButterFlyDotProdForHalfEdge(outingEdge);
//            if (dotButterFly < -0.7) {
//                // acos(0.7) = 45.57 deg
//
//                // calculate the angle between collapseHedgeDirection and the outingEdge.
//                Vector3d outingVector = outingEdge.getVector(null);
//                outingVector.normalize();
//                double dotBetweenHEdges = collapseHedgeDirection.dot(outingVector);
//                if (Math.abs(dotBetweenHEdges) < 0.95) { // acos(0.9) = 25.84 deg
//                    //dotButterFly = getButterFlyDotProdForHalfEdge(outingEdge);
//                    return false;
//                }
//            }

            HalfEdgeFace faceA = outingEdge.getFace();
//            if (faceA.isDegenerated())
//            {
//                continue;
//            }

            List<HalfEdgeVertex> verticesA = faceA.getVertices(null);

            // TODO
            double areaA = HalfEdgeUtils.calculateArea(verticesA.get(0), verticesA.get(1), verticesA.get(2));
//            if (areaA < 0.01) {
//                // is a small triangle, so continue
//                continue;
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
            double dotProd = Math.abs(collapseHedgeDirection.dot(normalA));
            double limitDotProd = 0.8; // 0.75 is ok, 0.9 is more restrict
            // arccos(0.9) = 25.84 deg
            // arcos(0.8) = 36.87 deg
            // arccos(0.75) = 41.41 deg
            if (dotProd > limitDotProd) {
                return false;
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
//            if (areaB < 0.01) {
//                // is a small triangle, so continue
//                continue;
//            }

            Vector3d normalB = HalfEdgeUtils.calculateNormalAsConvex(verticesB, null);

            if (normalB == null) {
                normalNullsCount++;
                continue;
            }

//            // Test**********************************************************
//            double dot = normalA.dot(normalB);
//            if (Math.abs(dot) < 0.342) {
//                return false;
//            }
//            // End test******************************************************

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
        }

        return true;
    }

    public static boolean decideIfCollapseRobust(
            HalfEdge halfEdge,
            Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap,
            Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices,
            double maxDiffAngDeg,
            double maxAspectRatio,
            double smallHedgeSize,
            double minAreaEpsilon) {

        HalfEdgeVertex vDel = halfEdge.getStartVertex();
        HalfEdgeVertex vKeep = halfEdge.getEndVertex();
        HalfEdge twin = halfEdge.getTwin();

        Vector3d collapseDir = halfEdge.getVector(null);
        if (collapseDir.lengthSquared() == 0) return false;
        collapseDir.normalize();

        // 🔥 1. Recoger edges relevantes (robusto pero acotado)
        List<HalfEdge> candidateEdges = new ArrayList<>();

        List<HalfEdgeVertex> samePosVertices = mapVertexToSamePosVertices.get(vDel);
        if (samePosVertices == null) return false;

        for (HalfEdgeVertex v : samePosVertices) {
            List<HalfEdge> edges = vertexAllOutingEdgesMap.get(v);
            if (edges == null) continue;

            for (HalfEdge e : edges) {
                if (e == null) continue;
                if (e.getStatus() == ObjectStatus.DELETED) continue;
                if (e.isDegeneratedByPointers()) continue;

                // evitar evaluar edges totalmente lejanos (filtro geométrico)
                if (e.getStartVertex().getPosition().distance(vDel.getPosition()) > smallHedgeSize * 2.0)
                    continue;

                candidateEdges.add(e);
            }
        }

        if (candidateEdges.isEmpty()) return false;

        // 🔥 Para detectar duplicados
        Set<String> triangleKeys = new HashSet<>();

        double hedgeLength = halfEdge.getLength();
        double angFactor = 1.0;
        if (hedgeLength < smallHedgeSize) {
            double t = hedgeLength / smallHedgeSize;
            angFactor = t * t; // suave
        }

        // 🔥 2. Evaluar cada cara afectada
        for (HalfEdge edge : candidateEdges) {

            if (edge == halfEdge) continue;
            if (twin != null && edge == twin.getNext()) continue;

            HalfEdgeFace faceA = edge.getFace();
            if (faceA == null) continue;

            List<HalfEdgeVertex> vertsA = faceA.getVertices(null);
            if (vertsA == null || vertsA.size() < 3) continue;

            // 🔥 Recalcular SIEMPRE (mesh imperfecto)
            Vector3d normalA = HalfEdgeUtils.calculateNormalAsConvex(vertsA, null);
            if (normalA == null) continue;
            normalA.normalize();

            double areaA = HalfEdgeUtils.calculateArea(vertsA.get(0), vertsA.get(1), vertsA.get(2));
            if (areaA < minAreaEpsilon) continue; // ignorar basura previa

            // 🔥 3. Simular colapso (A → B)
            List<HalfEdgeVertex> vertsB = new ArrayList<>(3);

            for (HalfEdgeVertex v : vertsA) {
                if (v == vDel) {
                    vertsB.add(vKeep);
                } else {
                    vertsB.add(v);
                }
            }

            // 🔥 4. Área
            double areaB = HalfEdgeUtils.calculateArea(vertsB.get(0), vertsB.get(1), vertsB.get(2));
            if (areaB < minAreaEpsilon) return false;
            if (areaB < areaA * 0.1) return false; // colapso agresivo

            // 🔥 5. Normal nueva
            Vector3d normalB = HalfEdgeUtils.calculateNormalAsConvex(vertsB, null);
            if (normalB == null) return false;
            normalB.normalize();

            // 🔥 6. Flip check (CRÍTICO)
            double dotNormals = normalA.dot(normalB);
            if (dotNormals < 0.0) return false;

            double angDeg = Math.toDegrees(Math.acos(Math.max(-1.0, Math.min(1.0, dotNormals))));

            FaceType faceType = faceA.getFaceType();
            double maxAllowed = (faceType == FaceType.SKIRT) ? maxDiffAngDeg * 0.3 : maxDiffAngDeg;

            if (angDeg * angFactor > maxAllowed) return false;

            // 🔥 7. Aspect ratio
            double aspect = HalfEdgeUtils.calculateAspectRatioAsTriangle(
                    vertsB.get(0), vertsB.get(1), vertsB.get(2));

            if (aspect > maxAspectRatio) return false;

            // 🔥 8. Dirección del edge (menos agresivo que antes)
            double dotDir = Math.abs(collapseDir.dot(normalA));
            if (dotDir > 0.95) return false;

            // 🔥 9. Detectar triángulos duplicados (MUY IMPORTANTE)
            int i0 = vertsB.get(0).hashCode();
            int i1 = vertsB.get(1).hashCode();
            int i2 = vertsB.get(2).hashCode();

            int a = Math.min(i0, Math.min(i1, i2));
            int c = Math.max(i0, Math.max(i1, i2));
            int b = i0 + i1 + i2 - a - c;

            String key = a + "_" + b + "_" + c;

            if (triangleKeys.contains(key)) return false;
            triangleKeys.add(key);
        }

        return true;
    }

    public static boolean decideIfCollapseBalanced_v2(
            HalfEdge halfEdge,
            Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap,
            Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices,
            double maxDiffAngDeg,
            double maxAspectRatio,
            double smallHedgeSize,
            double minAreaEpsilon) {

        HalfEdgeVertex vDel = halfEdge.getStartVertex();
        HalfEdgeVertex vKeep = halfEdge.getEndVertex();
        HalfEdge twin = halfEdge.getTwin();

        Vector3d collapseDir = halfEdge.getVector(null);
        if (collapseDir.lengthSquared() == 0) return false;
        collapseDir.normalize();

        double hedgeLength = halfEdge.getLength();

        // =========================================================
        // 🛡️ 1. PROTECCIÓN DE THIN STRUCTURES
        // =========================================================
        List<HalfEdge> ringEdges = vertexAllOutingEdgesMap.get(vDel);
        if (ringEdges == null || ringEdges.isEmpty()) return false;

        double avgLen = 0.0;
        int count = 0;
        for (HalfEdge e : ringEdges) {
            if (e == null || e.getStatus() == ObjectStatus.DELETED) continue;
            avgLen += e.getLength();
            count++;
        }
        if (count == 0) return false;
        avgLen /= count;

        // 👉 si el edge es grande respecto a su entorno → estructura fina
        if (hedgeLength > avgLen * 0.8) {
            return false;
        }

        // =========================================================
        // 🔥 2. RELAJACIÓN LOCAL (edges pequeños)
        // =========================================================
        double relaxFactor = 1.0;
        double angFactor = 1.0;

        if (hedgeLength < smallHedgeSize) {
            double t = hedgeLength / smallHedgeSize;
            angFactor = t * t;         // como ya hacías
            relaxFactor = 2.0;         // 🔥 clave: relajar restricciones
        }

        // =========================================================
        // 🔍 3. RECOGER EDGES RELEVANTES
        // =========================================================
        List<HalfEdge> candidateEdges = new ArrayList<>();

        List<HalfEdgeVertex> samePosVertices = mapVertexToSamePosVertices.get(vDel);
        if (samePosVertices == null) return false;

        Vector3d posDel = vDel.getPosition();

        for (HalfEdgeVertex v : samePosVertices) {
            List<HalfEdge> edges = vertexAllOutingEdgesMap.get(v);
            if (edges == null) continue;

            for (HalfEdge e : edges) {
                if (e == null) continue;
                if (e.getStatus() == ObjectStatus.DELETED) continue;
                if (e.isDegeneratedByPointers()) continue;

                // filtro geométrico
                if (e.getStartVertex().getPosition().distance(posDel) > smallHedgeSize * 2.0)
                    continue;

                candidateEdges.add(e);
            }
        }

        if (candidateEdges.isEmpty()) return false;

        // detectar duplicados
        Set<String> triangleKeys = new HashSet<>();

        // =========================================================
        // 🧠 4. VALIDACIÓN GEOMÉTRICA
        // =========================================================
        for (HalfEdge edge : candidateEdges) {

            if (edge == halfEdge) continue;
            if (twin != null && edge == twin.getNext()) continue;

            HalfEdgeFace faceA = edge.getFace();
            if (faceA == null) continue;

            List<HalfEdgeVertex> vertsA = faceA.getVertices(null);
            if (vertsA == null || vertsA.size() < 3) continue;

            // 🔥 recalcular siempre (mesh imperfecto)
            Vector3d normalA = HalfEdgeUtils.calculateNormalAsConvex(vertsA, null);
            if (normalA == null) continue;
            normalA.normalize();

            double areaA = HalfEdgeUtils.calculateArea(
                    vertsA.get(0), vertsA.get(1), vertsA.get(2));

            if (areaA < minAreaEpsilon) continue; // ignorar basura previa

            // =====================================================
            // 🔄 SIMULAR COLAPSO
            // =====================================================
            List<HalfEdgeVertex> vertsB = new ArrayList<>(3);

            for (HalfEdgeVertex v : vertsA) {
                if (v == vDel) {
                    vertsB.add(vKeep);
                } else {
                    vertsB.add(v);
                }
            }

            // =====================================================
            // 📐 ÁREA (menos agresivo)
            // =====================================================
            double areaB = HalfEdgeUtils.calculateArea(
                    vertsB.get(0), vertsB.get(1), vertsB.get(2));

            if (areaB < minAreaEpsilon) return false;

            // =====================================================
            // 🧭 NORMAL
            // =====================================================
            Vector3d normalB = HalfEdgeUtils.calculateNormalAsConvex(vertsB, null);
            if (normalB == null) return false;
            normalB.normalize();

            // 🔥 flip check (crítico)
            double dotNormals = normalA.dot(normalB);
            if (dotNormals < 0.0) return false;

            double angDeg = Math.toDegrees(
                    Math.acos(Math.max(-1.0, Math.min(1.0, dotNormals)))
            );

            FaceType faceType = faceA.getFaceType();
            double maxAllowed = (faceType == FaceType.SKIRT)
                    ? maxDiffAngDeg * 0.3
                    : maxDiffAngDeg;

            if (angDeg * angFactor > maxAllowed * relaxFactor) return false;

            // =====================================================
            // 📏 ASPECT RATIO
            // =====================================================
            double aspect = HalfEdgeUtils.calculateAspectRatioAsTriangle(
                    vertsB.get(0), vertsB.get(1), vertsB.get(2));

            if (aspect > maxAspectRatio) return false;

            // =====================================================
            // 🧱 DIRECCIÓN (menos agresivo)
            // =====================================================
            double dotDir = Math.abs(collapseDir.dot(normalA));
            if (dotDir > 0.97) return false;

            // =====================================================
            // 🔁 TRIÁNGULOS DUPLICADOS
            // =====================================================
            int i0 = vertsB.get(0).hashCode();
            int i1 = vertsB.get(1).hashCode();
            int i2 = vertsB.get(2).hashCode();

            int a = Math.min(i0, Math.min(i1, i2));
            int c = Math.max(i0, Math.max(i1, i2));
            int b = i0 + i1 + i2 - a - c;

            String key = a + "_" + b + "_" + c;

            if (triangleKeys.contains(key)) return false;
            triangleKeys.add(key);
        }

        return true;
    }

    public static boolean decideIfCollapseBalanced_v3(
            HalfEdge halfEdge,
            Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap,
            Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices,
            double maxDiffAngDeg,
            double maxAspectRatio,
            double smallHedgeSize,
            double minAreaEpsilon) {

        HalfEdgeVertex vDel = halfEdge.getStartVertex();
        HalfEdgeVertex vKeep = halfEdge.getEndVertex();
        HalfEdge twin = halfEdge.getTwin();

        Vector3d collapseDir = halfEdge.getVector(null);
        if (collapseDir.lengthSquared() == 0) return false;
        collapseDir.normalize();

        double hedgeLength = halfEdge.getLength();

        // =========================================================
        // 🔍 1. 1-RING
        // =========================================================
        List<HalfEdge> ringEdges = vertexAllOutingEdgesMap.get(vDel);
        if (ringEdges == null || ringEdges.isEmpty()) return false;

        Vector3d posDel = vDel.getPosition();

        // =========================================================
        // 🧠 2. ESTADÍSTICAS LOCALES
        // =========================================================
        double avgLen = 0.0;
        int lenCount = 0;

        Vector3d avgNormal = new Vector3d();
        int normalCount = 0;

        for (HalfEdge e : ringEdges) {
            if (e == null || e.getStatus() == ObjectStatus.DELETED) continue;

            avgLen += e.getLength();
            lenCount++;

            HalfEdgeFace f = e.getFace();
            if (f != null) {
                Vector3d n = HalfEdgeUtils.calculateNormalAsConvex(f.getVertices(null), null);
                if (n != null && n.lengthSquared() > 0) {
                    n.normalize();
                    avgNormal.add(n);
                    normalCount++;
                }
            }
        }

        if (lenCount == 0 || normalCount == 0) return false;

        avgLen /= lenCount;
        avgNormal.normalize();

        // =========================================================
        // 🌿 3. DETECCIÓN DE RUIDO (césped)
        // =========================================================
        double normalVariance = 0.0;

        for (HalfEdge e : ringEdges) {
            HalfEdgeFace f = e.getFace();
            if (f == null) continue;

            Vector3d n = HalfEdgeUtils.calculateNormalAsConvex(f.getVertices(null), null);
            if (n == null || n.lengthSquared() == 0) continue;

            n.normalize();
            normalVariance += (1.0 - avgNormal.dot(n));
        }

        normalVariance /= normalCount;

        //boolean isNoisySurface = normalVariance > 0.15;
        boolean isNoisySurface = normalVariance > 0.08;

        // =========================================================
        // 🛡️ 4. PROTECCIÓN DE THIN STRUCTURES
        // =========================================================
        if (!isNoisySurface) {
            if (hedgeLength > avgLen * 0.8) {
                return false;
            }
        }

        // =========================================================
        // 🔥 5. RELAJACIÓN LOCAL
        // =========================================================
        double relaxFactor = 1.0;
        double angFactor = 1.0;

        if (hedgeLength < smallHedgeSize) {
            double t = hedgeLength / smallHedgeSize;
            angFactor = t * t;
            relaxFactor = 2.0;
        }

        double noiseRelax = isNoisySurface ? 2.5 : 1.0;
        double aspectLimit = isNoisySurface ? maxAspectRatio * 2.0 : maxAspectRatio;
        double dotDirLimit = isNoisySurface ? 0.99 : 0.97;

        // =========================================================
        // 🔍 6. EDGES CANDIDATOS
        // =========================================================
        List<HalfEdge> candidateEdges = new ArrayList<>();

        List<HalfEdgeVertex> samePosVertices = mapVertexToSamePosVertices.get(vDel);
        if (samePosVertices == null) return false;

        for (HalfEdgeVertex v : samePosVertices) {
            List<HalfEdge> edges = vertexAllOutingEdgesMap.get(v);
            if (edges == null) continue;

            for (HalfEdge e : edges) {
                if (e == null) continue;
                if (e.getStatus() == ObjectStatus.DELETED) continue;
                if (e.isDegeneratedByPointers()) continue;

                if (e.getStartVertex().getPosition().distance(posDel) > smallHedgeSize * 2.0)
                    continue;

                candidateEdges.add(e);
            }
        }

        if (candidateEdges.isEmpty()) return false;

        Set<String> triangleKeys = new HashSet<>();

        // =========================================================
        // 🧪 7. VALIDACIÓN GEOMÉTRICA
        // =========================================================
        for (HalfEdge edge : candidateEdges) {

            if (edge == halfEdge) continue;
            if (twin != null && edge == twin.getNext()) continue;

            HalfEdgeFace faceA = edge.getFace();
            if (faceA == null) continue;

            List<HalfEdgeVertex> vertsA = faceA.getVertices(null);
            if (vertsA == null || vertsA.size() < 3) continue;

            Vector3d normalA = HalfEdgeUtils.calculateNormalAsConvex(vertsA, null);
            if (normalA == null) continue;
            normalA.normalize();

            double areaA = HalfEdgeUtils.calculateArea(
                    vertsA.get(0), vertsA.get(1), vertsA.get(2));

            if (areaA < minAreaEpsilon) continue;

            // =========================
            // SIMULACIÓN
            // =========================
            List<HalfEdgeVertex> vertsB = new ArrayList<>(3);

            for (HalfEdgeVertex v : vertsA) {
                vertsB.add(v == vDel ? vKeep : v);
            }

            double areaB = HalfEdgeUtils.calculateArea(
                    vertsB.get(0), vertsB.get(1), vertsB.get(2));

            if (areaB < minAreaEpsilon) return false;

            Vector3d normalB = HalfEdgeUtils.calculateNormalAsConvex(vertsB, null);
            if (normalB == null) return false;
            normalB.normalize();

            double dotNormals = normalA.dot(normalB);
            if (dotNormals < 0.0) return false;

            double angDeg = Math.toDegrees(
                    Math.acos(Math.max(-1.0, Math.min(1.0, dotNormals)))
            );

            FaceType faceType = faceA.getFaceType();
            double maxAllowed = (faceType == FaceType.SKIRT)
                    ? maxDiffAngDeg * 0.3
                    : maxDiffAngDeg;

            if (angDeg * angFactor > maxAllowed * relaxFactor * noiseRelax)
                return false;

            double aspect = HalfEdgeUtils.calculateAspectRatioAsTriangle(
                    vertsB.get(0), vertsB.get(1), vertsB.get(2));

            if (aspect > aspectLimit) return false;

            double dotDir = Math.abs(collapseDir.dot(normalA));
            if (dotDir > dotDirLimit) return false;

            // =========================
            // DUPLICADOS
            // =========================
            int i0 = vertsB.get(0).hashCode();
            int i1 = vertsB.get(1).hashCode();
            int i2 = vertsB.get(2).hashCode();

            int a = Math.min(i0, Math.min(i1, i2));
            int c = Math.max(i0, Math.max(i1, i2));
            int b = i0 + i1 + i2 - a - c;

            String key = a + "_" + b + "_" + c;

            if (triangleKeys.contains(key)) return false;
            triangleKeys.add(key);
        }

        return true;
    }

    public static boolean decideIfCollapseCheckingFaces_original(HalfEdge halfEdge, Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap,
                                                                 Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices, double maxDiffAngDeg, double maxAspectRatio, double smallHedgeSize) {

        HalfEdgeVertex deletingVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();
        HalfEdge twin = halfEdge.getTwin();
        Vector3d collapseHedgeDirection = halfEdge.getVector(null);
        collapseHedgeDirection.normalize();

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

//            double dotButterFly = getButterFlyDotProdForHalfEdge(outingEdge);
//            if (dotButterFly < -0.7) {
//                // acos(0.7) = 45.57 deg
//
//                // calculate the angle between collapseHedgeDirection and the outingEdge.
//                Vector3d outingVector = outingEdge.getVector(null);
//                outingVector.normalize();
//                double dotBetweenHEdges = collapseHedgeDirection.dot(outingVector);
//                if (Math.abs(dotBetweenHEdges) < 0.95) { // acos(0.9) = 25.84 deg
//                    //dotButterFly = getButterFlyDotProdForHalfEdge(outingEdge);
//                    return false;
//                }
//            }

            HalfEdgeFace faceA = outingEdge.getFace();
//            if (faceA.isDegenerated())
//            {
//                continue;
//            }

            List<HalfEdgeVertex> verticesA = faceA.getVertices(null);

            // TODO
            double areaA = HalfEdgeUtils.calculateArea(verticesA.get(0), verticesA.get(1), verticesA.get(2));
//            if (areaA < 0.01) {
//                // is a small triangle, so continue
//                continue;
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
            double dotProd = Math.abs(collapseHedgeDirection.dot(normalA));
            double limitDotProd = 0.8; // 0.75 is ok, 0.9 is more restrict
            // arccos(0.9) = 25.84 deg
            // arcos(0.8) = 36.87 deg
            // arccos(0.75) = 41.41 deg
            if (dotProd > limitDotProd) {
                return false;
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
//            if (areaB < 0.01) {
//                // is a small triangle, so continue
//                continue;
//            }

            Vector3d normalB = HalfEdgeUtils.calculateNormalAsConvex(verticesB, null);

            if (normalB == null) {
                normalNullsCount++;
                continue;
            }

//            // Test**********************************************************
//            double dot = normalA.dot(normalB);
//            if (Math.abs(dot) < 0.342) {
//                return false;
//            }
//            // End test******************************************************

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
        }

        return true;
    }

    public static boolean decideIfCollapseCheckingFacesOnlySmallTriangles(HalfEdge halfEdge, Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap,
                                                                          Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices, double maxDiffAngDeg, double maxAspectRatio, double smallHedgeSize,
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
}
