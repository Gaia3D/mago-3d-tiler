package com.gaia3d.basic.magogl.shader.resources;

import com.gaia3d.basic.magogl.shader.MagoFragmentInput;
import com.gaia3d.basic.magogl.shader.MagoFragmentOutput;
import com.gaia3d.basic.magogl.shader.MagoFragmentShader;
import com.gaia3d.basic.magogl.shader.MagoUniforms;

public final class MagoFlatFragmentShader
        implements MagoFragmentShader {

    @Override
    public void process(
            MagoFragmentInput input,
            MagoFragmentOutput output,
            MagoUniforms uniforms
    ) {
        output.discard = false;
        output.color.set(input.color);
    }
}