package com.gaia3d.converter.gltf.extension;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * EXT_structural_metadata
 */
@Slf4j
@Getter
@Setter
public class ExtensionStructuralMetadataMapper {
    private List<Integer> propertyAttributes;
}
