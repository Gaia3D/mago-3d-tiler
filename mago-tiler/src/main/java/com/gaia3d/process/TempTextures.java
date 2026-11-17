package com.gaia3d.process;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.LevelOfDetail;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.util.ImageResizer;
import com.gaia3d.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Textures of the minimized sets: the original image is referenced where it is, only the resized images of the LODs
 * above 0 are written to the temp images folder, as {@code <images>/<source folder id>/<level>_<name>}.
 * An image extracted by a loader (an {@code embedded_textures} folder) is copied to the images folder first.
 */
@Slf4j
public class TempTextures {
    private static final String EMBEDDED_TEXTURES = "embedded_textures";

    /**
     * Points the diffuse textures of the set to their images: an input image is referenced where it is, an image
     * extracted by the loader into an {@code embedded_textures} folder is copied to {@code <imagesPath>/<random id>/<name>},
     * as that folder is shared by the models of the input folder and its images are overwritten by later models.
     * A missing image is logged and its texture removed, the geometry is kept.
     * @return the referenced textures
     */
    public static List<GaiaTexture> referenceTextures(GaiaSet set, Path imagesPath) {
        List<GaiaTexture> referenced = new ArrayList<>();
        if (set.getMaterials() == null) {
            return referenced;
        }
        Path copyFolder = null; // one folder per set, created on the first embedded image
        for (GaiaMaterial material : set.getMaterials()) {
            GaiaTexture texture = reference(material);
            if (texture == null) {
                continue;
            }
            referenced.add(texture);
            if (!isLoaderImage(texture)) {
                continue;
            }
            Path source = Path.of(texture.getFullPath());
            try {
                if (copyFolder == null) {
                    copyFolder = Files.createDirectories(imagesPath.resolve(UUID.randomUUID().toString())).toAbsolutePath().normalize();
                }
                Path copy = copyFolder.resolve(texture.getPath());
                if (!Files.exists(copy)) {
                    Files.copy(source, copy);
                }
                texture.setParentPath(copyFolder.toString());
            } catch (IOException e) {
                log.error("[ERROR] Copying the embedded texture failed, it is referenced where it is: {}", source, e);
            }
        }
        return referenced;
    }

    /**
     * Points the diffuse texture of the material to the absolute path of its original image.
     * A missing image is logged and the diffuse textures of the material are removed.
     * @return the texture, or null when the material has none or its image is missing
     */
    public static GaiaTexture reference(GaiaMaterial material) {
        List<GaiaTexture> diffuseTextures = material.getTextures().get(TextureType.DIFFUSE);
        if (diffuseTextures == null || diffuseTextures.isEmpty()) {
            return null;
        }
        GaiaTexture texture = diffuseTextures.getFirst();
        if (texture == null || texture.getPath() == null) {
            return null;
        }
        File imageFile;
        try {
            String parentPath = texture.getParentPath() == null ? "" : texture.getParentPath();
            imageFile = ImageUtils.correctPath(new File(parentPath), new File(texture.getPath()));
        } catch (IOException e) {
            log.error("[ERROR] Texture Input Image Path is not exists, the texture is removed: {} ({})", texture.getPath(), e.getMessage());
            material.getTextures().put(TextureType.DIFFUSE, new ArrayList<>());
            return null;
        }
        Path imagePath = imageFile.toPath().toAbsolutePath().normalize();
        texture.setParentPath(imagePath.getParent().toString());
        texture.setPath(imagePath.getFileName().toString());
        return texture;
    }

    private static boolean isLoaderImage(GaiaTexture texture) {
        for (Path element : Path.of(texture.getParentPath())) {
            if (EMBEDDED_TEXTURES.equalsIgnoreCase(element.toString())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Folder of the resized images, one per source folder: images with the same name in different folders do not collide.
     */
    public static Path lodFolder(Path imagesPath, GaiaTexture texture) {
        String parentPath = Path.of(texture.getParentPath()).toAbsolutePath().normalize().toString();
        return imagesPath.resolve(UUID.nameUUIDFromBytes(parentPath.getBytes(StandardCharsets.UTF_8)).toString());
    }

    public static Path lodImage(Path imagesPath, GaiaTexture texture, LevelOfDetail lod) {
        return lodFolder(imagesPath, texture).resolve(lod.getLevel() + "_" + texture.getPath());
    }

    /**
     * Writes the resized images of a referenced texture for the LODs above 0 that are not written yet.
     * The images are optional (without one the texture is resized on the fly): a failure is logged and the next LOD
     * is written, nothing is thrown.
     */
    public static void writeLodImages(GaiaTexture texture, Path imagesPath, List<LevelOfDetail> lods) {
        String fileFormat = FilenameUtils.getExtension(texture.getPath()).toLowerCase();
        boolean isJpeg = fileFormat.equals("jpg") || fileFormat.equals("jpeg");
        if (!isJpeg && !fileFormat.equals("png")) {
            return;
        }
        List<LevelOfDetail> missingLods = lods.stream().filter(lod -> lod.getLevel() > 0 && !lodImage(imagesPath, texture, lod).toFile().exists()).toList();
        if (missingLods.isEmpty()) {
            return;
        }
        File imageFile = new File(texture.getFullPath());
        BufferedImage bufferedImage;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(imageFile))) {
            bufferedImage = ImageIO.read(bis);
        } catch (IOException | RuntimeException e) {
            log.error("[ERROR] Reading image failed: {}", imageFile.getAbsolutePath(), e);
            return;
        }
        if (bufferedImage == null) {
            log.error("[ERROR] Reading image failed: {}", imageFile.getAbsolutePath());
            return;
        }
        Path lodFolder = lodFolder(imagesPath, texture);
        try {
            Files.createDirectories(lodFolder);
        } catch (IOException e) {
            log.error("[ERROR] Creating the resized image folder failed: {}", lodFolder, e);
            return;
        }
        for (LevelOfDetail lod : missingLods) {
            writeLodImage(bufferedImage, lodImage(imagesPath, texture, lod), lod.getTextureScale(), isJpeg);
        }
        bufferedImage.flush();
    }

    // created exclusively: a thread sharing the image skips it, the images are read only after the pre-process.
    // Only a file opened here is deleted on failure.
    private static void writeLodImage(BufferedImage bufferedImage, Path outputImagePath, double scale, boolean isJpeg) {
        boolean created = false;
        try {
            int resizeWidth = Math.max(1, (int) Math.round(bufferedImage.getWidth() * scale));
            int resizeHeight = Math.max(1, (int) Math.round(bufferedImage.getHeight() * scale));
            BufferedImage resizedImage = ImageResizer.resizeImageGraphic2D(bufferedImage, resizeWidth, resizeHeight);
            try (OutputStream outputStream = Files.newOutputStream(outputImagePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                created = true;
                writeImage(resizedImage, outputStream, isJpeg ? "jpg" : "png", isJpeg ? 0.8f : 0.1f);
            }
        } catch (FileAlreadyExistsException e) {
            log.debug("Resized image already written by another thread: {}", outputImagePath);
        } catch (IOException | RuntimeException e) {
            log.error("[ERROR] Writing resized image failed: {}", outputImagePath, e);
            if (created) {
                try {
                    Files.deleteIfExists(outputImagePath);
                } catch (IOException deleteException) {
                    log.warn("[WARN] Deleting the partial resized image failed: {}", outputImagePath, deleteException);
                }
            }
        }
    }

    /**
     * Loads the written image of the LOD into the texture, as {@link GaiaTexture#getBufferedImage(LevelOfDetail)} does
     * with an image next to the original. Without a written image the texture keeps resizing the original itself.
     */
    public static void loadLodImage(GaiaTexture texture, Path imagesPath, LevelOfDetail lod) {
        if (lod.getLevel() <= 0 || texture.getParentPath() == null || texture.getPath() == null) {
            return;
        }
        if (texture.getPureBufferedImage() != null && texture.getBufferedImageLod() == lod) {
            return;
        }
        File lodImageFile = lodImage(imagesPath, texture, lod).toFile();
        if (!lodImageFile.isFile()) {
            return;
        }
        try (BufferedInputStream stream = new BufferedInputStream(new FileInputStream(lodImageFile))) {
            BufferedImage bufferedImage = ImageIO.read(stream);
            if (bufferedImage != null) {
                texture.deleteBufferedImage();
                texture.setBufferedImage(bufferedImage);
                texture.setBufferedImageLod(lod);
                texture.setWidth(bufferedImage.getWidth());
                texture.setHeight(bufferedImage.getHeight());
                texture.setFormat(bufferedImage.getType());
            }
        } catch (IOException e) {
            log.error("[ERROR] Reading image failed: {}", lodImageFile.getAbsolutePath(), e);
        }
    }

    private static void writeImage(BufferedImage bufferedImage, OutputStream outputStream, String formatName, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName(formatName).next();
        try (BufferedOutputStream bos = new BufferedOutputStream(outputStream); ImageOutputStream ios = new MemoryCacheImageOutputStream(bos)) {
            writer.setOutput(ios);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(bufferedImage, null, null), param);
        } finally {
            writer.dispose();
            bufferedImage.flush();
        }
    }
}
