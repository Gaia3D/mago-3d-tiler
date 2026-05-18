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
public class GeometryOnlyReMesherByOctreeV2 {
    private int limitDepth = 12;
    private double limitBoxSize = 0.2;
    private int minVertexCount = 20;
    private int minFacesCount = 4;
    private GaiaStatistics sceneStats = null;

    public enum FaceRemeshType {
        UNKNOWN,
        AXIS_LIKE,
        ORGANIC,
        BAR,
        DISCARDABLE,
        ARCHITECTURE
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

    private Map<GaiaFace, FaceRemeshType> classifyFacesForRemesh(
            List<GaiaFaceData> faceDataList,
            List<GaiaVertex> vertices
    ) {
        Map<GaiaFace, FaceRemeshType> result = new IdentityHashMap<>();

        if (faceDataList == null || vertices == null) {
            return result;
        }

        for (GaiaFaceData faceData : faceDataList) {
            if (faceData == null || faceData.getFace() == null) {
                continue;
            }

            GaiaFace face = faceData.getFace();
            FaceRemeshType type = classifySingleFaceForRemesh(face, vertices);

            result.put(face, type);
        }

        return result;
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
        int totalLeafs = 0;
        int skipFewFaces = 0;
        int skipEmptyFaces = 0;
        int skipFewTestVertices = 0;
        int skipArchitectureMix = 0;
        int skipFlat = 0;
        int skipNotOrganic = 0;
        int skipFewVerticesToReMesh = 0;
        int remeshedLeafs = 0;

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

        List<GaiaOctree<GaiaFaceData>> octreesWithContent =
                octreeFaces.extractOctreesWithContents();

        if (octreesWithContent == null || octreesWithContent.isEmpty()) {
            return;
        }

        Set<GaiaVertex> verticesToReMesh = new HashSet<>();
        List<GaiaVertex> verticesToTestShape = new ArrayList<>();
        Set<Integer> shapeVertexIndices = new HashSet<>();

        for (GaiaOctree<GaiaFaceData> octree : octreesWithContent) {
            totalLeafs++;
            GaiaOctreeFaces octFaces = (GaiaOctreeFaces) octree;

            verticesToReMesh.clear();
            verticesToTestShape.clear();
            shapeVertexIndices.clear();

            List<GaiaFaceData> facesDates = octree.getContents();

            if (facesDates == null || facesDates.size() <= 3) {
                skipFewFaces++;
                continue;
            }

            List<GaiaFace> facesOfOctree = new ArrayList<>();

            for (GaiaFaceData faceData : facesDates) {
                if (faceData != null && faceData.getFace() != null) {
                    facesOfOctree.add(faceData.getFace());
                }
            }

            if (facesOfOctree.isEmpty()) {
                skipEmptyFaces++;
                continue;
            }

            GaiaStatistics stats = GaiaStatistics.calculateStatistics(
                    facesOfOctree,
                    vertices
            );

            for (GaiaFaceData faceData : facesDates) {
                if (faceData == null || faceData.getFace() == null) {
                    continue;
                }

                int[] indices = faceData.getFace().getIndices();

                if (indices == null) {
                    continue;
                }

                for (int vertexIndex : indices) {
                    if (vertexIndex < 0 || vertexIndex >= vertices.size()) {
                        continue;
                    }

                    GaiaVertex vertex = vertices.get(vertexIndex);

                    if (vertex == null || vertex.getPosition() == null) {
                        continue;
                    }

                    if (!octFaces.intersects(vertex)) {
                        continue;
                    }

                    if (shapeVertexIndices.add(vertexIndex)) {
                        verticesToTestShape.add(vertex);
                    }

                    if (frontierVertices != null &&
                            vertexIndex < frontierVertices.length &&
                            frontierVertices[vertexIndex]) {
                        continue;
                    }

                    verticesToReMesh.add(vertex);
                }
            }

            if (verticesToTestShape.size() < 3) {
                skipFewTestVertices++;
                continue;
            }

            OctreeShapeInfo shapeInfo = classifyOctreeShape(
                    octFaces,
                    facesDates,
                    vertices
            );

            LeafFaceTypeSummary faceSummary =
                    summarizeLeafFaceTypes(facesDates, vertices);

            log.debug(
                    "FaceSummary before mix. total={}, axisLike={}, axisRatio={}, normalVariance={}, flatness={}",
                    faceSummary.totalCount,
                    faceSummary.axisLikeCount,
                    faceSummary.axisLikeRatio(),
                    stats.normalVariance,
                    shapeInfo.flatness
            );

            if (hasDangerousArchitectureMix(faceSummary, stats, shapeInfo)) {
                skipArchitectureMix++;

                log.debug(
                        "Skip by architecture mix. total={}, axisLike={}, axisRatio={}, normalVariance={}, flatness={}, areaFoldRatio={}",
                        faceSummary.totalCount,
                        faceSummary.axisLikeCount,
                        faceSummary.axisLikeRatio(),
                        stats.normalVariance,
                        shapeInfo.flatness,
                        stats.areaFoldRatio
                );

                continue;
            }

            if (isArchitecturalFlatCandidate(shapeInfo, stats)) {
                skipFlat++;

                log.debug(
                        "Skip flat. faces={}, normalVariance={}, flatness={}, type={}, avgNormalZ={}",
                        facesDates.size(),
                        stats.normalVariance,
                        shapeInfo.flatness,
                        shapeInfo.type,
                        shapeInfo.averageNormal.z
                );

                continue;
            }

            if (!isWrinkledOrganicCandidate(stats)) {
                skipNotOrganic++;

                log.debug(
                        "Skip not organic. faces={}, normalVariance={}, areaFoldRatio={}, density={}, sceneDensity={}",
                        facesDates.size(),
                        stats.normalVariance,
                        stats.areaFoldRatio,
                        stats.trianglesDensity,
                        sceneStats == null ? -1.0 : sceneStats.trianglesDensity
                );

                continue;
            }

            if (verticesToReMesh.size() < 2) {
                skipFewVerticesToReMesh++;
                continue;
            }

            ReMesh(verticesToReMesh.stream().toList());
            remeshedLeafs++;


        }

        log.debug(
                "GOR V2 primitive summary: totalLeafs={}, remeshed={}, skipFewFaces={}, skipEmptyFaces={}, skipFewTestVertices={}, skipArchitectureMix={}, skipFlat={}, skipNotOrganic={}, skipFewVerticesToReMesh={}",
                totalLeafs,
                remeshedLeafs,
                skipFewFaces,
                skipEmptyFaces,
                skipFewTestVertices,
                skipArchitectureMix,
                skipFlat,
                skipNotOrganic,
                skipFewVerticesToReMesh
        );
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
}
