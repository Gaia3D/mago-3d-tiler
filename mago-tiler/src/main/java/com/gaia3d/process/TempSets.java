package com.gaia3d.process;

import com.gaia3d.basic.exchangable.GaiaSet;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.command.mago.GlobalOptions;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Intermediate sets stored as they are, for the photogrammetry paths.
 * Unlike {@code GaiaSet.writeFile*} no texture is copied next to the set, and unlike {@code GaiaSet.readFile} the
 * texture paths are not rewritten to {@code <dir>/images/<name>} when the set is read back: the writer points the
 * textures to the images to use, the original image (see {@link TempTextures#reference}) or a newly written one.
 * <p>
 * A set written to a {@link TempPack} keeps the path it is named by, the paths are passed around (tile infos, scene
 * infos of the renderers) and {@link #read(Path)} resolves them to the pack entry. No file exists at that path.
 */
@Slf4j
public final class TempSets {
    private static final Map<Path, TempPack.Entry> ENTRIES = new ConcurrentHashMap<>();
    private static final String EMBEDDED_TEXTURES = "embedded_textures";

    private TempSets() {
    }

    /**
     * The pack directory of the sets in the temp directory of the run.
     */
    public static Path packDirectory() {
        return Path.of(GlobalOptions.getInstance().getTempPath(), "pack");
    }

    /**
     * Appends the set to the pack of the calling thread, to be read back by {@link #read(Path)} with the given name.
     * @param name a unique path naming the set, no file is created there
     * @return the name
     */
    public static Path write(GaiaSet set, Path packDirectory, Path name) throws IOException {
        TempPack.Entry entry = TempPack.of(packDirectory).write(outputStream -> serialize(set, outputStream));
        if (ENTRIES.put(key(name), entry) != null) {
            log.warn("[WARN] Temp set written twice, the last one is kept: {}", name);
        }
        return name;
    }

    /**
     * Writes the set to its own file, for a set that must exist as a file.
     */
    public static Path writeFile(GaiaSet set, Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        // created exclusively, so that only a file written here is deleted on failure
        OutputStream fileStream = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        try (OutputStream outputStream = new BufferedOutputStream(fileStream)) {
            serialize(set, outputStream);
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(file);
            throw e;
        }
        return file;
    }

    /**
     * Points the diffuse textures of the set to their images: an input image is referenced where it is, an image
     * extracted by the loader into an {@code embedded_textures} folder is copied to {@code <imagesPath>/<id>/<name>},
     * as that folder is shared by the models of the input folder and its images are overwritten by later models.
     * A missing or failed image is logged and the texture is kept as it is, the set is written anyway.
     */
    public static void referenceTextures(GaiaSet set, Path imagesPath) {
        if (set.getMaterials() == null) {
            return;
        }
        for (GaiaMaterial material : set.getMaterials()) {
            GaiaTexture texture;
            try {
                texture = TempTextures.reference(material);
            } catch (IOException e) {
                log.error("[ERROR] Texture Input Image Path is not exists. {}", e.getMessage());
                continue;
            }
            if (texture == null || !isLoaderImage(texture)) {
                continue;
            }
            Path source = Path.of(texture.getFullPath());
            Path copy = imagesPath.resolve(UUID.randomUUID().toString()).resolve(texture.getPath());
            try {
                Files.createDirectories(copy.getParent());
                Files.copy(source, copy);
                texture.setParentPath(copy.getParent().toString());
            } catch (IOException e) {
                log.error("[ERROR] Copying the embedded texture failed, it is referenced where it is: {}", source, e);
            }
        }
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
     * Reads a set written by {@link #write} or {@link #writeFile}, with the texture paths it was written with.
     */
    public static GaiaSet read(Path path) throws IOException {
        TempPack.Entry entry = ENTRIES.get(key(path));
        try (InputStream inputStream = entry != null ? TempPack.of(entry.pack().getParent()).read(entry) : new BufferedInputStream(Files.newInputStream(path))) {
            ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);
            return (GaiaSet) objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            throw new IOException("Failed to read the temp set: " + path, e);
        }
    }

    /**
     * Copies the image paths and sizes of the textures of the materials to the same textures of the set.
     * The cut writers save the images of the materials of the half-edge scene, while the set holds clones of those
     * materials made before: {@code GaiaSet.readFile} hid that by rewriting every texture to the images folder.
     */
    public static void copyTexturePaths(List<GaiaMaterial> source, GaiaSet set) {
        List<GaiaMaterial> target = set.getMaterials();
        if (source == null || target == null) {
            return;
        }
        if (source.size() != target.size()) {
            log.warn("[WARN] Can not copy texture paths, {} materials for {}", source.size(), target.size());
            return;
        }
        for (int i = 0; i < source.size(); i++) {
            Map<TextureType, List<GaiaTexture>> targetTextures = target.get(i).getTextures();
            source.get(i).getTextures().forEach((type, sourceList) -> {
                List<GaiaTexture> targetList = targetTextures.get(type);
                if (targetList == null || targetList.size() != sourceList.size()) {
                    return;
                }
                for (int j = 0; j < sourceList.size(); j++) {
                    GaiaTexture from = sourceList.get(j);
                    GaiaTexture to = targetList.get(j);
                    if (from == null || to == null || from == to) {
                        continue;
                    }
                    to.setParentPath(from.getParentPath());
                    to.setPath(from.getPath());
                    to.setWidth(from.getWidth());
                    to.setHeight(from.getHeight());
                }
            });
        }
    }

    /**
     * Points the textures without a parent path to the images folder of the set, as {@code GaiaSet.readFile} does
     * for every texture: a texture whose image could not be made (an atlas without image) then names a missing file
     * instead of failing on a null path.
     */
    public static void resolveMissingParents(GaiaSet set, Path imagesPath) {
        if (set.getMaterials() == null) {
            return;
        }
        for (GaiaMaterial material : set.getMaterials()) {
            material.getTextures().forEach((type, textures) -> {
                for (GaiaTexture texture : textures) {
                    if (texture != null && texture.getPath() != null && (texture.getParentPath() == null || texture.getParentPath().isEmpty())) {
                        texture.setParentPath(imagesPath.toString());
                        texture.setPath(new File(texture.getPath()).getName());
                    }
                }
            });
        }
    }

    public static boolean exists(Path path) {
        return path != null && (ENTRIES.containsKey(key(path)) || Files.isRegularFile(path));
    }

    /**
     * Forgets the packed sets, called with {@link TempPack#closeAll()} at the end of a run.
     */
    public static void clear() {
        ENTRIES.clear();
    }

    private static void serialize(GaiaSet set, OutputStream outputStream) throws IOException {
        ObjectOutputStream objectOutputStream = new ObjectOutputStream(outputStream);
        objectOutputStream.writeObject(set);
        objectOutputStream.flush();
    }

    private static Path key(Path path) {
        return path.toAbsolutePath().normalize();
    }
}
