package com.gaia3d.modifier.billboard;

import com.gaia3d.basic.geometry.entities.GaiaPlane;
import com.gaia3d.basic.geometry.entities.GaiaTriangle;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.modifier.billboard.merge.BillboardPlanePostMerger;
import com.gaia3d.modifier.billboard.merge.MergeConfig;
import com.gaia3d.modifier.billboard.render.OrthographicProjection;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.*;

/**
 * 개선 포인트
 * 1. 인접 face 기반 BFS 클러스터링
 * 2. area-weighted plane refinement
 * 3. plane이 너무 크면 2D plane space에서 분할
 * 4. primitive 단위 renderer init / cleanup
 */
@Slf4j
public class ImprovedBillboardCloudCreator extends AbstractBillboardCloudCreator {

    private static final double EPSILON = 1e-9;

    public ImprovedBillboardCloudCreator() {
        super();
    }

    public ImprovedBillboardCloudCreator(BillboardCloudOptions options) {
        super(options);
    }

    @Override
    public GaiaScene create(GaiaScene scene) {
        List<GaiaNode> leafNodes = extractor.extractAllNodes(scene, true);

        GaiaScene resultScene = createDefaultScene();
        GaiaNode rootNode = resultScene.getRootNode();

        int nodeIndex = 0;
        for (GaiaNode node : leafNodes) {
            log.info("[{}/{}] Processing node: {}", ++nodeIndex, leafNodes.size(), node.getName());

            /*GaiaNode billboardNode = new GaiaNode();
            billboardNode.setName(node.getName() + "_billboard");
            rootNode.getChildren().add(billboardNode);

            GaiaMesh billboardMesh = new GaiaMesh();
            billboardNode.getMeshes().add(billboardMesh);*/

            List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(node);
            log.info("Extracted {} primitives from node: {}", primitives.size(), node.getName());

            int primitiveIndex = 0;
            for (GaiaPrimitive primitive : primitives) {
                /*if (primitiveIndex > 0) {
                    continue;
                }*/

                /*GaiaNode billboardNode = new GaiaNode();
                billboardNode.setName(node.getName() + "_billboard" + primitiveIndex++);
                rootNode.getChildren().add(billboardNode);

                GaiaMesh billboardMesh = new GaiaMesh();
                billboardNode.getMeshes().add(billboardMesh);*/

                GaiaMaterial originalMaterial = scene.getMaterials().get(primitive.getMaterialIndex());
                log.info("  Original material for primitive: {}", originalMaterial != null ? originalMaterial.getName() : "null");
                List<BillboardPlane> billboardPlanes = generateImprovedBillboardPlanes(primitive);
                log.info("  Created {} improved billboard planes from primitive", billboardPlanes.size());
                if (billboardPlanes.isEmpty()) {
                    log.warn("No improved billboard planes found.");
                    continue;
                }

                log.info("  Merge billboard planes to reduce draw calls");
                BillboardPlanePostMerger merger = new BillboardPlanePostMerger(options.getMergeConfig());
                List<BillboardPlane> mergedPlanes = merger.merge(billboardPlanes);
                mergedPlanes = enforcePlaneBudget(mergedPlanes);
                log.info("  Reduced to {} merged billboard planes after post-merging", mergedPlanes.size());

                log.info("  Building billboard primitives for merged planes");
                List<GaiaPrimitive> newPrimitives = buildBillboardPrimitives(resultScene, mergedPlanes, primitive.getVertices(), originalMaterial);
                //List<GaiaPrimitive> newPrimitives = buildBillboardPrimitives(resultScene, billboardPlanes, primitive.getVertices(), originalMaterial);
                log.info("  Built {} new billboard primitives for node: {}", newPrimitives.size(), node.getName());
                //billboardMesh.getPrimitives().addAll(newPrimitives);

                GaiaNode billboardNode = new GaiaNode();
                billboardNode.setName(node.getName() + "_billboard" + primitiveIndex++);
                rootNode.getChildren().add(billboardNode);

                GaiaMesh billboardMesh = new GaiaMesh();
                billboardNode.getMeshes().add(billboardMesh);
                billboardMesh.getPrimitives().addAll(newPrimitives);
            }
        }

        if (!resultScene.getMaterials().isEmpty()) {
            atlasTextures(resultScene);
            batchPrimitives(resultScene);
        }

        return resultScene;
    }

    private List<BillboardPlane> enforcePlaneBudget(List<BillboardPlane> planes) {
        int targetCount = options.getMaxBillboardPlaneCount();
        if (targetCount <= 0 || planes.size() <= targetCount) {
            return planes;
        }

        int maxPasses = Math.max(0, options.getPlaneBudgetMergePasses());
        List<BillboardPlane> current = planes;
        for (int pass = 1; pass <= maxPasses && current.size() > targetCount; pass++) {
            MergeConfig relaxedConfig = createBudgetMergeConfig(options.getMergeConfig(), pass, maxPasses);
            List<BillboardPlane> merged = new BillboardPlanePostMerger(relaxedConfig).merge(current);

            log.info("  Plane budget merge pass {}/{}: {} -> {} planes (target={})",
                    pass, maxPasses, current.size(), merged.size(), targetCount);

            if (merged.size() < current.size()) {
                current = merged;
            }
        }

        if (current.size() > targetCount) {
            log.warn("  Plane budget target was not reached. current={}, target={}", current.size(), targetCount);
        }
        return current;
    }

    private MergeConfig createBudgetMergeConfig(MergeConfig base, int pass, int maxPasses) {
        MergeConfig config = base.copy();
        double t = maxPasses <= 0 ? 1.0 : (double) pass / (double) maxPasses;

        config.minNormalDot = lerp(base.minNormalDot, Math.cos(Math.toRadians(70.0)), t);
        config.maxPlaneDistance = base.maxPlaneDistance * lerp(1.0, 4.0, t);
        config.maxThickness = base.maxThickness * lerp(1.0, 6.0, t);
        config.maxCenterDistance = base.maxCenterDistance * lerp(1.0, 5.0, t);
        config.maxRectGap = base.maxRectGap * lerp(1.0, 6.0, t);
        config.minEfficiency = Math.max(0.01, base.minEfficiency * lerp(1.0, 0.15, t));
        config.gridCellSize = Math.max(config.maxCenterDistance, base.gridCellSize);
        config.prepare();
        return config;
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * Math.max(0.0, Math.min(1.0, t));
    }

    private void batchPrimitives(GaiaScene resultScene) {
        int primitiveCount = extractor.extractAllPrimitives(resultScene).size();
        if (primitiveCount <= 1) {
            return;
        }

        log.info("Batching {} billboard primitives into a single mesh", primitiveCount);
        resultScene.joinAllSurfaces();
        for (GaiaPrimitive primitive : extractor.extractAllPrimitives(resultScene)) {
            primitive.setMaterialIndex(0);
        }
        resultScene.updateBoundingBox();
    }

    private List<GaiaPrimitive> buildBillboardPrimitives(GaiaScene scene, List<BillboardPlane> billboardPlanes, List<GaiaVertex> sourceVertices, GaiaMaterial originalMaterial) {
        BufferedImage diffuseImage = resolveDiffuseImage(originalMaterial);

        List<GaiaPrimitive> primitives = new ArrayList<>();
        List<GaiaMaterial> materials = scene.getMaterials();

        renderer.init();
        try {
            int primitiveIndex = 0;
            for (BillboardPlane billboardPlane : billboardPlanes) {
                log.info(" - [{}/{}] Building improved billboard primitive", ++primitiveIndex, billboardPlanes.size());

                GaiaPrimitive targetPrimitive = new GaiaPrimitive();
                primitives.add(targetPrimitive);

                GaiaSurface surface = new GaiaSurface();
                targetPrimitive.getSurfaces().add(surface);

                GaiaMaterial material = new GaiaMaterial();
                int materialIndex = materials.size();
                String materialName = "improved_billboard_material_" + materialIndex;

                material.setId(materialIndex);
                material.setName(materialName);
                materials.add(material);
                if (options.isBlendTexture()) {
                    material.setBlend(true);
                    material.setOpaque(false);
                } else {
                    material.setBlend(false);
                    material.setOpaque(true);
                }

                targetPrimitive.setMaterialIndex(materialIndex);

                OrthographicProjection projection = createBillboardQuad(billboardPlane, sourceVertices, targetPrimitive.getVertices(), surface);

                if (projection == null) {
                    primitives.remove(targetPrimitive);
                    materials.remove(material);
                    continue;
                }

                renderer.bakeBillboardPlane(billboardPlane, sourceVertices, projection, diffuseImage);

                String parentPath = options.getTempPath();
                String textureName = materialName + ".png";
                String texturePath = parentPath + File.separator + textureName;
                renderer.saveCurrentFboToPng(texturePath);

                GaiaTexture texture = new GaiaTexture();
                texture.setName(textureName);
                texture.setParentPath(parentPath);
                texture.setPath(textureName);
                texture.loadImage();

                material.getTextures().computeIfAbsent(TextureType.DIFFUSE, k -> new ArrayList<>()).add(texture);
            }
        } finally {
            renderer.cleanup();
        }

        return primitives;
    }

    private BufferedImage resolveDiffuseImage(GaiaMaterial originalMaterial) {
        if (originalMaterial == null || originalMaterial.getTextures() == null) {
            return null;
        }

        List<GaiaTexture> diffuseTextures = originalMaterial.getTextures().getOrDefault(TextureType.DIFFUSE, Collections.emptyList());

        if (diffuseTextures.isEmpty()) {
            return null;
        }

        GaiaTexture diffuse = diffuseTextures.getFirst();
        diffuse.loadImage();
        return diffuse.getBufferedImage();
    }

    private List<BillboardPlane> generateImprovedBillboardPlanes(GaiaPrimitive primitive) {
        List<GaiaVertex> vertices = primitive.getVertices();
        List<GaiaFace> faces = extractor.extractAllFaces(primitive);

        List<FaceData> faceDataList = buildFaceDataList(faces, vertices);
        if (faceDataList.isEmpty()) {
            return Collections.emptyList();
        }

        log.info("      Built face data list with {} valid faces", faceDataList.size());
        List<List<FaceData>> clusters = clusterFaces(faceDataList);
        log.info("      Cluster face data list with {} valid clusters", clusters.size());
        List<BillboardPlane> result = new ArrayList<>();

        for (List<FaceData> cluster : clusters) {
            if (cluster.size() < options.getMinClusterFaceCount()) {
                continue;
            }

            BillboardPlane plane = createRefinedBillboardPlane(cluster, vertices);
            if (plane == null) {
                continue;
            }

            computePlaneOBB(plane, vertices);

            List<BillboardPlane> splitPlanes = splitPlaneIfNeeded(plane, vertices, 0);
            result.addAll(splitPlanes);
        }

        return result;
    }

    private List<FaceData> buildFaceDataList(List<GaiaFace> faces, List<GaiaVertex> vertices) {
        List<FaceData> result = new ArrayList<>(faces.size());

        for (int i = 0; i < faces.size(); i++) {
            GaiaFace face = faces.get(i);
            List<GaiaVertex> faceVertices = getVerticesFromFace(face, vertices);
            if (faceVertices.size() < 3) {
                continue;
            }

            GaiaTriangle triangle = new GaiaTriangle(faceVertices.get(0).getPosition(), faceVertices.get(1).getPosition(), faceVertices.get(2).getPosition());

            double area = triangle.area();
            if (area < options.getMinTriangleArea()) {
                continue;
            }

            GaiaPlane plane = triangle.getPlane();
            Vector3d normal = safeNormalize(new Vector3d(plane.getNormal()));
            Vector3d centroid = calculateCentroid(faceVertices);

            if (normal == null || centroid == null) {
                continue;
            }

            Set<Integer> vertexIndexSet = new HashSet<>();
            for (int index : face.getIndices()) {
                vertexIndexSet.add(index);
            }

            FaceData data = new FaceData();
            data.faceIndex = i;
            data.face = face;
            data.faceVertices = faceVertices;
            data.triangle = triangle;
            data.plane = plane;
            data.normal = normal;
            data.centroid = centroid;
            data.area = area;
            data.vertexIndexSet = vertexIndexSet;

            result.add(data);
        }

        return result;
    }

    /**
     * 기존 방식보다 나은 점:
     * - 전체 faces를 seed 기준으로 매번 훑지 않음
     * - 공유 vertex가 있는 인접 face 중심으로 BFS 확장
     * - normal / plane distance / centroid distance 조건 유지
     */
    private List<List<FaceData>> clusterFaces(List<FaceData> faceDataList) {
        Map<Integer, Set<Integer>> adjacency = buildAdjacency(faceDataList);
        boolean[] visited = new boolean[faceDataList.size()];

        List<List<FaceData>> clusters = new ArrayList<>();

        for (int i = 0; i < faceDataList.size(); i++) {
            if (visited[i]) {
                continue;
            }

            FaceData seed = faceDataList.get(i);
            List<FaceData> cluster = new ArrayList<>();
            Queue<Integer> queue = new ArrayDeque<>();

            visited[i] = true;
            queue.add(i);

            while (!queue.isEmpty()) {
                int currentIndex = queue.poll();
                FaceData current = faceDataList.get(currentIndex);
                cluster.add(current);

                Set<Integer> neighbors = adjacency.getOrDefault(currentIndex, Collections.emptySet());
                for (int neighborIndex : neighbors) {
                    if (visited[neighborIndex]) {
                        continue;
                    }

                    FaceData candidate = faceDataList.get(neighborIndex);
                    if (!isClusterCompatible(seed, cluster, candidate)) {
                        continue;
                    }

                    visited[neighborIndex] = true;
                    queue.add(neighborIndex);
                }
            }

            clusters.add(cluster);
        }

        return clusters;
    }

    /*private Map<Integer, List<Integer>> buildAdjacency(List<FaceData> faceDataList) {
        Map<Integer, List<Integer>> adjacency = new HashMap<>();

        for (int i = 0; i < faceDataList.size(); i++) {
            adjacency.put(i, new ArrayList<>());
        }

        for (int i = 0; i < faceDataList.size(); i++) {
            for (int j = i + 1; j < faceDataList.size(); j++) {
                if (sharesVertex(faceDataList.get(i), faceDataList.get(j))) {
                    adjacency.get(i).add(j);
                    adjacency.get(j).add(i);
                }
            }
        }

        return adjacency;
    }*/

    private Map<Integer, Set<Integer>> buildAdjacency(List<FaceData> faceDataList) {
        Map<Integer, Set<Integer>> adjacency = new HashMap<>();
        Map<Integer, List<Integer>> vertexToFaces = new HashMap<>();

        for (int i = 0; i < faceDataList.size(); i++) {
            adjacency.put(i, new HashSet<>());

            for (Integer vertexIndex : faceDataList.get(i).vertexIndexSet) {
                vertexToFaces.computeIfAbsent(vertexIndex, k -> new ArrayList<>()).add(i);
            }
        }

        for (List<Integer> connectedFaces : vertexToFaces.values()) {
            int size = connectedFaces.size();
            for (int i = 0; i < size; i++) {
                int faceA = connectedFaces.get(i);
                for (int j = i + 1; j < size; j++) {
                    int faceB = connectedFaces.get(j);

                    adjacency.get(faceA).add(faceB);
                    adjacency.get(faceB).add(faceA);
                }
            }
        }

        return adjacency;
    }

    private boolean sharesVertex(FaceData a, FaceData b) {
        for (Integer index : a.vertexIndexSet) {
            if (b.vertexIndexSet.contains(index)) {
                return true;
            }
        }
        return false;
    }

    private boolean isClusterCompatible(FaceData seed, List<FaceData> currentCluster, FaceData candidate) {
        double normalDot = Math.abs(seed.normal.dot(candidate.normal));
        if (normalDot < options.getNormalDotThreshold()) {
            return false;
        }

        double planeDistance = Math.abs(seed.plane.distanceToPoint(candidate.centroid));
        if (planeDistance > options.getPlaneDistanceEpsilon()) {
            return false;
        }

        Vector3d clusterCentroid = calculateAreaWeightedCentroid(currentCluster);
        double centroidDistance = clusterCentroid.distance(candidate.centroid);
        return !(centroidDistance > options.getSetRadius());
    }

    private BillboardPlane createRefinedBillboardPlane(List<FaceData> cluster, List<GaiaVertex> sourceVertices) {
        if (cluster == null || cluster.isEmpty()) {
            return null;
        }

        Vector3d refinedNormal = calculateAreaWeightedNormal(cluster);
        if (refinedNormal == null || refinedNormal.lengthSquared() < EPSILON) {
            return null;
        }

        Vector3d refinedCenter = calculateAreaWeightedCentroid(cluster);
        if (refinedCenter == null) {
            return null;
        }

        BillboardPlane plane = new BillboardPlane();
        plane.setNormal(refinedNormal);
        plane.setCenter(refinedCenter);
        plane.setPlane(new GaiaPlane(refinedCenter, refinedNormal));

        List<GaiaFace> groupedFaces = new ArrayList<>(cluster.size());
        for (FaceData data : cluster) {
            groupedFaces.add(data.face);
        }
        plane.setFaces(groupedFaces);

        if (options.isRefineBillboardPlane()) {
            refineBillboardPlaneAreaWeighted(plane, sourceVertices);
        }

        return plane;
    }

    private void refineBillboardPlaneAreaWeighted(BillboardPlane billboardPlane, List<GaiaVertex> sourceVertices) {
        List<GaiaFace> faces = billboardPlane.getFaces();
        if (faces == null || faces.isEmpty()) {
            return;
        }

        Vector3d normalSum = new Vector3d();
        Vector3d centroidSum = new Vector3d();
        double areaSum = 0.0;

        for (GaiaFace face : faces) {
            List<GaiaVertex> faceVertices = getVerticesFromFace(face, sourceVertices);
            if (faceVertices.size() < 3) {
                continue;
            }

            GaiaTriangle triangle = new GaiaTriangle(faceVertices.get(0).getPosition(), faceVertices.get(1).getPosition(), faceVertices.get(2).getPosition());

            double area = triangle.area();
            if (area < EPSILON) {
                continue;
            }

            Vector3d normal = new Vector3d(triangle.getPlane().getNormal());
            if (normal.lengthSquared() < EPSILON) {
                continue;
            }
            normal.normalize();

            if (normalSum.lengthSquared() > EPSILON && normalSum.dot(normal) < 0.0) {
                normal.negate();
            }
            normalSum.add(new Vector3d(normal).mul(area));

            Vector3d centroid = new Vector3d(faceVertices.get(0).getPosition()).add(faceVertices.get(1).getPosition()).add(faceVertices.get(2).getPosition()).div(3.0).mul(area);

            centroidSum.add(centroid);
            areaSum += area;
        }

        if (areaSum < EPSILON || normalSum.lengthSquared() < EPSILON) {
            return;
        }

        Vector3d refinedNormal = normalSum.normalize();
        Vector3d refinedCenter = centroidSum.div(areaSum);

        billboardPlane.setNormal(refinedNormal);
        billboardPlane.setCenter(refinedCenter);
        billboardPlane.setPlane(new GaiaPlane(refinedCenter, refinedNormal));
    }

    private Vector3d calculateAreaWeightedNormal(List<FaceData> cluster) {
        Vector3d sum = new Vector3d();

        for (FaceData data : cluster) {
            Vector3d n = new Vector3d(data.normal);

            if (sum.lengthSquared() > EPSILON && sum.dot(n) < 0.0) {
                n.negate();
            }

            sum.add(n.mul(data.area));
        }

        if (sum.lengthSquared() < EPSILON) {
            return null;
        }

        return sum.normalize();
    }

    private Vector3d calculateAreaWeightedCentroid(List<FaceData> cluster) {
        Vector3d sum = new Vector3d();
        double areaSum = 0.0;

        for (FaceData data : cluster) {
            sum.add(new Vector3d(data.centroid).mul(data.area));
            areaSum += data.area;
        }

        if (areaSum < EPSILON) {
            return null;
        }

        return sum.div(areaSum);
    }

    /**
     * plane이 너무 크면 2D plane local(U,V) 기준으로 반씩 잘라서 다시 생성.
     * 아주 정교한 spatial clustering은 아니지만,
     * 기존 한 장짜리 billboard가 너무 커지는 문제를 많이 줄여줌.
     */
    private List<BillboardPlane> splitPlaneIfNeeded(BillboardPlane plane, List<GaiaVertex> vertices, int depth) {
        if (plane == null) {
            return Collections.emptyList();
        }

        double width = plane.getMaxU() - plane.getMinU();
        double height = plane.getMaxV() - plane.getMinV();
        double splitThreshold = options.getMaxPlaneExtent();

        if (depth >= options.getMaxSplitDepth() || (width <= splitThreshold && height <= splitThreshold)) {
            return Collections.singletonList(plane);
        }

        boolean splitByU = width >= height;
        double middle = splitByU ? (plane.getMinU() + plane.getMaxU()) * 0.5 : (plane.getMinV() + plane.getMaxV()) * 0.5;

        List<GaiaFace> leftOrBottom = new ArrayList<>();
        List<GaiaFace> rightOrTop = new ArrayList<>();

        Vector3d origin = projectPointOntoPlane(new Vector3d(plane.getCenter()), plane.getPlane());
        Vector3d tangent = new Vector3d(plane.getTangent());
        Vector3d bitangent = new Vector3d(plane.getBitangent());

        for (GaiaFace face : plane.getFaces()) {
            List<GaiaVertex> faceVertices = getVerticesFromFace(face, vertices);
            Vector3d centroid = calculateCentroid(faceVertices);
            if (centroid == null) {
                continue;
            }

            Vector3d projected = projectPointOntoPlane(new Vector3d(centroid), plane.getPlane());
            Vector3d diff = new Vector3d(projected).sub(origin);

            double u = diff.dot(tangent);
            double v = diff.dot(bitangent);
            double value = splitByU ? u : v;

            if (value <= middle + options.getSplitBalanceEpsilon()) {
                leftOrBottom.add(face);
            } else {
                rightOrTop.add(face);
            }
        }

        if (leftOrBottom.isEmpty() || rightOrTop.isEmpty()) {
            return Collections.singletonList(plane);
        }

        double leftArea = calculateFaceAreaSum(leftOrBottom, vertices);
        double rightArea = calculateFaceAreaSum(rightOrTop, vertices);
        double totalArea = leftArea + rightArea;
        if (totalArea < EPSILON || Math.min(leftArea, rightArea) / totalArea < options.getSplitBalanceEpsilon()) {
            return Collections.singletonList(plane);
        }

        BillboardPlane planeA = rebuildPlaneFromFaces(leftOrBottom, vertices);
        BillboardPlane planeB = rebuildPlaneFromFaces(rightOrTop, vertices);

        List<BillboardPlane> result = new ArrayList<>();
        if (planeA != null) {
            computePlaneOBB(planeA, vertices);
            result.addAll(splitPlaneIfNeeded(planeA, vertices, depth + 1));
        }
        if (planeB != null) {
            computePlaneOBB(planeB, vertices);
            result.addAll(splitPlaneIfNeeded(planeB, vertices, depth + 1));
        }
        return result;
    }

    private double calculateFaceAreaSum(List<GaiaFace> faces, List<GaiaVertex> vertices) {
        double areaSum = 0.0;
        for (GaiaFace face : faces) {
            List<GaiaVertex> faceVertices = getVerticesFromFace(face, vertices);
            if (faceVertices.size() < 3) {
                continue;
            }

            GaiaTriangle triangle = new GaiaTriangle(faceVertices.get(0).getPosition(), faceVertices.get(1).getPosition(), faceVertices.get(2).getPosition());
            areaSum += triangle.area();
        }
        return areaSum;
    }

    private BillboardPlane rebuildPlaneFromFaces(List<GaiaFace> faces, List<GaiaVertex> vertices) {
        if (faces == null || faces.isEmpty()) {
            return null;
        }

        List<FaceData> cluster = new ArrayList<>(faces.size());
        for (GaiaFace face : faces) {
            List<GaiaVertex> faceVertices = getVerticesFromFace(face, vertices);
            if (faceVertices.size() < 3) {
                continue;
            }

            GaiaTriangle triangle = new GaiaTriangle(faceVertices.get(0).getPosition(), faceVertices.get(1).getPosition(), faceVertices.get(2).getPosition());

            double area = triangle.area();
            if (area < options.getMinTriangleArea()) {
                continue;
            }

            FaceData data = new FaceData();
            data.face = face;
            data.faceVertices = faceVertices;
            data.triangle = triangle;
            data.plane = triangle.getPlane();
            data.normal = safeNormalize(new Vector3d(data.plane.getNormal()));
            data.centroid = calculateCentroid(faceVertices);
            data.area = area;

            if (data.normal != null && data.centroid != null) {
                cluster.add(data);
            }
        }

        if (cluster.isEmpty()) {
            return null;
        }

        BillboardPlane plane = createRefinedBillboardPlane(cluster, vertices);
        if (plane != null) {
            plane.setFaces(faces);
        }
        return plane;
    }

    private OrthographicProjection createBillboardQuad(BillboardPlane billboardPlane, List<GaiaVertex> sourceVertices, List<GaiaVertex> targetVertices, GaiaSurface targetSurface) {
        if (billboardPlane == null) {
            return null;
        }

        Vector3d normal = new Vector3d(billboardPlane.getNormal());
        if (normal.lengthSquared() < EPSILON) {
            return null;
        }
        normal.normalize();

        Vector3d tangent = new Vector3d(billboardPlane.getTangent());
        Vector3d bitangent = new Vector3d(billboardPlane.getBitangent());
        if (tangent.lengthSquared() < EPSILON || bitangent.lengthSquared() < EPSILON) {
            return null;
        }

        Vector3d origin = projectPointOntoPlane(new Vector3d(billboardPlane.getCenter()), billboardPlane.getPlane());

        double minU = billboardPlane.getMinU();
        double minV = billboardPlane.getMinV();
        double maxU = billboardPlane.getMaxU();
        double maxV = billboardPlane.getMaxV();

        double width = maxU - minU;
        double height = maxV - minV;

        if (width <= EPSILON || height <= EPSILON) {
            return null;
        }

        double overlapRatio = Math.max(0.0, options.getQuadOverlapRatio());
        if (overlapRatio > 0.0) {
            double expandU = width * overlapRatio;
            double expandV = height * overlapRatio;
            minU -= expandU;
            maxU += expandU;
            minV -= expandV;
            maxV += expandV;
            width = maxU - minU;
            height = maxV - minV;
        }

        Vector3d p0 = new Vector3d(origin).add(new Vector3d(tangent).mul(minU)).add(new Vector3d(bitangent).mul(minV));
        Vector3d p1 = new Vector3d(origin).add(new Vector3d(tangent).mul(maxU)).add(new Vector3d(bitangent).mul(minV));
        Vector3d p2 = new Vector3d(origin).add(new Vector3d(tangent).mul(maxU)).add(new Vector3d(bitangent).mul(maxV));
        Vector3d p3 = new Vector3d(origin).add(new Vector3d(tangent).mul(minU)).add(new Vector3d(bitangent).mul(maxV));

        int baseIndex = targetVertices.size();

        targetVertices.add(createVertex(p0, normal, 0.0, 1.0));
        targetVertices.add(createVertex(p1, normal, 1.0, 1.0));
        targetVertices.add(createVertex(p2, normal, 1.0, 0.0));
        targetVertices.add(createVertex(p3, normal, 0.0, 0.0));

        GaiaFace face0 = new GaiaFace();
        face0.setIndices(new int[]{baseIndex, baseIndex + 1, baseIndex + 2});

        GaiaFace face1 = new GaiaFace();
        face1.setIndices(new int[]{baseIndex, baseIndex + 2, baseIndex + 3});

        targetSurface.getFaces().add(face0);
        targetSurface.getFaces().add(face1);

        Vector3d quadCenter = new Vector3d(p0).add(p1).add(p2).add(p3).mul(0.25);
        double planeSize = Math.max(width, height);
        double cameraDistance = Math.max(planeSize * 2.0, 0.1);

        Vector3d cameraPos = new Vector3d(quadCenter).add(new Vector3d(normal).mul(cameraDistance));
        Vector3d forward = new Vector3d(quadCenter).sub(cameraPos).normalize();

        double minDepth = Double.POSITIVE_INFINITY;
        double maxDepth = Double.NEGATIVE_INFINITY;

        for (GaiaFace face : billboardPlane.getFaces()) {
            List<GaiaVertex> faceVertices = getVerticesFromFace(face, sourceVertices);
            for (GaiaVertex vertex : faceVertices) {
                double depth = new Vector3d(vertex.getPosition()).sub(cameraPos).dot(forward);
                minDepth = Math.min(minDepth, depth);
                maxDepth = Math.max(maxDepth, depth);
            }
        }

        if (!Double.isFinite(minDepth) || !Double.isFinite(maxDepth)) {
            return null;
        }

        double margin = Math.max(planeSize * 0.1, 0.01);

        OrthographicProjection projection = new OrthographicProjection();
        projection.setCameraPosition(cameraPos);
        projection.setTarget(quadCenter);
        projection.setUp(bitangent);

        projection.setLeft(-width * 0.5);
        projection.setRight(width * 0.5);
        projection.setBottom(-height * 0.5);
        projection.setTop(height * 0.5);

        projection.setNear(Math.max(0.01, minDepth - margin));
        projection.setFar(maxDepth + margin);

        projection.setBitangent(bitangent);
        projection.setTangent(tangent);
        projection.setNormal(normal);

        return projection;
    }

    private static class FaceData {
        int faceIndex;
        GaiaFace face;
        List<GaiaVertex> faceVertices;
        GaiaTriangle triangle;
        GaiaPlane plane;
        Vector3d normal;
        Vector3d centroid;
        double area;
        Set<Integer> vertexIndexSet;
    }
}
