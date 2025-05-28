package com.gaia3d.process.postprocess.batch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gaia3d.process.postprocess.pointcloud.ByteAddress;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GaiaBatchTable {
    @JsonProperty("INTENSITY")
    private ByteAddress intensity;
    @JsonProperty("CLASSIFICATION")
    private ByteAddress classification;
}
