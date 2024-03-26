package com.gaia3d.converter.geometry;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Getter
@Setter
@Slf4j
public class GaiaTriangle {
    private final Vector3d[] positions;
    private Vector3d normal = new Vector3d();

    public GaiaTriangle(Vector3d position1, Vector3d position2, Vector3d position3) {
        this.positions = new Vector3d[3];
        this.positions[0] = position1;
        this.positions[1] = position2;
        this.positions[2] = position3;
        calcNormal();
    }

    private void calcNormal() {
        Vector3d v1 = new Vector3d();
        Vector3d v2 = new Vector3d();
        positions[1].sub(positions[0], v1);
        positions[2].sub(positions[1], v2);
        //v1.normalize();
        //v2.normalize();
        v1.cross(v2, this.normal);

        this.normal.normalize();


        if (Double.isNaN(this.normal.x) || Double.isNaN(this.normal.y) || Double.isNaN(this.normal.z)) {
            log.info("Normal: {}", this.normal);
            this.normal = new Vector3d(0, 0, 1);
        }




        //this.normal = new Vector3d(0, 0, 1);
        //this.normal.normalize();
    }
}
