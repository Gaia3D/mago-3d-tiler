package com.gaia3d.converter.gltf.extension;

import com.gaia3d.process.postprocess.batch.GaiaBatchTableMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * EXT_mesh_features
 */
@Slf4j
@Getter
@Setter
public class ExtensionMeshFeatures {
    private List<FeatureId> featureIds;

    public static ExtensionMeshFeatures fromBatchTable(GaiaBatchTableMap<String, List<String>> batchTableMap) {
        ExtensionMeshFeatures meshFeatures = new ExtensionMeshFeatures();

        int count = batchTableMap.values().stream()
                .mapToInt(List::size)
                .min().orElse(0);

        List<FeatureId> featureIds = new ArrayList<>();

        FeatureId featureId = new FeatureId();
        featureId.setFeatureCount(count);
        featureId.setAttribute(0); // Assuming attribute is 0 for simplicity
        //featureId.setAttribute("_FEATURE_ID_0");
        featureId.setPropertyTable(0); // Assuming property table is 0 for simplicity
        featureIds.add(featureId);
        meshFeatures.setFeatureIds(featureIds);

        return meshFeatures;
    }
}
