package com.gaia3d.converter.gltf.extension;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Getter
@Setter
public class PropertyAttribute {
    @JsonProperty("class")
    private String className;
    @JsonProperty("properties")
    private Map<String, Attribute> properties;
}
