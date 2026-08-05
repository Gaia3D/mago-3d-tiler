package com.gaia3d.basic.magogl.shader;

public interface MagoVertexShader {

    void process(
            MagoVertexInput input,
            MagoVertexOutput output,
            MagoUniforms uniforms
    );
}