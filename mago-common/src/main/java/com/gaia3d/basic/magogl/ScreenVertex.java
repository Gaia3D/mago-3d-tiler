package com.gaia3d.basic.magogl;

import com.gaia3d.basic.magogl.shader.MagoVertexOutput;

public final class ScreenVertex {

    final MagoVertexOutput output;

    final float x;
    final float y;
    final float depth;
    final float inverseW;

    ScreenVertex(
            MagoVertexOutput output,
            float x,
            float y,
            float depth,
            float inverseW
    ) {
        this.output = output;
        this.x = x;
        this.y = y;
        this.depth = depth;
        this.inverseW = inverseW;
    }
}
