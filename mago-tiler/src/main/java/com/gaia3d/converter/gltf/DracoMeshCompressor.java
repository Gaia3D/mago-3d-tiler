package com.gaia3d.converter.gltf;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.openize.drako.Draco;
import com.openize.drako.DataBuffer;
import com.openize.drako.DataType;
import com.openize.drako.DracoCompressionLevel;
import com.openize.drako.DracoEncodeOptions;
import com.openize.drako.DracoMesh;
import com.openize.drako.DracoPointCloud;
import com.openize.drako.PointAttribute;
import lombok.extern.slf4j.Slf4j;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.UnaryOperator;

/**
 * Rewrites the triangle primitives of a GLB with KHR_draco_mesh_compression. Each primitive's indices and attributes
 * are encoded into one Draco buffer view, the plain buffer views they used are dropped and the binary chunk is
 * repacked. Attributes are stored as floats: positions, normals, texture coordinates and colors are quantized, other
 * attributes such as _BATCHID and _FEATURE_ID_n are kept exact. A primitive that cannot be encoded is left as it was.
 */
@Slf4j
public class DracoMeshCompressor implements UnaryOperator<byte[]> {
    public static final String EXTENSION_NAME = "KHR_draco_mesh_compression";
    private static final int GLB_MAGIC = 0x46546C67;
    private static final int CHUNK_JSON = 0x4E4F534A;
    private static final int CHUNK_BIN = 0x004E4942;
    private static final int GL_BYTE = 5120, GL_UNSIGNED_BYTE = 5121, GL_SHORT = 5122, GL_UNSIGNED_SHORT = 5123, GL_UNSIGNED_INT = 5125, GL_FLOAT = 5126;
    private static final int GL_TRIANGLES = 4;
    // buffer view references of EXT_structural_metadata property tables
    private static final Set<String> BUFFER_VIEW_KEYS = Set.of("bufferView", "values", "arrayOffsets", "stringOffsets");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final int positionBits;
    private final int normalBits;
    private final int texcoordBits;
    private final int colorBits;

    public DracoMeshCompressor(int positionBits, int normalBits, int texcoordBits, int colorBits) {
        this.positionBits = positionBits;
        this.normalBits = normalBits;
        this.texcoordBits = texcoordBits;
        this.colorBits = colorBits;
    }

    /**
     * @return the compressed GLB, or the input when it has nothing to compress or cannot be read
     */
    @Override
    public byte[] apply(byte[] glb) {
        try {
            return compress(glb);
        } catch (Exception e) {
            log.error("[ERROR] Draco mesh compression failed, writing the mesh uncompressed.", e);
            return glb;
        }
    }

    private byte[] compress(byte[] glb) throws Exception {
        ByteBuffer input = ByteBuffer.wrap(glb).order(ByteOrder.LITTLE_ENDIAN);
        if (glb.length < 20 || input.getInt(0) != GLB_MAGIC || input.getInt(16) != CHUNK_JSON) {
            return glb;
        }
        int jsonLength = input.getInt(12);
        ObjectNode gltf = (ObjectNode) objectMapper.readTree(new String(glb, 20, jsonLength, StandardCharsets.UTF_8));
        byte[] bin = new byte[0];
        int binHeader = 20 + jsonLength;
        if (binHeader + 8 <= glb.length && input.getInt(binHeader + 4) == CHUNK_BIN) {
            bin = Arrays.copyOfRange(glb, binHeader + 8, binHeader + 8 + input.getInt(binHeader));
        }

        ArrayNode accessors = arrayOf(gltf, "accessors");
        ArrayNode bufferViews = arrayOf(gltf, "bufferViews");
        Map<Integer, Integer> accessorUses = countAccessorUses(gltf);
        List<byte[]> addedViews = new ArrayList<>();
        Set<Integer> freedViews = new HashSet<>();
        for (JsonNode mesh : arrayOf(gltf, "meshes")) {
            for (JsonNode primitiveNode : arrayOf((ObjectNode) mesh, "primitives")) {
                ObjectNode primitive = (ObjectNode) primitiveNode;
                byte[] encoded = encodePrimitive(primitive, accessors, bufferViews, bin, accessorUses, freedViews);
                if (encoded != null) {
                    ObjectNode extension = (ObjectNode) primitive.get("extensions").get(EXTENSION_NAME);
                    extension.put("bufferView", bufferViews.size() + addedViews.size());
                    addedViews.add(encoded);
                }
            }
        }
        if (addedViews.isEmpty()) {
            return glb;
        }

        // keep views still referenced elsewhere (images, metadata, other accessors)
        Set<Integer> referenced = new HashSet<>();
        collectBufferViews(gltf, referenced, false);
        freedViews.removeAll(referenced);

        List<byte[]> viewData = new ArrayList<>();
        List<ObjectNode> viewNodes = new ArrayList<>();
        int[] remap = new int[bufferViews.size() + addedViews.size()];
        Arrays.fill(remap, -1);
        for (int i = 0; i < bufferViews.size(); i++) {
            if (freedViews.contains(i)) {
                continue;
            }
            ObjectNode view = (ObjectNode) bufferViews.get(i);
            int offset = view.path("byteOffset").asInt(0);
            remap[i] = viewNodes.size();
            viewNodes.add(view);
            viewData.add(Arrays.copyOfRange(bin, offset, offset + view.get("byteLength").asInt()));
        }
        for (int i = 0; i < addedViews.size(); i++) {
            ObjectNode view = objectMapper.createObjectNode();
            view.put("buffer", 0);
            remap[bufferViews.size() + i] = viewNodes.size();
            viewNodes.add(view);
            viewData.add(addedViews.get(i));
        }
        collectBufferViews(gltf, null, true, remap);

        ByteArrayOutputStream body = new ByteArrayOutputStream();
        ArrayNode newViews = objectMapper.createArrayNode();
        for (int i = 0; i < viewNodes.size(); i++) {
            ObjectNode view = viewNodes.get(i);
            byte[] data = viewData.get(i);
            view.put("byteOffset", body.size());
            view.put("byteLength", data.length);
            body.write(data);
            while (body.size() % 4 != 0) {
                body.write(0);
            }
            newViews.add(view);
        }
        gltf.set("bufferViews", newViews);
        ((ObjectNode) arrayOf(gltf, "buffers").get(0)).put("byteLength", body.size());
        addExtension(gltf, "extensionsUsed");
        addExtension(gltf, "extensionsRequired");
        return writeGlb(gltf, body.toByteArray());
    }

    private byte[] encodePrimitive(ObjectNode primitive, ArrayNode accessors, ArrayNode bufferViews, byte[] bin, Map<Integer, Integer> accessorUses, Set<Integer> freedViews) throws Exception {
        if (primitive.path("mode").asInt(GL_TRIANGLES) != GL_TRIANGLES || !primitive.has("indices") || primitive.path("extensions").has(EXTENSION_NAME)
                || primitive.has("targets")) {
            return null;
        }
        ObjectNode attributes = (ObjectNode) primitive.get("attributes");
        if (attributes == null || !attributes.has("POSITION")) {
            return null;
        }
        int indicesId = primitive.get("indices").asInt();
        List<Integer> accessorIds = new ArrayList<>();
        accessorIds.add(indicesId);
        attributes.fields().forEachRemaining(entry -> accessorIds.add(entry.getValue().asInt()));
        for (int accessorId : accessorIds) {
            ObjectNode accessor = (ObjectNode) accessors.get(accessorId);
            if (accessorUses.getOrDefault(accessorId, 0) != 1 || !accessor.has("bufferView") || accessor.has("sparse")) {
                return null;
            }
        }

        int numPoints = accessors.get(attributes.get("POSITION").asInt()).get("count").asInt();
        int[] indices = removeDegenerateFaces(readIndices((ObjectNode) accessors.get(indicesId), bufferViews, bin));
        if (indices.length == 0) {
            return null;
        }
        DracoMesh mesh = new DracoMesh();
        Map<String, PointAttribute> dracoAttributes = new LinkedHashMap<>();
        Iterator<Map.Entry<String, JsonNode>> fields = attributes.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            ObjectNode accessor = (ObjectNode) accessors.get(entry.getValue().asInt());
            if (accessor.get("count").asInt() != numPoints) {
                return null;
            }
            PointAttribute attribute = createAttribute(entry.getKey(), accessor, bufferViews, bin);
            if (attribute == null) {
                return null;
            }
            dracoAttributes.put(entry.getKey(), attribute);
        }
        dracoAttributes.values().forEach(mesh::addAttribute);
        mesh.getIndices().addRange(indices);
        mesh.setNumPoints(numPoints);

        byte[] encoded;
        try {
            encoded = Draco.encode(mesh, createOptions(DracoCompressionLevel.STANDARD));
        } catch (RuntimeException e) {
            // the edgebreaker traversal fails on some non-manifold meshes; the sequential encoder still quantizes
            log.debug("[DEBUG] Draco edgebreaker failed ({}), using the sequential encoder.", e.toString());
            encoded = Draco.encode(mesh, createOptions(DracoCompressionLevel.NO_COMPRESSION));
        }

        // the encoder may split or merge points, so the accessor counts follow the decoded mesh
        DracoPointCloud decoded = Draco.decode(encoded);
        if (!(decoded instanceof DracoMesh decodedMesh) || decodedMesh.getNumFaces() == 0 || decodedMesh.getNumAttributes() != dracoAttributes.size()) {
            log.warn("[WARN] Draco round trip lost the primitive, keeping it uncompressed.");
            return null;
        }

        ObjectNode extensionAttributes = objectMapper.createObjectNode();
        for (Map.Entry<String, PointAttribute> entry : dracoAttributes.entrySet()) {
            extensionAttributes.put(entry.getKey(), entry.getValue().getUniqueId());
        }
        for (int accessorId : accessorIds) {
            ObjectNode accessor = (ObjectNode) accessors.get(accessorId);
            freedViews.add(accessor.get("bufferView").asInt());
            accessor.remove("bufferView");
            accessor.remove("byteOffset");
            accessor.put("count", accessorId == indicesId ? decodedMesh.getNumFaces() * 3 : decodedMesh.getNumPoints());
        }
        ObjectNode indicesAccessor = (ObjectNode) accessors.get(indicesId);
        if (decodedMesh.getNumPoints() > 65535 && indicesAccessor.get("componentType").asInt() != GL_UNSIGNED_INT) {
            indicesAccessor.put("componentType", GL_UNSIGNED_INT);
        }
        attributes.forEach(id -> toFloatAccessor((ObjectNode) accessors.get(id.asInt())));

        ObjectNode extensions = primitive.has("extensions") ? (ObjectNode) primitive.get("extensions") : primitive.putObject("extensions");
        ObjectNode extension = extensions.putObject(EXTENSION_NAME);
        extension.set("attributes", extensionAttributes);
        return encoded;
    }

    private DracoEncodeOptions createOptions(DracoCompressionLevel level) {
        DracoEncodeOptions options = new DracoEncodeOptions();
        options.setPositionBits(positionBits);
        options.setNormalBits(normalBits);
        options.setTextureCoordinateBits(texcoordBits);
        options.setColorBits(colorBits);
        options.setCompressionLevel(level);
        return options;
    }

    /**
     * Drops triangles that repeat a vertex index; they draw nothing and break the encoder's corner table.
     */
    static int[] removeDegenerateFaces(int[] indices) {
        int[] faces = new int[indices.length - indices.length % 3];
        int count = 0;
        for (int i = 0; i + 2 < indices.length; i += 3) {
            int a = indices[i], b = indices[i + 1], c = indices[i + 2];
            if (a != b && b != c && a != c) {
                faces[count++] = a;
                faces[count++] = b;
                faces[count++] = c;
            }
        }
        return count == faces.length ? faces : Arrays.copyOf(faces, count);
    }

    /**
     * The attribute as 32-bit floats. The encoder only quantizes float attributes and mishandles integer attributes
     * narrower than 32 bits or with several components, so integers are widened (normalized ones to [0, 1] / [-1, 1]);
     * generic floats such as _BATCHID and _FEATURE_ID_n are stored losslessly.
     */
    private PointAttribute createAttribute(String name, ObjectNode accessor, ArrayNode bufferViews, byte[] bin) {
        int componentType = accessor.get("componentType").asInt();
        int components = getComponentCount(accessor.get("type").asText());
        if (components <= 0 || getComponentSize(componentType) <= 0) {
            return null;
        }
        boolean normalized = accessor.path("normalized").asBoolean(false);
        byte[] data = readElements(accessor, bufferViews, bin, components * getComponentSize(componentType));
        ByteBuffer source = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
        int values = accessor.get("count").asInt() * components;
        ByteBuffer floats = ByteBuffer.allocate(values * 4).order(ByteOrder.LITTLE_ENDIAN);
        for (int i = 0; i < values; i++) {
            floats.putFloat(readComponent(source, i, componentType, normalized));
        }
        return new PointAttribute(getAttributeType(name), DataType.FLOAT32, components, false, -1, 0, new DataBuffer(floats.array()));
    }

    static float readComponent(ByteBuffer source, int index, int componentType, boolean normalized) {
        return switch (componentType) {
            case GL_BYTE -> normalized ? Math.max(source.get(index) / 127.0f, -1.0f) : source.get(index);
            case GL_UNSIGNED_BYTE -> normalized ? (source.get(index) & 0xFF) / 255.0f : source.get(index) & 0xFF;
            case GL_SHORT -> normalized ? Math.max(source.getShort(index * 2) / 32767.0f, -1.0f) : source.getShort(index * 2);
            case GL_UNSIGNED_SHORT -> normalized ? (source.getShort(index * 2) & 0xFFFF) / 65535.0f : source.getShort(index * 2) & 0xFFFF;
            case GL_UNSIGNED_INT -> source.getInt(index * 4) & 0xFFFFFFFFL;
            default -> source.getFloat(index * 4);
        };
    }

    /**
     * Retypes an attribute accessor to the floats the Draco buffer decodes to, converting normalized min / max.
     */
    private void toFloatAccessor(ObjectNode accessor) {
        int componentType = accessor.get("componentType").asInt();
        if (componentType == GL_FLOAT) {
            return;
        }
        boolean normalized = accessor.path("normalized").asBoolean(false);
        for (String bound : new String[]{"min", "max"}) {
            if (normalized && accessor.get(bound) instanceof ArrayNode values) {
                ArrayNode converted = objectMapper.createArrayNode();
                values.forEach(value -> converted.add(normalize(value.asDouble(), componentType)));
                accessor.set(bound, converted);
            }
        }
        accessor.put("componentType", GL_FLOAT);
        accessor.remove("normalized");
    }

    private static double normalize(double value, int componentType) {
        return switch (componentType) {
            case GL_BYTE -> Math.max(value / 127.0, -1.0);
            case GL_UNSIGNED_BYTE -> value / 255.0;
            case GL_SHORT -> Math.max(value / 32767.0, -1.0);
            case GL_UNSIGNED_SHORT -> value / 65535.0;
            default -> value;
        };
    }

    private int[] readIndices(ObjectNode accessor, ArrayNode bufferViews, byte[] bin) {
        int componentType = accessor.get("componentType").asInt();
        int size = getComponentSize(componentType);
        ByteBuffer data = ByteBuffer.wrap(readElements(accessor, bufferViews, bin, size)).order(ByteOrder.LITTLE_ENDIAN);
        int[] indices = new int[accessor.get("count").asInt()];
        for (int i = 0; i < indices.length; i++) {
            indices[i] = switch (componentType) {
                case GL_UNSIGNED_BYTE -> data.get(i) & 0xFF;
                case GL_UNSIGNED_SHORT -> data.getShort(i * 2) & 0xFFFF;
                default -> data.getInt(i * 4);
            };
        }
        return indices;
    }

    /**
     * Tightly packed element bytes of an accessor, following the buffer view stride.
     */
    private byte[] readElements(ObjectNode accessor, ArrayNode bufferViews, byte[] bin, int elementSize) {
        JsonNode view = bufferViews.get(accessor.get("bufferView").asInt());
        int count = accessor.get("count").asInt();
        int start = view.path("byteOffset").asInt(0) + accessor.path("byteOffset").asInt(0);
        int stride = view.path("byteStride").asInt(0) > 0 ? view.get("byteStride").asInt() : elementSize;
        byte[] data = new byte[count * elementSize];
        for (int i = 0; i < count; i++) {
            System.arraycopy(bin, start + i * stride, data, i * elementSize, elementSize);
        }
        return data;
    }

    private Map<Integer, Integer> countAccessorUses(ObjectNode gltf) {
        Map<Integer, Integer> uses = new HashMap<>();
        for (JsonNode mesh : arrayOf(gltf, "meshes")) {
            for (JsonNode primitive : arrayOf((ObjectNode) mesh, "primitives")) {
                if (primitive.has("indices")) {
                    uses.merge(primitive.get("indices").asInt(), 1, Integer::sum);
                }
                primitive.path("attributes").forEach(id -> uses.merge(id.asInt(), 1, Integer::sum));
                primitive.path("targets").forEach(target -> target.forEach(id -> uses.merge(id.asInt(), 2, Integer::sum)));
            }
        }
        // instancing, skins and animations also read accessors; mark them as shared
        for (JsonNode node : arrayOf(gltf, "nodes")) {
            node.path("extensions").forEach(extension -> extension.path("attributes").forEach(id -> uses.merge(id.asInt(), 2, Integer::sum)));
        }
        arrayOf(gltf, "skins").forEach(skin -> uses.merge(skin.path("inverseBindMatrices").asInt(-1), 2, Integer::sum));
        arrayOf(gltf, "animations").forEach(animation -> animation.path("samplers").forEach(sampler -> {
            uses.merge(sampler.path("input").asInt(-1), 2, Integer::sum);
            uses.merge(sampler.path("output").asInt(-1), 2, Integer::sum);
        }));
        return uses;
    }

    private void collectBufferViews(JsonNode node, Set<Integer> referenced, boolean rewrite, int... remap) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                if (BUFFER_VIEW_KEYS.contains(entry.getKey()) && entry.getValue().isInt()) {
                    if (rewrite) {
                        ((ObjectNode) node).put(entry.getKey(), remap[entry.getValue().asInt()]);
                    } else {
                        referenced.add(entry.getValue().asInt());
                    }
                } else if (!"bufferViews".equals(entry.getKey())) {
                    collectBufferViews(entry.getValue(), referenced, rewrite, remap);
                }
            }
        } else if (node.isArray()) {
            node.forEach(child -> collectBufferViews(child, referenced, rewrite, remap));
        }
    }

    private byte[] writeGlb(ObjectNode gltf, byte[] body) throws Exception {
        byte[] json = objectMapper.writeValueAsBytes(gltf);
        int jsonPadded = (json.length + 3) & ~3;
        int binPadded = (body.length + 3) & ~3;
        int total = 12 + 8 + jsonPadded + (body.length > 0 ? 8 + binPadded : 0);
        ByteBuffer out = ByteBuffer.allocate(total).order(ByteOrder.LITTLE_ENDIAN);
        out.putInt(GLB_MAGIC).putInt(2).putInt(total);
        out.putInt(jsonPadded).putInt(CHUNK_JSON).put(json);
        for (int i = json.length; i < jsonPadded; i++) {
            out.put((byte) ' ');
        }
        if (body.length > 0) {
            out.putInt(binPadded).putInt(CHUNK_BIN).put(body);
        }
        return out.array();
    }

    private void addExtension(ObjectNode gltf, String key) {
        ArrayNode names = gltf.has(key) ? (ArrayNode) gltf.get(key) : gltf.putArray(key);
        for (JsonNode name : names) {
            if (EXTENSION_NAME.equals(name.asText())) {
                return;
            }
        }
        names.add(EXTENSION_NAME);
    }

    private static ArrayNode arrayOf(ObjectNode node, String key) {
        JsonNode value = node.get(key);
        return value instanceof ArrayNode array ? array : new ObjectMapper().createArrayNode();
    }

    static int getAttributeType(String name) {
        if (name.equals("POSITION")) {
            return com.openize.drako.AttributeType.POSITION;
        } else if (name.equals("NORMAL")) {
            return com.openize.drako.AttributeType.NORMAL;
        } else if (name.startsWith("TEXCOORD_")) {
            return com.openize.drako.AttributeType.TEX_COORD;
        } else if (name.startsWith("COLOR_")) {
            return com.openize.drako.AttributeType.COLOR;
        }
        return com.openize.drako.AttributeType.GENERIC;
    }

    private static int getComponentSize(int componentType) {
        return switch (componentType) {
            case GL_BYTE, GL_UNSIGNED_BYTE -> 1;
            case GL_SHORT, GL_UNSIGNED_SHORT -> 2;
            case GL_UNSIGNED_INT, GL_FLOAT -> 4;
            default -> 0;
        };
    }

    private static int getComponentCount(String type) {
        return switch (type) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4" -> 4;
            default -> 0;
        };
    }
}
