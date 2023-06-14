package geometry.basic;

import geometry.structure.GaiaScene;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.util.List;

@Slf4j
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaBoundingBox {
    private double minX, minY, minZ;
    private double maxX, maxY, maxZ;
    private boolean isInit = false;

    public Vector3d getCenter() {
        return new Vector3d((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }

    public Vector3d getVolume() {
        return new Vector3d(maxX - minX, maxY - minY, maxZ - minZ);
    }

    public Vector3d getCenterCorrected() {
        return new Vector3d((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
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

    public double getLongestDistance() {
        Vector3d volume = getVolume();
        return Math.sqrt(volume.x * volume.x + volume.y * volume.y + volume.z * volume.z);
    }

    public double getLongestAxisValue() {
        Vector3d volume = getVolume();
        if (volume.x > volume.y) {
            return Math.max(volume.x, volume.z);
        } else {
            return Math.max(volume.y, volume.z);
        }
    }

    public GaiaBoundingBox tightBoundingBox(List<GaiaScene> scenes) {
        GaiaBoundingBox tightBoundingBox = new GaiaBoundingBox();
        tightBoundingBox.addPoint(new Vector3d(minX, minY, minZ));
        tightBoundingBox.addPoint(new Vector3d(maxX, maxY, maxZ));
        return tightBoundingBox;
    }

    public GaiaBoundingBox[] divideBoundingBox() {
        // child idx.***
        //       +-----+-----+
        //       |  3  |  2  |
        //       +-----+-----+
        //       |  0  |  1  |
        //       +-----+-----+
        Vector3d center = this.getCenter();
        double minX = this.getMinX();
        double minY = this.getMinY();
        double minZ = this.getMinZ();
        double maxX = this.getMaxX();
        double maxY = this.getMaxY();
        double maxZ = this.getMaxZ();
        double centerX = center.x();
        double centerY = center.y();
        GaiaBoundingBox[] divideBoundingBox = new GaiaBoundingBox[4];
        GaiaBoundingBox boundingBox0 = new GaiaBoundingBox();
        boundingBox0.addPoint(new Vector3d(minX, minY, minZ));
        boundingBox0.addPoint(new Vector3d(centerX, centerY, maxZ));
        divideBoundingBox[0] = boundingBox0;
        GaiaBoundingBox boundingBox1 = new GaiaBoundingBox();
        boundingBox1.addPoint(new Vector3d(centerX, minY, minZ));
        boundingBox1.addPoint(new Vector3d(maxX, centerY, maxZ));
        divideBoundingBox[1] = boundingBox1;
        GaiaBoundingBox boundingBox2 = new GaiaBoundingBox();
        boundingBox2.addPoint(new Vector3d(centerX, centerY, minZ));
        boundingBox2.addPoint(new Vector3d(maxX, maxY, maxZ));
        divideBoundingBox[2] = boundingBox2;
        GaiaBoundingBox boundingBox3 = new GaiaBoundingBox();
        boundingBox3.addPoint(new Vector3d(minX, centerY, minZ));
        boundingBox3.addPoint(new Vector3d(centerX, maxY, maxZ));
        divideBoundingBox[3] = boundingBox3;
        return divideBoundingBox;
    }
}
