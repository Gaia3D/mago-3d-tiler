package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.modifier.topology.GaiaSceneCleaner;
import com.gaia3d.basic.geometry.modifier.topology.GaiaWeldOptions;
import com.gaia3d.basic.geometry.modifier.topology.GaiaWelder;
import com.gaia3d.basic.geometry.octree.*;
import com.gaia3d.basic.halfedge.HalfEdgeUtils;
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
public class GeometryOnlyReMesherByOctree {
    private int limitDepth = 12;
    private double limitBoxSize = 0.2;
    private int minVertexCount = 20;
    private int minFacesCount = 4;
    private GaiaStatistics sceneStats = null;
    private boolean reMeshAnyWay = false;

    public GeometryOnlyReMesherByOctree() {
    }

    public BarInfo detectBarByPCA(List<GaiaVertex> vertices) {
        BarInfo info = new BarInfo();

        if (vertices == null || vertices.size() < 3) {
            return info;
        }

        int count = vertices.size();

        Vector3d center = new Vector3d();

        for (GaiaVertex v : vertices) {
            center.add(v.getPosition());
        }

        center.div(count);

        // Covariance matrix components.
        double xx = 0.0;
        double xy = 0.0;
        double xz = 0.0;
        double yy = 0.0;
        double yz = 0.0;
        double zz = 0.0;

        for (GaiaVertex v : vertices) {
            Vector3d p = v.getPosition();

            double x = p.x - center.x;
            double y = p.y - center.y;
            double z = p.z - center.z;

            xx += x * x;
            xy += x * y;
            xz += x * z;
            yy += y * y;
            yz += y * z;
            zz += z * z;
        }

        xx /= count;
        xy /= count;
        xz /= count;
        yy /= count;
        yz /= count;
        zz /= count;

        // Power iteration to find main axis.
        Vector3d axis = new Vector3d(1, 1, 1).normalize();

        for (int i = 0; i < 16; i++) {
            double nx = xx * axis.x + xy * axis.y + xz * axis.z;
            double ny = xy * axis.x + yy * axis.y + yz * axis.z;
            double nz = xz * axis.x + yz * axis.y + zz * axis.z;

            axis.set(nx, ny, nz);

            if (axis.lengthSquared() < 1e-20) {
                return info;
            }

            axis.normalize();
        }

        double minT = Double.POSITIVE_INFINITY;
        double maxT = Double.NEGATIVE_INFINITY;
        double maxPerpendicularDist = 0.0;

        for (GaiaVertex v : vertices) {
            Vector3d rel = new Vector3d(v.getPosition()).sub(center);

            double t = rel.dot(axis);

            minT = Math.min(minT, t);
            maxT = Math.max(maxT, t);

            Vector3d projected = new Vector3d(axis).mul(t);
            double perpendicularDist = rel.sub(projected).length();

            maxPerpendicularDist = Math.max(maxPerpendicularDist, perpendicularDist);
        }

        info.axis.set(axis);
        info.length = maxT - minT;
        info.radius = Math.max(maxPerpendicularDist, 1e-9);
        info.slenderness = info.length / info.radius;

        info.isBar =
                info.length > 0.05 &&
                        info.slenderness > 6.0;

        return info;
    }

    public OctreeShapeInfo classifyOctreeShape(
            GaiaOctreeFaces octFaces,
            List<GeometryContent> faceDataList,
            List<GaiaVertex> vertices
    ) {
        OctreeShapeInfo info = new OctreeShapeInfo();

        if (faceDataList == null || faceDataList.isEmpty()) {
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

        for (GeometryContent faceData : faceDataList) {
            GaiaFaceContent faceContent = (GaiaFaceContent) faceData;
            GaiaFace face = faceContent.getFace();
            int[] indices = face.getIndices();

            if (indices == null || indices.length < 3) {
                continue;
            }

            Vector3d p0 = vertices.get(indices[0]).getPosition();
            Vector3d p1 = vertices.get(indices[1]).getPosition();
            Vector3d p2 = vertices.get(indices[2]).getPosition();

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
                if (!usedIndices.add(idx)) {
                    continue;
                }

                GaiaVertex vertex = vertices.get(idx);

                // importante: solo medir vértices que realmente caen dentro del octree
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

        double[] sizes = new double[]{
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

        boolean mostlyHorizontal =
                Math.abs(info.averageNormal.z) > 0.75;

        boolean mostlyVertical =
                Math.abs(info.averageNormal.z) < 0.35;

        // Suelo: plano horizontal, dos dimensiones dominantes, poca altura
        if (veryFlat && mostlyHorizontal && !veryLong) {
            info.type = OctreeShapeType.FLOOR;
            return info;
        }

        // Pared: plano vertical
        if (veryFlat && mostlyVertical && !veryLong) {
            info.type = OctreeShapeType.WALL;
            return info;
        }

        // Barra: alargada
        if (veryLong) {
            info.type = OctreeShapeType.BAR;
            return info;
        }

        info.type = OctreeShapeType.VOLUME;
        return info;
    }

    public void reMeshScene(GaiaScene scene,
                            GaiaStatistics sceneStatsOptional,
                            GaiaBoundingBox nodeBBoxOptional) {
        //***********************************************************
        // The scene must be Baked (all tMatrix must be identity).***
        //-----------------------------------------------------------
        if (sceneStatsOptional != null) {
            this.sceneStats = sceneStatsOptional;
        } else {
            this.sceneStats = GaiaStatistics.calculateStatistics(scene);
        }

        List<GaiaNode> nodes = scene.getNodes();
        for (GaiaNode node : nodes) {
            reMeshNode(node, scene, nodeBBoxOptional);
        }

        GaiaWeldOptions weldOptions = GaiaWeldOptions.builder().error(1e-6).checkTexCoord(false).checkNormal(false).checkColor(false).checkBatchId(false).build();
        GaiaWelder weld = new GaiaWelder(weldOptions);
        weld.apply(scene);
        GaiaSceneCleaner cleaner = new GaiaSceneCleaner();
        cleaner.apply(scene);
    }

    public void reMeshNode(GaiaNode node, GaiaScene parentScene, GaiaBoundingBox nodeBBoxOptional) {
        List<GaiaMesh> meshes = node.getMeshes();
        for (GaiaMesh mesh : meshes) {
            reMeshMesh(mesh, node, parentScene, nodeBBoxOptional);
        }

        List<GaiaNode> children = node.getChildren();
        for (GaiaNode child : children) {
            reMeshNode(child, parentScene, nodeBBoxOptional);
        }
    }

    public void reMeshMesh(GaiaMesh mesh, GaiaNode parentNode, GaiaScene parentScene, GaiaBoundingBox nodeBBoxOptional) {
        List<GaiaPrimitive> primitives = mesh.getPrimitives();
        for (GaiaPrimitive primitive : primitives) {
            reMeshPrimitive(primitive, parentNode, parentScene, nodeBBoxOptional);
        }
    }

    private boolean isFloorCandidate(OctreeShapeInfo shapeInfo, GaiaStatistics stats) {
        return shapeInfo.flatness > 5.0
                && Math.abs(shapeInfo.averageNormal.z) > 0.75
                && stats.normalVariance < 0.15;
    }

    private boolean isBarCandidate(OctreeShapeInfo shapeInfo, GaiaStatistics stats) {
        return shapeInfo.elongation > 3.0
                && shapeInfo.flatness < 10.0
                && stats.normalVariance > 0.05;
    }

    public OctreeBBoxInfo calculateBoundingBoxForLeafDistInfo(
            GaiaBoundingBox currBBox,
            double leafDist
    ) {
        if (currBBox == null || leafDist <= 0.0) {
            return new OctreeBBoxInfo(currBBox, 0, 0.0, 0.0);
        }

        double currCubeSize = currBBox.getMaxSize();

        if (currCubeSize <= 0.0) {
            return new OctreeBBoxInfo(currBBox, 0, currCubeSize, currCubeSize);
        }

        int maxDepth = 0;

        if (currCubeSize > leafDist) {
            maxDepth = (int) Math.ceil(
                    HalfEdgeUtils.log2(currCubeSize / leafDist)
            );
        }

        double rootCubeSize = leafDist * Math.pow(2.0, maxDepth);
        double leafSize = rootCubeSize / Math.pow(2.0, maxDepth);

        double cx = (currBBox.getMinX() + currBBox.getMaxX()) * 0.5;
        double cy = (currBBox.getMinY() + currBBox.getMaxY()) * 0.5;
        double cz = (currBBox.getMinZ() + currBBox.getMaxZ()) * 0.5;

        double half = rootCubeSize * 0.5;

        GaiaBoundingBox resultBBox = new GaiaBoundingBox();

        resultBBox.setMinX(cx - half);
        resultBBox.setMaxX(cx + half);

        resultBBox.setMinY(cy - half);
        resultBBox.setMaxY(cy + half);

        resultBBox.setMinZ(cz - half);
        resultBBox.setMaxZ(cz + half);

        return new OctreeBBoxInfo(
                resultBBox,
                maxDepth,
                rootCubeSize,
                leafSize
        );
    }

    private List<GeometryContent> getContentsOfParent(GaiaOctree<GeometryContent> octree) {
        List<GeometryContent> result = new ArrayList<>();
        Set<GeometryContent> used = new HashSet<>();

        if (octree == null) {
            return result;
        }

        GaiaOctree<GeometryContent> parent = octree.getParent();

        if (parent == null) {
            addUniqueContents(octree.getContents(), result, used);
            return result;
        }

        List<GaiaOctree<GeometryContent>> children = parent.getChildren();

        if (children == null || children.isEmpty()) {
            addUniqueContents(parent.getContents(), result, used);
            return result;
        }

        for (GaiaOctree<GeometryContent> child : children) {
            if (child == null) {
                continue;
            }

            addUniqueContents(child.getContents(), result, used);
        }

        return result;
    }

    private void addUniqueContents(
            List<GeometryContent> contents,
            List<GeometryContent> result,
            Set<GeometryContent> used
    ) {
        if (contents == null || contents.isEmpty()) {
            return;
        }

        for (GeometryContent content : contents) {
            if (content == null) {
                continue;
            }

            if (used.add(content)) {
                result.add(content);
            }
        }
    }

    public void reMeshPrimitive(GaiaPrimitive primitive, GaiaNode parentNode, GaiaScene parentScene, GaiaBoundingBox nodeBBoxOptional) {
        // create []weldedIndices.***
        int[] weldedIndices = new int[primitive.getVertices().size()];

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

        double cubeSize = cubeBoundingBox.getMaxSize();

        int depth = (int) Math.ceil(
                Math.log(cubeSize / limitBoxSize) / Math.log(2.0)
        );

        double desiredCubeSize = limitBoxSize * Math.pow(2.0, depth);
        double difSize = desiredCubeSize - cubeSize;
        cubeBoundingBox.expand(difSize * 0.5);

        // 1rst, find the frontier vertices and frontier faces.
        List<GaiaVertex> vertices = primitive.getVertices();
        List<GaiaFace> faces = new ArrayList<>();
        primitive.extractGaiaAllFaces(faces);
        GaiaFrontierFinder finder = new GaiaFrontierFinder();
        boolean[] frontierVertices = finder.findBoundaryVertices(vertices, faces, 1e-6, weldedIndices);

        GaiaOctreeFaces octreeFaces = new GaiaOctreeFaces(null, cubeBoundingBox);
        List<GeometryContent> faceDataList = new ArrayList<>();
        GaiaOctreeUtils.getFaceDataListOfScene(parentScene, faceDataList);
        octreeFaces.addContents(faceDataList);
        octreeFaces.setLimitDepth(limitDepth);
        octreeFaces.setLimitSize(limitBoxSize);
        octreeFaces.setLimitFacesCount(minFacesCount);
        octreeFaces.setContentsCanBeInMultipleChildren(true);
        octreeFaces.makeTree();

        List<GaiaOctree<GeometryContent>> octreesWithContent = octreeFaces.extractOctreesWithContents();

        Set<GaiaVertex> verticesToReMesh = new HashSet<>();

        List<GaiaVertex> verticesToTestShape = new ArrayList<>();
        Set<Integer> shapeVertexIndices = new HashSet<>();

        for (GaiaOctree<GeometryContent> octree : octreesWithContent) {
            GaiaOctreeFaces octFaces = (GaiaOctreeFaces) octree;

            verticesToReMesh.clear();
            verticesToTestShape.clear();
            shapeVertexIndices.clear();

            List<GeometryContent> facesDates = octree.getContents();
            int facesCount = facesDates.size();

            if (facesCount <= 7) {
                continue;
            }

            List<GaiaFace> facesOfParentOctree = new ArrayList<>();
            List<GeometryContent> parentFaces = getContentsOfParent(octree);
            for (GeometryContent faceData : parentFaces) {
                GaiaFaceContent faceContent = (GaiaFaceContent) faceData;
                GaiaFace face = faceContent.getFace();
                facesOfParentOctree.add(face);
            }
            GaiaStatistics stats = GaiaStatistics.calculateStatistics(facesOfParentOctree, vertices);

            for (int i = 0; i < facesCount; i++) {
                GeometryContent faceData = facesDates.get(i);
                GaiaFaceContent faceContent = (GaiaFaceContent) faceData;
                GaiaFace face = faceContent.getFace();
                int[] indices = face.getIndices();

                for (int j = 0; j < indices.length; j++) {
                    int vertexIndex = indices[j];
                    GaiaVertex vertex = vertices.get(vertexIndex);

                    if (!octFaces.intersects(vertex)) {
                        continue;
                    }

                    // Vertices únicos dentro del octree para analizar la forma.
                    if (shapeVertexIndices.add(vertexIndex)) {
                        verticesToTestShape.add(vertex);
                    }

                    // Los vértices frontera no se remeshean.
                    if (frontierVertices[vertexIndex]) {
                        continue;
                    }

                    verticesToReMesh.add(vertex);
                }
            }

            if (verticesToTestShape.size() < 3) {
                continue;
            }

            if (verticesToReMesh.size() < 2) {
                continue;
            }

            if (!reMeshAnyWay) {
                // Detector fuerte para barras oblicuas.
                BarInfo barInfo = detectBarByPCA(verticesToTestShape);
                if (barInfo.isBar) {
                    log.debug("Slim object detected by PCA. Skip reMesh it. slenderness = " + barInfo.slenderness);
                    continue;
                }

                OctreeShapeInfo shapeInfo = classifyOctreeShape(octFaces, facesDates, vertices);

                // Detector rápido para barra alineada al mundo.
                if (isBarCandidate(shapeInfo, stats)) {
                    log.debug("Slim object detected by AABB. Skip reMesh it");
                    continue;
                }

                if (isBuildingWallProtected(shapeInfo, stats)) {
                    log.debug("Building wall detected by AABB and statistics. Skip reMesh it");
                    continue;
                }
            }

            List<GaiaVertex> verticesSelected = verticesToReMesh.stream().toList();

            ReMesh(verticesSelected);
        }
    }

    private boolean isBuildingWallProtected(
            OctreeShapeInfo shapeInfo,
            GaiaStatistics stats
    ) {
        if (shapeInfo == null || stats == null) {
            return false;
        }

        boolean orderedNormals =
                stats.normalVariance < 0.15;

        boolean notFolded =
                stats.areaFoldRatio > 0.5 &&
                        stats.areaFoldRatio < 3.0;

        boolean enoughWallScale =
                shapeInfo.longest > 1.0 &&
                        shapeInfo.middle > 0.5;

        return orderedNormals &&
                notFolded &&
                enoughWallScale;
    }

    private boolean isChaoticZone(GaiaStatistics stats) {
        if (stats == null) {
            return false;
        }

        return stats.normalVariance > 0.05 &&
                stats.areaFoldRatio > 0.3 &&
                stats.trianglesDensity > 1.0;
    }

    public void reMeshPrimitive_original(GaiaPrimitive primitive, GaiaNode parentNode, GaiaScene parentScene, GaiaBoundingBox nodeBBoxOptional) {
        // create []weldedIndices.***
        int[] weldedIndices = new int[primitive.getVertices().size()];

        GaiaBoundingBox cubeBoundingBox = nodeBBoxOptional.clone();
        if (cubeBoundingBox == null) {
            Matrix4d mat = new Matrix4d();
            mat.identity();
            GaiaBoundingBox boundingBox = primitive.getBoundingBox(mat);
            if (boundingBox == null) {return;}

            cubeBoundingBox = boundingBox.createCubeFromMinPosition();
        }

        // 1rst, find the frontier vertices and frontier faces.
        List<GaiaVertex> vertices = primitive.getVertices();
        List<GaiaFace> faces = new ArrayList<>();
        primitive.extractGaiaAllFaces(faces);
        GaiaFrontierFinder finder = new GaiaFrontierFinder();
        boolean[] frontierVertices = finder.findBoundaryVertices(vertices, faces, 1e-6, weldedIndices);

        GaiaOctreeFaces octreeFaces = new GaiaOctreeFaces(null, cubeBoundingBox);
        List<GeometryContent> faceDataList = new ArrayList<>();
        GaiaOctreeUtils.getFaceDataListOfScene(parentScene, faceDataList);
        octreeFaces.addContents(faceDataList);
        octreeFaces.setLimitDepth(limitDepth);
        octreeFaces.setLimitSize(limitBoxSize);
        octreeFaces.setLimitFacesCount(minFacesCount);
        octreeFaces.setContentsCanBeInMultipleChildren(true);
        octreeFaces.makeTree();

        List<GaiaOctree<GeometryContent>> octreesWithContent = octreeFaces.extractOctreesWithContents();

        Set<GaiaVertex> verticesToReMesh = new HashSet<>();

        List<GaiaVertex> verticesToTestShape = new ArrayList<>();
        Set<Integer> shapeVertexIndices = new HashSet<>();

        for (GaiaOctree<GeometryContent> octree : octreesWithContent) {
            GaiaOctreeFaces octFaces = (GaiaOctreeFaces) octree;

            verticesToReMesh.clear();
            verticesToTestShape.clear();
            shapeVertexIndices.clear();

            List<GeometryContent> facesDates = octree.getContents();
            int facesCount = facesDates.size();

            if (facesCount <= 7) {
                continue;
            }

            List<GaiaFace> facesOfOctree = new ArrayList<>();

            for (int i = 0; i < facesCount; i++) {
                GeometryContent faceData = facesDates.get(i);
                GaiaFaceContent faceContent = (GaiaFaceContent) faceData;
                GaiaFace face = faceContent.getFace();
                facesOfOctree.add(face);
            }

            GaiaStatistics stats = GaiaStatistics.calculateStatistics(facesOfOctree, vertices);

//            if (stats.trianglesDensity < this.sceneStats.trianglesDensity * 0.1) {
//                continue;
//            }

            for (int i = 0; i < facesCount; i++) {
                GeometryContent faceData = facesDates.get(i);
                GaiaFaceContent faceContent = (GaiaFaceContent) faceData;
                GaiaFace face = faceContent.getFace();
                int[] indices = face.getIndices();

                for (int j = 0; j < indices.length; j++) {
                    int vertexIndex = indices[j];
                    GaiaVertex vertex = vertices.get(vertexIndex);

                    if (!octFaces.intersects(vertex)) {
                        continue;
                    }

                    // Vertices únicos dentro del octree para analizar la forma.
                    if (shapeVertexIndices.add(vertexIndex)) {
                        verticesToTestShape.add(vertex);
                    }

                    // Los vértices frontera no se remeshean.
                    if (frontierVertices[vertexIndex]) {
                        continue;
                    }

                    verticesToReMesh.add(vertex);
                }
            }

            if (verticesToTestShape.size() < 3) {
                continue;
            }

            if (!reMeshAnyWay) {
                // Detector fuerte para barras oblicuas.
                BarInfo barInfo = detectBarByPCA(verticesToTestShape);
                if (barInfo.isBar) {
                    log.debug("Slim object detected by PCA. Skip reMesh it. slenderness = " + barInfo.slenderness);
                    continue;
                }

                OctreeShapeInfo shapeInfo = classifyOctreeShape(octFaces, facesDates, vertices);

                // Detector rápido para barra alineada al mundo.
                if (isBarCandidate(shapeInfo, stats)) {
                    log.debug("Slim object detected by AABB. Skip reMesh it");
                    continue;
                }
            }

            if (verticesToReMesh.size() < 2) {
                continue;
            }

            List<GaiaVertex> verticesSelected = verticesToReMesh.stream().toList();

            ReMesh(verticesSelected);
        }
    }

    private boolean isBuildingCornerCandidate(
            List<GeometryContent> faceDataList,
            List<GaiaVertex> vertices
    ) {
        if (faceDataList == null || faceDataList.isEmpty() || vertices == null) {
            return false;
        }

        class NormalCluster {
            Vector3d normal = new Vector3d();
            double area = 0.0;
            int faceCount = 0;
        }

        List<NormalCluster> clusters = new ArrayList<>();

        double totalArea = 0.0;
        int validFaces = 0;

        double sameNormalDot = Math.cos(Math.toRadians(15.0));

        for (GeometryContent faceData : faceDataList) {
            GaiaFaceContent faceContent = (GaiaFaceContent) faceData;
            GaiaFace face = faceContent.getFace();
            if (faceData == null || face == null) {
                continue;
            }

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
            Vector3d cross = e1.cross(e2, new Vector3d());

            double area2 = cross.length();

            if (area2 < 1e-12) {
                continue;
            }

            Vector3d n = cross.normalize(new Vector3d());

            NormalCluster bestCluster = null;
            double bestDot = -1.0;

            for (NormalCluster cluster : clusters) {
                double dot = Math.abs(n.dot(cluster.normal));

                if (dot > bestDot) {
                    bestDot = dot;
                    bestCluster = cluster;
                }
            }

            if (bestCluster != null && bestDot > sameNormalDot) {
                Vector3d nn = new Vector3d(n);

                if (nn.dot(bestCluster.normal) < 0.0) {
                    nn.negate();
                }

                Vector3d weightedOld = new Vector3d(bestCluster.normal).mul(bestCluster.area);
                Vector3d weightedNew = new Vector3d(nn).mul(area2);

                bestCluster.area += area2;
                bestCluster.faceCount++;

                bestCluster.normal.set(weightedOld.add(weightedNew));

                if (bestCluster.normal.lengthSquared() > 1e-20) {
                    bestCluster.normal.normalize();
                }
            } else {
                NormalCluster cluster = new NormalCluster();
                cluster.normal.set(n);
                cluster.area = area2;
                cluster.faceCount = 1;
                clusters.add(cluster);
            }

            totalArea += area2;
            validFaces++;
        }

        if (totalArea < 1e-12 || validFaces < 4 || clusters.size() < 2) {
            return false;
        }

        List<NormalCluster> dominantClusters = new ArrayList<>();

        for (NormalCluster cluster : clusters) {
            double ratio = cluster.area / totalArea;

            if (ratio >= 0.12 && cluster.faceCount >= 2) {
                dominantClusters.add(cluster);
            }
        }

        if (dominantClusters.size() < 2) {
            return false;
        }

        double dominantArea = 0.0;

        int verticalLikeCount = 0;
        int horizontalLikeCount = 0;
        int architecturalLikeCount = 0;

        for (NormalCluster cluster : dominantClusters) {
            dominantArea += cluster.area;

            double absZ = Math.abs(cluster.normal.z);

            // Plano vertical: normal casi horizontal.
            if (absZ < 0.35) {
                verticalLikeCount++;
                architecturalLikeCount++;
            }

            // Suelo/techo: normal casi vertical.
            if (absZ > 0.75) {
                horizontalLikeCount++;
                architecturalLikeCount++;
            }
        }

        double dominantAreaRatio = dominantArea / totalArea;
        double architecturalClusterRatio =
                (double) architecturalLikeCount / (double) dominantClusters.size();

        // En arquitectura, los planos dominantes deben explicar bastante área.
        if (dominantAreaRatio < 0.60) {
            return false;
        }

        // Si la mayoría de clusters dominantes no parecen pared/suelo/techo,
        // probablemente es vegetación.
        if (architecturalClusterRatio < 0.70) {
            return false;
        }

        // Caso 1: dos o más paredes, aunque estén en zigzag.
        boolean multipleWalls = verticalLikeCount >= 2;

        // Caso 2: pared + suelo/techo.
        boolean wallAndHorizontalPlane =
                verticalLikeCount >= 1 && horizontalLikeCount >= 1;

        if (!multipleWalls && !wallAndHorizontalPlane) {
            return false;
        }

        // Debe existir al menos un par de planos claramente diferentes.
        boolean hasDifferentPlanePair = false;

        for (int i = 0; i < dominantClusters.size(); i++) {
            for (int j = i + 1; j < dominantClusters.size(); j++) {
                Vector3d n0 = dominantClusters.get(i).normal;
                Vector3d n1 = dominantClusters.get(j).normal;

                double dot = Math.abs(n0.dot(n1));

                // dot bajo: esquina cercana a 90 grados.
                // dot medio: zigzag, fachadas oblicuas, balcones, etc.
                if (dot < 0.70) {
                    hasDifferentPlanePair = true;
                    break;
                }
            }

            if (hasDifferentPlanePair) {
                break;
            }
        }

        return hasDifferentPlanePair;
    }

    private boolean isWrinkledOrganicCandidate(GaiaStatistics stats) {
        if (stats == null) {
            return false;
        }

        if (stats.normalVariance > 0.18) {
            return true;
        }

        if (stats.areaFoldRatio > 1.5 && stats.normalVariance > 0.10) {
            return true;
        }

        if (sceneStats != null &&
                stats.trianglesDensity > sceneStats.trianglesDensity * 1.1 &&
                stats.normalVariance > 0.10) {
            return true;
        }

        return false;
    }

    private boolean isArchitecturalFlatCandidate(OctreeShapeInfo shapeInfo, GaiaStatistics stats) {
        if (shapeInfo == null || stats == null) {
            return false;
        }

        // Plano claro: suelo, pared, tejado plano, fachada.
        if (shapeInfo.type == OctreeShapeType.FLOOR ||
                shapeInfo.type == OctreeShapeType.WALL) {
            return true;
        }

        // Plano muy fino aunque la clasificación no haya sido perfecta.
        if (shapeInfo.flatness > 4.0 && stats.normalVariance < 0.20) {
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

        for (int i = 0; i < count; i++) {
            GaiaVertex vertex = vertices.get(i);

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

        for (int i = 0; i < count; i++) {
            GaiaVertex vertex = vertices.get(i);

            vertex.getPosition().set(avgX, avgY, avgZ);

            if (vertex.getTexcoords() != null && texCoordCount > 0) {
                vertex.getTexcoords().set(avgU, avgV);
            }
        }
    }

    public enum OctreeShapeType {
        UNKNOWN,
        BAR,
        FLOOR,
        WALL,
        VOLUME
    }

    public static class BarInfo {
        public boolean isBar;
        public Vector3d axis = new Vector3d(1, 0, 0);
        public double length;
        public double radius;
        public double slenderness;
    }

    public static class OctreeShapeInfo {
        public OctreeShapeType type = OctreeShapeType.UNKNOWN;

        public double sizeX;
        public double sizeY;
        public double sizeZ;

        public double longest;
        public double middle;
        public double shortest;

        public double elongation; // longest / middle
        public double flatness;   // middle / shortest

        public Vector3d averageNormal = new Vector3d();
    }
}
