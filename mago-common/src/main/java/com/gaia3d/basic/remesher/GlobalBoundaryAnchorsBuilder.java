package com.gaia3d.basic.remesher;

import com.gaia3d.basic.halfedge.PlaneCutPoint;
import com.gaia3d.basic.halfedge.PlaneType;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;
import org.joml.Vector3i;

import java.util.*;

@Slf4j
public class GlobalBoundaryAnchorsBuilder {

    private final CellGrid3D cellGrid;

    private final double planeTolerance;

    private final List<PlaneCutPoint> candidatePoints =
            new ArrayList<>();

    public GlobalBoundaryAnchorsBuilder(
            CellGrid3D cellGrid,
            double planeTolerance
    ) {
        if (cellGrid == null) {
            throw new IllegalArgumentException(
                    "cellGrid must not be null"
            );
        }

        if (!Double.isFinite(planeTolerance)
                || planeTolerance <= 0.0) {
            throw new IllegalArgumentException(
                    "planeTolerance must be finite and greater than zero"
            );
        }

        this.cellGrid = cellGrid;
        this.planeTolerance = planeTolerance;
    }

    public static boolean isValidPoint(
            PlaneCutPoint point
    ) {
        if (point == null
                || point.getPlaneType() == null) {
            return false;
        }

        if (!isSupportedPlaneType(
                point.getPlaneType()
        )) {
            return false;
        }

        return Double.isFinite(point.getX())
                && Double.isFinite(point.getY())
                && Double.isFinite(point.getZ())
                && Double.isFinite(
                point.getPlaneCoordinate()
        );
    }

    private static boolean isSupportedPlaneType(
            PlaneType planeType
    ) {
        return planeType == PlaneType.XY
                || planeType == PlaneType.XYNEG
                || planeType == PlaneType.XZ
                || planeType == PlaneType.XZNEG
                || planeType == PlaneType.YZ
                || planeType == PlaneType.YZNEG;
    }

    public void addPoint(
            PlaneCutPoint point
    ) {
        if (!isValidPoint(point)) {
            return;
        }

        /*
         * PlaneCutPoint debe ser inmutable.
         * Así podemos guardar la referencia sin crear otra instancia.
         */
        candidatePoints.add(point);
    }

    public void addPoints(
            Collection<PlaneCutPoint> points
    ) {
        if (points == null || points.isEmpty()) {
            return;
        }

        for (PlaneCutPoint point : points) {
            addPoint(point);
        }
    }

    public int getCandidateCount() {
        return candidatePoints.size();
    }

    public GlobalBoundaryAnchors build() {
        GlobalBoundaryAnchors result =
                new GlobalBoundaryAnchors();

        if (candidatePoints.isEmpty()) {
            return result;
        }

        /*
         * CompletionService devuelve resultados según terminan,
         * por lo que el orden puede cambiar entre ejecuciones.
         *
         * Ordenar los puntos hace más reproducible la suma de doubles.
         */
        candidatePoints.sort(
                Comparator.comparingDouble(
                                PlaneCutPoint::getX
                        )
                        .thenComparingDouble(
                                PlaneCutPoint::getY
                        )
                        .thenComparingDouble(
                                PlaneCutPoint::getZ
                        )
                        .thenComparingInt(
                                point ->
                                        point.getPlaneType().ordinal()
                        )
                        .thenComparingDouble(
                                PlaneCutPoint::getPlaneCoordinate
                        )
        );

        Map<Vector3i, PositionAccumulator>
                accumulatorsByCell =
                new HashMap<>();

        /*
         * Reutilizamos un Vector3d para no generar uno por candidato.
         * Más adelante puedes añadir a CellGrid3D una sobrecarga:
         *
         * getCellIndex(double x, double y, double z)
         */
        Vector3d reusablePosition =
                new Vector3d();

        for (PlaneCutPoint point : candidatePoints) {
            reusablePosition.set(
                    point.getX(),
                    point.getY(),
                    point.getZ()
            );

            Vector3i cellIndex =
                    cellGrid.getCellIndex(
                            reusablePosition
                    );

            if (cellIndex == null) {
                continue;
            }

            Vector3i storedCellIndex =
                    new Vector3i(cellIndex);

            PositionAccumulator accumulator =
                    accumulatorsByCell.computeIfAbsent(
                            storedCellIndex,
                            ignored ->
                                    new PositionAccumulator()
                    );

            accumulator.add(point);
        }

        int conflictingCellsCount = 0;

        for (Map.Entry<Vector3i, PositionAccumulator> entry
                : accumulatorsByCell.entrySet()) {

            PositionAccumulator accumulator =
                    entry.getValue();

            if (accumulator == null
                    || accumulator.getCount() == 0) {
                continue;
            }

            /*
             * Una celda puede contener planos ortogonales:
             *
             * YZ bloquea X
             * XZ bloquea Y
             * XY bloquea Z
             *
             * Eso representa correctamente una esquina.
             *
             * Lo que no puede contener son dos planos paralelos
             * diferentes sobre el mismo eje.
             */
            if (accumulator.hasPlaneConflict(
                    planeTolerance
            )) {
                conflictingCellsCount++;

                log.warn(
                        "Different parallel cutting planes found "
                                + "inside the same anchor cell. "
                                + "cell={}, points={}",
                        entry.getKey(),
                        accumulator.getCount()
                );

                continue;
            }

            Vector3d lockedAverage =
                    accumulator.calculateLockedAverage();

            if (lockedAverage == null) {
                continue;
            }

            result.putLockedAverage(
                    entry.getKey(),
                    lockedAverage
            );
        }

        log.info(
                "GlobalBoundaryAnchors built. "
                        + "candidates={}, cells={}, anchors={}, "
                        + "conflictingCells={}",
                candidatePoints.size(),
                accumulatorsByCell.size(),
                result.size(),
                conflictingCellsCount
        );

        return result;
    }

    private static class PositionAccumulator {

        private double sumX;
        private double sumY;
        private double sumZ;

        private int count;

        /*
         * Plano YZ:
         * bloquea la coordenada X.
         */
        private double lockedXSum;
        private int lockedXCount;
        private double lockedXMin =
                Double.POSITIVE_INFINITY;
        private double lockedXMax =
                Double.NEGATIVE_INFINITY;

        /*
         * Plano XZ:
         * bloquea la coordenada Y.
         */
        private double lockedYSum;
        private int lockedYCount;
        private double lockedYMin =
                Double.POSITIVE_INFINITY;
        private double lockedYMax =
                Double.NEGATIVE_INFINITY;

        /*
         * Plano XY:
         * bloquea la coordenada Z.
         */
        private double lockedZSum;
        private int lockedZCount;
        private double lockedZMin =
                Double.POSITIVE_INFINITY;
        private double lockedZMax =
                Double.NEGATIVE_INFINITY;

        public void add(
                PlaneCutPoint point
        ) {
            sumX += point.getX();
            sumY += point.getY();
            sumZ += point.getZ();

            count++;

            PlaneType planeType =
                    point.getPlaneType();

            double planeCoordinate =
                    point.getPlaneCoordinate();

            if (planeType == PlaneType.YZ
                    || planeType == PlaneType.YZNEG) {

                lockedXSum += planeCoordinate;
                lockedXCount++;

                lockedXMin =
                        Math.min(
                                lockedXMin,
                                planeCoordinate
                        );

                lockedXMax =
                        Math.max(
                                lockedXMax,
                                planeCoordinate
                        );

            } else if (planeType == PlaneType.XZ
                    || planeType == PlaneType.XZNEG) {

                lockedYSum += planeCoordinate;
                lockedYCount++;

                lockedYMin =
                        Math.min(
                                lockedYMin,
                                planeCoordinate
                        );

                lockedYMax =
                        Math.max(
                                lockedYMax,
                                planeCoordinate
                        );

            } else if (planeType == PlaneType.XY
                    || planeType == PlaneType.XYNEG) {

                lockedZSum += planeCoordinate;
                lockedZCount++;

                lockedZMin =
                        Math.min(
                                lockedZMin,
                                planeCoordinate
                        );

                lockedZMax =
                        Math.max(
                                lockedZMax,
                                planeCoordinate
                        );
            }
        }

        public int getCount() {
            return count;
        }

        public boolean hasPlaneConflict(
                double tolerance
        ) {
            if (lockedXCount > 0
                    && lockedXMax - lockedXMin
                    > tolerance) {
                return true;
            }

            if (lockedYCount > 0
                    && lockedYMax - lockedYMin
                    > tolerance) {
                return true;
            }

            return lockedZCount > 0
                    && lockedZMax - lockedZMin
                    > tolerance;
        }

        public Vector3d calculateLockedAverage() {
            if (count == 0) {
                return null;
            }

            double inverseCount =
                    1.0 / count;

            double averageX =
                    sumX * inverseCount;

            double averageY =
                    sumY * inverseCount;

            double averageZ =
                    sumZ * inverseCount;

            /*
             * Reproyectamos el promedio sobre todos los planos
             * presentes en la celda.
             */
            if (lockedXCount > 0) {
                averageX =
                        lockedXSum / lockedXCount;
            }

            if (lockedYCount > 0) {
                averageY =
                        lockedYSum / lockedYCount;
            }

            if (lockedZCount > 0) {
                averageZ =
                        lockedZSum / lockedZCount;
            }

            return new Vector3d(
                    averageX,
                    averageY,
                    averageZ
            );
        }
    }
}