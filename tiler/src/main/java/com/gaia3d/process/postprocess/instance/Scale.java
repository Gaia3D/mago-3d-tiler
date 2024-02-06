package com.gaia3d.process.postprocess.instance;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class Scale {

    @JsonProperty("byteOffset")
    int byteOffset;
}
