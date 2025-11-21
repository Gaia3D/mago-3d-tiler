package com.gaia3d.basic.geometry.modifier;

import com.gaia3d.basic.halfedge.HalfEdge;
import com.gaia3d.basic.halfedge.HalfEdgeFace;
import com.gaia3d.basic.halfedge.HalfEdgeSurface;
import com.gaia3d.basic.halfedge.HalfEdgeVertex;
import com.gaia3d.basic.model.*;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class GaiaTriangleReduction {
    public GaiaScene reduceScene(GaiaScene scene) {
        GaiaScene resultReducedScene = new GaiaScene();
        for (GaiaNode node : scene.getNodes()) {
            GaiaNode resultReducedNode = this.reduceNode(node);
            resultReducedScene.getNodes().add(resultReducedNode);
        }

        return resultReducedScene;
    }

    public GaiaNode reduceNode(GaiaNode node) {
        GaiaNode resultReducedNode = new GaiaNode();
        for (GaiaMesh mesh : node.getMeshes()) {
            GaiaMesh resultReducedMesh = this.reduceMesh(mesh);
            resultReducedNode.getMeshes().add(resultReducedMesh);
        }

        return resultReducedNode;
    }

    public GaiaMesh reduceMesh(GaiaMesh mesh) {
        GaiaMesh resultReducedMesh = new GaiaMesh();
        for (GaiaPrimitive primitive : mesh.getPrimitives()) {
            GaiaPrimitive resultReducedPrimitive = this.reducePrimitive(primitive);
            resultReducedMesh.getPrimitives().add(resultReducedPrimitive);
        }

        return resultReducedMesh;
    }

    public GaiaPrimitive reducePrimitive(GaiaPrimitive primitive) {
        GaiaPrimitive resultReducedPrimitive = new GaiaPrimitive();
        for (GaiaSurface surface : primitive.getSurfaces()) {
            GaiaSurface resultReducedSurface = this.reduceSurface(surface, primitive.getVertices());
            resultReducedPrimitive.getSurfaces().add(resultReducedSurface);
        }
        return resultReducedPrimitive;
    }

    public GaiaSurface reduceSurface(GaiaSurface surface, List<GaiaVertex> vertices) {
        // 1rst, create a halfEdgeSurface
        HalfEdgeSurface halfEdgeSurface = this.getHalfEdgeSurface(surface, vertices);

        GaiaSurface resultReducedSurface = new GaiaSurface();
        return resultReducedSurface;
    }

    public HalfEdgeSurface getHalfEdgeSurface(GaiaSurface surface, List<GaiaVertex> vertices) {
        HalfEdgeSurface halfEdgeSurface = new HalfEdgeSurface();

        for (GaiaFace face : surface.getFaces()) {
            HalfEdgeFace halfEdgeFace = this.getHalfEdgeFace(face, vertices, halfEdgeSurface);
        }

        halfEdgeSurface.setTwins();

        return halfEdgeSurface;
    }

    public HalfEdgeFace getHalfEdgeFace(GaiaFace face, List<GaiaVertex> vertices, HalfEdgeSurface halfEdgeSurface) {
        HalfEdgeFace halfEdgeFace = new HalfEdgeFace();
        int[] indices = face.getIndices();
        int indicesCount = indices.length;
        if (indicesCount < 3) {
            return null;
        }

        HalfEdge halfEdge1rst = null;
        HalfEdge halfEdgelast = null;
        for (int i = 0; i < indicesCount; i++) {
            int vertexIndex = indices[i];
            GaiaVertex vertex = vertices.get(vertexIndex);
            HalfEdgeVertex halfEdgeVertex = new HalfEdgeVertex(vertex);
            HalfEdge halfEdge = new HalfEdge();

            halfEdge.setStartVertex(halfEdgeVertex);
            halfEdgeVertex.setOutingHalfEdge(halfEdge);
            halfEdge.setFace(halfEdgeFace);
            halfEdgeFace.setHalfEdge(halfEdge);

            if (i == 0) {
                halfEdge1rst = halfEdge;
            }

            if (halfEdgelast != null) {
                halfEdgelast.setNext(halfEdge);
            }

            // Finally set the halfEdgeLast
            halfEdgelast = halfEdge;

            halfEdgeSurface.getHalfEdges().add(halfEdge);
            halfEdgeSurface.getVertices().add(halfEdgeVertex);
        }

        halfEdgelast.setNext(halfEdge1rst); // close the loop

        // Add the face to the surface
        halfEdgeSurface.getFaces().add(halfEdgeFace);

        return halfEdgeFace;
    }
}
