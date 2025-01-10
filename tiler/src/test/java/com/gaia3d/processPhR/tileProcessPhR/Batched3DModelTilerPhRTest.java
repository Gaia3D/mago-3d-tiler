package com.gaia3d.processPhR.tileProcessPhR;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.halfedge.DecimateParameters;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.Configurator;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.assimp.AssimpConverter;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.process.preprocess.GaiaRotator;
import com.gaia3d.process.preprocess.GaiaTranslatorExact;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.junit.jupiter.api.Test;
import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Slf4j
class Batched3DModelTilerPhRTest {

    @Test
    void decimate() {
        Configurator.initConsoleLogger();

        DecimateParameters decimateParameters = new DecimateParameters();
        decimateParameters.setBasicValues(12.0, 0.5, 0.9, 15.0, 1000000, 1, 1.2);

        AssimpConverter assimpConverter = new AssimpConverter();
        List<GaiaScene> gaiaScenes = assimpConverter.load("D:/workspace/input/BlockBABB.obj");
        GaiaScene scene = gaiaScenes.get(0);
        //scene.getNodes().get(0).setTransformMatrix(new Matrix4d().identity());
        GaiaBoundingBox boundingBox = scene.getBoundingBox();


        TileInfo tileInfo = TileInfo.builder()
                .scenePath(Path.of("D:/workspace/input/BlockBABB.obj"))
                .scene(scene)
                .build();

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setInputFormat(FormatType.OBJ);
        globalOptions.setOutputFormat(FormatType.B3DM);
        globalOptions.setRotateX(0);

        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem crs = crsFactory.createFromName("EPSG:5187");
        globalOptions.setCrs(crs);


        /*GaiaRotator rotator = new GaiaRotator();
        GaiaTranslatorExact translatorExact = new GaiaTranslatorExact(new ArrayList<>());

        rotator.run(tileInfo);
        translatorExact.run(tileInfo);

        boolean checkTexCoord = true;
        boolean checkNormal = false;
        boolean checkColor = false;
        boolean checkBatchId = false;
        double error = 1e-4;
        scene.weldVertices(error, checkTexCoord, checkNormal, checkColor, checkBatchId);
*/

        GltfWriter gltfWriter = new GltfWriter();
        gltfWriter.writeGlb(scene, new File("D:/workspace/output/BlockBABB-before.glb"));

        GaiaSet gaiaSet = GaiaSet.fromGaiaScene(scene);
        Path tempPath = gaiaSet.writeFile(Path.of("D:/workspace/temp"));

        List<TileInfo> tileInfoList = new ArrayList<>();
        tileInfo = TileInfo.builder()
                .tempPath(tempPath)
                .serial(1)
                .scenePath(Path.of("D:/workspace/input/BlockBABB.obj"))
                .scene(scene)
                .build();
        tileInfoList.add(tileInfo);

        Batched3DModelTilerPhR tiler = new Batched3DModelTilerPhR();
        tiler.decimateScenes(tileInfoList, 1, decimateParameters);


        //GaiaSet tempSetLod0 = GaiaSet.fromGaiaScene(scene);

        gltfWriter.writeGlb(tileInfoList.get(0).getScene(), new File("D:/workspace/output/BlockBABB-after.glb"));

        log.info("Decimate finished.");

    }

}