package com.gaia3d.basic.magogl.renderable;

import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.magogl.MagoBuffer;
import com.gaia3d.basic.magogl.texture.MagoTexture2D;
import lombok.AccessLevel;
import lombok.Getter;

import java.util.Arrays;
import java.util.Objects;

@Getter
public final class MagoRenderablePrimitive {

    /*
     * Required attributes.
     */
    private MagoBuffer positionsBuffer;
    private MagoBuffer indicesBuffer;

    /*
     * Optional vertex attributes.
     */
    private MagoBuffer normalsBuffer;
    private MagoBuffer texCoordsBuffer;
    private MagoBuffer colorsBuffer;

    private final int vertexCount;
    private final int indexCount;

    private GaiaMaterial material;
    private MagoTexture2D diffuseTexture;



    /*
     * One face code per rendered triangle.
     *
     * faceCodes[triangleIndex] contains the exact GaiaFace.id
     * associated with that triangle.
     */
    @Getter(AccessLevel.NONE)
    private final int[] faceCodes;

    /**
     * Constructor used when face-code rendering is not required,
     * or when this is the only primitive in the scene.
     */
    public MagoRenderablePrimitive(
            MagoBuffer positionsBuffer,
            MagoBuffer indicesBuffer,
            MagoBuffer normalsBuffer,
            MagoBuffer texCoordsBuffer,
            MagoBuffer colorsBuffer,
            int vertexCount,
            int indexCount,
            GaiaMaterial material,
            MagoTexture2D diffuseTexture
    ) {
        this(
                positionsBuffer,
                indicesBuffer,
                normalsBuffer,
                texCoordsBuffer,
                colorsBuffer,
                vertexCount,
                indexCount,
                material,
                diffuseTexture,
                null
        );
    }

    /**
     * Constructor with a global face-code offset.
     */
    public MagoRenderablePrimitive(
            MagoBuffer positionsBuffer,
            MagoBuffer indicesBuffer,
            MagoBuffer normalsBuffer,
            MagoBuffer texCoordsBuffer,
            MagoBuffer colorsBuffer,
            int vertexCount,
            int indexCount,
            GaiaMaterial material,
            MagoTexture2D diffuseTexture,
            int[] faceCodes
    ) {
        this.positionsBuffer = Objects.requireNonNull(
                positionsBuffer,
                "positionsBuffer must not be null"
        );

        this.indicesBuffer = Objects.requireNonNull(
                indicesBuffer,
                "indicesBuffer must not be null"
        );

        if (vertexCount <= 0) {
            throw new IllegalArgumentException(
                    "vertexCount must be greater than zero."
            );
        }

        if (indexCount <= 0 || indexCount % 3 != 0) {
            throw new IllegalArgumentException(
                    "indexCount must be greater than zero "
                            + "and divisible by 3."
            );
        }

        validateBufferSizes(
                positionsBuffer,
                indicesBuffer,
                normalsBuffer,
                texCoordsBuffer,
                colorsBuffer,
                vertexCount,
                indexCount
        );

        int trianglesCount =
                indexCount / 3;

        if (faceCodes != null) {
            if (faceCodes.length != trianglesCount) {
                throw new IllegalArgumentException(
                        "faceCodes length must match triangles count. "
                                + "Expected: "
                                + trianglesCount
                                + ", actual: "
                                + faceCodes.length
                );
            }

            for (int triangleIndex = 0;
                 triangleIndex < faceCodes.length;
                 triangleIndex++) {

                int faceCode =
                        faceCodes[triangleIndex];

                /*
                 * FaceVisibilityData uses the code as an array index.
                 * Therefore valid face IDs must be non-negative.
                 */
                if (faceCode < 0) {
                    throw new IllegalArgumentException(
                            "Invalid face code at triangle "
                                    + triangleIndex
                                    + ": "
                                    + faceCode
                    );
                }
            }
        }

        this.normalsBuffer = normalsBuffer;
        this.texCoordsBuffer = texCoordsBuffer;
        this.colorsBuffer = colorsBuffer;

        this.vertexCount = vertexCount;
        this.indexCount = indexCount;

        this.material = material;
        this.diffuseTexture = diffuseTexture;

        /*
         * Defensive copy to preserve immutability.
         */
        this.faceCodes = faceCodes == null
                ? null
                : Arrays.copyOf(
                faceCodes,
                faceCodes.length
        );
    }

    public boolean hasNormals() {
        return normalsBuffer != null;
    }

    public boolean hasTexCoords() {
        return texCoordsBuffer != null;
    }

    public boolean hasColors() {
        return colorsBuffer != null;
    }

    public int getTrianglesCount() {
        return indexCount / 3;
    }

    public boolean hasFaceCodes() {
        return faceCodes != null;
    }

    public int getFaceCode(int triangleIndex) {
        if (faceCodes == null) {
            throw new IllegalStateException(
                    "MagoRenderablePrimitive has no face codes."
            );
        }

        if (triangleIndex < 0
                || triangleIndex >= faceCodes.length) {

            throw new IndexOutOfBoundsException(
                    "Invalid triangle index: "
                            + triangleIndex
                            + ", trianglesCount="
                            + faceCodes.length
            );
        }

        return faceCodes[triangleIndex];
    }

    public int[] getFaceCodesCopy() {
        return faceCodes == null
                ? null
                : Arrays.copyOf(
                faceCodes,
                faceCodes.length
        );
    }

    private static void validateBufferSizes(
            MagoBuffer positionsBuffer,
            MagoBuffer indicesBuffer,
            MagoBuffer normalsBuffer,
            MagoBuffer texCoordsBuffer,
            MagoBuffer colorsBuffer,
            int vertexCount,
            int indexCount
    ) {
        int requiredPositionsBytes =
                Math.multiplyExact(
                        vertexCount,
                        3 * Float.BYTES
                );

        if (positionsBuffer.getSizeBytes()
                < requiredPositionsBytes) {

            throw new IllegalArgumentException(
                    "Positions buffer is too small. Required: "
                            + requiredPositionsBytes
                            + ", available: "
                            + positionsBuffer.getSizeBytes()
            );
        }

        int requiredIndicesBytes =
                Math.multiplyExact(
                        indexCount,
                        Integer.BYTES
                );

        if (indicesBuffer.getSizeBytes()
                < requiredIndicesBytes) {

            throw new IllegalArgumentException(
                    "Indices buffer is too small. Required: "
                            + requiredIndicesBytes
                            + ", available: "
                            + indicesBuffer.getSizeBytes()
            );
        }

        if (normalsBuffer != null) {
            int requiredNormalsBytes =
                    Math.multiplyExact(
                            vertexCount,
                            3 * Float.BYTES
                    );

            if (normalsBuffer.getSizeBytes()
                    < requiredNormalsBytes) {

                throw new IllegalArgumentException(
                        "Normals buffer is too small. Required: "
                                + requiredNormalsBytes
                                + ", available: "
                                + normalsBuffer.getSizeBytes()
                );
            }
        }

        if (texCoordsBuffer != null) {
            int requiredTexCoordsBytes =
                    Math.multiplyExact(
                            vertexCount,
                            2 * Float.BYTES
                    );

            if (texCoordsBuffer.getSizeBytes()
                    < requiredTexCoordsBytes) {

                throw new IllegalArgumentException(
                        "Texture coordinates buffer is too small. Required: "
                                + requiredTexCoordsBytes
                                + ", available: "
                                + texCoordsBuffer.getSizeBytes()
                );
            }
        }

        if (colorsBuffer != null) {
            /*
             * Four unsigned bytes per vertex: RGBA.
             */
            int requiredColorsBytes =
                    Math.multiplyExact(
                            vertexCount,
                            4
                    );

            if (colorsBuffer.getSizeBytes()
                    < requiredColorsBytes) {

                throw new IllegalArgumentException(
                        "Colors buffer is too small. Required: "
                                + requiredColorsBytes
                                + ", available: "
                                + colorsBuffer.getSizeBytes()
                );
            }
        }
    }

    public void deleteObjects() {
        positionsBuffer.delete();
        indicesBuffer.delete();

        if (normalsBuffer != null) {
            normalsBuffer.delete();
        }

        if (texCoordsBuffer != null) {
            texCoordsBuffer.delete();
        }

        if (colorsBuffer != null) {
            colorsBuffer.delete();
        }

        if (diffuseTexture != null) {
            diffuseTexture.delete();
        }

        positionsBuffer = null;
        indicesBuffer = null;
        normalsBuffer = null;
        texCoordsBuffer = null;
        colorsBuffer = null;
        diffuseTexture = null;
    }
}