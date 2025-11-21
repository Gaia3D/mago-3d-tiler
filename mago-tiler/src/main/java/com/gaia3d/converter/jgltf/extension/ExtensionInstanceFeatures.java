package com.gaia3d.converter.jgltf.extension;

import com.gaia3d.process.postprocess.instance.GaiaFeatureTable;
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
public class ExtensionInstanceFeatures {
    private List<FeatureId> featureIds;

    public static ExtensionInstanceFeatures fromBatchTable(GaiaFeatureTable featureTable) {
        ExtensionInstanceFeatures instanceFeatures = new ExtensionInstanceFeatures();

        int count = featureTable.getInstancesLength();

        /*int count = batchTableMap.values().stream()
                .mapToInt(List::size)
                .min().orElse(0);*/

        List<FeatureId> featureIds = new ArrayList<>();

        FeatureId featureId = new FeatureId();
        featureId.setFeatureCount(count);
        featureId.setAttribute(0); // _FEATURE_ID_0
        featureId.setPropertyTable(0);
        featureIds.add(featureId);
        instanceFeatures.setFeatureIds(featureIds);

        return instanceFeatures;
    }
}
