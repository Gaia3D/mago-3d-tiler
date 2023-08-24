package com.gaia3d.basic.structure;

import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;

/**
 * A class that represents a mesh of a Gaia object.
 * It contains the primitives.
 * The primitives are used for rendering.
 * @author znkim
 * @since 1.0.0
 * @see <a href="https://en.wikipedia.org/wiki/Polygon_mesh">Polygon mesh</a>
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaMesh {
    private ArrayList<GaiaPrimitive> primitives = new ArrayList<>();

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

    // getTotalIndices
    public short[] getIndices() {
        short[] totalIndices = new short[getIndicesCount()];
        int index = 0;
        for (GaiaPrimitive primitive : primitives) {
            for (int indices : primitive.getIndices()) {
                totalIndices[index++] = (short) indices;
                //totalIndices.add(indices.shortValue());
            }
        }
        return totalIndices;
    }

    // getTotalVerticesCount
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

    // getTotalVertices
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

    // getTotalNormalsCount
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

    // getTotalNormals
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

    // getTotalTexCoordsCount
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

    // getTotalTexCoords
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

    // getBatchIdsCount
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

    // getBatchId
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
            GaiaBufferDataSet gaiaBufferDataSet = primitive.toGaiaBufferSet(transformMatrix);
            gaiaBufferDataSet.setMaterialId(primitive.getMaterialIndex());
            bufferSets.add(gaiaBufferDataSet);
        }
    }

    public void translate(Vector3d translation) {
        for (GaiaPrimitive primitive : primitives) {
            primitive.translate(translation);
        }
    }

    public void clear() {
        this.primitives.forEach(GaiaPrimitive::clear);
        this.primitives.clear();
    }
}
