package com.gaia3d.render;

import com.gaia3d.basic.halfedge.CameraDirectionType;
import com.gaia3d.basic.magogl.MagoFbo;

import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class MagoFboSet {

    private final Map<CameraDirectionType, MagoFbo> fbos = new EnumMap<>(CameraDirectionType.class);

    public void create(CameraDirectionType direction, int width, int height) {
        Objects.requireNonNull(direction, "direction must not be null");

        MagoFbo previous = fbos.put(direction, new MagoFbo("MagoFbo-" + direction.name(), width, height));

        if (previous != null) {
            previous.cleanup();
        }
    }

    public MagoFbo get(CameraDirectionType direction) {
        Objects.requireNonNull(direction, "direction must not be null");

        MagoFbo fbo = fbos.get(direction);

        if (fbo == null) {
            throw new IllegalStateException("No MagoFbo exists for direction: " + direction);
        }

        return fbo;
    }

    public boolean contains(CameraDirectionType direction) {
        Objects.requireNonNull(direction, "direction must not be null");

        return fbos.containsKey(direction);
    }

    public void resizeAll(int width, int height) {
        for (MagoFbo fbo : fbos.values()) {
            fbo.resize(width, height);
        }
    }

    public void clearAll(int argbColor, float depth) {
        for (MagoFbo fbo : fbos.values()) {
            fbo.clear(argbColor, depth);
        }
    }

    public void cleanup() {
        for (MagoFbo fbo : fbos.values()) {
            fbo.cleanup();
        }

        fbos.clear();
    }

    public int size() {
        return fbos.size();
    }

    public MagoFboSet createCompatible(int clearColor, float clearDepth) {
        MagoFboSet result = new MagoFboSet();

        for (Map.Entry<CameraDirectionType, MagoFbo> entry : fbos.entrySet()) {

            CameraDirectionType direction = entry.getKey();

            MagoFbo source = entry.getValue();

            result.create(direction, source.getWidth(), source.getHeight());
        }

        result.clearAll(clearColor, clearDepth);

        return result;
    }
}
