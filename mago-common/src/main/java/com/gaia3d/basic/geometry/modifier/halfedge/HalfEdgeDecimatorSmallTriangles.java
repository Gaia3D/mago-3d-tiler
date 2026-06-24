package com.gaia3d.basic.geometry.modifier.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.modifier.HalfEdgeModifier;
import com.gaia3d.basic.geometry.octree.GaiaOctree;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctreeFaces;
import com.gaia3d.basic.halfedge.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.*;

@Slf4j
public class HalfEdgeDecimatorSmallTriangles extends HalfEdgeModifier {
    private final DecimateParameters decimateParameters;

    public HalfEdgeDecimatorSmallTriangles() {
        super();
        decimateParameters = new DecimateParameters();
    }

    public HalfEdgeDecimatorSmallTriangles(DecimateParameters decimateParameters) {
        super();
        this.decimateParameters = decimateParameters;
    }

    @Override
    public void applySurface(Matrix4d productTransformMatrix, List<HalfEdgeVertex> vertices, HalfEdgeSurface surface) {
        int trianglesCount = surface.getTrianglesCount();
        log.debug("trianglesCount: {}", trianglesCount);

        if (trianglesCount > 200000) {
            applySurfaceByOctree(productTransformMatrix, vertices, surface);
        } else {
            applySurfaceDirect(productTransformMatrix, vertices, surface);
        }
    }

    protected void applySurfaceDirect(Matrix4d productTransformMatrix, List<HalfEdgeVertex> vertices, HalfEdgeSurface surface) {
        List<HalfEdgeFace> faces = surface.getFaces();
        List<HalfEdge> halfEdges = surface.getHalfEdges();

        int originalFacesCount = faces.size();
        int originalHalfEdgesCount = halfEdges.size();
        int originalVerticesCount = vertices.size();

        log.debug("halfEdgesCount = " + originalHalfEdgesCount);
        int hedgesCollapsedCount = 0;
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

        mapHalfEdgeToInitialDirection = HalfEdgeDecimaterUtils.getMapHalfEdgeToDirection(mapHalfEdgeToInitialDirection, halfEdges);

        // classify vertices and faces.
        double minArea = decimateParameters.getSmallTriangleMinArea();
        weldedFacesGroups = WeldedFacesFinder.getWeldedFacesGroups(surface, weldedFacesGroups);
        int weldedFacesGroupsCount = weldedFacesGroups.size();
        for (int i = 0; i < weldedFacesGroupsCount; i++) {
            List<HalfEdgeFace> weldedFacesGroup = weldedFacesGroups.get(i);
            for (HalfEdgeFace face : weldedFacesGroup) {
                double area = face.calculateArea();
                if (area < minArea) {
                    face.setId(10);
                } else {
                    face.setId(-1);
                }
                List<HalfEdgeVertex> faceVertices = face.getVertices(null);
                for (HalfEdgeVertex vertex : faceVertices) {
                    vertex.setClassifyId(i);
                }
            }
        }
        // end classify vertices.---

        // calculate roughness og vertices.
        //HalfEdgeDecimaterUtils.calculateVerticesRoughness(surface);
        // end calculate roughness of vertices.---

        double smallTrianglesMinSize = decimateParameters.getSmallTrianglesMinSize();

        List<HalfEdge> resultHalfEdgesSortedByLength = new ArrayList<>();
        double smallHedgeSize = decimateParameters.getSmallHedgeSize();

        while (!finished && iteration < maxIterations) {
            resultHalfEdgesSortedByLength.clear();
            resultHalfEdgesSortedByLength = HalfEdgeDecimaterUtils.getHalfEdgesSortedByLength(resultHalfEdgesSortedByLength, halfEdges);
            int halfEdgesCount = resultHalfEdgesSortedByLength.size();

            // classify halfEdges
            int hedgesCount = resultHalfEdgesSortedByLength.size();
            for (int i = 0; i < hedgesCount; i++) {
                HalfEdge halfEdge = resultHalfEdgesSortedByLength.get(i);
                halfEdge.setClassifyId(0);
            }

            // clear maps
            vertexAllOutingEdgesMap.clear();
            mapFaceToHalfEdges.clear();
            mapVertexToSamePosVertices.clear();

            vertexAllOutingEdgesMap = HalfEdgeDecimaterUtils.getMapVertexAllOutingEdges(vertexAllOutingEdgesMap, halfEdges);
            mapFaceToHalfEdges = HalfEdgeDecimaterUtils.getMapFaceToHalfEdges(mapFaceToHalfEdges, halfEdges);
            WeldingParameters weldParams = decimateParameters.getWeldingParameters();
            boolean checkTexCoord = weldParams.getCheckTexCoords();
            mapVertexToSamePosVertices = HalfEdgeDecimaterUtils.getMapVertexToSamePosVertices(mapVertexToSamePosVertices, vertices, checkTexCoord);

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

                HalfEdgeFace face = halfEdge.getFace();
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

                    if (outingEdge.getFace() != null) {
                        if (outingEdge.getFace().getFaceType() == FaceType.SKIRT) {
                            positionType = PositionType.BOUNDARY_EDGE;
                            break;
                        }
                    }
                }

                if (halfEdge.hasTwin() && positionType == PositionType.BOUNDARY_EDGE) {
                    continue;
                }

                boolean testDebug = false;

                if (halfEdge.hasTwin() && positionType == PositionType.INTERIOR) {
                    if (collapseHalfEdgeOnlySmallTriangles(halfEdge, i, vertexAllOutingEdgesMap, mapVertexToSamePosVertices,
                            maxDiffAngDeg, frontierMaxDiffAngDeg, hedgeMinLengthCurrent, maxAspectRatio, smallHedgeSize, testDebug, smallTrianglesMinSize)) {
                        hedgesCollapsedCount += 1;
                        hedgesCollapsedInOneIteration += 1;
                        collapsed = true;
                        halfEdge.setStatus(ObjectStatus.DELETED);
                    }
                }
//                else if (!halfEdge.hasTwin() && positionType == PositionType.BOUNDARY_EDGE) {
//                    if (collapseFrontierHalfEdge(halfEdge, i, vertexAllOutingEdgesMap, mapHalfEdgeToInitialDirection, mapVertexToSamePosVertices, maxDiffAngDeg, frontierMaxDiffAngDeg, hedgeMinLengthCurrent, maxAspectRatio, smallHedgeSize, testDebug)) {
//                        frontierHedgesCollapsedCount += 1;
//                        frontierHedgesCollapsedInOneIteration += 1;
//                        collapsed = true;
//                        halfEdge.setStatus(ObjectStatus.DELETED);
//                    }
//                }
            }

            if (hedgesCollapsedInOneIteration + frontierHedgesCollapsedInOneIteration < 0) {
                finished = true;
            }

            if (!collapsed) {
                finished = true;
            }

            log.debug("iteration = " + iteration + ", hedgesCollapsedInOneIteration = " + hedgesCollapsedInOneIteration);
            log.debug("iteration = " + iteration + ", frontierHedgesCollapsedInOneIteration = " + frontierHedgesCollapsedInOneIteration);

            iteration++;

            // delete objects that status is DELETED
            surface.deleteDegeneratedFaces(mapFaceToHalfEdges);
            surface.deleteNoUsedVertices();
            surface.removeDeletedObjects();
//            WeldingParameters weldParams = decimateParameters.getWeldingParameters();
//            boolean checkTexCoord = weldParams.getCheckTexCoords();
            boolean checkNormal = weldParams.getCheckNormals();
            boolean checkColor = weldParams.getCheckColors();
            boolean checkBatchId = weldParams.getCheckBatchIds();
            double error = weldParams.getPositionEpsilon();
            surface.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
        }

        log.debug("*** TOTAL HALFEDGES DELETED = " + hedgesCollapsedCount);

        int finalFacesCount = faces.size();
        int finalHalfEdgesCount = halfEdges.size();
        int finalVerticesCount = vertices.size();

        int facesCountDiff = originalFacesCount - finalFacesCount;
        int halfEdgesCountDiff = originalHalfEdgesCount - finalHalfEdgesCount;
        int verticesCountDiff = originalVerticesCount - finalVerticesCount;

        log.debug("faces % deleted = " + (facesCountDiff * 100.0) / originalFacesCount);
        log.debug("halfEdges % deleted = " + (halfEdgesCountDiff * 100.0) / originalHalfEdgesCount);
        log.debug("vertices % deleted = " + (verticesCountDiff * 100.0) / originalVerticesCount);
    }

    protected void applySurfaceByOctree(Matrix4d productTransformMatrix, List<HalfEdgeVertex> vertices, HalfEdgeSurface surface) {
        // 1rst, find possible halfEdges to remove
        // Reasons to remove a halfEdge:
        // 1. The halfEdge is very short. (small length).
        // 2. All triangles around the startVertex has a similar normal.
        //----------------------------------------------------------------
        GaiaBoundingBox boundingBox = surface.getBoundingBox();

        // classify vertices.*******************************************************
        List<List<HalfEdgeFace>> weldedFacesGroups = new ArrayList<>();
        WeldedFacesFinder.getWeldedFacesGroups(surface, weldedFacesGroups);
        //weldedFacesGroups = surface.getWeldedFacesGroups(weldedFacesGroups);
        int weldedFacesGroupsCount = weldedFacesGroups.size();
        for (int i = 0; i < weldedFacesGroupsCount; i++) {
            List<HalfEdgeFace> weldedFacesGroup = weldedFacesGroups.get(i);
            for (HalfEdgeFace face : weldedFacesGroup) {
                List<HalfEdgeVertex> faceVertices = face.getVertices(null);
                for (HalfEdgeVertex vertex : faceVertices) {
                    vertex.setClassifyId(i);
                }
            }
        }
        // end classify vertices.---------------------------------------------------

        // calculate roughness og vertices.
        //HalfEdgeDecimaterUtils.calculateVerticesRoughness(surface);
        // end calculate roughness of vertices.---

        Set<HalfEdge> leafOctreeHalfEdges = new HashSet<>();
        List<HalfEdge> faceHalfEdges = new ArrayList<>();
        boolean finished = false;
        int maxIterations = decimateParameters.getIterationsCount();
        maxIterations = 5; // test.
        int iteration = 0;
        while (!finished && iteration < maxIterations) {
            log.debug("Decimate Iteration: " + iteration);
            // make octree.***********
            log.debug("Making octree");
            HalfEdgeOctreeFaces octreeFaces = new HalfEdgeOctreeFaces(null, boundingBox);
            octreeFaces.addContents(surface.getFaces());
            octreeFaces.setLimitDepth(2);
            octreeFaces.distributeFacesToTargetDepth(2);
            List<GaiaOctree<HalfEdgeFace>> leafOctrees = octreeFaces.extractOctreesWithContents();
            // End making octree.------------------------

            int leafOctreesCount = leafOctrees.size();
            int hedgesCollapsedInIteration = 0;
            for (int i = 0; i < leafOctreesCount; i++) {
                leafOctreeHalfEdges.clear();
                GaiaOctree<HalfEdgeFace> leafOctree = leafOctrees.get(i);
                int halfEdgeFacesCount = leafOctree.getContentsLength();
                for (int j = 0; j < halfEdgeFacesCount; j++) {
                    HalfEdgeFace face = leafOctree.getContents().get(j);
                    if (face.getStatus() == ObjectStatus.DELETED) {
                        continue;
                    }
                    faceHalfEdges.clear();
                    faceHalfEdges = face.getHalfEdgesLoop(faceHalfEdges);
                    for (HalfEdge halfEdge : faceHalfEdges) {
                        if (halfEdge.getStatus() == ObjectStatus.DELETED) {
                            continue;
                        }
                        leafOctreeHalfEdges.add(halfEdge);
                    }
                }

                if (leafOctreeHalfEdges.size() == 0) {
                    continue;
                }

                List<HalfEdge> halfEdges = leafOctreeHalfEdges.stream().toList();
                Set<HalfEdgeVertex> halfEdgeVertex = new HashSet<>();
                for (HalfEdge halfEdge : halfEdges) {
                    halfEdgeVertex.add(halfEdge.getStartVertex());
                }

                log.debug("Decimating:" + i + " / " + leafOctreesCount);
                int hedgesCollapsedCount = decimateSurface(productTransformMatrix, halfEdgeVertex.stream().toList(), surface, halfEdges);
                hedgesCollapsedInIteration += hedgesCollapsedCount;
            }

            // delete objects that status is DELETED
            log.debug("Finished Iteration: " + iteration);
            log.debug("Deleting degenerated faces");
            surface.deleteDegeneratedFaces(null);

            log.debug("Deleting no used vertices");
            surface.deleteNoUsedVertices();

            log.debug("Removing deleted objects");
            surface.removeDeletedObjects();
            WeldingParameters weldParams = decimateParameters.getWeldingParameters();
            boolean checkTexCoord = weldParams.getCheckTexCoords();
            boolean checkNormal = weldParams.getCheckNormals();
            boolean checkColor = weldParams.getCheckColors();
            boolean checkBatchId = weldParams.getCheckBatchIds();
            double error = weldParams.getPositionEpsilon();
            log.debug("Welding vertices");
            surface.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);

            log.info("Total edges collapsed: " + hedgesCollapsedInIteration + " iteration: " + iteration);

            if (hedgesCollapsedInIteration == 0) {
                finished = true;
            }

            iteration++;
        }

    }

    protected int decimateSurface(Matrix4d productTransformMatrix,
                                  List<HalfEdgeVertex> vertices,
                                  HalfEdgeSurface surface,
                                  List<HalfEdge> halfEdgesToDecimate) {
        // 1rst, find possible halfEdges to remove
        // Reasons to remove a halfEdge:
        // 1. The halfEdge is very short. (small length).
        // 2. All triangles around the startVertex has a similar normal.
        //----------------------------------------------------------------
        List<HalfEdgeFace> faces = surface.getFaces();
        List<HalfEdge> halfEdges = halfEdgesToDecimate; // only decimate the list.

        int originalFacesCount = faces.size();
        int originalHalfEdgesCount = halfEdges.size();
        int originalVerticesCount = vertices.size();

        boolean finished = false;

        log.debug("halfEdgesCount = " + originalHalfEdgesCount);
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

        int iteration = 0;

        Map<HalfEdge, Vector3d> mapHalfEdgeToInitialDirection = new HashMap<>();
        Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap = new HashMap<>();
        Map<HalfEdgeFace, List<HalfEdge>> mapFaceToHalfEdges = new HashMap<>();
        Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices = new HashMap<>();

        mapHalfEdgeToInitialDirection = HalfEdgeDecimaterUtils.getMapHalfEdgeToDirection(mapHalfEdgeToInitialDirection, halfEdges);

        List<HalfEdge> resultHalfEdgesSortedByLength = new ArrayList<>();
        double smallHedgeSize = decimateParameters.getSmallHedgeSize();

        resultHalfEdgesSortedByLength.clear();
        resultHalfEdgesSortedByLength = HalfEdgeDecimaterUtils.getHalfEdgesSortedByLength(resultHalfEdgesSortedByLength, halfEdges);
        int halfEdgesCount = resultHalfEdgesSortedByLength.size();

        // classify halfEdges
        int hedgesCount = resultHalfEdgesSortedByLength.size();
        for (int i = 0; i < hedgesCount; i++) {
            HalfEdge halfEdge = resultHalfEdgesSortedByLength.get(i);
            halfEdge.setClassifyId(0);
        }

        // clear maps
        vertexAllOutingEdgesMap.clear();
        mapFaceToHalfEdges.clear();
        mapVertexToSamePosVertices.clear();

        vertexAllOutingEdgesMap = HalfEdgeDecimaterUtils.getMapVertexAllOutingEdges(vertexAllOutingEdgesMap, vertices, surface);
        mapVertexToSamePosVertices = HalfEdgeDecimaterUtils.getMapVertexToSamePosVertices(mapVertexToSamePosVertices, vertices, false);

        boolean collapsed = false;
        hedgesCollapsedInOneIteration = 0;
        frontierHedgesCollapsedInOneIteration = 0;

        double smallTrianglesMinSize = decimateParameters.getSmallTrianglesMinSize();
        boolean testDebug = false;

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

                if (outingEdge.getFace() != null) {
                    if (outingEdge.getFace().getFaceType() == FaceType.SKIRT) {
                        positionType = PositionType.BOUNDARY_EDGE;
                        break;
                    }
                }
            }

            if (halfEdge.hasTwin() && positionType == PositionType.BOUNDARY_EDGE) {
                continue;
            }

            if (halfEdge.hasTwin() && positionType == PositionType.INTERIOR) {
                if (collapseHalfEdgeOnlySmallTriangles(halfEdge, i, vertexAllOutingEdgesMap, mapVertexToSamePosVertices,
                        maxDiffAngDeg, frontierMaxDiffAngDeg, hedgeMinLengthCurrent, maxAspectRatio, smallHedgeSize, testDebug, smallTrianglesMinSize)) {
                    hedgesCollapsedCount += 1;
                    hedgesCollapsedInOneIteration += 1;
                    collapsed = true;
                    halfEdge.setStatus(ObjectStatus.DELETED);
                }
            }
        }

        if (hedgesCollapsedInOneIteration + frontierHedgesCollapsedInOneIteration < 0) {
            finished = true;
        }

        if (!collapsed) {
            finished = true;
        }

        log.debug("iteration = " + iteration + ", hedgesCollapsedInOneIteration = " + hedgesCollapsedInOneIteration);
        log.debug("iteration = " + iteration + ", frontierHedgesCollapsedInOneIteration = " + frontierHedgesCollapsedInOneIteration);

        iteration++;

        //}
        log.debug("*** TOTAL HALFEDGES DELETED = " + hedgesCollapsedCount);

//        int finalFacesCount = faces.size();
//        int finalHalfEdgesCount = halfEdges.size();
//        int finalVerticesCount = vertices.size();
//
//        int facesCountDiff = originalFacesCount - finalFacesCount;
//        int halfEdgesCountDiff = originalHalfEdgesCount - finalHalfEdgesCount;
//        int verticesCountDiff = originalVerticesCount - finalVerticesCount;
//
//        log.debug("faces % deleted = " + (facesCountDiff * 100.0) / originalFacesCount);
//        log.debug("halfEdges % deleted = " + (halfEdgesCountDiff * 100.0) / originalHalfEdgesCount);
//        log.debug("vertices % deleted = " + (verticesCountDiff * 100.0) / originalVerticesCount);

        return hedgesCollapsedCount + frontierHedgesCollapsedCount;
    }

    public boolean collapseHalfEdgeOnlySmallTriangles(HalfEdge halfEdge,
                                                      int iteration,
                                                      Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap,
                                                      Map<HalfEdgeVertex, List<HalfEdgeVertex>> mapVertexToSamePosVertices,
                                                      double maxDiffAngDeg,
                                                      double frontierMaxDiffAngDeg,
                                                      double hedgeMinLength,
                                                      double maxAspectRatio,
                                                      double smallHedgeSize,
                                                      boolean testDebug,
                                                      double smallTriangleMinSize) {
        // When collapse a halfEdge, we delete the face, the twin's face, the twin & the startVertex
        // When deleting a face, must delete all halfEdges of the face
        // must find all halfEdges that startVertex is the deletingVertex, and set as startVertex the endVertex of the deletingHalfEdge

        HalfEdgeVertex startVertex = halfEdge.getStartVertex();
        HalfEdgeVertex endVertex = halfEdge.getEndVertex();

//        Rugosidad	Significado
//        0.0 – 0.03	plano
//        0.03 – 0.1	suave
//        0.1 – 0.2	irregular
//        > 0.2	rugoso (césped)
        double roughness = startVertex.getRoughness();

        if (halfEdge.getLength() > hedgeMinLength) {
            //if(roughness < 1.5) {
            if (!HalfEdgeDecimaterUtils.decideIfCollapseCheckingFacesOnlySmallTriangles(halfEdge, vertexAllOutingEdgesMap, mapVertexToSamePosVertices, maxDiffAngDeg, maxAspectRatio, smallHedgeSize, smallTriangleMinSize)) {
                return false;
            }
            //}
        }
        // end check if collapse

        int endVertexClassifyId = endVertex.getClassifyId();

        boolean isCollapsed = false;

        List<HalfEdge> outingEdgesOfEndVertex = vertexAllOutingEdgesMap.get(endVertex);
        List<HalfEdgeVertex> listVertexSamePosition = mapVertexToSamePosVertices.get(startVertex);

        if (listVertexSamePosition == null) {
            log.error("[ERROR] HalfEdgeSurface.collapseHalfEdge() : listVertexSamePosition == null.");
            return false;
        }

        List<HalfEdge> outingEdgesOfVertex = null;

        int samePositionVerticesCount = listVertexSamePosition.size();
        for (int i = 0; i < samePositionVerticesCount; i++) {
            HalfEdgeVertex vertex = listVertexSamePosition.get(i);
            outingEdgesOfVertex = vertexAllOutingEdgesMap.get(vertex);
            if (outingEdgesOfVertex == null) {
                log.error("[ERROR] HalfEdgeSurface.collapseHalfEdge() : outingEdgesOfVertex == null.");
                continue;
            }

            int outingEdgesOfVertexCount = outingEdgesOfVertex.size();
            // do not use the iterator because the list is modified.
            for (int gg = 0; gg < outingEdgesOfVertexCount; gg++) {
                HalfEdge outingEdge = outingEdgesOfVertex.get(gg);
                if (outingEdge == null) {
                    log.error("[ERROR] HalfEdgeSurface.collapseHalfEdge() : outingEdge == null.");
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
                    // must find another endVertex that has the same classifyId
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
        }

        return isCollapsed;
    }
}
