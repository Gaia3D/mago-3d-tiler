package com.gaia3d.basic.magogl.backend;

import com.gaia3d.basic.magogl.MagoFbo;
import com.gaia3d.basic.magogl.MagoRenderContext;
import com.gaia3d.basic.magogl.renderable.MagoRenderableScene;

public interface MagoRenderingSession extends AutoCloseable {

    void renderIntoFbo(MagoRenderableScene scene, MagoRenderContext context);

    /**
     * Copies backend-owned render targets into their corresponding CPU FBOs.
     * Software sessions already render into CPU FBOs and therefore do nothing.
     */
    default void readback() {
    }

    /** Resets a backend target from the current CPU FBO clear values. */
    default void reset(MagoFbo fbo) {
    }

    /**
     * Returns the backend result for comparison without replacing the CPU FBO.
     */
    default MagoFbo getComparisonFbo(MagoFbo fbo) {
        return null;
    }

    @Override
    void close();
}
