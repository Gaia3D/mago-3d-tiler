package com.gaia3d.util;

import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobeUtilsBodyTest {

    private static final double TOLERANCE = 1e-3;

    @Test
    void testEarthParameterizedMatchesWgs84() {
        double lon = 126.977, lat = 37.566, alt = 100.0;
        double[] wgs84 = GlobeUtils.geographicToCartesianWgs84(lon, lat, alt);
        double[] param = GlobeUtils.geographicToCartesian(lon, lat, alt, CelestialBody.EARTH);

        assertEquals(wgs84[0], param[0], TOLERANCE);
        assertEquals(wgs84[1], param[1], TOLERANCE);
        assertEquals(wgs84[2], param[2], TOLERANCE);
    }

    @Test
    void testMoonCartesianAtEquator() {
        // At lon=0, lat=0, alt=0 on a sphere: result should be [R, 0, 0]
        double[] result = GlobeUtils.geographicToCartesian(0, 0, 0, CelestialBody.MOON);
        assertEquals(1737400.0, result[0], TOLERANCE);
        assertEquals(0.0, result[1], TOLERANCE);
        assertEquals(0.0, result[2], TOLERANCE);
    }

    @Test
    void testMoonCartesianAtNorthPole() {
        // At lon=0, lat=90, alt=0 on a sphere: result should be [0, 0, R]
        double[] result = GlobeUtils.geographicToCartesian(0, 90, 0, CelestialBody.MOON);
        assertEquals(0.0, result[0], TOLERANCE);
        assertEquals(0.0, result[1], TOLERANCE);
        assertEquals(1737400.0, result[2], TOLERANCE);
    }

    @Test
    void testMoonCartesianRoundTrip() {
        double lon = 45.0, lat = 30.0, alt = 500.0;
        double[] cartesian = GlobeUtils.geographicToCartesian(lon, lat, alt, CelestialBody.MOON);
        Vector3d geographic = GlobeUtils.cartesianToGeographic(cartesian[0], cartesian[1], cartesian[2], CelestialBody.MOON);

        assertEquals(lon, geographic.x, 1e-6);
        assertEquals(lat, geographic.y, 1e-6);
        assertEquals(alt, geographic.z, 1e-1);
    }

    @Test
    void testEarthCartesianRoundTrip() {
        double lon = -73.9857, lat = 40.7484, alt = 443.0;
        double[] cartesian = GlobeUtils.geographicToCartesian(lon, lat, alt, CelestialBody.EARTH);
        Vector3d geographic = GlobeUtils.cartesianToGeographic(cartesian[0], cartesian[1], cartesian[2], CelestialBody.EARTH);

        assertEquals(lon, geographic.x, 1e-6);
        assertEquals(lat, geographic.y, 1e-6);
        assertEquals(alt, geographic.z, 1e-1);
    }

    @Test
    void testMoonRadiusConstantForSphere() {
        // On a sphere, radius at any latitude should be the same
        double r0 = GlobeUtils.radiusAtLatitudeRad(0.0, CelestialBody.MOON);
        double r45 = GlobeUtils.radiusAtLatitudeRad(Math.PI / 4.0, CelestialBody.MOON);
        double r90 = GlobeUtils.radiusAtLatitudeRad(Math.PI / 2.0, CelestialBody.MOON);

        assertEquals(1737400.0, r0, TOLERANCE);
        assertEquals(1737400.0, r45, TOLERANCE);
        assertEquals(1737400.0, r90, TOLERANCE);
    }

    @Test
    void testMoonNormalOnSphereIsNormalized() {
        // On a sphere, the normal at any point should be normalize(x, y, z)
        double lon = 30.0, lat = 60.0, alt = 0.0;
        double[] cart = GlobeUtils.geographicToCartesian(lon, lat, alt, CelestialBody.MOON);
        Vector3d normal = GlobeUtils.normalAtCartesianPoint(cart[0], cart[1], cart[2], CelestialBody.MOON);

        // The normal on a sphere should point in the same direction as the position vector
        Vector3d pos = new Vector3d(cart[0], cart[1], cart[2]).normalize();
        assertEquals(pos.x, normal.x, 1e-10);
        assertEquals(pos.y, normal.y, 1e-10);
        assertEquals(pos.z, normal.z, 1e-10);
    }

    @Test
    void testGetRadiusAtLatitudeParameterized() {
        double lat = 45.0;
        double earthRadius = GlobeUtils.getRadiusAtLatitude(lat);
        double earthRadiusParam = GlobeUtils.getRadiusAtLatitude(lat, CelestialBody.EARTH);
        assertEquals(earthRadius, earthRadiusParam, TOLERANCE);

        double moonRadius = GlobeUtils.getRadiusAtLatitude(lat, CelestialBody.MOON);
        assertEquals(1737400.0, moonRadius, TOLERANCE);
    }
}
