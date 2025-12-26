package com.gaia3d.process.postprocess.instance;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
public class GaiaFeatureTable {
    @JsonProperty("BATCH_LENGTH")
    private Integer batchLength;

    @JsonProperty("RTC_CENTER")
    private Double[] rtcCenter = new Double[3];

    /* Instanced3DModel */
    @JsonProperty("INSTANCES_LENGTH")
    private Integer instancesLength;
    @JsonProperty("EAST_NORTH_UP")
    private Boolean eastNorthUp;
    @JsonProperty("NORMAL_RIGHT")
    private ByteAddress normalRight;
    @JsonProperty("NORMAL_UP")
    private ByteAddress normalUp;
    @JsonProperty("SCALE")
    private ByteAddress scale;

    /* PointCloud */
    // POSITION = POSITION_QUANTIZED * QUANTIZED_VOLUME_SCALE / 65535.0 + QUANTIZED_VOLUME_OFFSET
    @JsonProperty("POINTS_LENGTH")
    private Integer pointsLength;
    @JsonProperty("POSITION_QUANTIZED")
    private ByteAddress positionQuantized;
    @JsonProperty("QUANTIZED_VOLUME_SCALE")
    private Float[] quantizedVolumeScale;
    @JsonProperty("QUANTIZED_VOLUME_OFFSET")
    private Float[] quantizedVolumeOffset;

    @JsonProperty("POSITION")
    private ByteAddress position;
    @JsonProperty("RGB")
    private ByteAddress color;
    @JsonProperty("BATCH_ID")
    private ByteAddress batchId;

    @JsonIgnore
    private Instanced3DModelBinary instancedBuffer;
}
