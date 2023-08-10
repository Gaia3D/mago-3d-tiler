package basic.exchangable;

import basic.geometry.GaiaBoundingBox;
import basic.geometry.GaiaRectangle;
import basic.structure.*;
import basic.types.AttributeType;
import util.io.LittleEndianDataInputStream;
import util.io.LittleEndianDataOutputStream;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import util.ArrayUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * A GaiaBufferDataSet can correspond to a Node in a 3d structure.
 * It uses a straightforward structure and has a buffer for each attribute.
 * @auther znkim
 * @since 1.0.0
 * @see GaiaSet, GaiaMaterial, GaiaBoundingBox, GaiaRectangle, GaiaPrimitive, GaiaNode, GaiaBuffer
 */
@Getter
@Setter
public class GaiaBufferDataSet {
    private Map<AttributeType, GaiaBuffer> buffers;
    private int id = -1;
    private String guid = "no_guid";
    private int materialId;
    public GaiaMaterial material = null;

    GaiaBoundingBox boundingBox = null;
    GaiaRectangle texcoordBoundingRectangle = null;

    Matrix4d transformMatrix = null;
    Matrix4d preMultipliedTransformMatrix = null;

    public GaiaBufferDataSet() {
        this.buffers = new LinkedHashMap<>();
    }

    public static void getMaterialslIstOfBufferDataSet(List<GaiaBufferDataSet> bufferDataSets, List<GaiaMaterial> materials) {
        // first, make a map to avoid duplicate materials
        Map<GaiaMaterial, GaiaMaterial> materialMap = new LinkedHashMap<>();
        for (GaiaBufferDataSet bufferDataSet : bufferDataSets) {
            materialMap.put(bufferDataSet.getMaterial(), bufferDataSet.getMaterial());
        }
        // second, make a list from the map
        materials.addAll(materialMap.values());

    }

    public void write(LittleEndianDataOutputStream stream) throws IOException {
        stream.writeInt(id);
        stream.writeText(guid);
        stream.writeInt(materialId);
        stream.writeInt(buffers.size());
        for (Map.Entry<AttributeType, GaiaBuffer> entry : buffers.entrySet()) {
            AttributeType attributeType = entry.getKey();
            GaiaBuffer buffer = entry.getValue();
            stream.writeText(attributeType.getGaia());
            buffer.writeBuffer(stream);
        }
    }

    //read
    public void read(LittleEndianDataInputStream stream) throws IOException {
        this.setId(stream.readInt());
        this.setGuid(stream.readText());
        this.setMaterialId(stream.readInt());
        int size = stream.readInt();
        for (int i = 0; i < size; i++) {
            String gaiaAttribute = stream.readText();
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

        for (Map.Entry<AttributeType, GaiaBuffer> entry : buffers.entrySet()) {
            AttributeType attributeType = entry.getKey();
            GaiaBuffer buffer = entry.getValue();

            if (attributeType.equals(AttributeType.INDICE)) {
                indices = ArrayUtils.convertIntListToShortArray(buffer.getShorts());
            } else if (attributeType.equals(AttributeType.POSITION)) {
                List<Float> positions = ArrayUtils.convertListToFloatArray(buffer.getFloats());
                if (!vertices.isEmpty()) {
                    int positionCount = 0;
                    for (GaiaVertex vertex : vertices) {
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
                List<Float> normals = ArrayUtils.convertListToFloatArray(buffer.getFloats());
                if (!vertices.isEmpty()) {
                    int normalCount = 0;
                    for (GaiaVertex vertex : vertices) {
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
                List<Float> texcoords = ArrayUtils.convertListToFloatArray(buffer.getFloats());
                if (!vertices.isEmpty()) {
                    int texcoordCount = 0;
                    for (GaiaVertex vertex : vertices) {
                        Vector2d texcoord = new Vector2d(texcoords.get(texcoordCount++), texcoords.get(texcoordCount++));
                        vertex.setTexcoords(texcoord);
                    }
                } else {
                    int texcoordSize = texcoords.size();
                    for (int i = 0; i < texcoordSize; i += 2) {
                        GaiaVertex vertex = new GaiaVertex();
                        Vector2d texcoord = new Vector2d(texcoords.get(i), texcoords.get(i + 1));
                        vertex.setTexcoords(texcoord);
                        vertices.add(vertex);
                    }
                }
            }
        }

        // set indices as face of a surface of the primitive 2023.07.19
        GaiaSurface surface = new GaiaSurface();
        GaiaFace face = new GaiaFace();
        face.setIndices((ArrayList<Integer>) indices);
        surface.setFaces(new ArrayList<GaiaFace>() {{
            add(face);
        }});
        primitive.setSurfaces(new ArrayList<GaiaSurface>() {{
            add(surface);
        }});
        ////primitive.setIndices(indices); // old. indices are now in faces of surface of the primitive
        primitive.setVertices(vertices);
        return primitive;
    }
}
