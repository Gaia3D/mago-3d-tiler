package basic.structure;

import basic.exchangable.GaiaBuffer;
import basic.exchangable.GaiaBufferDataSet;
import basic.geometry.GaiaBoundingBox;
import basic.geometry.GaiaRectangle;
import basic.types.AttributeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL20;
import util.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaPrimitive {
    private Integer accessorIndices = -1;
    private Integer materialIndex = -1;
    private List<GaiaVertex> vertices = new ArrayList<>();
    private List<GaiaSurface> surfaces = new ArrayList<>();

    private GaiaMaterial material = null;

    public GaiaBoundingBox getBoundingBox(Matrix4d transform) {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();

            Vector3d transformedPosition = new Vector3d(position);
            if (transform != null) {
                transform.transformPosition(position, transformedPosition);
            }
            boundingBox.addPoint(transformedPosition);
        }
        return boundingBox;
    }

    public void calculateNormal() {
        for (GaiaSurface surface : surfaces) {
            surface.calculateNormal(this.vertices);
        }
    }

    public ArrayList<Integer> getIndices() {
        ArrayList<Integer> resultIndices = new ArrayList<>();
        for (GaiaSurface surface : surfaces) {
            resultIndices.addAll(surface.getIndices());
        }
        return resultIndices;
    }

    public GaiaBufferDataSet toGaiaBufferSet() {
        ArrayList<Integer> indicesArray = getIndices();
        List<Short> indicesList = new ArrayList<>();
        for (Integer indices : indicesArray) {
            indicesList.add(indices.shortValue());
        }

        ArrayList<Float> positionList = new ArrayList<>();
        ArrayList<Float> batchIdList = new ArrayList<>();
        ArrayList<Float> colorList = new ArrayList<>();
        ArrayList<Float> normalList = new ArrayList<>();
        ArrayList<Float> textureCoordinateList = new ArrayList<>();

        GaiaRectangle texcoordBoundingRectangle = null;

        // calculate texcoordBoundingRectangle by indices.
        if (indicesList.size() > 0) {
            for (int index : indicesList) {
                GaiaVertex vertex = vertices.get(index);
                Vector2d textureCoordinate = vertex.getTexcoords();
                if (textureCoordinate != null) {
                    if (texcoordBoundingRectangle == null) {
                        texcoordBoundingRectangle = new GaiaRectangle();
                        texcoordBoundingRectangle.setInit(textureCoordinate);
                    } else {
                        texcoordBoundingRectangle.addPoint(textureCoordinate);
                    }
                }
            }
        }

        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            if (position != null) {
                positionList.add((float) position.x);
                positionList.add((float) position.y);
                positionList.add((float) position.z);
            }
            Vector3d normal = vertex.getNormal();
            if (normal != null) {
                normalList.add((float) normal.x);
                normalList.add((float) normal.y);
                normalList.add((float) normal.z);
            }

            // colors
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);
            colorList.add(1.0f);

            batchIdList.add(vertex.getBatchId());
            Vector2d textureCoordinate = vertex.getTexcoords();
            if (textureCoordinate != null) {
                textureCoordinateList.add((float) textureCoordinate.x);
                textureCoordinateList.add((float) textureCoordinate.y);
            }
        }

        GaiaBufferDataSet gaiaBufferDataSet = new GaiaBufferDataSet();
        if (indicesList.size() > 0) {
            GaiaBuffer indicesBuffer = new GaiaBuffer();
            indicesBuffer.setGlTarget(GL20.GL_ELEMENT_ARRAY_BUFFER);
            indicesBuffer.setGlType(GL20.GL_UNSIGNED_SHORT);
            indicesBuffer.setElementsCount(indicesList.size());
            indicesBuffer.setGlDimension((byte) 1);
            indicesBuffer.setShorts(ArrayUtils.convertShortArrayToList(indicesList));
            gaiaBufferDataSet.getBuffers().put(AttributeType.INDICE, indicesBuffer);
        }

        if (normalList.size() > 0) {
            GaiaBuffer normalBuffer = new GaiaBuffer();
            normalBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            normalBuffer.setGlType(GL20.GL_FLOAT);
            normalBuffer.setElementsCount(vertices.size());
            normalBuffer.setGlDimension((byte) 3);
            normalBuffer.setFloats(ArrayUtils.convertFloatArrayToList(normalList));
            gaiaBufferDataSet.getBuffers().put(AttributeType.NORMAL, normalBuffer);
        }

        if (colorList.size() > 0) {
            GaiaBuffer colorBuffer = new GaiaBuffer();
            colorBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            colorBuffer.setGlType(GL20.GL_FLOAT);
            colorBuffer.setElementsCount(vertices.size());
            colorBuffer.setGlDimension((byte) 4);
            colorBuffer.setFloats(ArrayUtils.convertFloatArrayToList(colorList));
            gaiaBufferDataSet.getBuffers().put(AttributeType.COLOR, colorBuffer);
        }

        if (batchIdList.size() > 0) {
            GaiaBuffer batchIdBuffer = new GaiaBuffer();
            batchIdBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            batchIdBuffer.setGlType(GL20.GL_FLOAT);
            batchIdBuffer.setElementsCount(vertices.size());
            batchIdBuffer.setGlDimension((byte) 1);
            batchIdBuffer.setFloats(ArrayUtils.convertFloatArrayToList(batchIdList));
            gaiaBufferDataSet.getBuffers().put(AttributeType.BATCHID, batchIdBuffer);
        }

        if (positionList.size() > 0) {
            GaiaBuffer positionBuffer = new GaiaBuffer();
            positionBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            positionBuffer.setGlType(GL20.GL_FLOAT);
            positionBuffer.setElementsCount(vertices.size());
            positionBuffer.setGlDimension((byte) 3);
            positionBuffer.setFloats(ArrayUtils.convertFloatArrayToList(positionList));
            gaiaBufferDataSet.getBuffers().put(AttributeType.POSITION, positionBuffer);
        }

        if (textureCoordinateList.size() > 0) {
            GaiaBuffer textureCoordinateBuffer = new GaiaBuffer();
            textureCoordinateBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            textureCoordinateBuffer.setGlType(GL20.GL_FLOAT);
            textureCoordinateBuffer.setElementsCount(vertices.size());
            textureCoordinateBuffer.setGlDimension((byte) 2);
            textureCoordinateBuffer.setFloats(ArrayUtils.convertFloatArrayToList(textureCoordinateList));
            gaiaBufferDataSet.getBuffers().put(AttributeType.TEXCOORD, textureCoordinateBuffer);
        }

        gaiaBufferDataSet.setTexcoordBoundingRectangle(texcoordBoundingRectangle);

        //assign material. Son 2023.07.17
        gaiaBufferDataSet.setMaterial(this.material);

        return gaiaBufferDataSet;
    }

    public void translate(Vector3d translation) {
        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            if (position != null) {
                position.add(translation);
            }
        }
    }
}
