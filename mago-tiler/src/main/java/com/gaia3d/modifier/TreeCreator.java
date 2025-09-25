package com.gaia3d.modifier;

import com.gaia3d.TilerExtensionModule;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.assimp.AssimpConverterOptions;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.lwjgl.assimp.Assimp;

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

        TreeBillBoardParameters treeBillBoardParameters = new TreeBillBoardParameters();

        TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
        tilerExtensionModule.makeBillBoard(gaiaScenes, resultGaiaScenes);
        int hola = 0;
    }
}
