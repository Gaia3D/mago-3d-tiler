package com.gaia3d.converter.geometry;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class GaiaExtrusionBuilding {
    private String id;
    private String name;
    private Classification classification;
    private double roofHeight;
    private double floorHeight;
    private GaiaBoundingBox boundingBox;
    private String originalFilePath;

    private List<Vector3d> positions;
    private Map<String, String> properties;
}
