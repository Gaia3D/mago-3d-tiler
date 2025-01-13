package com.gaia3d.processPhR.tileProcessPhR;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.DecimateParameters;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.Configurator;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.process.postprocess.batch.GaiaBatcher;
import com.gaia3d.process.preprocess.GaiaRotator;
import com.gaia3d.process.preprocess.GaiaTranslatorExact;
import com.gaia3d.process.tileprocess.tile.LevelOfDetail;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.junit.jupiter.api.Test;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Slf4j
class Batched3DModelTilerPhRTest {

    @Test
    void decimate() {
        Configurator.initConsoleLogger();
        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem crs = crsFactory.createFromName("EPSG:5187");
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setInputFormat(FormatType.OBJ);
        globalOptions.setOutputFormat(FormatType.B3DM);
        globalOptions.setRotateX(-90);
        globalOptions.setCrs(crs);

        DecimateParameters decimateParameters = new DecimateParameters();
        decimateParameters.setBasicValues(12.0, 0.5, 0.9, 15.0, 1000000, 1, 1.2);

        // read
        GaiaAttribute gaiaAttribute = new GaiaAttribute();
        gaiaAttribute.setAttributes(new HashMap<>());
        AssimpConverter assimpConverter = new AssimpConverter();
        List<GaiaScene> gaiaScenes = assimpConverter.load("D:/workspace/input/BlockBAYX.obj");
        GaiaScene scene = gaiaScenes.get(0);
        scene.setAttribute(gaiaAttribute);

        TileInfo preTileInfo = TileInfo.builder()
                .scenePath(Path.of("D:/workspace/input/BlockBABB.obj"))
                .scene(scene)
                .build();

        GaiaRotator rotator = new GaiaRotator();
        GaiaTranslatorExact translatorExact = new GaiaTranslatorExact(new ArrayList<>());
        rotator.run(preTileInfo);
        translatorExact.run(preTileInfo);
        GaiaSet gaiaSet = GaiaSet.fromGaiaScene(scene);

        List<TileInfo> tileInfoList = new ArrayList<>();
        TileInfo writeTileInfo = TileInfo.builder()
                .scenePath(Path.of("D:/workspace/input/BlockBABB.obj"))
                .tempPath(Path.of("D:\\workspace\\temp"))
                .scene(null)
                .set(gaiaSet)
                .build();
        tileInfoList.add(writeTileInfo);

        GaiaBatcher gaiaBatcher = new GaiaBatcher();
        GaiaSet newGaiaSet = gaiaBatcher.runBatching(tileInfoList, "C0000", LevelOfDetail.LOD0);
        newGaiaSet.setAttribute(gaiaAttribute);

        GaiaScene newScene = new GaiaScene(newGaiaSet);
        newScene.setAttribute(gaiaAttribute);
        GltfWriter gltfWriter = new GltfWriter();
        gltfWriter.writeGlb(newScene, new File("D:/workspace/output/BlockBABB-before-batched.glb"));

        Batched3DModelTilerPhR photoRealisticTiler = new Batched3DModelTilerPhR();
        GaiaScene decimatedScene = photoRealisticTiler.decimate(newScene, decimateParameters);
        gltfWriter.writeGlb(decimatedScene, new File("D:/workspace/output/BlockBABB-after-batched.glb"));
/*
        List<TileInfo> deciTileInfoList = new ArrayList<>();
        TileInfo deciTileInfo = TileInfo.builder()
                .tempPath(tempPath)
                .serial(1)
                .scene(newScene)
                .set(newGaiaSet)
                .build();
        deciTileInfoList.add(deciTileInfo);

        Batched3DModelTilerPhR tiler = new Batched3DModelTilerPhR();
        tiler.decimateScenes(deciTileInfoList, 1, decimateParameters);

        GaiaScene resultScene = deciTileInfoList.get(0).getScene();
        gltfWriter.writeGlb(resultScene, new File("D:/workspace/output/BlockBABB-after.glb"));
*/

        log.info("Decimate finished.");

    }

}