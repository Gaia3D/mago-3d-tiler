package com.gaia3d.basic.exchangable;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.structure.*;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.util.ImageUtils;
import com.gaia3d.util.io.BigEndianDataInputStream;
import com.gaia3d.util.io.BigEndianDataOutputStream;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.joml.Vector2d;
import org.joml.Vector3d;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A GaiaSet is a set of data that contains a buffer of raw scene-level 3D data.
 * @author znkim
 * @since 1.0.0
 * @see GaiaBufferDataSet, GaiaMaterial, GaiaTexture, GaiaScene, GaiaNode
 */
@Slf4j
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaSet implements Serializable{
    private List<GaiaBufferDataSet> bufferDataList;
    private List<GaiaMaterial> materials;
    private GaiaAttribute attribute;

    private byte isBigEndian = 0;
    private String projectName;
    private String filePath;
    private String folderPath;
    private String projectFolderPath;
    private String outputDir;

    public GaiaSet(Path path) {
        readFile(path);
    }

    public GaiaSet(GaiaScene gaiaScene) {
        String name;
        name = FilenameUtils.removeExtension(gaiaScene.getOriginalPath().getFileName().toString());

        this.projectName = name;
        List<GaiaBufferDataSet> bufferDataSets = new ArrayList<>();
        for (GaiaNode node : gaiaScene.getNodes()) {
            node.toGaiaBufferSets(bufferDataSets, null);
        }
        this.materials = gaiaScene.getMaterials();
        this.bufferDataList = bufferDataSets;
        this.attribute = gaiaScene.getAttribute();
    }

    public GaiaBoundingBox getBoundingBox() {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        for (GaiaBufferDataSet bufferDataSet : bufferDataList) {
            boundingBox.addBoundingBox(bufferDataSet.getBoundingBox());
        }
        return boundingBox;
    }

    public Path writeFile(Path path, int serial, GaiaAttribute gaiaAttribute) {
        //String tempFileName = projectName + "_" + serial + "." + FormatType.TEMP.getExtension();

        String tempFileName = gaiaAttribute.getIdentifier().toString() + "." + FormatType.TEMP.getExtension();

        int dividedNumber = serial / 50000;
        Path tempDir = path.resolve(this.projectName).resolve(String.valueOf(dividedNumber));
        File tempDirFile = tempDir.toFile();
        tempDirFile.mkdirs();
        File tempFile = tempDir.resolve(tempFileName).toFile();
        try (BigEndianDataOutputStream stream = new BigEndianDataOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile), 32768))) {
            stream.writeByte(isBigEndian);
            stream.writeText(projectName);
            stream.writeInt(materials.size());
            if (materials.isEmpty()) {
                log.error("material size is 0");
            }
            for (GaiaMaterial material : materials) {
                Map<TextureType, List<GaiaTexture>> materialTextures = material.getTextures();
                List<GaiaTexture> diffuseTextures = materialTextures.get(TextureType.DIFFUSE);
                if (diffuseTextures != null && !diffuseTextures.isEmpty()) {
                    GaiaTexture texture = materialTextures.get(TextureType.DIFFUSE).get(0);
                    Path parentPath = texture.getParentPath();
                    File parentFile = parentPath.toFile();
                    String diffusePath = texture.getPath();
                    File diffuseFile = new File(diffusePath);

                    File imageFile = ImageUtils.correctPath(parentFile, diffuseFile);

                    Path imagesFolderPath = tempDir.resolve("images");
                    imagesFolderPath.toFile().mkdirs();

                    Path outputImagePath = imagesFolderPath.resolve(imageFile.getName());
                    File outputImageFile = outputImagePath.toFile();

                    texture.setPath(imageFile.getName());

                    if (!imageFile.exists()) {
                        log.error("Texture Input Image Path is not exists. {}", diffusePath);
                    } else {
                        FileUtils.copyFile(imageFile, outputImageFile);
                    }
                }
                material.write(stream);
            }
            stream.writeInt(bufferDataList.size());
            for (GaiaBufferDataSet bufferData : bufferDataList) {
                bufferData.write(stream);
            }
            attribute.write(stream);
        } catch (Exception e) {
            log.error(e.getMessage());
            tempFile.delete();
        }
        return tempFile.toPath();
    }

    public void readFile(Path path) {
        File input = path.toFile();
        Path imagesPath = path.getParent().resolve("images");
        imagesPath.toFile().mkdir();

        try (BigEndianDataInputStream stream = new BigEndianDataInputStream(new BufferedInputStream(new FileInputStream(input), 32768))) {
            this.isBigEndian = stream.readByte();
            this.projectName = stream.readText();
            int materialCount = stream.readInt();
            List<GaiaMaterial> materials = new ArrayList<>();

            for (int i = 0; i < materialCount; i++) {
                GaiaMaterial material = new GaiaMaterial();
                material.read(stream, imagesPath);
                materials.add(material);
            }

            if (materials.isEmpty()) {
                log.error("material size is 0");
            }

            this.materials = materials;
            int bufferDataCount = stream.readInt();
            List<GaiaBufferDataSet> bufferDataSets = new ArrayList<>();
            for (int i = 0; i < bufferDataCount; i++) {
                GaiaBufferDataSet bufferDataSet = new GaiaBufferDataSet();
                bufferDataSet.read(stream);

                int materialId = bufferDataSet.getMaterialId();
                GaiaMaterial materialById = materials.stream()
                        .filter(material -> material.getId() == materialId)
                        .findFirst().orElseThrow();
                bufferDataSet.setMaterialId(materialById.getId());
                GaiaRectangle texcoordBoundingRectangle = calcTexcoordBoundingRectangle(bufferDataSet);
                bufferDataSet.setTexcoordBoundingRectangle(texcoordBoundingRectangle);
                bufferDataSets.add(bufferDataSet);
            }
            this.bufferDataList = bufferDataSets;

            GaiaAttribute attribute = new GaiaAttribute();
            attribute.read(stream);
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private GaiaRectangle calcTexcoordBoundingRectangle(GaiaBufferDataSet bufferDataSet) {
        GaiaRectangle texcoordBoundingRectangle = null;
        Map<AttributeType, GaiaBuffer> buffers = bufferDataSet.getBuffers();
        GaiaBuffer texcoordBuffer = buffers.get(AttributeType.TEXCOORD);
        if (texcoordBuffer != null) {
            float[] texcoords = texcoordBuffer.getFloats();
            for (int i = 0; i < texcoords.length; i += 2) {
                float x = texcoords[i];
                float y = texcoords[i + 1];
                Vector2d textureCoordinate = new Vector2d(x, y);
                if (texcoordBoundingRectangle == null) {
                    texcoordBoundingRectangle = new GaiaRectangle();
                    texcoordBoundingRectangle.setInit(textureCoordinate);
                } else {
                    texcoordBoundingRectangle.addPoint(textureCoordinate);
                }
            }
        }
        return texcoordBoundingRectangle;
    }

    public void translate(Vector3d translation) {
        for (GaiaBufferDataSet bufferData : this.bufferDataList) {
            GaiaBuffer positionBuffer = bufferData.getBuffers().get(AttributeType.POSITION);

            if (positionBuffer == null) {
                log.error("Position buffer is null");
                return;
            }

            float[] positions = positionBuffer.getFloats();
            for (int i = 0; i < positions.length; i += 3) {
                positions[i] += (float) translation.x;
                positions[i + 1] += (float) translation.y;
                positions[i + 2] += (float) translation.z;
            }
        }
    }

    public void deleteTextures() {
        List<GaiaMaterial> materials = getMaterials();
        materials.forEach(GaiaMaterial::deleteTextures);
    }

    public GaiaSet clone(){
        GaiaSet gaiaSet = new GaiaSet();
        gaiaSet.setBufferDataList(new ArrayList<>());
        for (GaiaBufferDataSet bufferData : this.bufferDataList) {
            gaiaSet.getBufferDataList().add(bufferData.clone());
        }
        gaiaSet.setMaterials(new ArrayList<>());
        for (GaiaMaterial material : this.materials) {
            gaiaSet.getMaterials().add(material.clone());
        }
        gaiaSet.setIsBigEndian(this.isBigEndian);
        gaiaSet.setProjectName(this.projectName);
        gaiaSet.setFilePath(this.filePath);
        gaiaSet.setFolderPath(this.folderPath);
        gaiaSet.setProjectFolderPath(this.projectFolderPath);
        gaiaSet.setOutputDir(this.outputDir);
        return gaiaSet;
    }

    public void clear() {
        this.bufferDataList.forEach(GaiaBufferDataSet::clear);
        this.bufferDataList.clear();
        this.materials.clear();
    }
}
