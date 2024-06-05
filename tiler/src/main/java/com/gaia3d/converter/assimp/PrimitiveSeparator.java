package com.gaia3d.converter.assimp;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaFace;
import com.gaia3d.basic.structure.GaiaPrimitive;
import com.gaia3d.basic.structure.GaiaSurface;
import com.gaia3d.basic.structure.GaiaVertex;
import com.gaia3d.converter.geometry.GaiaTriangle;
import lombok.NoArgsConstructor;
import org.joml.Vector3d;

import java.util.*;

@NoArgsConstructor
public class PrimitiveSeparator {

    private GaiaPrimitive originalPrimitive;
    private List<GaiaPrimitive> newPrimitives;
    private List<GaiaVertex> globlVertices;

    public List<GaiaPrimitive> separatePrimitives(GaiaPrimitive primitive, int threshold) {
        newPrimitives = new ArrayList<>();
        globlVertices = primitive.getVertices();
        originalPrimitive = primitive;
        List<GaiaSurface> surfaces = primitive.getSurfaces();

        List<List<GaiaVertex>> totalTriangles = getTotalTriangles(surfaces);
        GaiaBoundingBox boundingBox = getBoundingBox(totalTriangles);

        distributeTrianglesAtOctree(totalTriangles, boundingBox, threshold * 10);

        /*if (totalTriangles.size() <= threshold) {
            distributeTriangles(totalTriangles, boundingBox, threshold);
        } else {
            newPrimitives.add(createPrimitive(totalTriangles));
        }*/
        return newPrimitives;
    }

    private GaiaPrimitive createPrimitive(List<List<GaiaVertex>> triangles) {
        GaiaPrimitive primitive = new GaiaPrimitive();

        List<Integer> indices = new ArrayList<>();
        List<GaiaVertex> vertices = new ArrayList<>();
        List<GaiaSurface> surfaces = new ArrayList<>();

        Map<Vector3d, Integer> vertexMap = new HashMap<>();
        for (List<GaiaVertex> triangle : triangles) {
            for (GaiaVertex vertex : triangle) {
                Vector3d position = vertex.getPosition();
                if (!vertexMap.containsKey(position)) {
                    vertexMap.put(position, vertices.size());
                    vertices.add(vertex.clone());
                }
                indices.add(vertexMap.get(position));
            }
        }

        List<GaiaFace> faces = new ArrayList<>();
        for (int i = 0; i < indices.size(); i+=3) {
            int index1 = indices.get(i);
            int index2 = indices.get(i + 1);
            int index3 = indices.get(i + 2);
            GaiaFace face = new GaiaFace();
            face.setIndices(new int[] {index1, index2, index3});
            faces.add(face);
        }

        GaiaSurface surface = new GaiaSurface();
        surface.setFaces(faces);
        surfaces.add(surface);

        primitive.setMaterialIndex(originalPrimitive.getMaterialIndex());
        primitive.setVertices(vertices);
        primitive.setSurfaces(surfaces);
        return primitive;
    }

    private void distributeTriangles(List<List<GaiaVertex>> totalTriangles, GaiaBoundingBox boundingBox, int threshold) {
        List<List<GaiaVertex>> trianglesA = new ArrayList<>();
        List<List<GaiaVertex>> trianglesB = new ArrayList<>();
        List<List<GaiaVertex>> trianglesC = new ArrayList<>();
        List<List<GaiaVertex>> trianglesD = new ArrayList<>();

        double minX = boundingBox.getMinX();
        double minY = boundingBox.getMinY();
        double maxX = boundingBox.getMaxX();
        double maxY = boundingBox.getMaxY();
        double midX = (minX + maxX) / 2;
        double midY = (minY + maxY) / 2;

        totalTriangles.forEach(triangle -> {
            GaiaTriangle gaiaTriangle = new GaiaTriangle(triangle.get(0).getPosition(), triangle.get(1).getPosition(), triangle.get(2).getPosition());

            Vector3d center = gaiaTriangle.getCenter();
            if (center.x < midX && center.y < midY) {
                trianglesA.add(triangle);
            } else if (center.x < midX && center.y >= midY) {
                trianglesB.add(triangle);
            } else if (center.x >= midX && center.y < midY) {
                trianglesC.add(triangle);
            } else {
                trianglesD.add(triangle);
            }
        });

        if ((trianglesA.size()) > threshold) {
            GaiaBoundingBox boundingBoxA = getBoundingBox(trianglesA);
            distributeTriangles(trianglesA, boundingBoxA, threshold);
        } else {
            newPrimitives.add(createPrimitive(trianglesA));
        }

        if ((trianglesB.size()) > threshold) {
            GaiaBoundingBox boundingBoxB = getBoundingBox(trianglesB);
            distributeTriangles(trianglesB, boundingBoxB, threshold);
        } else {
            newPrimitives.add(createPrimitive(trianglesB));
        }

        if ((trianglesC.size()) > threshold) {
            GaiaBoundingBox boundingBoxC = getBoundingBox(trianglesC);
            distributeTriangles(trianglesC, boundingBoxC, threshold);
        } else {
            newPrimitives.add(createPrimitive(trianglesC));
        }

        if ((trianglesD.size()) > threshold) {
            GaiaBoundingBox boundingBoxD = getBoundingBox(trianglesD);
            distributeTriangles(trianglesD, boundingBoxD, threshold);
        } else {
            newPrimitives.add(createPrimitive(trianglesD));
        }
    }

    private void distributeTrianglesAtOctree(List<List<GaiaVertex>> totalTriangles, GaiaBoundingBox boundingBox, int threshold) {
        List<List<GaiaVertex>> trianglesA = new ArrayList<>();
        List<List<GaiaVertex>> trianglesB = new ArrayList<>();
        List<List<GaiaVertex>> trianglesC = new ArrayList<>();
        List<List<GaiaVertex>> trianglesD = new ArrayList<>();
        List<List<GaiaVertex>> trianglesE = new ArrayList<>();
        List<List<GaiaVertex>> trianglesF = new ArrayList<>();
        List<List<GaiaVertex>> trianglesG = new ArrayList<>();
        List<List<GaiaVertex>> trianglesH = new ArrayList<>();

        double minX = boundingBox.getMinX();
        double minY = boundingBox.getMinY();
        double minZ = boundingBox.getMinZ();
        double maxX = boundingBox.getMaxX();
        double maxY = boundingBox.getMaxY();
        double maxZ = boundingBox.getMaxZ();
        double midX = (minX + maxX) / 2;
        double midY = (minY + maxY) / 2;
        double midZ = (minZ + maxZ) / 2;

        totalTriangles.forEach(triangle -> {
            GaiaTriangle gaiaTriangle = new GaiaTriangle(triangle.get(0).getPosition(), triangle.get(1).getPosition(), triangle.get(2).getPosition());

            Vector3d center = gaiaTriangle.getCenter();
            if (center.x < midX && center.y < midY && center.z < midZ) {
                trianglesA.add(triangle);
            } else if (center.x < midX && center.y >= midY && center.z < midZ) {
                trianglesB.add(triangle);
            } else if (center.x >= midX && center.y < midY && center.z < midZ) {
                trianglesC.add(triangle);
            } else if (center.x >= midX && center.y >= midY && center.z < midZ) {
                trianglesD.add(triangle);
            } else if (center.x < midX && center.y < midY && center.z >= midZ) {
                trianglesE.add(triangle);
            } else if (center.x < midX && center.y >= midY && center.z >= midZ) {
                trianglesF.add(triangle);
            } else if (center.x >= midX && center.y < midY && center.z >= midZ) {
                trianglesG.add(triangle);
            } else {
                trianglesH.add(triangle);
            }
        });

        if ((trianglesA.size()) > threshold) {
            GaiaBoundingBox boundingBoxA = getBoundingBox(trianglesA);
            distributeTrianglesAtOctree(trianglesA, boundingBoxA, threshold);
        } else {
            newPrimitives.add(createPrimitive(trianglesA));
        }

        if ((trianglesB.size()) > threshold) {
            GaiaBoundingBox boundingBoxB = getBoundingBox(trianglesB);
            distributeTrianglesAtOctree(trianglesB, boundingBoxB, threshold);
        } else {
            newPrimitives.add(createPrimitive(trianglesB));
        }

        if ((trianglesC.size()) > threshold) {
            GaiaBoundingBox boundingBoxC = getBoundingBox(trianglesC);
            distributeTrianglesAtOctree(trianglesC, boundingBoxC, threshold);
        } else {
            newPrimitives.add(createPrimitive(trianglesC));
        }

        if ((trianglesD.size()) > threshold) {
            GaiaBoundingBox boundingBoxD = getBoundingBox(trianglesD);
            distributeTrianglesAtOctree(trianglesD, boundingBoxD, threshold);
        } else {
            newPrimitives.add(createPrimitive(trianglesD));
        }

        if ((trianglesE.size()) > threshold) {
            GaiaBoundingBox boundingBoxE = getBoundingBox(trianglesE);
            distributeTrianglesAtOctree(trianglesE, boundingBoxE, threshold);
        } else {
            newPrimitives.add(createPrimitive(trianglesE));
        }

        if ((trianglesF.size()) > threshold) {
            GaiaBoundingBox boundingBoxF = getBoundingBox(trianglesF);
            distributeTrianglesAtOctree(trianglesF, boundingBoxF, threshold);
        } else {
            newPrimitives.add(createPrimitive(trianglesF));
        }

        if ((trianglesG.size()) > threshold) {
            GaiaBoundingBox boundingBoxG = getBoundingBox(trianglesG);
            distributeTrianglesAtOctree(trianglesG, boundingBoxG, threshold);
        } else {
            newPrimitives.add(createPrimitive(trianglesG));
        }

        if ((trianglesH.size()) > threshold) {
            GaiaBoundingBox boundingBoxH = getBoundingBox(trianglesH);
            distributeTrianglesAtOctree(trianglesH, boundingBoxH, threshold);
        } else {
            newPrimitives.add(createPrimitive(trianglesH));
        }
    }

    private GaiaBoundingBox getBoundingBox(List<List<GaiaVertex>> triangles) {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        triangles.forEach(triangle -> {
            triangle.forEach(vertex -> {
                boundingBox.addPoint(vertex.getPosition());
            });
        });
        return boundingBox;
    }

    private List<List<GaiaVertex>> getTotalTriangles(List<GaiaSurface> surfaces) {
        List<List<GaiaVertex>> totalTriangles = new ArrayList<>();
        surfaces.forEach(surface -> {
            List<GaiaFace> faces = surface.getFaces();
            faces.forEach(face -> {
                int[] indices = face.getIndices();
                List<List<GaiaVertex>> triangles = getTriangles(indices);
                totalTriangles.addAll(triangles);
            });
        });
        return totalTriangles;
    }

    private List<List<GaiaVertex>> getTriangles(int[] indices) {
        List<List<GaiaVertex>> triangles = new ArrayList<>();
        for (int i = 0; i < indices.length; i+=3) {
            int indices1 = indices[i];
            int indices2 = indices[i + 1];
            int indices3 = indices[i + 2];
            GaiaVertex vertex1 = globlVertices.get(indices1);
            GaiaVertex vertex2 = globlVertices.get(indices2);
            GaiaVertex vertex3 = globlVertices.get(indices3);
            List<GaiaVertex> triangle = new ArrayList<>();
            triangle.add(vertex1);
            triangle.add(vertex2);
            triangle.add(vertex3);
            triangles.add(triangle);
        }
        return triangles;
    }
}
