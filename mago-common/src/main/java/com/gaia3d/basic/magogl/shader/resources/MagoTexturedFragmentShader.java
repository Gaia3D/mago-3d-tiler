package com.gaia3d.basic.magogl.shader.resources;

import com.gaia3d.basic.magogl.shader.MagoFragmentInput;
import com.gaia3d.basic.magogl.shader.MagoFragmentOutput;
import com.gaia3d.basic.magogl.shader.MagoFragmentShader;
import com.gaia3d.basic.magogl.shader.MagoUniforms;
import com.gaia3d.basic.magogl.texture.MagoTextureFilter;

public final class MagoTexturedFragmentShader
        implements MagoFragmentShader {

    @Override
    public void process(
            MagoFragmentInput input,
            MagoFragmentOutput output,
            MagoUniforms uniforms
    ) {
        output.discard = false;

        if (uniforms.diffuseTexture == null) {
            output.color.set(input.color);
            return;
        }

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

        /*
         * textura * color de vértice
         */
        output.color.mul(input.color);

        if (output.color.w < uniforms.alphaCutoff) {
            output.discard = true;
        } else if (output.color.w < uniforms.minimumAlpha) {
            output.color.w = uniforms.minimumAlpha;
        }
    }
}
