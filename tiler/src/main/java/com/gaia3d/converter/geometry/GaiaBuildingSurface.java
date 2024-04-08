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
public class GaiaBuildingSurface {
    private String id;
    private String name;
    private Classification classification;
    List<Vector3d> positions;
    private GaiaBoundingBox boundingBox;
}
