package gltf;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeatureTable {
    @JsonProperty("BATCH_LENGTH")
    int batchLength;
    @JsonProperty("RTC_CENTER")
    float[] rctCenter;
}
