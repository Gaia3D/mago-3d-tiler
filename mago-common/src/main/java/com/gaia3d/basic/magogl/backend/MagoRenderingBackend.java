package com.gaia3d.basic.magogl.backend;

/**
 * Creates rendering sessions. A session owns backend-specific render targets
 * and keeps their color and depth contents across multiple scene submissions.
 */
public interface MagoRenderingBackend {

    MagoRenderingSession openSession();
}
