package com.gaia3d.basic.halfedge;

import org.joml.Vector3d;
import org.joml.Vector3dc;

import java.util.Objects;

public final class PlaneCutPoint {

    private final double px;
    private final double py;
    private final double pz;

    private final PlaneType planeType;

    public PlaneCutPoint(
            double px,
            double py,
            double pz,
            PlaneType planeType
    ) {
        if (!Double.isFinite(px)
                || !Double.isFinite(py)
                || !Double.isFinite(pz)) {
            throw new IllegalArgumentException(
                    "Point coordinates must be finite"
            );
        }

        this.planeType =
                Objects.requireNonNull(
                        planeType,
                        "planeType must not be null"
                );

        this.px = px;
        this.py = py;
        this.pz = pz;
    }

    public static PlaneCutPoint of(
            Vector3dc position,
            PlaneType planeType,
            Vector3dc planePosition
    ) {
        Objects.requireNonNull(
                position,
                "position must not be null"
        );

        Objects.requireNonNull(
                planeType,
                "planeType must not be null"
        );

        Objects.requireNonNull(
                planePosition,
                "planePosition must not be null"
        );

        double px = position.x();
        double py = position.y();
        double pz = position.z();

        /*
         * El punto almacenado queda exactamente sobre
         * el plano de corte.
         */
        if (planeType == PlaneType.XY
                || planeType == PlaneType.XYNEG) {

            pz = planePosition.z();

        } else if (planeType == PlaneType.XZ
                || planeType == PlaneType.XZNEG) {

            py = planePosition.y();

        } else if (planeType == PlaneType.YZ
                || planeType == PlaneType.YZNEG) {

            px = planePosition.x();

        } else {
            throw new IllegalArgumentException(
                    "Unsupported plane type: " + planeType
            );
        }

        return new PlaneCutPoint(
                px,
                py,
                pz,
                planeType
        );
    }

    public PlaneCutPoint copy() {
        return new PlaneCutPoint(
                px,
                py,
                pz,
                planeType
        );
    }

    public double getX() {
        return px;
    }

    public double getY() {
        return py;
    }

    public double getZ() {
        return pz;
    }

    public PlaneType getPlaneType() {
        return planeType;
    }

    public double getPlaneCoordinate() {
        if (planeType == PlaneType.XY
                || planeType == PlaneType.XYNEG) {
            return pz;
        }

        if (planeType == PlaneType.XZ
                || planeType == PlaneType.XZNEG) {
            return py;
        }

        if (planeType == PlaneType.YZ
                || planeType == PlaneType.YZNEG) {
            return px;
        }

        throw new IllegalStateException(
                "Unsupported plane type: " + planeType
        );
    }

    public Vector3d getPositionCopy() {
        return new Vector3d(
                px,
                py,
                pz
        );
    }

    public Vector3d getPosition(
            Vector3d destination
    ) {
        Objects.requireNonNull(
                destination,
                "destination must not be null"
        );

        return destination.set(
                px,
                py,
                pz
        );
    }
}