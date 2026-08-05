package com.gaia3d.basic.geometry.modifier.transform;

import com.gaia3d.basic.geometry.modifier.Modifier;
import com.gaia3d.basic.model.GaiaVertex;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

@Slf4j
public class GaiaScaler extends Modifier {

    private final GaiaScalerOptions options;

    public GaiaScaler() {
        super();
        this.options = GaiaScalerOptions.builder().build();
    }

    public GaiaScaler(GaiaScalerOptions settings) {
        super();
        this.options = settings;
    }

    protected void applyVertex(Matrix4d productTransformMatrix, GaiaVertex vertex) {
        Vector3d position = vertex.getPosition();
        Vector3d scaledPosition = position.mul(options.getScaleX(), options.getScaleY(), options.getScaleZ(), new Vector3d());
        vertex.setPosition(scaledPosition);
    }
}
