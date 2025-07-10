package com.gaia3d.converter.kml;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

import java.util.Map;

/**
 * KmlInfo is a class that contains the information of the kml file.
 * It contains the information of the kml file, and the information of the buffer of each node.
 */
@Builder
@Getter
@Setter
public class TileTransformInfo {
    private String name;

    // Position in WGS84 coordinates (longitude, latitude, altitude in meters)
    private Vector3d position;
    // Translation vector in meters (absolute, clampedToGround, relativeToGround)
    private String altitudeMode;

    // Orientation
    private double heading;
    private double tilt;
    private double roll;

    // Scale Factors
    private double scaleX;
    private double scaleY;
    private double scaleZ;

    private String href;
    private Map<String, String> properties;
}
