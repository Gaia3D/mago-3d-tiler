package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaPlane;
import com.gaia3d.basic.geometry.octree.GaiaOctreeVertices;
import com.gaia3d.basic.model.*;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class HalfEdgeUtils {
    public static GaiaScene gaiaSceneFromHalfEdgeScene(HalfEdgeScene halfEdgeScene) {
        GaiaScene gaiaScene = new GaiaScene();

        // set original path
        gaiaScene.setOriginalPath(halfEdgeScene.getOriginalPath());
        gaiaScene.setGaiaBoundingBox(halfEdgeScene.getGaiaBoundingBox().clone());
        gaiaScene.setAttribute(halfEdgeScene.getAttribute().getCopy());

        // check nodes
        List<HalfEdgeNode> halfEdgeNodes = halfEdgeScene.getNodes();
        for (HalfEdgeNode halfEdgeNode : halfEdgeNodes) {
            GaiaNode gaiaNode = gaiaNodeFromHalfEdgeNode(halfEdgeNode);
            gaiaScene.getNodes().add(gaiaNode);
        }

        // check materials
        List<GaiaMaterial> gaiaMaterials = halfEdgeScene.getMaterials();
        for (GaiaMaterial gaiaMaterial : gaiaMaterials) {
            GaiaMaterial newGaiaMaterial = gaiaMaterial.clone();
            gaiaScene.getMaterials().add(newGaiaMaterial);
        }
        return gaiaScene;
    }


    public static GaiaScene gaiaSceneFromHalfEdgeFaces(List<HalfEdgeFace> halfEdgeFaces, Map<GaiaFace, HalfEdgeFace> mapGaiaFaceToHalfEdgeFace) {
        GaiaScene gaiaScene = new GaiaScene();
        GaiaNode gaiaRootNode = new GaiaNode();
        gaiaScene.getNodes().add(gaiaRootNode);
        GaiaNode gaiaNode = new GaiaNode();
        gaiaRootNode.getChildren().add(gaiaNode);

        GaiaMesh gaiaMesh = new GaiaMesh();
        gaiaNode.getMeshes().add(gaiaMesh);
        GaiaPrimitive gaiaPrimitive = new GaiaPrimitive();
        gaiaMesh.getPrimitives().add(gaiaPrimitive);
        GaiaSurface gaiaSurface = new GaiaSurface();
        gaiaPrimitive.getSurfaces().add(gaiaSurface);

        // make halfEdgeVertices
        List<HalfEdgeVertex> halfEdgeVertices = new ArrayList<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> mapUniqueHalfEdgeVertex = new HashMap<>();
        for (HalfEdgeFace halfEdgeFace : halfEdgeFaces) {
            halfEdgeVertices.clear();
            halfEdgeFace.getVertices(halfEdgeVertices);

            for (HalfEdgeVertex halfEdgeVertex : halfEdgeVertices) {
                mapUniqueHalfEdgeVertex.put(halfEdgeVertex, halfEdgeVertex);
            }
        }

        halfEdgeVertices.clear();
        halfEdgeVertices.addAll(mapUniqueHalfEdgeVertex.keySet());

        Map<HalfEdgeVertex, Integer> mapHalfEdgeVertexToIndex = new HashMap<>();
        for (int i = 0; i < halfEdgeVertices.size(); i++) {
            HalfEdgeVertex halfEdgeVertex = halfEdgeVertices.get(i);
            mapHalfEdgeVertexToIndex.put(halfEdgeVertex, i);
        }

        // copy vertices
        List<GaiaVertex> gaiaVertices = new ArrayList<>();
        for (HalfEdgeVertex halfEdgeVertex : halfEdgeVertices) {
            GaiaVertex gaiaVertex = halfEdgeVertex.toGaiaVertex();
            gaiaVertices.add(gaiaVertex);
        }

        // make faces
        for (HalfEdgeFace halfEdgeFace : halfEdgeFaces) {
            GaiaFace gaiaFace = new GaiaFace();
            List<HalfEdgeVertex> faceVertices = halfEdgeFace.getVertices(null);
            int verticesCount = faceVertices.size();
            int[] indices = new int[verticesCount];
            for (int i = 0; i < verticesCount; i++) {
                HalfEdgeVertex halfEdgeVertex = faceVertices.get(i);
                int index = mapHalfEdgeVertexToIndex.get(halfEdgeVertex);
                indices[i] = index;
            }
            gaiaFace.setIndices(indices);
            gaiaFace.setClassifyId(halfEdgeFace.getClassifyId());
            gaiaSurface.getFaces().add(gaiaFace);

            mapGaiaFaceToHalfEdgeFace.put(gaiaFace, halfEdgeFace);
        }

        gaiaPrimitive.setVertices(gaiaVertices);

        return gaiaScene;
    }

    public static GaiaNode gaiaNodeFromHalfEdgeNode(HalfEdgeNode halfEdgeNode) {
        GaiaNode gaiaNode = new GaiaNode();

        // check meshes
        List<HalfEdgeMesh> halfEdgeMeshes = halfEdgeNode.getMeshes();
        for (HalfEdgeMesh halfEdgeMesh : halfEdgeMeshes) {
            GaiaMesh gaiaMesh = gaiaMeshFromHalfEdgeMesh(halfEdgeMesh);
            gaiaNode.getMeshes().add(gaiaMesh);
        }

        // check children
        List<HalfEdgeNode> halfEdgeChildren = halfEdgeNode.getChildren();
        for (HalfEdgeNode halfEdgeChild : halfEdgeChildren) {
            GaiaNode gaiaChild = gaiaNodeFromHalfEdgeNode(halfEdgeChild);
            gaiaNode.getChildren().add(gaiaChild);
        }

        return gaiaNode;
    }

    public static GaiaMesh gaiaMeshFromHalfEdgeMesh(HalfEdgeMesh halfEdgeMesh) {
        GaiaMesh gaiaMesh = new GaiaMesh();

        // primitives
        List<HalfEdgePrimitive> halfEdgePrimitives = halfEdgeMesh.getPrimitives();
        for (HalfEdgePrimitive halfEdgePrimitive : halfEdgePrimitives) {
            GaiaPrimitive gaiaPrimitive = gaiaPrimitiveFromHalfEdgePrimitive(halfEdgePrimitive);
            gaiaMesh.getPrimitives().add(gaiaPrimitive);
        }
        return gaiaMesh;
    }

    public static GaiaPrimitive gaiaPrimitiveFromHalfEdgePrimitive(HalfEdgePrimitive halfEdgePrimitive) {
        GaiaPrimitive gaiaPrimitive = new GaiaPrimitive();

        // set accessor indices
        gaiaPrimitive.setAccessorIndices(halfEdgePrimitive.getAccessorIndices());
        gaiaPrimitive.setMaterialIndex(halfEdgePrimitive.getMaterialIndex());

        // surfaces
        List<HalfEdgeSurface> halfEdgeSurfaces = halfEdgePrimitive.getSurfaces();
        List<HalfEdgeVertex> halfEdgeVertices = halfEdgePrimitive.getVertices();

        Map<HalfEdgeVertex, GaiaVertex> mapHalfEdgeVertexToGaiaVertex = new HashMap<>();
        Map<GaiaVertex, Integer> mapGaiaVertexToIndex = new HashMap<>();

        // copy vertices
        int verticesCount = halfEdgeVertices.size();
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex halfEdgeVertex = halfEdgeVertices.get(i);
            GaiaVertex gaiaVertex = halfEdgeVertex.toGaiaVertex();
            mapHalfEdgeVertexToGaiaVertex.put(halfEdgeVertex, gaiaVertex);
            gaiaPrimitive.getVertices().add(gaiaVertex);

            mapGaiaVertexToIndex.put(gaiaVertex, i);
        }

        for (HalfEdgeSurface halfEdgeSurface : halfEdgeSurfaces) {
            GaiaSurface gaiaSurface = gaiaSurfaceFromHalfEdgeSurface(halfEdgeSurface, mapHalfEdgeVertexToGaiaVertex, mapGaiaVertexToIndex);
            gaiaPrimitive.getSurfaces().add(gaiaSurface);
        }

        return gaiaPrimitive;
    }

    public static GaiaSurface gaiaSurfaceFromHalfEdgeSurface(HalfEdgeSurface halfEdgeSurface, Map<HalfEdgeVertex, GaiaVertex> mapHalfEdgeVertexToGaiaVertex, Map<GaiaVertex, Integer> mapGaiaVertexToIndex) {
        GaiaSurface gaiaSurface = new GaiaSurface();

        // faces
        List<HalfEdgeFace> halfEdgeFaces = halfEdgeSurface.getFaces();
        for (HalfEdgeFace halfEdgeFace : halfEdgeFaces) {
            GaiaFace gaiaFace = gaiaFaceFromHalfEdgeFace(halfEdgeFace, mapHalfEdgeVertexToGaiaVertex, mapGaiaVertexToIndex);
            if (gaiaFace == null) {
                continue;
            }
            gaiaSurface.getFaces().add(gaiaFace);
        }

        return gaiaSurface;
    }

    public static GaiaFace gaiaFaceFromHalfEdgeFace(HalfEdgeFace halfEdgeFace, Map<HalfEdgeVertex, GaiaVertex> mapHalfEdgeVertexToGaiaVertex, Map<GaiaVertex, Integer> mapGaiaVertexToIndex) {
        if (halfEdgeFace == null) {
            return null;
        }

        if (halfEdgeFace.getStatus() == ObjectStatus.DELETED) {
            return null;
        }

        if (halfEdgeFace.isDegenerated()) {
            halfEdgeFace.isDegenerated();
            return null;
        }

        GaiaFace gaiaFace = new GaiaFace();
        List<HalfEdgeVertex> halfEdgeVertices = halfEdgeFace.getVertices(null);
        int verticesCount = halfEdgeVertices.size();
        int[] indices = new int[verticesCount];
        int indicesCount = 0;
        for (int i = 0; i < verticesCount; i++) {
            HalfEdgeVertex halfEdgeVertex = halfEdgeVertices.get(i);
            GaiaVertex gaiaVertex = mapHalfEdgeVertexToGaiaVertex.get(halfEdgeVertex);
            if (gaiaVertex == null) {
                continue;
            }
            indices[i] = mapGaiaVertexToIndex.get(gaiaVertex);
            indicesCount++;
        }

        if (indicesCount > 2) {
            gaiaFace.setIndices(indices);
        } else {
            gaiaFace = null;
        }

        return gaiaFace;
    }

    private static HalfEdgeSurface getHalfEdgeSurfaceRegularNet(int numCols, int numRows, float[][] depthValues, GaiaBoundingBox bbox) {
        HalfEdgeSurface halfEdgeSurface = new HalfEdgeSurface();
        double minX = bbox.getMinX();
        double minY = bbox.getMinY();
        double maxX = bbox.getMaxX();
        double maxY = bbox.getMaxY();
        double minZ = bbox.getMinZ();
        double maxZ = bbox.getMaxZ();

        double xStep = (maxX - minX) / (numCols - 1);
        double yStep = (maxY - minY) / (numRows - 1);

        // create vertices
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                double x = minX + c * xStep;
                double y = minY + r * yStep;
                int rInv = numRows - 1 - r;
                double depthValue = depthValues[c][rInv];

                double depthValueInv = 1.0 - depthValue;
                double z = minZ + (maxZ - minZ) * depthValueInv;
                HalfEdgeVertex halfEdgeVertex = new HalfEdgeVertex();
                halfEdgeVertex.setPosition(new Vector3d(x, y, z));

                // calculate texCoords
                double s = (double) c / (double) (numCols - 1);
                double t = (double) r / (double) (numRows - 1);
                halfEdgeVertex.setTexcoords(new Vector2d(s, 1.0 - t));

                if (depthValue >= 1.0) {
                    // this is noData
                    halfEdgeVertex.setStatus(ObjectStatus.DELETED);
                }

                halfEdgeSurface.getVertices().add(halfEdgeVertex);
            }
        }

        // check if some vertices are created
        if (halfEdgeSurface.getVertices().isEmpty()) {
            return null;
        }

        // create halfEdges & halfEdgeFaces
        for (int r = 0; r < numRows - 1; r++) {
            for (int c = 0; c < numCols - 1; c++) {
                HalfEdgeFace faceA = new HalfEdgeFace();
                HalfEdgeFace faceB = new HalfEdgeFace();
                int index1 = r * numCols + c;
                int index2 = r * numCols + c + 1;
                int index3 = (r + 1) * numCols + c + 1;
                int index4 = (r + 1) * numCols + c;

                HalfEdgeVertex vertex1 = halfEdgeSurface.getVertices().get(index1);
                HalfEdgeVertex vertex2 = halfEdgeSurface.getVertices().get(index2);
                HalfEdgeVertex vertex3 = halfEdgeSurface.getVertices().get(index3);
                HalfEdgeVertex vertex4 = halfEdgeSurface.getVertices().get(index4);

                if (vertex1.getStatus() != ObjectStatus.DELETED && vertex2.getStatus() != ObjectStatus.DELETED && vertex3.getStatus() != ObjectStatus.DELETED) {
                    // face A
                    HalfEdge halfEdgeA1 = new HalfEdge();
                    HalfEdge halfEdgeA2 = new HalfEdge();
                    HalfEdge halfEdgeA3 = new HalfEdge();
                    halfEdgeA1.setStartVertex(vertex1);
                    halfEdgeA2.setStartVertex(vertex2);
                    halfEdgeA3.setStartVertex(vertex3);
                    halfEdgeA1.setNext(halfEdgeA2);
                    halfEdgeA2.setNext(halfEdgeA3);
                    halfEdgeA3.setNext(halfEdgeA1);
                    halfEdgeA1.setFace(faceA);
                    halfEdgeA2.setFace(faceA);
                    halfEdgeA3.setFace(faceA);
                    vertex1.setOutingHalfEdge(halfEdgeA1);
                    vertex2.setOutingHalfEdge(halfEdgeA2);
                    vertex3.setOutingHalfEdge(halfEdgeA3);
                    faceA.setHalfEdge(halfEdgeA1);
                    halfEdgeSurface.getHalfEdges().add(halfEdgeA1);
                    halfEdgeSurface.getHalfEdges().add(halfEdgeA2);
                    halfEdgeSurface.getHalfEdges().add(halfEdgeA3);
                    halfEdgeSurface.getFaces().add(faceA);
                }

                if (vertex1.getStatus() != ObjectStatus.DELETED && vertex3.getStatus() != ObjectStatus.DELETED && vertex4.getStatus() != ObjectStatus.DELETED) {

                    // face B
                    HalfEdge halfEdgeB1 = new HalfEdge();
                    HalfEdge halfEdgeB2 = new HalfEdge();
                    HalfEdge halfEdgeB3 = new HalfEdge();
                    halfEdgeB1.setStartVertex(vertex1);
                    halfEdgeB2.setStartVertex(vertex3);
                    halfEdgeB3.setStartVertex(vertex4);
                    halfEdgeB1.setNext(halfEdgeB2);
                    halfEdgeB2.setNext(halfEdgeB3);
                    halfEdgeB3.setNext(halfEdgeB1);
                    halfEdgeB1.setFace(faceB);
                    halfEdgeB2.setFace(faceB);
                    halfEdgeB3.setFace(faceB);
                    vertex1.setOutingHalfEdge(halfEdgeB1);
                    vertex3.setOutingHalfEdge(halfEdgeB2);
                    vertex4.setOutingHalfEdge(halfEdgeB3);
                    faceB.setHalfEdge(halfEdgeB1);
                    halfEdgeSurface.getHalfEdges().add(halfEdgeB1);
                    halfEdgeSurface.getHalfEdges().add(halfEdgeB2);
                    halfEdgeSurface.getHalfEdges().add(halfEdgeB3);
                    halfEdgeSurface.getFaces().add(faceB);
                }
            }
        }

        halfEdgeSurface.setTwins();
        halfEdgeSurface.removeDeletedObjects();

        // check if exist geometry
        if (halfEdgeSurface.getVertices().isEmpty() || halfEdgeSurface.getHalfEdges().isEmpty() || halfEdgeSurface.getFaces().isEmpty()) {
            return null;
        }

        return halfEdgeSurface;
    }

    public static HalfEdgeScene getHalfEdgeSceneRectangularNet(int numCols, int numRows, float[][] depthValues, GaiaBoundingBox bbox) {
        // Create halfEdgeScene
        HalfEdgeScene halfEdgeScene = new HalfEdgeScene();
        GaiaAttribute gaiaAttribute = new GaiaAttribute();
        halfEdgeScene.setAttribute(gaiaAttribute);
        String originalPath = "";
        halfEdgeScene.setOriginalPath(Path.of(originalPath));

        // Create root node
        HalfEdgeNode halfEdgeRootNode = new HalfEdgeNode();
        halfEdgeScene.getNodes().add(halfEdgeRootNode);

        // Create node
        HalfEdgeNode halfEdgeNode = new HalfEdgeNode();
        halfEdgeRootNode.getChildren().add(halfEdgeNode);

        // Create mesh
        HalfEdgeMesh halfEdgeMesh = new HalfEdgeMesh();
        halfEdgeNode.getMeshes().add(halfEdgeMesh);

        // Create primitive
        HalfEdgePrimitive halfEdgePrimitive = new HalfEdgePrimitive();
        halfEdgeMesh.getPrimitives().add(halfEdgePrimitive);

        // Create surface
        HalfEdgeSurface halfEdgeSurface = getHalfEdgeSurfaceRegularNet(numCols, numRows, depthValues, bbox);
        if (halfEdgeSurface == null) {
            return null;
        }

        halfEdgePrimitive.getSurfaces().add(halfEdgeSurface);

        return halfEdgeScene;
    }

    public static HalfEdgeScene halfEdgeSceneFromGaiaScene(GaiaScene gaiaScene) {
        List<GaiaNode> gaiaNodes = gaiaScene.getNodes();
        int nodesCount = gaiaNodes.size(); // nodesCount must be 1. This is the root node
        HalfEdgeScene halfEdgeScene = new HalfEdgeScene();

        // set original path
        Path originalPath = gaiaScene.getOriginalPath();
        if (originalPath == null) {
            originalPath = Path.of("");
        }
        halfEdgeScene.setOriginalPath(originalPath);
        halfEdgeScene.setGaiaBoundingBox(gaiaScene.getBoundingBox().clone());
        halfEdgeScene.setAttribute(gaiaScene.getAttribute().getCopy());

        // copy gaiaAttributes
        GaiaAttribute gaiaAttribute = gaiaScene.getAttribute();
        GaiaAttribute newGaiaAttribute = gaiaAttribute.getCopy();
        halfEdgeScene.setAttribute(newGaiaAttribute);

        // check nodes
        for (int i = 0; i < nodesCount; i++) {
            GaiaNode gaiaNode = gaiaNodes.get(i);
            HalfEdgeNode halfEdgeNode = halfEdgeNodeFromGaiaNode(gaiaNode);
            halfEdgeScene.getNodes().add(halfEdgeNode);
        }

        // check materials
        List<GaiaMaterial> gaiaMaterials = gaiaScene.getMaterials();
        for (GaiaMaterial gaiaMaterial : gaiaMaterials) {
            GaiaMaterial newGaiaMaterial = gaiaMaterial.clone();
            halfEdgeScene.getMaterials().add(newGaiaMaterial);
        }

        return halfEdgeScene;
    }

    public static HalfEdgeNode halfEdgeNodeFromGaiaNode(GaiaNode gaiaNode) {
        HalfEdgeNode halfEdgeNode = new HalfEdgeNode();

        // 1rst, copy transform matrices
        Matrix4d transformMatrix = gaiaNode.getTransformMatrix();
        Matrix4d preMultipliedTransformMatrix = gaiaNode.getPreMultipliedTransformMatrix();

        Matrix4d transformMatrixCopy = new Matrix4d();
        transformMatrixCopy.set(transformMatrix);

        Matrix4d preMultipliedTransformMatrixCopy = new Matrix4d();
        preMultipliedTransformMatrixCopy.set(preMultipliedTransformMatrix);

        halfEdgeNode.setTransformMatrix(transformMatrixCopy);
        halfEdgeNode.setPreMultipliedTransformMatrix(preMultipliedTransformMatrixCopy);

        // check meshes
        List<GaiaMesh> gaiaMeshes = gaiaNode.getMeshes();
        for (GaiaMesh gaiaMesh : gaiaMeshes) {
            if (gaiaMesh == null) {
                log.error("[ERROR] gaiaMesh == null");
                continue;
            }
            HalfEdgeMesh halfEdgeMesh = HalfEdgeUtils.halfEdgeMeshFromGaiaMesh(gaiaMesh);
            halfEdgeNode.getMeshes().add(halfEdgeMesh);
        }

        // check children
        List<GaiaNode> gaiaChildren = gaiaNode.getChildren();
        for (GaiaNode gaiaChild : gaiaChildren) {
            if (gaiaChild == null) {
                log.error("[ERROR] gaiaChild == null");
                continue;
            }
            HalfEdgeNode halfEdgeChild = HalfEdgeUtils.halfEdgeNodeFromGaiaNode(gaiaChild);
            halfEdgeChild.setParent(halfEdgeNode);
            halfEdgeNode.getChildren().add(halfEdgeChild);
        }


        return halfEdgeNode;
    }

    public static HalfEdgeMesh halfEdgeMeshFromGaiaMesh(GaiaMesh gaiaMesh) {
        HalfEdgeMesh halfEdgeMesh = new HalfEdgeMesh();

        // primitives
        List<GaiaPrimitive> gaiaPrimitives = gaiaMesh.getPrimitives();
        for (GaiaPrimitive gaiaPrimitive : gaiaPrimitives) {
            if (gaiaPrimitive == null) {
                log.error("[ERROR] gaiaPrimitive == null");
                continue;
            }
            HalfEdgePrimitive halfEdgePrimitive = HalfEdgeUtils.halfEdgePrimitiveFromGaiaPrimitive(gaiaPrimitive);
            halfEdgeMesh.getPrimitives().add(halfEdgePrimitive);
        }
        return halfEdgeMesh;
    }

    public static HalfEdgePrimitive halfEdgePrimitiveFromGaiaPrimitive(GaiaPrimitive gaiaPrimitive) {
        HalfEdgePrimitive halfEdgePrimitive = new HalfEdgePrimitive();

        // set accessor indices
        halfEdgePrimitive.setAccessorIndices(gaiaPrimitive.getAccessorIndices());
        halfEdgePrimitive.setMaterialIndex(gaiaPrimitive.getMaterialIndex());

        // surfaces
        List<GaiaSurface> gaiaSurfaces = gaiaPrimitive.getSurfaces();
        List<GaiaVertex> gaiaVertices = gaiaPrimitive.getVertices();
        for (GaiaSurface gaiaSurface : gaiaSurfaces) {
            if (gaiaSurface == null) {
                log.error("[ERROR] gaiaSurface == null");
                continue;
            }
            HalfEdgeSurface halfEdgeSurface = HalfEdgeUtils.halfEdgeSurfaceFromGaiaSurface(gaiaSurface, gaiaVertices);
            halfEdgePrimitive.getSurfaces().add(halfEdgeSurface);
        }

        // make vertices of the primitive
        List<HalfEdgeSurface> halfEdgeSurfaces = halfEdgePrimitive.getSurfaces();
        for (HalfEdgeSurface halfEdgeSurface : halfEdgeSurfaces) {
            List<HalfEdgeVertex> halfEdgeVertices = halfEdgeSurface.getVertices();
            halfEdgePrimitive.getVertices().addAll(halfEdgeVertices);
        }

        return halfEdgePrimitive;
    }

    public static double calculateAngleBetweenNormals(Vector3d normalA, Vector3d normalB) {
        double dotProduct = normalA.dot(normalB);
        return Math.acos(dotProduct);
    }

    public static List<List<HalfEdgeFace>> getWeldedFacesGroups(List<HalfEdgeFace> facesList, List<List<HalfEdgeFace>> resultWeldedFacesGroups) {
        if (resultWeldedFacesGroups == null) {
            resultWeldedFacesGroups = new ArrayList<>();
        }

        Map<HalfEdgeVertex, List<HalfEdgeFace>> vertexFacesMap = new HashMap<>();
        for (HalfEdgeFace face : facesList) {
            List<HalfEdgeVertex> vertices = face.getVertices(null);
            for (HalfEdgeVertex vertex : vertices) {
                List<HalfEdgeFace> facesOfVertex = vertexFacesMap.computeIfAbsent(vertex, k -> new ArrayList<>());
                facesOfVertex.add(face);
            }
        }

        Map<HalfEdgeFace, HalfEdgeFace> mapVisitedFaces = new HashMap<>();
        int facesCount = facesList.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = facesList.get(i);
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (mapVisitedFaces.containsKey(face)) {
                continue;
            }

            List<HalfEdgeFace> weldedFaces = new ArrayList<>();
            getWeldedFacesWithFace(face, weldedFaces, mapVisitedFaces);

            resultWeldedFacesGroups.add(weldedFaces);
        }

        return resultWeldedFacesGroups;
    }

    public static boolean getWeldedFacesWithFace(HalfEdgeFace face, List<HalfEdgeFace> resultWeldedFaces, Map<HalfEdgeFace, HalfEdgeFace> mapVisitedFaces) {
        List<HalfEdgeFace> weldedFacesAux = new ArrayList<>();
        List<HalfEdgeFace> faces = new ArrayList<>();
        faces.add(face);
        //mapVisitedFaces.put(face, face);
        boolean finished = false;
        int counter = 0;
        while (!finished)// && counter < 10000000)
        {
            List<HalfEdgeFace> newAddedfaces = new ArrayList<>();
            int facesCount = faces.size();
            for (int i = 0; i < facesCount; i++) {
                HalfEdgeFace currFace = faces.get(i);
                if (currFace.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }

                if (mapVisitedFaces.containsKey(currFace)) {
                    continue;
                }

                resultWeldedFaces.add(currFace);
                mapVisitedFaces.put(currFace, currFace);
                weldedFacesAux.clear();
                currFace.getWeldedFaces(weldedFacesAux, mapVisitedFaces);
                newAddedfaces.addAll(weldedFacesAux);
            }

            if (newAddedfaces.isEmpty()) {
                finished = true;
            } else {
                faces.clear();
                faces.addAll(newAddedfaces);
            }

            counter++;
        }


        return true;
    }

    public static List<HalfEdgeVertex> getVerticesOfFaces(List<HalfEdgeFace> faces, List<HalfEdgeVertex> resultVertices) {
        Map<HalfEdgeVertex, HalfEdgeVertex> MapVertices = new HashMap<>();
        if (resultVertices == null) {
            resultVertices = new ArrayList<>();
        }
        for (HalfEdgeFace face : faces) {
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            List<HalfEdgeVertex> faceVertices = face.getVertices(null);
            for (HalfEdgeVertex vertex : faceVertices) {
                if (MapVertices.containsKey(vertex)) {
                    continue;
                }
                resultVertices.add(vertex);
                MapVertices.put(vertex, vertex);
            }
        }

        //resultVertices.addAll(MapVertices.values());
        return resultVertices;
    }

    public static List<HalfEdge> getHalfEdgesOfFaces(List<HalfEdgeFace> faces, List<HalfEdge> resultHalfEdges) {
        Map<HalfEdge, HalfEdge> MapHalfEdges = new HashMap<>();
        if (resultHalfEdges == null) {
            resultHalfEdges = new ArrayList<>();
        }
        List<HalfEdge> faceHalfEdges = new ArrayList<>();
        for (HalfEdgeFace face : faces) {
            faceHalfEdges.clear();
            faceHalfEdges = face.getHalfEdgesLoop(faceHalfEdges);
            for (HalfEdge halfEdge : faceHalfEdges) {
                if (MapHalfEdges.containsKey(halfEdge)) {
                    continue;
                }
                resultHalfEdges.add(halfEdge);
                MapHalfEdges.put(halfEdge, halfEdge);
            }
        }

        //resultHalfEdges.addAll(MapHalfEdges.values());
        return resultHalfEdges;
    }

    public static HalfEdgeSurface halfEdgeSurfaceFromGaiaSurface(GaiaSurface gaiaSurface, List<GaiaVertex> gaiaVertices) {
        HalfEdgeSurface halfEdgeSurface = new HalfEdgeSurface();
        Map<GaiaVertex, HalfEdgeVertex> mapGaiaVertexToHalfEdgeVertex = new HashMap<>();

        // faces
        List<GaiaFace> gaiaFaces = gaiaSurface.getFaces();
        for (GaiaFace gaiaFace : gaiaFaces) {
            if (gaiaFace == null) {
                log.error("[ERROR] gaiaFace == null");
                continue;
            }
            List<GaiaFace> gaiaTriangleFaces = new HalfEdgeUtils().getGaiaTriangleFacesFromGaiaFace(gaiaFace);
            for (GaiaFace gaiaTriangleFace : gaiaTriangleFaces) {
                if (gaiaTriangleFace == null) {
                    continue;
                }
                HalfEdgeFace halfEdgeFace = HalfEdgeUtils.halfEdgeFaceFromGaiaFace(gaiaTriangleFace, gaiaVertices, halfEdgeSurface, mapGaiaVertexToHalfEdgeVertex);
                halfEdgeSurface.getFaces().add(halfEdgeFace);
            }
        }

        List<HalfEdgeVertex> halfEdgeVertices = new ArrayList<>(mapGaiaVertexToHalfEdgeVertex.values());
        halfEdgeSurface.getVertices().addAll(halfEdgeVertices);

        // set twins
        halfEdgeSurface.setTwins();
        halfEdgeSurface.checkSandClockFaces();
        //halfEdgeSurface.TEST_checkEqualHEdges();

        return halfEdgeSurface;
    }

    public static HalfEdgeFace halfEdgeFaceFromGaiaFace(GaiaFace gaiaFace, List<GaiaVertex> gaiaVertices, HalfEdgeSurface halfEdgeSurfaceOwner, Map<GaiaVertex, HalfEdgeVertex> mapGaiaVertexToHalfEdgeVertex) {
        HalfEdgeFace halfEdgeFace = new HalfEdgeFace();

        // indices
        List<HalfEdge> currHalfEdges = new ArrayList<>();
        int[] indices = gaiaFace.getIndices();
        for (int index : indices) {
            if (index >= gaiaVertices.size()) {
                log.error("[ERROR] index >= gaiaVertices.size()");
            }
            GaiaVertex gaiaVertex = gaiaVertices.get(index);
            HalfEdgeVertex halfEdgeVertex = mapGaiaVertexToHalfEdgeVertex.get(gaiaVertex);
            if (halfEdgeVertex == null) {
                halfEdgeVertex = new HalfEdgeVertex();
                halfEdgeVertex.copyFromGaiaVertex(gaiaVertex);
                mapGaiaVertexToHalfEdgeVertex.put(gaiaVertex, halfEdgeVertex);
            }

            HalfEdge halfEdge = new HalfEdge();
            halfEdge.setStartVertex(halfEdgeVertex);
            halfEdge.setFace(halfEdgeFace);
            halfEdgeFace.setHalfEdge(halfEdge);

            currHalfEdges.add(halfEdge);
            halfEdgeSurfaceOwner.getHalfEdges().add(halfEdge);
        }

        // now set nextHalfEdges
        int currHalfEdgesCount = currHalfEdges.size();
        for (int i = 0; i < currHalfEdgesCount; i++) {
            HalfEdge currHalfEdge = currHalfEdges.get(i);
            HalfEdge nextHalfEdge = currHalfEdges.get((i + 1) % currHalfEdgesCount);
            currHalfEdge.setNext(nextHalfEdge);
        }

        return halfEdgeFace;
    }

    private static HalfEdgePrimitive getCopyHalfEdgePrimitive(HalfEdgePrimitive halfEdgePrimitive) {
        HalfEdgePrimitive copyHalfEdgePrimitive = new HalfEdgePrimitive();

        // copy surfaces
        List<HalfEdgeSurface> halfEdgeSurfaces = halfEdgePrimitive.getSurfaces();
        for (HalfEdgeSurface halfEdgeSurface : halfEdgeSurfaces) {
            HalfEdgeSurface newHalfEdgeSurface = getCopyHalfEdgeSurface(halfEdgeSurface);
            copyHalfEdgePrimitive.getSurfaces().add(newHalfEdgeSurface);
        }

        return copyHalfEdgePrimitive;
    }

    private static HalfEdgeSurface getCopyHalfEdgeSurface(HalfEdgeSurface halfEdgeSurface) {
        HalfEdgeSurface copyHalfEdgeSurface = new HalfEdgeSurface();
        halfEdgeSurface.removeDeletedObjects();
        halfEdgeSurface.setObjectIdsInList();

        // 1rst, copy vertices
        List<HalfEdgeVertex> halfEdgeVertices = halfEdgeSurface.getVertices();
        for (HalfEdgeVertex halfEdgeVertex : halfEdgeVertices) {
            HalfEdgeVertex newHalfEdgeVertex = new HalfEdgeVertex();
            newHalfEdgeVertex.copyFrom(halfEdgeVertex);
            newHalfEdgeVertex.setOutingHalfEdge(null); // no copy halfEdgeStructure pointers
            copyHalfEdgeSurface.getVertices().add(newHalfEdgeVertex);
        }

        // copy faces
        List<HalfEdgeFace> halfEdgeFaces = halfEdgeSurface.getFaces();
        for (HalfEdgeFace halfEdgeFace : halfEdgeFaces) {
            HalfEdgeFace newHalfEdgeFace = new HalfEdgeFace();
            newHalfEdgeFace.setId(halfEdgeFace.getId());
            newHalfEdgeFace.setClassifyId(halfEdgeFace.getClassifyId());
            copyHalfEdgeSurface.getFaces().add(newHalfEdgeFace);
        }

        // copy halfEdges
        List<HalfEdge> halfEdges = halfEdgeSurface.getHalfEdges();
        int halfEdgesCount = halfEdges.size();
        for (HalfEdge halfEdge : halfEdges) {
            HalfEdge newHalfEdge = new HalfEdge();
            newHalfEdge.setId(halfEdge.getId());
            copyHalfEdgeSurface.getHalfEdges().add(newHalfEdge);
        }

        // set startVertex & face to halfEdges
        List<HalfEdgeVertex> copyHalfEdgeVertices = copyHalfEdgeSurface.getVertices();
        List<HalfEdgeFace> copyHalfEdgeFaces = copyHalfEdgeSurface.getFaces();
        List<HalfEdge> copyHalfEdges = copyHalfEdgeSurface.getHalfEdges();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdges.get(i);
            HalfEdge copyHalfEdge = copyHalfEdges.get(i);

            // startVertex
            int startVertexId = halfEdge.getStartVertex().getId();
            HalfEdgeVertex copyStartVertex = copyHalfEdgeVertices.get(startVertexId);
            copyHalfEdge.setStartVertex(copyStartVertex);
            copyStartVertex.setOutingHalfEdge(copyHalfEdge);

            // face
            int faceId = halfEdge.getFace().getId();
            int classifyId = halfEdge.getFace().getClassifyId();
            HalfEdgeFace copyFace = copyHalfEdgeFaces.get(faceId);
            copyHalfEdge.setFace(copyFace);
            copyFace.setHalfEdge(copyHalfEdge);

            // twin
            HalfEdge twin = halfEdge.getTwin();
            if (twin != null && twin.getStatus() != ObjectStatus.DELETED) {
                int twinId = twin.getId();
                // check the twin's face classifyId
                HalfEdgeFace twinFace = twin.getFace();
                if (twinFace == null) {
                    log.error("[ERROR] twinFace is null.");
                }
                int twinFaceClassifyId = twin.getFace().getClassifyId();
                if (twinFaceClassifyId == classifyId) {
                    HalfEdge copyTwin = copyHalfEdges.get(twinId);
                    copyHalfEdge.setTwin(copyTwin);
                }
            }

            // next
            HalfEdge next = halfEdge.getNext();
            if (next == null) {
                log.error("[ERROR] next is null");
            } else {
                int nextId = next.getId();
                HalfEdge copyNext = copyHalfEdges.get(nextId);
                copyHalfEdge.setNext(copyNext);
            }

        }

        return copyHalfEdgeSurface;
    }

    public static List<HalfEdgeScene> getCopyHalfEdgeScenesByFaceClassifyId(HalfEdgeScene halfEdgeScene, List<HalfEdgeScene> resultHalfEdgeScenes) {
        // TEST FUNCTION
        if (resultHalfEdgeScenes == null) {
            resultHalfEdgeScenes = new ArrayList<>();
        }

        Map<Integer, HalfEdgeScene> mapClassifyIdToHalfEdgeScene = new HashMap<>();
        GaiaAttribute gaiaAttribute = halfEdgeScene.getAttribute();

        // test : delete faces with classifyId = 1
        HalfEdgeScene halfEdgeScene1 = halfEdgeScene.clone();
        HalfEdgeScene halfEdgeScene2 = halfEdgeScene.clone();

        halfEdgeScene1.deleteFacesWithClassifyId(2);
        halfEdgeScene2.deleteFacesWithClassifyId(1);

        if (halfEdgeScene1.getTrianglesCount() > 0) {
            resultHalfEdgeScenes.add(halfEdgeScene1);
        }

        if (halfEdgeScene2.getTrianglesCount() > 0) {
            resultHalfEdgeScenes.add(halfEdgeScene2);
        }


//        List<HalfEdgeNode> halfEdgeNodes = halfEdgeScene.getNodes();
//        int nodesCount = halfEdgeNodes.size();
//        for (int j=0; j<nodesCount; j++)
//        {
//            HalfEdgeNode rootNode = halfEdgeNodes.get(j);
//            Map<Integer,HalfEdgeNode> mapClassifyIdToNode = getMapHalfEdgeNodeByFaceClassifyId(rootNode, null);
//            for (Integer key : mapClassifyIdToNode.keySet())
//            {
//                int faceClassifyId = key;
//                HalfEdgeNode halfEdgeNode = mapClassifyIdToNode.get(faceClassifyId);
//                HalfEdgeScene halfEdgeSceneCopy = mapClassifyIdToHalfEdgeScene.get(faceClassifyId);
//                if (halfEdgeSceneCopy == null)
//                {
//                    halfEdgeSceneCopy = new HalfEdgeScene();
//
//                    // copy original path
//                    halfEdgeSceneCopy.setOriginalPath(halfEdgeScene.getOriginalPath());
//
//                    // copy gaiaAttributes
//                    GaiaAttribute newGaiaAttribute = gaiaAttribute.getCopy();
//                    halfEdgeSceneCopy.setAttribute(newGaiaAttribute);
//
//                    mapClassifyIdToHalfEdgeScene.put(faceClassifyId, halfEdgeSceneCopy);
//                }
//                halfEdgeSceneCopy.getNodes().add(halfEdgeNode);
//            }
//
//        }
//
//        for (Integer key : mapClassifyIdToHalfEdgeScene.keySet())
//        {
//            HalfEdgeScene halfEdgeSceneCopy = mapClassifyIdToHalfEdgeScene.get(key);
//
//            // copy materials
//            List<GaiaMaterial> gaiaMaterials = halfEdgeScene.getMaterials();
//            int materialsCount = gaiaMaterials.size();
//            for (int i=0; i<materialsCount; i++)
//            {
//                GaiaMaterial gaiaMaterial = gaiaMaterials.get(i);
//                GaiaMaterial newGaiaMaterial = gaiaMaterial.clone();
//                halfEdgeSceneCopy.getMaterials().add(newGaiaMaterial);
//            }
//            resultHalfEdgeScenes.add(halfEdgeSceneCopy);
//        }

        return resultHalfEdgeScenes;
    }

    public static List<HalfEdgeScene> getCopyHalfEdgeScenesByFaceClassifyId_original(HalfEdgeScene halfEdgeScene, List<HalfEdgeScene> resultHalfEdgeScenes) {
        if (resultHalfEdgeScenes == null) {
            resultHalfEdgeScenes = new ArrayList<>();
        }

        Map<Integer, HalfEdgeScene> mapClassifyIdToHalfEdgeScene = new HashMap<>();
        GaiaAttribute gaiaAttribute = halfEdgeScene.getAttribute();

        int nodesCount = halfEdgeScene.getNodes().size();
        for (int j = 0; j < nodesCount; j++) {
            HalfEdgeNode rootNode = halfEdgeScene.getNodes().get(j);
            Map<Integer, HalfEdgeNode> mapClassifyIdToNode = getMapHalfEdgeNodeByFaceClassifyId(rootNode, null);
            for (Integer key : mapClassifyIdToNode.keySet()) {
                int faceClassifyId = key;
                HalfEdgeNode halfEdgeNode = mapClassifyIdToNode.get(faceClassifyId);
                HalfEdgeScene halfEdgeSceneCopy = mapClassifyIdToHalfEdgeScene.get(faceClassifyId);
                if (halfEdgeSceneCopy == null) {
                    halfEdgeSceneCopy = new HalfEdgeScene();

                    // copy original path
                    halfEdgeSceneCopy.setOriginalPath(halfEdgeScene.getOriginalPath());

                    // copy gaiaAttributes
                    GaiaAttribute newGaiaAttribute = gaiaAttribute.getCopy();
                    halfEdgeSceneCopy.setAttribute(newGaiaAttribute);

                    mapClassifyIdToHalfEdgeScene.put(faceClassifyId, halfEdgeSceneCopy);
                }
                halfEdgeSceneCopy.getNodes().add(halfEdgeNode);
            }

        }

        for (Integer key : mapClassifyIdToHalfEdgeScene.keySet()) {
            HalfEdgeScene halfEdgeSceneCopy = mapClassifyIdToHalfEdgeScene.get(key);

            // copy materials
            List<GaiaMaterial> gaiaMaterials = halfEdgeScene.getMaterials();
            for (GaiaMaterial gaiaMaterial : gaiaMaterials) {
                GaiaMaterial newGaiaMaterial = gaiaMaterial.clone();
                halfEdgeSceneCopy.getMaterials().add(newGaiaMaterial);
            }
            resultHalfEdgeScenes.add(halfEdgeSceneCopy);
        }

        return resultHalfEdgeScenes;
    }

    private static Map<Integer, HalfEdgeNode> getMapHalfEdgeNodeByFaceClassifyId(HalfEdgeNode halfEdgeNode, Map<Integer, HalfEdgeNode> resultClassifyIdToNode) {
        if (resultClassifyIdToNode == null) {
            resultClassifyIdToNode = new HashMap<>();
        }

        List<HalfEdgeMesh> halfEdgeMeshes = halfEdgeNode.getMeshes();
        for (HalfEdgeMesh halfEdgeMesh : halfEdgeMeshes) {
            Map<Integer, HalfEdgeMesh> mapClassifyIdToMesh = getMapHalfEdgeMeshByFaceClassifyId(halfEdgeMesh, null);
            for (Integer key : mapClassifyIdToMesh.keySet()) {
                int faceClassifyId = key;
                HalfEdgeMesh newHalfEdgeMesh = mapClassifyIdToMesh.get(faceClassifyId);
                HalfEdgeNode newHalfEdgeNode = resultClassifyIdToNode.get(faceClassifyId);
                if (newHalfEdgeNode == null) {
                    newHalfEdgeNode = new HalfEdgeNode();

                    // copy transform matrices
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

        // check children
        List<HalfEdgeNode> children = halfEdgeNode.getChildren();
        for (HalfEdgeNode child : children) {
            Map<Integer, HalfEdgeNode> mapClassifyIdToNode = getMapHalfEdgeNodeByFaceClassifyId(child, null);
            for (Integer key : mapClassifyIdToNode.keySet()) {
                int faceClassifyId = key;
                HalfEdgeNode newHalfEdgeNode = mapClassifyIdToNode.get(faceClassifyId);
                HalfEdgeNode newHalfEdgeNodeParent = resultClassifyIdToNode.get(faceClassifyId);
                if (newHalfEdgeNodeParent == null) {
                    newHalfEdgeNodeParent = new HalfEdgeNode();
                    resultClassifyIdToNode.put(faceClassifyId, newHalfEdgeNodeParent);
                }
                newHalfEdgeNodeParent.getChildren().add(newHalfEdgeNode);
            }
        }

        return resultClassifyIdToNode;
    }

    private static Map<Integer, HalfEdgeMesh> getMapHalfEdgeMeshByFaceClassifyId(HalfEdgeMesh halfEdgeMesh, Map<Integer, HalfEdgeMesh> resultMap) {
        if (resultMap == null) {
            resultMap = new HashMap<>();
        }


        List<HalfEdgePrimitive> halfEdgePrimitives = halfEdgeMesh.getPrimitives();
        for (HalfEdgePrimitive halfEdgePrimitive : halfEdgePrimitives) {
            Map<Integer, HalfEdgePrimitive> mapClassifyIdToPrimitive = getMapHalfEdgePrimitiveByFaceClassifyId(halfEdgePrimitive, null);
            for (Integer key : mapClassifyIdToPrimitive.keySet()) {
                int faceClassifyId = key;
                HalfEdgePrimitive newHalfEdgePrimitive = mapClassifyIdToPrimitive.get(faceClassifyId);
                HalfEdgeMesh newHalfEdgeMesh = resultMap.get(faceClassifyId);
                if (newHalfEdgeMesh == null) {
                    newHalfEdgeMesh = new HalfEdgeMesh();
                    resultMap.put(faceClassifyId, newHalfEdgeMesh);
                }
                newHalfEdgeMesh.getPrimitives().add(newHalfEdgePrimitive);
            }

        }

        return resultMap;
    }

    private static Map<Integer, HalfEdgePrimitive> getMapHalfEdgePrimitiveByFaceClassifyId(HalfEdgePrimitive halfEdgePrimitive, Map<Integer, HalfEdgePrimitive> resultMap) {
        if (resultMap == null) {
            resultMap = new HashMap<>();
        }

        List<HalfEdgeSurface> halfEdgeSurfaces = halfEdgePrimitive.getSurfaces();
        for (HalfEdgeSurface halfEdgeSurface : halfEdgeSurfaces) {
            Map<Integer, HalfEdgeSurface> mapClassifyIdToSurface = getMapHalfEdgeSurfaceByFaceClassifyId(halfEdgeSurface, null);
            for (Integer key : mapClassifyIdToSurface.keySet()) {
                int faceClassifyId = key;
                HalfEdgeSurface newHalfEdgeSurface = mapClassifyIdToSurface.get(faceClassifyId);
                HalfEdgePrimitive newHalfEdgePrimitive = resultMap.get(faceClassifyId);
                if (newHalfEdgePrimitive == null) {
                    newHalfEdgePrimitive = new HalfEdgePrimitive();

                    // set accessor indices & materialId
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
        if (resultHalfEdgeSurfaces == null) {
            resultHalfEdgeSurfaces = new HashMap<>();
        }

        Map<Integer, List<HalfEdge>> mapFaceClassifyIdToHalfEdges = new HashMap<>();

        int halfEdgesCount = halfEdgeSurface.getHalfEdges().size();
        for (int i = 0; i < halfEdgesCount; i++) {
            HalfEdge halfEdge = halfEdgeSurface.getHalfEdges().get(i);
            HalfEdgeFace face = halfEdge.getFace();
            int faceClassifyId = face.getClassifyId();
            List<HalfEdge> halfEdges = mapFaceClassifyIdToHalfEdges.computeIfAbsent(faceClassifyId, k -> new ArrayList<>());
            halfEdges.add(halfEdge);
        }

        int faceClassifyIdsCount = mapFaceClassifyIdToHalfEdges.size();
        for (Integer key : mapFaceClassifyIdToHalfEdges.keySet()) {
            int faceClassifyId = key;
            List<HalfEdge> halfEdges = mapFaceClassifyIdToHalfEdges.get(faceClassifyId);
            HalfEdgeSurface newHalfEdgeSurface = createCopyHalfEdgeSurfaceFromHalfEdgesList(halfEdges);
            resultHalfEdgeSurfaces.put(faceClassifyId, newHalfEdgeSurface);
        }

        return resultHalfEdgeSurfaces;
    }

    private static HalfEdgeSurface createCopyHalfEdgeSurfaceFromHalfEdgesList(List<HalfEdge> halfEdges) {
        // create a temp surface
        HalfEdgeSurface halfEdgeSurfaceTemp = new HalfEdgeSurface();
        Map<HalfEdgeVertex, HalfEdgeVertex> mapUniqueHalfEdgeVertex = new HashMap<>();
        Map<HalfEdgeFace, HalfEdgeFace> mapUniqueHalfEdgeFace = new HashMap<>();
        for (HalfEdge halfEdge : halfEdges) {
            halfEdgeSurfaceTemp.getHalfEdges().add(halfEdge);

            HalfEdgeVertex startVertex = halfEdge.getStartVertex();
            mapUniqueHalfEdgeVertex.put(startVertex, startVertex);

            HalfEdgeFace face = halfEdge.getFace();
            mapUniqueHalfEdgeFace.put(face, face);
        }

        // set unique vertices
        List<HalfEdgeVertex> uniqueVertices = new ArrayList<>(mapUniqueHalfEdgeVertex.keySet());
        halfEdgeSurfaceTemp.getVertices().addAll(uniqueVertices);

        // set unique faces
        List<HalfEdgeFace> uniqueFaces = new ArrayList<>(mapUniqueHalfEdgeFace.keySet());
        halfEdgeSurfaceTemp.getFaces().addAll(uniqueFaces);

        halfEdgeSurfaceTemp.removeDeletedObjects();
        halfEdgeSurfaceTemp.setObjectIdsInList();

        // create a copy of the surface
        return getCopyHalfEdgeSurface(halfEdgeSurfaceTemp);
    }

    public static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    public static Vector3d calculateNormalAsConvex(List<HalfEdgeVertex> vertices, Vector3d resultNormal) {
        if (resultNormal == null) {
            resultNormal = new Vector3d();
        }
        int verticesCount = vertices.size();
        if (verticesCount < 3) {
            log.error("[ERROR] verticesCount < 3");
            return resultNormal;
        }
        HalfEdgeVertex vertex1 = vertices.get(0);
        HalfEdgeVertex vertex2 = vertices.get(1);
        HalfEdgeVertex vertex3 = vertices.get(2);
        Vector3d pos1 = vertex1.getPosition();
        Vector3d pos2 = vertex2.getPosition();
        Vector3d pos3 = vertex3.getPosition();
        Vector3d v1 = new Vector3d();
        Vector3d v2 = new Vector3d();
        v1.set(pos2.x - pos1.x, pos2.y - pos1.y, pos2.z - pos1.z);
        v2.set(pos3.x - pos1.x, pos3.y - pos1.y, pos3.z - pos1.z);
        v1.cross(v2, resultNormal);
        resultNormal.normalize();

        // check if x, y, z is NaN
        if (Double.isNaN(resultNormal.x) || Double.isNaN(resultNormal.y) || Double.isNaN(resultNormal.z)) {
            return null;
        }

        return resultNormal;
    }

    public static double calculateArea(HalfEdgeVertex a, HalfEdgeVertex b, HalfEdgeVertex c) {
        Vector3d posA = a.getPosition();
        Vector3d posB = b.getPosition();
        Vector3d posC = c.getPosition();

        double dist1 = posA.distance(posB);
        double dist2 = posB.distance(posC);
        double dist3 = posC.distance(posA);

        double s = (dist1 + dist2 + dist3) / 2.0;

        return Math.sqrt(s * (s - dist1) * (s - dist2) * (s - dist3));
    }

    public static double calculateAspectRatioAsTriangle(HalfEdgeVertex a, HalfEdgeVertex b, HalfEdgeVertex c) {
        Vector3d posA = a.getPosition();
        Vector3d posB = b.getPosition();
        Vector3d posC = c.getPosition();

        double dist1 = posA.distance(posB);
        double dist2 = posB.distance(posC);
        double dist3 = posC.distance(posA);

        double longest = Math.max(dist1, Math.max(dist2, dist3));
        double s = (dist1 + dist2 + dist3) / 2.0;
        double area = Math.sqrt(s * (s - dist1) * (s - dist2) * (s - dist3));

        double height = 2.0 * area / longest;

        return longest / height;
    }

    private static void getWeldableVertexMap(Map<GaiaVertex, GaiaVertex> mapVertexToVertexMaster, List<GaiaVertex> vertices, double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        Map<GaiaVertex, GaiaVertex> visitedMap = new HashMap<>();
        int verticesCount = vertices.size();
        for (int i = 0; i < verticesCount; i++) {
            GaiaVertex vertex = vertices.get(i);
            if (visitedMap.containsKey(vertex)) {
                continue;
            }
            mapVertexToVertexMaster.put(vertex, vertex);
            for (int j = i + 1; j < verticesCount; j++) {
                GaiaVertex vertex2 = vertices.get(j);
                if (visitedMap.containsKey(vertex2)) {
                    continue;
                }
                if (vertex.isWeldable(vertex2, error, checkTexCoord, checkNormal, checkColor, checkBatchId)) {
                    mapVertexToVertexMaster.put(vertex2, vertex);

                    visitedMap.put(vertex, vertex);
                    visitedMap.put(vertex2, vertex2);
                }
            }
        }
    }

    public static void weldVerticesGaiaSurface(GaiaSurface gaiaSurface, List<GaiaVertex> gaiaVertices, double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        // Weld the vertices
        GaiaOctreeVertices octreeVertices = new GaiaOctreeVertices(null);
        octreeVertices.getVertices().addAll(gaiaVertices);
        octreeVertices.calculateSize();
        octreeVertices.setAsCube();
        octreeVertices.setMaxDepth(10);
        octreeVertices.setMinBoxSize(1.0); // 1m

        octreeVertices.makeTreeByMinVertexCount(50);

        List<GaiaOctreeVertices> octreesWithContents = new ArrayList<>();
        octreeVertices.extractOctreesWithContents(octreesWithContents);

        Map<GaiaVertex, GaiaVertex> mapVertexToVertexMaster = new HashMap<>();

        for (GaiaOctreeVertices octree : octreesWithContents) {
            List<GaiaVertex> vertices = octree.getVertices();
            getWeldableVertexMap(mapVertexToVertexMaster, vertices, error, checkTexCoord, checkNormal, checkColor, checkBatchId);
        }

        Map<GaiaVertex, GaiaVertex> mapVertexMasters = new HashMap<>();
        for (GaiaVertex vertexMaster : mapVertexToVertexMaster.values()) {
            mapVertexMasters.put(vertexMaster, vertexMaster);
        }

        List<GaiaVertex> newVerticesArray = new ArrayList<>(mapVertexMasters.values());

        Map<GaiaVertex, Integer> vertexIdxMap = new HashMap<>();
        int verticesCount = newVerticesArray.size();
        for (int i = 0; i < verticesCount; i++) {
            vertexIdxMap.put(newVerticesArray.get(i), i);
        }

        // Now, update the indices of the faces
        Map<GaiaFace, GaiaFace> mapDeleteFaces = new HashMap<>();

        int facesCount = gaiaSurface.getFaces().size();
        for (int j = 0; j < facesCount; j++) {
            GaiaFace face = gaiaSurface.getFaces().get(j);
            int[] indices = face.getIndices();
            for (int k = 0; k < indices.length; k++) {
                GaiaVertex vertex = gaiaVertices.get(indices[k]);
                GaiaVertex vertexMaster = mapVertexToVertexMaster.get(vertex);
                int index = vertexIdxMap.get(vertexMaster);
                indices[k] = index;
            }

            // check indices
            for (int k = 0; k < indices.length; k++) {
                int index = indices[k];
                for (int m = k + 1; m < indices.length; m++) {
                    if (index == indices[m]) {
                        // must remove the face
                        mapDeleteFaces.put(face, face);
                    }
                }
            }
        }

        if (!mapDeleteFaces.isEmpty()) {
            List<GaiaFace> newFaces = new ArrayList<>();
            for (int j = 0; j < facesCount; j++) {
                GaiaFace face = gaiaSurface.getFaces().get(j);
                if (!mapDeleteFaces.containsKey(face)) {
                    newFaces.add(face);
                }
            }
            gaiaSurface.setFaces(newFaces);
        }

        // delete no used vertices
        for (GaiaVertex vertex : gaiaVertices) {
            if (!mapVertexMasters.containsKey(vertex)) {
                vertex.clear();
            }
        }
        gaiaVertices.clear();
        gaiaVertices.addAll(newVerticesArray);
    }

    public static Map<PlaneType, List<HalfEdgeFace>> makeMapPlaneTypeFacesList(List<HalfEdgeFace> facesList, Map<PlaneType, List<HalfEdgeFace>> mapPlaneTypeFacesList) {
        if (mapPlaneTypeFacesList == null) {
            mapPlaneTypeFacesList = new HashMap<>();
        }
        int facesCount = facesList.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = facesList.get(i);
            PlaneType planeType = face.getBestPlaneToProject();
            List<HalfEdgeFace> faces = mapPlaneTypeFacesList.computeIfAbsent(planeType, k -> new ArrayList<>());
            faces.add(face);
        }
        return mapPlaneTypeFacesList;
    }

    public static Map<CameraDirectionType, List<HalfEdgeFace>> makeMapCameraDirectionTypeFacesList(List<HalfEdgeFace> facesList) {
        Map<CameraDirectionType, List<HalfEdgeFace>> mapCameraDirectionFacesList = new HashMap<>();
        List<HalfEdgeFace> faces;

        int facesCount = facesList.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = facesList.get(i);
            CameraDirectionType cameraDirectionType = face.getCameraDirectionType();
            faces = mapCameraDirectionFacesList.computeIfAbsent(cameraDirectionType, k -> new ArrayList<>());
            faces.add(face);

        }
        return mapCameraDirectionFacesList;
    }

    public static GaiaBoundingBox getBoundingBoxOfFaces(List<HalfEdgeFace> faces) {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        boundingBox.setMinX(Double.MAX_VALUE);
        boundingBox.setMinY(Double.MAX_VALUE);
        boundingBox.setMinZ(Double.MAX_VALUE);
        boundingBox.setMaxX(-Double.MAX_VALUE);
        boundingBox.setMaxY(-Double.MAX_VALUE);
        boundingBox.setMaxZ(-Double.MAX_VALUE);

        List<HalfEdgeVertex> vertices = new ArrayList<>();
        for (HalfEdgeFace face : faces) {
            vertices = face.getVertices(vertices);
        }

        for (HalfEdgeVertex vertex : vertices) {
            Vector3d pos = vertex.getPosition();
            boundingBox.addPoint(pos);
        }

        return boundingBox;
    }

    public static void deformHalfEdgeSurfaceByVerticesConvexConcave(HalfEdgeScene scene, double factor) {
        List<HalfEdgeSurface> surfaces = scene.extractSurfaces(null);
        for (HalfEdgeSurface surface : surfaces) {
            deformHalfEdgeSurfaceByVerticesConvexConcave(surface, factor);
        }
    }

    public static void deformHalfEdgeSurfaceByVerticesConvexConcave(HalfEdgeSurface surface, double factor) {
        // 1rst, must determine if a vertex is convex or concave
        surface.calculateNormals();
        Map<HalfEdgeVertex, List<HalfEdgeFace>> mapVertexToFaces = new HashMap<>();
        surface.getMapVertexAllFaces(mapVertexToFaces);

        List<HalfEdgeVertex> convexVertices = new ArrayList<>();
        List<HalfEdgeVertex> concaveVertices = new ArrayList<>();
        List<HalfEdgeVertex> planeVertices = new ArrayList<>();
        List<HalfEdgeVertex> faceVertices = new ArrayList<>();

        List<HalfEdgeVertex> vertices = surface.getVertices();
        for (HalfEdgeVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            Vector3d normal = vertex.getNormal();
            GaiaPlane plane = new GaiaPlane(position, normal);

            List<HalfEdgeFace> faces = mapVertexToFaces.get(vertex);
            if (faces == null) {
                continue;
            }

            boolean isConvex = true;
            boolean isPlane = true;
            double error = 0.01; // 1cm
            for (HalfEdgeFace face : faces) {
                faceVertices.clear();
                faceVertices = face.getVertices(faceVertices);
                for (HalfEdgeVertex faceVertex : faceVertices) {
                    if (faceVertex == vertex) {
                        continue;
                    }

                    Vector3d faceVertexPos = faceVertex.getPosition();
                    double dist = plane.distanceToPoint(faceVertexPos);
                    if (Math.abs(dist) > error) {
                        if (dist < 0) {
                            isConvex = false;
                            isPlane = false;
                            break;
                        } else if (dist > 0) {
                            isPlane = false;
                        }
                    }
                }
            }

            if (isConvex) {
                convexVertices.add(vertex);
            } else if (isPlane) {
                planeVertices.add(vertex);
            } else {
                concaveVertices.add(vertex);
            }
        }

        // now, move the vertex in the normal direction using factor
        for (HalfEdgeVertex vertex : convexVertices) {
            // convex vertices move reverse to normal
            Vector3d position = vertex.getPosition();
            Vector3d normal = vertex.getNormal();
            Vector3d newPos = new Vector3d(position);
            newPos.x -= normal.x * factor;
            newPos.y -= normal.y * factor;
            newPos.z -= normal.z * factor;
            vertex.setPosition(newPos);
        }

        for (HalfEdgeVertex vertex : concaveVertices) {
            // concave vertices move in the normal direction
            Vector3d position = vertex.getPosition();
            Vector3d normal = vertex.getNormal();
            Vector3d newPos = new Vector3d(position);
            newPos.x += normal.x * factor;
            newPos.y += normal.y * factor;
            newPos.z += normal.z * factor;
            vertex.setPosition(newPos);
        }
    }

    public List<GaiaFace> getGaiaTriangleFacesFromGaiaFace(GaiaFace gaiaFace) {
        List<GaiaFace> gaiaFaces = new ArrayList<>();
        int[] indices = gaiaFace.getIndices();
        Vector3d normal = gaiaFace.getFaceNormal();
        int indicesCount = indices.length;

        for (int i = 0; i < indicesCount - 2; i += 3) {
            if (i + 2 >= indicesCount) {
                log.error("[ERROR] i + 2 >= indicesCount");
            }
            GaiaFace gaiaTriangleFace = new GaiaFace();
            gaiaTriangleFace.setIndices(new int[]{indices[i], indices[i + 1], indices[i + 2]});
            if (normal != null) {
                gaiaTriangleFace.setFaceNormal(new Vector3d(normal));
            }
            gaiaFaces.add(gaiaTriangleFace);
        }
        return gaiaFaces;
    }

}