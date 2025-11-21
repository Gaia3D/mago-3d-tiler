package com.gaia3d.modifier;

import com.gaia3d.TilerExtensionModule;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.assimp.AssimpConverterOptions;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.basic.geometry.modifier.FlipYTexCoordinate;
import com.gaia3d.util.ImageResizer;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Getter
@Setter
@Slf4j
@NoArgsConstructor
public class TreeCreator {

    public void createTreeBillBoard(TreeBillBoardParameters treeBillBoardParameters, String inputPath, String outputPath) {
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

        TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
        int verticalPlanesCount = treeBillBoardParameters.getVerticalRectanglesCount();
        int horizontalPlanesCount = treeBillBoardParameters.getHorizontalRectanglesCount();
        tilerExtensionModule.makeBillBoard(gaiaScenes, resultGaiaScenes, verticalPlanesCount, horizontalPlanesCount);

        // rotate 90 degree to make the tree upright
        for (GaiaScene gaiaScene : resultGaiaScenes) {
            GaiaNode rootNode = gaiaScene.getNodes().get(0);
            rootNode.getTransformMatrix().rotateX(Math.toRadians(-90));
            gaiaScene.spendTransformMatrix();
        }

        int lodCount = 6;
        for (int i = 0; i < lodCount; i++) {
            if (i == 0) {
                int maxTextureSize = 1024;
                // resize the texture of materials if it is larger than maxTextureSize
                resultGaiaScenes.forEach(gaiaScene -> resizeMaterialTextures(gaiaScene, maxTextureSize));
            } else if (i == 1) {
                int maxTextureSize = 512;
                // resize the texture of materials if it is larger than maxTextureSize
                resultGaiaScenes.forEach(gaiaScene -> resizeMaterialTextures(gaiaScene, maxTextureSize));
            } else if (i == 2) {
                int maxTextureSize = 256;
                // resize the texture of materials if it is larger than maxTextureSize
                resultGaiaScenes.forEach(gaiaScene -> resizeMaterialTextures(gaiaScene, maxTextureSize));
            } else if (i == 3) {
                int maxTextureSize = 128;
                // resize the texture of materials if it is larger than maxTextureSize
                resultGaiaScenes.forEach(gaiaScene -> resizeMaterialTextures(gaiaScene, maxTextureSize));
            } else if (i == 4) {
                int maxTextureSize = 64;
                // resize the texture of materials if it is larger than maxTextureSize
                resultGaiaScenes.forEach(gaiaScene -> resizeMaterialTextures(gaiaScene, maxTextureSize));
            } else {
                int maxTextureSize = 32;
                // resize the texture of materials if it is larger than maxTextureSize
                resultGaiaScenes.forEach(gaiaScene -> resizeMaterialTextures(gaiaScene, maxTextureSize));
            }
            // as a test, save glb file.
            GlobalOptions globalOptions = GlobalOptions.getInstance();
            globalOptions.setTilesVersion("1.1");
            GaiaScene gaiaScene = resultGaiaScenes.get(0);
            GltfWriter gltfWriter = new GltfWriter();
            //String outputFilePath = outputPath + File.separator + "tree_billboard_v" + verticalPlanesCount + "h" + horizontalPlanesCount + "_L" + i + ".glb";
            String outputFilePath = outputPath + File.separator + "instance-" + i + ".glb";
            gltfWriter.writeGlb(gaiaScene, outputFilePath);
        }
    }

    private void resizeMaterialTextures(GaiaScene gaiaScene, int maxTextureSize) {
        List<GaiaMaterial> materials = gaiaScene.getMaterials();
        for (GaiaMaterial material : materials) {
            Map<TextureType, List<GaiaTexture>> textures = material.getTextures();
            if (textures != null) {
                for (Map.Entry<TextureType, List<GaiaTexture>> entry : textures.entrySet()) {
                    List<GaiaTexture> textureList = entry.getValue();
                    for (GaiaTexture texture : textureList) {
                        BufferedImage image = texture.getBufferedImage();
                        if (image != null) {
                            int width = texture.getBufferedImage().getWidth();
                            int height = texture.getBufferedImage().getHeight();
                            if (width > maxTextureSize || height > maxTextureSize) {
                                double aspect = (double) width / (double) height;
                                int newWidth;
                                int newHeight;
                                if (width > height) {
                                    newWidth = maxTextureSize;
                                    newHeight = (int) (maxTextureSize / aspect);
                                } else {
                                    newHeight = maxTextureSize;
                                    newWidth = (int) (maxTextureSize * aspect);
                                }

                                ImageResizer imageResizer = new ImageResizer();
                                BufferedImage resizedImage = imageResizer.resizeImageGraphic2D(image, newWidth, newHeight, true);
                                texture.setBufferedImage(resizedImage);
                                texture.setWidth(resizedImage.getWidth());
                                texture.setHeight(resizedImage.getHeight());
                            }
                        }
                    }
                }
            }
        }
    }
}
