package com.gaia3d.modifier;

import com.gaia3d.TilerExtensionModule;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.assimp.AssimpConverterOptions;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.process.preprocess.sub.FlipYTexCoordinate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.assimp.Assimp;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Slf4j
@NoArgsConstructor
public class TreeCreator {

    public void createTreeBillBoard(String inputPath, String outputPath) {
        // 1rst, load the tree model from the given path
        log.info("Loading tree model from path: {}", inputPath);
        AssimpConverterOptions options = AssimpConverterOptions.builder()
                .isSplitByNode(false)
                .build();
        AssimpConverter assimpConverter = new AssimpConverter(options);
        List<GaiaScene> gaiaScenes = assimpConverter.load(inputPath);
        List<GaiaScene> resultGaiaScenes = new ArrayList<>();

        // Flip Y tex-coordinates
        FlipYTexCoordinate flipYTexCoordinate = new FlipYTexCoordinate();
        gaiaScenes.forEach(flipYTexCoordinate::flip);

        TreeBillBoardParameters treeBillBoardParameters = new TreeBillBoardParameters();

        TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
        tilerExtensionModule.makeBillBoard(gaiaScenes, resultGaiaScenes);

        // as a test, save glb file.
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setTilesVersion("1.1");
        GaiaScene gaiaScene = resultGaiaScenes.get(0);
        GltfWriter gltfWriter = new GltfWriter();
        String outputFilePath = outputPath + File.separator + "tree_billboard.glb";
        gltfWriter.writeGlb(gaiaScene, outputFilePath);
        int hola = 0;
    }
}
