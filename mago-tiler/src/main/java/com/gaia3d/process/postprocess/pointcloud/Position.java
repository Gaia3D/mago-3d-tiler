package com.gaia3d.process.postprocess.pointcloud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

@Deprecated
@AllArgsConstructor
public class Position {
    @JsonProperty("byteOffset")
    int byteOffset;
}
