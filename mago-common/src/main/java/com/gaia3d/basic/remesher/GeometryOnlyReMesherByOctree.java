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
public class GeometryOnlyReMesherByOctree {
    private int limitDepth = 12;
    private double limitBoxSize = 0.2;
    private int minVertexCount = 20;
    private int minFacesCount = 4;
    private GaiaStatistics sceneStats = null;

    public static class BarInfo {
        public boolean isBar;
        public Vector3d axis = new Vector3d(1, 0, 0);
        public double length;
        public double radius;
        public double slenderness;
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

        public double elongation; // longest / middle
        public double flatness;   // middle / shortest

        public Vector3d averageNormal = new Vector3d();
    }

    public OctreeShapeInfo classifyOctreeShape(
            GaiaOctreeFaces octFaces,
            List<GaiaFaceData> faceDataList,
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

        for (GaiaFaceData faceData : faceDataList) {
            GaiaFace face = faceData.getFace();
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

    public GeometryOnlyReMesherByOctree() {
    }


    public void reMeshScene(GaiaScene scene, GaiaStatistics sceneStatsOptional) {
        //***********************************************************
        // The scene must be Baked (all tMatrix must be identity).***
        //-----------------------------------------------------------
        if(sceneStatsOptional != null) {
            this.sceneStats = sceneStatsOptional;
        } else {
            this.sceneStats = GaiaStatistics.calculateStatistics(scene);
        }
        //GaiaStatistics sceneStats = GaiaStatistics.calculateStatistics(scene);

        // SangAm 1 building stats sample.
//        sceneStats = {GaiaStatistics@4834}
//        areaTotal = 8549.56866980057
//        trianglesCount = 47336
//        trianglesDensity = 5.5366535819758695
//        normalVariance = 0.5396526701369196
//        verticalRange = 45.24596405029297
//        areaFoldRatio = 3.43007347681656

        List<GaiaNode> nodes = scene.getNodes();
        for(GaiaNode node : nodes) {
            reMeshNode(node, scene);
        }

        GaiaWeldOptions weldOptions = GaiaWeldOptions.builder().error(1e-6).checkTexCoord(false).checkNormal(false).checkColor(false).checkBatchId(false).build();
        GaiaWelder weld = new GaiaWelder(weldOptions);
        weld.apply(scene);
        GaiaSceneCleaner cleaner = new GaiaSceneCleaner();
        cleaner.apply(scene);
    }

    public void reMeshNode(GaiaNode node, GaiaScene parentScene){
        List<GaiaMesh>  meshes = node.getMeshes();
        for(GaiaMesh mesh : meshes) {
            reMeshMesh(mesh, node, parentScene);
        }

        List<GaiaNode> children = node.getChildren();
        for(GaiaNode child : children) {
            reMeshNode(child, parentScene);
        }
    }

    public void reMeshMesh(GaiaMesh mesh, GaiaNode parentNode, GaiaScene parentScene){
        List<GaiaPrimitive>  primitives = mesh.getPrimitives();
        for(GaiaPrimitive primitive : primitives) {
            reMeshPrimitive(primitive, parentNode, parentScene);
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

    public void reMeshPrimitive(GaiaPrimitive primitive, GaiaNode parentNode, GaiaScene parentScene) {
        // create []weldedIndices.***
        int[] weldedIndices = new int[primitive.getVertices().size()];

        Matrix4d mat = new Matrix4d();
        mat.identity();
        GaiaBoundingBox boundingBox = primitive.getBoundingBox(mat);
        if (boundingBox == null) return;

        GaiaBoundingBox cubeBoundingBox = boundingBox.createCubeFromMinPosition();

        // 1rst, find the frontier vertices and frontier faces.
        List<GaiaVertex> vertices = primitive.getVertices();
        List<GaiaFace> faces = new ArrayList<>();
        primitive.extractGaiaAllFaces(faces);
        GaiaFrontierFinder  finder = new GaiaFrontierFinder();
        boolean[] frontierVertices = finder.findBoundaryVertices(vertices, faces, 1e-6, weldedIndices);

        GaiaOctreeFaces octreeFaces = new GaiaOctreeFaces(null, cubeBoundingBox);
        List<GaiaFaceData> faceDataList = new ArrayList<>();
        GaiaOctreeUtils.getFaceDataListOfScene(parentScene, faceDataList);
        octreeFaces.addContents(faceDataList);
        octreeFaces.setLimitDepth(limitDepth);
        octreeFaces.setLimitSize(limitBoxSize);
        octreeFaces.setLimitFacesCount(minFacesCount);
        octreeFaces.setContentsCanBeInMultipleChildren(true);
        octreeFaces.makeTree();


        List<GaiaOctree<GaiaFaceData>> octreesWithContent = octreeFaces.extractOctreesWithContents();

        Set<GaiaVertex> verticesToReMesh = new HashSet<>();

        List<GaiaVertex> verticesToTestShape = new ArrayList<>();
        Set<Integer> shapeVertexIndices = new HashSet<>();

        for (GaiaOctree<GaiaFaceData> octree : octreesWithContent) {
            GaiaOctreeFaces octFaces = (GaiaOctreeFaces) octree;

            verticesToReMesh.clear();
            verticesToTestShape.clear();
            shapeVertexIndices.clear();

            List<GaiaFaceData> facesDates = octree.getContents();
            int facesCount = facesDates.size();

            if (facesCount <= 7) {
                continue;
            }

            List<GaiaFace> facesOfOctree = new ArrayList<>();

            for (int i = 0; i < facesCount; i++) {
                GaiaFaceData faceData = facesDates.get(i);
                facesOfOctree.add(faceData.getFace());
            }

            GaiaStatistics stats = GaiaStatistics.calculateStatistics(facesOfOctree, vertices);

//            if (stats.trianglesDensity < this.sceneStats.trianglesDensity * 0.1) {
//                continue;
//            }

            for (int i = 0; i < facesCount; i++) {
                GaiaFaceData faceData = facesDates.get(i);
                int[] indices = faceData.getFace().getIndices();

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

            if (verticesToReMesh.size() < 2) {
                continue;
            }

            List<GaiaVertex> verticesSelected = verticesToReMesh.stream().toList();

            ReMesh(verticesSelected);
        }
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
}
