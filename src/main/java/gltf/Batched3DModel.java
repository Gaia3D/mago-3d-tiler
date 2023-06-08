package gltf;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import geometry.batch.Batcher;
import geometry.exchangable.GaiaSet;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaNode;
import geometry.structure.GaiaScene;
import io.LittleEndianDataOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import util.ImageUtils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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

    private final GaiaUniverse universe;

    public Batched3DModel(GaiaUniverse universe) {
        this.universe = universe;
    }

    public byte[] readGlb(File glbOutputFile) {
        ByteBuffer byteBuffer = ImageUtils.readFile(glbOutputFile, true);
        byte[] bytes = new byte[byteBuffer.remaining()];
        byteBuffer.get(bytes);
        return bytes;
    }

    public void write(String filename) {
        this.universe.convertGaiaSet();
        Batcher batcher = new Batcher(universe);
        GaiaSet set = batcher.batch();
        GaiaScene scene = new GaiaScene(set);
        GaiaNode node = scene.getNodes().get(0);

        Matrix4d rootTransformMatrix = node.getTransformMatrix();

        File glbOutputFile = this.universe.getOutputRoot().resolve(filename + ".glb").toFile();
        GltfWriter.writeGlb(scene, glbOutputFile);

        //float[] rctCenter = {-3056303.58824933f,4030639.359383482f,3872020.676540839f}; //ion-sample
        //FeatureTable featureTable = new FeatureTable();
        //featureTable.setBatchLength(1);
        //featureTable.setRctCenter(rctCenter);

        /*ObjectMapper objectMapper = new ObjectMapper();
        try {
            String test = objectMapper.writeValueAsString(featureTable);
            log.info(test);
            //featureTableJson = test;
            //featureTableJSONByteLength = test.length();
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
            throw new RuntimeException(e);
        }*/

        byte[] glbBytes = readGlb(glbOutputFile);

        this.byteLength = 28 + featureTableJSONByteLength + batchTableJSONByteLength + glbBytes.length; // without featureTable/batchTable

        File b3dmOutputFile = this.universe.getOutputRoot().resolve(filename + ".b3dm").toFile();
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

            //stream.writePureText(featureTableJson);
            //stream.writePureText(batchTableJson);

            // body
            stream.write(glbBytes);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
