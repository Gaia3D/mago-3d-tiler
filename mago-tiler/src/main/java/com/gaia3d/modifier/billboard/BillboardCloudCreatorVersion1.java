package com.gaia3d.modifier.billboard;

import com.gaia3d.basic.geometry.entities.GaiaPlane;
import com.gaia3d.basic.geometry.entities.GaiaTriangle;
import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
public class BillboardCloudCreatorVersion1 {
    // 초기 테스트용 threshold
    private static final double NORMAL_DOT_THRESHOLD = Math.cos(Math.toRadians(45)); // 법선 유사도 기준
    private static final double PLANE_DISTANCE_EPSILON = 0.5; // 후보 평면이 시드 평면에서 얼마나 떨어져 있어야 하는지
    private static final double SEED_RADIUS = 0.5;             // 시드 반경
    private static final double MIN_TRIANGLE_AREA = 0.001;
    private final GaiaExtractor extractor = new GaiaExtractor();

    public GaiaScene createBillboardCloud(GaiaScene scene) {
        List<GaiaNode> leafNodes = extractor.extractAllNodes(scene, true);

        GaiaScene resultScene = createDefaultScene();
        GaiaNode rootNode = resultScene.getRootNode();

        for (GaiaNode node : leafNodes) {
            log.info("Extracting faces from node: {}", node.getName());
            List<GaiaPrimitive> primitives = extractor.extractAllPrimitives(node);
            log.info("Extracted {} primitives from node: {}", primitives.size(), node.getName());

            for (GaiaPrimitive primitive : primitives) {
                List<BillboardPlane> billboardPlanes = createBillboardCloud(primitive);
                log.info("Created {} billboard planes from primitive", billboardPlanes.size());

                GaiaNode billboardNode = new GaiaNode();
                billboardNode.setName(node.getName() + "_Billboard");
                rootNode.getChildren().add(billboardNode);

                GaiaMesh billboardMesh = new GaiaMesh();
                billboardNode.getMeshes().add(billboardMesh);

                GaiaPrimitive billboardPrimitive = new GaiaPrimitive();
                billboardPrimitive.setMaterialIndex(0);
                billboardMesh.getPrimitives().add(billboardPrimitive);

                buildBillboardPrimitive(billboardPlanes, primitive.getVertices(), billboardPrimitive);
            }
        }

        return resultScene;
    }

    private void buildBillboardPrimitive(List<BillboardPlane> billboardPlanes, List<GaiaVertex> sourceVertices, GaiaPrimitive targetPrimitive) {
        List<GaiaVertex> targetVertices = targetPrimitive.getVertices();
        List<GaiaSurface> targetSurfaces = targetPrimitive.getSurfaces();

        GaiaSurface surface = new GaiaSurface();
        targetSurfaces.add(surface);

        for (BillboardPlane billboardPlane : billboardPlanes) {
            addBillboardQuad(billboardPlane, sourceVertices, targetVertices, surface);
        }
    }

    private void addBillboardQuad(BillboardPlane billboardPlane, List<GaiaVertex> sourceVertices, List<GaiaVertex> targetVertices, GaiaSurface targetSurface) {
        if (billboardPlane == null || billboardPlane.getFaces() == null || billboardPlane.getFaces().isEmpty()) {
            return;
        }

        Vector3d normal = new Vector3d(billboardPlane.getNormal());
        if (normal.lengthSquared() == 0.0) {
            return;
        }
        normal.normalize();

        Vector3d origin = projectPointOntoPlane(new Vector3d(billboardPlane.getCenter()), billboardPlane.getPlane());

        Vector3d tangent = createStableTangent(normal);
        Vector3d bitangent = new Vector3d(normal).cross(tangent).normalize();

        double minU = Double.POSITIVE_INFINITY;
        double minV = Double.POSITIVE_INFINITY;
        double maxU = Double.NEGATIVE_INFINITY;
        double maxV = Double.NEGATIVE_INFINITY;

        // grouped face들의 모든 점을 plane local 2D로 투영
        for (GaiaFace face : billboardPlane.getFaces()) {
            List<GaiaVertex> faceVertices = getVerticesFromFace(face, sourceVertices);
            for (GaiaVertex vertex : faceVertices) {
                Vector3d p = new Vector3d(vertex.getPosition());

                Vector3d diff = new Vector3d(p).sub(origin);
                double u = diff.dot(tangent);
                double v = diff.dot(bitangent);

                if (u < minU) {minU = u;}
                if (u > maxU) {maxU = u;}
                if (v < minV) {minV = v;}
                if (v > maxV) {maxV = v;}
            }
        }

        if (!Double.isFinite(minU) || !Double.isFinite(minV) || !Double.isFinite(maxU) || !Double.isFinite(maxV)) {
            return;
        }

        // 너무 작은 쿼드는 스킵
        double width = maxU - minU;
        double height = maxV - minV;
        if (width <= 1e-9 || height <= 1e-9) {
            return;
        }

        // local rect -> world corners
        Vector3d p0 = new Vector3d(origin).add(new Vector3d(tangent).mul(minU)).add(new Vector3d(bitangent).mul(minV));
        Vector3d p1 = new Vector3d(origin).add(new Vector3d(tangent).mul(maxU)).add(new Vector3d(bitangent).mul(minV));
        Vector3d p2 = new Vector3d(origin).add(new Vector3d(tangent).mul(maxU)).add(new Vector3d(bitangent).mul(maxV));
        Vector3d p3 = new Vector3d(origin).add(new Vector3d(tangent).mul(minU)).add(new Vector3d(bitangent).mul(maxV));

        int baseIndex = targetVertices.size();

        GaiaVertex v0 = createVertex(p0, normal, 0.0, 0.0);
        GaiaVertex v1 = createVertex(p1, normal, 1.0, 0.0);
        GaiaVertex v2 = createVertex(p2, normal, 1.0, 1.0);
        GaiaVertex v3 = createVertex(p3, normal, 0.0, 1.0);

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
    }

    private Vector3d createStableTangent(Vector3d normal) {
        Vector3d ref = Math.abs(normal.z) < 0.9 ? new Vector3d(0.0, 0.0, 1.0) : new Vector3d(0.0, 1.0, 0.0);

        Vector3d tangent = ref.cross(normal, new Vector3d());
        if (tangent.lengthSquared() == 0.0) {
            ref.set(1.0, 0.0, 0.0);
            tangent = ref.cross(normal, new Vector3d());
        }
        tangent.normalize();
        return tangent;
    }

    private GaiaVertex createVertex(Vector3d position, Vector3d normal, double u, double v) {
        GaiaVertex vertex = new GaiaVertex();
        vertex.setPosition(new Vector3d(position));
        vertex.setNormal(new Vector3d(normal));

        // GaiaVertex에 texcoord 타입이 Vector2d인지 Vector2f인지에 맞게 수정
        vertex.setTexcoords(new org.joml.Vector2d(u, v));

        return vertex;
    }

    private Vector3d projectPointOntoPlane(Vector3d point, GaiaPlane plane) {
        Vector3d normal = plane.getNormal();
        double lenSq = normal.lengthSquared();
        if (lenSq == 0.0) {
            return new Vector3d(point);
        }

        double distance = plane.distanceToPoint(point); // signed distance * |n| if n not normalized
        return new Vector3d(point).sub(new Vector3d(normal).mul(distance / lenSq));
    }

    public List<BillboardPlane> createBillboardCloud(GaiaPrimitive primitive) {
        List<GaiaVertex> vertices = primitive.getVertices();
        List<GaiaFace> faces = extractor.extractAllFaces(primitive);

        // filter out small triangles to avoid degenerate planes
        List<GaiaFace> filteredFaces = new ArrayList<>();
        for (GaiaFace face : faces) {
            GaiaTriangle triangle = new GaiaTriangle(
                vertices.get(face.getIndices()[0]).getPosition(),
                vertices.get(face.getIndices()[1]).getPosition(),
                vertices.get(face.getIndices()[2]).getPosition()
            );
            double seedArea = triangle.area();
            if (seedArea < MIN_TRIANGLE_AREA) {
                continue;
            }
            filteredFaces.add(face);
        }

        return createBillboardPlanes(filteredFaces, vertices);
    }

    private GaiaScene createDefaultScene() {
        GaiaScene scene = new GaiaScene();
        GaiaNode node = new GaiaNode();
        node.setName("DefaultNode");
        scene.getNodes().add(node);

        List<GaiaMaterial> materials = new ArrayList<>();
        GaiaMaterial material = new GaiaMaterial();
        material.setName("BillboardMaterial");
        materials.add(material);

        scene.setMaterials(materials);
        scene.updateBoundingBox();
        return scene;
    }

    private List<BillboardPlane> createBillboardPlanes(List<GaiaFace> faces, List<GaiaVertex> vertices) {
        List<BillboardPlane> billboardPlanes = new ArrayList<>();
        Set<Integer> usedFaceIndices = new HashSet<>();

        for (int i = 0; i < faces.size(); i++) {
            if (usedFaceIndices.contains(i)) {
                continue;
            }

            BillboardPlane billboardPlane = createBillboardPlane(faces, vertices, i, usedFaceIndices);
            if (billboardPlane != null) {
                billboardPlanes.add(billboardPlane);
            }
        }

        log.info("Created {} billboard planes from {} faces", billboardPlanes.size(), faces.size());
        return billboardPlanes;
    }

    private BillboardPlane createBillboardPlane(List<GaiaFace> faces, List<GaiaVertex> vertices, int seedFaceIndex, Set<Integer> usedFaceIndices) {
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

        for (int i = 0; i < faces.size(); i++) {
            if (i == seedFaceIndex || usedFaceIndices.contains(i)) {
                continue;
            }

            GaiaFace candidateFace = faces.get(i);
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
            double dot = seedNormal.dot(candidateNormal);
            if (dot < NORMAL_DOT_THRESHOLD) {
                continue;
            }

            // 2. candidate centroid가 seed plane에 가까운지
            double planeDistance = Math.abs(seedPlane.distanceToPoint(candidateCentroid));
            if (planeDistance > PLANE_DISTANCE_EPSILON) {
                continue;
            }

            // 3. candidate centroid가 seed centroid와 너무 멀지 않은지
            double centroidDistance = seedCentroid.distance(candidateCentroid);
            if (centroidDistance > SEED_RADIUS) {
                continue;
            }

            groupedFaces.add(candidateFace);
            usedFaceIndices.add(i);
        }

        BillboardPlane billboardPlane = new BillboardPlane();

        billboardPlane.setPlane(seedPlane);

        // BillboardPlane에 아래 필드가 있으면 같이 넣는 걸 추천
        billboardPlane.setFaces(groupedFaces);
        billboardPlane.setCenter(seedCentroid);
        billboardPlane.setNormal(seedNormal);

        // 1. groupedFaces 기반으로 normal / center 보정
        refineBillboardPlane(billboardPlane, vertices);

        // 2. 보정된 plane 위에서 OBB 계산
        computePlaneOBB(billboardPlane, vertices);

        log.info("Seed face {} -> grouped {} faces", seedFaceIndex, groupedFaces.size());
        return billboardPlane;
    }

    private List<GaiaVertex> getVerticesFromFace(GaiaFace face, List<GaiaVertex> allVertices) {
        int[] vertexIndices = face.getIndices();
        List<GaiaVertex> vertices = new ArrayList<>(vertexIndices.length);

        for (int index : vertexIndices) {
            if (index >= 0 && index < allVertices.size()) {
                vertices.add(allVertices.get(index));
            } else {
                log.warn("Vertex index {} is out of bounds for vertices list size {}", index, allVertices.size());
            }
        }
        return vertices;
    }

    private Vector3d calculateCentroid(List<GaiaVertex> vertices) {
        if (vertices == null || vertices.isEmpty()) {
            return null;
        }

        Vector3d centroid = new Vector3d();
        for (GaiaVertex vertex : vertices) {
            centroid.add(vertex.getPosition());
        }
        centroid.div(vertices.size());
        return centroid;
    }

    private Vector3d safeNormalize(Vector3d v) {
        if (v == null || v.lengthSquared() == 0.0) {
            return null;
        }
        return v.normalize(new Vector3d());
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

    private void computePlaneOBB(BillboardPlane billboardPlane, List<GaiaVertex> sourceVertices) {
        if (billboardPlane == null || billboardPlane.getFaces() == null || billboardPlane.getFaces().isEmpty()) {
            return;
        }

        Vector3d normal = new Vector3d(billboardPlane.getNormal());
        if (normal.lengthSquared() == 0.0) {
            return;
        }
        normal.normalize();

        Vector3d origin = projectPointOntoPlane(new Vector3d(billboardPlane.getCenter()), billboardPlane.getPlane());

        Vector3d tempTangent = createStableTangent(normal);
        Vector3d tempBitangent = new Vector3d(normal).cross(tempTangent).normalize();

        List<Vector2d> points2D = collectProjectedPoints2D(billboardPlane, sourceVertices, origin, tempTangent, tempBitangent);

        if (points2D.isEmpty()) {
            return;
        }

        double meanX = 0.0;
        double meanY = 0.0;
        for (Vector2d p : points2D) {
            meanX += p.x;
            meanY += p.y;
        }
        meanX /= points2D.size();
        meanY /= points2D.size();

        double xx = 0.0;
        double xy = 0.0;
        double yy = 0.0;
        for (Vector2d p : points2D) {
            double dx = p.x - meanX;
            double dy = p.y - meanY;
            xx += dx * dx;
            xy += dx * dy;
            yy += dy * dy;
        }

        double angle = 0.5 * Math.atan2(2.0 * xy, xx - yy);
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        Vector3d tangent = new Vector3d(tempTangent).mul(cos).add(new Vector3d(tempBitangent).mul(sin)).normalize();
        Vector3d bitangent = new Vector3d(normal).cross(tangent).normalize();

        double minU = Double.POSITIVE_INFINITY;
        double minV = Double.POSITIVE_INFINITY;
        double maxU = Double.NEGATIVE_INFINITY;
        double maxV = Double.NEGATIVE_INFINITY;

        for (GaiaFace face : billboardPlane.getFaces()) {
            List<GaiaVertex> faceVertices = getVerticesFromFace(face, sourceVertices);
            for (GaiaVertex vertex : faceVertices) {
                Vector3d projected = projectPointOntoPlane(new Vector3d(vertex.getPosition()), billboardPlane.getPlane());
                Vector3d diff = new Vector3d(projected).sub(origin);

                double u = diff.dot(tangent);
                double v = diff.dot(bitangent);

                if (u < minU) {minU = u;}
                if (u > maxU) {maxU = u;}
                if (v < minV) {minV = v;}
                if (v > maxV) {maxV = v;}
            }
        }

        billboardPlane.setTangent(tangent);
        billboardPlane.setBitangent(bitangent);
        billboardPlane.setMinU(minU);
        billboardPlane.setMinV(minV);
        billboardPlane.setMaxU(maxU);
        billboardPlane.setMaxV(maxV);
    }

    private List<Vector2d> collectProjectedPoints2D(BillboardPlane billboardPlane, List<GaiaVertex> sourceVertices, Vector3d origin, Vector3d tangent, Vector3d bitangent) {
        List<Vector2d> points2D = new ArrayList<>();

        for (GaiaFace face : billboardPlane.getFaces()) {
            List<GaiaVertex> faceVertices = getVerticesFromFace(face, sourceVertices);
            for (GaiaVertex vertex : faceVertices) {
                Vector3d projected = projectPointOntoPlane(new Vector3d(vertex.getPosition()), billboardPlane.getPlane());
                Vector3d diff = new Vector3d(projected).sub(origin);

                double u = diff.dot(tangent);
                double v = diff.dot(bitangent);

                points2D.add(new Vector2d(u, v));
            }
        }

        return points2D;
    }
}