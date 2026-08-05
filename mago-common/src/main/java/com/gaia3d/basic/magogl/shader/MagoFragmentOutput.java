package com.gaia3d.basic.magogl.shader;

import org.joml.Vector4f;

public final class MagoFragmentOutput {

    public final Vector4f color = new Vector4f();

    /**
     * Equivalent to GLSL discard.
     */
    public boolean discard;
}