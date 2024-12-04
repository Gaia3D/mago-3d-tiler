package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
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
        if(halfEdgeFace == null)
        {
            return null;
        }

        if(halfEdgeFace.getStatus() == ObjectStatus.DELETED)
        {
            return null;
        }

        if(halfEdgeFace.isDegenerated())
        {
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
            if (mapGaiaVertexToIndex.get(gaiaVertex) == null) {
                int hola = 0;
            }
            indices[i] = mapGaiaVertexToIndex.get(gaiaVertex);
            indicesCount++;
        }

        if(indicesCount > 2)
        {
            gaiaFace.setIndices(indices);
        }
        else
        {
            gaiaFace = null;
        }

        return gaiaFace;
    }

    private static HalfEdgeSurface getHalfEdgeSurfaceRegularNet(int numCols, int numRows, float[][] depthValues, GaiaBoundingBox bbox)
    {
        HalfEdgeSurface halfEdgeSurface = new HalfEdgeSurface();
        double minX = bbox.getMinX();
        double minY = bbox.getMinY();
        double maxX = bbox.getMaxX();
        double maxY = bbox.getMaxY();
        double minZ = bbox.getMinZ();
        double maxZ = bbox.getMaxZ();

        double xStep = (maxX - minX) / (numCols - 1);
        double yStep = (maxY - minY) / (numRows - 1);

        // create vertices.***
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

                // calculate texCoords.***
                double s = (double) c / (double) (numCols - 1);
                double t = (double) r / (double) (numRows - 1);
                halfEdgeVertex.setTexcoords(new Vector2d(s, 1.0 - t));

                if(depthValue >= 1.0)
                {
                    // this is noData.***
                    halfEdgeVertex.setStatus(ObjectStatus.DELETED);
                }

                halfEdgeSurface.getVertices().add(halfEdgeVertex);
            }
        }

        // create halfEdges & halfEdgeFaces.***
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

                if(vertex1.getStatus() != ObjectStatus.DELETED && vertex2.getStatus() != ObjectStatus.DELETED && vertex3.getStatus() != ObjectStatus.DELETED)
                {
                    // face A.***
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

                if(vertex1.getStatus() != ObjectStatus.DELETED && vertex3.getStatus() != ObjectStatus.DELETED && vertex4.getStatus() != ObjectStatus.DELETED) {

                    // face B.***
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

        return halfEdgeSurface;
    }

    public static HalfEdgeScene getHalfEdgeSceneRectangularNet(int numCols, int numRows, float[][] depthValues, GaiaBoundingBox bbox) {
        // Create halfEdgeScene.***
        HalfEdgeScene halfEdgeScene = new HalfEdgeScene();
        GaiaAttribute gaiaAttribute = new GaiaAttribute();
        halfEdgeScene.setAttribute(gaiaAttribute);
        String originalPath = "";
        halfEdgeScene.setOriginalPath(Path.of(originalPath));

        // Create root node.***
        HalfEdgeNode halfEdgeRootNode = new HalfEdgeNode();
        halfEdgeScene.getNodes().add(halfEdgeRootNode);

        // Create node.***
        HalfEdgeNode halfEdgeNode = new HalfEdgeNode();
        halfEdgeRootNode.getChildren().add(halfEdgeNode);

        // Create mesh.***
        HalfEdgeMesh halfEdgeMesh = new HalfEdgeMesh();
        halfEdgeNode.getMeshes().add(halfEdgeMesh);

        // Create primitive.***
        HalfEdgePrimitive halfEdgePrimitive = new HalfEdgePrimitive();
        halfEdgeMesh.getPrimitives().add(halfEdgePrimitive);

        // Create surface.***
        HalfEdgeSurface halfEdgeSurface = getHalfEdgeSurfaceRegularNet(numCols, numRows, depthValues, bbox);
        halfEdgePrimitive.getSurfaces().add(halfEdgeSurface);

        return halfEdgeScene;
    }

    public static HalfEdgeScene halfEdgeSceneFromGaiaScene(GaiaScene gaiaScene) {
        List<GaiaNode> gaiaNodes = gaiaScene.getNodes();
        int nodesCount = gaiaNodes.size(); // nodesCount must be 1. This is the root node.***
        HalfEdgeScene halfEdgeScene = new HalfEdgeScene();

        // set original path.***
        Path originalPath = gaiaScene.getOriginalPath();
        if(originalPath == null)
        {
            originalPath = Path.of("");
        }
        halfEdgeScene.setOriginalPath(originalPath);
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
            if(gaiaMesh == null)
            {
                log.error("gaiaMesh == null.***");
                continue;
            }
            HalfEdgeMesh halfEdgeMesh = HalfEdgeUtils.halfEdgeMeshFromGaiaMesh(gaiaMesh);
            halfEdgeNode.getMeshes().add(halfEdgeMesh);
        }

        // check children.***
        List<GaiaNode> gaiaChildren = gaiaNode.getChildren();
        int childrenCount = gaiaChildren.size();

        for (int i = 0; i < childrenCount; i++) {
            GaiaNode gaiaChild = gaiaChildren.get(i);
            if(gaiaChild == null)
            {
                log.error("gaiaChild == null.***");
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

        // primitives.***
        List<GaiaPrimitive> gaiaPrimitives = gaiaMesh.getPrimitives();
        int primitivesCount = gaiaPrimitives.size();
        for (int i = 0; i < primitivesCount; i++) {
            GaiaPrimitive gaiaPrimitive = gaiaPrimitives.get(i);
            if(gaiaPrimitive == null)
            {
                log.error("gaiaPrimitive == null.***");
                continue;
            }
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
            if(gaiaSurface == null)
            {
                log.error("gaiaSurface == null.***");
                continue;
            }
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

    public static double calculateAngleBetweenNormals(Vector3d normalA, Vector3d normalB) {
        double dotProduct = normalA.dot(normalB);
        double angle = Math.acos(dotProduct);
        return angle;
    }

    public static List<HalfEdgeVertex> getVerticesOfFaces(List<HalfEdgeFace> facesPlaneXYPos, List<HalfEdgeVertex> resultVertices) {
        Map<HalfEdgeVertex, HalfEdgeVertex> MapVertices = new HashMap<>();
        if(resultVertices == null)
        {
            resultVertices = new ArrayList<>();
        }
        int facesCount = facesPlaneXYPos.size();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = facesPlaneXYPos.get(i);
            List<HalfEdgeVertex> faceVertices = face.getVertices(null);
            int faceVerticesCount = faceVertices.size();
            for (int j = 0; j < faceVerticesCount; j++) {
                HalfEdgeVertex vertex = faceVertices.get(j);
                MapVertices.put(vertex, vertex);
            }
        }

        resultVertices.addAll(MapVertices.values());
        return resultVertices;
    }

    public List<GaiaFace> getGaiaTriangleFacesFromGaiaFace(GaiaFace gaiaFace) {
        List<GaiaFace> gaiaFaces = new ArrayList<>();
        int[] indices = gaiaFace.getIndices();
        Vector3d normal = gaiaFace.getFaceNormal();
        int indicesCount = indices.length;

        for (int i = 0; i < indicesCount - 2; i += 3) {
            if(i + 2 >= indicesCount)
            {
                log.error("i + 2 >= indicesCount.***");
                int hola = 0;
            }
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
            if(gaiaFace == null)
            {
                log.error("gaiaFace == null.***");
                continue;
            }
            List<GaiaFace> gaiaTriangleFaces = new HalfEdgeUtils().getGaiaTriangleFacesFromGaiaFace(gaiaFace);
            int triangleFacesCount = gaiaTriangleFaces.size();
            for (int j = 0; j < triangleFacesCount; j++) {
                GaiaFace gaiaTriangleFace = gaiaTriangleFaces.get(j);
                if(gaiaTriangleFace == null)
                {
                    continue;
                }
                HalfEdgeFace halfEdgeFace = HalfEdgeUtils.halfEdgeFaceFromGaiaFace(gaiaTriangleFace, gaiaVertices, halfEdgeSurface, mapGaiaVertexToHalfEdgeVertex);
                halfEdgeSurface.getFaces().add(halfEdgeFace);
            }
        }

        List<HalfEdgeVertex> halfEdgeVertices = new ArrayList<>(mapGaiaVertexToHalfEdgeVertex.values());
        halfEdgeSurface.getVertices().addAll(halfEdgeVertices);

        // set twins.***
        halfEdgeSurface.setTwins();
        halfEdgeSurface.checkSandClockFaces();
        //halfEdgeSurface.TEST_checkEqualHEdges();

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
            if(index >= gaiaVertices.size())
            {
                log.error("index >= gaiaVertices.size().***");
            }
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
        // TEST FUNCTION.***
        if(resultHalfEdgeScenes == null)
        {
            resultHalfEdgeScenes = new ArrayList<>();
        }

        Map<Integer, HalfEdgeScene> mapClassifyIdToHalfEdgeScene = new HashMap<>();
        GaiaAttribute gaiaAttribute = halfEdgeScene.getAttribute();

        // test : delete faces with classifyId = 1.***
        HalfEdgeScene halfEdgeScene1 = halfEdgeScene.clone();
        HalfEdgeScene halfEdgeScene2 = halfEdgeScene.clone();

        halfEdgeScene1.deleteFacesWithClassifyId(2);
        halfEdgeScene2.deleteFacesWithClassifyId(1);

        if(halfEdgeScene1.getTrianglesCount() > 0) {
            resultHalfEdgeScenes.add(halfEdgeScene1);
        }

        if(halfEdgeScene2.getTrianglesCount() > 0) {
            resultHalfEdgeScenes.add(halfEdgeScene2);
        }


//        List<HalfEdgeNode> halfEdgeNodes = halfEdgeScene.getNodes();
//        int nodesCount = halfEdgeNodes.size();
//        for(int j=0; j<nodesCount; j++)
//        {
//            HalfEdgeNode rootNode = halfEdgeNodes.get(j);
//            Map<Integer,HalfEdgeNode> mapClassifyIdToNode = getMapHalfEdgeNodeByFaceClassifyId(rootNode, null);
//            for(Integer key : mapClassifyIdToNode.keySet())
//            {
//                int faceClassifyId = key;
//                HalfEdgeNode halfEdgeNode = mapClassifyIdToNode.get(faceClassifyId);
//                HalfEdgeScene halfEdgeSceneCopy = mapClassifyIdToHalfEdgeScene.get(faceClassifyId);
//                if(halfEdgeSceneCopy == null)
//                {
//                    halfEdgeSceneCopy = new HalfEdgeScene();
//
//                    // copy original path.***
//                    halfEdgeSceneCopy.setOriginalPath(halfEdgeScene.getOriginalPath());
//
//                    // copy gaiaAttributes.***
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
//        for(Integer key : mapClassifyIdToHalfEdgeScene.keySet())
//        {
//            HalfEdgeScene halfEdgeSceneCopy = mapClassifyIdToHalfEdgeScene.get(key);
//
//            // copy materials.***
//            List<GaiaMaterial> gaiaMaterials = halfEdgeScene.getMaterials();
//            int materialsCount = gaiaMaterials.size();
//            for(int i=0; i<materialsCount; i++)
//            {
//                GaiaMaterial gaiaMaterial = gaiaMaterials.get(i);
//                GaiaMaterial newGaiaMaterial = gaiaMaterial.clone();
//                halfEdgeSceneCopy.getMaterials().add(newGaiaMaterial);
//            }
//            resultHalfEdgeScenes.add(halfEdgeSceneCopy);
//        }

        return resultHalfEdgeScenes;
    }

    public static List<HalfEdgeScene> getCopyHalfEdgeScenesByFaceClassifyId_original(HalfEdgeScene halfEdgeScene, List<HalfEdgeScene> resultHalfEdgeScenes)
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

    public static double log2(double x) {
        return Math.log(x) / Math.log(2);
    }

    public static Vector3d calculateNormalAsConvex(List<HalfEdgeVertex> vertices, Vector3d resultNormal) {
        if(resultNormal == null)
        {
            resultNormal = new Vector3d();
        }
        int verticesCount = vertices.size();
        if(verticesCount < 3)
        {
            log.error("verticesCount < 3.***");
            return resultNormal;
        }
        if(verticesCount > 3)
        {verticesCount = 3;}

        if(verticesCount == 3)
        {
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

            // check if x, y, z is NaN.***
            if(Double.isNaN(resultNormal.x) || Double.isNaN(resultNormal.y) || Double.isNaN(resultNormal.z))
            {
                return null;
            }
        }

        return resultNormal;
    }

    public static double calculateAspectRatioAsTriangle(HalfEdgeVertex a, HalfEdgeVertex b, HalfEdgeVertex c)
    {
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

    private static void getWeldableVertexMap(Map<GaiaVertex, GaiaVertex> mapVertexToVertexMaster, List<GaiaVertex> vertices, double error, boolean checkTexCoord, boolean checkNormal,
                                      boolean checkColor, boolean checkBatchId) {
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

    public static void weldVerticesGaiaSurface(GaiaSurface gaiaSurface, List<GaiaVertex> gaiaVertices, double error, boolean checkTexCoord,
                                               boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        // Weld the vertices.***
        GaiaOctreeVertices octreeVertices = new GaiaOctreeVertices(null);
        octreeVertices.getVertices().addAll(gaiaVertices);
        octreeVertices.calculateSize();
        octreeVertices.setAsCube();
        octreeVertices.setMaxDepth(10);
        octreeVertices.setMinBoxSize(1.0); // 1m.***

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

        // Now, update the indices of the faces.***
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

            // check indices.***
            for (int k = 0; k < indices.length; k++) {
                int index = indices[k];
                for (int m = k + 1; m < indices.length; m++) {
                    if (index == indices[m]) {
                        // must remove the face.***
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

    public static Map<PlaneType, List<HalfEdgeFace>> makeMapPlaneTypeFacesList(List<HalfEdgeFace> facesList, Map<PlaneType, List<HalfEdgeFace>> mapPlaneTypeFacesList)
    {
        if(mapPlaneTypeFacesList == null)
        {
            mapPlaneTypeFacesList = new HashMap<>();
        }
        int facesCount = facesList.size();
        for(int i=0; i<facesCount; i++)
        {
            HalfEdgeFace face = facesList.get(i);
            PlaneType planeType = face.getBestPlaneToProject();
            List<HalfEdgeFace> faces = mapPlaneTypeFacesList.computeIfAbsent(planeType, k -> new ArrayList<>());
            faces.add(face);
        }
        return mapPlaneTypeFacesList;
    }

    public static GaiaBoundingBox getBoundingBoxOfFaces(List<HalfEdgeFace> faces)
    {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        boundingBox.setMinX(Double.MAX_VALUE);
        boundingBox.setMinY(Double.MAX_VALUE);
        boundingBox.setMinZ(Double.MAX_VALUE);
        boundingBox.setMaxX(-Double.MAX_VALUE);
        boundingBox.setMaxY(-Double.MAX_VALUE);
        boundingBox.setMaxZ(-Double.MAX_VALUE);

        int facesCount = faces.size();
        List<HalfEdgeVertex> vertices = new ArrayList<>();
        for(int i=0; i<facesCount; i++)
        {
            HalfEdgeFace face = faces.get(i);
            vertices = face.getVertices(vertices);
        }

        int verticesCount = vertices.size();
        for(int i=0; i<verticesCount; i++)
        {
            HalfEdgeVertex vertex = vertices.get(i);
            Vector3d pos = vertex.getPosition();
            boundingBox.addPoint(pos);
        }

        return boundingBox;
    }

}