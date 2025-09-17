package com.gaia3d.basic.geometry.entities;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.PlaneType;
import com.gaia3d.util.GeometryUtils;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import javax.swing.text.Segment;

@Slf4j
@Getter
@Setter
public class GaiaTriangle {
    private Vector3d point1;
    private Vector3d point2;
    private Vector3d point3;
    private Vector3d normal;

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

    public GaiaBoundingBox getBoundingBox() {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        boundingBox.addPoint(point1);
        boundingBox.addPoint(point2);
        boundingBox.addPoint(point3);
        return boundingBox;
    }

    public Vector3d getNormal() {
        if (normal == null) {
            Vector3d edge1 = new Vector3d(point2).sub(point1);
            Vector3d edge2 = new Vector3d(point3).sub(point1);
            normal = new Vector3d();
            normal.cross(edge1, edge2);
            normal.normalize();
        }
        return normal;
    }

    public GaiaPlane getPlane() {
        Vector3d normal = getNormal();
        Vector3d position = new Vector3d(point1);
        return new GaiaPlane(position, normal);
    }

    public Vector3d[] getPoints() {
        return new Vector3d[]{new Vector3d(point1), new Vector3d(point2), new Vector3d(point3)};
    }

    public GaiaSegment[] getSegments() {
        return new GaiaSegment[]{
                new GaiaSegment(new Vector3d(point1), new Vector3d(point2)),
                new GaiaSegment(new Vector3d(point2), new Vector3d(point3)),
                new GaiaSegment(new Vector3d(point3), new Vector3d(point1))
        };
    }
}
