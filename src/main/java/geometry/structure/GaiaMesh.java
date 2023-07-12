package geometry.structure;

import geometry.basic.GaiaBoundingBox;
import geometry.exchangable.GaiaBufferDataSet;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.collections.CollectionUtils;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;

import java.util.ArrayList;
import java.util.List;

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
                    count+=3;
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
                    count+=3;
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
                    count+=2;
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

    public int getColorsCount() {
        int count = 0;
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                Vector4d color = vertex.getColor();
                if (color != null) {
                    count+=4;
                }
            }
        }
        return count;
    }

    public ArrayList<Float> getColors() {
        ArrayList<Float> totalColors = new ArrayList<>();
        for (GaiaPrimitive primitive : primitives) {
            for (GaiaVertex vertex : primitive.getVertices()) {
                Vector4d color = vertex.getColor();
                if (color != null) {
                    totalColors.add((float) vertex.getColor().x());
                    totalColors.add((float) vertex.getColor().y());
                    totalColors.add((float) vertex.getColor().z());
                    totalColors.add((float) vertex.getColor().w());
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

    public void toGaiaBufferSets(List<GaiaBufferDataSet> bufferSets) {
        if (bufferSets == null) {
            bufferSets = new ArrayList<>();
        }
        for (GaiaPrimitive primitive : primitives) {
            GaiaBufferDataSet gaiaBufferDataSet = primitive.toGaiaBufferSet();
            gaiaBufferDataSet.setMaterialId(primitive.getMaterialIndex());
            bufferSets.add(gaiaBufferDataSet);
        }
    }
}
