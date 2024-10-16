package com.gaia3d.basic.exchangable;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.model.GaiaFace;
import com.gaia3d.basic.model.GaiaPrimitive;
import com.gaia3d.basic.model.GaiaSurface;
import com.gaia3d.basic.model.GaiaVertex;
import com.gaia3d.basic.types.AttributeType;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GaiaBufferDataSet implements Serializable {
    private Map<AttributeType, GaiaBuffer> buffers;
    private int materialId;
    private int id = -1;
    private String guid = "no_guid";

    private GaiaBoundingBox boundingBox = null;
    private GaiaRectangle texcoordBoundingRectangle = null;
    private Matrix4d transformMatrix = null;
    private Matrix4d preMultipliedTransformMatrix = null;

    public GaiaBufferDataSet() {
        this.buffers = new HashMap<>();
    }

    public GaiaPrimitive toPrimitive() {
        int[] indices = new int[0];
        //short[] indices = new short[0];
        List<GaiaVertex> vertices = new ArrayList<>();
        GaiaPrimitive primitive = new GaiaPrimitive();

        for (Map.Entry<AttributeType, GaiaBuffer> entry : buffers.entrySet()) {
            AttributeType attributeType = entry.getKey();
            GaiaBuffer buffer = entry.getValue();

            if (attributeType.equals(AttributeType.INDICE)) {
                indices = buffer.getInts();
            } else if (attributeType.equals(AttributeType.POSITION)) {
                float[] positions = buffer.getFloats();
                if (!vertices.isEmpty()) {
                    int positionCount = 0;
                    for (GaiaVertex vertex : vertices) {
                        Vector3d position = new Vector3d(positions[positionCount++], positions[positionCount++], positions[positionCount]);
                        vertex.setPosition(position);
                    }
                } else {
                    for (int i = 0; i < positions.length; i += 3) {
                        GaiaVertex vertex = new GaiaVertex();
                        Vector3d position = new Vector3d(positions[i], positions[i + 1], positions[i + 2]);
                        vertex.setPosition(position);
                        vertices.add(vertex);
                    }
                }
            } else if (attributeType.equals(AttributeType.NORMAL)) {
                float[] normals = buffer.getFloats();
                if (!vertices.isEmpty()) {
                    int normalCount = 0;
                    for (GaiaVertex vertex : vertices) {
                        Vector3d normal = new Vector3d(normals[normalCount++], normals[normalCount++], normals[normalCount++]);
                        vertex.setNormal(normal);
                    }
                } else {
                    for (int i = 0; i < normals.length; i += 3) {
                        GaiaVertex vertex = new GaiaVertex();
                        Vector3d normal = new Vector3d(normals[i], normals[i + 1], normals[i + 2]);
                        vertex.setNormal(normal);
                        vertices.add(vertex);
                    }
                }
            } else if (attributeType.equals(AttributeType.TEXCOORD)) {
                float[] texcoords = buffer.getFloats();
                if (!vertices.isEmpty()) {
                    int texcoordCount = 0;
                    for (GaiaVertex vertex : vertices) {
                        Vector2d texcoord = new Vector2d(texcoords[texcoordCount++], texcoords[texcoordCount++]);
                        vertex.setTexcoords(texcoord);
                    }
                } else {
                    for (int i = 0; i < texcoords.length; i += 2) {
                        GaiaVertex vertex = new GaiaVertex();
                        Vector2d texcoord = new Vector2d(texcoords[i], texcoords[i + 1]);
                        vertex.setTexcoords(texcoord);
                        vertices.add(vertex);
                    }
                }
            }
        }

        // set indices as face of a surface of the primitive 2023.07.19
        GaiaSurface surface = new GaiaSurface();

//        // new 20240903 Son.******************************************
//        int indicesLength = indices.length;
//        int trianglesCount = indicesLength / 3;
//        for(int i=0; i<trianglesCount; i++)
//        {
//            int index = i * 3;
//            int[] triangleIndices = new int[3];
//            triangleIndices[0] = indices[index];
//            triangleIndices[1] = indices[index + 1];
//            triangleIndices[2] = indices[index + 2];
//            GaiaFace face = new GaiaFace();
//            face.setIndices(triangleIndices);
//            surface.getFaces().add(face);
//        }
//        // End new 20240903 Son.---------------------------------------------

        // Old.**********************************************************************
        GaiaFace face = new GaiaFace();

        int[] indicesInt = new int[indices.length];
        System.arraycopy(indices, 0, indicesInt, 0, indices.length);
        face.setIndices(indicesInt);
        surface.setFaces(new ArrayList<>() {{
            add(face);
        }});
        // End old.------------------------------------------------------------------

        primitive.setSurfaces(new ArrayList<>() {{
            add(surface);
        }});
        ////primitive.setIndices(indices); // old. indices are now in faces of surface of the primitive
        primitive.setVertices(vertices);
        return primitive;
    }

    public void clear() {
        buffers.forEach((key, value) -> value.clear());
        buffers.clear();
        boundingBox = null;
        texcoordBoundingRectangle = null;
        transformMatrix = null;
        preMultipliedTransformMatrix = null;
    }

    public GaiaBufferDataSet clone() {
        GaiaBufferDataSet clone = new GaiaBufferDataSet();
        clone.setId(this.id);
        clone.setGuid(this.guid);
        clone.setMaterialId(this.materialId);
        if (this.boundingBox != null) {
            clone.setBoundingBox(this.boundingBox.clone());
        }
        if (this.texcoordBoundingRectangle != null) {
            clone.setTexcoordBoundingRectangle(this.texcoordBoundingRectangle.clone());
        }
        if (this.transformMatrix != null) {
            clone.setTransformMatrix(new Matrix4d(this.transformMatrix));
        }
        if (this.preMultipliedTransformMatrix != null) {
            clone.setPreMultipliedTransformMatrix(new Matrix4d(this.preMultipliedTransformMatrix));
        }
        for (Map.Entry<AttributeType, GaiaBuffer> entry : this.buffers.entrySet()) {
            GaiaBuffer buffer = entry.getValue();
            GaiaBuffer clonedBuffer = buffer.clone();
            clone.buffers.put(entry.getKey(), clonedBuffer);
        }
        return clone;
    }
}
