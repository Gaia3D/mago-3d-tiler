package com.gaia3d.converter.geotools;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class VectorCollection {
    private final List<Point> points = new ArrayList<>();
    private final List<LineString> lineStrings = new ArrayList<>();
    private final List<Polygon> polygons = new ArrayList<>();
}
