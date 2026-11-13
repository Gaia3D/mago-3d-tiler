package com.gaia3d.converter.gltf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.openize.drako.AttributeType;
import com.openize.drako.Draco;
import com.openize.drako.DracoMesh;
import com.openize.drako.PointAttribute;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

@Tag("default")
class DracoMeshCompressorTest {
    private static final byte[] IMAGE = {1, 2, 3, 4, 5, 6, 7, 8};

    @Test
    void encodesTrianglesAndKeepsOtherBufferViews() throws Exception {
        // a 10 x 10 grid of 10 cm cells, one feature id per row, followed by an image buffer view
        int side = 11;
        ByteBuffer body = ByteBuffer.allocate(4096 * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                body.putFloat(x * 0.1f).putFloat(y * 0.1f).putFloat(0.0f);
            }
        }
        int positionsLength = body.position();
        for (int y = 0; y < side; y++) {
            for (int x = 0; x < side; x++) {
                body.putFloat(y);
            }
        }
        int idsLength = body.position() - positionsLength;
        int indexCount = 0;
        for (int y = 0; y < side - 1; y++) {
            for (int x = 0; x < side - 1; x++) {
                short a = (short) (y * side + x), b = (short) (a + 1), c = (short) (a + side), d = (short) (c + 1);
                body.putShort(a).putShort(b).putShort(d).putShort(a).putShort(d).putShort(c);
                indexCount += 6;
            }
        }
        int indicesLength = body.position() - positionsLength - idsLength;
        int imageOffset = body.position();
        body.put(IMAGE);
        byte[] bin = Arrays.copyOf(body.array(), body.position());

        int points = side * side;
        String json = "{\"asset\":{\"version\":\"2.0\"},\"buffers\":[{\"byteLength\":" + bin.length + "}],"
                + "\"bufferViews\":[{\"buffer\":0,\"byteOffset\":0,\"byteLength\":" + positionsLength + "},"
                + "{\"buffer\":0,\"byteOffset\":" + positionsLength + ",\"byteLength\":" + idsLength + "},"
                + "{\"buffer\":0,\"byteOffset\":" + (positionsLength + idsLength) + ",\"byteLength\":" + indicesLength + "},"
                + "{\"buffer\":0,\"byteOffset\":" + imageOffset + ",\"byteLength\":" + IMAGE.length + "}],"
                + "\"accessors\":[{\"bufferView\":0,\"componentType\":5126,\"count\":" + points + ",\"type\":\"VEC3\",\"min\":[0,0,0],\"max\":[1,1,0]},"
                + "{\"bufferView\":1,\"componentType\":5126,\"count\":" + points + ",\"type\":\"SCALAR\"},"
                + "{\"bufferView\":2,\"componentType\":5123,\"count\":" + indexCount + ",\"type\":\"SCALAR\"}],"
                + "\"images\":[{\"bufferView\":3,\"mimeType\":\"image/png\"}],"
                + "\"meshes\":[{\"primitives\":[{\"attributes\":{\"POSITION\":0,\"_FEATURE_ID_0\":1},\"indices\":2}]}]}";

        byte[] compressed = new DracoMeshCompressor(16, 10, 14, 8).apply(toGlb(json, bin));

        ByteBuffer glb = ByteBuffer.wrap(compressed).order(ByteOrder.LITTLE_ENDIAN);
        int jsonLength = glb.getInt(12);
        JsonNode gltf = new ObjectMapper().readTree(new String(compressed, 20, jsonLength, StandardCharsets.UTF_8));
        byte[] newBin = Arrays.copyOfRange(compressed, 28 + jsonLength, 28 + jsonLength + glb.getInt(20 + jsonLength));
        assertEquals(compressed.length, glb.getInt(8));
        assertTrue(gltf.get("extensionsRequired").toString().contains(DracoMeshCompressor.EXTENSION_NAME));
        assertEquals(2, gltf.get("bufferViews").size());

        JsonNode image = gltf.get("bufferViews").get(gltf.get("images").get(0).get("bufferView").asInt());
        int offset = image.get("byteOffset").asInt();
        assertArrayEquals(IMAGE, Arrays.copyOfRange(newBin, offset, offset + IMAGE.length));

        JsonNode extension = gltf.get("meshes").get(0).get("primitives").get(0).get("extensions").get(DracoMeshCompressor.EXTENSION_NAME);
        for (JsonNode accessor : gltf.get("accessors")) {
            assertFalse(accessor.has("bufferView"));
        }
        assertEquals(5126, gltf.get("accessors").get(1).get("componentType").asInt());
        JsonNode view = gltf.get("bufferViews").get(extension.get("bufferView").asInt());
        int dracoOffset = view.get("byteOffset").asInt();
        DracoMesh mesh = (DracoMesh) Draco.decode(Arrays.copyOfRange(newBin, dracoOffset, dracoOffset + view.get("byteLength").asInt()));
        assertEquals(indexCount / 3, mesh.getNumFaces());
        assertEquals(gltf.get("accessors").get(0).get("count").asInt(), mesh.getNumPoints());

        PointAttribute positions = mesh.getNamedAttribute(AttributeType.POSITION);
        PointAttribute ids = mesh.getNamedAttribute(AttributeType.GENERIC);
        assertEquals(extension.get("attributes").get("_FEATURE_ID_0").asInt(), ids.getUniqueId());
        float[] position = new float[3];
        float[] id = new float[1];
        for (int point = 0; point < mesh.getNumPoints(); point++) {
            positions.getValue(positions.mappedIndex(point), position);
            ids.getValue(ids.mappedIndex(point), id);
            assertEquals(Math.round(position[1] / 0.1f), id[0], 0.0); // ids stay exact
            assertEquals(Math.round(position[0] / 0.1f) * 0.1f, position[0], 1e-4); // 16 bits over 1 m
        }
    }

    @Test
    void leavesGlbWithoutTrianglesUnchanged() {
        String json = "{\"asset\":{\"version\":\"2.0\"},\"meshes\":[]}";
        byte[] glb = toGlb(json, new byte[0]);
        assertSame(glb, new DracoMeshCompressor(16, 10, 14, 8).apply(glb));
    }

    @Test
    void dropsTrianglesThatRepeatAVertex() {
        assertArrayEquals(new int[]{0, 1, 2, 2, 3, 0}, DracoMeshCompressor.removeDegenerateFaces(new int[]{0, 1, 2, 1, 1, 3, 2, 3, 0}));
    }

    @Test
    void widensNormalizedIntegersToFloats() {
        ByteBuffer colors = ByteBuffer.wrap(new byte[]{(byte) 255, 0, (byte) 128, -127}).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(1.0f, DracoMeshCompressor.readComponent(colors, 0, 5121, true), 1e-6);
        assertEquals(128 / 255.0f, DracoMeshCompressor.readComponent(colors, 2, 5121, true), 1e-6);
        assertEquals(-1.0f, DracoMeshCompressor.readComponent(colors, 3, 5120, true), 1e-6);
        assertEquals(128.0f, DracoMeshCompressor.readComponent(colors, 2, 5121, false), 0.0);
    }

    private static byte[] toGlb(String json, byte[] bin) {
        byte[] jsonBytes = json.getBytes(StandardCharsets.UTF_8);
        int jsonPadded = (jsonBytes.length + 3) & ~3;
        int binPadded = (bin.length + 3) & ~3;
        int total = 20 + jsonPadded + (bin.length > 0 ? 8 + binPadded : 0);
        ByteBuffer out = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        out.putInt(0x46546C67).putInt(2).putInt(total).putInt(jsonPadded).putInt(0x4E4F534A).put(jsonBytes);
        while (out.position() < 20 + jsonPadded) {
            out.put((byte) ' ');
        }
        if (bin.length > 0) {
            out.putInt(binPadded).putInt(0x004E4942).put(bin);
        }
        return out.array();
    }
}
