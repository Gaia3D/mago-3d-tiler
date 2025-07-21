package com.gaia3d.converter.jgltf.extension;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
@Getter
@Setter
public class Property {
    @JsonProperty("name")
    private String name;

    /* bufferView Address */
    @JsonProperty("values")
    private int values;

    /* bufferView Address */
    @JsonProperty("stringOffsets")
    private int stringOffsets;

    @JsonIgnore
    private List<String> primaryValues;
}
