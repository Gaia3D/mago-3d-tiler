package gltf;

import geometry.batch.Batcher;
import geometry.exchangable.GaiaSet;
import geometry.exchangable.GaiaUniverse;
import geometry.structure.GaiaScene;
import io.LittleEndianDataOutputStream;
import lombok.extern.slf4j.Slf4j;
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

    public void write() {
        this.universe.convertGaiaSet();
        Batcher batcher = new Batcher(universe);
        GaiaSet set = batcher.batch();
        GaiaScene scene = new GaiaScene(set);
        File glbOutputFile = this.universe.getOutputRoot().resolve("GaiaBatchedProject.glb").toFile();
        GltfWriter.writeGlb(scene, glbOutputFile);

        byte[] glbBytes = readGlb(glbOutputFile);

        this.byteLength = 28 + glbBytes.length; // without featureTable/batchTable

        File b3dmOutputFile = this.universe.getOutputRoot().resolve("result.b3dm").toFile();
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
            // body
            stream.write(glbBytes);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }
}
