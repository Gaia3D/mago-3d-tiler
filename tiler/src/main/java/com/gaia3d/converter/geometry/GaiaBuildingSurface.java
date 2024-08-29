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
public class GaiaBuildingSurface {
    private String id;
    private String name;
    private Classification classification;

    private List<Vector3d> exteriorPositions;
    private List<List<Vector3d>> interiorPositions;

    private GaiaBoundingBox boundingBox;
    private Map<String, String> properties;
}
