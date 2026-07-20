package com.gaia3d.basic.halfedge;

public class MapVertexAllFacesIndices {
    //******************************************************************************************************************
    // Note: this function supplies Map<HalfEdgeVertex, List<HalfEdgeFace>> vertexFacesMap in a compact form.
    // vertexOffsets is the array of offsets for each vertex, and vertexFaces is the array of face indices for all vertices.
    //******************************************************************************************************************
    private int[] vertexOffsets;
    private int[] vertexFaces;

    public MapVertexAllFacesIndices(int[] vertexOffsets, int[] vertexFaces) {
        this.vertexOffsets = vertexOffsets;
        this.vertexFaces = vertexFaces;
    }

    public void deleteObjects() {
        vertexOffsets = null;
        vertexFaces = null;
    }

    public int getVertexCount() {
        return vertexOffsets.length - 1;
    }

    public int getStart(int vertexIdx) {
        return vertexOffsets[vertexIdx];
    }

    public int getEnd(int vertexIdx) {
        return vertexOffsets[vertexIdx + 1];
    }

    public int getFaceIndex(int i) {
        return vertexFaces[i];
    }

    public int getFaceIndexOfVertex(int vertexIdx, int faceLocalIdx) {
        int start = vertexOffsets[vertexIdx];
        int end = vertexOffsets[vertexIdx + 1];
        int faceIdx = start + faceLocalIdx;
        if (faceLocalIdx < 0 || faceIdx >= end) {
            return -1;
        }
        return vertexFaces[faceIdx];
    }

    public int getFaceCountOfVertex(int vertexIdx) {
        return vertexOffsets[vertexIdx + 1] - vertexOffsets[vertexIdx];
    }

    public int[] getFaceIndices(int vertexIdx) {
        int start = getStart(vertexIdx);
        int end = getEnd(vertexIdx);

        int faceCount = end - start;
        int[] faceIndices = new int[faceCount];

        for (int i = 0; i < faceCount; i++) {
            faceIndices[i] = vertexFaces[start + i];
        }
        return faceIndices;
    }

    public void forEachFace(int vertexIdx, java.util.function.IntConsumer consumer) {
        int start = vertexOffsets[vertexIdx];
        int end = vertexOffsets[vertexIdx + 1];

        for (int i = start; i < end; i++) {
            consumer.accept(vertexFaces[i]);
        }
    }

}
