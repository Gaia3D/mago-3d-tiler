package com.gaia3d.basic.geometry.tessellator;

import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;

@Slf4j
public class Vector2dOnlyHashEquals extends Vector2d {
    public Vector2dOnlyHashEquals(Vector2d localPosition) {
        super(localPosition);
    }

    public Vector2dOnlyHashEquals() {
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