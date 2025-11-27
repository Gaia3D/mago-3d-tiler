package com.gaia3d.converter;

import com.gaia3d.command.LoggingConfiguration;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.assimp.AssimpConverterOptions;
import com.gaia3d.converter.gltf.GltfWriter;
import com.gaia3d.converter.gltf.GltfWriterOptions;
import com.gaia3d.process.postprocess.batch.GaiaBatcher;
import org.junit.jupiter.api.Test;

import java.io.File;

class AssimpGltfExporterTest {

    static {
        LoggingConfiguration.initConsoleLogger();
    }

    @Test
    void exportGlb() {
        //String inputPath = "D:\\data\\mago-3d-tiler\\assimp-sample\\tree\\LOD0-1m.glb";
        String inputPath = "D:\\data\\mago-3d-tiler\\assimp-sample\\tree\\instance-LOD0.glb";
        String outputPath = "E:\\data\\mago-server\\output\\tree\\TEST.glb";

        AssimpConverterOptions options = AssimpConverterOptions.builder().isGenerateNormals(true).isSplitByNode(false).build();
        AssimpConverter converter = new AssimpConverter(options);
        GaiaBatcher gaiaBatcher = new GaiaBatcher();

        GltfWriterOptions gltfOptions = GltfWriterOptions.builder()
                .isUseQuantization(true)
                .isDoubleSided(true)
                .build();
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setTilesVersion("1.0");
        if (globalOptions.getTilesVersion().equals("1.0")) {
            gltfOptions.setUriImage(true);
        }
        GltfWriter gltfWriter = new GltfWriter(gltfOptions);

        //globalOptions.setTilesVersion("1.1");

        AssimpGltfExporter exporter = new AssimpGltfExporter(converter, gltfWriter);

        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);
        exporter.exportGlb(inputFile, outputFile);
    }
}