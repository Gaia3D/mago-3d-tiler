package com.gaia3d.converter;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.process.postprocess.batch.GaiaBatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class AssimpGltfExporter {

    private final AssimpConverter assimpConverter;
    private final GaiaBatcher gaiaBatcher;
    private final GltfWriter gltfWriter;

    public void exportGlb(File inputFile, File outputFile) {
        List<GaiaScene> scenes = assimpConverter.load(inputFile);
        if (scenes.isEmpty()) {
            log.warn("No scenes found in the input file: {}", inputFile.getAbsolutePath());
            return;
        } else {
            log.info("Single scene found in the input file: {}", inputFile.getAbsolutePath());
            GaiaScene scene = scenes.get(0);
            GaiaSet set = GaiaSet.fromGaiaScene(scene);


            gltfWriter.writeGlb(scene, outputFile);
        }
    }

    public void exportGltf(File inputFile, File outputFile) {
        List<GaiaScene> scenes = assimpConverter.load(inputFile);
        if (scenes.isEmpty()) {
            log.warn("No scenes found in the input file: {}", inputFile.getAbsolutePath());
            return;
        } else {
            log.info("Single scene found in the input file: {}", inputFile.getAbsolutePath());
            GaiaScene scene = scenes.get(0);
            gltfWriter.writeGltf(scene, outputFile);
        }
    }
}
