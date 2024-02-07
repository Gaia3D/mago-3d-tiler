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
    @JsonProperty("INSTANCES_LENGTH")
    int instancesLength;
    @JsonProperty("POINTS_LENGTH")
    int pointsLength;

    @JsonProperty("RTC_CENTER")
    float[] rctCenter;

    /* Instanced3DModel */
    @JsonProperty("EAST_NORTH_UP")
    boolean eastNorthUp;
    @JsonProperty("NORMAL_RIGHT")
    Normal normalRight;
    @JsonProperty("NORMAL_UP")
    Normal normalUp;
    @JsonProperty("SCALE")
    Scale scale;

    /* PointCloud */
    @JsonProperty("POSITION")
    Position position;
    @JsonProperty("RGB")
    Color color;
    @JsonProperty("BATCH_ID")
    BatchId batchId;
}
