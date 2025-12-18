package com.gaia3d.basic.exchangable;

import com.gaia3d.basic.geometry.GaiaBoundingBox;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.model.*;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.FormatType;
import com.gaia3d.basic.types.LevelOfDetail;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.util.ImageResizer;
import com.gaia3d.util.ImageUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.joml.Vector2d;
import org.joml.Vector3d;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Slf4j
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GaiaSet implements Serializable {
    private List<GaiaBufferDataSet> bufferDataList;
    private List<GaiaMaterial> materials;
    private GaiaAttribute attribute;

    private String projectName;
    private String filePath;
    private String folderPath;
    private String projectFolderPath;
    private String outputDir;

    public static GaiaSet fromGaiaScene(GaiaScene gaiaScene) {
        GaiaSet newGaiaSet = new GaiaSet();
        newGaiaSet.projectName = FilenameUtils.removeExtension(gaiaScene.getOriginalPath().getFileName().toString());
        newGaiaSet.materials = gaiaScene.getMaterials();
        newGaiaSet.attribute = gaiaScene.getAttribute();
        List<GaiaBufferDataSet> bufferDataSets = new ArrayList<>();
        for (GaiaNode node : gaiaScene.getNodes()) {
            node.toGaiaBufferSets(bufferDataSets, null);
        }
        newGaiaSet.bufferDataList = bufferDataSets;

        return newGaiaSet;
    }

    public static GaiaSet readFile(Path path) throws FileNotFoundException {
        File input = path.toFile();
        Path imagesPath = path.getParent().resolve("images");
        try (ObjectInputStream inputStream = new ObjectInputStream(new BufferedInputStream(new FileInputStream(input)))) {
            GaiaSet gaiaSet = (GaiaSet) inputStream.readObject();
            for (GaiaMaterial material : gaiaSet.getMaterials()) {
                material.getTextures().forEach((textureType, textures) -> {
                    for (GaiaTexture texture : textures) {
                        String texturePath = texture.getPath();
                        File file = new File(texturePath);
                        String fileName = file.getName();
                        texture.setParentPath(imagesPath.toString());
                        texture.setPath(fileName);
                    }
                });
            }
            return gaiaSet;
        } catch (Exception e) {
            log.error("[ERROR] GaiaSet Read Error : ", e);
        }
        return null;
    }

    public GaiaBoundingBox getBoundingBox() {
        GaiaBoundingBox boundingBox = new GaiaBoundingBox();
        for (GaiaBufferDataSet bufferDataSet : bufferDataList) {
            boundingBox.addBoundingBox(bufferDataSet.getBoundingBox());
        }
        return boundingBox;
    }

    public Path writeFileForPR(Path path, boolean copyTexturesToNewPath) {
        String tempFileName = this.attribute.getIdentifier().toString() + "." + FormatType.TEMP.getExtension();
        Path tempDir = path.resolve(this.projectName);
        File tempFile = path.resolve(tempFileName).toFile();
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)))) {
            outputStream.writeObject(this);
            outputStream.flush();
            outputStream.close();

            // Copy images to the temp directory
            if (copyTexturesToNewPath) {
                for (GaiaMaterial material : materials) {
                    copyTextures(material, tempDir);
                }
            }
        } catch (Exception e) {
            log.error("[ERROR] GaiaSet Write Error : ", e);
            tempFile.delete();
        }
        return tempFile.toPath();
    }

    public Path writeFileInThePath(Path path) {
        Path folder = path.getParent();
        File file = path.toFile();
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            outputStream.writeObject(this);
            outputStream.flush();
            outputStream.close();

            // Copy images to the temp directory
            for (GaiaMaterial material : materials) {
                copyTextures(material, folder);
            }
        } catch (Exception e) {
            log.error("[ERROR] GaiaSet Write Error : ", e);
            file.delete();
        }
        return file.toPath();
    }

    public Path writeFile(Path path, int serial) {
        int dividedNumber = serial / 10000;

        String tempFileName = this.attribute.getIdentifier().toString() + "." + FormatType.TEMP.getExtension();
        Path tempDir = path.resolve(this.projectName).resolve(String.valueOf(dividedNumber));
        File tempDirFile = tempDir.toFile();
        if (tempDirFile.mkdirs()) {
            log.debug("Directory created: {}", tempDir);
        }
        File tempFile = tempDir.resolve(tempFileName).toFile();
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)))) {
            outputStream.writeObject(this);
            outputStream.flush();
            outputStream.close();

            // Copy images to the temp directory
            for (GaiaMaterial material : materials) {
                copyTextures(material, tempDir);
            }
        } catch (Exception e) {
            log.error("[ERROR] GaiaSet Write Error : ", e);
            tempFile.delete();
        }
        return tempFile.toPath();
    }

    public Path writeFileWithLod(Path path, int serial, List<LevelOfDetail> lods) {
        int dividedNumber = serial / 10000;

        String tempFileName = this.attribute.getIdentifier().toString() + "." + FormatType.TEMP.getExtension();
        Path tempDir = path.resolve(this.projectName).resolve(String.valueOf(dividedNumber));
        File tempDirFile = tempDir.toFile();
        if (tempDirFile.mkdirs()) {
            log.debug("Directory created: {}", tempDir);
        }
        File tempFile = tempDir.resolve(tempFileName).toFile();
        try (ObjectOutputStream outputStream = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(tempFile)))) {
            outputStream.writeObject(this);

            // Copy images to the temp directory
            for (GaiaMaterial material : materials) {
                copyTextures(material, tempDir, lods);
            }
        } catch (Exception e) {
            log.error("[ERROR] GaiaSet Write Error : ", e);
            FileUtils.deleteQuietly(tempFile);
        }
        return tempFile.toPath();
    }

    private void copyTextures(GaiaMaterial material, Path copyDirectory) throws IOException {
        Map<TextureType, List<GaiaTexture>> materialTextures = material.getTextures();
        List<GaiaTexture> diffuseTextures = materialTextures.get(TextureType.DIFFUSE);
        if (diffuseTextures != null && !diffuseTextures.isEmpty()) {
            GaiaTexture texture = materialTextures.get(TextureType.DIFFUSE).get(0);
            String parentPath = texture.getParentPath();
            File parentFile = new File(parentPath);
            String diffusePath = texture.getPath();
            File diffuseFile = new File(diffusePath);

            File imageFile = ImageUtils.correctPath(parentFile, diffuseFile);

            Path imagesFolderPath = copyDirectory.resolve("images");
            if (imagesFolderPath.toFile().mkdirs()) {
                log.debug("Images Directory created: {}", imagesFolderPath);
            }

            Path outputImagePath = imagesFolderPath.resolve(imageFile.getName());
            File outputImageFile = outputImagePath.toFile();

            // check if the source and destination are the same
            if (imageFile.getAbsolutePath().equals(outputImageFile.getAbsolutePath())) {
                return;
            }
            texture.setPath(imageFile.getName());
            if (!imageFile.exists()) {
                log.error("[ERROR] Texture Input Image Path is not exists. {}", diffusePath);
            } else {
                FileUtils.copyFile(imageFile, outputImageFile);
            }
        }
    }

    /**
     * Copy textures to the output directory with scaling.
     */
    private void copyTextures(GaiaMaterial material, Path copyDirectory, List<LevelOfDetail> lods) throws IOException {
        Map<TextureType, List<GaiaTexture>> materialTextures = material.getTextures();
        List<GaiaTexture> diffuseTextures = materialTextures.get(TextureType.DIFFUSE);
        if (diffuseTextures != null && !diffuseTextures.isEmpty()) {
            GaiaTexture texture = materialTextures.get(TextureType.DIFFUSE).get(0);
            String parentPath = texture.getParentPath();
            File parentFile = new File(parentPath);
            String diffusePath = texture.getPath();
            File diffuseFile = new File(diffusePath);

            File imageFile = ImageUtils.correctPath(parentFile, diffuseFile);
            Path imagesFolderPath = copyDirectory.resolve("images");
            if (imagesFolderPath.toFile().mkdirs()) {
                log.debug("Images Directory created: {}", imagesFolderPath);
            }
            String fileFormat = FilenameUtils.getExtension(imageFile.getName()).toLowerCase();

            texture.setPath(imageFile.getName());
            if (!imageFile.exists()) {
                log.error("[ERROR] Texture Input Image Path is not exists. {}", diffusePath);
            } else {
                ImageResizer imageResizer = new ImageResizer();
                BufferedImage bufferedImage;
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(imageFile))) {
                    bufferedImage = ImageIO.read(bis);
                } catch (IOException e) {
                    log.error("[ERROR] Reading image failed: {}", imageFile.getAbsolutePath(), e);
                    return;
                }

                for (LevelOfDetail lod : lods) {
                    int level = lod.getLevel();
                    double scale = lod.getTextureScale();

                    // Generate file name based on LOD level, Level 0 uses original name
                    boolean isOriginalLevel = level == 0;
                    String fileName = !isOriginalLevel ? level + "_" + imageFile.getName() : imageFile.getName();
                    Path outputImagePath = imagesFolderPath.resolve(fileName);
                    File outputImageFile = outputImagePath.toFile();
                    if (outputImageFile.exists()) {
                        log.debug("File already exists, skipping: {}", outputImageFile.getAbsolutePath());
                        continue;
                    }
                    if (isOriginalLevel && scale == 1.0) {
                        // No resizing needed, copy original file
                        FileUtils.copyFile(imageFile, outputImageFile);
                        continue;
                    }
                    int resizeWidth = Math.max(1, (int)Math.round(bufferedImage.getWidth() * scale));
                    int resizeHeight = Math.max(1, (int)Math.round(bufferedImage.getHeight() * scale));
                    BufferedImage resizedImage = imageResizer.resizeImageGraphic2D(bufferedImage, resizeWidth, resizeHeight);
                    //ImageIO.write(resizedImage, fileFormat, outputImageFile);
                    if (fileFormat.equals("jpg") || fileFormat.equals("jpeg")) {
                        writeOnlyJpeg(resizedImage, outputImageFile, 0.8f);
                    } else if (fileFormat.equals("png")) {
                        writeOnlyPng(resizedImage, outputImageFile, 0.1f);
                    }
                }
            }
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
                log.error("[ERROR] Position buffer is null");
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

    public void deleteMaterials() {
        deleteTextures();
        materials.clear();
    }

    public GaiaSet clone() {
        GaiaSet gaiaSet = new GaiaSet();
        gaiaSet.setBufferDataList(new ArrayList<>());
        for (GaiaBufferDataSet bufferData : this.bufferDataList) {
            gaiaSet.getBufferDataList().add(bufferData.clone());
        }
        gaiaSet.setMaterials(new ArrayList<>());
        for (GaiaMaterial material : this.materials) {
            gaiaSet.getMaterials().add(material.clone());
        }
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

        int materialsCount = this.materials.size();
        for (int i = 0; i < materialsCount; i++) {
            GaiaMaterial material = this.materials.get(i);
            material.clear();
        }
        this.materials.clear();
    }

    private void writeOnlyJpeg(BufferedImage bufferedImage, File outputPath, float quality) {
        ImageOutputStream ios = null;
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputPath))) {
            ios = ImageIO.createImageOutputStream(bos);

            // Image compression
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter writer = writers.next();
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            writer.write(null, new IIOImage(bufferedImage, null, null), param); // 5
            bufferedImage.flush();

            bos.flush();
            bos.close();
            ios.close();
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            log.error("[ERROR] Error writing jpeg image");
            if (ios != null) {
                try {
                    ios.close();
                } catch (IOException ex) {
                    log.error("[ERROR] :", ex);
                }
            }
        }
    }

    private void writeOnlyPng(BufferedImage bufferedImage, File outputPath, float quality) {
        ImageOutputStream ios = null;
        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outputPath))) {
            ios = ImageIO.createImageOutputStream(bos);

            // Image compression
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("png");
            ImageWriter writer = writers.next();
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            writer.write(null, new IIOImage(bufferedImage, null, null), param);
            bufferedImage.flush();

            bos.flush();
            bos.close();
            ios.close();
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            log.error("[ERROR] Error writing jpeg image");
            if (ios != null) {
                try {
                    ios.close();
                } catch (IOException ex) {
                    log.error("[ERROR] :", ex);
                }
            }
        }
    }

    private byte[] writeJpeg(BufferedImage bufferedImage, float quality) {
        ByteArrayOutputStream baos = null;
        ImageOutputStream ios = null;
        try {
            baos = new ByteArrayOutputStream();
            ios = ImageIO.createImageOutputStream(baos);

            // Image compression
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter writer = writers.next();
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);

            writer.write(null, new IIOImage(bufferedImage, null, null), param); // 5
            byte[] bytes = baos.toByteArray();
            bufferedImage.flush();

            baos.close();
            ios.close();
            return bytes;
        } catch (IOException e) {
            log.error("[ERROR] :", e);
            log.error("[ERROR] Error writing jpeg image");
            try {
                baos.close();
            } catch (IOException ex) {
                log.error("[ERROR] :", ex);
            }
            if (ios != null) {
                try {
                    ios.close();
                } catch (IOException ex) {
                    log.error("[ERROR] :", ex);
                }
            }
        }
        return null;
    }
}
