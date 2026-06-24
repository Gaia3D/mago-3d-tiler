package com.gaia3d.converter.assimp.validation;

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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("release")
class GaiaSceneValidatorTest {
    private final GaiaSceneValidator validator = new GaiaSceneValidator();

    @TempDir
    Path tempDir;

    @Test
    void reportsCountsResourcesAndTransformedBoundingBoxForValidScene() throws IOException {
        Path source = Files.writeString(tempDir.resolve("model.obj"), "model");
        Files.write(tempDir.resolve("texture.png"), new byte[]{1});

        GaiaScene scene = createScene(List.of(vertex(0, 0, 0), vertex(2, 0, 0), vertex(0, 4, 0)), new int[]{0, 1, 2});
        scene.getRootNode().setTransformMatrix(new Matrix4d().translation(10, 20, 30));
        addTexture(scene, "texture.png");

        GaiaSceneValidationReport report = validator.validate(source.toFile(), List.of(scene));

        assertFalse(report.hasIssues(), report.toDetailString());
        assertTrue(report.isSourceFileExists());
        assertTrue(report.isSourceFileReadable());
        assertEquals(1, report.getSceneCount());
        assertEquals(1, report.getNodeCount());
        assertEquals(1, report.getTransformCount());
        assertEquals(1, report.getMeshCount());
        assertEquals(1, report.getPrimitiveCount());
        assertEquals(1, report.getMaterialCount());
        assertEquals(1, report.getTextureCount());
        assertEquals(3, report.getVertexCount());
        assertEquals(3, report.getPositionCount());
        assertEquals(3, report.getNormalCount());
        assertEquals(3, report.getTexcoordCount());
        assertEquals(1, report.getFaceCount());
        assertEquals(3, report.getIndexCount());
        assertEquals(new Vector3d(10, 20, 30), report.getBoundingBox().getMinPosition());
        assertEquals(new Vector3d(12, 24, 30), report.getBoundingBox().getMaxPosition());
        assertEquals(new Vector3d(11, 22, 30), report.getCenter());
    }

    @Test
    void collectsMalformedSceneIssuesWithoutThrowing() {
        File missingSource = tempDir.resolve("missing.obj").toFile();
        List<GaiaVertex> vertices = new ArrayList<>();
        vertices.add(vertex(0, 0, 0));
        vertices.add(vertex(1, 0, 0));
        vertices.add(vertex(2, 0, 0));
        vertices.get(1).setNormal(null);
        vertices.get(2).setColor(new byte[3]);
        vertices.get(2).setBatchId(Float.POSITIVE_INFINITY);

        GaiaScene scene = createScene(vertices, new int[]{0, 1, 1});
        GaiaSurface surface = scene.getRootNode().getMeshes().getFirst().getPrimitives().getFirst().getSurfaces().getFirst();
        GaiaFace outOfRange = new GaiaFace();
        outOfRange.setIndices(new int[]{0, 1, 99});
        surface.getFaces().add(outOfRange);
        scene.getRootNode().setTransformMatrix(new Matrix4d().m00(Double.NaN));
        addTexture(scene, "missing-texture.png");

        GaiaSceneValidationReport report = validator.validate(missingSource, List.of(scene));

        assertTrue(report.hasIssues());
        assertFalse(report.isSourceFileExists());
        assertEquals(1, report.getInvalidTransformCount());
        assertEquals(1, report.getInvalidIndexCount());
        assertEquals(1, report.getDegenerateTriangleCount());
        assertEquals(1, report.getInvalidColorCount());
        assertEquals(1, report.getInvalidBatchIdCount());
        assertEquals(1, report.getMissingExternalResourceCount());
        assertTrue(report.getDetails().stream().anyMatch(detail -> detail.contains("partial normals")));
    }

    @Test
    void handlesNullVerticesAndCyclicNodes() throws IOException {
        Path source = Files.writeString(tempDir.resolve("cyclic.obj"), "model");
        GaiaScene scene = createScene(new ArrayList<>(List.of(vertex(0, 0, 0), vertex(1, 0, 0))), new int[]{0, 1, 1});
        scene.getRootNode().getMeshes().getFirst().getPrimitives().getFirst().getVertices().set(1, null);
        scene.getRootNode().getChildren().add(scene.getRootNode());

        GaiaSceneValidationReport report = validator.validate(source.toFile(), List.of(scene));

        assertTrue(report.hasIssues());
        assertEquals(1, report.getInvalidVertexCount());
        assertTrue(report.getDetails().stream().anyMatch(detail -> detail.contains("cyclic node")));
    }

    private GaiaScene createScene(List<GaiaVertex> vertices, int[] indices) {
        GaiaFace face = new GaiaFace();
        face.setIndices(indices);
        GaiaSurface surface = new GaiaSurface();
        surface.getFaces().add(face);
        GaiaPrimitive primitive = new GaiaPrimitive();
        primitive.setMaterialIndex(0);
        primitive.setVertices(vertices);
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
