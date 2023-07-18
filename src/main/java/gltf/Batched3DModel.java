package gltf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import geometry.batch.Batcher;
import geometry.batch.GaiaBatchTable;
import geometry.batch.GaiaFeatureTable;
import geometry.exchangable.GaiaSet;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaScene;
import io.LittleEndianDataOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import tiler.LevelOfDetail;
import tiler.TileInfo;
import util.ImageUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

@Slf4j
public class Batched3DModel {
    private final String MAGIC = "b3dm";
    private final int VERSION = 1;

    private int byteLength = 0;
    private int featureTableJSONByteLength = 0;
    private int featureTableBinaryByteLength = 0;
    private int batchTableJSONByteLength = 0;
    private int batchTableBinaryByteLength = 0;

    private String featureTableJson = "";
    private String batchTableJson = "";

    private final TileInfo tileInfo;
    private final LevelOfDetail lod;
    private final CommandLine command;

    public Batched3DModel(TileInfo tileInfo, LevelOfDetail lod, CommandLine command) {
        this.tileInfo = tileInfo;
        this.lod = lod;
        this.command = command;
    }

    public byte[] readGlb(File glbOutputFile) {
        ByteBuffer byteBuffer = ImageUtils.readFile(glbOutputFile, true);
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }

    public boolean write(String filename) throws IOException {
        GaiaUniverse universe = this.tileInfo.getUniverse();
        universe.convertGaiaSet();

        int batchLength = universe.getGaiaSets().size();

        Batcher batcher = new Batcher(universe, this.tileInfo.getBoundingBox(), this.lod, this.command);
        GaiaSet set = batcher.batch();

        if (set.getMaterials().size() < 1 || set.getBufferDatas().size() < 1) {
            log.info(set.getMaterials().size() + " materials");
            log.info(set.getBufferDatas().size() + " buffers");
            return false;
        }

        GaiaScene scene = new GaiaScene(set);

        //GaiaNode node = scene.getNodes().get(0);

        File glbOutputFile = universe.getOutputRoot().resolve(filename + ".glb").toFile();
        GltfWriter.writeGlb(scene, glbOutputFile);

        //File gltfOutputFile2 = universe.getOutputRoot().resolve(filename + ".gltf").toFile();
        //GltfWriter.writeGltf(scene, gltfOutputFile2);

        //ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        //GltfWriter.writeGlb(scene, byteArrayOutputStream);
        //Matrix4d rootTransformMatrix = node.getTransformMatrix();

        GaiaFeatureTable featureTable = new GaiaFeatureTable();
        featureTable.setBatchLength(batchLength);

        GaiaBatchTable batchTable = new GaiaBatchTable();
        for (int i = 0 ; i < batchLength ; i++) {
            batchTable.getName().add("GAIA_BATCH_NAME" + i);
        }

        ObjectMapper objectMapper = new ObjectMapper();
        try {
            String featureTableText = objectMapper.writeValueAsString(featureTable);
            log.info("featureTable : {}", featureTableText);
            featureTableJson = featureTableText;
            featureTableJSONByteLength = featureTableText.length();

            String batchTableText = objectMapper.writeValueAsString(batchTable);
            log.info("batchTable : {}", batchTableText);
            batchTableJson = batchTableText;
            batchTableJSONByteLength = batchTableText.length();
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }

        //byte[] glbBytes = byteArrayOutputStream.toByteArray();
        byte[] glbBytes = readGlb(glbOutputFile);

        // without featureTable/batchTable
        this.byteLength = 28 + featureTableJSONByteLength + batchTableJSONByteLength + glbBytes.length + featureTableJson.length() + batchTableJson.length();

        File b3dmOutputFile = universe.getOutputRoot().resolve(filename + ".b3dm").toFile();
        try (LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(b3dmOutputFile)))) {
            // 28-byte header (first 20 bytes)
            stream.writePureText(MAGIC);
            stream.writeInt(VERSION);
            stream.writeInt(byteLength);
            stream.writeInt(featureTableJSONByteLength);
            stream.writeInt(featureTableBinaryByteLength);
            // 28-byte header (next 8 bytes)
            stream.writeInt(batchTableJSONByteLength);
            stream.writeInt(batchTableBinaryByteLength);
            stream.writePureText(featureTableJson);
            stream.writePureText(batchTableJson);

            // body
            stream.write(glbBytes);
            // delete glb file
            if (!command.hasOption("debug")) {
                glbOutputFile.delete();
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        return true;
    }
}
