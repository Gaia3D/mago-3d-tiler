package com.gaia3d.basic.geometry.modifier.topology;

import com.gaia3d.basic.geometry.modifier.Modifier;
import com.gaia3d.basic.model.GaiaVertex;
import org.joml.Matrix4d;

public class GaiaColorCleaner extends Modifier {

    @Override
    protected void applyVertex(Matrix4d productTransformMatrix, GaiaVertex vertex) {
        vertex.setColor(null);
    }
}
