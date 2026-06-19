package com.gaia3d.basic.magogl.maker;

import com.gaia3d.basic.model.*;
import com.gaia3d.basic.magogl.MagoBuffer;
import com.gaia3d.basic.magogl.renderable.MagoRenderableMesh;
import com.gaia3d.basic.magogl.renderable.MagoRenderableNode;
import com.gaia3d.basic.magogl.renderable.MagoRenderablePrimitive;
import com.gaia3d.basic.magogl.renderable.MagoRenderableScene;
import com.gaia3d.basic.magogl.texture.MagoTexture2D;
import com.gaia3d.basic.types.TextureType;
import org.joml.Matrix4d;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class MagoRenderableMaker {
    private final Map<String, MagoTexture2D> textureCache =
            new HashMap<>();

    public MagoRenderableScene makeScene(GaiaScene gaiaScene) {
        Objects.requireNonNull(
                gaiaScene,
                "gaiaScene must not be null"
        );

        MagoRenderableScene result = new MagoRenderableScene();

        result.setOriginalGaiaScene(gaiaScene);

        List<GaiaMaterial> sourceMaterials = gaiaScene.getMaterials();

        if (sourceMaterials != null) {
            result.getMaterials().addAll(sourceMaterials);
        }

        List<GaiaNode> rootNodes = gaiaScene.getNodes();

        if (rootNodes == null || rootNodes.isEmpty()) {
            return result;
        }

        Matrix4d identity = new Matrix4d();

        for (GaiaNode gaiaNode : rootNodes) {
            if (gaiaNode == null) {
                continue;
            }

            MagoRenderableNode renderableNode = makeNode(
                    gaiaNode,
                    identity,
                    result.getMaterials()
            );

            result.getRenderableNodes().add(renderableNode);
        }

        return result;
    }

    private MagoRenderableNode makeNode(
            GaiaNode gaiaNode,
            Matrix4d parentWorldMatrix,
            List<GaiaMaterial> materials
    ) {
        MagoRenderableNode result = new MagoRenderableNode();

        result.setOriginalGaiaNode(gaiaNode);
        result.setName(gaiaNode.getName());

        Matrix4d localMatrix = gaiaNode.getTransformMatrix();

        if (localMatrix == null) {
            localMatrix = new Matrix4d();
        }

        Matrix4d worldMatrix = new Matrix4d(parentWorldMatrix)
                .mul(localMatrix);

        result.setTransformMatrix(localMatrix);
        result.setPreMultipliedTransformMatrix(worldMatrix);

        if (gaiaNode.getMeshes() != null) {
            for (GaiaMesh gaiaMesh : gaiaNode.getMeshes()) {
                if (gaiaMesh == null) {
                    continue;
                }

                MagoRenderableMesh mesh =
                        makeMesh(gaiaMesh, materials);

                result.addRenderableMesh(mesh);
            }
        }

        if (gaiaNode.getChildren() != null) {
            for (GaiaNode gaiaChild : gaiaNode.getChildren()) {
                if (gaiaChild == null) {
                    continue;
                }

                MagoRenderableNode child = makeNode(
                        gaiaChild,
                        worldMatrix,
                        materials
                );

                result.addChild(child);
            }
        }

        return result;
    }

    private MagoRenderableMesh makeMesh(
            GaiaMesh gaiaMesh,
            List<GaiaMaterial> materials
    ) {
        MagoRenderableMesh result = new MagoRenderableMesh();

        result.setOriginalGaiaMesh(gaiaMesh);

        List<GaiaPrimitive> gaiaPrimitives =
                gaiaMesh.getPrimitives();

        if (gaiaPrimitives == null) {
            return result;
        }

        for (GaiaPrimitive gaiaPrimitive : gaiaPrimitives) {
            if (gaiaPrimitive == null) {
                continue;
            }

            MagoRenderablePrimitive renderablePrimitive =
                    makePrimitive(gaiaPrimitive, materials);

            if (renderablePrimitive != null) {
                result.getRenderablePrimitives()
                        .add(renderablePrimitive);
            }
        }

        return result;
    }

    private MagoRenderablePrimitive makePrimitive(
            GaiaPrimitive gaiaPrimitive,
            List<GaiaMaterial> materials
    ) {
        List<GaiaVertex> vertices =
                gaiaPrimitive.getVertices();

        if (vertices == null || vertices.isEmpty()) {
            return null;
        }

        List<GaiaFace> faces =
                gaiaPrimitive.extractGaiaAllFaces(null);

        if (faces == null || faces.isEmpty()) {
            return null;
        }

        int vertexCount = vertices.size();
        int indexCount = calculateIndexCount(faces);

        if (indexCount == 0) {
            return null;
        }

        if (indexCount % 3 != 0) {
            throw new IllegalArgumentException(
                    "Primitive indices are not triangulated. "
                            + "indexCount="
                            + indexCount
            );
        }

        boolean hasNormals =
                hasNormals(vertices);

        boolean hasTexCoords =
                hasTexCoords(vertices);

        boolean hasColors =
                hasColors(vertices);

        ByteBuffer positionsData =
                createPositionsBuffer(vertices);

        ByteBuffer indicesData =
                createIndicesBuffer(
                        faces,
                        vertexCount
                );

        /*
         * One GaiaFace ID for each triangle written
         * into indicesData.
         */
        int[] faceCodes =
                createFaceCodes(faces);

        ByteBuffer normalsData =
                hasNormals
                        ? createNormalsBuffer(vertices)
                        : null;

        ByteBuffer texCoordsData =
                hasTexCoords
                        ? createTexCoordsBuffer(vertices)
                        : null;

        ByteBuffer colorsData =
                hasColors
                        ? createColorsBuffer(vertices)
                        : null;

        MagoBuffer positionsBuffer =
                new MagoBuffer(positionsData);

        MagoBuffer indicesBuffer =
                new MagoBuffer(indicesData);

        MagoBuffer normalsBuffer =
                normalsData == null
                        ? null
                        : new MagoBuffer(normalsData);

        MagoBuffer texCoordsBuffer =
                texCoordsData == null
                        ? null
                        : new MagoBuffer(texCoordsData);

        MagoBuffer colorsBuffer =
                colorsData == null
                        ? null
                        : new MagoBuffer(colorsData);

        GaiaMaterial material =
                resolveMaterial(
                        gaiaPrimitive,
                        materials
                );

        MagoTexture2D diffuseTexture =
                null;

        if (texCoordsBuffer != null) {
            diffuseTexture =
                    resolveDiffuseTexture(material);
        }

        return new MagoRenderablePrimitive(
                positionsBuffer,
                indicesBuffer,
                normalsBuffer,
                texCoordsBuffer,
                colorsBuffer,
                vertexCount,
                indexCount,
                material,
                diffuseTexture,
                faceCodes
        );
    }

    private static int[] createFaceCodes(
            List<GaiaFace> faces
    ) {
        Objects.requireNonNull(
                faces,
                "faces must not be null"
        );

        boolean hasAssignedIds = false;
        boolean hasUnassignedIds = false;

        for (int i = 0; i < faces.size(); i++) {
            GaiaFace face = faces.get(i);

            if (face == null) {
                throw new IllegalStateException(
                        "Null GaiaFace at index " + i
                );
            }

            int[] indices = face.getIndices();

            if (indices == null || indices.length != 3) {
                throw new IllegalStateException(
                        "GaiaFace must contain exactly 3 indices. "
                                + "faceIndex=" + i
                                + ", faceId=" + face.getId()
                );
            }

            if (face.getId() < 0) {
                hasUnassignedIds = true;
            } else {
                hasAssignedIds = true;
            }
        }

        /*
         * A primitive should not contain a mixture of assigned
         * and unassigned face IDs.
         */
        if (hasAssignedIds && hasUnassignedIds) {
            throw new IllegalStateException(
                    "Primitive contains both assigned and unassigned "
                            + "GaiaFace IDs."
            );
        }

        /*
         * Normal textured scene:
         * face codes are not required.
         */
        if (!hasAssignedIds) {
            return null;
        }

        int[] faceCodes = new int[faces.size()];

        for (int i = 0; i < faces.size(); i++) {
            faceCodes[i] = faces.get(i).getId();
        }

        return faceCodes;
    }

    private MagoTexture2D resolveDiffuseTexture(
            GaiaMaterial material
    ) {
        if (material == null) {
            return null;
        }

        List<GaiaTexture> diffuseTextures =
                material.getTextures().get(TextureType.DIFFUSE);

        if (diffuseTextures == null || diffuseTextures.isEmpty()) {
            return null;
        }

        GaiaTexture gaiaTexture =
                diffuseTextures.getFirst();

        return loadMagoTexture(gaiaTexture);
    }

    private MagoTexture2D loadMagoTexture(
            GaiaTexture gaiaTexture
    ) {
        String textureKey =
                getTextureKey(gaiaTexture);

        return textureCache.computeIfAbsent(
                textureKey,
                key -> createMagoTexture(gaiaTexture)
        );
    }

    private String getTextureKey(GaiaTexture texture) {
        Path path = Path.of(
                texture.getParentPath(),
                texture.getPath()
        );

        return path.toAbsolutePath()
                .normalize()
                .toString();
    }

    private MagoTexture2D createMagoTexture(
            GaiaTexture gaiaTexture
    ) {
        BufferedImage image =
                gaiaTexture.getBufferedImage();

        if (image == null) {
            throw new IllegalStateException(
                    "Could not load diffuse texture: "
                            + gaiaTexture.getPath()
            );
        }

        return MagoTexture2D.fromBufferedImage(image);
    }

    private ByteBuffer createPositionsBuffer(
            List<GaiaVertex> vertices
    ) {
        ByteBuffer buffer = allocate(
                Math.multiplyExact(
                        vertices.size(),
                        3 * Float.BYTES
                )
        );

        for (GaiaVertex vertex : vertices) {
            if (vertex == null || vertex.getPosition() == null) {
                throw new IllegalArgumentException(
                        "Primitive contains a vertex without position."
                );
            }

            Vector3d position = vertex.getPosition();

            buffer.putFloat((float) position.x);
            buffer.putFloat((float) position.y);
            buffer.putFloat((float) position.z);
        }

        buffer.flip();
        return buffer;
    }

    private ByteBuffer createNormalsBuffer(
            List<GaiaVertex> vertices
    ) {
        ByteBuffer buffer = allocate(
                Math.multiplyExact(
                        vertices.size(),
                        3 * Float.BYTES
                )
        );

        for (GaiaVertex vertex : vertices) {
            Vector3d normal = vertex.getNormal();

            buffer.putFloat((float) normal.x);
            buffer.putFloat((float) normal.y);
            buffer.putFloat((float) normal.z);
        }

        buffer.flip();
        return buffer;
    }

    private ByteBuffer createTexCoordsBuffer(
            List<GaiaVertex> vertices
    ) {
        ByteBuffer buffer = allocate(
                Math.multiplyExact(
                        vertices.size(),
                        2 * Float.BYTES
                )
        );

        for (GaiaVertex vertex : vertices) {
            Vector2d texCoords = vertex.getTexcoords();

            buffer.putFloat((float) texCoords.x);
            buffer.putFloat((float) texCoords.y);
        }

        buffer.flip();
        return buffer;
    }

    /**
     * Color format: four unsigned bytes per vertex, RGBA.
     */
    private ByteBuffer createColorsBuffer(
            List<GaiaVertex> vertices
    ) {
        ByteBuffer buffer = allocate(
                Math.multiplyExact(vertices.size(), 4)
        );

        for (GaiaVertex vertex : vertices) {
            byte[] color = vertex.getColor();

            if (color.length < 4) {
                throw new IllegalArgumentException(
                        "Vertex color must contain RGBA values."
                );
            }

            buffer.put(color[0]);
            buffer.put(color[1]);
            buffer.put(color[2]);
            buffer.put(color[3]);
        }

        buffer.flip();
        return buffer;
    }

    private ByteBuffer createIndicesBuffer(
            List<GaiaFace> faces,
            int vertexCount
    ) {
        int indexCount = calculateIndexCount(faces);

        int expectedIndexCount =
                Math.multiplyExact(
                        faces.size(),
                        3
                );

        if (indexCount != expectedIndexCount) {
            throw new IllegalStateException(
                    "Face count and index count do not match. "
                            + "faces="
                            + faces.size()
                            + ", expectedIndexCount="
                            + expectedIndexCount
                            + ", actualIndexCount="
                            + indexCount
            );
        }

        ByteBuffer buffer = allocate(
                Math.multiplyExact(indexCount, Integer.BYTES)
        );

        for (GaiaFace face : faces) {
            if (face == null || face.getIndices() == null) {
                continue;
            }

            int[] indices = face.getIndices();

            for (int index : indices) {
                if (index < 0 || index >= vertexCount) {
                    throw new IndexOutOfBoundsException(
                            "Invalid vertex index: "
                                    + index
                                    + ", vertexCount="
                                    + vertexCount
                    );
                }

                buffer.putInt(index);
            }
        }

        buffer.flip();
        return buffer;
    }

    private int calculateIndexCount(List<GaiaFace> faces) {
        int result = 0;

        for (GaiaFace face : faces) {
            if (face == null || face.getIndices() == null) {
                continue;
            }

            result = Math.addExact(
                    result,
                    face.getIndices().length
            );
        }

        return result;
    }

    private boolean hasNormals(List<GaiaVertex> vertices) {
        for (GaiaVertex vertex : vertices) {
            if (vertex == null || vertex.getNormal() == null) {
                return false;
            }
        }

        return true;
    }

    private boolean hasTexCoords(List<GaiaVertex> vertices) {
        for (GaiaVertex vertex : vertices) {
            if (vertex == null || vertex.getTexcoords() == null) {
                return false;
            }
        }

        return true;
    }

    private boolean hasColors(List<GaiaVertex> vertices) {
        for (GaiaVertex vertex : vertices) {
            if (vertex == null
                    || vertex.getColor() == null
                    || vertex.getColor().length < 4) {

                return false;
            }
        }

        return true;
    }

    private GaiaMaterial resolveMaterial(
            GaiaPrimitive primitive,
            List<GaiaMaterial> materials
    ) {
        if (materials == null || materials.isEmpty()) {
            return null;
        }

        int materialIndex = primitive.getMaterialIndex();

        if (materialIndex < 0 || materialIndex >= materials.size()) {
            return null;
        }

        return materials.get(materialIndex);
    }

    private ByteBuffer allocate(int capacityBytes) {
        return ByteBuffer
                .allocateDirect(capacityBytes)
                .order(ByteOrder.nativeOrder());
    }
}