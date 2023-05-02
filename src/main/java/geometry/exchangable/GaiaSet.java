package geometry.exchangable;

import geometry.structure.GaiaMaterial;
import geometry.structure.GaiaNode;
import geometry.structure.GaiaScene;
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

    public void writeFile(Path path) {
        Path imagesPath = path.resolve("images");
        File imagesDir = imagesPath.toFile();
        if (!imagesDir.exists()) {
            imagesDir.mkdirs();
        }
        File output = new File(path.toAbsolutePath().toString(), projectName + ".mgb");

        try (DataOutputStream stream = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {
            // material count
            for (GaiaMaterial material : materials) {
                material.write(stream);
            }
            // buffer count
            for (GaiaBufferDataSet bufferData : bufferDatas) {
                bufferData.write(stream);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println(output);
    }
}
