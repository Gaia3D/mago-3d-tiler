package com.gaia3d.converter.jgltf.extension;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MaterialUnlit {
    @JsonProperty("name")
    private String name;
}
