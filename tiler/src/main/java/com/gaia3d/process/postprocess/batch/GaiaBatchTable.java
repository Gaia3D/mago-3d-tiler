package com.gaia3d.process.postprocess.batch;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@RequiredArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GaiaBatchTable {
    @JsonProperty("ID")
    private final List<String> uuid = new ArrayList<>();
    @JsonProperty("Name")
    private final List<String> name = new ArrayList<>();
    @JsonProperty("FileName")
    private final List<String> fileName = new ArrayList<>();
    @JsonProperty("NodeName")
    private final List<String> nodeName = new ArrayList<>();
    @JsonProperty("BatchId")
    private final List<String> batchId = new ArrayList<>();
}
