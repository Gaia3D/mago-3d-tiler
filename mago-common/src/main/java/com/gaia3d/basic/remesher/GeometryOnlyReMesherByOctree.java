package com.gaia3d.basic.remesher;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.modifier.topology.GaiaSceneCleaner;
import com.gaia3d.basic.geometry.modifier.topology.GaiaWeldOptions;
import com.gaia3d.basic.geometry.modifier.topology.GaiaWelder;
import com.gaia3d.basic.geometry.octree.GaiaFaceData;
import com.gaia3d.basic.geometry.octree.GaiaOctree;
import com.gaia3d.basic.geometry.octree.GaiaOctreeFaces;
import com.gaia3d.basic.model.*;
import com.gaia3d.util.GaiaOctreeUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Getter
@Setter
public class GeometryOnlyReMesherByOctree {
    private int limitDepth = 12;
    private double limitBoxSize = 0.2;
    private int minVertexCount = 20;
    private int minFacesCount = 4;

    public GeometryOnlyReMesherByOctree() {
    }

    public GeometryOnlyReMesherByOctree(int limitDepth, double limitBoxSize, int minVertexCount) {
        this.limitDepth = limitDepth;
        this.limitBoxSize = limitBoxSize;
        this.minVertexCount = minVertexCount;
    }

    public void reMeshScene(GaiaScene scene) {
        //***********************************************************
        // The scene must be Baked (all tMatrix must be identity).***
        //-----------------------------------------------------------
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

        for(GaiaOctree<GaiaFaceData> octree : octreesWithContent) {
            GaiaOctreeFaces octFaces = (GaiaOctreeFaces)octree;
            verticesToReMesh.clear();
            List<GaiaFaceData> facesDates = octree.getContents();
            int facesCount = facesDates.size();
            if(facesCount <= 7){
                continue;
            }
            List<GaiaFace> facesOfOctree = new ArrayList<>();

            for(int i=0; i<facesCount; i++) {
                GaiaFaceData faceData = facesDates.get(i);
                facesOfOctree.add(faceData.getFace());
            }
// stats sample.
//            stats = {GaiaStatistics@4806}
//            areaTotal = 0.5175006421919343
//            trianglesCount = 5
//            trianglesDensity = 9.661823758946303
//            normalVariance = 0.06857522576908927
//            verticalRange = 1.048004150390625
//            areaFoldRatio = 0.7135929144674774
            GaiaStatistics stats = GaiaStatistics.calculateStatistics(facesOfOctree, vertices);
            if(stats.trianglesDensity > 6.0) {
                for (int i = 0; i < facesCount; i++) {
                    GaiaFaceData faceData = facesDates.get(i);
                    int[] indices = faceData.getFace().getIndices();
                    int indicesCount = indices.length;
                    for (int j = 0; j < indicesCount; j++) {
                        GaiaVertex vertex = vertices.get(indices[j]);
                        if(!octFaces.intersects(vertex)) {
                            // if the vertex is out of octree, then continue.
                            continue;
                        }

                        // check if is frontier-vertex.
                        if (frontierVertices[indices[j]]) {
                            // if is frontier-vertex, do not move it.
                            continue;
                        }

                        verticesToReMesh.add(vertex);
                    }
                }

                if(verticesToReMesh.size() < 2) {
                    continue;
                }

                List<GaiaVertex> verticesSelected = verticesToReMesh.stream().toList();
                ReMesh(verticesSelected);
            }
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
