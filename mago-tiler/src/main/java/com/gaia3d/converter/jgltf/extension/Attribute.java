package com.gaia3d.converter.jgltf.extension;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
public class Attribute {
    @JsonProperty("attribute")
    private String attribute;
}
