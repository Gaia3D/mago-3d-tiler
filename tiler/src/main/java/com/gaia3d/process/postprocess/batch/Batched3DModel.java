package com.gaia3d.process.postprocess.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.structure.GaiaScene;
import com.gaia3d.converter.jgltf.GltfWriter;
import com.gaia3d.process.ProcessOptions;
import com.gaia3d.process.postprocess.TileModel;
import com.gaia3d.process.postprocess.instance.GaiaFeatureTable;
import com.gaia3d.process.tileprocess.tile.ContentInfo;
import com.gaia3d.process.tileprocess.tile.TileInfo;
import com.gaia3d.util.DecimalUtils;
import com.gaia3d.util.io.LittleEndianDataInputStream;
import com.gaia3d.util.io.LittleEndianDataOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.lwjgl.BufferUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
public class Batched3DModel implements TileModel {
    private static final String MAGIC = "b3dm";
    private static final int VERSION = 1;
    private final GltfWriter gltfWriter;
    private final CommandLine command;

    public Batched3DModel(CommandLine command) {
        this.gltfWriter = new GltfWriter();
        this.command = command;
    }

    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        GaiaSet batchedSet = contentInfo.getBatchedSet();
        int featureTableJSONByteLength;
        int batchTableJSONByteLength;
        String featureTableJson;
        String batchTableJson;
        String nodeCode = contentInfo.getNodeCode();

        List<TileInfo> tileInfos = contentInfo.getTileInfos();
        int batchLength = tileInfos.size();
        List<String> projectNames = tileInfos.stream()
                .map((tileInfo) -> tileInfo.getSet().getProjectName())
                .toList();
        List<String> nodeNames = tileInfos.stream()
                .map(TileInfo::getName)
                .toList();
        List<Double> geometricErrors = tileInfos.stream()
                .map((tileInfo) -> tileInfo.getBoundingBox().getLongestDistance())
                .toList();
        List<Double> heights = tileInfos.stream()
                .map((tileInfo) -> {
                    GaiaBoundingBox boundingBox = tileInfo.getBoundingBox();
                    return boundingBox.getMaxZ() - boundingBox.getMinZ();
                })
                .toList();

        GaiaScene scene = new GaiaScene(batchedSet);

        File outputFile = new File(command.getOptionValue(ProcessOptions.OUTPUT.getArgName()));
        Path outputRoot = outputFile.toPath().resolve("data");
        if (outputRoot.toFile().mkdir()) {
            log.info("Create directory: {}", outputRoot);
        }

        byte[] glbBytes;
        if (command.hasOption(ProcessOptions.DEBUG_GLTF.getArgName())) {
            String glbFileName = nodeCode + ".gltf";
            File glbOutputFile = outputRoot.resolve(glbFileName).toFile();
            this.gltfWriter.writeGltf(scene, glbOutputFile);
        }

        if (command.hasOption(ProcessOptions.DEBUG_GLB.getArgName())) {
            String glbFileName = nodeCode + ".glb";
            File glbOutputFile = outputRoot.resolve(glbFileName).toFile();
            this.gltfWriter.writeGlb(scene, glbOutputFile);
            glbBytes = readGlb(glbOutputFile);
        } else {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            this.gltfWriter.writeGlb(scene, byteArrayOutputStream);
            glbBytes = byteArrayOutputStream.toByteArray();
        }
        scene = null;
        //this.gltfWriter = null;

        GaiaFeatureTable featureTable = new GaiaFeatureTable();
        featureTable.setBatchLength(batchLength);

        GaiaBatchTable batchTable = new GaiaBatchTable();
        for (int i = 0; i < batchLength; i++) {
            batchTable.getBatchId().add(String.valueOf(i));
            batchTable.getProejctName().add(projectNames.get(i));
            batchTable.getNodeName().add(nodeNames.get(i));
            batchTable.getGeometricError().add(DecimalUtils.cut(geometricErrors.get(i)));
            batchTable.getHeight().add(DecimalUtils.cut(heights.get(i)));
        }

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String featureTableText = objectMapper.writeValueAsString(featureTable);
            featureTableJson = featureTableText;
            featureTableJSONByteLength = featureTableText.length();

            String batchTableText = objectMapper.writeValueAsString(batchTable);
            batchTableJson = batchTableText;
            batchTableJSONByteLength = batchTableText.length();
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        int byteLength = 28 + featureTableJSONByteLength + batchTableJSONByteLength + glbBytes.length;

        File b3dmOutputFile = outputRoot.resolve(nodeCode + ".b3dm").toFile();
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
            // delete glb file
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
