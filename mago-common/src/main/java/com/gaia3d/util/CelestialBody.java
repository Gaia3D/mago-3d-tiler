package com.gaia3d.util;

import lombok.Getter;

/**
 * Enum representing supported celestial bodies with their physical constants.
 */
@Getter
public enum CelestialBody {
    EARTH("Earth", 6378137.0,           // equatorialRadius (meters)
            6356752.3142,        // polarRadius (meters)
            6.69437999014E-3,    // firstEccentricitySquared
            "EPSG:4326"          // WGS84
    ), MOON("Moon", 1737400.0,           // equatorialRadius (meters, sphere)
            1737400.0,           // polarRadius (meters, sphere)
            0.0,                 // firstEccentricitySquared (perfect sphere)
            "IAU:30100"          // IAU 2015 Moon CRS
    );

    private final String displayName;
    private final double equatorialRadius;
    private final double polarRadius;
    private final double firstEccentricitySquared;
    private final String crsCode;

    private final double equatorialRadiusSquared;
    private final double polarRadiusSquared;

    CelestialBody(String displayName, double equatorialRadius, double polarRadius, double firstEccentricitySquared, String crsCode) {
        this.displayName = displayName;
        this.equatorialRadius = equatorialRadius;
        this.polarRadius = polarRadius;
        this.firstEccentricitySquared = firstEccentricitySquared;
        this.crsCode = crsCode;
        this.equatorialRadiusSquared = equatorialRadius * equatorialRadius;
        this.polarRadiusSquared = polarRadius * polarRadius;
    }

    public static CelestialBody fromString(String value) {
        if (value == null) {
            throw new IllegalArgumentException("Body value cannot be null");
        }
        return switch (value.trim().toLowerCase()) {
            case "earth" -> EARTH;
            case "moon" -> MOON;
            default -> throw new IllegalArgumentException("Unknown celestial body: " + value);
        };
    }

    public boolean isSphere() {
        return firstEccentricitySquared == 0.0;
    }
}
