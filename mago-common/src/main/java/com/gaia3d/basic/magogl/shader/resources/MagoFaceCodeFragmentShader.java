package com.gaia3d.basic.magogl.shader.resources;

import com.gaia3d.basic.magogl.shader.MagoFragmentInput;
import com.gaia3d.basic.magogl.shader.MagoFragmentOutput;
import com.gaia3d.basic.magogl.shader.MagoFragmentShader;
import com.gaia3d.basic.magogl.shader.MagoUniforms;

public final class MagoFaceCodeFragmentShader
        implements MagoFragmentShader {

    private static final float BYTE_TO_FLOAT =
            1.0f / 255.0f;

    @Override
    public boolean requiresFaceCode() {
        return true;
    }

    @Override
    public void process(
            MagoFragmentInput input,
            MagoFragmentOutput output,
            MagoUniforms uniforms
    ) {
        int code = input.faceCode;

        int alpha =
                (code >>> 24) & 0xFF;

        int red =
                (code >>> 16) & 0xFF;

        int green =
                (code >>> 8) & 0xFF;

        int blue =
                code & 0xFF;

        output.color.set(
                red * BYTE_TO_FLOAT,
                green * BYTE_TO_FLOAT,
                blue * BYTE_TO_FLOAT,
                alpha * BYTE_TO_FLOAT
        );

        output.discard = false;
    }
}