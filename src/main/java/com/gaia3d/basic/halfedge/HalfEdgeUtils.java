package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
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
//            String vertexNote = halfEdgeVertex.getNote();
//            if(vertexNote != null && vertexNote.equals("intersectionVertex"))
//            {
//                int hola = 0;
//            }
            GaiaVertex gaiaVertex = halfEdgeVertex.toGaiaVertex();
            mapHalfEdgeVertexToGaiaVertex.put(halfEdgeVertex, gaiaVertex);
            gaiaPrimitive.getVertices().add(gaiaVertex);

            mapGaiaVertexToIndex.put(gaiaVertex, i);
        }

        if(mapHalfEdgeVertexToGaiaVertex.size() == 0)
        {
            int hola = 0;
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
//            if(gaiaFace == null)
//            {
//                continue;
//            }
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
            if (gaiaVertex == null) {
                int hola = 0;
            }
            if (mapGaiaVertexToIndex.get(gaiaVertex) == null) {
                int hola = 0;
            }
            indices[i] = mapGaiaVertexToIndex.get(gaiaVertex);
            gaiaFace.setIndices(indices);
        }

        return gaiaFace;
    }

    public static HalfEdgeScene getHalfEdgeSceneRectangularNet(int numCols, int numRows) {
        HalfEdgeScene halfEdgeScene = new HalfEdgeScene();
        HalfEdgeNode halfEdgeNode = new HalfEdgeNode();
        halfEdgeScene.getNodes().add(halfEdgeNode);

        HalfEdgeMesh halfEdgeMesh = new HalfEdgeMesh();
        halfEdgeNode.getMeshes().add(halfEdgeMesh);

        HalfEdgePrimitive halfEdgePrimitive = new HalfEdgePrimitive();
        halfEdgeMesh.getPrimitives().add(halfEdgePrimitive);

        GaiaPrimitive gaiaPrimitive = new GaiaPrimitive();
        halfEdgePrimitive.setMaterialIndex(0);
        halfEdgePrimitive.setAccessorIndices(0);
        halfEdgePrimitive.getSurfaces().add(new HalfEdgeSurface());

//        GaiaSurface gaiaSurface = new GaiaSurface();
//        gaiaPrimitive.getSurfaces().add(gaiaSurface);
//
//        GaiaFace gaiaFace = new GaiaFace();
//        gaiaSurface.getFaces().add(gaiaFace);
//
//        List<GaiaVertex> gaiaVertices = new ArrayList<>();
//        for(int i=0; i<numRows; i++)
//        {
//            for(int j=0; j<numCols; j++)
//            {
//                GaiaVertex gaiaVertex = new GaiaVertex();
//                gaiaVertex.setPosition(new double[]{j, i, 0});
//                gaiaVertices.add(gaiaVertex);
//            }
//        }
//
//        halfEdgePrimitive.setVertices(gaiaVertices);

        return halfEdgeScene;
    }

    public static HalfEdgeScene halfEdgeSceneFromGaiaScene(GaiaScene gaiaScene) {
        List<GaiaNode> gaiaNodes = gaiaScene.getNodes();
        int nodesCount = gaiaNodes.size(); // nodesCount must be 1. This is the root node.***
        HalfEdgeScene halfEdgeScene = new HalfEdgeScene();

        // set original path.***
        halfEdgeScene.setOriginalPath(gaiaScene.getOriginalPath());
        halfEdgeScene.setGaiaBoundingBox(gaiaScene.getBoundingBox().clone());
        halfEdgeScene.setAttribute(gaiaScene.getAttribute().getCopy());

        // copy gaiaAttributes.***
        GaiaAttribute gaiaAttribute = gaiaScene.getAttribute();
        GaiaAttribute newGaiaAttribute = gaiaAttribute.getCopy();
        halfEdgeScene.setAttribute(newGaiaAttribute);

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

        // 1rst, copy transform matrices.***
        Matrix4d transformMatrix = gaiaNode.getTransformMatrix();
        Matrix4d preMultipliedTransformMatrix = gaiaNode.getPreMultipliedTransformMatrix();

        Matrix4d transformMatrixCopy = new Matrix4d();
        transformMatrixCopy.set(transformMatrix);

        Matrix4d preMultipliedTransformMatrixCopy = new Matrix4d();
        preMultipliedTransformMatrixCopy.set(preMultipliedTransformMatrix);

        halfEdgeNode.setTransformMatrix(transformMatrixCopy);
        halfEdgeNode.setPreMultipliedTransformMatrix(preMultipliedTransformMatrixCopy);

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
            halfEdgeChild.setParent(halfEdgeNode);
            halfEdgeNode.getChildren().add(halfEdgeChild);
        }


        return halfEdgeNode;
    }

    public static HalfEdgeMesh halfEdgeMeshFromGaiaMesh(GaiaMesh gaiaMesh) {
        HalfEdgeMesh halfEdgeMesh = new HalfEdgeMesh();

        // primitives.***
        List<GaiaPrimitive> gaiaPrimitives = gaiaMesh.getPrimitives();
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
        if (surfacesCount > 1) {
            int hola = 0;
        }
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

    public List<GaiaFace> getGaiaTriangleFacesFromGaiaFace(GaiaFace gaiaFace) {
        List<GaiaFace> gaiaFaces = new ArrayList<>();
        int[] indices = gaiaFace.getIndices();
        Vector3d normal = gaiaFace.getFaceNormal();
        int indicesCount = indices.length;
        for (int i = 0; i < indicesCount; i += 3) {
            GaiaFace gaiaTriangleFace = new GaiaFace();
            gaiaTriangleFace.setIndices(new int[]{indices[i], indices[i + 1], indices[i + 2]});
            if(normal != null)
            {
                gaiaTriangleFace.setFaceNormal(new Vector3d(normal));
            }
            gaiaFaces.add(gaiaTriangleFace);
        }
        return gaiaFaces;
    }

    public static HalfEdgeSurface halfEdgeSurfaceFromGaiaSurface(GaiaSurface gaiaSurface, List<GaiaVertex> gaiaVertices) {
        HalfEdgeSurface halfEdgeSurface = new HalfEdgeSurface();
        Map<GaiaVertex, HalfEdgeVertex> mapGaiaVertexToHalfEdgeVertex = new HashMap<>();

        // faces.***
        List<GaiaFace> gaiaFaces = gaiaSurface.getFaces();
        int facesCount = gaiaFaces.size();
        for (int i = 0; i < facesCount; i++) {
            GaiaFace gaiaFace = gaiaFaces.get(i);
            List<GaiaFace> gaiaTriangleFaces = new HalfEdgeUtils().getGaiaTriangleFacesFromGaiaFace(gaiaFace);
            int triangleFacesCount = gaiaTriangleFaces.size();
            for (int j = 0; j < triangleFacesCount; j++) {
                GaiaFace gaiaTriangleFace = gaiaTriangleFaces.get(j);
                HalfEdgeFace halfEdgeFace = HalfEdgeUtils.halfEdgeFaceFromGaiaFace(gaiaTriangleFace, gaiaVertices, halfEdgeSurface, mapGaiaVertexToHalfEdgeVertex);
                halfEdgeSurface.getFaces().add(halfEdgeFace);
            }
            // old.***
//            HalfEdgeFace halfEdgeFace = HalfEdgeUtils.halfEdgeFaceFromGaiaFace(gaiaFace, gaiaVertices, halfEdgeSurface, mapGaiaVertexToHalfEdgeVertex);
//            halfEdgeSurface.getFaces().add(halfEdgeFace);
        }

        List<HalfEdgeVertex> halfEdgeVertices = new ArrayList<>(mapGaiaVertexToHalfEdgeVertex.values());
        halfEdgeSurface.getVertices().addAll(halfEdgeVertices);

        // set twins.***
        halfEdgeSurface.setTwins();
        halfEdgeSurface.checkSandClockFaces();
        halfEdgeSurface.TEST_checkEqualHEdges();

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
            } else {
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

    private static HalfEdgePrimitive getCopyHalfEdgePrimitive(HalfEdgePrimitive halfEdgePrimitive)
    {
        HalfEdgePrimitive copyHalfEdgePrimitive = new HalfEdgePrimitive();

        // copy surfaces.***
        List<HalfEdgeSurface> halfEdgeSurfaces = halfEdgePrimitive.getSurfaces();
        int surfacesCount = halfEdgeSurfaces.size();
        for (int i = 0; i < surfacesCount; i++) {
            HalfEdgeSurface halfEdgeSurface = halfEdgeSurfaces.get(i);
            HalfEdgeSurface newHalfEdgeSurface = getCopyHalfEdgeSurface(halfEdgeSurface);
            copyHalfEdgePrimitive.getSurfaces().add(newHalfEdgeSurface);
        }

        return copyHalfEdgePrimitive;
    }

    private static HalfEdgeSurface getCopyHalfEdgeSurface(HalfEdgeSurface halfEdgeSurface)
    {
        HalfEdgeSurface copyHalfEdgeSurface = new HalfEdgeSurface();
        halfEdgeSurface.removeDeletedObjects();
        halfEdgeSurface.setObjectIdsInList();

        // 1rst, copy vertices.***
        List<HalfEdgeVertex> halfEdgeVertices = halfEdgeSurface.getVertices();
        int verticesCount = halfEdgeVertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex halfEdgeVertex = halfEdgeVertices.get(i);
            HalfEdgeVertex newHalfEdgeVertex = new HalfEdgeVertex();
            newHalfEdgeVertex.copyFrom(halfEdgeVertex);
            newHalfEdgeVertex.setOutingHalfEdge(null); // no copy halfEdgeStructure pointers.***

            copyHalfEdgeSurface.getVertices().add(newHalfEdgeVertex);
        }

        // copy faces.***
        List<HalfEdgeFace> halfEdgeFaces = halfEdgeSurface.getFaces();
        int facesCount = halfEdgeFaces.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace halfEdgeFace = halfEdgeFaces.get(i);
            HalfEdgeFace newHalfEdgeFace = new HalfEdgeFace();
            newHalfEdgeFace.setId(halfEdgeFace.getId());
            newHalfEdgeFace.setClassifyId(halfEdgeFace.getClassifyId());
            copyHalfEdgeSurface.getFaces().add(newHalfEdgeFace);
        }

        // copy halfEdges.***
        List<HalfEdge> halfEdges = halfEdgeSurface.getHalfEdges();
        int halfEdgesCount = halfEdges.size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            HalfEdge newHalfEdge = new HalfEdge();
            newHalfEdge.setId(halfEdge.getId());
            copyHalfEdgeSurface.getHalfEdges().add(newHalfEdge);
        }

        // set startVertex & face to halfEdges.***
        List<HalfEdgeVertex> copyHalfEdgeVertices = copyHalfEdgeSurface.getVertices();
        List<HalfEdgeFace> copyHalfEdgeFaces = copyHalfEdgeSurface.getFaces();
        List<HalfEdge> copyHalfEdges = copyHalfEdgeSurface.getHalfEdges();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            HalfEdge copyHalfEdge = copyHalfEdges.get(i);

            // startVertex.***
            int startVertexId = halfEdge.getStartVertex().getId();
            if(startVertexId < 0)
            {
                log.error("startVertexId < 0.***");
                int hola = 0;
            }
            HalfEdgeVertex copyStartVertex = copyHalfEdgeVertices.get(startVertexId);
            copyHalfEdge.setStartVertex(copyStartVertex);
            copyStartVertex.setOutingHalfEdge(copyHalfEdge);

            // face.***
            int faceId = halfEdge.getFace().getId();
            int classifyId = halfEdge.getFace().getClassifyId();
            HalfEdgeFace copyFace = copyHalfEdgeFaces.get(faceId);
            copyHalfEdge.setFace(copyFace);
            copyFace.setHalfEdge(copyHalfEdge);

            // twin.***
            HalfEdge twin = halfEdge.getTwin();
            if(twin != null && twin.getStatus() != ObjectStatus.DELETED)
            {
                int twinId = twin.getId();
                // check the twin's face classifyId.***
                HalfEdgeFace twinFace = twin.getFace();
                if(twinFace == null)
                {
                    log.error("twinFace is null.***");
                }
                int twinFaceClassifyId = twin.getFace().getClassifyId();
                if(twinFaceClassifyId == classifyId)
                {
                    HalfEdge copyTwin = copyHalfEdges.get(twinId);
                    copyHalfEdge.setTwin(copyTwin);
                }
            }

            // next.***
            HalfEdge next = halfEdge.getNext();
            if(next == null) {
                log.error("next is null.***");
            }
            else {
                int nextId = next.getId();
                HalfEdge copyNext = copyHalfEdges.get(nextId);
                copyHalfEdge.setNext(copyNext);
            }

        }

        return copyHalfEdgeSurface;
    }

    public static List<HalfEdgeScene> getCopyHalfEdgeScenesByFaceClassifyId(HalfEdgeScene halfEdgeScene, List<HalfEdgeScene> resultHalfEdgeScenes)
    {
        if(resultHalfEdgeScenes == null)
        {
            resultHalfEdgeScenes = new ArrayList<>();
        }

        Map<Integer, HalfEdgeScene> mapClassifyIdToHalfEdgeScene = new HashMap<>();
        GaiaAttribute gaiaAttribute = halfEdgeScene.getAttribute();

        int nodesCount = halfEdgeScene.getNodes().size();
        for(int j=0; j<nodesCount; j++)
        {
            HalfEdgeNode rootNode = halfEdgeScene.getNodes().get(j);
            Map<Integer,HalfEdgeNode> mapClassifyIdToNode = getMapHalfEdgeNodeByFaceClassifyId(rootNode, null);
            for(Integer key : mapClassifyIdToNode.keySet())
            {
                int faceClassifyId = key;
                HalfEdgeNode halfEdgeNode = mapClassifyIdToNode.get(faceClassifyId);
                HalfEdgeScene halfEdgeSceneCopy = mapClassifyIdToHalfEdgeScene.get(faceClassifyId);
                if(halfEdgeSceneCopy == null)
                {
                    halfEdgeSceneCopy = new HalfEdgeScene();

                    // copy original path.***
                    halfEdgeSceneCopy.setOriginalPath(halfEdgeScene.getOriginalPath());

                    // copy gaiaAttributes.***
                    GaiaAttribute newGaiaAttribute = gaiaAttribute.getCopy();
                    halfEdgeSceneCopy.setAttribute(newGaiaAttribute);

                    mapClassifyIdToHalfEdgeScene.put(faceClassifyId, halfEdgeSceneCopy);
                }
                halfEdgeSceneCopy.getNodes().add(halfEdgeNode);
            }

        }

        for(Integer key : mapClassifyIdToHalfEdgeScene.keySet())
        {
            HalfEdgeScene halfEdgeSceneCopy = mapClassifyIdToHalfEdgeScene.get(key);

            // copy materials.***
            List<GaiaMaterial> gaiaMaterials = halfEdgeScene.getMaterials();
            int materialsCount = gaiaMaterials.size();
            for(int i=0; i<materialsCount; i++)
            {
                GaiaMaterial gaiaMaterial = gaiaMaterials.get(i);
                GaiaMaterial newGaiaMaterial = gaiaMaterial.clone();
                halfEdgeSceneCopy.getMaterials().add(newGaiaMaterial);
            }
            resultHalfEdgeScenes.add(halfEdgeSceneCopy);
        }

        return resultHalfEdgeScenes;
    }

    private static Map<Integer,HalfEdgeNode> getMapHalfEdgeNodeByFaceClassifyId(HalfEdgeNode halfEdgeNode, Map<Integer,HalfEdgeNode> resultClassifyIdToNode)
    {
        if(resultClassifyIdToNode == null)
        {
            resultClassifyIdToNode = new HashMap<>();
        }

        List<HalfEdgeMesh> halfEdgeMeshes = halfEdgeNode.getMeshes();
        int meshesCount = halfEdgeMeshes.size();
        for(int i=0; i<meshesCount; i++)
        {
            HalfEdgeMesh halfEdgeMesh = halfEdgeMeshes.get(i);
            Map<Integer, HalfEdgeMesh> mapClassifyIdToMesh = getMapHalfEdgeMeshByFaceClassifyId(halfEdgeMesh, null);
            for(Integer key : mapClassifyIdToMesh.keySet())
            {
                int faceClassifyId = key;
                HalfEdgeMesh newHalfEdgeMesh = mapClassifyIdToMesh.get(faceClassifyId);
                HalfEdgeNode newHalfEdgeNode = resultClassifyIdToNode.get(faceClassifyId);
                if(newHalfEdgeNode == null)
                {
                    newHalfEdgeNode = new HalfEdgeNode();

                    // copy transform matrices.***
                    Matrix4d transformMatrix = halfEdgeNode.getTransformMatrix();
                    Matrix4d preMultipliedTransformMatrix = halfEdgeNode.getPreMultipliedTransformMatrix();

                    Matrix4d transformMatrixCopy = new Matrix4d();
                    transformMatrixCopy.set(transformMatrix);

                    Matrix4d preMultipliedTransformMatrixCopy = new Matrix4d();
                    preMultipliedTransformMatrixCopy.set(preMultipliedTransformMatrix);

                    newHalfEdgeNode.setTransformMatrix(transformMatrixCopy);
                    newHalfEdgeNode.setPreMultipliedTransformMatrix(preMultipliedTransformMatrixCopy);

                    resultClassifyIdToNode.put(faceClassifyId, newHalfEdgeNode);
                }
                newHalfEdgeNode.getMeshes().add(newHalfEdgeMesh);
            }
        }

        // check children.***
        List<HalfEdgeNode> children = halfEdgeNode.getChildren();
        int childrenCount = children.size();
        for(int i=0; i<childrenCount; i++)
        {
            HalfEdgeNode child = children.get(i);
            Map<Integer, HalfEdgeNode> mapClassifyIdToNode = getMapHalfEdgeNodeByFaceClassifyId(child, null);
            for(Integer key : mapClassifyIdToNode.keySet())
            {
                int faceClassifyId = key;
                HalfEdgeNode newHalfEdgeNode = mapClassifyIdToNode.get(faceClassifyId);
                HalfEdgeNode newHalfEdgeNodeParent = resultClassifyIdToNode.get(faceClassifyId);
                if(newHalfEdgeNodeParent == null)
                {
                    newHalfEdgeNodeParent = new HalfEdgeNode();
                    resultClassifyIdToNode.put(faceClassifyId, newHalfEdgeNodeParent);
                }
                newHalfEdgeNodeParent.getChildren().add(newHalfEdgeNode);
            }
        }

        return resultClassifyIdToNode;
    }


    private static Map<Integer, HalfEdgeMesh> getMapHalfEdgeMeshByFaceClassifyId(HalfEdgeMesh halfEdgeMesh, Map<Integer, HalfEdgeMesh> resultMap)
    {
        if(resultMap == null)
        {
            resultMap = new HashMap<>();
        }


        List<HalfEdgePrimitive> halfEdgePrimitives = halfEdgeMesh.getPrimitives();
        int primitivesCount = halfEdgePrimitives.size();
        for(int i=0; i<primitivesCount; i++)
        {
            HalfEdgePrimitive halfEdgePrimitive = halfEdgePrimitives.get(i);
            Map<Integer, HalfEdgePrimitive> mapClassifyIdToPrimitive = getMapHalfEdgePrimitiveByFaceClassifyId(halfEdgePrimitive, null);
            for(Integer key : mapClassifyIdToPrimitive.keySet())
            {
                int faceClassifyId = key;
                HalfEdgePrimitive newHalfEdgePrimitive = mapClassifyIdToPrimitive.get(faceClassifyId);
                HalfEdgeMesh newHalfEdgeMesh = resultMap.get(faceClassifyId);
                if(newHalfEdgeMesh == null)
                {
                    newHalfEdgeMesh = new HalfEdgeMesh();
                    resultMap.put(faceClassifyId, newHalfEdgeMesh);
                }
                newHalfEdgeMesh.getPrimitives().add(newHalfEdgePrimitive);
            }

        }

        return resultMap;
    }



    private static Map<Integer, HalfEdgePrimitive> getMapHalfEdgePrimitiveByFaceClassifyId(HalfEdgePrimitive halfEdgePrimitive, Map<Integer, HalfEdgePrimitive> resultMap)
    {
        if(resultMap == null)
        {
            resultMap = new HashMap<>();
        }

        List<HalfEdgeSurface> halfEdgeSurfaces = halfEdgePrimitive.getSurfaces();
        int surfacesCount = halfEdgeSurfaces.size();
        for(int i=0; i<surfacesCount; i++)
        {
            HalfEdgeSurface halfEdgeSurface = halfEdgeSurfaces.get(i);
            Map<Integer, HalfEdgeSurface> mapClassifyIdToSurface = getMapHalfEdgeSurfaceByFaceClassifyId(halfEdgeSurface, null);
            for(Integer key : mapClassifyIdToSurface.keySet())
            {
                int faceClassifyId = key;
                HalfEdgeSurface newHalfEdgeSurface = mapClassifyIdToSurface.get(faceClassifyId);
                HalfEdgePrimitive newHalfEdgePrimitive = resultMap.get(faceClassifyId);
                if(newHalfEdgePrimitive == null)
                {
                    newHalfEdgePrimitive = new HalfEdgePrimitive();

                    // set accessor indices & materialId.***
                    newHalfEdgePrimitive.setAccessorIndices(halfEdgePrimitive.getAccessorIndices());
                    newHalfEdgePrimitive.setMaterialIndex(halfEdgePrimitive.getMaterialIndex());

                    resultMap.put(faceClassifyId, newHalfEdgePrimitive);
                }
                newHalfEdgePrimitive.getSurfaces().add(newHalfEdgeSurface);
            }
        }

        return resultMap;

    }

    private static Map<Integer, HalfEdgeSurface> getMapHalfEdgeSurfaceByFaceClassifyId(HalfEdgeSurface halfEdgeSurface, Map<Integer, HalfEdgeSurface> resultHalfEdgeSurfaces) {
        if(resultHalfEdgeSurfaces == null)
        {
            resultHalfEdgeSurfaces = new HashMap<>();
        }

        Map<Integer, List<HalfEdge>> mapFaceClassifyIdToHalfEdges = new HashMap<>();

        int halfEdgesCount = halfEdgeSurface.getHalfEdges().size();
        for(int i=0; i<halfEdgesCount; i++)
        {
            HalfEdge halfEdge = halfEdgeSurface.getHalfEdges().get(i);
            HalfEdgeFace face = halfEdge.getFace();
            int faceClassifyId = face.getClassifyId();
            List<HalfEdge> halfEdges = mapFaceClassifyIdToHalfEdges.computeIfAbsent(faceClassifyId, k -> new ArrayList<>());
            halfEdges.add(halfEdge);
        }

        int faceClassifyIdsCount = mapFaceClassifyIdToHalfEdges.size();
        for(Integer key : mapFaceClassifyIdToHalfEdges.keySet())
        {
            int faceClassifyId = key;
            List<HalfEdge> halfEdges = mapFaceClassifyIdToHalfEdges.get(faceClassifyId);
            HalfEdgeSurface newHalfEdgeSurface = createCopyHalfEdgeSurfaceFromHalfEdgesList(halfEdges);
            resultHalfEdgeSurfaces.put(faceClassifyId, newHalfEdgeSurface);
        }

        return resultHalfEdgeSurfaces;
    }

    private static HalfEdgeSurface createCopyHalfEdgeSurfaceFromHalfEdgesList(List<HalfEdge> halfEdges)
    {
        // create a temp surface.***
        HalfEdgeSurface halfEdgeSurfaceTemp = new HalfEdgeSurface();
        Map<HalfEdgeVertex, HalfEdgeVertex> mapUniqueHalfEdgeVertex = new HashMap<>();
        Map<HalfEdgeFace, HalfEdgeFace> mapUniqueHalfEdgeFace = new HashMap<>();
        int halfEdgesCount = halfEdges.size();
        for(int i=0; i<halfEdgesCount; i++)
        {
            HalfEdge halfEdge = halfEdges.get(i);
            halfEdgeSurfaceTemp.getHalfEdges().add(halfEdge);

            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            mapUniqueHalfEdgeVertex.put(startVertex, startVertex);

            HalfEdgeFace face = halfEdge.getFace();
            mapUniqueHalfEdgeFace.put(face, face);
        }

        // set unique vertices.***
        List<HalfEdgeVertex> uniqueVertices = new ArrayList<>(mapUniqueHalfEdgeVertex.keySet());
        halfEdgeSurfaceTemp.getVertices().addAll(uniqueVertices);

        // set unique faces.***
        List<HalfEdgeFace> uniqueFaces = new ArrayList<>(mapUniqueHalfEdgeFace.keySet());
        halfEdgeSurfaceTemp.getFaces().addAll(uniqueFaces);

        halfEdgeSurfaceTemp.removeDeletedObjects();
        halfEdgeSurfaceTemp.setObjectIdsInList();

        // create a copy of the surface.***
        return getCopyHalfEdgeSurface(halfEdgeSurfaceTemp);
    }


}