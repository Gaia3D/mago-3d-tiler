package com.gaia3d.basic.magogl;

import com.gaia3d.basic.magogl.renderable.MagoRenderableMesh;
import com.gaia3d.basic.magogl.renderable.MagoRenderableNode;
import com.gaia3d.basic.magogl.renderable.MagoRenderablePrimitive;
import com.gaia3d.basic.magogl.renderable.MagoRenderableScene;
import com.gaia3d.basic.magogl.shader.*;
import com.gaia3d.basic.magogl.shader.program.MagoShaderProgram;
import org.joml.Vector4f;

import java.nio.ByteBuffer;
import java.util.Objects;


public final class MagoRenderer {
    private static final float EPSILON = 1e-7f;

    public void renderScene(
            MagoRenderableScene scene,
            MagoRenderContext context
    ) {
        Objects.requireNonNull(scene, "scene must not be null");
        Objects.requireNonNull(context, "context must not be null");

        if (context.getFbo() == null) {
            throw new IllegalStateException(
                    "No MagoFbo configured in render context."
            );
        }

        if (context.getShaderProgram() == null) {
            throw new IllegalStateException(
                    "No shader program configured."
            );
        }

        for (MagoRenderableNode rootNode
                : scene.getRenderableNodes()) {

            if (rootNode != null) {
                renderNode(rootNode, context);
            }
        }
    }

    private void renderNode(
            MagoRenderableNode node,
            MagoRenderContext context
    ) {
        prepareNodeUniforms(node, context);

        for (MagoRenderableMesh mesh
                : node.getRenderableMeshes()) {

            renderMesh(mesh, context);
        }

        for (MagoRenderableNode child
                : node.getChildren()) {

            renderNode(child, context);
        }
    }

    private void renderMesh(
            MagoRenderableMesh mesh,
            MagoRenderContext context
    ) {
        for (MagoRenderablePrimitive primitive
                : mesh.getRenderablePrimitives()) {

            renderPrimitive(primitive, context);
        }
    }

    private void renderPrimitive(
            MagoRenderablePrimitive primitive,
            MagoRenderContext context
    ) {
        Objects.requireNonNull(
                primitive,
                "primitive must not be null"
        );

        Objects.requireNonNull(
                context,
                "context must not be null"
        );

        MagoFbo fbo = Objects.requireNonNull(
                context.getFbo(),
                "No MagoFbo configured."
        );

        MagoShaderProgram shaderProgram =
                Objects.requireNonNull(
                        context.getShaderProgram(),
                        "No MagoShaderProgram configured."
                );

        MagoVertexShader vertexShader =
                shaderProgram.getVertexShader();

        MagoFragmentShader fragmentShader =
                shaderProgram.getFragmentShader();

        boolean requiresFaceCode =
                fragmentShader.requiresFaceCode();

        if (requiresFaceCode
                && !primitive.hasFaceCodes()) {

            throw new IllegalStateException(
                    "The current fragment shader requires face codes, "
                            + "but the primitive has no face codes."
            );
        }

        MagoUniforms uniforms =
                context.getUniforms();

        uniforms.diffuseTexture =
                primitive.getDiffuseTexture();

        ByteBuffer positions =
                primitive.getPositionsBuffer().getData();

        ByteBuffer indices =
                primitive.getIndicesBuffer().getData();

        ByteBuffer normals = primitive.hasNormals()
                ? primitive.getNormalsBuffer().getData()
                : null;

        ByteBuffer texCoords = primitive.hasTexCoords()
                ? primitive.getTexCoordsBuffer().getData()
                : null;

        ByteBuffer colors = primitive.hasColors()
                ? primitive.getColorsBuffer().getData()
                : null;

        int vertexCount = primitive.getVertexCount();
        int indexCount = primitive.getIndexCount();

        /*
         * Execute the vertex shader exactly once per vertex.
         */
        MagoVertexOutput[] vertexOutputs =
                new MagoVertexOutput[vertexCount];

        ScreenVertex[] screenVertices =
                new ScreenVertex[vertexCount];

        MagoVertexInput vertexInput =
                new MagoVertexInput();

        for (int vertexIndex = 0;
             vertexIndex < vertexCount;
             vertexIndex++) {

            readVertex(
                    vertexIndex,
                    positions,
                    normals,
                    texCoords,
                    colors,
                    vertexInput
            );

            MagoVertexOutput vertexOutput =
                    new MagoVertexOutput();

            vertexShader.process(
                    vertexInput,
                    vertexOutput,
                    uniforms
            );

            vertexOutputs[vertexIndex] = vertexOutput;

            screenVertices[vertexIndex] = projectVertex(
                    vertexOutput,
                    fbo.getWidth(),
                    fbo.getHeight()
            );
        }

        /*
         * Reusable fragment objects.
         * The rasterizer is currently single-threaded.
         */
        MagoFragmentInput fragmentInput =
                new MagoFragmentInput();

        MagoFragmentOutput fragmentOutput =
                new MagoFragmentOutput();

        /*
         * Each group of three indices represents one triangle.
         */
        for (int indexPosition = 0;
             indexPosition + 2 < indexCount;
             indexPosition += 3) {

            int triangleIndex =
                    indexPosition / 3;

            int faceCode = 0;
            if (requiresFaceCode) {
                faceCode =
                        primitive.getFaceCode(
                                triangleIndex
                        );
            }

            int index0 = indices.getInt(
                    indexPosition * Integer.BYTES
            );

            int index1 = indices.getInt(
                    (indexPosition + 1) * Integer.BYTES
            );

            int index2 = indices.getInt(
                    (indexPosition + 2) * Integer.BYTES
            );

            validateVertexIndex(index0, vertexCount);
            validateVertexIndex(index1, vertexCount);
            validateVertexIndex(index2, vertexCount);

            MagoVertexOutput output0 =
                    vertexOutputs[index0];

            MagoVertexOutput output1 =
                    vertexOutputs[index1];

            MagoVertexOutput output2 =
                    vertexOutputs[index2];

            /*
             * Reject triangles completely outside one clip plane.
             */
            if (isCompletelyOutsideClipVolume(
                    output0,
                    output1,
                    output2
            )) {
                continue;
            }

            ScreenVertex vertex0 =
                    screenVertices[index0];

            ScreenVertex vertex1 =
                    screenVertices[index1];

            ScreenVertex vertex2 =
                    screenVertices[index2];

            /*
             * A null ScreenVertex means invalid coordinates or w <= 0.
             * Full homogeneous clipping will later handle these cases.
             */
            if (vertex0 == null
                    || vertex1 == null
                    || vertex2 == null) {
                continue;
            }

            float triangleArea = edgeFunction(
                    vertex0.x,
                    vertex0.y,
                    vertex1.x,
                    vertex1.y,
                    vertex2.x,
                    vertex2.y
            );

            if (!Float.isFinite(triangleArea)
                    || Math.abs(triangleArea) <= EPSILON) {
                continue;
            }

            if (context.isCullFaceEnabled()
                    && triangleArea <= 0.0f) {
                continue;
            }

            switch (context.getPolygonMode()) {
                case FILL -> rasterizeFilledTriangle(
                        vertex0,
                        vertex1,
                        vertex2,
                        faceCode,
                        fragmentShader,
                        fragmentInput,
                        fragmentOutput,
                        uniforms,
                        context
                );

                case LINE -> rasterizeWireTriangle(
                        vertex0,
                        vertex1,
                        vertex2,
                        context,
                        false
                );

                case FILL_AND_LINE -> {
                    rasterizeFilledTriangle(
                            vertex0,
                            vertex1,
                            vertex2,
                            faceCode,
                            fragmentShader,
                            fragmentInput,
                            fragmentOutput,
                            uniforms,
                            context
                    );

                    rasterizeWireTriangle(
                            vertex0,
                            vertex1,
                            vertex2,
                            context,
                            true
                    );
                }
            }
        }
    }

    private static void rasterizeWireTriangle(
            ScreenVertex vertex0,
            ScreenVertex vertex1,
            ScreenVertex vertex2,
            MagoRenderContext context,
            boolean overlay
    ) {
        rasterizeLine(
                vertex0,
                vertex1,
                context,
                overlay
        );

        rasterizeLine(
                vertex1,
                vertex2,
                context,
                overlay
        );

        rasterizeLine(
                vertex2,
                vertex0,
                context,
                overlay
        );
    }

    private static void rasterizeLine(
            ScreenVertex start,
            ScreenVertex end,
            MagoRenderContext context,
            boolean overlay
    ) {
        MagoFbo fbo = context.getFbo();

        int width = fbo.getWidth();
        int height = fbo.getHeight();

        int[] colorBuffer =
                fbo.getColorBuffer();

        float[] depthBuffer =
                fbo.getDepthBuffer();

        float deltaX =
                end.x - start.x;

        float deltaY =
                end.y - start.y;

        int steps = (int) Math.ceil(
                Math.max(
                        Math.abs(deltaX),
                        Math.abs(deltaY)
                )
        );

        if (steps <= 0) {
//            plotWirePixel(
//                    Math.round(start.x),
//                    Math.round(start.y),
//                    start.depth,
//                    context,
//                    overlay
//            );
            int x = Math.round(start.x);
            int y = Math.round(start.y);

            if (x >= 0 && x < width
                    && y >= 0 && y < height) {

                int index = y * width + x;
                colorBuffer[index] =
                        context.getWireframeColor();
            }

            return;
        }

        float inverseSteps =
                1.0f / steps;

        for (int step = 0; step <= steps; step++) {
            float factor =
                    step * inverseSteps;

            float screenX =
                    start.x + deltaX * factor;

            float screenY =
                    start.y + deltaY * factor;

            float depth =
                    start.depth
                            + (end.depth - start.depth)
                            * factor;

            int x =
                    Math.round(screenX);

            int y =
                    Math.round(screenY);

            if (x < 0 || x >= width
                    || y < 0 || y >= height) {

                continue;
            }

            int pixelIndex =
                    y * width + x;

            float biasedDepth =
                    depth + context.getWireframeDepthBias();

            if (context.isDepthTestEnabled()) {
                if (overlay) {
                    /*
                     * The filled triangle has already written its depth.
                     * Allow almost-equal values.
                     */
                    if (biasedDepth
                            > depthBuffer[pixelIndex] + 0.0001f) {

                        continue;
                    }
                } else {
                    /*
                     * Equivalent to GL_LESS for line-only mode.
                     */
                    if (!(biasedDepth
                            < depthBuffer[pixelIndex])) {

                        continue;
                    }

                    depthBuffer[pixelIndex] =
                            biasedDepth;
                }
            }

            colorBuffer[pixelIndex] =
                    context.getWireframeColor();
        }
    }

    private static boolean isCompletelyOutsideClipVolume(
            MagoVertexOutput vertex0,
            MagoVertexOutput vertex1,
            MagoVertexOutput vertex2
    ) {
        Vector4f a = vertex0.clipPosition;
        Vector4f b = vertex1.clipPosition;
        Vector4f c = vertex2.clipPosition;

        /*
         * Left: x >= -w
         */
        if (a.x < -a.w
                && b.x < -b.w
                && c.x < -c.w) {

            return true;
        }

        /*
         * Right: x <= w
         */
        if (a.x > a.w
                && b.x > b.w
                && c.x > c.w) {

            return true;
        }

        /*
         * Bottom: y >= -w
         */
        if (a.y < -a.w
                && b.y < -b.w
                && c.y < -c.w) {

            return true;
        }

        /*
         * Top: y <= w
         */
        if (a.y > a.w
                && b.y > b.w
                && c.y > c.w) {

            return true;
        }

        /*
         * Near: z >= -w
         */
        if (a.z < -a.w
                && b.z < -b.w
                && c.z < -c.w) {

            return true;
        }

        /*
         * Far: z <= w
         */
        return a.z > a.w
                && b.z > b.w
                && c.z > c.w;
    }

    private static void rasterizeFilledTriangle(
            ScreenVertex vertex0,
            ScreenVertex vertex1,
            ScreenVertex vertex2,
            int faceCode,
            MagoFragmentShader fragmentShader,
            MagoFragmentInput fragmentInput,
            MagoFragmentOutput fragmentOutput,
            MagoUniforms uniforms,
            MagoRenderContext context
    ) {
        MagoFbo fbo =
                context.getFbo();

        int width =
                fbo.getWidth();

        int height =
                fbo.getHeight();

        float area = edgeFunction(
                vertex0.x,
                vertex0.y,
                vertex1.x,
                vertex1.y,
                vertex2.x,
                vertex2.y
        );

        if (!Float.isFinite(area)
                || Math.abs(area) <= EPSILON) {

            return;
        }

        /*
         * With a bottom-left origin, positive area means CCW.
         * CCW is considered the front face.
         */
        if (context.isCullFaceEnabled()
                && area <= 0.0f) {

            return;
        }

        float minXValue = Math.min(
                vertex0.x,
                Math.min(vertex1.x, vertex2.x)
        );

        float maxXValue = Math.max(
                vertex0.x,
                Math.max(vertex1.x, vertex2.x)
        );

        float minYValue = Math.min(
                vertex0.y,
                Math.min(vertex1.y, vertex2.y)
        );

        float maxYValue = Math.max(
                vertex0.y,
                Math.max(vertex1.y, vertex2.y)
        );

        int minX = Math.max(
                0,
                (int) Math.floor(minXValue)
        );

        int maxX = Math.min(
                width - 1,
                (int) Math.ceil(maxXValue)
        );

        int minY = Math.max(
                0,
                (int) Math.floor(minYValue)
        );

        int maxY = Math.min(
                height - 1,
                (int) Math.ceil(maxYValue)
        );

        if (minX > maxX || minY > maxY) {
            return;
        }

        int[] colorBuffer =
                fbo.getColorBuffer();

        float[] depthBuffer =
                fbo.getDepthBuffer();

        float inverseArea =
                1.0f / area;

        /*
         * Flat triangle attribute.
         * Constant for every generated fragment.
         */
        fragmentInput.faceCode =
                faceCode;

        for (int y = minY; y <= maxY; y++) {
            float pixelY =
                    y + 0.5f;

            int rowOffset =
                    y * width;

            for (int x = minX; x <= maxX; x++) {
                float pixelX =
                        x + 0.5f;

                float barycentric0 =
                        edgeFunction(
                                vertex1.x,
                                vertex1.y,
                                vertex2.x,
                                vertex2.y,
                                pixelX,
                                pixelY
                        ) * inverseArea;

                float barycentric1 =
                        edgeFunction(
                                vertex2.x,
                                vertex2.y,
                                vertex0.x,
                                vertex0.y,
                                pixelX,
                                pixelY
                        ) * inverseArea;

                float barycentric2 =
                        1.0f
                                - barycentric0
                                - barycentric1;

                if (barycentric0 < -EPSILON
                        || barycentric1 < -EPSILON
                        || barycentric2 < -EPSILON) {

                    continue;
                }

                /*
                 * Window-space depth is interpolated linearly.
                 */
                float depth =
                        barycentric0 * vertex0.depth
                                + barycentric1 * vertex1.depth
                                + barycentric2 * vertex2.depth;

                /*
                 * Fragments outside OpenGL depth range are discarded.
                 */
                if (!(depth >= 0.0f && depth <= 1.0f)) {
                    continue;
                }

                int pixelIndex =
                        rowOffset + x;

                /*
                 * Equivalent to GL_LESS.
                 */
                if (context.isDepthTestEnabled()
                        && !(depth < depthBuffer[pixelIndex])) {

                    continue;
                }

                float corrected0 =
                        barycentric0 * vertex0.inverseW;

                float corrected1 =
                        barycentric1 * vertex1.inverseW;

                float corrected2 =
                        barycentric2 * vertex2.inverseW;

                float denominator =
                        corrected0
                                + corrected1
                                + corrected2;

                if (!(denominator > EPSILON)
                        || !Float.isFinite(denominator)) {

                    continue;
                }

                float inverseDenominator =
                        1.0f / denominator;

                corrected0 *= inverseDenominator;
                corrected1 *= inverseDenominator;
                corrected2 *= inverseDenominator;

                interpolateFragmentInput(
                        fragmentInput,
                        vertex0,
                        vertex1,
                        vertex2,
                        corrected0,
                        corrected1,
                        corrected2,
                        x,
                        y,
                        depth
                );

                /*
                 * Reset output because the same object is reused.
                 */
                fragmentOutput.discard = false;
                fragmentOutput.color.zero();

                fragmentShader.process(
                        fragmentInput,
                        fragmentOutput,
                        uniforms
                );

                if (fragmentOutput.discard) {
                    continue;
                }

                int sourceColor =
                        toArgb(fragmentOutput.color);

                if (context.isBlendEnabled()) {
                    colorBuffer[pixelIndex] =
                            blendSourceOver(
                                    sourceColor,
                                    colorBuffer[pixelIndex],
                                    context.isSeparateAlphaBlend()
                            );
                } else {
                    colorBuffer[pixelIndex] =
                            sourceColor;
                }

                if (context.isDepthTestEnabled()) {
                    depthBuffer[pixelIndex] =
                            depth;
                }
            }
        }
    }

    private static void interpolateFragmentInput(
            MagoFragmentInput result,
            ScreenVertex vertex0,
            ScreenVertex vertex1,
            ScreenVertex vertex2,
            float weight0,
            float weight1,
            float weight2,
            int x,
            int y,
            float depth
    ) {
        MagoVertexOutput output0 =
                vertex0.output;

        MagoVertexOutput output1 =
                vertex1.output;

        MagoVertexOutput output2 =
                vertex2.output;

        result.x = x;
        result.y = y;
        result.depth = depth;

        result.worldPosition.set(
                output0.worldPosition.x * weight0
                        + output1.worldPosition.x * weight1
                        + output2.worldPosition.x * weight2,

                output0.worldPosition.y * weight0
                        + output1.worldPosition.y * weight1
                        + output2.worldPosition.y * weight2,

                output0.worldPosition.z * weight0
                        + output1.worldPosition.z * weight1
                        + output2.worldPosition.z * weight2
        );

        result.normal.set(
                output0.normal.x * weight0
                        + output1.normal.x * weight1
                        + output2.normal.x * weight2,

                output0.normal.y * weight0
                        + output1.normal.y * weight1
                        + output2.normal.y * weight2,

                output0.normal.z * weight0
                        + output1.normal.z * weight1
                        + output2.normal.z * weight2
        );

        if (result.normal.lengthSquared() > EPSILON) {
            result.normal.normalize();
        }

        result.texCoord.set(
                output0.texCoord.x * weight0
                        + output1.texCoord.x * weight1
                        + output2.texCoord.x * weight2,

                output0.texCoord.y * weight0
                        + output1.texCoord.y * weight1
                        + output2.texCoord.y * weight2
        );

        result.color.set(
                output0.color.x * weight0
                        + output1.color.x * weight1
                        + output2.color.x * weight2,

                output0.color.y * weight0
                        + output1.color.y * weight1
                        + output2.color.y * weight2,

                output0.color.z * weight0
                        + output1.color.z * weight1
                        + output2.color.z * weight2,

                output0.color.w * weight0
                        + output1.color.w * weight1
                        + output2.color.w * weight2
        );
    }

    private static float edgeFunction(
            float ax,
            float ay,
            float bx,
            float by,
            float px,
            float py
    ) {
        return (bx - ax) * (py - ay)
                - (by - ay) * (px - ax);
    }

    private static int toArgb(Vector4f color) {
        int red =
                floatToByte(color.x);

        int green =
                floatToByte(color.y);

        int blue =
                floatToByte(color.z);

        int alpha =
                floatToByte(color.w);

        return (alpha << 24)
                | (red << 16)
                | (green << 8)
                | blue;
    }

    private static int floatToByte(float value) {
        if (!Float.isFinite(value)) {
            return 0;
        }

        float clamped =
                Math.max(0.0f, Math.min(1.0f, value));

        return Math.round(
                clamped * 255.0f
        );
    }

    private static int blendSourceOver(
            int source,
            int destination,
            boolean separateAlphaBlend
    ) {
        int sourceAlpha =
                (source >>> 24) & 0xFF;

        if (sourceAlpha == 255) {
            return source;
        }

        if (sourceAlpha == 0) {
            return destination;
        }

        int destinationAlpha =
                (destination >>> 24) & 0xFF;

        int sourceRed =
                (source >>> 16) & 0xFF;

        int sourceGreen =
                (source >>> 8) & 0xFF;

        int sourceBlue =
                source & 0xFF;

        int destinationRed =
                (destination >>> 16) & 0xFF;

        int destinationGreen =
                (destination >>> 8) & 0xFF;

        int destinationBlue =
                destination & 0xFF;

        int inverseSourceAlpha =
                255 - sourceAlpha;

        int outputRed =
                (
                        sourceRed * sourceAlpha
                                + destinationRed
                                * inverseSourceAlpha
                                + 127
                ) / 255;

        int outputGreen =
                (
                        sourceGreen * sourceAlpha
                                + destinationGreen
                                * inverseSourceAlpha
                                + 127
                ) / 255;

        int outputBlue =
                (
                        sourceBlue * sourceAlpha
                                + destinationBlue
                                * inverseSourceAlpha
                                + 127
                ) / 255;

        /*
         * Same blend factors applied to alpha:
         * srcA * srcA + dstA * (1 - srcA).
         */
        int outputAlpha = separateAlphaBlend
                ? sourceAlpha + (
                destinationAlpha * inverseSourceAlpha + 127
        ) / 255
                : (
                sourceAlpha * sourceAlpha
                + destinationAlpha * inverseSourceAlpha
                + 127
        ) / 255;

        return (outputAlpha << 24)
                | (outputRed << 16)
                | (outputGreen << 8)
                | outputBlue;
    }

    private static void validateVertexIndex(
            int index,
            int vertexCount
    ) {
        if (index < 0 || index >= vertexCount) {
            throw new IndexOutOfBoundsException(
                    "Invalid primitive vertex index: "
                            + index
                            + ", vertexCount="
                            + vertexCount
            );
        }
    }

    private static void readVertex(
            int vertexIndex,
            ByteBuffer positions,
            ByteBuffer normals,
            ByteBuffer texCoords,
            ByteBuffer colors,
            MagoVertexInput result
    ) {
        int positionOffset =
                vertexIndex * 3 * Float.BYTES;

        result.position.set(
                positions.getFloat(positionOffset),
                positions.getFloat(
                        positionOffset + Float.BYTES
                ),
                positions.getFloat(
                        positionOffset + 2 * Float.BYTES
                )
        );

        if (normals != null) {
            int normalOffset =
                    vertexIndex * 3 * Float.BYTES;

            result.normal.set(
                    normals.getFloat(normalOffset),
                    normals.getFloat(
                            normalOffset + Float.BYTES
                    ),
                    normals.getFloat(
                            normalOffset + 2 * Float.BYTES
                    )
            );
        } else {
            result.normal.set(
                    0.0f,
                    0.0f,
                    1.0f
            );
        }

        if (texCoords != null) {
            int texCoordOffset =
                    vertexIndex * 2 * Float.BYTES;

            result.texCoord.set(
                    texCoords.getFloat(texCoordOffset),
                    texCoords.getFloat(
                            texCoordOffset + Float.BYTES
                    )
            );
        } else {
            result.texCoord.zero();
        }

        if (colors != null) {
            int colorOffset = vertexIndex * 4;

            float red =
                    Byte.toUnsignedInt(
                            colors.get(colorOffset)
                    ) / 255.0f;

            float green =
                    Byte.toUnsignedInt(
                            colors.get(colorOffset + 1)
                    ) / 255.0f;

            float blue =
                    Byte.toUnsignedInt(
                            colors.get(colorOffset + 2)
                    ) / 255.0f;

            float alpha =
                    Byte.toUnsignedInt(
                            colors.get(colorOffset + 3)
                    ) / 255.0f;

            result.color.set(
                    red,
                    green,
                    blue,
                    alpha
            );
        } else {
            /*
             * White by default.
             */
            result.color.set(
                    1.0f,
                    1.0f,
                    1.0f,
                    1.0f
            );
        }
    }

    private static ScreenVertex projectVertex(
            MagoVertexOutput output,
            int width,
            int height
    ) {
        Vector4f clip =
                output.clipPosition;

        if (!isFinite(clip)) {
            return null;
        }

        if (!(clip.w > EPSILON)) {
            return null;
        }

        float inverseW =
                1.0f / clip.w;

        float ndcX =
                clip.x * inverseW;

        float ndcY =
                clip.y * inverseW;

        float ndcZ =
                clip.z * inverseW;

        if (!Float.isFinite(ndcX)
                || !Float.isFinite(ndcY)
                || !Float.isFinite(ndcZ)) {

            return null;
        }

        /*
         * Viewport coordinates.
         *
         * Pixel centers are:
         *
         *   0.5, 1.5, ..., width - 0.5
         *
         * Therefore NDC [-1, 1] maps to [0, width],
         * not to [0, width - 1].
         */
        float screenX =
                (ndcX * 0.5f + 0.5f)
                        * width;

        float screenY =
                (ndcY * 0.5f + 0.5f)
                        * height;

        float depth =
                ndcZ * 0.5f + 0.5f;

        return new ScreenVertex(
                output,
                screenX,
                screenY,
                depth,
                inverseW
        );
    }

    private static boolean isFinite(Vector4f value) {
        return Float.isFinite(value.x)
                && Float.isFinite(value.y)
                && Float.isFinite(value.z)
                && Float.isFinite(value.w);
    }

    private void prepareNodeUniforms(
            MagoRenderableNode node,
            MagoRenderContext context
    ) {
        var uniforms = context.getUniforms();

        uniforms.modelMatrix.set(
                node.getPreMultipliedTransformMatrix()
        );

        uniforms.viewMatrix.set(
                context.getViewMatrix()
        );

        uniforms.projectionMatrix.set(
                context.getProjectionMatrix()
        );

        uniforms.modelViewProjectionMatrix
                .set(uniforms.projectionMatrix)
                .mul(uniforms.viewMatrix)
                .mul(uniforms.modelMatrix);

        uniforms.modelMatrix.normal(
                uniforms.normalMatrix
        );
    }
}
