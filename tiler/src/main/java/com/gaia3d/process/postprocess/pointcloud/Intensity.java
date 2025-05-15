package com.gaia3d.process.postprocess.pointcloud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class Intensity {

    @JsonProperty("byteOffset")
    int byteOffset;

    @JsonProperty("componentType")
    String componentType;

    @JsonProperty("type")
    String type;
}
