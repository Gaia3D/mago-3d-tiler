package com.gaia3d.process.postprocess.instance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.gaia3d.process.postprocess.pointcloud.BatchId;
import com.gaia3d.process.postprocess.pointcloud.Color;
import com.gaia3d.process.postprocess.pointcloud.Position;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GaiaFeatureTable {
    @JsonProperty("BATCH_LENGTH")
    int batchLength;
    @JsonProperty("RTC_CENTER")
    float[] rctCenter;
    @JsonProperty("POINTS_LENGTH")
    int pointsLength;
    @JsonProperty("POSITION")
    Position position;
    @JsonProperty("RGB")
    Color color;
    @JsonProperty("BATCH_ID")
    BatchId batchId;
}
