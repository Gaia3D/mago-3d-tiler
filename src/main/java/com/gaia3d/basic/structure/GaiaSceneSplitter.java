package com.gaia3d.basic.structure;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.*;
import com.gaia3d.util.GlobeUtils;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GaiaSceneSplitter {
    public static GaiaBoundingBox getBoundingBox(List<GaiaScene> scenes)
    {
        int sceneCount = scenes.size();
        if(sceneCount > 0)
        {
            GaiaBoundingBox boundingBox = new GaiaBoundingBox();
            for (int i = 0; i < sceneCount; i++)
            {
                GaiaScene scene = scenes.get(i);
                boundingBox.addBoundingBox(scene.getBoundingBox());
            }
            return boundingBox;
        }
        return null;
    }
    public static void splitScenes(List<GaiaScene> scenes, List<GaiaScene> splittedScenes)
    {
        GaiaBoundingBox boundingBox = getBoundingBox(scenes);
        if(boundingBox == null)
        {
            return;
        }

        GaiaOctreeSceneSplitter octree = new GaiaOctreeSceneSplitter(null, boundingBox.getMinX(), boundingBox.getMinY(), boundingBox.getMinZ(),
                boundingBox.getMaxX(), boundingBox.getMaxY(), boundingBox.getMaxZ());

        octree.takeCubeForm();

        Map<String, List<GaiaScene>> mapOctreeNameScenes = new HashMap<>();

        int sceneCount = scenes.size();
        for (int i = 0; i < sceneCount; i++)
        {
            GaiaScene scene = scenes.get(i);
            splitScene(scene, octree, mapOctreeNameScenes);
        }

        // now create the GaiaScenes.***
        int counterAux = 0;
        for(String octreeName : mapOctreeNameScenes.keySet())
        {
//            counterAux++;
//            if(counterAux < 3)
//            {
//                continue;
//            }
            List<GaiaScene> scenesList = mapOctreeNameScenes.get(octreeName);
            int scenesCount = scenesList.size();
            for(int i=0; i<scenesCount; i++) {
                GaiaScene newScene = scenesList.get(i);
                splittedScenes.add(newScene);
            }
        }
        int hola = 0;
    }

    public static void splitScene(GaiaScene originalScene, GaiaOctreeSceneSplitter octree, Map<String, List<GaiaScene>> mapOctreeNameScenes)
    {
        if(originalScene == null || octree == null)
        {
            return;
        }

        GaiaNode rootNode = originalScene.getNodes().get(0);
        //splitNode(rootNode, null, octree);

        List<GaiaNode> nodes = new ArrayList<>();
        rootNode.extractNodesWithContents(nodes);
        Matrix4d rootTMatrix = new Matrix4d(rootNode.getTransformMatrix());

        Map<String, List<GaiaMesh>> resultMapOctreeNameMeshes = new HashMap<>();

        int nodesCount = nodes.size();
        for (int i = 0; i < nodesCount; i++)
        {
            GaiaNode node = nodes.get(i);
            Matrix4d transformMatrix = new Matrix4d(node.getTransformMatrix()); // must be pre-multiplied (TODO).***
            transformMatrix.mul(rootTMatrix, transformMatrix);
            List<GaiaMesh> meshes = node.getMeshes();
            for(GaiaMesh mesh : meshes)
            {
                splitMesh(mesh, transformMatrix, octree, resultMapOctreeNameMeshes);
            }
        }

        // now create the GaiaScenes.***
        for(String octreeName : resultMapOctreeNameMeshes.keySet())
        {
            List<GaiaMesh> meshesList = resultMapOctreeNameMeshes.get(octreeName);
            GaiaScene newScene = new GaiaScene();

            // copy the materials.***
            List<GaiaMaterial> materials = originalScene.getMaterials()
                    .stream().map(GaiaMaterial::clone)
                    .collect(Collectors.toList());
            newScene.setMaterials(materials);

            GaiaNode newRootNode = new GaiaNode();
            newScene.getNodes().add(newRootNode);
            newRootNode.getMeshes().addAll(meshesList);
            newRootNode.setTransformMatrix(new Matrix4d(rootNode.getTransformMatrix()));

            // copy the path.***
            newScene.setOriginalPath(originalScene.getOriginalPath());

            // finally insert the new scene into the mapOctreeNameScenes.***
            List<GaiaScene> scenesList = mapOctreeNameScenes.get(octreeName);
            if(scenesList == null)
            {
                scenesList = new ArrayList<>();
                mapOctreeNameScenes.put(octreeName, scenesList);
            }
            scenesList.add(newScene);
        }
    }

    public static void splitNode(GaiaNode node, Matrix4d parentMatrix, GaiaOctreeSceneSplitter octree)
    {
        if(node == null || octree == null)
        {
            return;
        }

        Matrix4d transformMatrix = new Matrix4d(node.getTransformMatrix());
        if (parentMatrix != null) {
            parentMatrix.mul(transformMatrix, transformMatrix);
        }

        // check if the node has meshes.***
        Map<String, List<GaiaMesh>> resultMapOctreeNameMeshes = new HashMap<>();
        List<GaiaMesh> meshes = node.getMeshes();
        if(meshes != null && meshes.size() > 0)
        {
            GaiaMesh mesh = meshes.get(0);
            splitMesh(mesh, transformMatrix, octree, resultMapOctreeNameMeshes);
        }

        // check if the node has children.***
        List<GaiaNode> children = node.getChildren();
        if(children != null && children.size() > 0)
        {
            for (int i = 0; i < children.size(); i++)
            {
                GaiaNode child = children.get(i);
                splitNode(child, transformMatrix, octree);
            }
        }
    }

    public static void splitMesh(GaiaMesh mesh, Matrix4d transformMatrix, GaiaOctreeSceneSplitter octree, Map<String, List<GaiaMesh>> resultMapOctreeNameMeshes)
    {
        if(mesh == null || octree == null)
        {
            return;
        }

        Map<String, List<GaiaPrimitive>> mapOctreeNamePrimitives = new HashMap<>();

        List<GaiaPrimitive> primitives = mesh.getPrimitives();
        int primitivesCount = primitives.size();
        for (int i = 0; i < primitivesCount; i++)
        {
            GaiaPrimitive primitive = primitives.get(i);
            splitPrimitive(primitive, transformMatrix, octree, mapOctreeNamePrimitives);
        }

        // now create the GaiaMeshes.***
        for(String octreeName : mapOctreeNamePrimitives.keySet())
        {
            List<GaiaPrimitive> primitivesList = mapOctreeNamePrimitives.get(octreeName);
            GaiaMesh newMesh = new GaiaMesh();
            newMesh.getPrimitives().addAll(primitivesList);

            // finally insert the new mesh into the resultMapOctreeNameMeshes.***
            List<GaiaMesh> meshesList = resultMapOctreeNameMeshes.get(octreeName);
            if(meshesList == null)
            {
                meshesList = new ArrayList<>();
                resultMapOctreeNameMeshes.put(octreeName, meshesList);
            }
            meshesList.add(newMesh);
        }

        int hola = 0;
    }

    public static GaiaPrimitive createPrimitiveFromSurfaces(List<GaiaSurface> surfacesList, List<GaiaVertex> originalVertices)
    {
        //*******************************************************
        // note : in originalVertices can be no used vertices.***
        //*******************************************************

        GaiaPrimitive newPrimitive = null;
        if(surfacesList == null || surfacesList.size() == 0) {
            return newPrimitive;
        }

        newPrimitive = new GaiaPrimitive();
        newPrimitive.getSurfaces().addAll(surfacesList);

        // now make verticesList of the newPrimitive.***
        Map<GaiaVertex, GaiaVertex> mapVertex = new HashMap<>();
        for(GaiaSurface surface : surfacesList) {
            List<GaiaFace> faces = surface.getFaces();
            for (GaiaFace face : faces) {
                int indices[] = face.getIndices();
                for (int index : indices) {
                    GaiaVertex vertex = originalVertices.get(index);
                    mapVertex.put(vertex, vertex);
                }
            }
        }

        List<GaiaVertex> newVertices = mapVertex.keySet().stream().collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        // now make vertices index map.***
        Map<GaiaVertex, Integer> mapVertexIndex = new HashMap<>();
        int newVerticesCount = newVertices.size();
        for (int i = 0; i < newVerticesCount; i++) {
            GaiaVertex vertex = newVertices.get(i);
            mapVertexIndex.put(vertex, i);
        }

        // now reassign face indices.***
        for(GaiaSurface surface : surfacesList)
        {
            List<GaiaFace> faces = surface.getFaces();
            for (GaiaFace face : faces) {
                int indices[] = face.getIndices();
                int indicesCount = indices.length;
                for (int i = 0; i < indicesCount; i++) {
                    GaiaVertex vertex = originalVertices.get(indices[i]);
                    int newIndex = mapVertexIndex.get(vertex);
                    indices[i] = newIndex;
                }
            }
        }

        // finally copy the vertices of newVertices to the newPrimitive.***
        for(int i=0; i<newVerticesCount; i++)
        {
            GaiaVertex vertex = newVertices.get(i);
            newPrimitive.getVertices().add(vertex.clone());
        }

        // test check.***
//        int indices[] = newPrimitive.getIndices();
//        int maxIdx = 0;
//        int minIdx = 0;
//        int indicesCount = indices.length;
//        for(int i=0; i<indicesCount; i++)
//        {
//            int idx = indices[i];
//            if(idx > maxIdx)
//            {
//                maxIdx = idx;
//            }
//            if(idx < minIdx)
//            {
//                minIdx = idx;
//            }
//        }
//        int veticesCount = newPrimitive.getVertices().size();
//        for(int i=0; i<veticesCount; i++)
//        {
//            GaiaVertex vertex = newPrimitive.getVertices().get(i);
//            Vector3d pos = vertex.getPosition();
//            //pos.set(pos.x, pos.z, -pos.y);
//            pos.add(500.0, 500.0, 500.0); // test.***
//        }

        return newPrimitive;
    }

    private static void splitPrimitive(GaiaPrimitive originalPrimitive, Matrix4d transformMatrix, GaiaOctreeSceneSplitter octree, Map<String, List<GaiaPrimitive>> resultMapOctreeNamePrimitives) {
        if(originalPrimitive == null || octree == null)
        {
            return;
        }

        List<GaiaSurface> originalSurfaces = originalPrimitive.getSurfaces();
        List<GaiaVertex> originalVertices = originalPrimitive.getVertices();

        Map<String, List<GaiaSurface>> resultMapOctreeNameSurfaces = new HashMap<>();

        int surfacesCount = originalSurfaces.size();
        for (int i = 0; i < surfacesCount; i++)
        {
            GaiaSurface surface = originalSurfaces.get(i);
            splitSurface(surface, originalVertices, transformMatrix, octree, resultMapOctreeNameSurfaces);
        }

        // for each octreeName, create a new primitive.***
        for(String octreeName : resultMapOctreeNameSurfaces.keySet())
        {
            List<GaiaSurface> surfacesList = resultMapOctreeNameSurfaces.get(octreeName);
            GaiaPrimitive newPrimitive = createPrimitiveFromSurfaces(surfacesList, originalVertices);

            // set the material index.***
            newPrimitive.setMaterialIndex(originalPrimitive.getMaterialIndex());

            // finally insert the new primitive into the resultMapOctreeNamePrimitives.***
            List<GaiaPrimitive> primitivesList = resultMapOctreeNamePrimitives.get(octreeName);
            if(primitivesList == null)
            {
                primitivesList = new ArrayList<>();
                resultMapOctreeNamePrimitives.put(octreeName, primitivesList);
            }
            primitivesList.add(newPrimitive);
        }

        int hola = 0;
    }

    private static void splitSurface(GaiaSurface surface, List<GaiaVertex> vertices, Matrix4d transformMatrix, GaiaOctreeSceneSplitter octree, Map<String, List<GaiaSurface>> resultMapOctreeNameSurfaces)
    {
        if(surface == null || octree == null)
        {
            return;
        }

        GaiaOctreeSceneSplitter octreeCopy = new GaiaOctreeSceneSplitter(null, octree.getMinX(), octree.getMinY(), octree.getMinZ(),
                octree.getMaxX(), octree.getMaxY(), octree.getMaxZ());

        List<GaiaVertex> faceVertices = new ArrayList<>();
        List<GaiaFace> faces = surface.getFaces();
        int facesCount = faces.size();
        double testMaxY = 0.0;
        Map<GaiaFace, Vector3d> mapFaceTransformedCenterPos = new HashMap<>();
        for (int i = 0; i < facesCount; i++)
        {
            faceVertices.clear();
            GaiaFace face = faces.get(i);
            int indices[] = face.getIndices();
            int indicesCount = indices.length;
            for (int j = 0; j < indicesCount; j++)
            {
                GaiaVertex vertex = vertices.get(indices[j]);
                faceVertices.add(vertex);

                if(vertex.getPosition().y > testMaxY)
                {
                    testMaxY = vertex.getPosition().y;
                }
            }

            octreeCopy.addFace(face);
            Vector3d faceCenterPos = getCenterPositionOfVertices(faceVertices);
            Vector3d transformedCenterPos = new Vector3d(faceCenterPos);
            transformMatrix.transformPosition(transformedCenterPos);
            mapFaceTransformedCenterPos.put(face, transformedCenterPos);

            int hola = 0;
        }

        octreeCopy.setVertices(vertices);
        octreeCopy.setTransformMatrix(transformMatrix);
        octreeCopy.setMapFaceTransformedCenterPos(mapFaceTransformedCenterPos);

        Vector3d octreeSize = octreeCopy.getSize();

        int targetDepth = 1;
        octreeCopy.distributeContents(targetDepth);


        List<GaiaOctreeSceneSplitter> octrees = new ArrayList<>();
        octreeCopy.extractOctreesWithContents(octrees);

        int octreesCount = octrees.size();
        for(int i=0; i<octreesCount; i++)
        {
            GaiaOctreeSceneSplitter octreeSplit = octrees.get(i);
            List<GaiaFace> facesSplit = octreeSplit.getFaces();
            if(facesSplit != null && facesSplit.size() > 0)
            {
                String octreeName = octreeSplit.getName();
                List<GaiaFace> splittedFaces = octreeSplit.getFaces();

                // create a surface.***
                GaiaSurface splittedSurface = new GaiaSurface();
                splittedSurface.getFaces().addAll(splittedFaces);

                // now insert the splitted surface into the resultMapOctreeNameSurfaces.***
                List<GaiaSurface> surfacesList = resultMapOctreeNameSurfaces.get(octreeName);
                if(surfacesList == null)
                {
                    surfacesList = new ArrayList<>();
                    resultMapOctreeNameSurfaces.put(octreeName, surfacesList);
                }
                surfacesList.add(splittedSurface);
            }
        }

        int hola = 0;
    }

    private static Vector3d getCenterPositionOfVertices(List<GaiaVertex> vertices)
    {
        Vector3d center = new Vector3d();
        int count = vertices.size();
        for (int i = 0; i < count; i++)
        {
            GaiaVertex vertex = vertices.get(i);
            center.add(vertex.getPosition());
        }
        center.div(count);
        return center;
    }
}
