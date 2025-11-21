package com.gaia3d.process.tileprocess.tile.tileset.schema;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClassProperty {
    @JsonProperty("description")
    private String description;

    @JsonProperty("name")
    private String name;

    @JsonProperty("type")
    private String type;

    @JsonProperty("componentType")
    private String componentType;

    @JsonProperty("required")
    private Boolean required;
}