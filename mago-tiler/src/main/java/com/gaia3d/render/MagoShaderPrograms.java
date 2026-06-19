package com.gaia3d.render;

import com.gaia3d.basic.magogl.shader.program.MagoShaderProgram;
import com.gaia3d.basic.magogl.shader.resources.MagoDefaultVertexShader;
import com.gaia3d.basic.magogl.shader.resources.MagoFaceCodeFragmentShader;
import com.gaia3d.basic.magogl.shader.resources.MagoTexturedFragmentShader;

public final class MagoShaderPrograms {

    public static final MagoShaderProgram TEXTURED =
            new MagoShaderProgram(
                    "textured",
                    new MagoDefaultVertexShader(),
                    new MagoTexturedFragmentShader()
            );

    public static final MagoShaderProgram FACE_CODE =
            new MagoShaderProgram(
                    "face-code",
                    new MagoDefaultVertexShader(),
                    new MagoFaceCodeFragmentShader()
            );

    private MagoShaderPrograms() {
    }
}
