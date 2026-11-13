package com.gaia3d.process.postprocess.batch;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.model.GaiaAttribute;
import com.gaia3d.basic.model.GaiaNode;
import com.gaia3d.basic.model.GaiaScene;
import com.gaia3d.command.mago.GlobalConstants;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.command.mago.MeshCompression;
import com.gaia3d.command.mago.TextureCompression;
import com.gaia3d.converter.gltf.DracoMeshCompressor;
import com.gaia3d.converter.gltf.Ktx2TextureEncoder;
import com.gaia3d.converter.gltf.GltfWriterOptions;
import com.gaia3d.converter.gltf.tiles.BatchedModelGltfWriter;
import com.gaia3d.process.postprocess.ContentModel;
import com.gaia3d.process.postprocess.instance.GaiaFeatureTable;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.StringUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix3d;
import org.joml.Matrix4d;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.UnaryOperator;

/**
 * 3D Tiles 1.1 Batched Model
 */
@Slf4j
public class Batched3DModelV2 implements ContentModel {
    private static final String MAGIC = "glb";
    private final BatchedModelGltfWriter gltfWriter;
    private final UnaryOperator<byte[]> meshCompressor;

    public Batched3DModelV2() {
        GltfWriterOptions gltfOptions = GltfWriterOptions.builder()
                .build();
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        if ("1.0".equals(globalOptions.getTilesVersion())) {
            gltfOptions.setUriImage(true);
        }
        if (globalOptions.isUseQuantization()) {
            gltfOptions.setUseQuantization(true);
        }
        if (globalOptions.isUnlit()) {
            gltfOptions.setUnlit(true);
        }
        if (globalOptions.getTextureCompression() == TextureCompression.KTX2) {
            gltfOptions.setKtx2Encoder(new Ktx2TextureEncoder(GlobalConstants.DEFAULT_KTX2_ETC1S_QUALITY, GlobalConstants.DEFAULT_KTX2_ETC1S_COMPRESSION_LEVEL,
                    Runtime.getRuntime().availableProcessors() / Math.max(1, globalOptions.getMultiThreadCount())));
        }
        if (globalOptions.isPhotogrammetry()) {
            gltfOptions.setForceJpeg(true);
            if (globalOptions.getMeshCompression() != MeshCompression.DRACO) {
                gltfOptions.setUseQuantization(true);
                gltfOptions.setUseShortTexCoord(true);
                gltfOptions.setUseByteNormal(true);
            }
        }
        if (globalOptions.getMeshCompression() == MeshCompression.DRACO) {
            this.meshCompressor = new DracoMeshCompressor(GlobalConstants.DEFAULT_DRACO_POSITION_BITS, GlobalConstants.DEFAULT_DRACO_NORMAL_BITS,
                    GlobalConstants.DEFAULT_DRACO_TEXCOORD_BITS, GlobalConstants.DEFAULT_DRACO_COLOR_BITS);
        } else {
            this.meshCompressor = null;
        }
        this.gltfWriter = new BatchedModelGltfWriter(gltfOptions);
    }

    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        GaiaBatcher gaiaBatcher = new GaiaBatcher();
        GaiaSet batchedSet = gaiaBatcher.runBatching(contentInfo.getTileInfos(), contentInfo.getNodeCode(), contentInfo.getLod());
        String nodeCode = contentInfo.getNodeCode();

        List<TileInfo> tileInfos = contentInfo.getTileInfos();
        int batchLength = tileInfos.size();

        if (batchedSet == null) {
            log.error("[ERROR] BatchedSet is null, return null.");
            return contentInfo;
        }
        GaiaScene scene = new GaiaScene(batchedSet);

        /* create FeatureTable */
        GaiaFeatureTable featureTable = new GaiaFeatureTable();
        featureTable.setBatchLength(batchLength);
        if (!globalOptions.isClassicTransformMatrix()) {
            /* relative to center */
            Matrix4d worldTransformMatrix = contentInfo.getTransformMatrix();
            Matrix3d rotationMatrix3d = worldTransformMatrix.get3x3(new Matrix3d());
            Matrix3d xRotationMatrix3d = new Matrix3d();
            xRotationMatrix3d.identity();
            xRotationMatrix3d.rotateX(Math.toRadians(-90));
            xRotationMatrix3d.mul(rotationMatrix3d, rotationMatrix3d);
            Matrix4d rotationMatrix4d = new Matrix4d(rotationMatrix3d);

            GaiaNode rootNode = scene.getNodes()
                    .get(0); // z-up
            Matrix4d sceneTransformMatrix = rootNode.getTransformMatrix();
            rotationMatrix4d.mul(sceneTransformMatrix, sceneTransformMatrix);

            Double[] rtcCenter = new Double[3];
            rtcCenter[0] = worldTransformMatrix.m30();
            rtcCenter[1] = worldTransformMatrix.m31();
            rtcCenter[2] = worldTransformMatrix.m32();
            featureTable.setRtcCenter(rtcCenter);
        }

        File outputFile = new File(globalOptions.getOutputPath());
        Path outputRoot = outputFile.toPath()
                .resolve("data");
        if (!outputRoot.toFile()
                .exists() && outputRoot.toFile()
                .mkdir()) {
            log.debug("[Create][data] Created output data directory,", outputRoot);
        }

        /* create BatchTable */
        GaiaBatchTableMap<String, List<String>> batchTableMap = new GaiaBatchTableMap<>();
        AtomicInteger batchIdIndex = new AtomicInteger(0);
        tileInfos.forEach((tileInfo) -> {
            GaiaAttribute attribute = tileInfo.getScene()
                    .getAttribute();
            Map<String, String> attributes = attribute.getAttributes();
            String UUID = attribute.getIdentifier()
                    .toString();
            String FileName = attribute.getFileName();
            String NodeName = attribute.getNodeName();

            UUID = StringUtils.convertUTF8(UUID);
            FileName = StringUtils.convertUTF8(FileName);
            NodeName = StringUtils.convertUTF8(NodeName);

            batchTableMap.computeIfAbsent("id", k -> new ArrayList<>());
            batchTableMap.get("id").add(UUID);

            batchTableMap.computeIfAbsent("FileName", k -> new ArrayList<>());
            batchTableMap.get("FileName").add(FileName);

            batchTableMap.computeIfAbsent("NodeName", k -> new ArrayList<>());
            batchTableMap.get("NodeName").add(NodeName);

            batchTableMap.computeIfAbsent("BatchId", k -> new ArrayList<>());
            batchTableMap.get("BatchId").add(String.valueOf(batchIdIndex.getAndIncrement()));

            attributes.forEach((key, value) -> {
                String utf8Value = StringUtils.convertUTF8(value);
                batchTableMap.computeIfAbsent(key, k -> new ArrayList<>());
                batchTableMap.get(key).add(utf8Value);
            });
        });

        File glbOutputFile = contentInfo.resolveContentFile(outputRoot, MAGIC);
        if (globalOptions.isPhotogrammetry()) {
            scene.deleteNormals();
        }

        if (meshCompressor != null) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            this.gltfWriter.writeGlb(scene, byteArrayOutputStream, featureTable, batchTableMap);
            try {
                Files.write(glbOutputFile.toPath(), meshCompressor.apply(byteArrayOutputStream.toByteArray()));
            } catch (IOException e) {
                log.error("[ERROR] Failed to write glb file.", e);
            }
        } else {
            this.gltfWriter.writeGlb(scene, glbOutputFile, featureTable, batchTableMap);
        }
        return contentInfo;
    }
}
