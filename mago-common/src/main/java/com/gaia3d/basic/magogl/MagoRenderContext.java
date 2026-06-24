package com.gaia3d.basic.magogl;

import com.gaia3d.basic.magogl.shader.MagoUniforms;
import com.gaia3d.basic.magogl.shader.program.MagoShaderProgram;
import lombok.Getter;
import lombok.Setter;
import org.joml.Matrix4d;
import org.joml.Matrix4f;

@Getter
@Setter
public final class MagoRenderContext {

    private MagoFbo fbo;
    private MagoShaderProgram shaderProgram;

    private final Matrix4d viewMatrix =
            new Matrix4d();

    private final Matrix4d projectionMatrix =
            new Matrix4d();

    private final MagoUniforms uniforms =
            new MagoUniforms();

    private boolean depthTestEnabled = true;
    private boolean cullFaceEnabled = true;
    private boolean blendEnabled = false;
    private boolean separateAlphaBlend = false;

    private MagoPolygonMode polygonMode =
            MagoPolygonMode.FILL;

    private int wireframeColor =
            0xFF000000;

    private float wireframeDepthBias =
            -0.00001f;

    public void setViewMatrix(Matrix4d matrix) {
        viewMatrix.set(matrix);
    }

    public void setProjectionMatrix(Matrix4f matrix) {
        projectionMatrix.set(matrix);
    }
}
