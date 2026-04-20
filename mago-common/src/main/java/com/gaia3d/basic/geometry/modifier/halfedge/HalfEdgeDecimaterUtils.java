package com.gaia3d.basic.geometry.modifier.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.octree.GaiaOctree;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctreeVertices;
import com.gaia3d.basic.halfedge.*;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
            Map<HalfEdgeVertex, List<HalfEdge>> resultVertexAllOutingEdgesMap, List<HalfEdge> halfEdges) {

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

    public static boolean decideIfCollapseCheckingFaces(HalfEdge halfEdge, Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap,
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
        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap = getMapVertexAllOutingEdges(null, halfEdgeSurface.getHalfEdges());
        for (HalfEdgeVertex vertex : vertices) {
            List<HalfEdge> outingEdges = vertexAllOutingEdgesMap.get(vertex);
            //List<Vector3d> normals = new ArrayList<>();
            Vector3d weightedNormalSum = new Vector3d(0, 0, 0);
            double totalArea = 0.0;
            for (HalfEdge outingEdge : outingEdges) {
                if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }
                HalfEdgeFace face = outingEdge.getFace();
                Vector3d normal = face.getNormal();
                if (normal == null) {
                    normal = face.calculatePlaneNormal();
                }

                if (normal != null) {
                    double area = face.calculateArea();
                    totalArea += area;
                    normal.x *= area;
                    normal.y *= area;
                    normal.z *= area;
                    //normals.add(normal);
                    weightedNormalSum.add(normal);
                }
            }

            weightedNormalSum.normalize();
            Vector3d avgNormal = weightedNormalSum;

            // calculate roughness of the vertex by the normals of the outing edges
            double roughnessSum = 0.0;
            for (HalfEdge outingEdge : outingEdges) {
                if (outingEdge.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }
                HalfEdgeFace face = outingEdge.getFace();
                double area = face.calculateArea();
                Vector3d normal = face.getNormal();
                double dot = normal.dot(avgNormal);

                // clamp para evitar NaN por errores numéricos
                dot = Math.max(-1.0, Math.min(1.0, dot));

                double angle = Math.acos(dot);

                roughnessSum += area * angle;
            }

            double roughness = roughnessSum / totalArea;
            vertex.setRoughness(roughness);
        }
    }
}
