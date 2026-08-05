package com.gaia3d.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CelestialBodyTest {

    @Test
    void testEarthConstants() {
        CelestialBody earth = CelestialBody.EARTH;
        assertEquals(6378137.0, earth.getEquatorialRadius(), 1e-1);
        assertEquals(6356752.3142, earth.getPolarRadius(), 1e-1);
        assertEquals(6.69437999014E-3, earth.getFirstEccentricitySquared(), 1e-15);
        assertEquals("EPSG:4326", earth.getCrsCode());
        assertFalse(earth.isSphere());
    }

    @Test
    void testMoonConstants() {
        CelestialBody moon = CelestialBody.MOON;
        assertEquals(1737400.0, moon.getEquatorialRadius(), 1e-1);
        assertEquals(1737400.0, moon.getPolarRadius(), 1e-1);
        assertEquals(0.0, moon.getFirstEccentricitySquared(), 1e-15);
        assertEquals("IAU:30100", moon.getCrsCode());
        assertTrue(moon.isSphere());
    }

    @Test
    void testDerivedConstants() {
        CelestialBody moon = CelestialBody.MOON;
        assertEquals(1737400.0 * 1737400.0, moon.getEquatorialRadiusSquared(), 1e0);
        assertEquals(1737400.0 * 1737400.0, moon.getPolarRadiusSquared(), 1e0);
    }

    @Test
    void testFromStringEarth() {
        assertEquals(CelestialBody.EARTH, CelestialBody.fromString("earth"));
        assertEquals(CelestialBody.EARTH, CelestialBody.fromString("Earth"));
        assertEquals(CelestialBody.EARTH, CelestialBody.fromString("EARTH"));
    }

    @Test
    void testFromStringMoon() {
        assertEquals(CelestialBody.MOON, CelestialBody.fromString("moon"));
        assertEquals(CelestialBody.MOON, CelestialBody.fromString("Moon"));
        assertEquals(CelestialBody.MOON, CelestialBody.fromString("MOON"));
    }

    @Test
    void testFromStringInvalid() {
        assertThrows(IllegalArgumentException.class, () -> CelestialBody.fromString("mars"));
        assertThrows(IllegalArgumentException.class, () -> CelestialBody.fromString(null));
        assertThrows(IllegalArgumentException.class, () -> CelestialBody.fromString(""));
    }
}
