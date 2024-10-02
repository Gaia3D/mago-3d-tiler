package com.gaia3d.basic.halfEdgeStructure;

import com.gaia3d.basic.structure.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HalfEdgeUtils {
    public static HalfEdgeScene halfEdgeSceneFromGaiaScene(GaiaScene gaiaScene) {
        List<GaiaNode> gaiaNodes = gaiaScene.getNodes();
        int nodesCount = gaiaNodes.size(); // nodesCount must be 1. This is the root node.***
        HalfEdgeScene halfEdgeScene = new HalfEdgeScene();
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
        return null;
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

        // surfaces.***
        List<GaiaSurface> gaiaSurfaces = gaiaPrimitive.getSurfaces();
        List<GaiaVertex> gaiaVertices = gaiaPrimitive.getVertices();
        int surfacesCount = gaiaSurfaces.size();
        for (int i = 0; i < surfacesCount; i++) {
            GaiaSurface gaiaSurface = gaiaSurfaces.get(i);
            HalfEdgeSurface halfEdgeSurface = HalfEdgeUtils.halfEdgeSurfaceFromGaiaSurface(gaiaSurface, gaiaVertices);
            halfEdgePrimitive.getSurfaces().add(halfEdgeSurface);
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
