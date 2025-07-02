package com.gaia3d.process.tileprocess.tile.tileset.metadata;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

@Slf4j
@Getter
@Setter
@EqualsAndHashCode
@NoArgsConstructor
public class Metadata {
    @JsonProperty("class")
    private String clazz;
    @JsonProperty("properties")
    private Map<String, Object> properties;
}
