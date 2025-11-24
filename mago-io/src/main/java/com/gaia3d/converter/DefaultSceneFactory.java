package com.gaia3d.converter;

import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.Classification;
import com.gaia3d.basic.types.TextureType;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;
import org.joml.Vector4d;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@NoArgsConstructor
public class DefaultSceneFactory {
    private static final String ROOT_NODE_NAME = "root";
    private static final String DEFAULT_MATERIAL_NAME = "default-material";

    public GaiaScene createScene(File file) {
        GaiaScene scene = new GaiaScene();

        String fileName = file.getName();
        scene.setOriginalPath(file.toPath());

        // Set the attribute of the scene
        GaiaAttribute attribute = new GaiaAttribute();
        attribute.setIdentifier(UUID.randomUUID());
        attribute.setNodeName(ROOT_NODE_NAME);
        attribute.setFileName(fileName);
        scene.setAttribute(attribute);

        // Set the materials of the scene
        List<GaiaMaterial> materials = createClassificationMaterials();
        scene.setMaterials(materials);

        // Set the root node of the scene
        GaiaNode rootNode = new GaiaNode();
        Matrix4d transformMatrix = new Matrix4d();
        transformMatrix.identity();
        rootNode.setTransformMatrix(transformMatrix);
        rootNode.setName(ROOT_NODE_NAME);
        scene.getNodes().add(rootNode);

        return scene;
    }

    private List<GaiaMaterial> createClassificationMaterials() {
        List<GaiaMaterial> materials = new ArrayList<>();
        materials.add(createDefaultMaterial(0));
        materials.add(createMaterial(1, Classification.DOOR.getColor()));
        materials.add(createMaterial(2, Classification.WINDOW.getColor()));
        materials.add(createMaterial(3, Classification.ROOF.getColor()));
        materials.add(createMaterial(4, Classification.WATER.getColor()));
        materials.add(createMaterial(5, Classification.GROUND.getColor()));
        materials.add(createMaterial(6, Classification.FURNITURE.getColor()));
        materials.add(createMaterial(7, Classification.INSTALLATION.getColor()));
        materials.add(createMaterial(8, Classification.INFRASTRUCTURE.getColor()));
        return materials;
    }

    private GaiaMaterial createMaterial(int id, Vector4d color) {
        GaiaMaterial material = new GaiaMaterial();
        material.setId(id);
        material.setName(DEFAULT_MATERIAL_NAME);
        material.setDiffuseColor(color);
        Map<TextureType, List<GaiaTexture>> textureTypeListMap = material.getTextures();
        textureTypeListMap.put(TextureType.DIFFUSE, new ArrayList<>());
        return material;
    }

    private GaiaMaterial createDefaultMaterial(int id) {
        Vector4d color = new Vector4d(0.9, 0.9, 0.9, 1);
        return createMaterial(id, color);
    }

    /**
     * Create a grid node with specified width and height
     * @param width
     * @param height
     * @return
     */
    public GaiaNode createGridNode(int width, int height) {
        GaiaNode node = new GaiaNode();
        Matrix4d transformMatrix = new Matrix4d();
        transformMatrix.identity();

        node.setTransformMatrix(transformMatrix);
        node.setName("grid-node");

        GaiaMesh mesh = new GaiaMesh();

        GaiaPrimitive primitive = new GaiaPrimitive();
        primitive.setMaterialIndex(0);

        List<GaiaVertex> vertices = createGridVertices(width, height);
        List<GaiaSurface> surfaces = createGridSurface(width, height);

        primitive.setVertices(vertices);
        primitive.setSurfaces(surfaces);

        mesh.getPrimitives().add(primitive);
        node.getMeshes().add(mesh);
        return node;
    }

    // 3X3 grid
    private List<GaiaVertex> createGridVertices(int width, int height) {
        List<GaiaVertex> vertices = new ArrayList<>();
        int indexId = 0;

        int halfWidth = width / 2;
        int halfHeight = height / 2;
        for (int i = -halfHeight; i < halfHeight; i++) {
            for (int j = -halfWidth; j < halfWidth; j++) {
                GaiaVertex vertex = new GaiaVertex();
                Vector3d position = new Vector3d(i, 0, -j);
                Vector3d normal = new Vector3d(0.0, -1.0, 0.0);

                int x = i + halfHeight;
                int y = j + halfWidth;
                Vector2d texcoords = new Vector2d((double) x / width, (double) y / height);
                vertex.setTexcoords(texcoords);
                vertex.setPosition(position);
                vertex.setNormal(normal);
                vertex.setBatchId(indexId++);
                vertices.add(vertex);
            }
        }
        return vertices;
    }

    private List<GaiaSurface> createGridSurface(int width, int height) {
        List<GaiaSurface> surfaces = new ArrayList<>();
        for (int i = 0; i < height - 1; i++) {
            GaiaSurface surface = new GaiaSurface();
            int heightIndex = i * width;
            for (int j = 0; j < width - 1; j++) {
                GaiaFace face = new GaiaFace();
                int[] indices = new int[6];

                int currentIndex = heightIndex + j;
                int downIndex = currentIndex + width;
                int rightIndex = currentIndex + 1;
                int rightDownIndex = downIndex + 1;

                indices[0] = currentIndex;
                indices[1] = downIndex;
                indices[2] = rightDownIndex;

                indices[3] = currentIndex;
                indices[4] = rightDownIndex;
                indices[5] = rightIndex;

                face.setIndices(indices);
                surface.getFaces().add(face);
            }
            surfaces.add(surface);
        }
        return surfaces;
    }
}
