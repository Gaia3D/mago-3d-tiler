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

/**
 * GaiaBoundingBox is a class to store the bounding box of a geometry.
 * It can be used to calculate the center and volume of the geometry.
 * It can also be used to convert the local bounding box to lonlat bounding box.
 * It can also be used to calculate the longest distance of the geometry.
 * @author znkim
 * @since 1.0.0
 * @see GaiaRectangle
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

    public Vector3d getVolume() {
        return new Vector3d(maxX - minX, maxY - minY, maxZ - minZ);
    }

    //addPoint
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

    public void addBoundingBox(GaiaBoundingBox boundingBox) {
        if (isInit) {
            if (boundingBox.minX < minX) {
                minX = boundingBox.minX;
            }
            if (boundingBox.minY < minY) {
                minY = boundingBox.minY;
            }
            if (boundingBox.minZ < minZ) {
                minZ = boundingBox.minZ;
            }
            if (boundingBox.maxX > maxX) {
                maxX = boundingBox.maxX;
            }
            if (boundingBox.maxY > maxY) {
                maxY = boundingBox.maxY;
            }
            if (boundingBox.maxZ > maxZ) {
                maxZ = boundingBox.maxZ;
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
        Matrix4d minTransfromMatrix = transformMatrix.translate(minLocalCoordinate, new Matrix4d());
        Vector3d minWorldCoordinate = new Vector3d(minTransfromMatrix.m30(), minTransfromMatrix.m31(), minTransfromMatrix.m32());
        minWorldCoordinate = GlobeUtils.cartesianToGeographicWgs84(minWorldCoordinate);

        Vector3d maxLocalCoordinate = new Vector3d(maxX, maxY, maxZ);
        Matrix4d maxTransfromMatrix = transformMatrix.translate(maxLocalCoordinate, new Matrix4d());
        Vector3d maxWorldCoordinate = new Vector3d(maxTransfromMatrix.m30(), maxTransfromMatrix.m31(), maxTransfromMatrix.m32());
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

    public boolean contains(GaiaBoundingBox boundingBox) {
        return minX <= boundingBox.minX && minY <= boundingBox.minY && minZ <= boundingBox.minZ
                && maxX >= boundingBox.maxX && maxY >= boundingBox.maxY && maxZ >= boundingBox.maxZ;
    }

    public GaiaBoundingBox clone() {
        return new GaiaBoundingBox(minX, minY, minZ, maxX, maxY, maxZ, isInit);
    }
}
