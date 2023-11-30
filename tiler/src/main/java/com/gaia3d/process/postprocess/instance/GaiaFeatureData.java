package com.gaia3d.process.postprocess.instance;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GaiaFeatureData {
    @JsonProperty("RTC_CENTER")
    float[] rctCenter;

    @JsonProperty("POSITION")
    float[] position;

    @JsonProperty("RGB")
    short[] rgb;

    @JsonProperty("POSITION_QUANTIZED")
    int[] positionQuantized;

    @JsonProperty("NORMAL")
    float[] normal;
}
