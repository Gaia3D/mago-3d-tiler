package geometry.exchangable;

import geometry.structure.GaiaPrimitive;
import geometry.structure.GaiaSurface;
import geometry.structure.GaiaVertex;
import geometry.types.AttributeType;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;
import util.ArrayUtils;
import util.BinaryUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class GaiaBufferDataSet<T> {
    private LinkedHashMap<AttributeType, GaiaBuffer> buffers;
    private int id = -1;
    private String guid = "no_guid";
    private int materialId;

    public GaiaBufferDataSet() {
        this.buffers = new LinkedHashMap<>();
    }

    public void write(DataOutputStream stream) throws IOException {
        BinaryUtils.writeInt(stream, id);
        BinaryUtils.writeText(stream, guid);
        BinaryUtils.writeInt(stream, materialId);
        BinaryUtils.writeInt(stream,  buffers.size());
        for (Map.Entry<AttributeType, GaiaBuffer> entry : buffers.entrySet()) {
            AttributeType attributeType = entry.getKey();
            GaiaBuffer buffer = entry.getValue();
            BinaryUtils.writeText(stream, attributeType.getGaia());
            buffer.writeBuffer(stream);
        }
    }

    //read
    public void read(DataInputStream stream) throws IOException {
        this.setId(BinaryUtils.readInt(stream));
        this.setGuid(BinaryUtils.readText(stream));
        this.setMaterialId(BinaryUtils.readInt(stream));
        int size = BinaryUtils.readInt(stream);
        for (int i = 0; i < size; i++) {
            String gaiaAttribute = BinaryUtils.readText(stream);
            AttributeType attributeType = AttributeType.getGaiaAttribute(gaiaAttribute);
            GaiaBuffer buffer = new GaiaBuffer();
            buffer.readBuffer(stream);
            buffers.put(attributeType, buffer);
        }
    }

    public GaiaPrimitive toPrimitive() {
        List<Integer> indices = null;
        List<GaiaVertex> vertices = new ArrayList<>();
        GaiaPrimitive primitive = new GaiaPrimitive();
        //int elementsCount = buffers.get(AttributeType.POSITION).getElementsCount();

        for (Map.Entry<AttributeType, GaiaBuffer> entry : buffers.entrySet()) {
            AttributeType attributeType = entry.getKey();
            GaiaBuffer buffer = entry.getValue();

            if (attributeType.equals(AttributeType.INDICE)) {
                indices = ArrayUtils.convertIntArrayListToShortArray(buffer.getShorts());
            } else if (attributeType.equals(AttributeType.POSITION)) {
                List<Float> positions = ArrayUtils.convertArrayListToFloatArray(buffer.getFloats());
                if (vertices.size() > 0) {
                    int positionCount = 0;
                    for (int i = 0; i < vertices.size(); i++) {
                        GaiaVertex vertex = vertices.get(i);
                        Vector3d position = new Vector3d(positions.get(positionCount++), positions.get(positionCount++), positions.get(positionCount++));
                        vertex.setPosition(position);
                    }
                } else {
                    int positionSize = positions.size();
                    for (int i = 0; i < positionSize; i += 3) {
                        GaiaVertex vertex = new GaiaVertex();
                        Vector3d position = new Vector3d(positions.get(i), positions.get(i + 1), positions.get(i + 2));
                        vertex.setPosition(position);
                        vertices.add(vertex);
                    }
                }
            } else if (attributeType.equals(AttributeType.NORMAL)) {
                List<Float> normals = ArrayUtils.convertArrayListToFloatArray(buffer.getFloats());
                if (vertices.size() > 0) {
                    int normalCount = 0;
                    for (int i = 0; i < vertices.size(); i++) {
                        GaiaVertex vertex = vertices.get(i);
                        Vector3d normal = new Vector3d(normals.get(normalCount++), normals.get(normalCount++), normals.get(normalCount++));
                        vertex.setNormal(normal);
                    }
                } else {
                    int normalSize = normals.size();
                    for (int i = 0; i < normalSize; i += 3) {
                        GaiaVertex vertex = new GaiaVertex();
                        Vector3d normal = new Vector3d(normals.get(i), normals.get(i + 1), normals.get(i + 2));
                        vertex.setNormal(normal);
                        vertices.add(vertex);
                    }
                }
            } else if (attributeType.equals(AttributeType.TEXCOORD)) {
                List<Float> texcoords = ArrayUtils.convertArrayListToFloatArray(buffer.getFloats());
                if (vertices.size() > 0) {
                    int texcoordCount = 0;
                    for (int i = 0; i < vertices.size(); i++) {
                        GaiaVertex vertex = vertices.get(i);
                        Vector2d texcoord = new Vector2d(texcoords.get(texcoordCount++), texcoords.get(texcoordCount++));
                        vertex.setTextureCoordinates(texcoord);
                    }
                } else {
                    int texcoordSize = texcoords.size();
                    for (int i = 0; i < texcoordSize; i += 2) {
                        GaiaVertex vertex = new GaiaVertex();
                        Vector2d texcoord = new Vector2d(texcoords.get(i), texcoords.get(i + 1));
                        vertex.setTextureCoordinates(texcoord);
                        vertices.add(vertex);
                    }
                }
            }
        }

        primitive.setIndices(indices);
        primitive.setVertices(vertices);

        return primitive;
    }
}
