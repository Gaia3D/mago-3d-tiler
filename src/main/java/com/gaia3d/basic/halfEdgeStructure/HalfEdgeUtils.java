package com.gaia3d.basic.halfEdgeStructure;

import com.gaia3d.basic.structure.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HalfEdgeUtils {
    public static GaiaScene gaiaSceneFromHalfEdgeScene(HalfEdgeScene halfEdgeScene) {
        GaiaScene gaiaScene = new GaiaScene();

        // set original path.***
        gaiaScene.setOriginalPath(halfEdgeScene.getOriginalPath());
        gaiaScene.setGaiaBoundingBox(halfEdgeScene.getGaiaBoundingBox().clone());
        gaiaScene.setAttribute(halfEdgeScene.getAttribute().getCopy());

        // check nodes.***
        List<HalfEdgeNode> halfEdgeNodes = halfEdgeScene.getNodes();
        int nodesCount = halfEdgeNodes.size();
        for (int i = 0; i < nodesCount; i++) {
            HalfEdgeNode halfEdgeNode = halfEdgeNodes.get(i);
            GaiaNode gaiaNode = gaiaNodeFromHalfEdgeNode(halfEdgeNode);
            gaiaScene.getNodes().add(gaiaNode);
        }

        // check materials.***
        List<GaiaMaterial> gaiaMaterials = halfEdgeScene.getMaterials();
        int materialsCount = gaiaMaterials.size();
        for (int i = 0; i < materialsCount; i++) {
            GaiaMaterial gaiaMaterial = gaiaMaterials.get(i);
            GaiaMaterial newGaiaMaterial = gaiaMaterial.clone();
            gaiaScene.getMaterials().add(newGaiaMaterial);
        }
        return gaiaScene;
    }

    public static GaiaNode gaiaNodeFromHalfEdgeNode(HalfEdgeNode halfEdgeNode) {
        GaiaNode gaiaNode = new GaiaNode();

        // check meshes.***
        List<HalfEdgeMesh> halfEdgeMeshes = halfEdgeNode.getMeshes();
        int meshesCount = halfEdgeMeshes.size();
        for (int i = 0; i < meshesCount; i++) {
            HalfEdgeMesh halfEdgeMesh = halfEdgeMeshes.get(i);
            GaiaMesh gaiaMesh = gaiaMeshFromHalfEdgeMesh(halfEdgeMesh);
            gaiaNode.getMeshes().add(gaiaMesh);
        }

        // check children.***
        List<HalfEdgeNode> halfEdgeChildren = halfEdgeNode.getChildren();
        int childrenCount = halfEdgeChildren.size();
        for (int i = 0; i < childrenCount; i++) {
            HalfEdgeNode halfEdgeChild = halfEdgeChildren.get(i);
            GaiaNode gaiaChild = gaiaNodeFromHalfEdgeNode(halfEdgeChild);
            gaiaNode.getChildren().add(gaiaChild);
        }

        return gaiaNode;
    }

    public static GaiaMesh gaiaMeshFromHalfEdgeMesh(HalfEdgeMesh halfEdgeMesh) {
        GaiaMesh gaiaMesh = new GaiaMesh();

        // primitives.***
        List<HalfEdgePrimitive> halfEdgePrimitives = halfEdgeMesh.getPrimitives();
        int primitivesCount = halfEdgePrimitives.size();
        for (int i = 0; i < primitivesCount; i++) {
            HalfEdgePrimitive halfEdgePrimitive = halfEdgePrimitives.get(i);
            GaiaPrimitive gaiaPrimitive = gaiaPrimitiveFromHalfEdgePrimitive(halfEdgePrimitive);
            gaiaMesh.getPrimitives().add(gaiaPrimitive);
        }
        return gaiaMesh;
    }

    public static GaiaPrimitive gaiaPrimitiveFromHalfEdgePrimitive(HalfEdgePrimitive halfEdgePrimitive) {
        GaiaPrimitive gaiaPrimitive = new GaiaPrimitive();

        // set accessor indices.***
        gaiaPrimitive.setAccessorIndices(halfEdgePrimitive.getAccessorIndices());
        gaiaPrimitive.setMaterialIndex(halfEdgePrimitive.getMaterialIndex());

        // surfaces.***
        List<HalfEdgeSurface> halfEdgeSurfaces = halfEdgePrimitive.getSurfaces();
        List<HalfEdgeVertex> halfEdgeVertices = halfEdgePrimitive.getVertices();
        Map<HalfEdgeVertex, GaiaVertex> mapHalfEdgeVertexToGaiaVertex = new HashMap<>();
        Map<GaiaVertex, Integer> mapGaiaVertexToIndex = new HashMap<>();

        // copy vertices.***
        int verticesCount = halfEdgeVertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex halfEdgeVertex = halfEdgeVertices.get(i);
            GaiaVertex gaiaVertex = halfEdgeVertex.toGaiaVertex();
            mapHalfEdgeVertexToGaiaVertex.put(halfEdgeVertex, gaiaVertex);
            gaiaPrimitive.getVertices().add(gaiaVertex);

            mapGaiaVertexToIndex.put(gaiaVertex, i);
        }

        int surfacesCount = halfEdgeSurfaces.size();
        for (int i = 0; i < surfacesCount; i++) {
            HalfEdgeSurface halfEdgeSurface = halfEdgeSurfaces.get(i);
            GaiaSurface gaiaSurface = gaiaSurfaceFromHalfEdgeSurface(halfEdgeSurface, mapHalfEdgeVertexToGaiaVertex, mapGaiaVertexToIndex, gaiaPrimitive);
            gaiaPrimitive.getSurfaces().add(gaiaSurface);
        }

        return gaiaPrimitive;
    }

    public static GaiaSurface gaiaSurfaceFromHalfEdgeSurface(HalfEdgeSurface halfEdgeSurface, Map<HalfEdgeVertex, GaiaVertex> mapHalfEdgeVertexToGaiaVertex, Map<GaiaVertex, Integer> mapGaiaVertexToIndex, GaiaPrimitive gaiaPrimitiveOwner) {
        GaiaSurface gaiaSurface = new GaiaSurface();

        // faces.***
        List<HalfEdgeFace> halfEdgeFaces = halfEdgeSurface.getFaces();
        int facesCount = halfEdgeFaces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace halfEdgeFace = halfEdgeFaces.get(i);
            GaiaFace gaiaFace = gaiaFaceFromHalfEdgeFace(halfEdgeFace, mapHalfEdgeVertexToGaiaVertex, mapGaiaVertexToIndex, gaiaPrimitiveOwner);
            gaiaSurface.getFaces().add(gaiaFace);
        }

        return gaiaSurface;
    }

    public static GaiaFace gaiaFaceFromHalfEdgeFace(HalfEdgeFace halfEdgeFace, Map<HalfEdgeVertex, GaiaVertex> mapHalfEdgeVertexToGaiaVertex, Map<GaiaVertex, Integer> mapGaiaVertexToIndex, GaiaPrimitive gaiaPrimitiveOwner) {
        GaiaFace gaiaFace = new GaiaFace();
        List<HalfEdgeVertex> halfEdgeVertices = halfEdgeFace.getVertices(null);
        int verticesCount = halfEdgeVertices.size();
        int[] indices = new int[verticesCount];
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex halfEdgeVertex = halfEdgeVertices.get(i);
            GaiaVertex gaiaVertex = mapHalfEdgeVertexToGaiaVertex.get(halfEdgeVertex);
            indices[i] = mapGaiaVertexToIndex.get(gaiaVertex);
            gaiaFace.setIndices(indices);
        }

        return gaiaFace;
    }

    public static HalfEdgeScene halfEdgeSceneFromGaiaScene(GaiaScene gaiaScene) {
        List<GaiaNode> gaiaNodes = gaiaScene.getNodes();
        int nodesCount = gaiaNodes.size(); // nodesCount must be 1. This is the root node.***
        HalfEdgeScene halfEdgeScene = new HalfEdgeScene();

        // set original path.***
        halfEdgeScene.setOriginalPath(gaiaScene.getOriginalPath());
        halfEdgeScene.setGaiaBoundingBox(gaiaScene.getBoundingBox().clone());
        halfEdgeScene.setAttribute(gaiaScene.getAttribute().getCopy());

        // check nodes.***
        for (int i = 0; i < nodesCount; i++) {
            GaiaNode gaiaNode = gaiaNodes.get(i);
            HalfEdgeNode halfEdgeNode = halfEdgeNodeFromGaiaNode(gaiaNode);
            halfEdgeScene.getNodes().add(halfEdgeNode);
        }

        // check materials.***
        List<GaiaMaterial> gaiaMaterials = gaiaScene.getMaterials();
        int materialsCount = gaiaMaterials.size();
        for (int i = 0; i < materialsCount; i++) {
            GaiaMaterial gaiaMaterial = gaiaMaterials.get(i);
            GaiaMaterial newGaiaMaterial = gaiaMaterial.clone();
            halfEdgeScene.getMaterials().add(newGaiaMaterial);
        }
        return halfEdgeScene;
    }

    public static HalfEdgeNode halfEdgeNodeFromGaiaNode(GaiaNode gaiaNode) {
        HalfEdgeNode halfEdgeNode = new HalfEdgeNode();

        // check meshes.***
        List<GaiaMesh> gaiaMeshes = gaiaNode.getMeshes();
        int meshesCount = gaiaMeshes.size();
        for (int i = 0; i < meshesCount; i++) {
            GaiaMesh gaiaMesh = gaiaMeshes.get(i);
            HalfEdgeMesh halfEdgeMesh = HalfEdgeUtils.halfEdgeMeshFromGaiaMesh(gaiaMesh);
            halfEdgeNode.getMeshes().add(halfEdgeMesh);
        }

        // check children.***
        List<GaiaNode> gaiaChildren = gaiaNode.getChildren();
        int childrenCount = gaiaChildren.size();
        for (int i = 0; i < childrenCount; i++) {
            GaiaNode gaiaChild = gaiaChildren.get(i);
            HalfEdgeNode halfEdgeChild = HalfEdgeUtils.halfEdgeNodeFromGaiaNode(gaiaChild);
            halfEdgeNode.getChildren().add(halfEdgeChild);
        }


        return halfEdgeNode;
    }

    public static HalfEdgeMesh halfEdgeMeshFromGaiaMesh(GaiaMesh gaiaMesh) {
        HalfEdgeMesh halfEdgeMesh = new HalfEdgeMesh();

        // primitives.***
        List< GaiaPrimitive> gaiaPrimitives = gaiaMesh.getPrimitives();
        int primitivesCount = gaiaPrimitives.size();
        for (int i = 0; i < primitivesCount; i++) {
            GaiaPrimitive gaiaPrimitive = gaiaPrimitives.get(i);
            HalfEdgePrimitive halfEdgePrimitive = HalfEdgeUtils.halfEdgePrimitiveFromGaiaPrimitive(gaiaPrimitive);
            halfEdgeMesh.getPrimitives().add(halfEdgePrimitive);
        }
        return halfEdgeMesh;
    }

    public static HalfEdgePrimitive halfEdgePrimitiveFromGaiaPrimitive(GaiaPrimitive gaiaPrimitive) {
        HalfEdgePrimitive halfEdgePrimitive = new HalfEdgePrimitive();

        // set accessor indices.***
        halfEdgePrimitive.setAccessorIndices(gaiaPrimitive.getAccessorIndices());
        halfEdgePrimitive.setMaterialIndex(gaiaPrimitive.getMaterialIndex());

        // surfaces.***
        List<GaiaSurface> gaiaSurfaces = gaiaPrimitive.getSurfaces();
        List<GaiaVertex> gaiaVertices = gaiaPrimitive.getVertices();
        int surfacesCount = gaiaSurfaces.size();
        for (int i = 0; i < surfacesCount; i++) {
            GaiaSurface gaiaSurface = gaiaSurfaces.get(i);
            HalfEdgeSurface halfEdgeSurface = HalfEdgeUtils.halfEdgeSurfaceFromGaiaSurface(gaiaSurface, gaiaVertices);
            halfEdgePrimitive.getSurfaces().add(halfEdgeSurface);
        }

        // make vertices of the primitive.***
        List<HalfEdgeSurface> halfEdgeSurfaces = halfEdgePrimitive.getSurfaces();
        int halfEdgeSurfacesCount = halfEdgeSurfaces.size();
        for (int i = 0; i < halfEdgeSurfacesCount; i++) {
            HalfEdgeSurface halfEdgeSurface = halfEdgeSurfaces.get(i);
            List<HalfEdgeVertex> halfEdgeVertices = halfEdgeSurface.getVertices();
            halfEdgePrimitive.getVertices().addAll(halfEdgeVertices);
        }

        return halfEdgePrimitive;
    }

    public static HalfEdgeSurface halfEdgeSurfaceFromGaiaSurface(GaiaSurface gaiaSurface, List<GaiaVertex> gaiaVertices) {
        HalfEdgeSurface halfEdgeSurface = new HalfEdgeSurface();
        Map<GaiaVertex, HalfEdgeVertex> mapGaiaVertexToHalfEdgeVertex = new HashMap<>();

        // faces.***
        List<GaiaFace> gaiaFaces = gaiaSurface.getFaces();
        int facesCount = gaiaFaces.size();
        for (int i = 0; i < facesCount; i++) {
            GaiaFace gaiaFace = gaiaFaces.get(i);
            HalfEdgeFace halfEdgeFace = HalfEdgeUtils.halfEdgeFaceFromGaiaFace(gaiaFace, gaiaVertices, halfEdgeSurface, mapGaiaVertexToHalfEdgeVertex);
            halfEdgeSurface.getFaces().add(halfEdgeFace);
        }

        List<HalfEdgeVertex> halfEdgeVertices = new ArrayList<>(mapGaiaVertexToHalfEdgeVertex.values());
        halfEdgeSurface.getVertices().addAll(halfEdgeVertices);

        // set twins.***
        halfEdgeSurface.setTwins();

        return halfEdgeSurface;
    }

    public static HalfEdgeFace halfEdgeFaceFromGaiaFace(GaiaFace gaiaFace, List<GaiaVertex> gaiaVertices, HalfEdgeSurface halfEdgeSurfaceOwner, Map<GaiaVertex, HalfEdgeVertex> mapGaiaVertexToHalfEdgeVertex) {
        HalfEdgeFace halfEdgeFace = new HalfEdgeFace();

        // indices.***
        List<HalfEdge> currHalfEdges = new ArrayList<>();
        int[] indices = gaiaFace.getIndices();
        int indicesCount = indices.length;
        for (int i = 0; i < indicesCount; i++) {
            int index = indices[i];
            GaiaVertex gaiaVertex = gaiaVertices.get(index);
            HalfEdgeVertex halfEdgeVertex = mapGaiaVertexToHalfEdgeVertex.get(gaiaVertex);
            if (halfEdgeVertex == null) {
                halfEdgeVertex = new HalfEdgeVertex();
                halfEdgeVertex.copyFromGaiaVertex(gaiaVertex);
                mapGaiaVertexToHalfEdgeVertex.put(gaiaVertex, halfEdgeVertex);
            }
            else {
                int hola = 0;
            }

            HalfEdge halfEdge = new HalfEdge();
            halfEdge.setStartVertex(halfEdgeVertex);
            halfEdge.setFace(halfEdgeFace);
            halfEdgeFace.setHalfEdge(halfEdge);

            currHalfEdges.add(halfEdge);
            halfEdgeSurfaceOwner.getHalfEdges().add(halfEdge);
        }

        // now set nextHalfEdges.***
        int currHalfEdgesCount = currHalfEdges.size();
        for (int i = 0; i < currHalfEdgesCount; i++) {
            HalfEdge currHalfEdge = currHalfEdges.get(i);
            HalfEdge nextHalfEdge = currHalfEdges.get((i + 1) % currHalfEdgesCount);
            currHalfEdge.setNext(nextHalfEdge);
        }

        return halfEdgeFace;
    }
}
