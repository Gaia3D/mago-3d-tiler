package com.gaia3d.basic.halfedge;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WeldedFacesFinder {
    public static List<List<HalfEdgeFace>> getWeldedFacesGroups(HalfEdgeSurface surface,
                                                                List<List<HalfEdgeFace>> resultWeldedFacesGroups) {
        if (resultWeldedFacesGroups == null) {
            resultWeldedFacesGroups = new ArrayList<>();
        }

        List<HalfEdgeFace> faces = surface.getFaces();

        MapVertexAllFacesIndices resultMapVertexAllFacesIndices = HalfEdgeUtils.getMapVertexAllFacesIndices(surface);
        Set<HalfEdgeFace> mapVisitedFaces = new HashSet<>();

        int facesCount = faces.size();
        HalfEdgeUtils.setHalfEdgeFacesIdsInList(faces);
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            if (mapVisitedFaces.contains(face)) {
                continue;
            }

            List<HalfEdgeFace> weldedFaces = new ArrayList<>();
            getWeldedFacesWithFace(surface, face, weldedFaces, mapVisitedFaces, resultMapVertexAllFacesIndices);
            resultWeldedFacesGroups.add(weldedFaces);
        }

        return resultWeldedFacesGroups;
    }

    public static boolean getWeldedFacesWithFace(HalfEdgeSurface surface,
                                                 HalfEdgeFace face,
                                                 List<HalfEdgeFace> resultWeldedFaces,
                                                 Set<HalfEdgeFace> mapVisitedFaces,
                                                 MapVertexAllFacesIndices mapVertexAllFacesIndices) {
        List<HalfEdgeFace> weldedFacesAux = new ArrayList<>();
        List<HalfEdgeFace> faces = new ArrayList<>();
        faces.add(face);

        boolean finished = false;
        int counter = 0;
        List<HalfEdgeFace> memSaveAdjacentFaces = new ArrayList<>();
        List<HalfEdgeVertex> memSaveVertices = new ArrayList<>();
        List<HalfEdge> memSaveHalfEdges = new ArrayList<>();
        while (!finished)// && counter < 10000000)
        {
            List<HalfEdgeFace> newAddedfaces = new ArrayList<>();
            int facesCount = faces.size();
            for (int i = 0; i < facesCount; i++) {
                HalfEdgeFace currFace = faces.get(i);
                if (currFace.getStatus() == ObjectStatus.DELETED) {
                    continue;
                }

                if (mapVisitedFaces.contains(currFace)) {
                    continue;
                }

                resultWeldedFaces.add(currFace);
                mapVisitedFaces.add(currFace);
                weldedFacesAux.clear();
                memSaveAdjacentFaces.clear();
                memSaveVertices.clear();
                memSaveHalfEdges.clear();
                currFace.getWeldedFaces(weldedFacesAux, mapVisitedFaces, mapVertexAllFacesIndices,
                        surface.getFaces(), memSaveAdjacentFaces, memSaveVertices, memSaveHalfEdges);
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
}
