package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaAAPlane;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctree;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaMaterial;
import org.joml.Vector3d;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HalfEdgeCutter {
    public static void getPlanesGridXYZForBox(GaiaBoundingBox bbox, double gridSpacing, List<GaiaAAPlane> resultPlanesYZ, List<GaiaAAPlane> resultPlanesXZ, List<GaiaAAPlane> resultPlanesXY,
                                              HalfEdgeOctree resultOctree)
    {
        // Note : the grid is regularly spaced in the 3 axis.***
        double maxSize = bbox.getMaxSize();
        int desiredDepth = (int)Math.ceil(HalfEdgeUtils.log2(maxSize/gridSpacing));
        double desiredDistanceRoot = gridSpacing * Math.pow(2, desiredDepth);

        GaiaBoundingBox cubeBBox = bbox.clone();
        cubeBBox.setMaxX(cubeBBox.getMinX() + desiredDistanceRoot);
        cubeBBox.setMaxY(cubeBBox.getMinY() + desiredDistanceRoot);
        cubeBBox.setMaxZ(cubeBBox.getMinZ() + desiredDistanceRoot);

        resultOctree.setSize(cubeBBox.getMinX(), cubeBBox.getMinY(), cubeBBox.getMinZ(), cubeBBox.getMaxX(), cubeBBox.getMaxY(), cubeBBox.getMaxZ());
        resultOctree.makeTreeByMaxDepth(desiredDepth);

        // create GaiaAAPlanes.***
        int leafOctreesCountForAxis = (int) Math.pow(2, desiredDepth);
        for(int i=1; i<leafOctreesCountForAxis; i++) // 'i' starts in 1 because the first plane is the bbox min.***
        {
            // planes_YZ.***
            GaiaAAPlane planeYZ = new GaiaAAPlane();
            planeYZ.setPlaneType(PlaneType.YZ);
            Vector3d point = new Vector3d();
            point.x = bbox.getMinX() + i * gridSpacing;
            point.y = bbox.getMinY();
            point.z = bbox.getMinZ();
            planeYZ.setPoint(point);
            resultPlanesYZ.add(planeYZ);

            // planes_XZ.***
            GaiaAAPlane planeXZ = new GaiaAAPlane();
            planeXZ.setPlaneType(PlaneType.XZ);
            point = new Vector3d();
            point.x = bbox.getMinX();
            point.y = bbox.getMinY() + i * gridSpacing;
            point.z = bbox.getMinZ();
            planeXZ.setPoint(point);
            resultPlanesXZ.add(planeXZ);

            // planes_XY.***
            GaiaAAPlane planeXY = new GaiaAAPlane();
            planeXY.setPlaneType(PlaneType.XY);
            point = new Vector3d();
            point.x = bbox.getMinX();
            point.y = bbox.getMinY();
            point.z = bbox.getMinZ() + i * gridSpacing;
            planeXY.setPoint(point);
            resultPlanesXY.add(planeXY);
        }
    }

    public static HalfEdgeScene cutHalfEdgeSceneGridXYZ(HalfEdgeScene halfEdgeScene, double gridSpacing, HalfEdgeOctree resultOctree)
    {
        GaiaBoundingBox bbox = halfEdgeScene.getBoundingBox();

        List<GaiaAAPlane> resultPlanesYZ = new ArrayList<>();
        List<GaiaAAPlane> resultPlanesXZ = new ArrayList<>();
        List<GaiaAAPlane> resultPlanesXY = new ArrayList<>();
        getPlanesGridXYZForBox(bbox, gridSpacing, resultPlanesYZ, resultPlanesXZ, resultPlanesXY, resultOctree);

        double error = 1e-8;
        int planesCount = resultPlanesYZ.size();
        for(int i=0; i<planesCount; i++)
        {
            GaiaAAPlane planeYZ = resultPlanesYZ.get(i);
            halfEdgeScene.cutByPlane(planeYZ.getPlaneType(), planeYZ.getPoint(), error);
        }

        planesCount = resultPlanesXZ.size();
        for(int i=0; i<planesCount; i++)
        {
            GaiaAAPlane planeXZ = resultPlanesXZ.get(i);
            halfEdgeScene.cutByPlane(planeXZ.getPlaneType(), planeXZ.getPoint(), error);
        }

        planesCount = resultPlanesXY.size();
        for(int i=0; i<planesCount; i++)
        {
            GaiaAAPlane planeXY = resultPlanesXY.get(i);
            halfEdgeScene.cutByPlane(planeXY.getPlaneType(), planeXY.getPoint(), error);
        }

        // now, distribute faces into octree.***
        resultOctree.getFaces().clear();
        List<HalfEdgeSurface> surfaces = halfEdgeScene.extractSurfaces(null);
        int surfacesCount = surfaces.size();
        for(int i=0; i<surfacesCount; i++)
        {
            HalfEdgeSurface surface = surfaces.get(i);
            List<HalfEdgeFace> faces = surface.getFaces();
            int facesCount = faces.size();
            for(int j=0; j<facesCount; j++)
            {
                HalfEdgeFace face = faces.get(j);
                resultOctree.getFaces().add(face);
            }
        }

        resultOctree.distributeFacesToLeaf();
        List<HalfEdgeOctree> octreesWithContents = new ArrayList<>();
        resultOctree.extractOctreesWithFaces(octreesWithContents);

        // now, separate the surface by the octrees.***
        // set the classifyId for each face.***
        List<HalfEdgeSurface> newSurfaces = new ArrayList<>();
        int octreesCount = octreesWithContents.size();
        for(int j=0; j<octreesCount; j++)
        {
            HalfEdgeOctree octree = octreesWithContents.get(j);
            List<HalfEdgeFace> faces = octree.getFaces();
            int facesCount = faces.size();
            for(int k=0; k<facesCount; k++)
            {
                HalfEdgeFace face = faces.get(k);
                face.setClassifyId(j);
            }

            HalfEdgeSurface newSurface = createHalfEdgeSurfaceByFacesCopyCheckingClassifiedId(faces);
            newSurfaces.add(newSurface);

            // now, clear the faces of the ecTree.***
            octree.getFaces().clear();

            // add the new surface to the octree.***
            octree.getSurfaces().add(newSurface);
        }

        // now join all newSurfaces into a one surface.***
        HalfEdgeSurface uniqueSurface = new HalfEdgeSurface();
        int newSurfacesCount = newSurfaces.size();
        for(int i=0; i<newSurfacesCount; i++)
        {
            HalfEdgeSurface newSurface = newSurfaces.get(i);
            uniqueSurface.joinSurface(newSurface);
        }

        // create a new HalfEdgeScene.***
        HalfEdgeScene cuttedScene = new HalfEdgeScene();
        HalfEdgeNode rootNode = new HalfEdgeNode();
        cuttedScene.getNodes().add(rootNode);
        HalfEdgeNode childNode = new HalfEdgeNode();
        rootNode.getChildren().add(childNode);
        HalfEdgeMesh mesh = new HalfEdgeMesh();
        childNode.getMeshes().add(mesh);
        HalfEdgePrimitive primitive = new HalfEdgePrimitive();
        mesh.getPrimitives().add(primitive);
        primitive.getSurfaces().add(uniqueSurface);

        // copy attributes, originalPath, boundingBox, etc.***
        GaiaAttribute attribute = halfEdgeScene.getAttribute();
        if(attribute != null)
        {
            GaiaAttribute newAttribute = attribute.getCopy();
            cuttedScene.setAttribute(newAttribute);
        }

        GaiaBoundingBox newBBox = bbox.clone();
        cuttedScene.setBoundingBox(newBBox);

        Path originalPath = halfEdgeScene.getOriginalPath();
        cuttedScene.setOriginalPath(originalPath);

        List<GaiaMaterial> materials = halfEdgeScene.getMaterials();
        if(materials != null)
        {
            List<GaiaMaterial> newMaterials = new ArrayList<>();
            int materialsCount = materials.size();
            for(int i=0; i<materialsCount; i++)
            {
                GaiaMaterial material = materials.get(i);
                GaiaMaterial newMaterial = material.clone();
                newMaterials.add(newMaterial);
            }
            cuttedScene.setMaterials(newMaterials);
        }

        return cuttedScene;
    }

    private static HalfEdgeSurface createHalfEdgeSurfaceByFacesCopyCheckingClassifiedId(List<HalfEdgeFace> faces)
    {
        HalfEdgeSurface newSurface = new HalfEdgeSurface();

        Map<HalfEdgeVertex, HalfEdgeVertex> vertexToNewVertexMap = new HashMap<>();
        Map<HalfEdge, HalfEdge> edgeToNewEdgeMap = new HashMap<>();
        Map<HalfEdgeFace, HalfEdgeFace> faceToNewFaceMap = new HashMap<>();

        List<HalfEdge> faceEdges = new ArrayList<>();

        int facesCount = faces.size();
        for(int i=0; i<facesCount; i++)
        {
            HalfEdgeFace face = faces.get(i);
            faceEdges.clear();
            faceEdges = face.getHalfEdgesLoop(faceEdges);
            int faceEdgesCount = faceEdges.size();
            for(int j=0; j<faceEdgesCount; j++)
            {
                HalfEdge edge = faceEdges.get(j);

                // copy vertex.***
                HalfEdgeVertex startVertex = edge.getStartVertex();
                if(!vertexToNewVertexMap.containsKey(startVertex))
                {
                    HalfEdgeVertex copyStartVertex = new HalfEdgeVertex();
                    copyStartVertex.copyFrom(startVertex);
                    vertexToNewVertexMap.put(startVertex, copyStartVertex);
                }

                // copy edge.***
                HalfEdge copyEdge = new HalfEdge();
                edgeToNewEdgeMap.put(edge, copyEdge);
            }

            // copy face.***
            HalfEdgeFace copyFace = new HalfEdgeFace();
            copyFace.copyFrom(face);
            faceToNewFaceMap.put(face, copyFace);
        }

        // original halfEdges.***
        List<HalfEdge> edges = new ArrayList<>(edgeToNewEdgeMap.keySet());
        int halfEdgesCount = edges.size();
        for(int i=0; i<halfEdgesCount; i++)
        {
            HalfEdge edge = edges.get(i);
            HalfEdge copyEdge = edgeToNewEdgeMap.get(edge);

            // startVertex.***
            HalfEdgeVertex startVertex = edge.getStartVertex();
            HalfEdgeVertex copyStartVertex = vertexToNewVertexMap.get(startVertex);
            copyEdge.setStartVertex(copyStartVertex);
            copyStartVertex.setOutingHalfEdge(copyEdge);

            // next.***
            HalfEdge nextEdge = edge.getNext();
            HalfEdge copyNextEdge = edgeToNewEdgeMap.get(nextEdge);
            copyEdge.setNext(copyNextEdge);

            // copy face.***
            HalfEdgeFace face = edge.getFace();
            HalfEdgeFace copyFace = faceToNewFaceMap.get(face);
            copyEdge.setFace(copyFace);
            copyFace.setHalfEdge(copyEdge);

            int classifyId = face.getClassifyId();

            // copy twin (check the classifiedId of the face).***
            HalfEdge twin = edge.getTwin();
            if(twin != null)
            {
                HalfEdgeFace twinFace = twin.getFace();
                int twinClassifyId = twinFace.getClassifyId();
                if(classifyId == twinClassifyId)
                {
                    HalfEdge copyTwin = edgeToNewEdgeMap.get(twin);
                    copyEdge.setTwin(copyTwin);
                }
            }
        }

        List<HalfEdgeFace> newFaces = new ArrayList<>(faceToNewFaceMap.values());
        List<HalfEdgeVertex> newVertices = new ArrayList<>(vertexToNewVertexMap.values());
        List<HalfEdge> newEdges = new ArrayList<>(edgeToNewEdgeMap.values());

        newSurface.setVertices(newVertices);
        newSurface.setFaces(newFaces);
        newSurface.setHalfEdges(newEdges);

        return newSurface;
    }

    public static HalfEdgeSurface createHalfEdgeSurfaceByFacesCopy(List<HalfEdgeFace> faces, boolean checkClassifyId, boolean checkBestPlaneToProject)
    {
        HalfEdgeSurface newSurface = new HalfEdgeSurface();

        Map<HalfEdgeVertex, HalfEdgeVertex> vertexToNewVertexMap = new HashMap<>();
        Map<HalfEdge, HalfEdge> edgeToNewEdgeMap = new HashMap<>();
        Map<HalfEdgeFace, HalfEdgeFace> faceToNewFaceMap = new HashMap<>();

        List<HalfEdge> faceEdges = new ArrayList<>();

        int facesCount = faces.size();
        for(int i=0; i<facesCount; i++)
        {
            HalfEdgeFace face = faces.get(i);
            faceEdges.clear();
            faceEdges = face.getHalfEdgesLoop(faceEdges);
            int faceEdgesCount = faceEdges.size();
            for(int j=0; j<faceEdgesCount; j++)
            {
                HalfEdge edge = faceEdges.get(j);

                // copy vertex.***
                HalfEdgeVertex startVertex = edge.getStartVertex();
                if(!vertexToNewVertexMap.containsKey(startVertex))
                {
                    HalfEdgeVertex copyStartVertex = new HalfEdgeVertex();
                    copyStartVertex.copyFrom(startVertex);
                    vertexToNewVertexMap.put(startVertex, copyStartVertex);
                }

                // copy edge.***
                HalfEdge copyEdge = new HalfEdge();
                edgeToNewEdgeMap.put(edge, copyEdge);
            }

            // copy face.***
            HalfEdgeFace copyFace = new HalfEdgeFace();
            copyFace.copyFrom(face);
            faceToNewFaceMap.put(face, copyFace);
        }

        // original halfEdges.***
        List<HalfEdge> edges = new ArrayList<>(edgeToNewEdgeMap.keySet());
        int halfEdgesCount = edges.size();
        for(int i=0; i<halfEdgesCount; i++)
        {
            HalfEdge edge = edges.get(i);
            HalfEdge copyEdge = edgeToNewEdgeMap.get(edge);

            // startVertex.***
            HalfEdgeVertex startVertex = edge.getStartVertex();
            HalfEdgeVertex copyStartVertex = vertexToNewVertexMap.get(startVertex);
            copyEdge.setStartVertex(copyStartVertex);
            copyStartVertex.setOutingHalfEdge(copyEdge);

            // next.***
            HalfEdge nextEdge = edge.getNext();
            HalfEdge copyNextEdge = edgeToNewEdgeMap.get(nextEdge);
            copyEdge.setNext(copyNextEdge);

            // copy face.***
            HalfEdgeFace face = edge.getFace();
            HalfEdgeFace copyFace = faceToNewFaceMap.get(face);
            copyEdge.setFace(copyFace);
            copyFace.setHalfEdge(copyEdge);

            int classifyId = face.getClassifyId();
            PlaneType bestPlaneType = face.getBestPlaneToProject();

            // copy twin (check the classifiedId of the face).***
            boolean classifyIdOk = false;
            boolean bestPlaneTypeOk = false;
            HalfEdge twin = edge.getTwin();
            if(twin != null)
            {
                HalfEdgeFace twinFace = twin.getFace();
                int twinClassifyId = twinFace.getClassifyId();
                PlaneType twinBestPlaneType = twinFace.getBestPlaneToProject();
                if(checkClassifyId) {
                    if(classifyId == twinClassifyId){
                        classifyIdOk = true;
                    }
                }
                else {
                    classifyIdOk = true;
                }

                if(checkBestPlaneToProject) {
                    if(bestPlaneType == twinBestPlaneType) {
                        bestPlaneTypeOk = true;
                    }
                }
                else {
                    bestPlaneTypeOk = true;
                }

                if(bestPlaneTypeOk && classifyIdOk)
                {
                    HalfEdge copyTwin = edgeToNewEdgeMap.get(twin);
                    copyEdge.setTwin(copyTwin);
                }
            }
        }

        List<HalfEdgeFace> newFaces = new ArrayList<>(faceToNewFaceMap.values());
        List<HalfEdgeVertex> newVertices = new ArrayList<>(vertexToNewVertexMap.values());
        List<HalfEdge> newEdges = new ArrayList<>(edgeToNewEdgeMap.values());

        newSurface.setVertices(newVertices);
        newSurface.setFaces(newFaces);
        newSurface.setHalfEdges(newEdges);

        return newSurface;
    }
}
