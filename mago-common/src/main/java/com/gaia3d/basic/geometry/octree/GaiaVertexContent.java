package com.gaia3d.basic.geometry.octree;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.entities.GaiaTriangle;
import com.gaia3d.basic.model.GaiaVertex;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Slf4j
@Setter
@Getter
@RequiredArgsConstructor
public class GaiaVertexContent implements GeometryContent {
    private final GaiaVertex vertex;
    private GaiaBoundingBox boundingBox = null;

    @Override
    public GaiaBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            if (vertex != null) {
                boundingBox = new GaiaBoundingBox();
                boundingBox.addPoint(vertex.getPosition());
            } else {
                log.error("[ERROR][getBoundingBox] : primitiveParent is null.");
            }
        }
        return boundingBox;
    }

    @Override
    public Vector3d getCenterPoint() {
        return vertex.getPosition();
    }
}
