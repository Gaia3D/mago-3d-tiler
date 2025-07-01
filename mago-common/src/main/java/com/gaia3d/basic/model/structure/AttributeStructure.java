package com.gaia3d.basic.model.structure;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Getter
@Setter
public abstract class AttributeStructure implements Serializable {
    protected UUID identifier = UUID.randomUUID();
    protected String fileName = "unknown";
    protected String nodeName = "unknown";
    protected Map<String, String> attributes = new HashMap<>();
}
