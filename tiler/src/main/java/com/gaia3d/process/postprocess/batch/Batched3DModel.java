package com.gaia3d.process.postprocess.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaNode;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.process.postprocess.TileModel;
import com.gaia3d.process.postprocess.instance.GaiaFeatureTable;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.DecimalUtils;
import com.gaia3d.util.StringUtils;
import com.gaia3d.util.io.LittleEndianDataInputStream;
import com.gaia3d.util.io.LittleEndianDataOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix3d;
import org.joml.Matrix4d;
import org.lwjgl.BufferUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Slf4j
public class Batched3DModel implements TileModel {
    private static final String MAGIC = "b3dm";
    private static final int VERSION = 1;
    private final GltfWriter gltfWriter;

    public Batched3DModel() {
        this.gltfWriter = new GltfWriter();
    }

    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();

        GaiaBatcher gaiaBatcher = new GaiaBatcher();
        GaiaSet batchedSet = gaiaBatcher.runBatching(contentInfo.getTileInfos(), contentInfo.getNodeCode(), contentInfo.getLod());

        int featureTableJSONByteLength;
        int batchTableJSONByteLength;
        String featureTableJson;
        String batchTableJson;
        String nodeCode = contentInfo.getNodeCode();

        List<TileInfo> tileInfos = contentInfo.getTileInfos();
        int batchLength = tileInfos.size();

        List<String> projectNames = new ArrayList<>();
        List<String> nodeNames = new ArrayList<>();
        List<Double> geometricErrors = new ArrayList<>();
        List<Double> heights = new ArrayList<>();
        tileInfos.forEach((tileInfo) -> {
            GaiaSet set = tileInfo.getSet();
            String projectName = set.getProjectName();
            // convert utf-8 to ascii
            String asciiProjectName = StringUtils.convertUTF8(projectName);
            projectNames.add(asciiProjectName);
            nodeNames.add(tileInfo.getName());
            geometricErrors.add(tileInfo.getBoundingBox().getLongestDistance());
            GaiaBoundingBox boundingBox = tileInfo.getBoundingBox();
            heights.add(boundingBox.getMaxZ() - boundingBox.getMinZ());
        });


        GaiaScene scene = new GaiaScene(batchedSet);

        /* FeatureTable */
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

            GaiaNode rootNode = scene.getNodes().get(0); // z-up
            Matrix4d sceneTransformMatrix = rootNode.getTransformMatrix();
            rotationMatrix4d.mul(sceneTransformMatrix, sceneTransformMatrix);

            double[] rtcCenter = new double[3];
            rtcCenter[0] = worldTransformMatrix.m30();
            rtcCenter[1] = worldTransformMatrix.m31();
            rtcCenter[2] = worldTransformMatrix.m32();
            featureTable.setRctCenter(rtcCenter);
        }

        File outputFile = new File(globalOptions.getOutputPath());
        Path outputRoot = outputFile.toPath().resolve("data");
        if (!outputRoot.toFile().exists() && outputRoot.toFile().mkdir()) {
            log.info("[Create][data] Created output data directory:", outputRoot);
        }

        byte[] glbBytes;
        if (globalOptions.isGltf()) {
            String glbFileName = nodeCode + ".gltf";
            File glbOutputFile = outputRoot.resolve(glbFileName).toFile();
            this.gltfWriter.writeGltf(scene, glbOutputFile);
        }
        if (globalOptions.isGlb()) {
            String glbFileName = nodeCode + ".glb";
            File glbOutputFile = outputRoot.resolve(glbFileName).toFile();
            this.gltfWriter.writeGlb(scene, glbOutputFile);
            glbBytes = readGlb(glbOutputFile);
        } else {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            this.gltfWriter.writeGlb(scene, byteArrayOutputStream);
            glbBytes = byteArrayOutputStream.toByteArray();
        }
        //this.gltfWriter = null;
        scene = null;

        /* BatchTable */
        GaiaBatchTable batchTable = new GaiaBatchTable();
        for (int i = 0; i < batchLength; i++) {
            batchTable.getProejctName().add(projectNames.get(i));
            batchTable.getNodeName().add(nodeNames.get(i));
            batchTable.getBatchName().add(nodeCode);
            batchTable.getBatchId().add(i + "/" + batchLength);
            batchTable.getGeometricError().add(DecimalUtils.cut(geometricErrors.get(i), 2));
            batchTable.getHeight().add(DecimalUtils.cut(heights.get(i), 2));
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String featureTableText = StringUtils.doPadding8Bytes(objectMapper.writeValueAsString(featureTable));
            featureTableJson = featureTableText;
            featureTableJSONByteLength = featureTableText.length();

            String batchTableText = StringUtils.doPadding8Bytes(objectMapper.writeValueAsString(batchTable));
            batchTableJson = batchTableText;
            batchTableJSONByteLength = batchTableText.length();
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        int byteLength = 28 + featureTableJSONByteLength + batchTableJSONByteLength + glbBytes.length;

        File b3dmOutputFile = outputRoot.resolve(nodeCode + "." + MAGIC).toFile();
        try (LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(b3dmOutputFile)))) {
            // 28-byte header (first 20 bytes)
            stream.writePureText(MAGIC);
            stream.writeInt(VERSION);
            stream.writeInt(byteLength);
            stream.writeInt(featureTableJSONByteLength);
            int featureTableBinaryByteLength = 0;
            stream.writeInt(featureTableBinaryByteLength);
            // 28-byte header (next 8 bytes)
            stream.writeInt(batchTableJSONByteLength);
            int batchTableBinaryByteLength = 0;
            stream.writeInt(batchTableBinaryByteLength);
            stream.writePureText(featureTableJson);
            stream.writePureText(batchTableJson);
            // body
            stream.write(glbBytes);
            glbBytes = null;
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return contentInfo;
    }

    private byte[] readGlb(File glbOutputFile) {
        ByteBuffer byteBuffer = readFile(glbOutputFile, true);
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }

    public void extract(File b3dm, File output) {
        byte[] glbBytes = null;
        try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream(new BufferedInputStream(new FileInputStream(b3dm)))) {
            // 28-byte header (first 20 bytes)
            String magic = stream.readIntAndUTF(4);
            int version = stream.readInt();
            int byteLength = stream.readInt();
            int featureTableJSONByteLength = stream.readInt();
            int featureTableBinaryByteLength = stream.readInt();
            // 28-byte header (next 8 bytes)
            int batchTableJSONByteLength = stream.readInt();
            int batchTableBinaryByteLength = stream.readInt();
            String featureTableJson = stream.readIntAndUTF(featureTableJSONByteLength);
            String batchTableJson = stream.readIntAndUTF(batchTableJSONByteLength);
            String featureTableBinary = stream.readIntAndUTF(featureTableBinaryByteLength);
            String batchTableBinary = stream.readIntAndUTF(batchTableBinaryByteLength);
            // body
            int glbSize = byteLength - 28 - featureTableJSONByteLength - batchTableJSONByteLength - featureTableBinaryByteLength - batchTableBinaryByteLength;
            glbBytes = new byte[glbSize];
            int result = stream.read(glbBytes);
            log.info("{}, {}", magic, version);
            log.info("{}", featureTableJson);
            log.info("{}", batchTableJson);
            log.info("{}", featureTableBinary);
            log.info("{}", batchTableBinary);
            log.info("{}", result);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        try (LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {
            assert glbBytes != null;
            stream.write(glbBytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public ByteBuffer readFile(File file, boolean flip) {
        Path path = file.toPath();
        try (var is = new BufferedInputStream(Files.newInputStream(path))) {
            int size = (int) Files.size(path);
            ByteBuffer byteBuffer = BufferUtils.createByteBuffer(size);

            int bufferSize = 8192;
            bufferSize = Math.min(size, bufferSize);
            byte[] buffer = new byte[bufferSize];
            while (buffer.length > 0 && is.read(buffer) != -1) {
                byteBuffer.put(buffer);
                if (is.available() < bufferSize) {
                    buffer = new byte[is.available()];
                }
            }
            if (flip)
                byteBuffer.flip();
            return byteBuffer;
        } catch (IOException e) {
            log.error("FileUtils.readBytes: " + e.getMessage());
        }
        return null;
    }
}
