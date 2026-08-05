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
    CellGrid3D cellGrid;
    Map<Vector3i, Vector3d> cellAveragePositions;
    Vector3d scenePositionRelToCellGrid; // Scene position relative to the cell grid origin.
    double angleDeg = 50.0;
    GlobalBoundaryAnchors globalBoundaryAnchors = null;
    private double texturePixelsForMeter = 20.0; // 20 pixels per meter as default

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

    public ReMeshParameters copyForWorker() {
        ReMeshParameters copy =
                new ReMeshParameters();

        /*
         * Compartidos como estructuras inmutables
         * o estrictamente de solo lectura.
         */
        copy.cellGrid =
                this.cellGrid;

        copy.globalBoundaryAnchors =
                this.globalBoundaryAnchors;

        /*
         * Estado mutable privado del worker.
         */
        copy.cellAveragePositions =
                copyCellAveragePositions(
                        this.cellAveragePositions
                );

        copy.scenePositionRelToCellGrid =
                this.scenePositionRelToCellGrid == null
                        ? null
                        : new Vector3d(
                        this.scenePositionRelToCellGrid
                );

        copy.angleDeg =
                this.angleDeg;

        copy.texturePixelsForMeter =
                this.texturePixelsForMeter;

        return copy;
    }

    private static Map<Vector3i, Vector3d>
    copyCellAveragePositions(
            Map<Vector3i, Vector3d> source
    ) {
        Map<Vector3i, Vector3d> result =
                new HashMap<>();

        if (source == null || source.isEmpty()) {
            return result;
        }

        for (Map.Entry<Vector3i, Vector3d> entry
                : source.entrySet()) {

            Vector3i key =
                    entry.getKey();

            Vector3d value =
                    entry.getValue();

            if (key == null || value == null) {
                continue;
            }

            result.put(
                    new Vector3i(key),
                    new Vector3d(value)
            );
        }

        return result;
    }
}
