package geometry.batch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import geometry.exchangable.GaiaSet;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaScene;
import gltf.GltfWriter;
import io.LittleEndianDataOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import tiler.LevelOfDetail;
import tiler.BatchInfo;
import util.ImageUtils;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class Batched3DModel {
    private static final GltfWriter gltfWriter = new GltfWriter();
    private static final String MAGIC = "b3dm";
    private static final int VERSION = 1;

    private final BatchInfo tileInfo;
    private final CommandLine command;

    public Batched3DModel(BatchInfo tileInfo, CommandLine command) {
        this.tileInfo = tileInfo;
        this.command = command;
    }

    public boolean write() throws IOException {
        int featureTableJSONByteLength;
        int batchTableJSONByteLength;
        String featureTableJson;
        String batchTableJson;
        LevelOfDetail lod = this.tileInfo.getLod();
        String nodeCode = this.tileInfo.getNodeCode();

        GaiaUniverse universe = this.tileInfo.getUniverse();
        universe.convertGaiaSet();

        int batchLength = universe.getGaiaSets().size();
        List<String> names = universe.getGaiaSets().stream()
                .map(GaiaSet::getProjectName)
                .collect(Collectors.toList());

        Batcher batcher = new GaiaBatcher(universe, this.tileInfo.getBoundingBox(), lod, this.command);
        GaiaSet set = batcher.batch();

        if (set.getMaterials().size() < 1 || set.getBufferDatas().size() < 1) {
            throw new RuntimeException("No materials or buffers");
        }

        GaiaScene scene = new GaiaScene(set);
        byte[] glbBytes;
        if (command.hasOption("glb")) {
            String glbFileName = nodeCode + ".glb";
            File glbOutputFile = universe.getOutputRoot().resolve(glbFileName).toFile();
            gltfWriter.writeGlb(scene, glbOutputFile);
            glbBytes = readGlb(glbOutputFile);
        } else {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            gltfWriter.writeGlb(scene, byteArrayOutputStream);
            glbBytes = byteArrayOutputStream.toByteArray();
        }

        GaiaFeatureTable featureTable = new GaiaFeatureTable();
        featureTable.setBatchLength(batchLength);

        GaiaBatchTable batchTable = new GaiaBatchTable();
        for (int i = 0 ; i < batchLength ; i++) {
            batchTable.getBatchId().add(String.valueOf(i));
            batchTable.getFileName().add(names.get(i));
        }

        ObjectMapper objectMapper = new ObjectMapper();

        try {
            String featureTableText = objectMapper.writeValueAsString(featureTable);
            featureTableJson = featureTableText;
            featureTableJSONByteLength = featureTableText.length();
            //log.info("featureTable : {}", featureTableText);

            String batchTableText = objectMapper.writeValueAsString(batchTable);
            batchTableJson = batchTableText;
            batchTableJSONByteLength = batchTableText.length();
            //log.info("batchTable : {}", batchTableText);
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        int byteLength = 28 + featureTableJSONByteLength + batchTableJSONByteLength + glbBytes.length;

        File b3dmOutputFile = universe.getOutputRoot().resolve(nodeCode + ".b3dm").toFile();
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
            // delete glb file
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return true;
    }

    private byte[] readGlb(File glbOutputFile) {
        ByteBuffer byteBuffer = ImageUtils.readFile(glbOutputFile, true);
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }
}
