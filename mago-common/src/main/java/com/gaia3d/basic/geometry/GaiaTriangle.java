package com.gaia3d.basic.geometry;

import com.gaia3d.basic.geometry.entities.GaiaPlane;
import com.gaia3d.basic.geometry.entities.GaiaSegment;
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

    /* get Triangle Center */
    public Vector3d getCenter() {
        Vector3d center = new Vector3d();
        center.add(positions[0], center);
        center.add(positions[1], center);
        center.add(positions[2], center);
        center.mul(1.0 / 3.0);
        return center;
    }

    private void calcNormal() {
        if (positions[0].equals(positions[1]) || positions[1].equals(positions[2]) || positions[2].equals(positions[0])) {
            //log.warn("[WARN] Degenerate triangle detected");
            this.normal = new Vector3d(0, 0, 1);
            return;
        }

        Vector3d v1 = new Vector3d();
        Vector3d v2 = new Vector3d();
        positions[1].sub(positions[0], v1);
        positions[2].sub(positions[1], v2);
        v1.normalize();
        v2.normalize();
        v1.cross(v2, this.normal);

        double z = this.normal.z();
        if (z < 0.0) {
            this.normal.negate();
        }

        this.normal.normalize();
    }

    public Vector3d getNormal() {
        if (this.normal == null || this.normal.length() == 0) {
            calcNormal();
        }
        return this.normal;
    }

    public GaiaBoundingBox getBoundingBox() {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        boundingBox.addPoint(positions[0]);
        boundingBox.addPoint(positions[1]);
        boundingBox.addPoint(positions[2]);
        return boundingBox;
    }

    public GaiaPlane getPlane() {
        Vector3d position = getCenter();
        Vector3d normal = getNormal();
        return new GaiaPlane(position, normal);
    }

    public Vector3d[] getPoints() {
        Vector3d[] points = new Vector3d[3];
        points[0] = new Vector3d(positions[0]);
        points[1] = new Vector3d(positions[1]);
        points[2] = new Vector3d(positions[2]);
        return points;
    }

    public GaiaSegment[] getSegments() {
        GaiaSegment[] segments = new GaiaSegment[3];
        segments[0] = new GaiaSegment(new Vector3d(positions[0]), new Vector3d(positions[1]));
        segments[1] = new GaiaSegment(new Vector3d(positions[1]), new Vector3d(positions[2]));
        segments[2] = new GaiaSegment(new Vector3d(positions[2]), new Vector3d(positions[0]));
        return segments;
    }
}
