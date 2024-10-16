package com.gaia3d.basic.halfEdgeStructure;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Setter
@Getter

public class HalfEdgeFace {
    private HalfEdge halfEdge = null;
    private Vector3d normal = null;
    private ObjectStatus status = ObjectStatus.ACTIVE;

    public List<HalfEdge> getHalfEdgesLoop(List<HalfEdge> resultHalfEdgesLoop) {
        if(this.halfEdge == null) {
            return resultHalfEdgesLoop;
        }

        if(resultHalfEdgesLoop == null) {
            resultHalfEdgesLoop = new ArrayList<>();
        }

        return this.halfEdge.getLoop(resultHalfEdgesLoop);
    }

    public List<HalfEdgeVertex> getVertices(List<HalfEdgeVertex> resultVertices) {
        if(this.halfEdge == null) {
            return resultVertices;
        }

        if(resultVertices == null) {
            resultVertices = new ArrayList<>();
        }

        List<HalfEdge> halfEdgesLoop = this.halfEdge.getLoop(null);
        for(HalfEdge halfEdge : halfEdgesLoop) {
            resultVertices.add(halfEdge.getStartVertex());
        }

        return resultVertices;
    }

    public void breakRelations() {
        if(this.halfEdge == null) {
            return;
        }

        this.halfEdge = null;
    }

    public boolean isApplauseFace(HalfEdgeFace face) {
        //*****************************************************************
        // Note : 2 faces are applause faces if they have same vertices.
        //*****************************************************************
        if(face == null) {
            return false;
        }

        if(this.halfEdge == null || face.halfEdge == null) {
            return false;
        }

        List<HalfEdgeVertex> vertices = this.getVertices(null);
        Map<HalfEdgeVertex, HalfEdgeVertex> vertexMap = new HashMap<>();
        int verticesSize = vertices.size();
        for(int i=0; i<verticesSize; i++) {
            HalfEdgeVertex vertex = vertices.get(i);
            vertexMap.put(vertex, vertex);
        }

        List<HalfEdgeVertex> faceVertices = face.getVertices(null);
        int faceVerticesSize = faceVertices.size();
        for(int i=0; i<faceVerticesSize; i++) {
            HalfEdgeVertex vertex = faceVertices.get(i);
            if(vertexMap.get(vertex) == null) {
                return false;
            }
        }

        return true;
    }
}
