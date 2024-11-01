package com.gaia3d.converter.kml;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

/**
 * KmlInfo is a class that contains the information of the kml file.
 * It contains the information of the kml file, and the information of the buffer of each node.
 */
@Builder
@Getter
@Setter
public class KmlInfo {
    private String name;
    private Vector3d position;
    private String altitudeMode;
    private double heading;
    private double tilt;
    private double roll;
    private String href;
    private double scaleX;
    private double scaleY;
    private double scaleZ;
}
