package com.gaia3d.converter.assimp.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.TextureType;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("release")
class GaiaSceneValidationReportJsonTest {
    private final GaiaSceneValidator validator = new GaiaSceneValidator();
    private final ObjectMapper mapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void toJsonProducesValidJsonForCleanScene() throws IOException {
        Path source = Files.writeString(tempDir.resolve("model.obj"), "model");
        Files.write(tempDir.resolve("texture.png"), new byte[]{1});

        GaiaScene scene = createScene();
        scene.getRootNode().setTransformMatrix(new Matrix4d().translation(10, 20, 30));
        addTexture(scene, "texture.png");

        GaiaSceneValidationReport report = validator.validate(source.toFile(), List.of(scene));
        String json = report.toJson();

        JsonNode root = mapper.readTree(json);

        assertEquals(source.toAbsolutePath().toString(), root.get("source").asText());
        assertTrue(root.get("sourceFileExists").asBoolean());
        assertTrue(root.get("sourceFileReadable").asBoolean());
        assertFalse(root.get("hasIssues").asBoolean());
        assertEquals(0, root.get("issueCount").asInt());

        JsonNode counts = root.get("counts");
        assertEquals(1, counts.get("scenes").asInt());
        assertEquals(1, counts.get("nodes").asInt());
        assertEquals(1, counts.get("meshes").asInt());
        assertEquals(1, counts.get("primitives").asInt());
        assertEquals(1, counts.get("materials").asInt());
        assertEquals(1, counts.get("textures").asInt());
        assertEquals(3, counts.get("vertices").asInt());
        assertEquals(3, counts.get("positions").asInt());
        assertEquals(3, counts.get("normals").asInt());
        assertEquals(1, counts.get("faces").asInt());
        assertEquals(3, counts.get("indices").asInt());

        JsonNode invalid = root.get("invalidCounts");
        assertEquals(0, invalid.get("invalidVertices").asInt());
        assertEquals(0, invalid.get("invalidFaces").asInt());
        assertEquals(0, invalid.get("degenerateTriangles").asInt());
        assertEquals(0, invalid.get("invalidTransforms").asInt());

        JsonNode bbox = root.get("boundingBox");
        assertNotNull(bbox);
        assertFalse(bbox.isNull());
        assertEquals(10.0, bbox.get("min").get("x").asDouble(), 1e-6);
        assertEquals(20.0, bbox.get("min").get("y").asDouble(), 1e-6);
        assertEquals(30.0, bbox.get("min").get("z").asDouble(), 1e-6);

        JsonNode center = root.get("center");
        assertNotNull(center);
        assertFalse(center.isNull());
        assertEquals(11.0, center.get("x").asDouble(), 1e-6);
        assertEquals(22.0, center.get("y").asDouble(), 1e-6);
        assertEquals(30.0, center.get("z").asDouble(), 1e-6);

        assertTrue(root.get("details").isArray());
        assertEquals(0, root.get("details").size());
    }

    @Test
    void toJsonIncludesIssueDetailsForMalformedScene() throws IOException {
        File missingSource = tempDir.resolve("missing.obj").toFile();
        GaiaScene scene = createScene();
        scene.getRootNode().setTransformMatrix(new Matrix4d().m00(Double.NaN));
        addTexture(scene, "missing-texture.png");

        GaiaSceneValidationReport report = validator.validate(missingSource, List.of(scene));
        String json = report.toJson();

        JsonNode root = mapper.readTree(json);

        assertFalse(root.get("sourceFileExists").asBoolean());
        assertTrue(root.get("hasIssues").asBoolean());
        assertTrue(root.get("issueCount").asInt() > 0);

        assertEquals(1, root.get("invalidCounts").get("invalidTransforms").asInt());
        assertEquals(1, root.get("invalidCounts").get("missingExternalResources").asInt());

        JsonNode details = root.get("details");
        assertTrue(details.isArray());
        assertTrue(details.size() > 0);

        boolean foundTransformIssue = false;
        for (JsonNode detail : details) {
            if (detail.asText().contains("non-finite transform")) {
                foundTransformIssue = true;
                break;
            }
        }
        assertTrue(foundTransformIssue, "Expected a detail about non-finite transform");
    }

    @Test
    void toJsonReturnsBoundingBoxNullWhenNoBoundingBox() {
        GaiaSceneValidationReport report = new GaiaSceneValidationReport((File) null);
        report.addIssue("No scenes were converted.");

        String json = report.toJson();

        JsonNode root = assertDoesNotThrow(() -> mapper.readTree(json));
        assertTrue(root.get("boundingBox").isNull());
        assertTrue(root.get("center").isNull());
    }

    @Test
    void writeJsonFileCreatesFileWithCorrectContent() throws IOException {
        Path source = Files.writeString(tempDir.resolve("model.obj"), "model");
        GaiaScene scene = createScene();
        GaiaSceneValidationReport report = validator.validate(source.toFile(), List.of(scene));

        File outputFile = tempDir.resolve("report.json").toFile();
        report.writeJsonFile(outputFile);

        assertTrue(outputFile.exists());
        String content = Files.readString(outputFile.toPath(), StandardCharsets.UTF_8);
        JsonNode root = mapper.readTree(content);

        assertEquals(source.toAbsolutePath().toString(), root.get("source").asText());
        assertNotNull(root.get("counts"));
        assertNotNull(root.get("invalidCounts"));
    }

    @Test
    void writeJsonFileCreatesParentDirContentsOnly() throws IOException {
        Path source = Files.writeString(tempDir.resolve("model.obj"), "model");
        GaiaSceneValidationReport report = validator.validate(source.toFile(), List.of(createScene()));

        Path outputPath = tempDir.resolve("subdir").resolve("validation.json");
        Files.createDirectories(outputPath.getParent());
        report.writeJsonFile(outputPath.toFile());

        assertTrue(Files.exists(outputPath));
        JsonNode root = mapper.readTree(outputPath.toFile());
        assertTrue(root.has("source"));
    }

    @Test
    void toJsonOutputIsPrettyPrinted() throws IOException {
        Path source = Files.writeString(tempDir.resolve("model.obj"), "model");
        GaiaSceneValidationReport report = validator.validate(source.toFile(), List.of(createScene()));

        String json = report.toJson();

        assertTrue(json.contains("\n"), "Expected pretty-printed JSON with newlines");
        assertTrue(json.contains("  "), "Expected pretty-printed JSON with indentation");
    }

    private GaiaScene createScene() {
        GaiaFace face = new GaiaFace();
        face.setIndices(new int[]{0, 1, 2});
        GaiaSurface surface = new GaiaSurface();
        surface.getFaces().add(face);
        GaiaPrimitive primitive = new GaiaPrimitive();
        primitive.setMaterialIndex(0);
        primitive.setVertices(List.of(vertex(0, 0, 0), vertex(2, 0, 0), vertex(0, 4, 0)));
        primitive.getSurfaces().add(surface);
        GaiaMesh mesh = new GaiaMesh();
        mesh.getPrimitives().add(primitive);
        GaiaNode node = new GaiaNode();
        node.getMeshes().add(mesh);
        GaiaScene scene = new GaiaScene();
        scene.getNodes().add(node);
        scene.getMaterials().add(new GaiaMaterial());
        return scene;
    }

    private GaiaVertex vertex(double x, double y, double z) {
        GaiaVertex vertex = new GaiaVertex();
        vertex.setPosition(new Vector3d(x, y, z));
        vertex.setNormal(new Vector3d(0, 0, 1));
        vertex.setTexcoords(new Vector2d(0, 0));
        vertex.setColor(new byte[4]);
        return vertex;
    }

    private void addTexture(GaiaScene scene, String path) {
        GaiaTexture texture = new GaiaTexture();
        texture.setType(TextureType.DIFFUSE);
        texture.setParentPath(tempDir.toString());
        texture.setPath(path);
        scene.getMaterials().getFirst().getTextures().put(TextureType.DIFFUSE, List.of(texture));
    }
}
