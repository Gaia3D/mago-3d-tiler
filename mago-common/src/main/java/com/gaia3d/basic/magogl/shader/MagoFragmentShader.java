package com.gaia3d.basic.magogl.shader;

public interface MagoFragmentShader {

    default boolean requiresFaceCode() {
        return false;
    }

    void process(
            MagoFragmentInput input,
            MagoFragmentOutput output,
            MagoUniforms uniforms
    );
}