package com.gaia3d.command.mago;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class AttributeFilter {
    private final String attributeName;
    private final String attributeValue;
}
