package com.gaia3d.basic.geometry;

import com.gaia3d.basic.geometry.entities.GaiaPlane;
import com.gaia3d.basic.geometry.entities.GaiaSegment;
import com.gaia3d.basic.geometry.entities.GaiaTriangle;
import com.gaia3d.basic.halfedge.PlaneType;
import com.gaia3d.util.GeometryUtils;
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

    public GaiaBoundingBox(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.isInit = true;
    }

    public Vector3d getCenter() {
        return new Vector3d((minX + maxX) / 2, (minY + maxY) / 2, (minZ + maxZ) / 2);
    }

    public Vector3d getFloorCenter() {
        return new Vector3d((minX + maxX) / 2, (minY + maxY) / 2, minZ);
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

    public boolean isValid() {
        boolean isInvalid = Double.isNaN(minX) || Double.isNaN(minY) || Double.isNaN(minZ) || Double.isNaN(maxX) || Double.isNaN(maxY) || Double.isNaN(maxZ);
        boolean isInverted = minX > maxX || minY > maxY || minZ > maxZ;
        boolean isZeroVolume = (minX == maxX) || (minY == maxY) || (minZ == maxZ);

        if (isInvalid) {
            log.debug("Bounding box is invalid (NaN values).");
        } else if (isInverted) {
            log.debug("Bounding box is inverted (min values greater than max values).");
        } else if (isZeroVolume) {
            log.debug("Bounding box has zero volume (one or more dimensions are zero).");
        }
        return !isInvalid && !isInverted && !isZeroVolume;
    }

    public double getMaxRadius() {
        Vector3d center = getCenter();
        Vector3d minPosition = getMinPosition();
        double radiusX = Math.abs(center.x - minPosition.x);
        double radiusY = Math.abs(center.y - minPosition.y);
        double radiusZ = Math.abs(center.z - minPosition.z);
        return Math.sqrt(radiusX * radiusX + radiusY * radiusY + radiusZ * radiusZ);
    }

    public GaiaBoundingBox createIntersection(GaiaBoundingBox other) {
        double ixMin = Math.max(this.minX, other.minX);
        double iyMin = Math.max(this.minY, other.minY);
        double izMin = Math.max(this.minZ, other.minZ);

        double ixMax = Math.min(this.maxX, other.maxX);
        double iyMax = Math.min(this.maxY, other.maxY);
        double izMax = Math.min(this.maxZ, other.maxZ);

        if (ixMin >= ixMax || iyMin >= iyMax || izMin >= izMax) {
            return null; // No intersection
        }

        return new GaiaBoundingBox(ixMin, iyMin, izMin, ixMax, iyMax, izMax);
    }

    public boolean intersectsPoint(Vector3d point) {
        // Check if the point is inside the bounding box.
        return !(point.x < minX) && !(point.x > maxX) && !(point.y < minY) && !(point.y > maxY) && !(point.z < minZ) && !(point.z > maxZ);
    }

    public boolean intersectsTriangle(GaiaTriangle triangle) {
        GaiaBoundingBox triangleBbox = triangle.getBoundingBox();
        if (!this.intersects(triangleBbox)) {
            return false;
        }

        Vector3d barycenter = triangle.getBarycenter();
        if (intersectsPoint(barycenter)) {
            return true;
        }

        Vector3d point1 = triangle.getPoint1();
        Vector3d point2 = triangle.getPoint2();
        Vector3d point3 = triangle.getPoint3();
        if (intersectsPoint(point1) || intersectsPoint(point2) || intersectsPoint(point3)) {
            return true;
        }

        GaiaPlane trianglePlane = triangle.getPlane();
        if (trianglePlane == null) {
            log.info("[INFO][intersectsTriangle] : Triangle plane is null.");
            return false;
        }
        double centerX = (minX + maxX) * 0.5;
        double centerY = (minY + maxY) * 0.5;
        double centerZ = (minZ + maxZ) * 0.5;
        double radiusX = (maxX - minX) * 0.5;
        double radiusY = (maxY - minY) * 0.5;
        double radiusZ = (maxZ - minZ) * 0.5;
        double maxRadius = Math.sqrt(radiusX * radiusX + radiusY * radiusY + radiusZ * radiusZ);
        double distanceToPlane = trianglePlane.distanceToPoint(centerX, centerY, centerZ);
        if (Math.abs(distanceToPlane) > maxRadius) {
            return false;
        }

        if (!intersectsPlane(trianglePlane)) {
            return false;
        }

        GaiaSegment[] triangleEdges = triangle.getSegments();
        for (GaiaSegment edge : triangleEdges) {
            if (this.intersectsSegment(edge)) {
                return true;
            }
        }

        return intersectsAASegmentsToTriangle(trianglePlane, point1, point2, point3);
    }

    private boolean intersectsAASegmentsToTriangle(GaiaPlane trianglePlane, Vector3d point1, Vector3d point2, Vector3d point3) {
        Vector3d normal = trianglePlane.getNormal();
        PlaneType bestPlane = GeometryUtils.getBestPlaneToProject(normal);
        if (bestPlane == null) {
            log.error("[ERROR][intersectsAASegmentToTriangle] : Best plane is null.");
            return false;
        }

        if (intersectsAASegmentToTriangle(trianglePlane, point1, point2, point3, bestPlane, 0, minX, maxX, minY, minZ)) {
            return true;
        }
        if (intersectsAASegmentToTriangle(trianglePlane, point1, point2, point3, bestPlane, 0, minX, maxX, maxY, minZ)) {
            return true;
        }
        if (intersectsAASegmentToTriangle(trianglePlane, point1, point2, point3, bestPlane, 0, minX, maxX, minY, maxZ)) {
            return true;
        }
        if (intersectsAASegmentToTriangle(trianglePlane, point1, point2, point3, bestPlane, 0, minX, maxX, maxY, maxZ)) {
            return true;
        }

        if (intersectsAASegmentToTriangle(trianglePlane, point1, point2, point3, bestPlane, 1, minY, maxY, minX, minZ)) {
            return true;
        }
        if (intersectsAASegmentToTriangle(trianglePlane, point1, point2, point3, bestPlane, 1, minY, maxY, maxX, minZ)) {
            return true;
        }
        if (intersectsAASegmentToTriangle(trianglePlane, point1, point2, point3, bestPlane, 1, minY, maxY, minX, maxZ)) {
            return true;
        }
        if (intersectsAASegmentToTriangle(trianglePlane, point1, point2, point3, bestPlane, 1, minY, maxY, maxX, maxZ)) {
            return true;
        }

        if (intersectsAASegmentToTriangle(trianglePlane, point1, point2, point3, bestPlane, 2, minZ, maxZ, minX, minY)) {
            return true;
        }
        if (intersectsAASegmentToTriangle(trianglePlane, point1, point2, point3, bestPlane, 2, minZ, maxZ, maxX, minY)) {
            return true;
        }
        if (intersectsAASegmentToTriangle(trianglePlane, point1, point2, point3, bestPlane, 2, minZ, maxZ, minX, maxY)) {
            return true;
        }
        if (intersectsAASegmentToTriangle(trianglePlane, point1, point2, point3, bestPlane, 2, minZ, maxZ, maxX, maxY)) {
            return true;
        }

        return false;
    }

    private boolean intersectsAASegmentToTriangle(GaiaPlane trianglePlane, Vector3d point1, Vector3d point2, Vector3d point3, PlaneType bestPlane, int axis, double segmentMin, double segmentMax, double fixed1, double fixed2) {
        try {
            double aCoeff = trianglePlane.getA();
            double bCoeff = trianglePlane.getB();
            double cCoeff = trianglePlane.getC();
            double dCoeff = trianglePlane.getD();
            double ix;
            double iy;
            double iz;

            if (axis == 0) {
                if (Math.abs(aCoeff) < 1e-12) {
                    return false;
                }
                ix = (-dCoeff - bCoeff * fixed1 - cCoeff * fixed2) / aCoeff;
                if (ix < segmentMin || ix > segmentMax) {
                    return false;
                }
                iy = fixed1;
                iz = fixed2;
            } else if (axis == 1) {
                if (Math.abs(bCoeff) < 1e-12) {
                    return false;
                }
                iy = (-dCoeff - aCoeff * fixed1 - cCoeff * fixed2) / bCoeff;
                if (iy < segmentMin || iy > segmentMax) {
                    return false;
                }
                ix = fixed1;
                iz = fixed2;
            } else {
                if (Math.abs(cCoeff) < 1e-12) {
                    return false;
                }
                iz = (-dCoeff - aCoeff * fixed1 - bCoeff * fixed2) / cCoeff;
                if (iz < segmentMin || iz > segmentMax) {
                    return false;
                }
                ix = fixed1;
                iy = fixed2;
            }

            double px;
            double py;
            double ax;
            double ay;
            double bx;
            double by;
            double cx;
            double cy;
            if (bestPlane == PlaneType.XY || bestPlane == PlaneType.XYNEG) {
                px = ix;
                py = iy;
                ax = point1.x;
                ay = point1.y;
                bx = point2.x;
                by = point2.y;
                cx = point3.x;
                cy = point3.y;
            } else if (bestPlane == PlaneType.XZ || bestPlane == PlaneType.XZNEG) {
                px = ix;
                py = iz;
                ax = point1.x;
                ay = point1.z;
                bx = point2.x;
                by = point2.z;
                cx = point3.x;
                cy = point3.z;
            } else if (bestPlane == PlaneType.YZ || bestPlane == PlaneType.YZNEG) {
                px = iy;
                py = iz;
                ax = point1.y;
                ay = point1.z;
                bx = point2.y;
                by = point2.z;
                cx = point3.y;
                cy = point3.z;
            } else {
                log.error("[ERROR][intersectsAASegmentToTriangle] : Projection failed, one of the points is null.");
                return false;
            }

            double area1 = cross2D(px - ax, py - ay, bx - ax, by - ay);
            double area2 = cross2D(px - bx, py - by, cx - bx, cy - by);
            double area3 = cross2D(px - cx, py - cy, ax - cx, ay - cy);

            if (Double.isNaN(area1) || Double.isNaN(area2) || Double.isNaN(area3)) {
                return false;
            }

            boolean hasNeg = (area1 < 0) || (area2 < 0) || (area3 < 0);
            boolean hasPos = (area1 > 0) || (area2 > 0) || (area3 > 0);

            return !(hasNeg && hasPos);
        } catch (Exception e) {
            log.error("[ERROR][intersectsAASegmentToTriangle] : Exception occurred while checking intersection.", e);
            return false;
        }
    }

    private double cross2D(double ax, double ay, double bx, double by) {
        return ax * by - ay * bx;
    }

    private boolean intersectsSegment(GaiaSegment edge) {
        // Check if the bounding box intersects with the segment.
        // This is a simple AABB vs segment intersection test.
        Vector3d start = edge.getStartPoint();
        Vector3d end = edge.getEndPoint();

        // Check if both endpoints of the segment are inside the bounding box.
        if (intersectsPoint(start) || intersectsPoint(end)) {
            return true; // At least one endpoint is inside the bounding box.
        }

        // Check if the segment intersects the bounding box by checking each axis.
        double tEnter = 0.0;
        double tExit = 1.0;

        double[] minB = {minX, minY, minZ};
        double[] maxB = {maxX, maxY, maxZ};

        double[] startP = {start.x, start.y, start.z};
        double[] endP = {end.x, end.y, end.z};

        for (int i = 0; i < 3; i++) {
            if (Math.abs(endP[i] - startP[i]) < 1e-8) { // Segment is parallel to the axis
                if (startP[i] < minB[i] || startP[i] > maxB[i]) {
                    return false; // Segment is outside the bounding box
                }
            } else {
                double t1 = (minB[i] - startP[i]) / (endP[i] - startP[i]);
                double t2 = (maxB[i] - startP[i]) / (endP[i] - startP[i]);
                if (t1 > t2) {
                    double temp = t1;
                    t1 = t2;
                    t2 = temp;
                }
                tEnter = Math.max(tEnter, t1);
                tExit = Math.min(tExit, t2);
                if (tEnter > tExit) {
                    return false; // No intersection
                }
            }
        }

        // If we reach here, the segment intersects the bounding box.
        return true;
    }

    public boolean intersectsPlane(GaiaPlane plane) {
        int positiveCount = 0;
        int negativeCount = 0;
        int distanceZeroCount = 0;
        double eps = 1e-8;
        for (int i = 0; i < 8; i++) {
            double pointX = (i & 1) == 0 ? minX : maxX;
            double pointY = (i & 2) == 0 ? minY : maxY;
            double pointZ = (i & 4) == 0 ? minZ : maxZ;
            double distance = plane.distanceToPoint(pointX, pointY, pointZ);
            if (distance > eps) {
                positiveCount++;
            } else if (distance < -eps) {
                negativeCount++;
            } else {
                distanceZeroCount++;
            }

            if (positiveCount > 0 && negativeCount > 0) {
                return true; // The bounding box intersects the plane.
            }

            if (distanceZeroCount > 2) {
                return true; // some face is coplanar with the plane.
            }
        }

        return false;
    }

    public void set(GaiaBoundingBox bbox) {
        this.minX = bbox.minX;
        this.minY = bbox.minY;
        this.minZ = bbox.minZ;
        this.maxX = bbox.maxX;
        this.maxY = bbox.maxY;
        this.maxZ = bbox.maxZ;
        this.isInit = bbox.isInit;
    }

    public void set(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;
        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
        this.isInit = true;
    }

    public void addPoint(double[] xyz) {
        addPoint(xyz[0], xyz[1], xyz[2]);
    }

    public void addPoint(double x, double y, double z) {
        /*addPoint(new Vector3d(x, y, z));*/
        if (isInit) {
            if (x < minX) {
                minX = x;
            }
            if (y < minY) {
                minY = y;
            }
            if (z < minZ) {
                minZ = z;
            }
            if (x > maxX) {
                maxX = x;
            }
            if (y > maxY) {
                maxY = y;
            }
            if (z > maxZ) {
                maxZ = z;
            }
        } else {
            isInit = true;
            minX = x;
            minY = y;
            minZ = z;
            maxX = x;
            maxY = y;
            maxZ = z;
        }
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

    public GaiaBoundingBox multiplyMatrix4d(Matrix4d matrix) {
        return multiplyMatrix4d(matrix, this);
    }

    public GaiaBoundingBox multiplyMatrix4d(Matrix4d matrix, GaiaBoundingBox boundingBox) {
        GaiaBoundingBox result = new GaiaBoundingBox();
        Vector3d minPoint = new Vector3d(boundingBox.minX, boundingBox.minY, boundingBox.minZ);
        Vector3d maxPoint = new Vector3d(boundingBox.maxX, boundingBox.maxY, boundingBox.maxZ);

        // Transform the min and max points using the matrix
        matrix.transformPosition(minPoint);
        matrix.transformPosition(maxPoint);

        // Set the transformed points as the new bounding box
        result.setMinX(Math.min(minPoint.x, maxPoint.x));
        result.setMinY(Math.min(minPoint.y, maxPoint.y));
        result.setMinZ(Math.min(minPoint.z, maxPoint.z));
        result.setMaxX(Math.max(minPoint.x, maxPoint.x));
        result.setMaxY(Math.max(minPoint.y, maxPoint.y));
        result.setMaxZ(Math.max(minPoint.z, maxPoint.z));
        result.isInit = true;

        return result;
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

    public Vector3d getSize() {
        return new Vector3d(maxX - minX, maxY - minY, maxZ - minZ);
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

    public void expand(double value) {
        minX -= value;
        minY -= value;
        minZ -= value;
        maxX += value;
        maxY += value;
        maxZ += value;
    }

    public void expandXYZ(double valueX, double valueY, double valueZ) {
        minX -= valueX;
        minY -= valueY;
        minZ -= valueZ;
        maxX += valueX;
        maxY += valueY;
        maxZ += valueZ;
    }

    public boolean isBoxInside(GaiaBoundingBox box) {
        return box.getMinX() >= minX && box.getMinY() >= minY && box.getMinZ() >= minZ && box.getMaxX() <= maxX && box.getMaxY() <= maxY && box.getMaxZ() <= maxZ;
    }

    public GaiaBoundingBox createCubeFromMinPosition() {
        double maxSize = getMaxSize();
        double minX = this.minX;
        double minY = this.minY;
        double minZ = this.minZ;
        double maxX = minX + maxSize;
        double maxY = minY + maxSize;
        double maxZ = minZ + maxSize;
        return new GaiaBoundingBox(minX, minY, minZ, maxX, maxY, maxZ, true);
    }

    public List<Vector3d> getVertices() {
        return List.of(
                new Vector3d(minX, minY, minZ),
                new Vector3d(maxX, minY, minZ),
                new Vector3d(maxX, maxY, minZ),
                new Vector3d(minX, maxY, minZ),
                new Vector3d(minX, minY, maxZ),
                new Vector3d(maxX, minY, maxZ),
                new Vector3d(maxX, maxY, maxZ),
                new Vector3d(minX, maxY, maxZ)
        );
    }
}
