package com.gaia3d.basic.geometry.modifier.topology;

import com.gaia3d.basic.halfedge.*;
import com.gaia3d.basic.model.GaiaScene;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class SurfaceSeparator {

    public GaiaScene separateSurfaces(GaiaScene scene) {
        HalfEdgeScene halfEdgeScene = HalfEdgeUtils.halfEdgeSceneFromGaiaScene(scene);

        List<HalfEdgeNode> nodes = halfEdgeScene.getNodes();
        for (HalfEdgeNode node : nodes) {
            separateSurfaces(node);
        }

        return HalfEdgeUtils.gaiaSceneFromHalfEdgeScene(halfEdgeScene);
    }

    public void separateSurfaces(HalfEdgeNode node) {
        List<HalfEdgeNode> nodes = node.getChildren();
        for (HalfEdgeNode childNode : nodes) {
            separateSurfaces(childNode);
        }

        List<HalfEdgeMesh> meshes = node.getMeshes();
        for (HalfEdgeMesh mesh : meshes) {

            List<HalfEdgePrimitive> primitives = mesh.getPrimitives();
            HalfEdgePrimitive firstPrimitive = primitives.get(0);
            List<HalfEdgeSurface> surfaces = firstPrimitive.getSurfaces();
            HalfEdgeSurface firstSurface = surfaces.get(0);

            List<HalfEdgeFace> faces = firstSurface.getFaces();
            List<List<HalfEdgeFace>> weldedFacesGroups = HalfEdgeUtils.getWeldedFacesGroups(faces, null);

            List<HalfEdgeSurface> separatedSurfaces = new ArrayList<>();
            for (List<HalfEdgeFace> weldedFaces : weldedFacesGroups) {
                if (weldedFaces.isEmpty()) {continue;}

                HalfEdgeSurface newSurface = new HalfEdgeSurface();
                newSurface.setFaces(weldedFaces);
                newSurface.setVertices(firstSurface.getVertices());
                newSurface.setHalfEdges(firstSurface.getHalfEdges());
                separatedSurfaces.add(newSurface);
            }
            firstPrimitive.setSurfaces(separatedSurfaces);
        }
    }
}
