package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.modifier.topology.GaiaSceneCleaner;
import com.gaia3d.basic.geometry.modifier.topology.GaiaWeldOptions;
import com.gaia3d.basic.geometry.modifier.topology.GaiaWelder;
import com.gaia3d.basic.geometry.octree.GaiaFaceData;
import com.gaia3d.basic.geometry.octree.GaiaOctree;
import com.gaia3d.basic.geometry.octree.GaiaOctreeFaces;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.remesher.information.GaiaStatistics;
import com.gaia3d.util.GaiaOctreeUtils;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.*;

@Slf4j
@Getter
@Setter
public class GeometryOnlyReMesherByOctreeV3 {
    private int limitDepth = 12;

    // Leaf pequeño para clasificar con precisión.
    private double limitBoxSize = 0.2;

    private int minFacesCount = 4;
    //private int minVertexCount = 20;

    // Cuánto permitimos juntar dentro del parent.
    //private double parentCollapseDiameterFactor = 1.5;
    private double parentCollapseDiameterFactor = 3.5;

    private GaiaStatistics sceneStats = null;

    private static class ReMeshGroupResult {
        int parentGroups = 0;
        int childFallbacks = 0;
    }

    private enum VertexCollectMode {
        ALL_SAFE_FACES,
        ORGANIC_ONLY_FACES
    }

    public enum FaceRemeshType {
        UNKNOWN,
        AXIS_LIKE,
        ORGANIC,
        BAR,
        DISCARDABLE,
        ARCHITECTURE
    }

    public enum OctreeShapeType {
        UNKNOWN,
        BAR,
        FLOOR,
        WALL,
        VOLUME
    }

    public static class OctreeShapeInfo {
        public OctreeShapeType type = OctreeShapeType.UNKNOWN;

        public double sizeX;
        public double sizeY;
        public double sizeZ;

        public double longest;
        public double middle;
        public double shortest;

        public double elongation;
        public double flatness;

        public Vector3d averageNormal = new Vector3d();
    }

    private static class LeafFaceTypeSummary {
        int architectureCount = 0;
        int organicCount = 0;
        int barCount = 0;
        int unknownCount = 0;
        int discardableCount = 0;
        int totalCount = 0;
        int axisLikeCount = 0;

        double architectureRatio() {
            if (totalCount == 0) return 0.0;
            return (double) architectureCount / (double) totalCount;
        }

        double unknownRatio() {
            if (totalCount == 0) return 0.0;
            return (double) unknownCount / (double) totalCount;
        }

        double axisLikeRatio() {
            if (totalCount == 0) return 0.0;
            return (double) axisLikeCount / (double) totalCount;
        }
    }

    private static class LeafReMeshDecision {
        GaiaOctreeFaces leaf;

        boolean remeshable = false;
        boolean protectedArchitecture = false;
        boolean discarded = false;

        Set<GaiaVertex> verticesToReMesh = new HashSet<>();

        GaiaStatistics stats;
        OctreeShapeInfo shapeInfo;
        LeafFaceTypeSummary faceSummary;
    }

    public void reMeshScene(
            GaiaScene scene,
            GaiaStatistics sceneStatsOptional,
            GaiaBoundingBox nodeBBoxOptional
    ) {
        if (scene == null) {
            return;
        }

        if (sceneStatsOptional != null) {
            this.sceneStats = sceneStatsOptional;
        } else {
            this.sceneStats = GaiaStatistics.calculateStatistics(scene);
        }

        List<GaiaNode> nodes = scene.getNodes();

        if (nodes != null) {
            for (GaiaNode node : nodes) {
                reMeshNode(node, scene, nodeBBoxOptional);
            }
        }

        GaiaWeldOptions weldOptions = GaiaWeldOptions.builder()
                .error(1e-6)
                .checkTexCoord(false)
                .checkNormal(false)
                .checkColor(false)
                .checkBatchId(false)
                .build();

        GaiaWelder weld = new GaiaWelder(weldOptions);
        weld.apply(scene);

        GaiaSceneCleaner cleaner = new GaiaSceneCleaner();
        cleaner.apply(scene);
    }

    public void reMeshNode(
            GaiaNode node,
            GaiaScene parentScene,
            GaiaBoundingBox nodeBBoxOptional
    ) {
        if (node == null) {
            return;
        }

        List<GaiaMesh> meshes = node.getMeshes();

        if (meshes != null) {
            for (GaiaMesh mesh : meshes) {
                reMeshMesh(mesh, node, parentScene, nodeBBoxOptional);
            }
        }

        List<GaiaNode> children = node.getChildren();

        if (children != null) {
            for (GaiaNode child : children) {
                reMeshNode(child, parentScene, nodeBBoxOptional);
            }
        }
    }

    public void reMeshMesh(
            GaiaMesh mesh,
            GaiaNode parentNode,
            GaiaScene parentScene,
            GaiaBoundingBox nodeBBoxOptional
    ) {
        if (mesh == null) {
            return;
        }

        List<GaiaPrimitive> primitives = mesh.getPrimitives();

        if (primitives == null) {
            return;
        }

        for (GaiaPrimitive primitive : primitives) {
            reMeshPrimitive(
                    primitive,
                    parentNode,
                    parentScene,
                    nodeBBoxOptional
            );
        }

    }

    public void reMeshPrimitive(
            GaiaPrimitive primitive,
            GaiaNode parentNode,
            GaiaScene parentScene,
            GaiaBoundingBox nodeBBoxOptional
    ) {
        if (primitive == null || parentScene == null) {
            return;
        }

        List<GaiaVertex> vertices = primitive.getVertices();

        if (vertices == null || vertices.size() < 3) {
            return;
        }

        GaiaBoundingBox cubeBoundingBox;

        if (nodeBBoxOptional != null) {
            cubeBoundingBox = nodeBBoxOptional.clone();
        } else {
            Matrix4d mat = new Matrix4d();
            mat.identity();

            GaiaBoundingBox boundingBox = primitive.getBoundingBox(mat);
            if (boundingBox == null) {
                return;
            }

            cubeBoundingBox = boundingBox.createCubeFromMinPosition();
        }

        List<GaiaFace> primitiveFaces = new ArrayList<>();
        primitive.extractGaiaAllFaces(primitiveFaces);

        if (primitiveFaces.isEmpty()) {
            return;
        }

        int[] weldedIndices = new int[vertices.size()];

        GaiaFrontierFinder finder = new GaiaFrontierFinder();
        boolean[] frontierVertices = finder.findBoundaryVertices(
                vertices,
                primitiveFaces,
                1e-6,
                weldedIndices
        );

        GaiaOctreeFaces octreeFaces = new GaiaOctreeFaces(null, cubeBoundingBox);

        List<GaiaFaceData> faceDataList = new ArrayList<>();
        GaiaOctreeUtils.getFaceDataListOfScene(parentScene, faceDataList);

        if (faceDataList.isEmpty()) {
            return;
        }

        octreeFaces.addContents(faceDataList);
        octreeFaces.setLimitDepth(limitDepth);
        octreeFaces.setLimitSize(limitBoxSize);
        octreeFaces.setLimitFacesCount(minFacesCount);
        octreeFaces.setContentsCanBeInMultipleChildren(true);
        octreeFaces.makeTree();

        List<GaiaOctree<GaiaFaceData>> leafOctrees =
                octreeFaces.extractOctreesWithContents();

        if (leafOctrees == null || leafOctrees.isEmpty()) {
            return;
        }

        Map<GaiaOctree<GaiaFaceData>, LeafReMeshDecision> decisionsByLeaf =
                new IdentityHashMap<>();

        int classifiedLeafs = 0;
        int protectedLeafs = 0;
        int organicLeafs = 0;

        // 1) Primera fase: clasificar leafs pequeños.
        for (GaiaOctree<GaiaFaceData> octree : leafOctrees) {
            GaiaOctreeFaces leaf = (GaiaOctreeFaces) octree;
            List<GaiaFaceData> facesDates = octree.getContents();

            LeafReMeshDecision decision = decideLeafForReMesh(
                    leaf,
                    facesDates,
                    vertices,
                    frontierVertices
            );

            decisionsByLeaf.put(octree, decision);

            classifiedLeafs++;

            if (decision.protectedArchitecture) {
                protectedLeafs++;
            }

            if (decision.remeshable) {
                organicLeafs++;
            }
        }

        // 2) Segunda fase: agrupar por parent.
        Map<GaiaOctree<GaiaFaceData>, List<LeafReMeshDecision>> decisionsByParent =
                groupLeafDecisionsByParent(decisionsByLeaf);

        int remeshedParentGroups = 0;
        int remeshedChildFallbacks = 0;

        for (List<LeafReMeshDecision> childDecisions : decisionsByParent.values()) {
            ReMeshGroupResult result =
                    reMeshParentGroupOrFallbackToChildren(childDecisions);

            remeshedParentGroups += result.parentGroups;
            remeshedChildFallbacks += result.childFallbacks;
        }

        log.debug(
                "GOR V3 primitive summary: classifiedLeafs={}, organicLeafs={}, protectedLeafs={}, parentGroups={}, childFallbacks={}",
                classifiedLeafs,
                organicLeafs,
                protectedLeafs,
                remeshedParentGroups,
                remeshedChildFallbacks
        );
    }

    private Map<GaiaOctree<GaiaFaceData>, List<LeafReMeshDecision>> groupLeafDecisionsByParent(
            Map<GaiaOctree<GaiaFaceData>, LeafReMeshDecision> decisionsByLeaf
    ) {
        Map<GaiaOctree<GaiaFaceData>, List<LeafReMeshDecision>> result =
                new IdentityHashMap<>();

        if (decisionsByLeaf == null || decisionsByLeaf.isEmpty()) {
            return result;
        }

        for (Map.Entry<GaiaOctree<GaiaFaceData>, LeafReMeshDecision> entry : decisionsByLeaf.entrySet()) {
            GaiaOctree<GaiaFaceData> leaf = entry.getKey();
            LeafReMeshDecision decision = entry.getValue();

            if (leaf == null || decision == null) {
                continue;
            }

            GaiaOctree<GaiaFaceData> parent = leaf.getParent();

            if (parent == null) {
                parent = leaf;
            }

            result.computeIfAbsent(parent, k -> new ArrayList<>())
                    .add(decision);
        }

        return result;
    }

    private ReMeshGroupResult reMeshParentGroupOrFallbackToChildren(
            Collection<LeafReMeshDecision> childDecisions
    ) {
        ReMeshGroupResult result = new ReMeshGroupResult();

        if (childDecisions == null || childDecisions.isEmpty()) {
            return result;
        }

        boolean parentHasArchitecture = false;

        Set<GaiaVertex> parentVertices = new HashSet<>();
        int remeshableChildCount = 0;

        for (LeafReMeshDecision decision : childDecisions) {
            if (decision == null) {
                continue;
            }

            if (decision.protectedArchitecture) {
                parentHasArchitecture = true;
            }

            if (decision.remeshable && decision.verticesToReMesh.size() >= 2) {
                parentVertices.addAll(decision.verticesToReMesh);
                remeshableChildCount++;
            }
        }

        if (parentVertices.size() < 2) {
            return result;
        }

        double groupDiagonal = calculateBoundingDiagonal(parentVertices);

        double maxCollapseDiameter;

        if (parentHasArchitecture) {
            // Modo prudente: hay arquitectura cerca,
            // pero intentamos agrupar orgánicos si están suficientemente cerca.
            maxCollapseDiameter = limitBoxSize * 2.2;
        } else {
            // Modo más agresivo: parent limpio.
            maxCollapseDiameter = limitBoxSize * parentCollapseDiameterFactor;
        }

        log.debug(
                "V3 parent candidate: parentHasArchitecture={}, remeshableChildCount={}, vertices={}, groupDiagonal={}, maxCollapseDiameter={}",
                parentHasArchitecture,
                remeshableChildCount,
                parentVertices.size(),
                groupDiagonal,
                maxCollapseDiameter
        );

        if (groupDiagonal <= maxCollapseDiameter) {
            ReMesh(parentVertices.stream().toList());
            result.parentGroups++;
            return result;
        }

        // Fallback por child.
        for (LeafReMeshDecision decision : childDecisions) {
            if (decision == null || !decision.remeshable) {
                continue;
            }

            if (decision.verticesToReMesh.size() >= 2) {
                ReMesh(decision.verticesToReMesh.stream().toList());
                result.childFallbacks++;
            }
        }

        return result;
    }

    private LeafReMeshDecision decideLeafForReMesh(
            GaiaOctreeFaces leaf,
            List<GaiaFaceData> facesDates,
            List<GaiaVertex> vertices,
            boolean[] frontierVertices
    ) {
        LeafReMeshDecision decision = new LeafReMeshDecision();
        decision.leaf = leaf;

        if (leaf == null || facesDates == null || facesDates.size() <= 3 || vertices == null) {
            decision.discarded = true;
            return decision;
        }

        List<GaiaFace> facesOfOctree = new ArrayList<>();

        for (GaiaFaceData faceData : facesDates) {
            if (faceData != null && faceData.getFace() != null) {
                facesOfOctree.add(faceData.getFace());
            }
        }

        if (facesOfOctree.isEmpty()) {
            decision.discarded = true;
            return decision;
        }

        GaiaStatistics stats = GaiaStatistics.calculateStatistics(
                facesOfOctree,
                vertices
        );

        OctreeShapeInfo shapeInfo = classifyOctreeShape(
                leaf,
                facesDates,
                vertices
        );

        LeafFaceTypeSummary faceSummary =
                summarizeLeafFaceTypes(facesDates, vertices);

        decision.stats = stats;
        decision.shapeInfo = shapeInfo;
        decision.faceSummary = faceSummary;

        if (isFacadeCandidate(faceSummary, stats, shapeInfo)) {
            decision.protectedArchitecture = true;
            return decision;
        }

        if (hasDangerousArchitectureMix(faceSummary, stats, shapeInfo)) {
            decision.protectedArchitecture = true;
            return decision;
        }

        if (isArchitecturalFlatCandidate(shapeInfo, stats)) {
            decision.protectedArchitecture = true;
            return decision;
        }

//        if (!isWrinkledOrganicCandidate(stats)) {
//            decision.discarded = true;
//            return decision;
//        }
//
//        collectLeafVerticesToReMesh(
//                leaf,
//                facesDates,
//                vertices,
//                frontierVertices,
//                decision.verticesToReMesh
//        );

        boolean organicCandidate = isWrinkledOrganicCandidate(stats);
        boolean mixedOrganicWithAxis = isMixedOrganicWithAxisLike(
                faceSummary,
                stats,
                shapeInfo
        );

        if (!organicCandidate && !mixedOrganicWithAxis) {
            decision.discarded = true;
            return decision;
        }

        VertexCollectMode collectMode;

        if (mixedOrganicWithAxis) {
            collectMode = VertexCollectMode.ORGANIC_ONLY_FACES;
        } else {
            collectMode = VertexCollectMode.ALL_SAFE_FACES;
        }

        collectLeafVerticesToReMesh(
                leaf,
                facesDates,
                vertices,
                frontierVertices,
                decision.verticesToReMesh,
                collectMode
        );

        if (decision.verticesToReMesh.size() >= 2) {
            decision.remeshable = true;
        }

        if (decision.verticesToReMesh.size() >= 2) {
            decision.remeshable = true;
        }

        log.debug(
                "V3 leaf decision. organic={}, mixedOrganicWithAxis={}, axisRatio={}, normalVariance={}, flatness={}, collectedVertices={}",
                organicCandidate,
                mixedOrganicWithAxis,
                faceSummary.axisLikeRatio(),
                stats.normalVariance,
                shapeInfo.flatness,
                decision.verticesToReMesh.size()
        );

        return decision;
    }

    private boolean isMixedOrganicWithAxisLike(
            LeafFaceTypeSummary summary,
            GaiaStatistics stats,
            OctreeShapeInfo shapeInfo
    ) {
        if (summary == null || stats == null || shapeInfo == null) {
            return false;
        }

        double axisRatio = summary.axisLikeRatio();

        // Hay rugosidad suficiente para pensar en vegetación,
        // pero también hay muchas caras tipo pared/suelo.
        if (stats.normalVariance > 0.25 &&
                axisRatio > 0.35 &&
                shapeInfo.flatness < 4.0) {
            return true;
        }

        // Caso árbol/ruido pegado a fachada:
        // bastante axis-like, pero normalVariance alta.
        if (stats.normalVariance > 0.35 &&
                axisRatio > 0.45) {
            return true;
        }

        // Caso con plegado fuerte.
        if (stats.areaFoldRatio > 1.2 &&
                stats.normalVariance > 0.18 &&
                axisRatio > 0.30) {
            return true;
        }

        return false;
    }

    private void collectLeafVerticesToReMesh(
            GaiaOctreeFaces leaf,
            List<GaiaFaceData> facesDates,
            List<GaiaVertex> vertices,
            boolean[] frontierVertices,
            Set<GaiaVertex> result,
            VertexCollectMode collectMode
    ) {
        if (leaf == null || facesDates == null || vertices == null || result == null) {
            return;
        }

        for (GaiaFaceData faceData : facesDates) {
            if (faceData == null || faceData.getFace() == null) {
                continue;
            }

            GaiaFace face = faceData.getFace();

            FaceRemeshType faceType = classifySingleFaceForRemesh(face, vertices);

            if (collectMode == VertexCollectMode.ORGANIC_ONLY_FACES) {
                // En modo mixto, las caras axis-like NO aportan vértices.
                // Así evitamos meter fachada/suelo/tejado en el colapso.
                if (faceType == FaceRemeshType.AXIS_LIKE) {
                    continue;
                }

                if (faceType == FaceRemeshType.DISCARDABLE) {
                    continue;
                }
            }



            int[] indices = face.getIndices();

            if (indices == null) {
                continue;
            }

            for (int vertexIndex : indices) {
                if (vertexIndex < 0 || vertexIndex >= vertices.size()) {
                    continue;
                }

                if (frontierVertices != null &&
                        vertexIndex < frontierVertices.length &&
                        frontierVertices[vertexIndex]) {
                    continue;
                }

                GaiaVertex vertex = vertices.get(vertexIndex);

                if (vertex == null || vertex.getPosition() == null) {
                    continue;
                }

                if (!leaf.intersects(vertex)) {
                    continue;
                }

                result.add(vertex);
            }
        }
    }

    private FaceRemeshType classifySingleFaceForRemesh(
            GaiaFace face,
            List<GaiaVertex> vertices
    ) {
        if (face == null || vertices == null) {
            return FaceRemeshType.UNKNOWN;
        }

        int[] indices = face.getIndices();

        if (indices == null || indices.length < 3) {
            return FaceRemeshType.UNKNOWN;
        }

        int i0 = indices[0];
        int i1 = indices[1];
        int i2 = indices[2];

        if (i0 < 0 || i1 < 0 || i2 < 0 ||
                i0 >= vertices.size() ||
                i1 >= vertices.size() ||
                i2 >= vertices.size()) {
            return FaceRemeshType.UNKNOWN;
        }

        Vector3d p0 = vertices.get(i0).getPosition();
        Vector3d p1 = vertices.get(i1).getPosition();
        Vector3d p2 = vertices.get(i2).getPosition();

        if (p0 == null || p1 == null || p2 == null) {
            return FaceRemeshType.UNKNOWN;
        }

        Vector3d e1 = new Vector3d(p1).sub(p0);
        Vector3d e2 = new Vector3d(p2).sub(p0);
        Vector3d cross = e1.cross(e2, new Vector3d());

        double area2 = cross.length();

        if (area2 < 1e-12) {
            return FaceRemeshType.DISCARDABLE;
        }

        cross.normalize();

        double absZ = Math.abs(cross.z);

        boolean wallLike = absZ < 0.35;
        boolean horizontalLike = absZ > 0.75;

        if (wallLike || horizontalLike) {
            return FaceRemeshType.AXIS_LIKE;
        }

        return FaceRemeshType.UNKNOWN;
    }

    private LeafFaceTypeSummary summarizeLeafFaceTypes(
            List<GaiaFaceData> facesDates,
            List<GaiaVertex> vertices
    ) {
        LeafFaceTypeSummary summary = new LeafFaceTypeSummary();

        if (facesDates == null || vertices == null) {
            return summary;
        }

        for (GaiaFaceData faceData : facesDates) {
            if (faceData == null || faceData.getFace() == null) {
                continue;
            }

            GaiaFace face = faceData.getFace();

            FaceRemeshType type = classifySingleFaceForRemesh(face, vertices);

            summary.totalCount++;

            switch (type) {
                case AXIS_LIKE:
                    summary.axisLikeCount++;
                    break;
                case ARCHITECTURE:
                    summary.architectureCount++;
                    break;
                case ORGANIC:
                    summary.organicCount++;
                    break;
                case BAR:
                    summary.barCount++;
                    break;
                case DISCARDABLE:
                    summary.discardableCount++;
                    break;
                case UNKNOWN:
                default:
                    summary.unknownCount++;
                    break;
            }
        }

        return summary;
    }

    private boolean hasDangerousArchitectureMix(
            LeafFaceTypeSummary summary,
            GaiaStatistics stats,
            OctreeShapeInfo shapeInfo
    ) {
        if (summary == null || summary.totalCount == 0 || stats == null || shapeInfo == null) {
            return false;
        }

        double axisRatio = summary.axisLikeRatio();

        // Hoja plana con muchas normales tipo pared/suelo/techo.
        if (axisRatio > 0.45 &&
                shapeInfo.flatness > 2.5 &&
                stats.normalVariance < 0.35) {
            return true;
        }

        // Fachada/esquina con ruido moderado.
        if (axisRatio > 0.30 &&
                shapeInfo.flatness > 4.0 &&
                stats.normalVariance < 0.45) {
            return true;
        }

        // Leaf pequeño casi todo axis-like.
        if (summary.totalCount <= 20 &&
                axisRatio > 0.60 &&
                stats.normalVariance < 0.40) {
            return true;
        }

        return false;
    }

    private boolean isWrinkledOrganicCandidate(GaiaStatistics stats) {
        if (stats == null) {
            return false;
        }

        if (stats.normalVariance > 0.12) {
            return true;
        }

        if (stats.areaFoldRatio > 1.3 && stats.normalVariance > 0.08) {
            return true;
        }

        if (sceneStats != null &&
                stats.trianglesDensity > sceneStats.trianglesDensity * 0.8 &&
                stats.normalVariance > 0.08) {
            return true;
        }

        return false;
    }

    public OctreeShapeInfo classifyOctreeShape(
            GaiaOctreeFaces octFaces,
            List<GaiaFaceData> faceDataList,
            List<GaiaVertex> vertices
    ) {
        OctreeShapeInfo info = new OctreeShapeInfo();

        if (octFaces == null || faceDataList == null || faceDataList.isEmpty() || vertices == null) {
            return info;
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;

        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        Vector3d normalSum = new Vector3d();
        int normalCount = 0;
        int vertexCount = 0;

        Set<Integer> usedIndices = new HashSet<>();

        for (GaiaFaceData faceData : faceDataList) {
            if (faceData == null || faceData.getFace() == null) {
                continue;
            }

            GaiaFace face = faceData.getFace();
            int[] indices = face.getIndices();

            if (indices == null || indices.length < 3) {
                continue;
            }

            int i0 = indices[0];
            int i1 = indices[1];
            int i2 = indices[2];

            if (i0 < 0 || i1 < 0 || i2 < 0 ||
                    i0 >= vertices.size() ||
                    i1 >= vertices.size() ||
                    i2 >= vertices.size()) {
                continue;
            }

            Vector3d p0 = vertices.get(i0).getPosition();
            Vector3d p1 = vertices.get(i1).getPosition();
            Vector3d p2 = vertices.get(i2).getPosition();

            Vector3d e1 = new Vector3d(p1).sub(p0);
            Vector3d e2 = new Vector3d(p2).sub(p0);
            Vector3d n = e1.cross(e2, new Vector3d());

            double len = n.length();

            if (len > 1e-12) {
                n.div(len);
                normalSum.add(n);
                normalCount++;
            }

            for (int idx : indices) {
                if (idx < 0 || idx >= vertices.size()) {
                    continue;
                }

                if (!usedIndices.add(idx)) {
                    continue;
                }

                GaiaVertex vertex = vertices.get(idx);

                if (vertex == null || vertex.getPosition() == null) {
                    continue;
                }

                if (!octFaces.intersects(vertex)) {
                    continue;
                }

                Vector3d p = vertex.getPosition();

                minX = Math.min(minX, p.x);
                minY = Math.min(minY, p.y);
                minZ = Math.min(minZ, p.z);

                maxX = Math.max(maxX, p.x);
                maxY = Math.max(maxY, p.y);
                maxZ = Math.max(maxZ, p.z);

                vertexCount++;
            }
        }

        if (vertexCount < 3) {
            return info;
        }

        info.sizeX = maxX - minX;
        info.sizeY = maxY - minY;
        info.sizeZ = maxZ - minZ;

        double[] sizes = new double[] {
                info.sizeX,
                info.sizeY,
                info.sizeZ
        };

        Arrays.sort(sizes);

        info.shortest = Math.max(sizes[0], 1e-9);
        info.middle = Math.max(sizes[1], 1e-9);
        info.longest = Math.max(sizes[2], 1e-9);

        info.elongation = info.longest / info.middle;
        info.flatness = info.middle / info.shortest;

        if (normalCount > 0) {
            normalSum.div(normalCount);

            if (normalSum.length() > 1e-12) {
                normalSum.normalize();
            }

            info.averageNormal.set(normalSum);
        }

        boolean veryFlat = info.flatness > 5.0;
        boolean veryLong = info.elongation > 3.0;

        boolean mostlyHorizontal = Math.abs(info.averageNormal.z) > 0.75;
        boolean mostlyVertical = Math.abs(info.averageNormal.z) < 0.35;

        if (veryFlat && mostlyHorizontal && !veryLong) {
            info.type = OctreeShapeType.FLOOR;
            return info;
        }

        if (veryFlat && mostlyVertical && !veryLong) {
            info.type = OctreeShapeType.WALL;
            return info;
        }

        if (veryLong) {
            info.type = OctreeShapeType.BAR;
            return info;
        }

        info.type = OctreeShapeType.VOLUME;
        return info;
    }

    private boolean isArchitecturalFlatCandidate(OctreeShapeInfo shapeInfo, GaiaStatistics stats) {
        if (shapeInfo == null || stats == null) {
            return false;
        }

        if (shapeInfo.type == OctreeShapeType.FLOOR ||
                shapeInfo.type == OctreeShapeType.WALL) {
            return stats.normalVariance < 0.35;
        }

        if (shapeInfo.flatness > 5.0 && stats.normalVariance < 0.30) {
            return true;
        }

        return false;
    }

    public void ReMesh(List<GaiaVertex> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            return;
        }

        double avgX = 0.0;
        double avgY = 0.0;
        double avgZ = 0.0;

        double avgU = 0.0;
        double avgV = 0.0;

        int count = vertices.size();
        int texCoordCount = 0;

        for (GaiaVertex vertex : vertices) {
            if (vertex == null || vertex.getPosition() == null) {
                continue;
            }

            Vector3d position = vertex.getPosition();

            avgX += position.x;
            avgY += position.y;
            avgZ += position.z;

            if (vertex.getTexcoords() != null) {
                avgU += vertex.getTexcoords().x;
                avgV += vertex.getTexcoords().y;
                texCoordCount++;
            }
        }

        avgX /= count;
        avgY /= count;
        avgZ /= count;

        if (texCoordCount > 0) {
            avgU /= texCoordCount;
            avgV /= texCoordCount;
        }

        for (GaiaVertex vertex : vertices) {
            if (vertex == null || vertex.getPosition() == null) {
                continue;
            }

            vertex.getPosition().set(avgX, avgY, avgZ);

            if (vertex.getTexcoords() != null && texCoordCount > 0) {
                vertex.getTexcoords().set(avgU, avgV);
            }
        }
    }

    private double calculateBoundingDiagonal(Collection<GaiaVertex> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            return 0.0;
        }

        double minX = Double.POSITIVE_INFINITY;
        double minY = Double.POSITIVE_INFINITY;
        double minZ = Double.POSITIVE_INFINITY;

        double maxX = Double.NEGATIVE_INFINITY;
        double maxY = Double.NEGATIVE_INFINITY;
        double maxZ = Double.NEGATIVE_INFINITY;

        int count = 0;

        for (GaiaVertex vertex : vertices) {
            if (vertex == null || vertex.getPosition() == null) {
                continue;
            }

            Vector3d p = vertex.getPosition();

            minX = Math.min(minX, p.x);
            minY = Math.min(minY, p.y);
            minZ = Math.min(minZ, p.z);

            maxX = Math.max(maxX, p.x);
            maxY = Math.max(maxY, p.y);
            maxZ = Math.max(maxZ, p.z);

            count++;
        }

        if (count == 0) {
            return 0.0;
        }

        double dx = maxX - minX;
        double dy = maxY - minY;
        double dz = maxZ - minZ;

        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    private boolean isFacadeCandidate(
            LeafFaceTypeSummary summary,
            GaiaStatistics stats,
            OctreeShapeInfo shapeInfo
    ) {
        if (summary == null || stats == null || shapeInfo == null) {
            return false;
        }

        double axisRatio = summary.axisLikeRatio();

        if (axisRatio > 0.55 &&
                shapeInfo.flatness > 3.0 &&
                Math.abs(shapeInfo.averageNormal.z) < 0.35 &&
                stats.normalVariance < 0.35) {
            return true;
        }

        return false;
    }
}