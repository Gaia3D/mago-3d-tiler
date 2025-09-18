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

    public void deleteCellAveragePositionInsideBox(Vector3i minCellIndex, Vector3i maxCellIndex) {
        int cnt = 0;
        for (int i = minCellIndex.x + 1; i < maxCellIndex.x; i++) {
            for (int j = minCellIndex.y + 1; j < maxCellIndex.y; j++) {
                for (int k = minCellIndex.z + 1; k < maxCellIndex.z; k++) {
                    Vector3i cellIndex = new Vector3i(i, j, k);
                    if (cellAveragePositions.containsKey(cellIndex)) {
                        cellAveragePositions.remove(cellIndex);
                        cnt++;
                    }
                }
            }
        }
        log.debug("Deleted {} cell average positions inside the box.", cnt);
    }
}
