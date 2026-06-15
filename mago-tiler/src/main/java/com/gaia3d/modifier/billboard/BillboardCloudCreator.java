package com.gaia3d.modifier.billboard;

import com.gaia3d.basic.geometry.entities.GaiaPlane;
import com.gaia3d.basic.geometry.entities.GaiaTriangle;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.modifier.billboard.atlas.TextureAtlas;
import com.gaia3d.modifier.billboard.atlas.TextureAtlasSource;
import com.gaia3d.modifier.billboard.atlas.TextureAtlasUtils;
import com.gaia3d.modifier.billboard.render.OrthographicProjection;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.*;

@Slf4j
@Deprecated
public class BillboardCloudCreator extends AbstractBillboardCloudCreator {

    public BillboardCloudCreator() {
        super();
    }

    public BillboardCloudCreator(BillboardCloudOptions options) {
        super(options);
    }

    public GaiaScene create(GaiaScene scene) {
        List<GaiaNode> leafNodes = extractor.extractAllNodes(scene, true);

        GaiaScene resultScene = createDefaultScene();
        GaiaNode rootNode = resultScene.getRootNode();

        int nodeIndex = 0;
        for (GaiaNode node : leafNodes) {
            log.info("[{}/{}] Processing node: {}", ++nodeIndex, leafNodes.size(), node.getName());
            List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(node);
            log.info("Extracted {} primitives from node: {}", primitives.size(), node.getName());

            for (GaiaPrimitive primitive : primitives) {
                List<BillboardPlane> billboardPlanes = generateBillboardPlanes(primitive);
                log.info("Created {} billboard planes from primitive", billboardPlanes.size());
                GaiaMaterial material = scene.getMaterials().get(primitive.getMaterialIndex());

                GaiaNode billboardNode = new GaiaNode();
                billboardNode.setName(node.getName() + "_billboard");
                rootNode.getChildren().add(billboardNode);

                GaiaMesh billboardMesh = new GaiaMesh();
                billboardNode.getMeshes().add(billboardMesh);

                List<GaiaPrimitive> newPrimitives = buildBillboardPrimitive(resultScene, billboardPlanes, primitive.getVertices(), material);
                for (GaiaPrimitive newPrimitive : newPrimitives) {
                    billboardMesh.getPrimitives().add(newPrimitive);
                }
            }
        }

        atlasTextures(resultScene);
        mergePrimitives(resultScene);
        return resultScene;
    }

    private List<GaiaPrimitive> buildBillboardPrimitive(GaiaScene scene, List<BillboardPlane> billboardPlanes, List<GaiaVertex> sourceVertices, GaiaMaterial originalMaterial) {
        Map<TextureType, List<GaiaTexture>> materialTextures = originalMaterial.getTextures();
        List<GaiaTexture> diffuseTextures = materialTextures.getOrDefault(TextureType.DIFFUSE, Collections.emptyList());
        GaiaTexture finalDiffuseTexture = null;
        BufferedImage finalDiffuseTextureImage = null;
        if (!diffuseTextures.isEmpty()) {
            finalDiffuseTexture = diffuseTextures.getFirst();
            finalDiffuseTexture.loadImage();
            finalDiffuseTextureImage = finalDiffuseTexture.getBufferedImage();
        }

        List<GaiaPrimitive> primitives = new ArrayList<>();
        List<GaiaMaterial> materials = scene.getMaterials();
        int primitiveIndex = 0;
        for (BillboardPlane billboardPlane : billboardPlanes) {
            log.info(" - [{}/{}] Adding billboard primitive to mesh", ++primitiveIndex, billboardPlanes.size());
            GaiaPrimitive targetPrimitive = new GaiaPrimitive();
            primitives.add(targetPrimitive);

            List<GaiaVertex> targetVertices = targetPrimitive.getVertices();
            List<GaiaSurface> targetSurfaces = targetPrimitive.getSurfaces();

            GaiaSurface surface = new GaiaSurface();
            targetSurfaces.add(surface);

            GaiaMaterial material = new GaiaMaterial();
            int materialIndex = materials.size();
            String materialName = "billboard_material_" + materialIndex;
            material.setId(materialIndex);
            material.setName(materialName);
            //material.setOpaque(false);
            //material.setBlend(true);
            materials.add(material);

            targetPrimitive.setMaterialIndex(materialIndex);

            OrthographicProjection orthographicProjection = createBillboardQuad(billboardPlane, sourceVertices, targetVertices, surface);
            log.debug("Added billboard quad for plane with center {} and normal {}, orthographic projection: {}", billboardPlane.getCenter(), billboardPlane.getNormal(), orthographicProjection);

            renderer.init();
            if (orthographicProjection != null) {
                log.debug("Baking texture for billboard plane with center {} and normal {}", billboardPlane.getCenter(), billboardPlane.getNormal());

                renderer.bakeBillboardPlane(billboardPlane, sourceVertices, orthographicProjection, finalDiffuseTextureImage);
                String parentPath = options.getTempPath();
                String textureName = materialName + ".png";
                String texturePath = parentPath + File.separator + textureName;
                renderer.saveCurrentFboToPng(texturePath);

                GaiaTexture texture = new GaiaTexture();
                texture.setName(textureName);
                texture.setParentPath(parentPath);
                texture.setPath(textureName);
                material.getTextures().computeIfAbsent(TextureType.DIFFUSE, k -> new ArrayList<>()).add(texture);
                texture.loadImage();
            }
            renderer.cleanup();
        }

        return primitives;
    }

    private OrthographicProjection createBillboardQuad(BillboardPlane billboardPlane, List<GaiaVertex> sourceVertices, List<GaiaVertex> targetVertices, GaiaSurface targetSurface) {
        if (billboardPlane == null) {
            return null;
        }

        Vector3d normal = new Vector3d(billboardPlane.getNormal()).normalize();
        Vector3d origin = projectPointOntoPlane(new Vector3d(billboardPlane.getCenter()), billboardPlane.getPlane());
        Vector3d tangent = new Vector3d(billboardPlane.getTangent());
        Vector3d bitangent = new Vector3d(billboardPlane.getBitangent());

        double minU = billboardPlane.getMinU();
        double minV = billboardPlane.getMinV();
        double maxU = billboardPlane.getMaxU();
        double maxV = billboardPlane.getMaxV();

        double width = maxU - minU;
        double height = maxV - minV;

        if (width <= 1e-9 || height <= 1e-9) {
            return null;
        }

        // quad corners
        Vector3d p0 = new Vector3d(origin).add(new Vector3d(tangent).mul(minU)).add(new Vector3d(bitangent).mul(minV));
        Vector3d p1 = new Vector3d(origin).add(new Vector3d(tangent).mul(maxU)).add(new Vector3d(bitangent).mul(minV));
        Vector3d p2 = new Vector3d(origin).add(new Vector3d(tangent).mul(maxU)).add(new Vector3d(bitangent).mul(maxV));
        Vector3d p3 = new Vector3d(origin).add(new Vector3d(tangent).mul(minU)).add(new Vector3d(bitangent).mul(maxV));

        int baseIndex = targetVertices.size();

        GaiaVertex v0 = createVertex(p0, 0.0, 1.0);
        GaiaVertex v1 = createVertex(p1, 1.0, 1.0);
        GaiaVertex v2 = createVertex(p2, 1.0, 0.0);
        GaiaVertex v3 = createVertex(p3, 0.0, 0.0);

        targetVertices.add(v0);
        targetVertices.add(v1);
        targetVertices.add(v2);
        targetVertices.add(v3);

        GaiaFace face0 = new GaiaFace();
        face0.setIndices(new int[]{baseIndex, baseIndex + 1, baseIndex + 2});

        GaiaFace face1 = new GaiaFace();
        face1.setIndices(new int[]{baseIndex, baseIndex + 2, baseIndex + 3});

        targetSurface.getFaces().add(face0);
        targetSurface.getFaces().add(face1);

        Vector3d obbCenter = new Vector3d(p0).add(p1).add(p2).add(p3).mul(0.25);

        double planeSize = Math.max(width, height);

        double cameraDistance = planeSize * 2.0;

        Vector3d cameraPos = new Vector3d(obbCenter).add(new Vector3d(normal).mul(cameraDistance));
        Vector3d forward = new Vector3d(obbCenter).sub(cameraPos).normalize();

        double minDepth = Double.POSITIVE_INFINITY;
        double maxDepth = Double.NEGATIVE_INFINITY;

        for (GaiaFace face : billboardPlane.getFaces()) {
            List<GaiaVertex> faceVertices = getVerticesFromFace(face, sourceVertices);

            for (GaiaVertex vertex : faceVertices) {
                Vector3d p = new Vector3d(vertex.getPosition());
                double depth = new Vector3d(p).sub(cameraPos).dot(forward);

                if (depth < minDepth) minDepth = depth;
                if (depth > maxDepth) maxDepth = depth;
            }
        }

        double margin = planeSize * 0.1;

        double near = Math.max(0.01, minDepth - margin);
        double far = maxDepth + margin;

        // projection 객체 생성
        OrthographicProjection projection = new OrthographicProjection();

        projection.setCameraPosition(cameraPos);
        projection.setTarget(obbCenter);
        projection.setUp(bitangent);

        double halfWidth = width * 0.5;
        double halfHeight = height * 0.5;

        projection.setLeft(-halfWidth);
        projection.setRight(halfWidth);
        projection.setBottom(-halfHeight);
        projection.setTop(halfHeight);

        projection.setNear(near);
        projection.setFar(far);

        projection.setBitangent(bitangent);
        projection.setTangent(tangent);
        projection.setNormal(normal);

        return projection;
    }

    private List<BillboardPlane> generateBillboardPlanes(GaiaPrimitive primitive) {
        List<GaiaVertex> vertices = primitive.getVertices();
        List<GaiaFace> faces = extractor.extractAllFaces(primitive);

        List<GaiaFace> filteredFaces = new ArrayList<>();
        for (GaiaFace face : faces) {
            GaiaTriangle triangle = new GaiaTriangle(vertices.get(face.getIndices()[0]).getPosition(), vertices.get(face.getIndices()[1]).getPosition(), vertices.get(face.getIndices()[2]).getPosition());
            double seedArea = triangle.area();
            if (seedArea < options.getMinTriangleArea()) {
                log.debug("Skipping face with area {} below threshold {}, indices: {}", seedArea, options.getMinTriangleArea(), face.getIndices());
                continue;
            }
            filteredFaces.add(face);
        }

        return generateBillboardPlanes(filteredFaces, vertices);
    }

    private List<BillboardPlane> generateBillboardPlanes(List<GaiaFace> faces, List<GaiaVertex> vertices) {
        List<BillboardPlane> billboardPlanes = new ArrayList<>();
        Set<Integer> usedFaceIndices = new HashSet<>();

        for (int i = 0; i < faces.size(); i++) {
            if (usedFaceIndices.contains(i)) {
                continue;
            }

            BillboardPlane billboardPlane = generateBillboardPlanes(faces, vertices, i, usedFaceIndices);
            if (billboardPlane != null) {
                billboardPlanes.add(billboardPlane);
            }
        }

        log.debug("Created {} billboard planes from {} faces", billboardPlanes.size(), faces.size());
        return billboardPlanes;
    }

    private BillboardPlane generateBillboardPlanes(List<GaiaFace> faces, List<GaiaVertex> vertices, int seedFaceIndex, Set<Integer> usedFaceIndices) {
        GaiaFace seedFace = faces.get(seedFaceIndex);
        List<GaiaVertex> seedVertices = getVerticesFromFace(seedFace, vertices);
        if (seedVertices.size() < 3) {
            return null;
        }

        GaiaTriangle seedTriangle = new GaiaTriangle(seedVertices.get(0).getPosition(), seedVertices.get(1).getPosition(), seedVertices.get(2).getPosition());

        GaiaPlane seedPlane = seedTriangle.getPlane();
        Vector3d seedNormal = safeNormalize(seedPlane.getNormal());
        Vector3d seedCentroid = calculateCentroid(seedVertices);

        if (seedNormal == null || seedCentroid == null) {
            return null;
        }

        List<GaiaFace> groupedFaces = new ArrayList<>();
        groupedFaces.add(seedFace);
        usedFaceIndices.add(seedFaceIndex);

        for (int faceIndex = 0; faceIndex < faces.size(); faceIndex++) {
            if (faceIndex == seedFaceIndex || usedFaceIndices.contains(faceIndex)) {
                continue;
            }

            GaiaFace candidateFace = faces.get(faceIndex);
            List<GaiaVertex> candidateVertices = getVerticesFromFace(candidateFace, vertices);
            if (candidateVertices.size() < 3) {
                continue;
            }

            GaiaTriangle candidateTriangle = new GaiaTriangle(candidateVertices.get(0).getPosition(), candidateVertices.get(1).getPosition(), candidateVertices.get(2).getPosition());

            GaiaPlane candidatePlane = candidateTriangle.getPlane();
            Vector3d candidateNormal = safeNormalize(candidatePlane.getNormal());
            Vector3d candidateCentroid = calculateCentroid(candidateVertices);

            if (candidateNormal == null || candidateCentroid == null) {
                continue;
            }

            // 1. normal 유사도 (양면 leaf 고려)
            double dot = Math.abs(seedNormal.dot(candidateNormal));
            if (dot < options.getNormalDotThreshold()) {
                continue;
            }

            // 2. candidate centroid가 seed plane에 가까운지
            double planeDistance = Math.abs(seedPlane.distanceToPoint(candidateCentroid));
            if (planeDistance > options.getPlaneDistanceEpsilon()) {
                continue;
            }

            // 3. candidate centroid가 seed centroid와 너무 멀지 않은지
            double centroidDistance = seedCentroid.distance(candidateCentroid);
            if (centroidDistance > options.getSetRadius()) {
                continue;
            }

            groupedFaces.add(candidateFace);
            usedFaceIndices.add(faceIndex);
        }

        BillboardPlane billboardPlane = new BillboardPlane();
        billboardPlane.setPlane(seedPlane);
        billboardPlane.setFaces(groupedFaces);
        billboardPlane.setCenter(seedCentroid);
        billboardPlane.setNormal(seedNormal);

        if (options.isRefineBillboardPlane()) {
            refineBillboardPlane(billboardPlane, vertices);
        }

        computePlaneOBB(billboardPlane, vertices);

        log.debug("Seed face {} -> grouped {} faces", seedFaceIndex, groupedFaces.size());
        return billboardPlane;
    }

    private void refineBillboardPlane(BillboardPlane billboardPlane, List<GaiaVertex> sourceVertices) {
        List<GaiaFace> faces = billboardPlane.getFaces();
        if (faces == null || faces.isEmpty()) {
            return;
        }

        Vector3d normalSum = new Vector3d();
        Vector3d centerSum = new Vector3d();
        int pointCount = 0;

        for (GaiaFace face : faces) {
            List<GaiaVertex> faceVertices = getVerticesFromFace(face, sourceVertices);
            if (faceVertices.size() < 3) {
                continue;
            }

            GaiaTriangle triangle = new GaiaTriangle(faceVertices.get(0).getPosition(), faceVertices.get(1).getPosition(), faceVertices.get(2).getPosition());

            Vector3d n = triangle.getPlane().getNormal();
            if (n.lengthSquared() > 0.0) {
                n.normalize();

                if (normalSum.lengthSquared() > 0.0 && normalSum.dot(n) < 0.0) {
                    n.negate();
                }
                normalSum.add(n);
            }

            for (GaiaVertex v : faceVertices) {
                centerSum.add(v.getPosition());
                pointCount++;
            }
        }

        if (normalSum.lengthSquared() == 0.0 || pointCount == 0) {
            return;
        }

        Vector3d refinedNormal = normalSum.normalize();
        Vector3d refinedCenter = centerSum.div(pointCount);

        billboardPlane.setNormal(refinedNormal);
        billboardPlane.setCenter(refinedCenter);
        billboardPlane.setPlane(new GaiaPlane(refinedCenter, refinedNormal));
    }
}