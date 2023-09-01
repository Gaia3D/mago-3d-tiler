package com.gaia3d.converter.geometry;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;
import org.xmlobjects.gml.model.geometry.primitives.Triangle;

import java.util.List;

@Getter
@Setter
@Builder
public class GaiaBuilding {

    private String id;
    private String name;
    private double roofHeight;
    private double floorHeight;
    private GaiaBoundingBox boundingBox;

    List<Vector3d> positions;
}
