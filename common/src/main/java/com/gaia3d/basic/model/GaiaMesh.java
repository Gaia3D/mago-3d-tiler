package com.gaia3d.basic.model;

import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.structure.MeshStructure;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents a mesh of a Gaia object.
 * It contains the primitives.
 * The primitives are used for rendering.
 */
@Slf4j
@Getter
@Setter
public class GaiaMesh extends MeshStructure implements Serializable {
    public GaiaBoundingBox getBoundingBox(Matrix4d transform) {
        GaiaBoundingBox boundingBox = null;
        for (GaiaPrimitive primitive : primitives) {
            GaiaBoundingBox primitiveBoundingBox = primitive.getBoundingBox(transform);
            if (boundingBox == null) {
                boundingBox = primitiveBoundingBox;
            } else {
                boundingBox.addBoundingBox(primitiveBoundingBox);
            }
        }
        return boundingBox;
    }

    public long getTriangleCount() {
        long count = 0;
        for (GaiaPrimitive primitive : primitives) {
            count += getIndicesCount() / 3;
        }
        return count;
    }

    public int[] getIndices() {
        int[] totalIndices = new int[getIndicesCount()];
        int index = 0;
        for (GaiaPrimitive primitive : primitives) {
            for (int indices : primitive.getIndices()) {
                totalIndices[index++] = indices;
            }
        }
        return totalIndices;
    }

    public int getPositionsCount() {
        int count = 0;
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                Vector3d position = vertex.getPosition();
                if (position != null) {
                    count += 3;
                }
            }
        }
        return count;
    }

    public float[] getPositions() {
        float[] totalVertices = new float[getPositionsCount()];
        int index = 0;
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                Vector3d position = vertex.getPosition();
                if (position != null) {
                    totalVertices[index++] = (float) position.x();
                    totalVertices[index++] = (float) position.y();
                    totalVertices[index++] = (float) position.z();
                }
            }
        }
        return totalVertices;
    }

    public int getNormalsCount() {
        int count = 0;
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                Vector3d normal = vertex.getNormal();
                if (normal != null) {
                    count += 3;
                }
            }
        }
        return count;
    }

    public float[] getNormals() {
        float[] totalNormals = new float[getNormalsCount()];
        int index = 0;
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                Vector3d normal = vertex.getNormal();
                if (normal != null) {
                    totalNormals[index++] = (float) normal.x();
                    totalNormals[index++] = (float) normal.y();
                    totalNormals[index++] = (float) normal.z();
                }
            }
        }
        return totalNormals;
    }

    public int getTexcoordsCount() {
        int count = 0;
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                Vector2d texcoords = vertex.getTexcoords();
                if (texcoords != null) {
                    count += 2;
                }
            }
        }
        return count;
    }

    public float[] getTexcoords() {
        float[] totalTexcoords = new float[getTexcoordsCount()];
        int index = 0;
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                Vector2d texcoords = vertex.getTexcoords();
                if (texcoords != null) {
                    totalTexcoords[index++] = (float) texcoords.x();
                    totalTexcoords[index++] = (float) texcoords.y();
                }
            }
        }
        return totalTexcoords;
    }

    public int getBatchIdsCount() {
        int count = 0;
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                float batchId = vertex.getBatchId();
                if (batchId >= 0) {
                    count += 1;
                }
            }
        }
        return count;
    }

    public float[] getBatchIds() {
        float[] totalBatchIds = new float[getBatchIdsCount()];
        int index = 0;
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                float batchId = vertex.getBatchId();
                totalBatchIds[index++] = batchId;
            }
        }
        return totalBatchIds;
    }

    public int getColorsCount() {
        int count = 0;
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                byte[] color = vertex.getColor();
                if (color != null) {
                    count += 4;
                }
            }
        }
        return count;
    }

    public byte[] getColors() {
        byte[] totalColors = new byte[getColorsCount()];
        int index = 0;
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                byte[] color = vertex.getColor();
                if (color != null) {
                    totalColors[index++] = color[0];
                    totalColors[index++] = color[1];
                    totalColors[index++] = color[2];
                    totalColors[index++] = color[3];
                }
            }
        }
        return totalColors;
    }

    public int getIndicesCount() {
        int totalIndices = 0;
        for (GaiaPrimitive primitive : primitives) {
            totalIndices += primitive.getIndices().length;
        }
        return totalIndices;
    }

    public void toGaiaBufferSets(List<GaiaBufferDataSet> bufferSets, Matrix4d transformMatrix) {
        if (bufferSets == null) {
            bufferSets = new ArrayList<>();
        }
        for (GaiaPrimitive primitive : primitives) {
            if (primitive.getVertices().size() < 3) {
                log.debug("The primitive has less than 3 vertices. It will be ignored.");
                continue;
            }
            GaiaBufferDataSet gaiaBufferDataSet = primitive.toGaiaBufferSet(transformMatrix);
            gaiaBufferDataSet.setMaterialId(primitive.getMaterialIndex());
            bufferSets.add(gaiaBufferDataSet);
        }
    }

    public void doNormalLengthUnitary() {
        for (GaiaPrimitive primitive : primitives) {
            primitive.doNormalLengthUnitary();
        }
    }

    public void deleteNormals() {
        for (GaiaPrimitive primitive : primitives) {
            primitive.deleteNormals();
        }
    }

    public void translate(Vector3d translation) {
        for (GaiaPrimitive primitive : primitives) {
            primitive.translate(translation);
        }
    }

    public void clear() {
        this.primitives.forEach(GaiaPrimitive::clear);
        //this.primitives.clear();
    }

    public GaiaMesh clone() {
        GaiaMesh gaiaMesh = new GaiaMesh();
        gaiaMesh.setPrimitives(new ArrayList<>());
        for (GaiaPrimitive primitive : this.primitives) {
            gaiaMesh.getPrimitives().add(primitive.clone());
        }
        return gaiaMesh;
    }

    public void weldVertices(double error, boolean checkTexCoord, boolean checkNormal, boolean checkColor, boolean checkBatchId) {
        int primitivesCount = primitives.size();
        for (int i = 0; i < primitivesCount; i++) {
            GaiaPrimitive primitive = primitives.get(i);
            primitive.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
            primitive.deleteNoUsedVertices();
        }
    }

    public void unWeldVertices() {
        for (GaiaPrimitive primitive : primitives) {
            primitive.unWeldVertices();
        }
    }

    public List<GaiaFace> extractGaiaFaces(List<GaiaFace> resultFaces) {
        if (resultFaces == null) {
            resultFaces = new ArrayList<>();
        }
        for (GaiaPrimitive primitive : primitives) {
            primitive.extractGaiaFaces(resultFaces);
        }
        return resultFaces;
    }

    public void deleteObjects() {
        for (GaiaPrimitive primitive : primitives) {
            primitive.deleteObjects();
        }
    }

    public void deleteDegeneratedFaces() {
        for (GaiaPrimitive primitive : primitives) {
            primitive.deleteDegeneratedFaces();
        }
    }

    public void extractPrimitives(List<GaiaPrimitive> resultPrimitives) {
        if (resultPrimitives == null) {
            resultPrimitives = new ArrayList<>();
        }
        resultPrimitives.addAll(primitives);
    }

    public void makeTriangleFaces() {
        for (GaiaPrimitive primitive : primitives) {
            primitive.makeTriangleFaces();
        }
    }

    public void transformPoints(Matrix4d finalMatrix) {
        for (GaiaPrimitive primitive : primitives) {
            primitive.transformPoints(finalMatrix);
        }
    }

    public void makeTriangularFaces() {
        for (GaiaPrimitive primitive : primitives) {
            primitive.makeTriangularFaces();
        }
    }

    public int getFacesCount() {
        int facesCount = 0;
        for (GaiaPrimitive primitive : primitives) {
            facesCount += primitive.getFacesCount();
        }
        return facesCount;
    }
}
