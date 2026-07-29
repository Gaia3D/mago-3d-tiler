package com.gaia3d.converter.kml;

import lombok.*;
import org.joml.Vector3d;

import java.util.Map;

/**
 * KmlInfo is a class that contains the information of the kml file.
 * It contains the information of the kml file, and the information of the buffer of each node.
 */
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TileTransformInfo {
    private String name;

    // geographic coordinate in degrees (longitude, latitude, altitude)
    // Translation vector in meters (absolute, clampedToGround, relativeToGround)
    private Vector3d position;
    private String altitudeMode;

    // Orientation in degrees
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
