package com.gaia3d.basic.geometry.tessellator;

import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Slf4j
public class Vector3dOnlyHashEquals extends Vector3d {
    public Vector3dOnlyHashEquals(Vector3d localPosition) {
        super(localPosition);
    }

    public Vector3dOnlyHashEquals() {
        super();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        } else if (obj == null) {
            return false;
        } else if (this.getClass() != obj.getClass()) {
            return false;
        } else {
            return false;
        }
    }
}
