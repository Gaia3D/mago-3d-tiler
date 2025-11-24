package com.gaia3d.converter.gltf;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ExtensionConstant {
    EX_3DTILES_CONTENT_GLTF("3DTILES_content_gltf"),
    // EXT_structural_metadata
    STRUCTURAL_METADATA("EXT_structural_metadata"),
    // EXT_feature_metadata
    MESH_FEATURES("EXT_mesh_features"),
    // EXT_feature_metadata_schema
    FEATURE_METADATA_SCHEMA("EXT_feature_metadata_schema"),
    // EXT_instance_features
    INSTANCE_FEATURES("EXT_instance_features"),
    // EXT_mesh_gpu_instancing
    MESH_GPU_INSTANCING("EXT_mesh_gpu_instancing"),
    // EXT_meshopt_compression
    MESHOPT_COMPRESSION("EXT_meshopt_compression"),
    // KHR_mesh_quantization
    MESH_QUANTIZATION("KHR_mesh_quantization"),
    // KHR_materials_unlit
    MATERIAL_UNLIT("KHR_materials_unlit");

    private final String extensionName;
}
