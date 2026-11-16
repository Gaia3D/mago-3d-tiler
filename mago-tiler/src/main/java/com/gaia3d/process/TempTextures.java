package com.gaia3d.process;

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
import java.util.List;
import java.util.UUID;

/**
 * Textures of the minimized sets: the original image is referenced where it is, only the resized images of the LODs
 * above 0 are written to the temp images folder, as {@code <images>/<source folder id>/<level>_<name>}.
 */
@Slf4j
public class TempTextures {

    /**
     * Points the diffuse texture of the material to the absolute path of its original image.
     * @return the texture, or null when the material has none
     * @throws FileNotFoundException when the image does not exist
     */
    public static GaiaTexture reference(GaiaMaterial material) throws IOException {
        List<GaiaTexture> diffuseTextures = material.getTextures().get(TextureType.DIFFUSE);
        if (diffuseTextures == null || diffuseTextures.isEmpty()) {
            return null;
        }
        GaiaTexture texture = diffuseTextures.getFirst();
        File imageFile = ImageUtils.correctPath(new File(texture.getParentPath()), new File(texture.getPath())).getAbsoluteFile();
        texture.setParentPath(imageFile.getParent());
        texture.setPath(imageFile.getName());
        return texture;
    }

    /**
     * Folder of the resized images, one per source folder: images with the same name in different folders do not collide.
     */
    public static Path lodFolder(Path imagesPath, GaiaTexture texture) {
        return imagesPath.resolve(UUID.nameUUIDFromBytes(texture.getParentPath().getBytes(StandardCharsets.UTF_8)).toString());
    }

    public static Path lodImage(Path imagesPath, GaiaTexture texture, LevelOfDetail lod) {
        return lodFolder(imagesPath, texture).resolve(lod.getLevel() + "_" + texture.getPath());
    }

    /**
     * Writes the resized images of a referenced texture for the LODs above 0 that are not written yet.
     */
    public static void writeLodImages(GaiaTexture texture, Path imagesPath, List<LevelOfDetail> lods) throws IOException {
        String fileFormat = FilenameUtils.getExtension(texture.getPath()).toLowerCase();
        boolean isJpeg = fileFormat.equals("jpg") || fileFormat.equals("jpeg");
        List<LevelOfDetail> missingLods = lods.stream().filter(lod -> lod.getLevel() > 0 && !lodImage(imagesPath, texture, lod).toFile().exists()).toList();
        if (missingLods.isEmpty() || (!isJpeg && !fileFormat.equals("png"))) {
            return;
        }
        File imageFile = new File(texture.getFullPath());
        BufferedImage bufferedImage;
        try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(imageFile))) {
            bufferedImage = ImageIO.read(bis);
        } catch (IOException e) {
            log.error("[ERROR] Reading image failed: {}", imageFile.getAbsolutePath(), e);
            return;
        }
        if (bufferedImage == null) {
            log.error("[ERROR] Reading image failed: {}", imageFile.getAbsolutePath());
            return;
        }
        Files.createDirectories(lodFolder(imagesPath, texture));

        for (LevelOfDetail lod : missingLods) {
            double scale = lod.getTextureScale();
            int resizeWidth = Math.max(1, (int) Math.round(bufferedImage.getWidth() * scale));
            int resizeHeight = Math.max(1, (int) Math.round(bufferedImage.getHeight() * scale));
            BufferedImage resizedImage = ImageResizer.resizeImageGraphic2D(bufferedImage, resizeWidth, resizeHeight);

            // created exclusively: a thread sharing the image skips it, the images are read only after the pre-process
            Path outputImagePath = lodImage(imagesPath, texture, lod);
            try (OutputStream outputStream = Files.newOutputStream(outputImagePath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
                writeImage(resizedImage, outputStream, isJpeg ? "jpg" : "png", isJpeg ? 0.8f : 0.1f);
            } catch (FileAlreadyExistsException e) {
                log.debug("Resized image already written by another thread: {}", outputImagePath);
            } catch (IOException e) {
                log.error("[ERROR] Writing resized image failed: {}", outputImagePath, e);
                Files.deleteIfExists(outputImagePath);
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
