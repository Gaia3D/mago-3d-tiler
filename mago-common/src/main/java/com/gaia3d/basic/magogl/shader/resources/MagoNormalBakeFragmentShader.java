package com.gaia3d.basic.magogl.shader.resources;

import com.gaia3d.basic.magogl.shader.MagoFragmentInput;
import com.gaia3d.basic.magogl.shader.MagoFragmentOutput;
import com.gaia3d.basic.magogl.shader.MagoFragmentShader;
import com.gaia3d.basic.magogl.shader.MagoUniforms;
import com.gaia3d.basic.magogl.texture.MagoTextureFilter;
import org.joml.Vector4f;

/**
 * Bakes an interpolated normal into an RGB texture. The incoming tangent,
 * bitangent, and normal are expected to already be expressed in the target
 * billboard tangent basis.
 * <p>
 * When a diffuse texture is bound it is sampled only for its alpha, so cut-out
 * regions (e.g. foliage) are discarded instead of writing background normals.
 */
public final class MagoNormalBakeFragmentShader
        implements MagoFragmentShader {

    private static final float NORMAL_EPSILON = 1e-20f;

    @Override
    public void process(
            MagoFragmentInput input,
            MagoFragmentOutput output,
            MagoUniforms uniforms
    ) {
        output.discard = false;

        // Alpha masking driven by the source diffuse texture (cut-out geometry).
        if (uniforms.diffuseTexture != null) {
            if (uniforms.textureFilter
                    == MagoTextureFilter.BILINEAR) {

                uniforms.diffuseTexture.sampleBilinear(
                        input.texCoord.x,
                        input.texCoord.y,
                        uniforms.wrapS,
                        uniforms.wrapT,
                        uniforms.invertTextureV,
                        output.color
                );
            } else {
                uniforms.diffuseTexture.sampleNearest(
                        input.texCoord.x,
                        input.texCoord.y,
                        uniforms.wrapS,
                        uniforms.wrapT,
                        uniforms.invertTextureV,
                        output.color
                );
            }

            if (output.color.w < uniforms.alphaCutoff) {
                output.discard = true;
                return;
            }
        }

        float sx = 0.0f;
        float sy = 0.0f;
        float sz = 1.0f;
        if (uniforms.normalTexture != null) {
            Vector4f sampledNormal = new Vector4f();
            if (uniforms.textureFilter
                    == MagoTextureFilter.BILINEAR) {

                uniforms.normalTexture.sampleBilinear(
                        input.texCoord.x,
                        input.texCoord.y,
                        uniforms.wrapS,
                        uniforms.wrapT,
                        uniforms.invertTextureV,
                        sampledNormal
                );
            } else {
                uniforms.normalTexture.sampleNearest(
                        input.texCoord.x,
                        input.texCoord.y,
                        uniforms.wrapS,
                        uniforms.wrapT,
                        uniforms.invertTextureV,
                        sampledNormal
                );
            }

            sx = sampledNormal.x * 2.0f - 1.0f;
            sy = sampledNormal.y * 2.0f - 1.0f;
            sz = sampledNormal.z * 2.0f - 1.0f;
            if (uniforms.invertTextureV) {
                sy = -sy;
            }
            float sourceLengthSquared = sx * sx + sy * sy + sz * sz;
            if (sourceLengthSquared > NORMAL_EPSILON) {
                float inverseSourceLength = (float) (1.0 / Math.sqrt(sourceLengthSquared));
                sx *= inverseSourceLength;
                sy *= inverseSourceLength;
                sz *= inverseSourceLength;
            } else {
                sx = 0.0f;
                sy = 0.0f;
                sz = 1.0f;
            }
        }

        float nx = input.tangent.x * sx
                + input.bitangent.x * sy
                + input.normal.x * sz;
        float ny = input.tangent.y * sx
                + input.bitangent.y * sy
                + input.normal.y * sz;
        float nz = input.tangent.z * sx
                + input.bitangent.z * sy
                + input.normal.z * sz;

        float lengthSquared = nx * nx + ny * ny + nz * nz;
        if (lengthSquared > NORMAL_EPSILON) {
            float inverseLength = (float) (1.0 / Math.sqrt(lengthSquared));
            nx *= inverseLength;
            ny *= inverseLength;
            nz *= inverseLength;
        } else {
            nx = 0.0f;
            ny = 0.0f;
            nz = 1.0f;
        }

        // glTF/Cesium normal maps expect the green channel in the opposite
        // vertical convention from the billboard bake target.
        ny = -ny;

        output.color.set(
                nx * 0.5f + 0.5f,
                ny * 0.5f + 0.5f,
                nz * 0.5f + 0.5f,
                1.0f
        );
    }
}
