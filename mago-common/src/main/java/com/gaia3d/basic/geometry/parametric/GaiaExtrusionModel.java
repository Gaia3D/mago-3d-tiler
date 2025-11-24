package com.gaia3d.basic.geometry.parametric;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.types.Classification;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector3d;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Slf4j
@Getter
@Setter
@Builder
public class GaiaExtrusionModel implements Serializable {
    private String id;
    private Classification classification;
    private double roofHeight;
    private double floorHeight;
    private GaiaBoundingBox boundingBox;
    private String originalFilePath;

    private List<Vector3d> positions;
    private Map<String, String> properties;
}
