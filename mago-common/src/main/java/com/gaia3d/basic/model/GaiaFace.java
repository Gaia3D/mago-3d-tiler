package com.gaia3d.basic.model;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.structure.FaceStructure;
import com.gaia3d.util.GeometryUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents a face of a Gaia object.
 * It contains the indices and the face normal.
 * The face normal is calculated by the indices and the vertices.
 */
@Slf4j
@Getter
@Setter
public class GaiaFace extends FaceStructure implements Serializable {
    private int id = -1;
    private int classifyId = -1; // use to classify the face for some purpose.

    public void calculateFaceNormal(List<GaiaVertex> vertices) {
        if (indices.length < 3) {
            log.error("[ERROR] calculateFaceNormal not enough indices. (indices.length < 3)");
            return;
        }
        for (int i = 0; i < indices.length; i += 3) {
            int indices1 = indices[i];
            int indices2 = indices[i + 1];
            int indices3 = indices[i + 2];
            GaiaVertex vertex1 = vertices.get(indices1);
            GaiaVertex vertex2 = vertices.get(indices2);
            GaiaVertex vertex3 = vertices.get(indices3);
            this.faceNormal = calcNormal(vertex1, vertex2, vertex3);
        }
    }

    public GaiaBoundingBox getBoundingBox(List<GaiaVertex> vertices, GaiaBoundingBox resultBoundingBox) {
        if (resultBoundingBox == null) {
            resultBoundingBox = new GaiaBoundingBox();
        }
        for (int index : indices) {
            GaiaVertex vertex = vertices.get(index);
            resultBoundingBox.addPoint(vertex.getPosition());
        }
        return resultBoundingBox;
    }

    public void copyFrom(GaiaFace sourceFace) {
        this.id = sourceFace.id;
        this.classifyId = sourceFace.classifyId;
        if (sourceFace.indices != null) {
            this.indices = sourceFace.indices.clone();
        } else {
            this.indices = null;
        }
        if (sourceFace.faceNormal != null) {
            this.faceNormal = new Vector3d(sourceFace.faceNormal);
        } else {
            this.faceNormal = null;
        }
    }

    public boolean validateNormal(Vector3d normal) {
        return !Double.isNaN(normal.lengthSquared()) && !Double.isNaN(normal.x()) && !Double.isNaN(normal.y()) && !Double.isNaN(normal.z()) && !Float.isNaN((float) normal.x()) && !Float.isNaN((float) normal.y()) && !Float.isNaN((float) normal.z());
    }

    public Vector3d calcNormal(Vector3d p1, Vector3d p2, Vector3d p3) {
        Vector3d p2SubP1 = new Vector3d(p2).sub(p1);
        Vector3d p3SubP2 = new Vector3d(p3).sub(p2);
        Vector3d normal = new Vector3d(p2SubP1).cross(p3SubP2);
        normal.normalize();
        p3SubP2 = null;
        p2SubP1 = null;
        return normal;
    }

    public Vector3d calcNormal(GaiaVertex vertex1, GaiaVertex vertex2, GaiaVertex vertex3) {
        Vector3d position1 = vertex1.getPosition();
        Vector3d position2 = vertex2.getPosition();
        Vector3d position3 = vertex3.getPosition();
        Vector3d resultNormal = calcNormal(position1, position2, position3);

        if (!validateNormal(resultNormal)) {
            resultNormal = new Vector3d(1.0, 1.0, 1.0);
            resultNormal.normalize();
        }
        vertex1.setNormal(new Vector3d(resultNormal));
        vertex2.setNormal(new Vector3d(resultNormal));
        vertex3.setNormal(new Vector3d(resultNormal));

        return resultNormal;
    }

    public void clear() {
        indices = null;
        faceNormal = null;
    }

    public GaiaFace clone() {
        GaiaFace cloneGaiaFace = new GaiaFace();
        cloneGaiaFace.setIndices(indices.clone());
        cloneGaiaFace.setFaceNormal(new Vector3d(faceNormal));
        return cloneGaiaFace;
    }

    public boolean hasCoincidentIndices(GaiaFace face) {
        for (int index : indices) {
            for (int j = 0; j < face.getIndices().length; j++) {
                if (index == face.getIndices()[j]) {
                    return true;
                }
            }
        }
        return false;
    }

    public double calculateArea(List<GaiaVertex> vertices) {
        double area = 0.0;
        for (int i = 0; i < indices.length; i += 3) {
            int indices1 = indices[i];
            int indices2 = indices[i + 1];
            int indices3 = indices[i + 2];
            GaiaVertex vertex1 = vertices.get(indices1);
            GaiaVertex vertex2 = vertices.get(indices2);
            GaiaVertex vertex3 = vertices.get(indices3);
            area += GeometryUtils.getTriangleArea(vertex1, vertex2, vertex3);
        }
        return area;
    }

    public boolean isDegenerated(List<GaiaVertex> vertices) {
        // if has equal indices, it is degenerated.
        for (int i = 0; i < indices.length; i++) {
            for (int j = i + 1; j < indices.length; j++) {
                if (indices[i] == indices[j]) {
                    return true;
                }
            }
        }

        if (indices.length < 3) {
            return true;
        }

        // check if has coincident positions.
        double error = 1e-5;
        for (int i = 0; i < indices.length; i += 3) {
            int indices1 = indices[i];
            int indices2 = indices[i + 1];
            int indices3 = indices[i + 2];
            GaiaVertex vertex1 = vertices.get(indices1);
            GaiaVertex vertex2 = vertices.get(indices2);
            GaiaVertex vertex3 = vertices.get(indices3);
            Vector3d vectorA = vertex1.getPosition();
            Vector3d vectorB = vertex2.getPosition();
            Vector3d vectorC = vertex3.getPosition();

            if (vectorA.distance(vectorB) < error || vectorA.distance(vectorC) < error || vectorB.distance(vectorC) < error) {
                return true;
            }

            // check if the area of the triangle is zero.
            double area = GeometryUtils.getTriangleArea(vertex1, vertex2, vertex3);
            if (area < error) {
                return true;
            }
        }


        return false;
    }

    public List<GaiaFace> getTriangleFaces(List<GaiaFace> resultGaiaFaces) {
        if (resultGaiaFaces == null) {
            resultGaiaFaces = new ArrayList<>();
        }
        int[] indices = this.getIndices();
        Vector3d normal = this.getFaceNormal();
        int indicesCount = indices.length;

        for (int i = 0; i < indicesCount - 2; i += 3) {
            if (i + 2 >= indicesCount) {
                log.error("[ERROR] i + 2 >= indicesCount.");
            }
            GaiaFace gaiaTriangleFace = new GaiaFace();
            gaiaTriangleFace.setIndices(new int[]{indices[i], indices[i + 1], indices[i + 2]});
            if (normal != null) {
                gaiaTriangleFace.setFaceNormal(new Vector3d(normal));
            }
            resultGaiaFaces.add(gaiaTriangleFace);
        }
        return resultGaiaFaces;
    }
}
