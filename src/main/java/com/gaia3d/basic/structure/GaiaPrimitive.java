package com.gaia3d.basic.structure;

import com.gaia3d.basic.exchangable.GaiaBuffer;
import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.types.AttributeType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.lwjgl.opengl.GL20;

import java.io.Serializable;
import java.util.*;

/**
 * A class that represents a primitive of a Gaia object.
 * It contains the vertices and surfaces.
 * The vertices are used for rendering.
 * The surfaces are used for calculating normals.
 * @author znkim
 * @since 1.0.0
 * @see <a href="https://en.wikipedia.org/wiki/Polygon_mesh">Polygon mesh</a>
 */
@Slf4j
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaPrimitive implements Serializable {
    private Integer accessorIndices = -1;
    private Integer materialIndex = -1;
    private List<GaiaVertex> vertices = new ArrayList<>();
    private List<GaiaSurface> surfaces = new ArrayList<>();

    //private GaiaMaterial material = null;

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

    public GaiaRectangle getTexcoordBoundingRectangle(GaiaRectangle texcoordBoundingRectangle) {
        if(texcoordBoundingRectangle == null) {
            texcoordBoundingRectangle = new GaiaRectangle();
        }
        boolean is1rst = true;
        for (GaiaVertex vertex : vertices) {
            Vector2d textureCoordinate = vertex.getTexcoords();
            if (textureCoordinate != null) {
                if (is1rst) {
                    texcoordBoundingRectangle.setInit(textureCoordinate);
                    is1rst = false;
                } else {
                    texcoordBoundingRectangle.addPoint(textureCoordinate);
                }
            }
        }
        return texcoordBoundingRectangle;
    }

    public void calculateNormal() {
        for (GaiaSurface surface : surfaces) {
            surface.calculateNormal(this.vertices);
        }
    }

    public int[] getIndices() {
        int[] resultIndices = new int[0];
        for (GaiaSurface surface : surfaces) {
            resultIndices = ArrayUtils.addAll(resultIndices, surface.getIndices());
        }
        return resultIndices;
    }

    public GaiaBufferDataSet toGaiaBufferSet(Matrix4d transformMatrixOrigin) {
        Matrix4d transformMatrix = new Matrix4d(transformMatrixOrigin);
        Matrix3d rotationMatrix = new Matrix3d();
        transformMatrix.get3x3(rotationMatrix);
        // normalize the rotation matrix
        rotationMatrix.normal();
        Matrix4d rotationMatrix4 = new Matrix4d(rotationMatrix);

        int[] indices = getIndices();

        GaiaRectangle texcoordBoundingRectangle = null;
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();

        // calculate texcoord BoundingRectangle by indices.
        for (int index : indices) {
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

        int positionCount = 0;
        int normalCount = 0;
        int colorCount = 0;
        int batchIdCount = 0;
        int textureCoordinateCount = 0;
        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            if (position != null) {
                positionCount+=3;
            }
            Vector3d normal = vertex.getNormal();
            if (normal != null) {
                normalCount+=3;
            }
            byte[] color = vertex.getColor();
            if (color != null) {
                colorCount+=4;
            }
            float batchId = vertex.getBatchId();
            if (batchId != 0) {
                batchIdCount+=1;
            }
            Vector2d textureCoordinate = vertex.getTexcoords();
            if (textureCoordinate != null) {
                textureCoordinateCount+=2;
            }
        }

        float[] positionList = new float[positionCount];
        float[] normalList = new float[normalCount];
        float[] batchIdList = new float[batchIdCount];
        float[] textureCoordinateList = new float[textureCoordinateCount];
        byte[] colorList = new byte[colorCount];

        int positionIndex = 0;
        int normalIndex = 0;
        int batchIdIndex = 0;
        int textureCoordinateIndex = 0;
        int colorIndex = 0;

        Random random = new Random();
        byte[] bytes = new byte[4];
        random.nextBytes(bytes);
        for (GaiaVertex vertex : vertices) {
            Vector3d position = vertex.getPosition();
            transformMatrix.transformPosition(position);
            if (position != null) {
                positionList[positionIndex++] = (float) position.x;
                positionList[positionIndex++] = (float) position.y;
                positionList[positionIndex++] = (float) position.z;
            }
            boundingBox.addPoint(position);
            Vector3d normal = vertex.getNormal();
            if (normal != null) {
                rotationMatrix4.transformPosition(normal);
                normalList[normalIndex++] = (float) normal.x;
                normalList[normalIndex++] = (float) normal.y;
                normalList[normalIndex++] = (float) normal.z;
            }
            byte[] color = vertex.getColor();
            if (color != null) {
                colorList[colorIndex++] = color[0];
                colorList[colorIndex++] = color[1];
                colorList[colorIndex++] = color[2];
                colorList[colorIndex++] = color[3];
            }
            if (batchIdList.length > 0) {
                batchIdList[batchIdIndex++] = vertex.getBatchId();
            }
            Vector2d textureCoordinate = vertex.getTexcoords();
            if (textureCoordinate != null) {
                textureCoordinateList[textureCoordinateIndex++] = (float) textureCoordinate.x;
                textureCoordinateList[textureCoordinateIndex++] = (float) textureCoordinate.y;
            }
        }

        GaiaBufferDataSet gaiaBufferDataSet = new GaiaBufferDataSet();
        if (indices.length > 0) {
            GaiaBuffer indicesBuffer = new GaiaBuffer();
            indicesBuffer.setGlTarget(GL20.GL_ELEMENT_ARRAY_BUFFER);
            indicesBuffer.setGlType(GL20.GL_UNSIGNED_INT);
            indicesBuffer.setElementsCount(indices.length);
            indicesBuffer.setGlDimension((byte) 1);
            indicesBuffer.setInts(indices);
            gaiaBufferDataSet.getBuffers().put(AttributeType.INDICE, indicesBuffer);
        }
        /*if (indicesShort.length > 0) {
            // TODO : if indices size is less than 65536, use short type.
            GaiaBuffer indicesBuffer = new GaiaBuffer();
            indicesBuffer.setGlTarget(GL20.GL_ELEMENT_ARRAY_BUFFER);
            indicesBuffer.setGlType(GL20.GL_UNSIGNED_SHORT);
            indicesBuffer.setElementsCount(indicesShort.length);
            indicesBuffer.setGlDimension((byte) 1);
            indicesBuffer.setShorts(indicesShort);
            gaiaBufferDataSet.getBuffers().put(AttributeType.INDICE, indicesBuffer);
        }*/
        if (normalList.length > 0) {
            GaiaBuffer normalBuffer = new GaiaBuffer();
            normalBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            normalBuffer.setGlType(GL20.GL_FLOAT);
            normalBuffer.setElementsCount(vertices.size());
            normalBuffer.setGlDimension((byte) 3);
            normalBuffer.setFloats(normalList);
            gaiaBufferDataSet.getBuffers().put(AttributeType.NORMAL, normalBuffer);
        }
        if (colorList.length > 0) {
            GaiaBuffer colorBuffer = new GaiaBuffer();
            colorBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            colorBuffer.setGlType(GL20.GL_UNSIGNED_BYTE);
            colorBuffer.setElementsCount(vertices.size());
            colorBuffer.setGlDimension((byte) 1);
            colorBuffer.setBytes(colorList);
            gaiaBufferDataSet.getBuffers().put(AttributeType.COLOR, colorBuffer);
        }
        if (batchIdList.length > 0) {
            GaiaBuffer batchIdBuffer = new GaiaBuffer();
            batchIdBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            batchIdBuffer.setGlType(GL20.GL_FLOAT);
            batchIdBuffer.setElementsCount(vertices.size());
            batchIdBuffer.setGlDimension((byte) 1);
            batchIdBuffer.setFloats(batchIdList);
            gaiaBufferDataSet.getBuffers().put(AttributeType.BATCHID, batchIdBuffer);
        }
        if (positionList.length > 0) {
            GaiaBuffer positionBuffer = new GaiaBuffer();
            positionBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            positionBuffer.setGlType(GL20.GL_FLOAT);
            positionBuffer.setElementsCount(vertices.size());
            positionBuffer.setGlDimension((byte) 3);
            positionBuffer.setFloats(positionList);
            gaiaBufferDataSet.getBuffers().put(AttributeType.POSITION, positionBuffer);
        }
        if (textureCoordinateList.length > 0) {
            GaiaBuffer textureCoordinateBuffer = new GaiaBuffer();
            textureCoordinateBuffer.setGlTarget(GL20.GL_ARRAY_BUFFER);
            textureCoordinateBuffer.setGlType(GL20.GL_FLOAT);
            textureCoordinateBuffer.setElementsCount(vertices.size());
            textureCoordinateBuffer.setGlDimension((byte) 2);
            textureCoordinateBuffer.setFloats(textureCoordinateList);
            gaiaBufferDataSet.getBuffers().put(AttributeType.TEXCOORD, textureCoordinateBuffer);
        }
        gaiaBufferDataSet.setTexcoordBoundingRectangle(texcoordBoundingRectangle);
        gaiaBufferDataSet.setBoundingBox(boundingBox);

        //assign material. Son 2023.07.17
        //gaiaBufferDataSet.setMaterial(this.material);
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

    public void clear() {
        if (this.vertices != null) {
            this.vertices.forEach(GaiaVertex::clear);
            this.vertices.clear();
        }
        if (this.surfaces != null) {
            this.surfaces.forEach(GaiaSurface::clear);
            this.surfaces.clear();
        }
    }

    public GaiaPrimitive clone() {
        GaiaPrimitive gaiaPrimitive = new GaiaPrimitive();
        gaiaPrimitive.setAccessorIndices(this.accessorIndices);
        gaiaPrimitive.setMaterialIndex(this.materialIndex);
        for (GaiaVertex vertex : this.vertices) {
            gaiaPrimitive.getVertices().add(vertex.clone());
        }
        for (GaiaSurface surface : this.surfaces) {
            gaiaPrimitive.getSurfaces().add(surface.clone());
        }
        return gaiaPrimitive;
    }
}
