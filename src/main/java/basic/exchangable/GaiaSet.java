package basic.exchangable;

import basic.structure.GaiaMaterial;
import basic.structure.GaiaNode;
import basic.structure.GaiaScene;
import basic.structure.GaiaTexture;
import basic.types.AttributeType;
import basic.types.FormatType;
import basic.types.TextureType;
import org.apache.commons.io.FileUtils;
import util.io.LittleEndianDataInputStream;
import util.io.LittleEndianDataOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.joml.Matrix4d;
import org.joml.Quaterniond;
import org.joml.Vector3d;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
        for (GaiaBufferDataSet buffDataSet : bufferDatas) {
            Map<AttributeType, GaiaBuffer> mapAttribName_Buffer = buffDataSet.getBuffers();
            // now check if exist "TEXCOORD" attribute

            if (mapAttribName_Buffer.containsKey(AttributeType.TEXCOORD)) {
                GaiaBuffer buff = mapAttribName_Buffer.get(AttributeType.TEXCOORD);
                if (buff.getGlType() == 5126) // 5126 is float type
                {
                    float[] buffData = buff.floats;
                    int buffDataCount = buffData.length;
                    for (float buffDatum : buffData) {
                        if (buffDatum > 1.1f || buffDatum < -0.2f) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public Path writeFile(Path path) {
        String tempFile = projectName + "." + FormatType.TEMP.getExtension();
        File output = new File(path.toAbsolutePath().toString(), tempFile);
        try (LittleEndianDataOutputStream stream = new LittleEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(output)))) {
            stream.writeByte(isBigEndian);
            stream.writeText(projectName);
            stream.writeInt(materials.size());

            if (materials.size() == 0) {
                log.error("material size is 0");
            }

            for (GaiaMaterial material : materials) {
                material.write(stream);
                LinkedHashMap<TextureType, List<GaiaTexture>> materialTextures = material.getTextures();
                List<GaiaTexture> diffuseTextures = materialTextures.get(TextureType.DIFFUSE);
                if (diffuseTextures.size() > 0) {
                    GaiaTexture texture = materialTextures.get(TextureType.DIFFUSE).get(0);
                    Path parentPath = texture.getParentPath();
                    String diffusePath = texture.getPath();
                    String imagePath = parentPath + File.separator + diffusePath;
                    Path outputPath = path.resolve("images").resolve(diffusePath);
                    FileUtils.copyFile(new File(imagePath), outputPath.toFile());
                }
            }
            stream.writeInt(bufferDatas.size());
            for (GaiaBufferDataSet bufferData : bufferDatas) {
                bufferData.write(stream);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error(e.getMessage());
        }
        return output.toPath();
    }

    public void readFile(Path path) {
        File input = path.toFile();
        Path imagesPath = path.getParent().resolve("images");
        imagesPath.toFile().mkdir();

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

            if (materials.size() == 0) {
                log.error("material size is 0");
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

    public void translate(Vector3d translation) {
        for (GaiaBufferDataSet bufferData : this.bufferDatas) {
            GaiaBuffer positionBuffer = bufferData.getBuffers().get(AttributeType.POSITION);
            float[] positions = positionBuffer.getFloats();
            for (int i = 0; i < positions.length; i += 3) {
                positions[i] += translation.x;
                positions[i + 1] += translation.y;
                positions[i + 2] += translation.z;
            }
        }
    }

    public void deleteTextures() {
        List<GaiaMaterial> materials = getMaterials();
        materials.forEach(GaiaMaterial::deleteTextures);
    }
}
