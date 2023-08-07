package process.postprocess.batch;

import basic.exchangable.GaiaSet;
import basic.structure.GaiaScene;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import converter.jgltf.GltfWriter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import process.ProcessOptions;
import process.postprocess.TileModel;
import process.postprocess.instance.GaiaFeatureTable;
import process.tileprocess.tile.ContentInfo;
import process.tileprocess.tile.TileInfo;
import util.ImageUtils;
import util.io.LittleEndianDataOutputStream;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

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
    public ContentInfo run(ContentInfo batchInfo) {
        GaiaSet batchedSet = batchInfo.getBatchedSet();
        int featureTableJSONByteLength;
        int batchTableJSONByteLength;
        String featureTableJson;
        String batchTableJson;
        String nodeCode = batchInfo.getNodeCode();

        List<TileInfo> tileInfos = batchInfo.getTileInfos();
        int batchLength = tileInfos.size();
        List<String> names = tileInfos.stream()
                .map((tileInfo) -> {
                    //Path path = tileInfo.getScene().getOriginalPath().getFileName();
                    //return path.toString();
                    return tileInfo.getSet().getProjectName();
                })
                .collect(Collectors.toList());
        GaiaScene scene = new GaiaScene(batchedSet);

        File outputFile = new File(command.getOptionValue(ProcessOptions.OUTPUT.getArgName()));
        Path outputRoot = outputFile.toPath().resolve("data");
        outputRoot.toFile().mkdir();
        byte[] glbBytes;
        if (command.hasOption("gltf")) {
            String glbFileName = nodeCode + ".gltf";
            File glbOutputFile = outputRoot.resolve(glbFileName).toFile();
            this.gltfWriter.writeGltf(scene, glbOutputFile);
        }

        if (command.hasOption("glb")) {
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
        return batchInfo;
    }

    private byte[] readGlb(File glbOutputFile) {
        ByteBuffer byteBuffer = ImageUtils.readFile(glbOutputFile, true);
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }
}
