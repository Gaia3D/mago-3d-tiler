package com.gaia3d.basic.geometry;

import com.gaia3d.util.GlobeUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector3d;

import java.io.Serializable;
import java.util.List;

/**
 * GaiaBoundingBox is a class to store the bounding box of a geometry.
 * It can be used to calculate the center and volume of the geometry.
 * It can also be used to convert the local bounding box to lonlat bounding box.
 * It can also be used to calculate the longest distance of the geometry.
 */
@Slf4j
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaBoundingBox implements Serializable {
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;
    private boolean isInit = false;

    public Vector3d getCenter() {
        return new Vector3d((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }

    public Vector3d getMinPosition() {
        return new Vector3d(minX, minY, minZ);
    }

    public Vector3d getMaxPosition() {
        return new Vector3d(maxX, maxY, maxZ);
    }

    public Vector3d getVolume() {
        return new Vector3d(maxX - minX, maxY - minY, maxZ - minZ);
    }

    public void addPoint(double x, double y, double z) {
        addPoint(new Vector3d(x, y, z));
    }

    public void addPoint(Vector3d vector3d) {
        if (isInit) {
            if (vector3d.x < minX) {
                minX = vector3d.x;
            }
            if (vector3d.y < minY) {
                minY = vector3d.y;
            }
            if (vector3d.z < minZ) {
                minZ = vector3d.z;
            }
            if (vector3d.x > maxX) {
                maxX = vector3d.x;
            }
            if (vector3d.y > maxY) {
                maxY = vector3d.y;
            }
            if (vector3d.z > maxZ) {
                maxZ = vector3d.z;
            }
        } else {
            isInit = true;
            minX = vector3d.x;
            minY = vector3d.y;
            minZ = vector3d.z;
            maxX = vector3d.x;
            maxY = vector3d.y;
            maxZ = vector3d.z;
        }
    }

    public boolean intersects(GaiaBoundingBox bbox) {
        if (maxX < bbox.minX || minX > bbox.maxX) {
            return false;
        }
        if (maxY < bbox.minY || minY > bbox.maxY) {
            return false;
        }
        return !(maxZ < bbox.minZ) && !(minZ > bbox.maxZ);
    }

    public boolean intersects(GaiaBoundingBox bbox, double tolerance) {
        if (maxX + tolerance < bbox.minX || minX - tolerance > bbox.maxX) {
            return false;
        }
        if (maxY + tolerance < bbox.minY || minY - tolerance > bbox.maxY) {
            return false;
        }
        return !(maxZ + tolerance < bbox.minZ) && !(minZ - tolerance > bbox.maxZ);
    }

    public void addBoundingBox(GaiaBoundingBox boundingBox) {
        if (isInit) {
            if (boundingBox.getMinX() < minX) {
                minX = boundingBox.getMinX();
            }
            if (boundingBox.getMinY() < minY) {
                minY = boundingBox.getMinY();
            }
            if (boundingBox.getMinZ() < minZ) {
                minZ = boundingBox.getMinZ();
            }
            if (boundingBox.getMaxX() > maxX) {
                maxX = boundingBox.getMaxX();
            }
            if (boundingBox.getMaxY() > maxY) {
                maxY = boundingBox.getMaxY();
            }
            if (boundingBox.getMaxZ() > maxZ) {
                maxZ = boundingBox.getMaxZ();
            }
        } else {
            isInit = true;
            minX = boundingBox.getMinX();
            minY = boundingBox.getMinY();
            minZ = boundingBox.getMinZ();
            maxX = boundingBox.getMaxX();
            maxY = boundingBox.getMaxY();
            maxZ = boundingBox.getMaxZ();
        }
    }

    public GaiaBoundingBox convertLocalToLonlatBoundingBox(Vector3d center) {
        Vector3d centerWorldCoordinate = GlobeUtils.geographicToCartesianWgs84(center);
        Matrix4d transformMatrix = GlobeUtils.transformMatrixAtCartesianPointWgs84(centerWorldCoordinate);

        Vector3d minLocalCoordinate = new Vector3d(minX, minY, minZ);
        Matrix4d minTransformMatrix = transformMatrix.translate(minLocalCoordinate, new Matrix4d());
        Vector3d minWorldCoordinate = new Vector3d(minTransformMatrix.m30(), minTransformMatrix.m31(), minTransformMatrix.m32());
        minWorldCoordinate = GlobeUtils.cartesianToGeographicWgs84(minWorldCoordinate);

        Vector3d maxLocalCoordinate = new Vector3d(maxX, maxY, maxZ);
        Matrix4d maxTransformMatrix = transformMatrix.translate(maxLocalCoordinate, new Matrix4d());
        Vector3d maxWorldCoordinate = new Vector3d(maxTransformMatrix.m30(), maxTransformMatrix.m31(), maxTransformMatrix.m32());
        maxWorldCoordinate = GlobeUtils.cartesianToGeographicWgs84(maxWorldCoordinate);

        GaiaBoundingBox result = new GaiaBoundingBox();
        result.addPoint(minWorldCoordinate);
        result.addPoint(maxWorldCoordinate);
        return result;
    }

    public double getLongestDistance() {
        Vector3d volume = getVolume();
        return Math.sqrt(volume.x * volume.x + volume.y * volume.y + volume.z * volume.z);
    }

    public double getSizeX() {
        return maxX - minX;
    }

    public double getSizeY() {
        return maxY - minY;
    }

    public double getSizeZ() {
        return maxZ - minZ;
    }

    public double getMaxSize() {
        return Math.max(getSizeX(), Math.max(getSizeY(), getSizeZ()));
    }

    public double getMinSize() {
        return Math.min(getSizeX(), Math.min(getSizeY(), getSizeZ()));
    }

    public boolean contains(GaiaBoundingBox boundingBox) {
        return minX <= boundingBox.getMinX() && minY <= boundingBox.getMinY() && minZ <= boundingBox.getMinZ() && maxX >= boundingBox.getMaxX() && maxY >= boundingBox.getMaxY() && maxZ >= boundingBox.getMaxZ();
    }

    public GaiaBoundingBox clone() {
        return new GaiaBoundingBox(minX, minY, minZ, maxX, maxY, maxZ, isInit);
    }

    /* from terrainer */
    public boolean intersectsPointXY(double pos_x, double pos_y) {
        // this function checks if a point2D is intersected by the boundingBox only meaning xAxis and yAxis
        return !(pos_x < minX) && !(pos_x > maxX) && !(pos_y < minY) && !(pos_y > maxY);
    }

    public boolean intersectsRectangleXY(double min_x, double min_y, double max_x, double max_y) {
        // this function checks if a rectangle2D is intersected by the boundingBox only meaning xAxis and yAxis
        return !(max_x < minX) && !(min_x > maxX) && !(max_y < minY) && !(min_y > maxY);
    }

    public boolean intersectsPointXYWithXAxis(double posX) {
        // this function checks if a point2D is intersected by the boundingBox only meaning xAxis and yAxis
        return !(posX < minX) && !(posX > maxX);
    }

    public boolean intersectsPointXYWithYAxis(double posY) {
        // this function checks if a point2D is intersected by the boundingBox only meaning xAxis and yAxis
        return !(posY < minY) && !(posY > maxY);
    }

    public double getLengthX() {
        return maxX - minX;
    }

    public double getLengthY() {
        return maxY - minY;
    }

    public double getLengthZ() {
        return maxZ - minZ;
    }

    public double getLongestDistanceXY() {
        Vector3d volume = getVolume();
        return Math.sqrt(volume.x * volume.x + volume.y * volume.y);
    }

    public void setFromPoints(List<Vector3d> transformedVertices) {
        this.isInit = false;
        for (Vector3d vertex : transformedVertices) {
            addPoint(vertex);
        }
    }
}
