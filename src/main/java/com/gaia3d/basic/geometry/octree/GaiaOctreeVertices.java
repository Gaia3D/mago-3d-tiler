package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.model.GaiaVertex;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Setter
@Getter

public class GaiaOctreeVertices {

    private GaiaOctreeVertices parent = null;
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;
    private int idx = -1;
    private GaiaOctreeCoordinate coordinate = new GaiaOctreeCoordinate();
    private int maxDepth = 5;
    private double minBoxSize = 1.0;
    private GaiaOctreeVertices[] children = null;

    private List<GaiaVertex> vertices = new ArrayList<>();

    public GaiaOctreeVertices(GaiaOctreeVertices parent) {
        this.parent = parent;

        if (parent != null) {
            this.maxDepth = parent.maxDepth;
        }
    }

    public void setAsCube() {
        // Only modify the maximum values
        double x = maxX - minX;
        double y = maxY - minY;
        double z = maxZ - minZ;
        double max = Math.max(x, Math.max(y, z));
        maxX = minX + max;
        maxY = minY + max;
        maxZ = minZ + max;
    }

    public void createChildren() {
        children = new GaiaOctreeVertices[8];
        for (int i = 0; i < 8; i++) {
            children[i] = new GaiaOctreeVertices(this);
            children[i].idx = i;
            children[i].minBoxSize = this.minBoxSize;
        }

        // now set children sizes.***
        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        children[0].setSize(minX, minY, minZ, midX, midY, midZ);
        children[1].setSize(midX, minY, minZ, maxX, midY, midZ);
        children[2].setSize(midX, midY, minZ, maxX, maxY, midZ);
        children[3].setSize(minX, midY, minZ, midX, maxY, midZ);

        children[4].setSize(minX, minY, midZ, midX, midY, maxZ);
        children[5].setSize(midX, minY, midZ, maxX, midY, maxZ);
        children[6].setSize(midX, midY, midZ, maxX, maxY, maxZ);
        children[7].setSize(minX, midY, midZ, midX, maxY, maxZ);

        // now set children coords.***
        int L = this.coordinate.getDepth();
        int X = this.coordinate.getX();
        int Y = this.coordinate.getY();
        int Z = this.coordinate.getZ();
        children[0].coordinate.setDepthAndCoord(L + 1, X * 2, Y * 2, Z * 2);
        children[1].coordinate.setDepthAndCoord(L + 1, X * 2 + 1, Y * 2, Z * 2);
        children[2].coordinate.setDepthAndCoord(L + 1, X * 2 + 1, Y * 2 + 1, Z * 2);
        children[3].coordinate.setDepthAndCoord(L + 1, X * 2, Y * 2 + 1, Z * 2);

        children[4].coordinate.setDepthAndCoord(L + 1, X * 2, Y * 2, Z * 2 + 1);
        children[5].coordinate.setDepthAndCoord(L + 1, X * 2 + 1, Y * 2, Z * 2 + 1);
        children[6].coordinate.setDepthAndCoord(L + 1, X * 2 + 1, Y * 2 + 1, Z * 2 + 1);
        children[7].coordinate.setDepthAndCoord(L + 1, X * 2, Y * 2 + 1, Z * 2 + 1);
    }

    public void makeTreeByMinBoxSize(double minBoxSize) {
        if ((maxX - minX) < minBoxSize || (maxY - minY) < minBoxSize || (maxZ - minZ) < minBoxSize) {
            return;
        }

        if (this.coordinate.getDepth() >= maxDepth) {
            return;
        }

        if (vertices.isEmpty()) {
            return;
        }

        createChildren();
        distributeContents();

        for (GaiaOctreeVertices child : children) {
            child.makeTreeByMinBoxSize(minBoxSize);
        }
    }

    public void makeTreeByMinVertexCount(int minVertexCount) {
        if (this.coordinate.getDepth() >= maxDepth) {
            return;
        }

        if (vertices.isEmpty()) {
            return;
        }

        int vertexCount = vertices.size();
        if (vertexCount < minVertexCount) {
            return;
        }

        double sizeX = maxX - minX;
        if (sizeX < minBoxSize) {
            return;
        }

        createChildren();
        distributeContents();

        for (GaiaOctreeVertices child : children) {
            child.makeTreeByMinVertexCount(minVertexCount);
        }
    }

    public void printVertices() {
        if (!vertices.isEmpty() && children == null) {
            //log.info("vertices.size() : {}", vertices.size());
        }

        if (children != null) {
            for (GaiaOctreeVertices child : children) {
                child.printVertices();
            }
        }
    }

    public void reduceVerticesByDistance() {
        /*GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        for (GaiaVertex vertex : vertices) {
            boundingBox.addPoint(vertex.getPosition());
        }
        Vector3d centerPosition = boundingBox.getCenter();*/
        List<GaiaVertex> newVertices = new ArrayList<>();
        if (!vertices.isEmpty()) {
            GaiaVertex vertex = vertices.get(0);
            Vector3d vertexPosition = vertex.getPosition();
            for (int i = 1; i < vertices.size(); i++) {
                GaiaVertex compareVertex = vertices.get(i);
                Vector3d comparePosition = compareVertex.getPosition();
                if (vertexPosition.distance(comparePosition) < 0.003) {
                    //
                } else {
                    newVertices.add(compareVertex);
                }
            }
        }
        vertices = newVertices;
    }

    /*public void reduceVerticesByDistance() {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        for (GaiaVertex vertex : vertices) {
            boundingBox.addPoint(vertex.getPosition());
        }
        Vector3d center = boundingBox.getCenter();
        GaiaVertex temp = vertices.get(0);
        temp.setPosition(center);

        vertices = new ArrayList<>();
        vertices.add(temp);
        *//*if (!vertices.isEmpty()) {
            GaiaVertex vertex = vertices.get(0);
            for (int i = 1; i < vertices.size(); i++) {
                GaiaVertex compareVertex = vertices.get(i);
                if (vertex.getPosition().distance(compareVertex.getPosition()) < minBoxSize) {
                    vertices.remove(i);
                    i--;
                }
            }
        }*//*
    }*/

    public void reduceVertices(int maxVertexCount) {

        if (!vertices.isEmpty() && children == null) {
            reduceVerticesByDistance();
            if (vertices.size() > maxVertexCount) {
                reduceVerticesByDistance();
            }

            /*if (vertices.size() > maxVertexCount) {
                //log.info("vertices.size() : {}", vertices.size());
                //reduceVerticesByDistance();
                createChildren();
                distributeContents();
                for (GaiaOctreeVertices child : children) {
                    child.reduceVertices(maxVertexCount);
                }
            } else if (vertices.size() > 10) {
                reduceVerticesByDistance();
            }*/
        }

        if (children != null) {
            for (GaiaOctreeVertices child : children) {
                child.reduceVertices(maxVertexCount);
            }
        }

        printVertices();
    }

    public int findMaximumDepth() {
        int maxDepth = this.coordinate.getDepth();
        if (children == null) {
            return maxDepth;
        }

        for (GaiaOctreeVertices child : children) {
            int depth = child.findMaximumDepth();
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }
        return maxDepth;
    }

    public List<GaiaVertex> getAllVertices(List<GaiaVertex> allVertices) {
        if (allVertices == null) {
            allVertices = new ArrayList<>();
        }
        allVertices.addAll(vertices);
        if (children != null) {
            for (GaiaOctreeVertices child : children) {
                child.getAllVertices(allVertices);
            }
        }
        return allVertices;
    }

    public int calculateVerticesCount(int[] count) {
        if (count == null) {
            count = new int[1];
        }
        count[0] += vertices.size();
        if (children != null) {
            for (GaiaOctreeVertices child : children) {
                child.calculateVerticesCount(count);
            }
        }
        return count[0];
    }

    public void calculateSize() {
        int verticesCount = vertices.size();
        if (verticesCount == 0) {
            return;
        }

        minX = Double.MAX_VALUE;
        minY = Double.MAX_VALUE;
        minZ = Double.MAX_VALUE;
        maxX = -Double.MAX_VALUE;
        maxY = -Double.MAX_VALUE;
        maxZ = -Double.MAX_VALUE;

        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            if (position.x < minX) {
                minX = position.x;
            }
            if (position.y < minY) {
                minY = position.y;
            }
            if (position.z < minZ) {
                minZ = position.z;
            }
            if (position.x > maxX) {
                maxX = position.x;
            }
            if (position.y > maxY) {
                maxY = position.y;
            }
            if (position.z > maxZ) {
                maxZ = position.z;
            }
        }
    }

    public void setSize(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public void distributeContents() {
        if (vertices.isEmpty()) {
            return;
        }

        double midX = (minX + maxX) / 2.0;
        double midY = (minY + maxY) / 2.0;
        double midZ = (minZ + maxZ) / 2.0;

        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            if (position.x < midX) {
                if (position.y < midY) {
                    if (position.z < midZ) {
                        children[0].addVertex(vertex);
                    } else {
                        children[4].addVertex(vertex);
                    }
                } else {
                    if (position.z < midZ) {
                        children[3].addVertex(vertex);
                    } else {
                        children[7].addVertex(vertex);
                    }
                }
            } else {
                if (position.y < midY) {
                    if (position.z < midZ) {
                        children[1].addVertex(vertex);
                    } else {
                        children[5].addVertex(vertex);
                    }
                } else {
                    if (position.z < midZ) {
                        children[2].addVertex(vertex);
                    } else {
                        children[6].addVertex(vertex);
                    }
                }
            }
        }

        // clear the vertices list.***
        vertices.clear();
    }

    private void addVertex(GaiaVertex vertex) {
        vertices.add(vertex);
    }

    public void extractOctreesWithContents(List<GaiaOctreeVertices> octrees) {
        if (!vertices.isEmpty()) {
            octrees.add(this);
        }

        if (children != null) {
            for (GaiaOctreeVertices child : children) {
                child.extractOctreesWithContents(octrees);
            }
        }
    }
}
