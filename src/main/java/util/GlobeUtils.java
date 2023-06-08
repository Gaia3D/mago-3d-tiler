package util;

import org.joml.Matrix4d;
import org.joml.Vector3d;
import org.locationtech.proj4j.BasicCoordinateTransform;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.ProjCoordinate;

public class GlobeUtils {

    private static final double degToRadFactor = 0.017453292519943296d; // 3.141592653589793 / 180.0;

    private static final double equatorialRadius = 6378137.0d; // meters.
    private static final double equatorialRadiusSquared = 40680631590769.0d;
    private static final double polarRadius = 6356752.3142d; // meters.
    private static final double polarRadiusSquared = 40408299984087.05552164d;


    private static final double firstEccentricitySquared = 6.69437999014E-3d;

    private static final CRSFactory factory = new CRSFactory();
    private static final CoordinateReferenceSystem wgs84 = factory.createFromParameters("WGS84", "+proj=longlat +datum=WGS84 +no_defs");
    private static final CoordinateReferenceSystem grs80 = factory.createFromParameters("EPSG:5186", "+proj=tmerc +lat_0=38 +lon_0=127 +k=1 +x_0=200000 +y_0=600000 +ellps=GRS80 +units=m +no_defs");

    /**
     * convert world coordinate to wgs84
     */
    public static double[] geographicToCartesianWgs84(double longitude, double latitude, double altitude) {
        double[] result = new double[3];
        double lonRad = longitude * degToRadFactor;
        double latRad = latitude * degToRadFactor;
        double cosLon = Math.cos(lonRad);
        double cosLat = Math.cos(latRad);
        double sinLon = Math.sin(lonRad);
        double sinLat = Math.sin(latRad);
        double a = equatorialRadius;
        double e2 = firstEccentricitySquared;
        double v = a / Math.sqrt(1.0 - e2 * sinLat * sinLat);
        double h = altitude;
        result[0] = (v + h) * cosLat * cosLon;
        result[1] = (v + h) * cosLat * sinLon;
        result[2] = (v * (1.0 - e2) + h) * sinLat;
        return result;
    }


    /**
     *
     */
    public static Matrix4d normalAtCartesianPointWgs84(double x, double y, double z) {
        Vector3d zAxis = new Vector3d(x / equatorialRadiusSquared, y / equatorialRadiusSquared, z / polarRadiusSquared);
        zAxis.normalize();
        Vector3d xAxis = new Vector3d(-y, x, 0.0);
        xAxis.normalize();
        Vector3d yAxis = zAxis.cross(xAxis, new Vector3d());
        yAxis.normalize();

        //        double[] result = new double[3];
        //        result[0] = normalResult.x();
        //        result[1] = normalResult.y();
        //        result[2] = normalResult.z();
        //        return result;


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

    public static ProjCoordinate transform(CoordinateReferenceSystem source, ProjCoordinate beforeCoord) {
        if (source == null) {
            source = grs80; // for Test
        }
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, wgs84);
        ProjCoordinate result = new ProjCoordinate();
        transformer.transform(beforeCoord, result);
        return result;
    }

    public static ProjCoordinate transform(CoordinateReferenceSystem source, CoordinateReferenceSystem target, ProjCoordinate beforeCoord) {
        BasicCoordinateTransform transformer = new BasicCoordinateTransform(source, target);
        ProjCoordinate result = new ProjCoordinate();
        transformer.transform(beforeCoord, result);
        return result;
    }
}
