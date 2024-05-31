package com.gaia3d.converter.geometry;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector3d;

import java.util.List;

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

    List<Vector3d> positions;
}
