package com.gaia3d.process.tileprocess.tile.tileset.schema;

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
public class SchemaClass {
    private String name;
    private String description;
    private Map<String, ClassProperty> properties;
}
