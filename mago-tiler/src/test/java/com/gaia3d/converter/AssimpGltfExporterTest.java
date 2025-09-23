package com.gaia3d.converter;

import com.gaia3d.command.Configuration;
import com.gaia3d.command.mago.GlobalConstants;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.assimp.AssimpConverterOptions;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.process.postprocess.batch.GaiaBatcher;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

class AssimpGltfExporterTest {

    static {
        Configuration.initConsoleLogger();
    }

    @Test
    void exportGlb() {
        String inputPath = "D:\\data\\mago-3d-tiler\\assimp-sample\\tree\\LOD0-1m.glb";
        //String inputPath = "D:\\data\\mago-3d-tiler\\assimp-sample\\tree\\LOD0-1m-test.glb";
        String outputPath = "E:\\data\\mago-server\\output\\tree\\LOD0-1m.glb";

        AssimpConverterOptions options = AssimpConverterOptions.builder()
                .isGenerateNormals(true)
                .isSplitByNode(false)
                .build();
        AssimpConverter converter = new AssimpConverter(options);
        GaiaBatcher gaiaBatcher = new GaiaBatcher();
        GltfWriter gltfWriter = new GltfWriter();

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setTilesVersion("1.0");
        //globalOptions.setTilesVersion("1.1");

        AssimpGltfExporter exporter = new AssimpGltfExporter(converter, gltfWriter);

        File inputFile = new File(inputPath);
        File outputFile = new File(outputPath);
        exporter.exportGlb(inputFile, outputFile);



    }
}