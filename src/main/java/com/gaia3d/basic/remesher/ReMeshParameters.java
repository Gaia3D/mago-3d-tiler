package com.gaia3d.basic.remesher;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
@Slf4j
public class ReMeshParameters {
    private double texturePixelsForMeter = 20.0; // 20 pixels per meter as default
    CellGrid3D cellGrid;
    Map<Vector3i, Vector3d> cellAveragePositions;
    Vector3d scenePositionRelToCellGrid; // Scene position relative to the cell grid origin.

    public ReMeshParameters() {
        this.cellAveragePositions = new HashMap<>();
    }
}
