package com.gaia3d.basic.geometry;

import com.gaia3d.basic.geometry.entities.GaiaPlane;
import com.gaia3d.basic.geometry.entities.GaiaSegment;
import com.gaia3d.basic.geometry.entities.GaiaTriangle;
import com.gaia3d.basic.halfedge.PlaneType;
import com.gaia3d.util.GeometryUtils;
import com.gaia3d.util.GlobeUtils;
import com.gaia3d.util.VectorUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Vector2d;
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

    public double getMaxRadius() {
        Vector3d center = getCenter();
        Vector3d minPosition = getMinPosition();
        double radiusX = Math.abs(center.x - minPosition.x);
        double radiusY = Math.abs(center.y - minPosition.y);
        double radiusZ = Math.abs(center.z - minPosition.z);
        return Math.sqrt(radiusX * radiusX + radiusY * radiusY + radiusZ * radiusZ);
    }

    public boolean intersectsPoint(Vector3d point) {
        // Check if the point is inside the bounding box.
        return !(point.x < minX) && !(point.x > maxX) && !(point.y < minY) && !(point.y > maxY) && !(point.z < minZ) && !(point.z > maxZ);
    }

    public boolean intersectsTriangle(GaiaTriangle triangle) {
        // Check if the bounding box intersects with the triangle.
        // This is a simple AABB vs triangle intersection test.
        GaiaBoundingBox triangleBbox = triangle.getBoundingBox();
        if (!this.intersects(triangleBbox)) {
            return false; // No intersection if bounding boxes do not intersect.
        }

        // Check if the barycentric coordinates of the triangle are inside the bounding box.
        Vector3d barycenter = triangle.getBarycenter();
        if (intersectsPoint(barycenter)) {
            return true; // The barycenter of the triangle is inside the bounding box.
        }

        // Check if some vertices of the triangle are inside the bounding box.
        Vector3d[] trianglePoints = triangle.getPoints();
        for (Vector3d point : trianglePoints) {
            if (intersectsPoint(point)) {
                return true; // At least one vertex is inside the bounding box.
            }
        }

        // Check the distance of the bbox center to the triangle plane.
        double maxRadius = getMaxRadius();
        GaiaPlane trianglePlane = triangle.getPlane();
        if (trianglePlane == null) {
            log.info("[INFO][intersectsTriangle] : Triangle plane is null.");
            return false; // No valid triangle plane to check against.
        }
        Vector3d center = getCenter();
        double distanceToPlane = trianglePlane.distanceToPoint(center);
        if (Math.abs(distanceToPlane) > maxRadius) {
            return false; // The bounding box is too far from the triangle plane.
        }

        // Check if the plane intersects the bounding box.
        if (!intersectsPlane(trianglePlane)) {
            return false; // The bounding box does not intersect the triangle plane.
        }

        // Check if some edges of the triangle intersect the bounding box.
        GaiaSegment[] triangleEdges = triangle.getSegments();
        for (GaiaSegment edge : triangleEdges) {
            if (this.intersectsSegment(edge)) {
                return true; // At least one edge intersects the bounding box.
            }
        }

        // Check if some edges of the bounding box intersect the triangle.
        if (intersectsAASegmentsToTriangle(triangle)) {
//            if (!this.intersects(triangleBbox)) {
//                intersectsAASegmentsToTriangle(triangle);
//                int hola = 0;
//            }
            return true; // At least one axis-aligned segment intersects the triangle.
        }

        return false;
    }

    private boolean intersectsAASegmentsToTriangle(GaiaTriangle triangle) {
        GaiaPlane trianglePlane = triangle.getPlane();
        if (trianglePlane == null) {
            log.info("[INFO][intersectsAASegmentsToTriangle] : Triangle plane is null.");
            return false; // No valid triangle plane to check against.
        }

        Vector3d normal = trianglePlane.getNormal();
        PlaneType bestPlane = GeometryUtils.getBestPlaneToProject(normal);
        if (bestPlane == null) {
            log.error("[ERROR][intersectsAASegmentToTriangle] : Best plane is null.");
            return false; // No valid plane to project onto.
        }

        // axis X
        GaiaSegment aaSegment1 = new GaiaSegment(new Vector3d(minX, minY, minZ), new Vector3d(maxX, minY, minZ));
        if (intersectsAASegmentToTriangle(triangle, trianglePlane, aaSegment1, bestPlane, 0)) {
            return true; // Intersection found with the first segment.
        }
        GaiaSegment aaSegment2 = new GaiaSegment(new Vector3d(minX, maxY, minZ), new Vector3d(maxX, maxY, minZ));
        if (intersectsAASegmentToTriangle(triangle, trianglePlane, aaSegment2, bestPlane, 0)) {
            return true; // Intersection found with the second segment.
        }
        GaiaSegment aaSegment3 = new GaiaSegment(new Vector3d(minX, minY, maxZ), new Vector3d(maxX, minY, maxZ));
        if (intersectsAASegmentToTriangle(triangle, trianglePlane, aaSegment3, bestPlane, 0)) {
            return true; // Intersection found with the third segment.
        }
        GaiaSegment aaSegment4 = new GaiaSegment(new Vector3d(minX, minY, maxZ), new Vector3d(maxX, minY, maxZ));
        if (intersectsAASegmentToTriangle(triangle, trianglePlane, aaSegment4, bestPlane, 0)) {
            return true; // Intersection found with the fourth segment.
        }

        // axis Y
        GaiaSegment aaSegment5 = new GaiaSegment(new Vector3d(minX, minY, minZ), new Vector3d(minX, maxY, minZ));
        if (intersectsAASegmentToTriangle(triangle, trianglePlane, aaSegment5, bestPlane, 1)) {
            return true; // Intersection found with the first segment.
        }
        GaiaSegment aaSegment6 = new GaiaSegment(new Vector3d(maxX, minY, minZ), new Vector3d(maxX, maxY, minZ));
        if (intersectsAASegmentToTriangle(triangle, trianglePlane, aaSegment6, bestPlane, 1)) {
            return true; // Intersection found with the second segment.
        }
        GaiaSegment aaSegment7 = new GaiaSegment(new Vector3d(minX, minY, maxZ), new Vector3d(minX, maxY, maxZ));
        if (intersectsAASegmentToTriangle(triangle, trianglePlane, aaSegment7, bestPlane, 1)) {
            return true; // Intersection found with the third segment.
        }
        GaiaSegment aaSegment8 = new GaiaSegment(new Vector3d(maxX, minY, maxZ), new Vector3d(maxX, maxY, maxZ));
        if (intersectsAASegmentToTriangle(triangle, trianglePlane, aaSegment8, bestPlane, 1)) {
            return true; // Intersection found with the fourth segment.
        }

        // axis Z
        GaiaSegment aaSegment9 = new GaiaSegment(new Vector3d(minX, minY, minZ), new Vector3d(minX, minY, maxZ));
        if (intersectsAASegmentToTriangle(triangle, trianglePlane, aaSegment9, bestPlane, 2)) {
            return true; // Intersection found with the first segment.
        }
        GaiaSegment aaSegment10 = new GaiaSegment(new Vector3d(maxX, minY, minZ), new Vector3d(maxX, minY, maxZ));
        if (intersectsAASegmentToTriangle(triangle, trianglePlane, aaSegment10, bestPlane, 2)) {
            return true; // Intersection found with the second segment.
        }
        GaiaSegment aaSegment11 = new GaiaSegment(new Vector3d(minX, maxY, minZ), new Vector3d(minX, maxY, maxZ));
        if (intersectsAASegmentToTriangle(triangle, trianglePlane, aaSegment11, bestPlane, 2)) {
            return true; // Intersection found with the third segment.
        }
        GaiaSegment aaSegment12 = new GaiaSegment(new Vector3d(maxX, maxY, minZ), new Vector3d(maxX, maxY, maxZ));
        if (intersectsAASegmentToTriangle(triangle, trianglePlane, aaSegment12, bestPlane, 2)) {
            return true; // Intersection found with the fourth segment.
        }

        return false; // No intersection found with any segment.
    }

    private boolean intersectsAASegmentToTriangle(GaiaTriangle triangle, GaiaPlane trianglePlane, GaiaSegment aaSegment, PlaneType bestPlane, int axis) {
        // This method checks if an axis-aligned segment intersects with a triangle defined by its normal and D value.
        // The triangle is defined in the form: normal.x * x + normal.y * y + normal.z * z + D = 0
        try {
            Vector3d intersectionPoint = trianglePlane.intersectionAASegment(aaSegment, axis);
            if (intersectionPoint == null) {
                return false; // No intersection with the triangle plane.
            }

//            double dist1 = trianglePlane.distanceToPoint(aaSegment.getStartPoint());
//            double dist2 = trianglePlane.distanceToPoint(aaSegment.getEndPoint());
//            if(dist1>0 && dist2>0 || dist1<0 && dist2<0) {
//                intersectionPoint = trianglePlane.intersectionAASegment(aaSegment, axis);
//                int hola = 0;
//            }

            Vector3d[] trianglePoints = triangle.getPoints();

            Vector2d p = null;
            Vector2d a = null;
            Vector2d b = null;
            Vector2d c = null;
            if (bestPlane == PlaneType.XY || bestPlane == PlaneType.XYNEG) {
                // Project the intersection point onto the XY plane.
                p = new Vector2d(intersectionPoint.x, intersectionPoint.y);
                a = new Vector2d(trianglePoints[0].x, trianglePoints[0].y);
                b = new Vector2d(trianglePoints[1].x, trianglePoints[1].y);
                c = new Vector2d(trianglePoints[2].x, trianglePoints[2].y);
            } else if (bestPlane == PlaneType.XZ || bestPlane == PlaneType.XZNEG) {
                // Project the intersection point onto the XZ plane.
                p = new Vector2d(intersectionPoint.x, intersectionPoint.z);
                a = new Vector2d(trianglePoints[0].x, trianglePoints[0].z);
                b = new Vector2d(trianglePoints[1].x, trianglePoints[1].z);
                c = new Vector2d(trianglePoints[2].x, trianglePoints[2].z);
            } else if (bestPlane == PlaneType.YZ || bestPlane == PlaneType.YZNEG) {
                // Project the intersection point onto the YZ plane.
                p = new Vector2d(intersectionPoint.y, intersectionPoint.z);
                a = new Vector2d(trianglePoints[0].y, trianglePoints[0].z);
                b = new Vector2d(trianglePoints[1].y, trianglePoints[1].z);
                c = new Vector2d(trianglePoints[2].y, trianglePoints[2].z);
            }


            if (p == null) {
                log.error("[ERROR][intersectsAASegmentToTriangle] : Projection failed, one of the points is null.");
                return false; // Projection failed, one of the points is null.
            }

            Vector2d pSubA = new Vector2d(p).sub(a);
            Vector2d pSubB = new Vector2d(p).sub(b);
            Vector2d pSubC = new Vector2d(p).sub(c);
            Vector2d bSubA = new Vector2d(b).sub(a);
            Vector2d cSubB = new Vector2d(c).sub(b);
            Vector2d aSubC = new Vector2d(a).sub(c);

            double area1 = VectorUtils.cross(pSubA, bSubA);
            double area2 = VectorUtils.cross(pSubB, cSubB);
            double area3 = VectorUtils.cross(pSubC, aSubC);

            if (Double.isNaN(area1) || Double.isNaN(area2) || Double.isNaN(area3)) {
                //log.error("[ERROR][intersectsAASegmentToTriangle] : Area calculation resulted in NaN.");
                return false; // Area calculation resulted in NaN, cannot determine intersection.
            }

            boolean hasNeg = (area1 < 0) || (area2 < 0) || (area3 < 0);
            boolean hasPos = (area1 > 0) || (area2 > 0) || (area3 > 0);

            return !(hasNeg && hasPos);
        } catch (Exception e) {
            log.error("[ERROR][intersectsAASegmentToTriangle] : Exception occurred while checking intersection.", e);
            return false; // An exception occurred, cannot determine intersection.
        }
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
        // 1rst, check the 8 points of the bounding box against the plane.
        // If there are some points on one side of the plane and some on the other side, then the bounding box intersects the plane.
        int positiveCount = 0;
        int negativeCount = 0;
        int distanceZeroCount = 0;
        double eps = 1e-8;
        for (int i = 0; i < 8; i++) {
            Vector3d point = new Vector3d(
                    (i & 1) == 0 ? minX : maxX,
                    (i & 2) == 0 ? minY : maxY,
                    (i & 4) == 0 ? minZ : maxZ
            );

            double distance = plane.distanceToPoint(point);
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
