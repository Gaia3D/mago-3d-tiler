package com.gaia3d.converter.jgltf.extension;

import com.gaia3d.process.postprocess.DataType;
import com.gaia3d.process.postprocess.batch.GaiaBatchTableMap;
import com.gaia3d.process.postprocess.pointcloud.PointCloudBuffer;
import com.gaia3d.process.tileprocess.tile.tileset.schema.ClassProperty;
import com.gaia3d.process.tileprocess.tile.tileset.schema.Schema;
import com.gaia3d.process.tileprocess.tile.tileset.schema.SchemaClass;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * EXT_structural_metadata
 */
@Slf4j
@Getter
@Setter
public class ExtensionStructuralMetadataMapper {
    private List<Integer> propertyAttributes;
}
