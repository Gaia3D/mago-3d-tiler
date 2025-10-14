package com.gaia3d.converter.geometry;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum GisVectorType {
    SURFACE("Surface"), // Polygon with Z
    EXTRUSION("Extrusion"), // Building Footprint
    PIPELINE("Pipeline"), // LineString
    INSTANCE("Instance"); // Point, Polygon, LineString

    private final String description;
}
