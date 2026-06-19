package com.gaia3d.basic.magogl.shader.resources;

import com.gaia3d.basic.magogl.shader.MagoUniforms;
import com.gaia3d.basic.magogl.shader.MagoVertexInput;
import com.gaia3d.basic.magogl.shader.MagoVertexOutput;
import com.gaia3d.basic.magogl.shader.MagoVertexShader;

public final class MagoDefaultVertexShader
        implements MagoVertexShader {

    private static final float NORMAL_EPSILON = 1e-20f;

    @Override
    public void process(
            MagoVertexInput input,
            MagoVertexOutput output,
            MagoUniforms uniforms
    ) {
        // Equivalent to gl_Position.
        output.clipPosition.set(
                input.position.x,
                input.position.y,
                input.position.z,
                1.0f
        );

        uniforms.modelViewProjectionMatrix.transform(
                output.clipPosition
        );

        // Position in world coordinates.
        output.worldPosition.set(input.position);

        uniforms.modelMatrix.transformPosition(
                output.worldPosition
        );

        // Normal in world coordinates.
        output.normal.set(input.normal);
        uniforms.normalMatrix.transform(output.normal);

        if (output.normal.lengthSquared() > NORMAL_EPSILON) {
            output.normal.normalize();
        } else {
            output.normal.set(0.0f, 0.0f, 1.0f);
        }

        // Varyings.
        output.texCoord.set(input.texCoord);
        output.color.set(input.color);
    }
}
