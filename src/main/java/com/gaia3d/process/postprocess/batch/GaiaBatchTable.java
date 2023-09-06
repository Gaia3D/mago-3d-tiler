package com.gaia3d.process.postprocess.batch;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class GaiaBatchTable {
    @JsonProperty("ProjectName")
    private final List<String> proejctName = new ArrayList<>();
    @JsonProperty("NodeName")
    private final List<String> nodeName = new ArrayList<>();
    @JsonProperty("GeometricError")
    private final List<Double> geometricError = new ArrayList<>();
    @JsonProperty("BatchId")
    private final List<String> batchId = new ArrayList<>();
    @JsonProperty("Height")
    private final List<Double> height = new ArrayList<>();
}
