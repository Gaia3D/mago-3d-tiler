package com.gaia3d.converter.gltf.extension;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Getter
@Setter
public class ExtensionMeshGpuInstancing {

    // "TRANSLATION": 0,
    // "ROTATION": 1,
    // "SCALE": 2,
    // "_FEATURE_ID_0": 3
    @JsonProperty("attributes")
    private Map<String, Integer> attributes = new HashMap<>();

}
