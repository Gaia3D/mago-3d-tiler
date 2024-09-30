package com.gaia3d.util;

import lombok.extern.slf4j.Slf4j;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.proj4j.*;
import org.opengis.geometry.Geometry;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * Utility class for converting between geographic and cartesian coordinates.
 * @author znkim
 * @since 1.0.0
 */
@Slf4j
public class GlobeUtils {
    public static final double DEGREE_TO_RADIAN_FACTOR = 0.017453292519943296d; // 3.141592653589793 / 180.0;
    public static final double EQUATORIAL_RADIUS = 6378137.0d;
    public static final double EQUATORIAL_RADIUS_SQUARED = 40680631590769.0d;
    public static final double POLAR_RADIUS = 6356752.3142d;
    public static final double POLAR_RADIUS_SQUARED = 40408299984087.05552164d;
    public static final double FIRST_ECCENTRICITY_SQUARED = 6.69437999014E-3d;
    private static final CRSFactory factory = new CRSFactory();
    private static final CoordinateReferenceSystem wgs84 = factory.createFromParameters("WGS84", "+proj=longlat +datum=WGS84 +no_defs");

    public static double[] geographicToCartesianWgs84(double longitude, double latitude, double altitude) {
        double[] result = new double[3];
        double lonRad = longitude * DEGREE_TO_RADIAN_FACTOR;
        double latRad = latitude * DEGREE_TO_RADIAN_FACTOR;
        double cosLon = Math.cos(lonRad);
        double cosLat = Math.cos(latRad);
        double sinLon = Math.sin(lonRad);
        double sinLat = Math.sin(latRad);
        double e2 = FIRST_ECCENTRICITY_SQUARED;
        double v = EQUATORIAL_RADIUS / Math.sqrt(1.0 - e2 * sinLat * sinLat);
        result[0] = (v + altitude) * cosLat * cosLon;
        result[1] = (v + altitude) * cosLat * sinLon;
        result[2] = (v * (1.0 - e2) + altitude) * sinLat;
        return result;
    }

    public static Vector3d geographicToCartesianWgs84(Vector3d position) {
        double[] result = geographicToCartesianWgs84(position.x, position.y, position.z);
        return new Vector3d(result[0], result[1], result[2]);
    }

    public static Matrix4d transformMatrixAtCartesianPointWgs84(double x, double y, double z) {
        Vector3d zAxis = normalAtCartesianPointWgs84(x, y, z);
        Vector3d xAxis = new Vector3d(-y, +x, 0.0);
        xAxis.normalize();
        Vector3d yAxis = zAxis.cross(xAxis, new Vector3d());
        yAxis.normalize();

        double[] transform = new double[16];
        transform[0] = xAxis.x();
        transform[1] = xAxis.y();
        transform[2] = xAxis.z();
        transform[3] = 0.0f;

        transform[4] = yAxis.x();
        transform[5] = yAxis.y();
        transform[6] = yAxis.z();
        transform[7] = 0.0f;

        transform[8] = zAxis.x();
        transform[9] = zAxis.y();
        transform[10] = zAxis.z();
        transform[11] = 0.0f;

        transform[12] = x;
        transform[13] = y;
        transform[14] = z;
        transform[15] = 1.0f;

        Matrix4d transformMatrix = new Matrix4d();
        transformMatrix.set(transform);
        return transformMatrix;
    }

    public static Vector3d normalAtCartesianPointWgs84(Vector3d cartesian) {
        return normalAtCartesianPointWgs84(cartesian.x, cartesian.y, cartesian.z);
    }

    public static Vector3d normalAtCartesianPointWgs84(double x, double y, double z) {
        Vector3d zAxis = new Vector3d(x / EQUATORIAL_RADIUS_SQUARED, y / EQUATORIAL_RADIUS_SQUARED, z / POLAR_RADIUS_SQUARED);
        zAxis.normalize();
        return zAxis;
    }

    public static Matrix4d transformMatrixAtCartesianPointWgs84(Vector3d position) {
        return transformMatrixAtCartesianPointWgs84(position.x, position.y, position.z);
    }

    public static Vector3d cartesianToGeographicWgs84(Vector3d position) {
        double x = position.x;
        double y = position.y;
        double z = position.z;

        double xxpyy = x * x + y * y;
        double sqrtXXpYY = Math.sqrt(xxpyy);
        double a = EQUATORIAL_RADIUS;
        double ra2 = 1.0 / (a * a);
        double e2 = FIRST_ECCENTRICITY_SQUARED;
        double e4 = e2 * e2;
        double p = xxpyy * ra2;
        double q = z * z * (1.0 - e2) * ra2;
        double r = (p + q - e4) / 6.0;

        double evoluteBorderTest = 8 * r * r * r + e4 * p * q;
        double h, phi, u, v, w, k, D, sqrtDDpZZ, e, lambda, s2, rad1, rad2, rad3, atan;

        if (evoluteBorderTest > 0.0 || q != 0.0) {
            if (evoluteBorderTest > 0) {
                rad1 = Math.sqrt(evoluteBorderTest);
                rad2 = Math.sqrt(e4 * p * q);
                double cbrt = Math.cbrt((rad1 + rad2) * (rad1 + rad2));
                if (evoluteBorderTest > 10 * e2) {
                    u = r + 0.5 * cbrt + 2 * r * r / cbrt;
                } else {
                    u = r + 0.5 * cbrt + 0.5 * Math.cbrt((rad1 - rad2) * (rad1 - rad2));
                }
            } else {
                rad1 = Math.sqrt(-evoluteBorderTest);
                rad2 = Math.sqrt(-8 * r * r * r);
                rad3 = Math.sqrt(e4 * p * q);
                atan = 2 * Math.atan2(rad3, rad1 + rad2) / 3;
                u = -4 * r * Math.sin(atan) * Math.cos(Math.PI / 6 + atan);
            }

            v = Math.sqrt(u * u + e4 * q);
            w = e2 * (u + v - q) / (2 * v);
            k = (u + v) / (Math.sqrt(w * w + u + v) + w);
            D = k * sqrtXXpYY / (k + e2);
            sqrtDDpZZ = Math.sqrt(D * D + z * z);

            h = (k + e2 - 1) * sqrtDDpZZ / k;
            phi = 2 * Math.atan2(z, sqrtDDpZZ + D);
        } else {
            rad1 = Math.sqrt(1 - e2);
            rad2 = Math.sqrt(e2 - p);
            e = Math.sqrt(e2);
            h = -a * rad1 * rad2 / e;
            phi = rad2 / (e * rad2 + rad1 * Math.sqrt(p));
        }

        s2 = Math.sqrt(2);
        if ((s2 - 1) * y < sqrtXXpYY + x) {
            lambda = 2 * Math.atan2(y, sqrtXXpYY + x);
        } else if (sqrtXXpYY + y < (s2 + 1) * x) {
            lambda = -Math.PI * 0.5 + 2 * Math.atan2(x, sqrtXXpYY - y);
        } else {
            lambda = Math.PI * 0.5 - 2 * Math.atan2(x, sqrtXXpYY + y);
        }

        double factor = 180.0 / Math.PI;
        return new Vector3d(factor * lambda, factor * phi, h);
    }

    public static ProjCoordinate transform(CoordinateReferenceSystem source, ProjCoordinate coordinate) {
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, wgs84);
        ProjCoordinate result = new ProjCoordinate();
        transformer.transform(coordinate, result);
        return result;
    }

    public static Coordinate transformOnGeotools(org.opengis.referencing.crs.CoordinateReferenceSystem source, Coordinate coordinate) {
        try {
            org.opengis.referencing.crs.CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");

            MathTransform transform = CRS.findMathTransform(source, wgs84, false);
            Coordinate result = JTS.transform(coordinate, coordinate, transform);
            return result;
        } catch (FactoryException | TransformException e) {
            log.error("Failed to transform coordinate", e);
            throw new RuntimeException(e);
        }
    }
}
