package com.gaia3d.basic.magogl.backend;

import com.gaia3d.basic.magogl.MagoRenderEngine;
import com.gaia3d.basic.magogl.MagoRenderContext;
import com.gaia3d.basic.magogl.renderable.MagoRenderableScene;

public final class SoftwareRenderingBackend implements MagoRenderingBackend {

    @Override
    public MagoRenderingSession openSession() {
        return new Session();
    }

    private static final class Session implements MagoRenderingSession {
        private final MagoRenderEngine engine = new MagoRenderEngine();

        @Override
        public void renderIntoFbo(
                MagoRenderableScene scene,
                MagoRenderContext context
        ) {
            engine.renderIntoFbo(scene, context);
        }

        @Override
        public void close() {
            // CPU render targets are owned by the caller.
        }
    }
}
