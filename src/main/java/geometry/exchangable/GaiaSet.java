package geometry.exchangable;

import geometry.structure.GaiaMaterial;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaSet {
    List<GaiaBufferData> bufferDatas;
    List<GaiaMaterial> materials;

    Vector3d position;
    Vector3d scale;
    Quaterniond quaternion;

    String projectName;
    String filePath;
    String folderPath;
    String projectFolderPath;
    String outputDir;
}
