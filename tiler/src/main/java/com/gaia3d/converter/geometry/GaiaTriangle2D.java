package com.gaia3d.converter.geometry;

import com.gaia3d.util.VectorUtils;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2d;

@Getter
@Setter
public class GaiaTriangle2D {
    private final Vector2d[] positions;
    private final Vector2d normal = new Vector2d();

    public GaiaTriangle2D(Vector2d position1, Vector2d position2) {
        this.positions = new Vector2d[3];
        this.positions[0] = position1;
        this.positions[1] = position2;
        calcNormal();
    }

    private void calcNormal() {
        Vector2d v1 = new Vector2d();
        Vector2d v2 = new Vector2d();
        positions[1].sub(positions[0], v1);
        positions[2].sub(positions[1], v2);

        VectorUtils.cross(v1, v2, this.normal);
        this.normal.normalize();
    }
}