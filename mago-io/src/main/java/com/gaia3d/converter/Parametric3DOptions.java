package com.gaia3d.converter;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import java.util.List;

@Getter
@Setter
@Builder
public class Parametric3DOptions {
    private List<AttributeFilter> attributeFilters;

    private String heightColumnName;
    private String altitudeColumnName;
    private String diameterColumnName;
    private String scaleColumnName;
    private String densityColumnName;
    private String headingColumnName;

    private double absoluteAltitudeValue;
    private double minimumHeightValue;
    private double skirtHeight;

    private double defaultDiameter;
    private double defaultHeight;
    private double defaultScale;
    private double defaultDensity;
    private double defaultHeading;

    private CoordinateReferenceSystem sourceCrs;
    private CoordinateReferenceSystem targetCrs;

    private boolean flipCoordinate;
}
