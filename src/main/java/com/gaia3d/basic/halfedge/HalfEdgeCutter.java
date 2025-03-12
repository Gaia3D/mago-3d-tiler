package com.gaia3d.basic.halfedge;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaAAPlane;
import com.gaia3d.basic.geometry.octree.HalfEdgeOctree;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaSurface;
import com.gaia3d.basic.model.GaiaVertex;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class HalfEdgeCutter {
    public static void getPlanesGridXYZForBox(GaiaBoundingBox bbox, double gridSpacing, List<GaiaAAPlane> resultPlanesYZ, List<GaiaAAPlane> resultPlanesXZ,
                                              List<GaiaAAPlane> resultPlanesXY, HalfEdgeOctree resultOctree) {
        // Note : the grid is regularly spaced in the 3 axis
        double maxSize = bbox.getMaxSize();
        int desiredDepth = (int) Math.ceil(HalfEdgeUtils.log2(maxSize / gridSpacing));
        double desiredDistanceRoot = gridSpacing * Math.pow(2, desiredDepth);

        GaiaBoundingBox cubeBBox = bbox.clone();
        cubeBBox.setMaxX(cubeBBox.getMinX() + desiredDistanceRoot);
        cubeBBox.setMaxY(cubeBBox.getMinY() + desiredDistanceRoot);
        cubeBBox.setMaxZ(cubeBBox.getMinZ() + desiredDistanceRoot);

        resultOctree.setSize(cubeBBox.getMinX(), cubeBBox.getMinY(), cubeBBox.getMinZ(), cubeBBox.getMaxX(), cubeBBox.getMaxY(), cubeBBox.getMaxZ());
        resultOctree.setMaxDepth(desiredDepth);


        // create GaiaAAPlanes
        int leafOctreesCountForAxis = (int) Math.pow(2, desiredDepth);
        for (int i = 1; i < leafOctreesCountForAxis; i++) // 'i' starts in 1 because the first plane is the bbox min
        {
            // planes_YZ
            GaiaAAPlane planeYZ = new GaiaAAPlane();
            planeYZ.setPlaneType(PlaneType.YZ);
            Vector3d point = new Vector3d();
            point.x = bbox.getMinX() + i * gridSpacing;
            point.y = bbox.getMinY();
            point.z = bbox.getMinZ();
            planeYZ.setPoint(point);
            resultPlanesYZ.add(planeYZ);

            // planes_XZ
            GaiaAAPlane planeXZ = new GaiaAAPlane();
            planeXZ.setPlaneType(PlaneType.XZ);
            point = new Vector3d();
            point.x = bbox.getMinX();
            point.y = bbox.getMinY() + i * gridSpacing;
            point.z = bbox.getMinZ();
            planeXZ.setPoint(point);
            resultPlanesXZ.add(planeXZ);

            // planes_XY
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

    public static List<HalfEdgeScene> cutHalfEdgeSceneByGaiaAAPlanes(HalfEdgeScene halfEdgeScene, List<GaiaAAPlane> planes, HalfEdgeOctree resultOctree,
                                                                     boolean scissorTextures, boolean makeSkirt)
    {
        double error = 1e-5; //
        int planesCount = planes.size();
        for (int i = 0; i < planesCount; i++) {
            GaiaAAPlane plane = planes.get(i);
            halfEdgeScene.cutByPlane(plane.getPlaneType(), plane.getPoint(), error);

//            List<HalfEdgeFace> faces = new ArrayList<>();
//            double error2 = 1e-5;
//            halfEdgeScene.getIntersectedFacesByPlane(plane.getPlaneType(), plane.getPoint(), faces, error2);
//            if (faces.size() > 0) {
//                log.info("plane intersected faces count = " + faces.size());
//                List<HalfEdgeVertex> faceVertices = faces.get(0).getVertices(null);
//                Vector3d pos0 = faceVertices.get(0).getPosition();
//                Vector3d pos1 = faceVertices.get(1).getPosition();
//                Vector3d pos2 = faceVertices.get(2).getPosition();
//
//                int hola = 0;
//            }
        }

        //halfEdgeScene.deleteDegeneratedFaces();
        //halfEdgeScene.updateFacesList();

        // now, distribute faces into octree
        resultOctree.getFaces().clear();
        List<HalfEdgeSurface> surfaces = halfEdgeScene.extractSurfaces(null);
        for (HalfEdgeSurface surface : surfaces) {
            List<HalfEdgeFace> faces = surface.getFaces();
            for (HalfEdgeFace face : faces) {
                if (face.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }
                resultOctree.getFaces().add(face);
            }
        }

        resultOctree.distributeFacesToTargetDepth(resultOctree.getMaxDepth());
        List<HalfEdgeOctree> octreesWithContents = new ArrayList<>();
        resultOctree.extractOctreesWithFaces(octreesWithContents);

        // now, separate the surface by the octrees
        List<HalfEdgeScene> resultScenes = new ArrayList<>();

        // set the classifyId for each face
        int octreesCount = octreesWithContents.size();
//        for (int j = 0; j < octreesCount; j++) {
//            HalfEdgeOctree octree = octreesWithContents.get(j);
//            List<HalfEdgeFace> faces = octree.getFaces();
//            for (HalfEdgeFace face : faces) {
//                face.setClassifyId(j);
//            }
//        }

        for (int j = 0; j < octreesCount; j++) {
            HalfEdgeOctree octree = octreesWithContents.get(j);
            List<HalfEdgeFace> faces = octree.getFaces();
            for (HalfEdgeFace face : faces) {
                face.setClassifyId(j);
            }
            // create a new HalfEdgeScene
            HalfEdgeScene cuttedScene = halfEdgeScene.cloneByClassifyId(j);

            if (cuttedScene == null) {
                log.info("cuttedScene is null");
                continue;
            }

            if (scissorTextures) {
                cuttedScene.scissorTexturesByMotherScene(halfEdgeScene.getMaterials());
            }

            if (makeSkirt) {
                cuttedScene.makeSkirt();
            }


            resultScenes.add(cuttedScene);
        }
        return resultScenes;
    }

    public static HalfEdgeScene cutHalfEdgeSceneGridXYZ(HalfEdgeScene halfEdgeScene, double gridSpacing, HalfEdgeOctree resultOctree) {
        GaiaBoundingBox bbox = halfEdgeScene.getBoundingBox();

        List<GaiaAAPlane> resultPlanesYZ = new ArrayList<>();
        List<GaiaAAPlane> resultPlanesXZ = new ArrayList<>();
        List<GaiaAAPlane> resultPlanesXY = new ArrayList<>();
        getPlanesGridXYZForBox(bbox, gridSpacing, resultPlanesYZ, resultPlanesXZ, resultPlanesXY, resultOctree);

        double error = 1e-4;
        int planesCount = resultPlanesYZ.size();
        for (int i = 0; i < planesCount; i++) {
            GaiaAAPlane planeYZ = resultPlanesYZ.get(i);
            halfEdgeScene.cutByPlane(planeYZ.getPlaneType(), planeYZ.getPoint(), error);
        }

        planesCount = resultPlanesXZ.size();
        for (int i = 0; i < planesCount; i++) {
            GaiaAAPlane planeXZ = resultPlanesXZ.get(i);
            halfEdgeScene.cutByPlane(planeXZ.getPlaneType(), planeXZ.getPoint(), error);
        }

        planesCount = resultPlanesXY.size();
        for (int i = 0; i < planesCount; i++) {
            GaiaAAPlane planeXY = resultPlanesXY.get(i);
            halfEdgeScene.cutByPlane(planeXY.getPlaneType(), planeXY.getPoint(), error);
        }

        halfEdgeScene.deleteDegeneratedFaces();

        // now, distribute faces into octree
        resultOctree.getFaces().clear();
        List<HalfEdgeSurface> surfaces = halfEdgeScene.extractSurfaces(null);
        for (HalfEdgeSurface surface : surfaces) {
            List<HalfEdgeFace> faces = surface.getFaces();
            for (HalfEdgeFace face : faces) {
                if (face.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }
                resultOctree.getFaces().add(face);
            }
        }

        resultOctree.distributeFacesToTargetDepth(resultOctree.getMaxDepth());
        List<HalfEdgeOctree> octreesWithContents = new ArrayList<>();
        resultOctree.extractOctreesWithFaces(octreesWithContents);

        // now, separate the surface by the octrees
        // set the classifyId for each face
        List<HalfEdgeSurface> newSurfaces = new ArrayList<>();
        int octreesCount = octreesWithContents.size();
        for (int j = 0; j < octreesCount; j++) {
            HalfEdgeOctree octree = octreesWithContents.get(j);
            List<HalfEdgeFace> faces = octree.getFaces();
            for (HalfEdgeFace face : faces) {
                face.setClassifyId(j);
            }

            HalfEdgeSurface newSurface = createHalfEdgeSurfaceByFacesCopyCheckingClassifiedId(faces);
            newSurfaces.add(newSurface);

            // now, clear the faces of the ecTree
            octree.getFaces().clear();

            // add the new surface to the octree
            octree.getSurfaces().add(newSurface);
        }

        // now join all newSurfaces into a one surface
        HalfEdgeSurface uniqueSurface = new HalfEdgeSurface();
        for (HalfEdgeSurface newSurface : newSurfaces) {
            uniqueSurface.joinSurface(newSurface);
        }

        // create a new HalfEdgeScene
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

        // copy attributes, originalPath, boundingBox, etc
        GaiaAttribute attribute = halfEdgeScene.getAttribute();
        if (attribute != null) {
            GaiaAttribute newAttribute = attribute.getCopy();
            cuttedScene.setAttribute(newAttribute);
        }

        Path originalPath = halfEdgeScene.getOriginalPath();
        cuttedScene.setOriginalPath(originalPath);

        List<GaiaMaterial> materials = halfEdgeScene.getMaterials();
        if (materials != null) {
            List<GaiaMaterial> newMaterials = new ArrayList<>();
            for (GaiaMaterial material : materials) {
                GaiaMaterial newMaterial = material.clone();
                newMaterials.add(newMaterial);
            }
            cuttedScene.setMaterials(newMaterials);
        }

        return cuttedScene;
    }

    private static HalfEdgeSurface createHalfEdgeSurfaceByFacesCopyCheckingClassifiedId(List<HalfEdgeFace> faces) {
        HalfEdgeSurface newSurface = new HalfEdgeSurface();

        Map<HalfEdgeVertex, HalfEdgeVertex> vertexToNewVertexMap = new HashMap<>();
        Map<HalfEdge, HalfEdge> edgeToNewEdgeMap = new HashMap<>();
        Map<HalfEdgeFace, HalfEdgeFace> faceToNewFaceMap = new HashMap<>();

        List<HalfEdgeVertex> facesVertices = HalfEdgeUtils.getVerticesOfFaces(faces, null);

        // copy vertices
        for (HalfEdgeVertex vertex : facesVertices) {
            HalfEdgeVertex copyVertex = new HalfEdgeVertex();
            copyVertex.copyFrom(vertex);
            vertexToNewVertexMap.put(vertex, copyVertex);
        }

        // copy faces
        for (HalfEdgeFace face : faces) {
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeFace copyFace = new HalfEdgeFace();
            copyFace.copyFrom(face);
            faceToNewFaceMap.put(face, copyFace);
        }

        // copy edges
        Map<HalfEdge, HalfEdge> mapOriginalToCloneHalfEdge = new HashMap<>();
        List<HalfEdge> halfEdgesOfFaces = HalfEdgeUtils.getHalfEdgesOfFaces(faces, null);
        for (HalfEdge edge : halfEdgesOfFaces) {
            HalfEdge copyEdge = new HalfEdge();

            // set startVertex
            HalfEdgeVertex startVertex = edge.getStartVertex();
            HalfEdgeVertex copyStartVertex = vertexToNewVertexMap.get(startVertex);
            copyEdge.setStartVertex(copyStartVertex);
            copyStartVertex.setOutingHalfEdge(copyEdge);

            edgeToNewEdgeMap.put(edge, copyEdge);
            mapOriginalToCloneHalfEdge.put(edge, copyEdge);
        }

        // set next & face to the copy edges
        for (HalfEdge edge : halfEdgesOfFaces) {
            HalfEdge copyEdge = edgeToNewEdgeMap.get(edge);

            // set next
            HalfEdge nextEdge = edge.getNext();
            HalfEdge copyNextEdge = edgeToNewEdgeMap.get(nextEdge);
            copyEdge.setNext(copyNextEdge);

            // set face
            HalfEdgeFace face = edge.getFace();
            HalfEdgeFace copyFace = faceToNewFaceMap.get(face);
            copyEdge.setFace(copyFace);
            copyFace.setHalfEdge(copyEdge);
        }

        // original halfEdges
        List<HalfEdge> edges = new ArrayList<>(edgeToNewEdgeMap.keySet());
        for (HalfEdge edge : edges) {
            HalfEdge copyEdge = edgeToNewEdgeMap.get(edge);

            // copy face
            HalfEdgeFace face = edge.getFace();
            HalfEdgeFace copyFace = faceToNewFaceMap.get(face);

            int classifyId = face.getClassifyId();

            // copy twin (check the classifiedId of the face)
            HalfEdge twin = edge.getTwin();
            if (twin != null) {
                HalfEdgeFace twinFace = twin.getFace();
                int twinClassifyId = twinFace.getClassifyId();
                if (classifyId == twinClassifyId) {
                    HalfEdge copyTwin = edgeToNewEdgeMap.get(twin);
                    if (!copyEdge.setTwin(copyTwin))
                    {
                        log.error("Error setting twin");
                    }
                }
            }
        }

        List<HalfEdgeFace> newFaces = new ArrayList<>(faceToNewFaceMap.values());
        List<HalfEdgeVertex> newVertices = new ArrayList<>(vertexToNewVertexMap.values());
        List<HalfEdge> newEdges = new ArrayList<>(edgeToNewEdgeMap.values());

        newSurface.setVertices(newVertices);
        newSurface.setFaces(newFaces);
        newSurface.setHalfEdges(newEdges);

        newSurface.setTwins();

        return newSurface;
    }

    public static HalfEdgeSurface createHalfEdgeSurfaceByFacesCopy(List<HalfEdgeFace> faces, boolean checkClassifyId, boolean checkBestCameraDirectionType) {
        Map<HalfEdgeVertex, HalfEdgeVertex> vertexToNewVertexMap = new HashMap<>();

        List<HalfEdgeVertex> facesVertices = HalfEdgeUtils.getVerticesOfFaces(faces, null);

        // copy vertices
        for (HalfEdgeVertex vertex : facesVertices) {
            HalfEdgeVertex copyVertex = new HalfEdgeVertex();
            copyVertex.copyFrom(vertex);
            vertexToNewVertexMap.put(vertex, copyVertex);
        }

        List<HalfEdge> newHalfEdges = new ArrayList<>();
        List<HalfEdgeFace> newFaces = new ArrayList<>();

        // copy faces
        for (HalfEdgeFace face : faces) {
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            HalfEdgeFace copyFace = new HalfEdgeFace();
            copyFace.copyFrom(face);

            List<HalfEdgeVertex> faceVertices = face.getVertices(null);

            HalfEdgeVertex hVertex0 = faceVertices.get(0);
            HalfEdgeVertex hVertex1 = faceVertices.get(1);
            HalfEdgeVertex hVertex2 = faceVertices.get(2);

            HalfEdgeVertex copyVertex0 = vertexToNewVertexMap.get(hVertex0);
            HalfEdgeVertex copyVertex1 = vertexToNewVertexMap.get(hVertex1);
            HalfEdgeVertex copyVertex2 = vertexToNewVertexMap.get(hVertex2);

            HalfEdge copyEdge0 = new HalfEdge();
            copyEdge0.setStartVertex(copyVertex0);
            copyVertex0.setOutingHalfEdge(copyEdge0);
            copyEdge0.setFace(copyFace);

            HalfEdge copyEdge1 = new HalfEdge();
            copyEdge1.setStartVertex(copyVertex1);
            copyVertex1.setOutingHalfEdge(copyEdge1);
            copyEdge1.setFace(copyFace);

            HalfEdge copyEdge2 = new HalfEdge();
            copyEdge2.setStartVertex(copyVertex2);
            copyVertex2.setOutingHalfEdge(copyEdge2);
            copyEdge2.setFace(copyFace);

            copyEdge0.setNext(copyEdge1);
            copyEdge1.setNext(copyEdge2);
            copyEdge2.setNext(copyEdge0);

            copyFace.setHalfEdge(copyEdge0);

            newHalfEdges.add(copyEdge0);
            newHalfEdges.add(copyEdge1);
            newHalfEdges.add(copyEdge2);

            newFaces.add(copyFace);
        }

        List<HalfEdgeVertex> newVertices = new ArrayList<>(vertexToNewVertexMap.values());

        HalfEdgeSurface newSurface = new HalfEdgeSurface();
        newSurface.setVertices(newVertices);
        newSurface.setFaces(newFaces);
        newSurface.setHalfEdges(newHalfEdges);

        newSurface.setTwins();

        return newSurface;
    }


}
