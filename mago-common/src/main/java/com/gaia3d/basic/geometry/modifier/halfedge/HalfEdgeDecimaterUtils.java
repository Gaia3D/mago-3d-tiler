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
        for (HalfEdge outingEdge : outingEdgesOfEndVertex) {
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

    public static boolean decideIfCollapseCheckingFaces(HalfEdge halfEdge,
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

        if (twin != null) {
            deletingFaceB = twin.getFace();
        }

        List<HalfEdgeFace> facesA = new ArrayList<>();
        List<HalfEdgeFace> facesB = new ArrayList<>();

        List<HalfEdgeVertex> verticesA = new ArrayList<>();
        List<HalfEdge> memSaveEdges = new ArrayList<>();

        getFacesImplicatedWithHalfEdge(halfEdge, facesA, facesB, outgoingEdgesByVertexId);
        int facesACount = facesA.size();
        for (int i = 0; i < facesACount; i++) {
            HalfEdgeFace faceA = facesA.get(i);
            if (faceA == deletingFaceA || faceA == deletingFaceB) {
                continue;
            }
            Vector3d normalA = faceA.getNormal();
            if (normalA == null) {
                memSaveEdges.clear();
                verticesA.clear();
                verticesA = faceA.getVertices(verticesA, memSaveEdges);
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
            memSaveEdges.clear();
            verticesA.clear();
            verticesA = faceA.getVertices(verticesA, memSaveEdges);
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
}
