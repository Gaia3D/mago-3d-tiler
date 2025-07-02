package com.gaia3d.converter.jgltf;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Getter
@Setter
public class PropertyTable {
    @JsonProperty("class")
    private String className;
    @JsonProperty("count")
    private int count;
    @JsonProperty("properties")
    private Map<String, Property> properties;
}
