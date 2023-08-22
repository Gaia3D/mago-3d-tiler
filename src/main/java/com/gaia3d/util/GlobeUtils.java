package com.gaia3d.util;

import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

/**
 * Utility class for converting between geographic and cartesian coordinates.
 * @author znkim
 * @since 1.0.0
 */
public class GlobeUtils {
    private static final double degToRadFactor = 0.017453292519943296d; // 3.141592653589793 / 180.0;
    private static final double equatorialRadius = 6378137.0d; // meters.
    private static final double equatorialRadiusSquared = 40680631590769.0d;
    private static final double polarRadius = 6356752.3142d; // meters.
    private static final double polarRadiusSquared = 40408299984087.05552164d;
    private static final double firstEccentricitySquared = 6.69437999014E-3d;

    private static final CRSFactory factory = new CRSFactory();
    private static final CoordinateReferenceSystem wgs84 = factory.createFromParameters("WGS84", "+proj=longlat +datum=WGS84 +no_defs");

    public static double[] geographicToCartesianWgs84(double longitude, double latitude, double altitude) {
        double[] result = new double[3];
        double lonRad = longitude * degToRadFactor;
        double latRad = latitude * degToRadFactor;
        double cosLon = Math.cos(lonRad);
        double cosLat = Math.cos(latRad);
        double sinLon = Math.sin(lonRad);
        double sinLat = Math.sin(latRad);
        double e2 = firstEccentricitySquared;
        double v = equatorialRadius / Math.sqrt(1.0 - e2 * sinLat * sinLat);
        result[0] = (v + altitude) * cosLat * cosLon;
        result[1] = (v + altitude) * cosLat * sinLon;
        result[2] = (v * (1.0 - e2) + altitude) * sinLat;
        return result;
    }

    public static Vector3d geographicToCartesianWgs84(Vector3d position) {
        double[] result = geographicToCartesianWgs84(position.x, position.y, position.z);
        return new Vector3d(result[0], result[1], result[2]);
    }

    public static Matrix4d normalAtCartesianPointWgs84(double x, double y, double z) {
        Vector3d zAxis = new Vector3d(x / equatorialRadiusSquared, y / equatorialRadiusSquared, z / polarRadiusSquared);
        zAxis.normalize();
        Vector3d xAxis = new Vector3d(-y, +x, 0.0);
        xAxis.normalize();
        Vector3d yAxis = zAxis.cross(xAxis, new Vector3d());
        yAxis.normalize();

        double[] transfrom = new double[16];
        transfrom[0] = xAxis.x();
        transfrom[1] = xAxis.y();
        transfrom[2] = xAxis.z();
        transfrom[3] = 0.0f;

        transfrom[4] = yAxis.x();
        transfrom[5] = yAxis.y();
        transfrom[6] = yAxis.z();
        transfrom[7] = 0.0f;

        transfrom[8] = zAxis.x();
        transfrom[9] = zAxis.y();
        transfrom[10] = zAxis.z();
        transfrom[11] = 0.0f;

        transfrom[12] = x;
        transfrom[13] = y;
        transfrom[14] = z;
        transfrom[15] = 1.0f;

        Matrix4d transfromMatrix = new Matrix4d();
        transfromMatrix.set(transfrom);
        return transfromMatrix;
    }

    public static Matrix4d normalAtCartesianPointWgs84(Vector3d position) {
        return normalAtCartesianPointWgs84(position.x, position.y, position.z);
    }

    public static Vector3d cartesianToGeographicWgs84(Vector3d position) {
        double x = position.x;
        double y = position.y;
        double z = position.z;

        double xxpyy = x * x + y * y;
        double sqrtXXpYY = Math.sqrt(xxpyy);
        double a = equatorialRadius;
        double ra2 = 1.0 / (a * a);
        double e2 = firstEccentricitySquared;
        double e4 = e2 * e2;
        double p = xxpyy * ra2;
        double q = z * z * (1.0 - e2) * ra2;
        double r = (p + q - e4) / 6.0;

        //double r = 1.0 / 6.0 * (p - q - e4);
        double evoluteBorderTest = 8 * r * r * r + e4 * p * q;

        double h, phi, u, v, w, k, D, sqrtDDpZZ, e, lambda, s2, rad1, rad2, rad3, atan;

        if (evoluteBorderTest > 0.0 || q != 0.0) {
            if (evoluteBorderTest > 0) {
                rad1 = Math.sqrt(evoluteBorderTest);
                rad2 = Math.sqrt(e4 * p * q);
                if (evoluteBorderTest > 10 * e2) {
                    rad3 = Math.cbrt((rad1 + rad2) * (rad1 + rad2));
                    u = r + 0.5 * rad3 + 2 * r * r / rad3;
                } else {
                    u = r + 0.5 * Math.cbrt((rad1 + rad2) * (rad1 + rad2)) + 0.5 * Math.cbrt((rad1 - rad2) * (rad1 - rad2));
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
        /*double[] result = new double[3];
        result[0] = factor * lambda;
        result[1] = factor * phi;
        result[2] = h;*/
        return new Vector3d(factor * lambda, factor * phi, h);
    }

    /*public static ProjCoordinate transform(CoordinateReferenceSystem source, CoordinateReferenceSystem target, ProjCoordinate coordinate) {
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, target);
        ProjCoordinate result = new ProjCoordinate();
        transformer.transform(coordinate, result);
        return result;
    }*/

    public static ProjCoordinate transform(CoordinateReferenceSystem source, ProjCoordinate coordinate) {
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, wgs84);
        ProjCoordinate result = new ProjCoordinate();
        transformer.transform(coordinate, result);
        return result;
    }
}
