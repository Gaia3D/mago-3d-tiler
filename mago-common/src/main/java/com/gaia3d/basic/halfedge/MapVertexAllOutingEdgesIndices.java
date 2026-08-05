package com.gaia3d.basic.halfedge;

public class MapVertexAllOutingEdgesIndices {
    //******************************************************************************************************************
    // Note: this function supplies Map<HalfEdgeVertex, List<HalfEdge>> vertexAllOutingEdgesMap in a compact form.
    // vertexOffsets is the array of offsets for each vertex, and vertexOutingEdges is the array of edge indices for all vertices.
    //******************************************************************************************************************
    private final int[] vertexOffsets;
    private final int[] vertexOutingEdges;

    public MapVertexAllOutingEdgesIndices(int[] vertexOffsets, int[] vertexOutingEdges) {
        this.vertexOffsets = vertexOffsets;
        this.vertexOutingEdges = vertexOutingEdges;
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

    public int getEdgeIndex(int i) {
        return vertexOutingEdges[i];
    }

    public int getEdgeIndexOfVertex(int vertexIdx, int edgeLocalIdx) {
        int start = vertexOffsets[vertexIdx];
        int end = vertexOffsets[vertexIdx + 1];
        int edgeIdx = start + edgeLocalIdx;
        if (edgeLocalIdx < 0 || edgeIdx >= end) {
            return -1;
        }
        return vertexOutingEdges[edgeIdx];
    }

    public int getEdgesCountOfVertex(int vertexIdx) {
        return vertexOffsets[vertexIdx + 1] - vertexOffsets[vertexIdx];
    }

    public int[] getEdgeIndices(int vertexIdx) {
        int start = getStart(vertexIdx);
        int end = getEnd(vertexIdx);

        int faceCount = end - start;
        int[] edgeIndices = new int[faceCount];

        for (int i = 0; i < faceCount; i++) {
            edgeIndices[i] = vertexOutingEdges[start + i];
        }
        return edgeIndices;
    }

    public void forEachFace(int vertexIdx, java.util.function.IntConsumer consumer) {
        int start = vertexOffsets[vertexIdx];
        int end = vertexOffsets[vertexIdx + 1];

        for (int i = start; i < end; i++) {
            consumer.accept(vertexOutingEdges[i]);
        }
    }
}
