package basic.structure;

import basic.exchangable.GaiaBufferDataSet;
import basic.geometry.GaiaBoundingBox;
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
    public ArrayList<Short> getIndices() {
        ArrayList<Short> totalIndices = new ArrayList<>();
        for (GaiaPrimitive primitive : primitives) {
            for (Integer indices : primitive.getIndices()) {
                totalIndices.add(indices.shortValue());
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
    public ArrayList<Float> getPositions() {
        ArrayList<Float> totalVertices = new ArrayList<>();
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                Vector3d position = vertex.getPosition();
                if (position != null) {
                    totalVertices.add((float) vertex.getPosition().x());
                    totalVertices.add((float) vertex.getPosition().y());
                    totalVertices.add((float) vertex.getPosition().z());
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
    public ArrayList<Float> getNormals() {
        ArrayList<Float> totalNormals = new ArrayList<>();
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                Vector3d normal = vertex.getNormal();
                if (normal != null) {
                    totalNormals.add((float) vertex.getNormal().x());
                    totalNormals.add((float) vertex.getNormal().y());
                    totalNormals.add((float) vertex.getNormal().z());
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
    public ArrayList<Float> getTexcoords() {
        ArrayList<Float> totalTexcoords = new ArrayList<>();
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                Vector2d texcoords = vertex.getTexcoords();
                if (texcoords != null) {
                    totalTexcoords.add((float) vertex.getTexcoords().x());
                    totalTexcoords.add((float) vertex.getTexcoords().y());
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
    public ArrayList<Float> getBatchIds() {
        ArrayList<Float> totalBatchIds = new ArrayList<>();
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                float batchId = vertex.getBatchId();
                if (batchId >= 0) {
                    totalBatchIds.add(vertex.getBatchId());
                }
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

    public List<Byte> getColors() {
        List<Byte> totalColors = new ArrayList<>();
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                byte[] color = vertex.getColor();
                if (color != null) {
                    totalColors.add(color[0]);
                    totalColors.add(color[1]);
                    totalColors.add(color[2]);
                    totalColors.add(color[3]);
                }
            }
        }
        return totalColors;
    }

    public int getIndicesCount() {
        int totalIndices = 0;
        for (GaiaPrimitive primitive : primitives) {
            totalIndices += primitive.getIndices().size();
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
}
