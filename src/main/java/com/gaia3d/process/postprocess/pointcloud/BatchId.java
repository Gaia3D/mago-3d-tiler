
package com.gaia3d.process.postprocess.pointcloud;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class BatchId {
    @JsonProperty("byteOffset")
    int byteOffset;

    @JsonProperty("componentType")
    String componentType;
}
