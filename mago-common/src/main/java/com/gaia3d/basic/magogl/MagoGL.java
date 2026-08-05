package com.gaia3d.basic.magogl;

import java.util.Objects;

public final class MagoGL {

    private MagoFbo boundFramebuffer;

    public void bindFramebuffer(MagoFbo framebuffer) {
        this.boundFramebuffer = Objects.requireNonNull(framebuffer);
    }

    public void unbindFramebuffer() {
        this.boundFramebuffer = null;
    }

    public MagoFbo getBoundFramebuffer() {
        if (boundFramebuffer == null) {
            throw new IllegalStateException(
                    "No MagoFbo is currently bound."
            );
        }

        return boundFramebuffer;
    }
}
