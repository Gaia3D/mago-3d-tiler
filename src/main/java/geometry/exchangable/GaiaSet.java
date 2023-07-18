package geometry.exchangable;

import geometry.structure.GaiaMaterial;
import geometry.structure.GaiaNode;
import geometry.structure.GaiaScene;
import geometry.types.AttributeType;
import geometry.types.FormatType;
import io.LittleEndianDataInputStream;
import io.LittleEndianDataOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileExistsException;
import org.apache.commons.io.FilenameUtils;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaSet {
    List<GaiaBufferDataSet> bufferDatas;
    List<GaiaMaterial> materials;

    private Matrix4d transformMatrix;

    Vector3d position;
    Vector3d scale;
    Quaterniond quaternion;

    byte isBigEndian = 0;
    String projectName;
    String filePath;
    String folderPath;
    String projectFolderPath;
    String outputDir;

    public GaiaSet(Path path) {
        readFile(path);
    }

    public GaiaSet(GaiaScene gaiaScene) {
        this.projectName = FilenameUtils.removeExtension(gaiaScene.getOriginalPath().getFileName().toString());
        List<GaiaBufferDataSet> bufferDataSets = new ArrayList<>();
        for (GaiaNode node : gaiaScene.getNodes()) {
            this.transformMatrix = node.toGaiaBufferSets(bufferDataSets, null);
        }
        this.materials = gaiaScene.getMaterials();
        this.bufferDatas = bufferDataSets;
    }

    public boolean checkIfIsTextureReperat_TEST()
    {
        int buffDataSetsCount = bufferDatas.size();
        for(int i = 0; i < buffDataSetsCount; i++)
        {
            GaiaBufferDataSet buffDataSet = bufferDatas.get(i);
            Map<AttributeType, GaiaBuffer> mapAttribName_Buffer = buffDataSet.getBuffers();
            // now check if exist "TEXCOORD" attribute

            if(mapAttribName_Buffer.containsKey(AttributeType.TEXCOORD))
            {
                GaiaBuffer buff = mapAttribName_Buffer.get(AttributeType.TEXCOORD);
                if(buff.getGlType() == 5126) // 5126 is float type
                {
                    float[] buffData = buff.floats;
                    int buffDataCount = buffData.length;
                    for(int j = 0; j < buffDataCount; j++)
                    {
                        if(buffData[j] > 1.1f || buffData[j] < -0.2f)
                        {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public void writeFile(Path path) {
        String tempFile = projectName + "." + FormatType.TEMP.getExtension();
        File output = new File(path.toAbsolutePath().toString(), tempFile);
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
            log.error(e.getMessage());
        }
    }

    public void readFile(Path path) {
        File input = path.toFile();
        Path imagesPath = path.getParent().resolve("images");

        try (LittleEndianDataInputStream stream = new LittleEndianDataInputStream(new BufferedInputStream(new FileInputStream(input)))) {
            this.isBigEndian = stream.readByte();
            this.projectName = stream.readText();
            int materialCount = stream.readInt();
            List<GaiaMaterial> materials = new ArrayList<>();
            for (int i = 0; i < materialCount; i++) {
                GaiaMaterial material = new GaiaMaterial();
                material.read(stream, imagesPath);
                materials.add(material);
            }
            this.materials = materials;
            int bufferDataCount = stream.readInt();
            List<GaiaBufferDataSet> bufferDataSets = new ArrayList<>();
            for (int i = 0; i < bufferDataCount; i++) {
                GaiaBufferDataSet bufferDataSet = new GaiaBufferDataSet();
                bufferDataSet.read(stream);
                bufferDataSets.add(bufferDataSet);
            }
            this.bufferDatas = bufferDataSets;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static List<GaiaSet> readFiles(Path path){
        List<GaiaSet> result = new ArrayList<>();
        readTree(result, path.toFile());
        return result;
    }

    private static void readTree(List<GaiaSet> result, File outputFile){
        if (outputFile.isFile() && outputFile.getName().endsWith("." + FormatType.TEMP.getExtension())) {
            GaiaSet gaiaSet = new GaiaSet();
            gaiaSet.readFile(outputFile.toPath());
            result.add(gaiaSet);
        } else if (outputFile.isDirectory()){
            for (File child : outputFile.listFiles()) {
                if (result.size() <= 100) {
                    readTree(result, child);
                }
            }
        }
    }
}
