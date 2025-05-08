package com.gaia3d.basic.model;

import com.gaia3d.basic.model.structure.SurfaceStructure;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class that represents a face of a Gaia object.
 * It contains the indices and the face normal.
 * The face normal is calculated by the indices and the vertices.
 */
@Slf4j
@Getter
@Setter
public class GaiaSurface extends SurfaceStructure implements Serializable {
    public void calculateNormal(List<GaiaVertex> vertices) {
        for (GaiaFace face : faces) {
            face.calculateFaceNormal(vertices);
        }
    }

    public void calculateVertexNormals(List<GaiaVertex> vertices) {
        for (GaiaFace face : faces) {
            face.calculateFaceNormal(vertices);
        }

        Map<GaiaVertex, List<GaiaFace>> mapVertexFaces = getMapVertexFaces(vertices);
        for (Map.Entry<GaiaVertex, List<GaiaFace>> entry : mapVertexFaces.entrySet()) {
            GaiaVertex vertex = entry.getKey();
            List<GaiaFace> vertexFaces = entry.getValue();
            Vector3d normal = new Vector3d();
            for (GaiaFace face : vertexFaces) {
                normal.add(face.getFaceNormal());
            }
            normal.normalize();
            vertex.setNormal(normal);
        }
    }

    public Map<GaiaVertex, List<GaiaFace>> getMapVertexFaces(List<GaiaVertex> vertices) {
        Map<GaiaVertex, List<GaiaFace>> mapVertexFaces = new HashMap<>();
        for (GaiaFace face : faces) {
            int[] indices = face.getIndices();
            for (int index : indices) {
                GaiaVertex vertex = vertices.get(index);
                if (!mapVertexFaces.containsKey(vertex)) {
                    mapVertexFaces.put(vertex, new ArrayList<>());
                }
                mapVertexFaces.get(vertex).add(face);
            }
        }
        return mapVertexFaces;
    }

    public int[] getIndices() {
        int index = 0;
        int indicesCount = getIndicesCount();
        int[] resultIndices = new int[indicesCount];
        for (GaiaFace face : faces) {
            for (int indices : face.getIndices()) {
                resultIndices[index++] = indices;
            }
        }
        return resultIndices;
    }

    public int getIndicesCount() {
        int count = 0;
        for (GaiaFace face : faces) {
            count += face.getIndices().length;
        }
        return count;
    }

    public void clear() {
        for (GaiaFace face : faces) {
            if (face != null) {
                face.clear();
            }
        }
        faces.clear();
    }

    public GaiaSurface clone() {
        GaiaSurface clonedSurface = new GaiaSurface();
        for (GaiaFace face : faces) {
            if (face != null) {
                clonedSurface.getFaces().add(face.clone());
            }
        }
        return clonedSurface;
    }

    private boolean getFacesWeldedWithFaces(List<GaiaFace> masterFaces, List<GaiaFace> resultFaces, Map<GaiaFace, GaiaFace> mapVisitedFaces) {
        boolean newFaceAdded = false;
        Map<Integer, Integer> mapIndices = new HashMap<>();

        // make a map of indices
        for (GaiaFace face : masterFaces) {
            int[] indices = face.getIndices();
            for (int index : indices) {
                mapIndices.put(index, index);
            }
        }

        int i = 0;
        int facesCount = faces.size();
        boolean finished = false;

        while (!finished && i < facesCount) {
            boolean newFaceAddedOneLoop = false;
            for (GaiaFace currFace : faces) {
                if (!mapVisitedFaces.containsKey(currFace)) {
                    int[] currFaceIndices = currFace.getIndices();
                    // if some indices of the currFace exists in the mapIndices, then add the face to the resultFaces
                    for (int index : currFaceIndices) {
                        if (mapIndices.containsKey(index)) {
                            resultFaces.add(currFace);
                            mapVisitedFaces.put(currFace, currFace);
                            newFaceAdded = true;
                            newFaceAddedOneLoop = true;

                            // add the indices of the face to the mapIndices
                            for (int index2 : currFaceIndices) {
                                mapIndices.put(index2, index2);
                            }
                            break;
                        }
                    }
                }
            }

            if (!newFaceAddedOneLoop) {
                finished = true;
            }

            i++;
        }

        return newFaceAdded;
    }

    public void getWeldedFaces(List<List<GaiaFace>> resultWeldedFaces) {
        List<GaiaFace> weldedFaces = new ArrayList<>();
        Map<GaiaFace, GaiaFace> mapVisitedFaces = new HashMap<>();
        for (GaiaFace masterFace : faces) {
            if (mapVisitedFaces.containsKey(masterFace)) {
                continue;
            }
            mapVisitedFaces.put(masterFace, masterFace);

            List<GaiaFace> masterFaces = new ArrayList<>();
            masterFaces.add(masterFace);

            weldedFaces.clear();
            if (this.getFacesWeldedWithFaces(masterFaces, weldedFaces, mapVisitedFaces)) {
                masterFaces.addAll(weldedFaces);
                for (GaiaFace weldedFace : weldedFaces) {
                    mapVisitedFaces.put(weldedFace, weldedFace);
                }
            }

            resultWeldedFaces.add(masterFaces);
        }
    }

    public int deleteDegeneratedFaces(List<GaiaVertex> vertices) {
        List<GaiaFace> facesToDelete = new ArrayList<>();
        for (GaiaFace face : faces) {
            if (face.isDegenerated(vertices)) {
                facesToDelete.add(face);
            }
        }
        int facesToDeleteCount = facesToDelete.size();
        faces.removeAll(facesToDelete);

        return facesToDeleteCount;
    }

    public void makeTriangleFaces() {
        List<GaiaFace> facesToAdd = new ArrayList<>();
        List<GaiaFace> triFaces = new ArrayList<>();
        for (GaiaFace face : faces) {
            triFaces.clear();
            triFaces = face.getTriangleFaces(triFaces);
            facesToAdd.addAll(triFaces);
        }

        faces.clear();
        faces.addAll(facesToAdd);
    }

    public void makeTriangularFaces(List<GaiaVertex> vertices) {
        List<GaiaFace> facesToAdd = new ArrayList<>();
        List<GaiaFace> triangularFaces = new ArrayList<>();
        for (GaiaFace face : faces) {
            triangularFaces.clear();
            facesToAdd.addAll(face.getTriangleFaces(triangularFaces));
        }
        faces.clear();
        faces.addAll(facesToAdd);
    }
}
