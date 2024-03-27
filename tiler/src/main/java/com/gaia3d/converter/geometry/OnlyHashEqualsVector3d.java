package com.gaia3d.converter.geometry;

import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Slf4j

public class OnlyHashEqualsVector3d extends Vector3d {
    public OnlyHashEqualsVector3d(Vector3d localPosition) {
        super(localPosition);
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
            log.debug("This is a test log message");
            return false;
        }
    }
}
