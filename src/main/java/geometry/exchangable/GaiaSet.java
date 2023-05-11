package geometry.exchangable;

import geometry.structure.GaiaMaterial;
import geometry.structure.GaiaNode;
import geometry.structure.GaiaScene;
import io.LittleEndianDataInputStream;
import io.LittleEndianDataOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import util.FileUtils;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaSet {
    List<GaiaBufferDataSet> bufferDatas;
    List<GaiaMaterial> materials;

    Vector3d position;
    Vector3d scale;
    Quaterniond quaternion;

    byte isBigEndian = 0;
    String projectName;
    String filePath;
    String folderPath;
    String projectFolderPath;
    String outputDir;

    public GaiaSet(List<GaiaBufferDataSet> bufferDatas, List<GaiaMaterial> materials) {
        this.bufferDatas = bufferDatas;
        this.materials = materials;
    }

    public GaiaSet(GaiaScene gaiaScene) {
        String projectName = FileUtils.getFileNameWithoutExtension(gaiaScene.getOriginalPath().getFileName().toString());
        setProjectName(projectName);
        List<GaiaBufferDataSet> bufferDataSets = new ArrayList<>();
        for (GaiaNode node : gaiaScene.getNodes()) {
            node.toGaiaBufferSets(bufferDataSets);
        }
        setMaterials(gaiaScene.getMaterials());
        setBufferDatas(bufferDataSets);
    }

    public void renderSet(int program) {
        for (GaiaBufferDataSet bufferData : bufferDatas) {
            bufferData.render(program, materials);
        }
    }

    public void writeFile(Path path) {
        File output = new File(path.toAbsolutePath().toString(), projectName + ".mgb");
        try (LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {
            stream.writeByte(isBigEndian);
            stream.writeText(projectName);
            stream.writeInt(materials.size());
            for (GaiaMaterial material : materials) {
                material.write(stream);
            }
            stream.writeInt(bufferDatas.size());
            for (GaiaBufferDataSet bufferData : bufferDatas) {
                bufferData.write(stream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void readFile(Path path) {
        File input = path.toFile();
        Path imagesPath = path.getParent().resolve("images");

        try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream(new BufferedInputStream(new FileInputStream(input)))) {
            this.setIsBigEndian(stream.readByte());
            this.setProjectName(stream.readText());
            int materialCount = stream.readInt();
            List<GaiaMaterial> materials = new ArrayList<>();
            for (int i = 0; i < materialCount; i++) {
                GaiaMaterial material = new GaiaMaterial();
                material.read(stream, imagesPath);
                materials.add(material);
            }
            this.setMaterials(materials);
            int bufferDataCount = stream.readInt();
            List<GaiaBufferDataSet> bufferDataSets = new ArrayList<>();
            for (int i = 0; i < bufferDataCount; i++) {
                GaiaBufferDataSet bufferDataSet = new GaiaBufferDataSet();
                bufferDataSet.read(stream);
                bufferDataSets.add(bufferDataSet);
            }
            this.setBufferDatas(bufferDataSets);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
