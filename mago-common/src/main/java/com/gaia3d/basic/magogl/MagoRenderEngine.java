package com.gaia3d.basic.magogl;

import com.gaia3d.basic.magogl.renderable.MagoRenderableScene;
import org.joml.Vector4f;

import java.util.Objects;

public final class MagoRenderEngine {

    private final MagoRenderer renderer = new MagoRenderer();

    public void renderIntoFbo(
            MagoRenderableScene scene,
            MagoRenderContext context
    ) {
        Objects.requireNonNull(
                scene,
                "scene must not be null"
        );

        Objects.requireNonNull(
                context,
                "context must not be null"
        );

        MagoFbo fbo = Objects.requireNonNull(
                context.getFbo(),
                "MagoRenderContext has no MagoFbo"
        );

        Objects.requireNonNull(
                context.getShaderProgram(),
                "MagoRenderContext has no shader program"
        );

        renderer.renderScene(
                scene,
                context
        );
    }

    public static int toArgb(Vector4f color) {
        int red = floatToByte(color.x);
        int green = floatToByte(color.y);
        int blue = floatToByte(color.z);
        int alpha = floatToByte(color.w);

        return (alpha << 24)
                | (red << 16)
                | (green << 8)
                | blue;
    }

    private static int floatToByte(float value) {
        if (!Float.isFinite(value)) {
            return 0;
        }

        float clamped = Math.max(
                0.0f,
                Math.min(1.0f, value)
        );

        return Math.round(clamped * 255.0f);
    }
}