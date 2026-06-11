package com.gaia3d.basic.geometry.entities;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.model.GaiaVertex;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

@Slf4j
@Getter
public class GaiaTriangle {
    private Vector3d point1;
    private Vector3d point2;
    private Vector3d point3;
    private Vector3d normal;
    private GaiaBoundingBox boundingBox;
    private GaiaPlane plane;
    private GaiaSegment[] segments;
    private Vector3d barycenter;

    public GaiaTriangle() {
        this.point1 = new Vector3d();
        this.point2 = new Vector3d();
        this.point3 = new Vector3d();
    }

    public GaiaTriangle(Vector3d point1, Vector3d point2, Vector3d point3) {
        this.point1 = new Vector3d(point1);
        this.point2 = new Vector3d(point2);
        this.point3 = new Vector3d(point3);
    }

    public void setPoint1(Vector3d point1) {
        this.point1 = point1;
        invalidateCaches();
    }

    public void setPoint2(Vector3d point2) {
        this.point2 = point2;
        invalidateCaches();
    }

    public void setPoint3(Vector3d point3) {
        this.point3 = point3;
        invalidateCaches();
    }

    public void setNormal(Vector3d normal) {
        this.normal = normal;
        this.plane = null;
    }

    private void invalidateCaches() {
        this.boundingBox = null;
        this.plane = null;
        this.segments = null;
        this.barycenter = null;
        this.normal = null;
    }

    public GaiaBoundingBox getBoundingBox() {
        if (boundingBox == null) {
            boundingBox = new GaiaBoundingBox();
            boundingBox.addPoint(point1);
            boundingBox.addPoint(point2);
            boundingBox.addPoint(point3);
        }
        return boundingBox;
    }

    public Vector3d getNormal() {
        if (normal == null) {
            Vector3d edge1 = new Vector3d(point2).sub(point1);
            Vector3d edge2 = new Vector3d(point3).sub(point1);
            normal = new Vector3d();
            edge1.cross(edge2, normal);
            normal.normalize();
        }
        return normal;
    }

    public GaiaPlane getPlane() {
        if (plane == null) {
            Vector3d normal = getNormal();
            if (normal.length() == 0) {
                log.info("[INFO][getPlane] : Normal vector is zero-length, cannot create plane.");
                return null;
            }
            if (Double.isNaN(normal.x) || Double.isNaN(normal.y) || Double.isNaN(normal.z)) {
                log.info("[INFO][getPlane] : Normal vector contains NaN values, cannot create plane.");
                return null;
            }
            plane = new GaiaPlane(point1, normal);
        }
        return plane;
    }

    public Vector3d[] getPoints() {
        return new Vector3d[]{point1, point2, point3};
    }

    public GaiaSegment[] getSegments() {
        if (segments == null) {
            segments = new GaiaSegment[]{
                    new GaiaSegment(point1, point2),
                    new GaiaSegment(point2, point3),
                    new GaiaSegment(point3, point1)
            };
        }
        return segments;
    }

    public Vector3d getBarycenter() {
        if (barycenter == null) {
            barycenter = new Vector3d(
                    (point1.x + point2.x + point3.x) / 3,
                    (point1.y + point2.y + point3.y) / 3,
                    (point1.z + point2.z + point3.z) / 3
            );
        }
        return barycenter;
    }

    public double area() {
        Vector3d edge1 = new Vector3d(point2).sub(point1);
        Vector3d edge2 = new Vector3d(point3).sub(point1);
        Vector3d crossProduct = new Vector3d();
        edge1.cross(edge2, crossProduct);
        return 0.5 * crossProduct.length();
    }
}
