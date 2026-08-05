package com.gaia3d.basic.magogl.backend;

import com.gaia3d.basic.magogl.MagoFbo;
import com.gaia3d.basic.magogl.MagoRenderContext;
import com.gaia3d.basic.magogl.renderable.MagoRenderableScene;
import lombok.extern.slf4j.Slf4j;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Renders with the candidate backend first and software second. The software
 * result remains canonical while the candidate result is retained for parity
 * diagnostics.
 */
public final class ComparingRenderingBackend implements MagoRenderingBackend {
    private final MagoRenderingBackend candidate;

    public ComparingRenderingBackend(MagoRenderingBackend candidate) {
        this.candidate = Objects.requireNonNull(candidate);
    }

    @Override
    public MagoRenderingSession openSession() {
        return new Session(
                candidate.openSession(),
                new SoftwareRenderingBackend().openSession()
        );
    }

    @Slf4j
    private static final class Session implements MagoRenderingSession {
        private final MagoRenderingSession candidate;
        private final MagoRenderingSession software;
        private final Map<MagoFbo, MagoFbo> candidateFbos =
                new IdentityHashMap<>();
        private long candidateRenderNanos;
        private long softwareRenderNanos;
        private long candidateReadbackNanos;
        private int submissions;

        private Session(
                MagoRenderingSession candidate,
                MagoRenderingSession software
        ) {
            this.candidate = candidate;
            this.software = software;
        }

        private static MagoFbo copyOf(MagoFbo source, String prefix) {
            MagoFbo copy = new MagoFbo(
                    prefix + source.getName(),
                    source.getWidth(),
                    source.getHeight()
            );
            System.arraycopy(
                    source.getColorBuffer(), 0,
                    copy.getColorBuffer(), 0,
                    source.getColorBuffer().length
            );
            System.arraycopy(
                    source.getDepthBuffer(), 0,
                    copy.getDepthBuffer(), 0,
                    source.getDepthBuffer().length
            );
            return copy;
        }

        private static String nanosToMillis(long nanos) {
            return String.format(java.util.Locale.ROOT, "%.3f", nanos / 1_000_000.0);
        }

        @Override
        public void renderIntoFbo(
                MagoRenderableScene scene,
                MagoRenderContext context
        ) {
            MagoFbo softwareFbo = Objects.requireNonNull(context.getFbo());
            MagoFbo candidateFbo = candidateFbos.computeIfAbsent(
                    softwareFbo,
                    key -> copyOf(key, "compare-")
            );

            context.setFbo(candidateFbo);
            long started = System.nanoTime();
            candidate.renderIntoFbo(scene, context);
            candidateRenderNanos += System.nanoTime() - started;
            context.setFbo(softwareFbo);
            started = System.nanoTime();
            software.renderIntoFbo(scene, context);
            softwareRenderNanos += System.nanoTime() - started;
            submissions++;
        }

        @Override
        public void readback() {
            long started = System.nanoTime();
            candidate.readback();
            candidateReadbackNanos += System.nanoTime() - started;
            log.info(
                    "Render comparison timing: submissions={}, candidateRenderMs={}, softwareRenderMs={}, candidateReadbackMs={}",
                    submissions,
                    nanosToMillis(candidateRenderNanos),
                    nanosToMillis(softwareRenderNanos),
                    nanosToMillis(candidateReadbackNanos)
            );
        }

        @Override
        public void reset(MagoFbo fbo) {
            MagoFbo candidateFbo = candidateFbos.computeIfAbsent(
                    fbo,
                    key -> copyOf(key, "compare-")
            );
            System.arraycopy(
                    fbo.getColorBuffer(), 0,
                    candidateFbo.getColorBuffer(), 0,
                    fbo.getColorBuffer().length
            );
            System.arraycopy(
                    fbo.getDepthBuffer(), 0,
                    candidateFbo.getDepthBuffer(), 0,
                    fbo.getDepthBuffer().length
            );
            candidate.reset(candidateFbo);
            software.reset(fbo);
        }

        @Override
        public MagoFbo getComparisonFbo(MagoFbo fbo) {
            return candidateFbos.get(fbo);
        }

        @Override
        public void close() {
            try {
                candidate.close();
            } finally {
                software.close();
            }
        }
    }
}
