package com.gaia3d.modifier.billboard;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.modifier.texcoord.FlipYTexCoordinate;
import com.gaia3d.basic.geometry.modifier.transform.GaiaBaker;
import com.gaia3d.basic.geometry.modifier.transform.GaiaScaler;
import com.gaia3d.basic.geometry.modifier.transform.GaiaScalerOptions;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.LoggingConfiguration;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.assimp.AssimpConverterOptions;
import com.gaia3d.converter.gltf.GltfWriter;
import com.gaia3d.converter.gltf.GltfWriterOptions;
import com.gaia3d.modifier.billboard.merge.MergeConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.logging.log4j.Level;
import org.joml.Vector3d;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@Slf4j
class BillboardCloudCreatorTest {

    static {
        LoggingConfiguration.initConsoleLogger();
        LoggingConfiguration.setLevel(Level.INFO);
    }

    @Test
    void resizeModel() {
        //String inputPath = "D:\\data\\mago-3d-tiler\\build-sample\\sample-tree\\broad.glb";
        //String inputPath = "D:\\data\\mago-3d-tiler\\build-sample\\sample-tree\\bamboo.glb";
        //String inputPath = "D:\\data\\mago-3d-tiler\\build-sample\\sample-tree\\needle.glb";
        String inputPath = "D:\\data\\mago-3d-tiler\\build-sample\\sample-tree\\mixed.glb";
        String outputPath = "H:\\workspace\\billboardclouds-output";
        rescaleScene(inputPath, outputPath);
    }

    @Test
    void createBillboardCloudSingle() {
        //String inputPath = "D:\\data\\mago-3d-tiler\\build-sample\\sample-tree\\original.glb";
        String inputPath = "D:\\data\\mago-3d-tiler\\build-sample\\sample-tree\\sample.glb";
        String outputPath = "H:\\workspace\\billboardclouds-output";
        List<BillboardCloudOptions> lods =  new ArrayList<>();

        MergeConfig mergeConfigA = new MergeConfig();
        mergeConfigA.setMinNormalDot(Math.cos(Math.toRadians(50)));
        mergeConfigA.setMaxCenterDistance(0.5);
        mergeConfigA.setMinEfficiency(0.2);
        mergeConfigA.setMaxThickness(0.05);
        mergeConfigA.setMaxRectGap(0.05);
        mergeConfigA.prepare();

        BillboardCloudOptions billboardCloudOptions0 = BillboardCloudOptions.builder()
                .normalDotThreshold(Math.cos(Math.toRadians(50)))
                .planeDistanceEpsilon(0.5)
                .maxPlaneExtent(0.5)
                .setRadius(0.5)
                .maxSplitDepth(1)
                .minClusterFaceCount(1)
                .minTriangleArea(1e-6)
                .maxBillboardPlaneCount(64)
                .quadOverlapRatio(0.03)
                .maximumTextureSize(128)
                .blendTexture(true)
                .mergeConfig(mergeConfigA)
                .build();
        lods.add(billboardCloudOptions0);

        convertBC(lods, inputPath, outputPath);
    }

    @Test
    void createBillboardCloud() {
        //String inputPath = "D:\\data\\mago-3d-tiler\\build-sample\\sample-tree\\original.glb";
        //String inputPath = "D:\\data\\mago-3d-tiler\\build-sample\\sample-tree\\hwal-sample.glb";
        //String inputPath = "D:\\data\\mago-3d-tiler\\build-sample\\sample-tree\\coconut_palm.glb";
        String inputPath = "D:\\data\\mago-3d-tiler\\build-sample\\sample-tree\\coconut_palm-cloud-lod0-original.glb";
        String outputPath = "H:\\workspace\\billboardclouds-output";
        List<BillboardCloudOptions> lods =  new ArrayList<>();

        MergeConfig mergeConfigA = new MergeConfig();
        mergeConfigA.setMinNormalDot(Math.cos(Math.toRadians(50)));
        mergeConfigA.setMaxCenterDistance(0.5);
        mergeConfigA.setMinEfficiency(0.2);
        mergeConfigA.setMaxThickness(0.05);
        mergeConfigA.setMaxRectGap(0.05);
        mergeConfigA.prepare();

        MergeConfig mergeConfigB = new MergeConfig();
        mergeConfigB.setMinNormalDot(Math.cos(Math.toRadians(50)));
        mergeConfigB.setMaxCenterDistance(0.5);
        mergeConfigB.setMinEfficiency(0.2);
        mergeConfigB.setMaxThickness(0.05);
        mergeConfigB.setMaxRectGap(0.05);
        mergeConfigB.prepare();

        MergeConfig mergeConfigC = new MergeConfig();
        mergeConfigC.setMinNormalDot(Math.cos(Math.toRadians(50)));
        mergeConfigC.setMaxCenterDistance(0.5);
        mergeConfigC.setMinEfficiency(0.2);
        mergeConfigC.setMaxThickness(0.05);
        mergeConfigC.setMaxRectGap(0.05);
        mergeConfigC.prepare();

        MergeConfig mergeConfigD = new MergeConfig();
        mergeConfigD.setMinNormalDot(Math.cos(Math.toRadians(50)));
        mergeConfigD.setMaxCenterDistance(0.5);
        mergeConfigD.setMinEfficiency(0.2);
        mergeConfigD.setMaxThickness(0.05);
        mergeConfigD.setMaxRectGap(0.05);
        mergeConfigD.prepare();

        BillboardCloudOptions billboardCloudOptions0 = BillboardCloudOptions.builder()
                .normalDotThreshold(Math.cos(Math.toRadians(45)))
                .planeDistanceEpsilon(0.5)
                .maxPlaneExtent(0.5)
                .setRadius(0.5)
                .maxSplitDepth(1)
                .minClusterFaceCount(1)
                .minTriangleArea(1e-6)
                .maxBillboardPlaneCount(256)
                .quadOverlapRatio(0.03)
                .maximumTextureSize(64)
                .blendTexture(true)
                .mergeConfig(mergeConfigA)
                .build();
        lods.add(billboardCloudOptions0);

        BillboardCloudOptions billboardCloudOptions1 = BillboardCloudOptions.builder()
                .normalDotThreshold(Math.cos(Math.toRadians(45)))
                .planeDistanceEpsilon(0.5)
                .maxPlaneExtent(0.5)
                .setRadius(0.5)
                .maxSplitDepth(1)
                .minClusterFaceCount(1)
                .minTriangleArea(1e-6)
                .maxBillboardPlaneCount(128)
                .quadOverlapRatio(0.03)
                .maximumTextureSize(64)
                .blendTexture(true)
                .mergeConfig(mergeConfigA)
                .build();
        lods.add(billboardCloudOptions1);

        BillboardCloudOptions billboardCloudOptions2 = BillboardCloudOptions.builder()
                .normalDotThreshold(Math.cos(Math.toRadians(45)))
                .planeDistanceEpsilon(0.5)
                .maxPlaneExtent(0.5)
                .setRadius(0.5)
                .maxSplitDepth(1)
                .minClusterFaceCount(1)
                .minTriangleArea(1e-6)
                .maxBillboardPlaneCount(96)
                .quadOverlapRatio(0.04)
                .maximumTextureSize(64)
                .blendTexture(true)
                .mergeConfig(mergeConfigC)
                .build();
        lods.add(billboardCloudOptions2);

        BillboardCloudOptions billboardCloudOptions3 = BillboardCloudOptions.builder()
                .normalDotThreshold(Math.cos(Math.toRadians(45)))
                .planeDistanceEpsilon(0.5)
                .maxPlaneExtent(0.5)
                .setRadius(0.5)
                .maxSplitDepth(1)
                .minClusterFaceCount(1)
                .minTriangleArea(1e-6)
                .maxBillboardPlaneCount(64)
                .quadOverlapRatio(0.05)
                .maximumTextureSize(64)
                .blendTexture(true)
                .mergeConfig(mergeConfigD)
                .build();
        lods.add(billboardCloudOptions3);

        convertBC(lods, inputPath, outputPath);
    }

    private void rescaleScene(String inputPath, String outputPath) {
        GaiaScene scene = prepareScene(inputPath, true);
        File inputFile = new File(inputPath);
        String inputFileName = inputFile.getName();
        File outputFile = new File(outputPath, inputFileName.replace(".glb", "_resized.glb"));
        writeGlb(scene, outputFile.getAbsolutePath());
    }

    private void convertBC(List<BillboardCloudOptions> lods, String inputPath, String outputPath) {
        GaiaScene scene = prepareScene(inputPath, false);
        File inputFile = new File(inputPath);
        String inputFileName = inputFile.getName();
        File outputFile = new File(outputPath, inputFileName.replace(".glb", "-bc.glb"));
        File outputTempPath = new  File(outputPath, "temp");

        int index = 0;
        for (BillboardCloudOptions lod : lods) {
            lod.setTempPath(outputTempPath.getAbsolutePath());
            File name = new File(outputFile.getAbsolutePath().replace(".glb", "-lod" + index + ".glb"));
            AbstractBillboardCloudCreator creator = new ImprovedBillboardCloudCreator(lod);
            GaiaScene billboardCloud = creator.create(scene);
            writeGlb(billboardCloud, name.getAbsolutePath());
            index++;
        }
    }

    private GaiaScene prepareScene(String inputPath, boolean flipY) {
        // 1rst, load the tree model from the given path
        log.info("Loading tree model from path: {}", inputPath);
        AssimpConverterOptions options = AssimpConverterOptions.builder()
                .isSplitByNode(false)
                .build();
        AssimpConverter assimpConverter = new AssimpConverter(options);
        List<GaiaScene> gaiaScenes = assimpConverter.load(inputPath);

        // Flip Y tex-coordinates
        if (flipY) {
            FlipYTexCoordinate flipYTexCoordinate = new FlipYTexCoordinate();
            gaiaScenes.forEach(flipYTexCoordinate::flip);
        }

        //List<GaiaScene> resultGaiaScenes = new ArrayList<>();
        //TilerExtensionModule tilerExtensionModule = new TilerExtensionModule();
        //int verticalPlanesCount = treeBillBoardParameters.getVerticalRectanglesCount();
        //int horizontalPlanesCount = treeBillBoardParameters.getHorizontalRectanglesCount();
        //tilerExtensionModule.makeBillBoard(gaiaScenes, resultGaiaScenes, verticalPlanesCount, horizontalPlanesCount);

        // rotate 90 degree to make the tree upright
        for (GaiaScene gaiaScene : gaiaScenes) {
            //GaiaNode rootNode = gaiaScene.getNodes().get(0);
            //rootNode.getTransformMatrix().rotateX(Math.toRadians(-90));

            GaiaBaker baker = new GaiaBaker();
            baker.apply(gaiaScene);

            GaiaBoundingBox bbox = gaiaScene.updateBoundingBox();
            Vector3d volume = bbox.getVolume();
            double maxDimension = Math.max(volume.x, Math.max(volume.y, volume.z));
            GaiaScalerOptions scalerOptions = GaiaScalerOptions.builder()
                    .scaleX(1.0 / maxDimension)
                    .scaleY(1.0 / maxDimension)
                    .scaleZ(1.0 / maxDimension)
                    .build();
            GaiaScaler scaler = new GaiaScaler(scalerOptions);
            scaler.apply(gaiaScene);
        }
        return gaiaScenes.getFirst();
    }

    private void writeGlb(GaiaScene scene, String outputPath) {
        GltfWriterOptions options = GltfWriterOptions.builder()
                .isDoubleSided(true)
                //.isUseQuantization(true)
                //.isUseShortTexCoord(true)
                .build();

        GltfWriter gltfWriter = new GltfWriter(options);
        gltfWriter.writeGlb(scene, outputPath);
    }
}
