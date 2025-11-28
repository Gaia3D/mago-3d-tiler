package com.gaia3d.util.geographic;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import org.joml.Vector3d;

/**
 * GeographicTilingScheme implements a tiling scheme based on geographic coordinates (latitude and longitude).
 * It divides the world into tiles at various levels of detail.
 */
public class GeographicTilingScheme {
    public static final double MIN_LONGITUDE = -180.0;
    public static final double MAX_LONGITUDE = 180.0;
    public static final double MIN_LATITUDE = -90.0;
    public static final double MAX_LATITUDE = 90.0;
    public static final double LON_RANGE = MAX_LONGITUDE - MIN_LONGITUDE; // 360
    public static final double LAT_RANGE = MAX_LATITUDE - MIN_LATITUDE;   // 180

    public Vector3d cartesianToGeographic(Vector3d cartesian) {
        double x = cartesian.x;
        double y = cartesian.y;
        double z = cartesian.z;

        double longitude = Math.toDegrees(Math.atan2(y, x));
        double hypotenuse = Math.sqrt(x * x + y * y);
        double latitude = Math.toDegrees(Math.atan2(z, hypotenuse));

        return new Vector3d(longitude, latitude, 0);
    }

    public Vector3d geographicToCartesian(Vector3d geographic) {
        double longitude = Math.toRadians(geographic.x);
        double latitude = Math.toRadians(geographic.y);

        double cosLat = Math.cos(latitude);
        double x = cosLat * Math.cos(longitude);
        double y = cosLat * Math.sin(longitude);
        double z = Math.sin(latitude);

        return new Vector3d(x, y, z);
    }

    /**
     * (lat, lon) 좌표를 타일 좌표(level, x, y)로 변환한다.
     * y=0 이 북쪽, y 증가할수록 남쪽으로 내려간다.
     *
     * @param level tile level (0 ~ maxLevel)
     * @param latitude 위도(-90 ~ 90)
     * @param longitude 경도(-180 ~ 180)
     */
    public TileCoordinate positionToTile(int level, double latitude, double longitude) {
        int numX = getNumberOfXTilesAtLevel(level);
        int numY = getNumberOfYTilesAtLevel(level);

        // 경도 정규화: [-180,180] -> [0,1)
        double lonClamped = clamp(longitude, MIN_LONGITUDE, MAX_LONGITUDE);
        double u = (lonClamped - MIN_LONGITUDE) / LON_RANGE; // 0 ~ 1

        // 위도 정규화: [-90,90] -> [0,1], MIN_LAT=남, MAX_LAT=북
        double latClamped = clamp(latitude, MIN_LATITUDE, MAX_LATITUDE);
        double vSouthUp = (latClamped - MIN_LATITUDE) / LAT_RANGE; // 남=0, 북=1

        // y=0이 북쪽이 되도록 뒤집기 (Cesium 기준)
        double vNorthDown = 1.0 - vSouthUp; // 북=0, 남=1

        int x = (int) Math.floor(u * numX);
        int y = (int) Math.floor(vNorthDown * numY);

        // 경계값 처리 (lon=180, lat=-90 일 때 마지막 타일에 들어가도록)
        if (x == numX) x = numX - 1;
        if (y == numY) y = numY - 1;

        return new TileCoordinate(level, x, y);
    }

    /**
     * 주어진 타일(level, x, y)의 위경도 경계를 반환한다.
     * y=0 이 북쪽, y 증가할수록 남쪽으로 내려간다.
     */
    public GaiaRectangle tileToBounds(TileCoordinate tile) {
        return tileToBounds(tile.level, tile.x, tile.y);
    }

    public GaiaRectangle tileToBounds(int level, int x, int y) {
        int numX = getNumberOfXTilesAtLevel(level);
        int numY = getNumberOfYTilesAtLevel(level);

        // 경도 분할: MIN_LONGITUDE -> MAX_LONGITUDE 방향으로 균등 분할
        double lonWidth = LON_RANGE / numX;
        double minLon = MIN_LONGITUDE + lonWidth * x;
        double maxLon = minLon + lonWidth;

        // 위도 분할:
        //   y=0 이 북극 근처, y 증가할수록 남쪽
        //   전체 [MIN_LAT, MAX_LAT] 를 numY 개로 나눴다고 보면,
        //   index 0 이 [maxLat - latHeight, maxLat] 구간이 된다.
        double latHeight = LAT_RANGE / numY;

        double maxLat = MAX_LATITUDE - latHeight * y;
        double minLat = maxLat - latHeight;

        return new GaiaRectangle(minLat, maxLat, minLon, maxLon);
    }

    /**
     * 주어진 level에서 X 방향 타일 개수 (경도 방향).
     */
    public int getNumberOfXTilesAtLevel(int level) {
        return 1 << level;
    }

    /**
     * 주어진 level에서 Y 방향 타일 개수 (위도 방향).
     */
    public int getNumberOfYTilesAtLevel(int level) {
        return 1 << level;
    }

    private static double clamp(double value, double min, double max) {
        if (value < min) return min;
        if (value > max) return max;
        return value;
    }
}
