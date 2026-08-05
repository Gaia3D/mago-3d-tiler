package com.gaia3d.basic.magogl.shader.program;

import com.gaia3d.basic.magogl.shader.MagoFragmentShader;
import com.gaia3d.basic.magogl.shader.MagoVertexShader;
import lombok.Getter;

import java.util.Objects;

@Getter
public final class MagoShaderProgram {

    private final String name;
    private final MagoVertexShader vertexShader;
    private final MagoFragmentShader fragmentShader;

    public MagoShaderProgram(
            String name,
            MagoVertexShader vertexShader,
            MagoFragmentShader fragmentShader
    ) {
        this.name = Objects.requireNonNullElse(
                name,
                "unnamed"
        );

        this.vertexShader = Objects.requireNonNull(
                vertexShader,
                "vertexShader must not be null"
        );

        this.fragmentShader = Objects.requireNonNull(
                fragmentShader,
                "fragmentShader must not be null"
        );
    }
}
