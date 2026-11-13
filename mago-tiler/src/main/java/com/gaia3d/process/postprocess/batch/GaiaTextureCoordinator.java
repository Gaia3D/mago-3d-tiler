package com.gaia3d.process.postprocess.batch;

import com.gaia3d.basic.exchangable.GaiaBuffer;
import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.LevelOfDetail;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.command.mago.GlobalConstants;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.util.ImageResizer;
import com.gaia3d.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import org.joml.Vector4d;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

@Slf4j
public class GaiaTextureCoordinator {
    private final String ATLAS_IMAGE;
    private final Color CLAMP_COLOR = new Color(255, 255, 0);
    private final Color BACKGROUND_COLOR = new Color(10, 10, 10);
    private final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);
    private static final int CROP_PADDING = 4; // source texels kept around the used UV region

    private final List<GaiaMaterial> materials;
    private final List<GaiaBufferDataSet> bufferDataSets;
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();
    // planned sources by material, shared between the grouping coordinator and the per-group coordinators so each
    // source is decoded once
    private final Map<GaiaMaterial, GaiaBatchImage> plannedImages;
    private BufferedImage atlasImage;

    public GaiaTextureCoordinator(String name, List<GaiaMaterial> materials, List<GaiaBufferDataSet> bufferDataSets) {
        this(name, materials, bufferDataSets, new IdentityHashMap<>());
    }

    public GaiaTextureCoordinator(String name, List<GaiaMaterial> materials, List<GaiaBufferDataSet> bufferDataSets, Map<GaiaMaterial, GaiaBatchImage> plannedImages) {
        this.ATLAS_IMAGE = name;
        this.materials = materials;
        this.bufferDataSets = bufferDataSets;
        this.plannedImages = plannedImages;
        this.initBatchImage(0, 0, BufferedImage.TYPE_INT_ARGB);
    }

    public Map<GaiaMaterial, GaiaBatchImage> getPlannedImages() {
        return plannedImages;
    }

    private GaiaBatchImage getPlannedImage(GaiaMaterial material, LevelOfDetail lod, boolean isPhotorealistic) {
        GaiaBatchImage planned = plannedImages.computeIfAbsent(material, key -> planSourceImage(key, lod, isPhotorealistic));
        planned.setMaterialId(material.getId()); // ids are renumbered per atlas group after planning
        return planned;
    }

    private void initBatchImage(int width, int height, int imageType) {
        // imageType :
        // TYPE_INT_RGB = 1
        // TYPE_INT_ARGB = 2
        // TYPE_4BYTE_ABGR = 6
        if (width > 0 || height > 0) {
            this.atlasImage = new BufferedImage(width, height, imageType);
            // now fill the image with white fuchsia
            Graphics2D graphics = this.atlasImage.createGraphics();

            if (globalOptions.isPhotogrammetry()) {
                graphics.setColor(CLAMP_COLOR);
                graphics.fillRect(0, 0, width, height);
            } else {
                graphics.setColor(TRANSPARENT_COLOR);
                graphics.fillRect(0, 0, width, height);
            }
        } else {
            this.atlasImage = null;
        }
    }

    private BufferedImage createShamImage() {
        BufferedImage bufferedImage = new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = bufferedImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 8, 8);
        graphics.dispose();
        return bufferedImage;
    }

    public BufferedImage batchTextures(LevelOfDetail lod) {
        // We have MaterialList & BufferDataSetList.
        // 1- List<GaiaMaterial> this.materials;
        // 2- List<GaiaBufferDataSet> this.bufferDataSets;
        boolean isPhotorealistic = globalOptions.isPhotogrammetry();

        // 1rst, make a list of GaiaBatchImage (splitImage), sized to the used UV region and the LOD texel budget
        List<GaiaBatchImage> splitImages = new ArrayList<>();
        boolean existPngTextures = false;
        for (GaiaMaterial material : materials) {
            List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);
            if (!textures.isEmpty() && (textures.get(0).getPath().endsWith(".png") || textures.get(0).getPath().endsWith(".PNG"))) {
                existPngTextures = true;
            }
            splitImages.add(getPlannedImage(material, lod, isPhotorealistic));
        }

        // pack once at the planned size, then shrink the sources so the packed atlas fits the LOD cap; the full-size
        // atlas is never allocated and each source is resampled only once
        int atlasMaxSize = getBatchAtlasMaxSize(lod);
        int[] sourceWidths = splitImages.stream().mapToInt(splitImage -> (int) splitImage.getOriginBoundary().getWidth()).toArray();
        int[] sourceHeights = splitImages.stream().mapToInt(splitImage -> (int) splitImage.getOriginBoundary().getHeight()).toArray();
        int[] widths = sourceWidths;
        int[] heights = sourceHeights;
        GaiaAtlasPacker.Result packed = GaiaAtlasPacker.pack(widths, heights);
        double sourceScale = 1.0;
        for (int attempt = 0; !isPhotorealistic && attempt < 8 && packed.longestSide() > atlasMaxSize; attempt++) {
            sourceScale *= atlasMaxSize / (double) packed.longestSide();
            widths = scaleSizes(sourceWidths, sourceScale);
            heights = scaleSizes(sourceHeights, sourceScale);
            packed = GaiaAtlasPacker.pack(widths, heights);
        }
        log.debug("Packed atlas {}: {} images, {}x{}, source scale {}, sources {}x{}", ATLAS_IMAGE, splitImages.size(), packed.width(), packed.height(),
                sourceScale, Arrays.toString(sourceWidths), Arrays.toString(sourceHeights));
        for (int i = 0; i < splitImages.size(); i++) {
            GaiaBatchImage splitImage = splitImages.get(i);
            Vector2d minPoint = new Vector2d(packed.xs()[i], packed.ys()[i]);
            splitImage.setOriginBoundary(new GaiaRectangle(new Vector2d(0, 0), new Vector2d(widths[i], heights[i])));
            splitImage.setBatchedBoundary(new GaiaRectangle(minPoint, new Vector2d(minPoint).add(widths[i], heights[i])));
        }

        // 4x4 aligned so block-compressed (KTX2) atlases need no stretch; UVs are normalized by this size
        int maxWidth = alignToBlock(getMaxWidth(splitImages));
        int maxHeight = alignToBlock(getMaxHeight(splitImages));
        int imageType = existPngTextures ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        // existPngTextures
        initBatchImage(maxWidth, maxHeight, imageType);
        if (this.atlasImage == null) {
            log.error("[ERROR] AtlasImage is null");
            return null;
        }

        Graphics2D graphics = this.atlasImage.createGraphics();
        graphics.setComposite(AlphaComposite.Src);

        for (GaiaBatchImage splitImage : splitImages) {
            GaiaRectangle splitRectangle = splitImage.getBatchedBoundary();
            GaiaMaterial material = findMaterial(splitImage.getMaterialId());

            Map<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
            if (!textures.isEmpty()) {
                BufferedImage source = splitImage.getPreparedImage();
                if (source == null) {
                    source = textures.getFirst().getBufferedImage();
                    int[] crop = splitImage.getSourceCrop();
                    if (crop[0] != 0 || crop[1] != 0 || crop[2] != source.getWidth() || crop[3] != source.getHeight()) {
                        source = source.getSubimage(crop[0], crop[1], crop[2] - crop[0], crop[3] - crop[1]);
                    }
                }
                int targetWidth = (int) splitRectangle.getWidth();
                int targetHeight = (int) splitRectangle.getHeight();
                if (source.getWidth() != targetWidth || source.getHeight() != targetHeight) {
                    source = ImageResizer.resizeMultiStepSmart(source, targetWidth, targetHeight);
                }
                graphics.drawImage(source, (int) splitRectangle.getMinX(), (int) splitRectangle.getMinY(), null); // original code
                //graphics.drawImage(randomColoredImage, (int) splitRectangle.getMinX(), (int) splitRectangle.getMinY(), null); // test code
            }
        }
        graphics.dispose();

        // the atlas material is written white, so bake each material's own color factor into its region
        for (GaiaBatchImage splitImage : splitImages) {
            GaiaRectangle rectangle = splitImage.getBatchedBoundary();
            tintRegion(this.atlasImage, (int) rectangle.getMinX(), (int) rectangle.getMinY(), (int) rectangle.getWidth(), (int) rectangle.getHeight(),
                    findMaterial(splitImage.getMaterialId()).getDiffuseColor());
        }

        for (GaiaBatchImage target : splitImages) {
            GaiaRectangle splitRectangle = target.getBatchedBoundary();

            int width = (int) splitRectangle.getMaxX() - (int) splitRectangle.getMinX();
            int height = (int) splitRectangle.getMaxY() - (int) splitRectangle.getMinY();
            double pixelXSize = 1.0 / splitRectangle.getWidth();
            double pixelYSize = 1.0 / splitRectangle.getHeight();

            GaiaMaterial material = findMaterial(target.getMaterialId());
            Map<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);

            GaiaTexture texture = null;
            if (!textures.isEmpty()) {
                texture = textures.get(0);
            } else {
                texture = new GaiaTexture();
                texture.setType(TextureType.DIFFUSE);
                textures.add(texture);
            }

            texture.setBufferedImage(this.atlasImage);
            texture.setWidth(maxWidth);
            texture.setHeight(maxHeight);

            if (existPngTextures) {
                texture.setPath(ATLAS_IMAGE + ".png");
            } else {
                texture.setPath(ATLAS_IMAGE + ".jpg");
            }

            List<GaiaBufferDataSet> materialBufferDataSets = bufferDataSets.stream().filter((bufferDataSet) -> bufferDataSet.getMaterialId() == target.getMaterialId()).toList();

            int[] crop = target.getSourceCrop();
            double cropWidth = crop[2] - crop[0];
            double cropHeight = crop[3] - crop[1];
            for (GaiaBufferDataSet materialBufferDataSet : materialBufferDataSets) {
                GaiaBuffer texcoordBuffer = materialBufferDataSet.getBuffers().get(AttributeType.TEXCOORD);
                if (texcoordBuffer != null) {
                    float[] texcoords = texcoordBuffer.getFloats();

                    for (int i = 0; i < texcoords.length; i += 2) {
                        double u, v;
                        double u2, v2;

                        // source UV -> UV inside the cropped region
                        u = (toUnitTexcoord(texcoords[i]) * target.getSourceWidth() - crop[0]) / cropWidth;
                        v = (toUnitTexcoord(texcoords[i + 1]) * target.getSourceHeight() - crop[1]) / cropHeight;

                        // clamp the u, v values
                        if (u < pixelXSize) {
                            u = pixelXSize;
                        } else if (u > 1.0 - pixelXSize) {
                            u = 1.0 - pixelXSize;
                        }

                        if (v < pixelYSize) {
                            v = pixelYSize;
                        } else if (v > 1.0 - pixelYSize) {
                            v = 1.0 - pixelYSize;
                        }
                        // end clamp the u, v values.---

                        if (u < 0.0) {
                            u = 1.0 + u;
                        }

                        // "width" is the width of the splitRectangle
                        u2 = (splitRectangle.getMinX() + u * width) / maxWidth;
                        v2 = (splitRectangle.getMinY() + v * height) / maxHeight;

                        texcoords[i] = (float) (u2);
                        texcoords[i + 1] = (float) (v2);
                    }
                }
            }
        }

        /* Only Photogrammetry Mode */
        if (isPhotorealistic) {
            /* debug */
            if (globalOptions.isDebug()) {
                writeAtlasImageForTest(existPngTextures, lod, "-before");
            }

            // limit the max image size to 4096
            int lodLevel = lod.getLevel();
            int imageWidth = this.atlasImage.getWidth();
            int imageHeight = this.atlasImage.getHeight();
            float ratio = (float) imageWidth / imageHeight;

            int lod0size = GlobalConstants.REALISTIC_LOD0_MAX_TEXTURE_SIZE;
            int maximumSize = GlobalConstants.REALISTIC_MAX_TEXTURE_SIZE;
            int minimumSize = GlobalConstants.REALISTIC_MIN_TEXTURE_SIZE;

            double scaleFactor = lod.getRealisticScale();

            if (lodLevel == 0) {
                if (imageWidth > lod0size) {
                    imageWidth = lod0size;
                } else if (imageWidth < minimumSize) {
                    imageWidth = minimumSize;
                }
                if (imageHeight > lod0size) {
                    imageHeight = lod0size;
                } else if (imageHeight < minimumSize) {
                    imageHeight = minimumSize;
                }
            } else {
                if (imageWidth > maximumSize) {
                    imageWidth = maximumSize;
                } else if (imageWidth < minimumSize) {
                    imageWidth = minimumSize;
                }
                if (imageHeight > maximumSize) {
                    imageHeight = maximumSize;
                } else if (imageHeight < minimumSize) {
                    imageHeight = minimumSize;
                }
            }

            // clamp the backGroundColor
            //BufferedImage clamped = ImageUtils.clampBackGroundColor(this.atlasImage, CLAMP_COLOR, 1, 20);

            BufferedImage clamped = this.atlasImage;
            clamped = ImageUtils.changeBackgroundColor(clamped, CLAMP_COLOR, BACKGROUND_COLOR);
            // New way to eliminate backGround color.**********************************************************
            // activate code for test.***
            // TextureAtlasManager textureAtlasManager = new TextureAtlasManager();
            // textureAtlasManager.dilateBackgroundColor(clamped, CLAMP_COLOR);
            // End new way to eliminate backGround color.------------------------------------------------------
            Graphics2D graphics2D = this.atlasImage.createGraphics();
            graphics2D.drawImage(clamped, 0, 0, null);
            graphics2D.dispose();

            boolean sizeChanged = (atlasImage.getWidth() != imageWidth) || (atlasImage.getHeight() != imageHeight);
            if (sizeChanged) {
                log.debug("=== Batching textures is done ===");
                log.debug(" - atlasImage name : {}", (ATLAS_IMAGE + "_L" + lod.getLevel()));
                log.debug(" - splitImages count : {}", splitImages.size());
                log.debug(" - originalWidth : {}", this.atlasImage.getWidth());
                log.debug(" - originalHeight : {}", this.atlasImage.getHeight());
                log.debug(" - resizeWidth : {}", imageWidth);
                log.debug(" - resizeHeight : {}", imageHeight);
                log.debug(" - ratio : {}", ratio);
                log.debug(" - scaleFactor : {}", scaleFactor);
                log.debug(" - lodLevel : {}", lodLevel);
                log.debug("==================================");
                this.atlasImage = ImageResizer.resizeMultiStepSmart(this.atlasImage, imageWidth, imageHeight);
//                if (lodLevel == 0) {
//                    this.atlasImage = imageResizer.resizeImageGraphic2D(this.atlasImage, imageWidth, imageHeight, true);
//                } else {
//                    //this.atlasImage = imageResizer.resizeMultiStepSmart(this.atlasImage, imageWidth, imageHeight);
//                    this.atlasImage = imageResizer.resizeImageGraphic2D(this.atlasImage, imageWidth, imageHeight, true);
//                }
            }
        } else {
            limitAtlasSize(atlasMaxSize);
        }

        updateAtlasTextureMetadata(maxWidth, maxHeight);

        /* debug */
        if (globalOptions.isDebug()) {
            writeAtlasImageForTest(existPngTextures, lod, "-after");
        }

        return this.atlasImage;
    }

    /**
     * Source image of a material as it goes into the atlas: cropped to the region its UVs use and scaled down to the
     * LOD texel budget (never up). The planned size is the origin boundary. Outside photogrammetry the budget is measured
     * against the full-resolution source, since it replaces the fixed per-LOD texture scale; the image already loaded for
     * the LOD is reused when it has enough resolution, otherwise the original is decoded. Only the prepared (cropped and
     * scaled) image is kept.
     */
    private GaiaBatchImage planSourceImage(GaiaMaterial material, LevelOfDetail lod, boolean isPhotorealistic) {
        List<GaiaTexture> textures = material.getTextures().get(TextureType.DIFFUSE);
        GaiaTexture texture = textures.isEmpty() ? null : textures.get(0);
        GaiaBatchImage splitImage = new GaiaBatchImage();
        splitImage.setMaterialId(material.getId());
        if (texture == null || isPhotorealistic) {
            BufferedImage bufferedImage;
            if (texture == null) {
                bufferedImage = createShamImage();
            } else {
                // Reload the texture with the original image
                texture.setBufferedImage(null);
                bufferedImage = texture.getBufferedImage();
            }
            int width = bufferedImage.getWidth();
            int height = bufferedImage.getHeight();
            splitImage.setOriginBoundary(new GaiaRectangle(new Vector2d(0, 0), new Vector2d(width, height)));
            splitImage.setSourceCrop(new int[]{0, 0, width, height});
            splitImage.setSourceWidth(width);
            splitImage.setSourceHeight(height);
            return splitImage;
        }

        List<GaiaBufferDataSet> materialDataSets = findDataSets(material.getId());
        List<float[]> texcoords = materialDataSets.stream().map(dataSet -> dataSet.getBuffers().get(AttributeType.TEXCOORD))
                .filter(Objects::nonNull).map(GaiaBuffer::getFloats).toList();
        BufferedImage loaded = texture.hasBufferedImage() ? texture.getBufferedImage() : null;
        int[] originalSize = readImageSize(new File(texture.getFullPath()));
        if (originalSize == null) {
            texture.setBufferedImage(null);
            loaded = texture.getBufferedImage();
            if (loaded == null) {
                loaded = createShamImage();
            }
            originalSize = new int[]{loaded.getWidth(), loaded.getHeight()};
        }

        int[] originalCrop = getUsedSourceRegion(texcoords, originalSize[0], originalSize[1], CROP_PADDING);
        double scale = getDensityScale(getTexelDensity(materialDataSets, originalSize[0], originalSize[1]), getTargetTexelDensity(lod));
        int plannedWidth = Math.max(1, (int) Math.round((originalCrop[2] - originalCrop[0]) * scale));
        int plannedHeight = Math.max(1, (int) Math.round((originalCrop[3] - originalCrop[1]) * scale));

        boolean loadedIsEnough = loaded != null && loaded.getWidth() >= originalSize[0] * scale * 0.99 && loaded.getHeight() >= originalSize[1] * scale * 0.99;
        BufferedImage source = loadedIsEnough ? loaded : null;
        if (source == null) {
            texture.setBufferedImage(null);
            source = texture.getBufferedImage();
            if (source == null) {
                source = loaded != null ? loaded : createShamImage();
            }
        }
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int[] crop = getUsedSourceRegion(texcoords, sourceWidth, sourceHeight, CROP_PADDING);

        splitImage.setOriginBoundary(new GaiaRectangle(new Vector2d(0, 0), new Vector2d(plannedWidth, plannedHeight)));
        splitImage.setSourceCrop(crop);
        splitImage.setSourceWidth(sourceWidth);
        splitImage.setSourceHeight(sourceHeight);

        BufferedImage prepared = source;
        if (crop[0] != 0 || crop[1] != 0 || crop[2] != sourceWidth || crop[3] != sourceHeight) {
            prepared = prepared.getSubimage(crop[0], crop[1], crop[2] - crop[0], crop[3] - crop[1]);
        }
        if (prepared.getWidth() != plannedWidth || prepared.getHeight() != plannedHeight) {
            prepared = ImageResizer.resizeMultiStepSmart(prepared, plannedWidth, plannedHeight);
        } else if (prepared != source) {
            prepared = copyImage(prepared); // a sub-image would keep the whole source raster alive
        }
        splitImage.setPreparedImage(prepared);
        texture.setBufferedImage(null);
        return splitImage;
    }

    /**
     * Width and height from the image header, without decoding; null when no reader handles the file.
     */
    static int[] readImageSize(File file) {
        if (!file.isFile()) {
            return null;
        }
        try (ImageInputStream inputStream = ImageIO.createImageInputStream(file)) {
            Iterator<ImageReader> readers = inputStream != null ? ImageIO.getImageReaders(inputStream) : Collections.emptyIterator();
            if (!readers.hasNext()) {
                return null;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(inputStream, true, true);
                return new int[]{reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        } catch (IOException e) {
            log.debug("Unable to read the image size of {}", file, e);
            return null;
        }
    }

    private static BufferedImage copyImage(BufferedImage image) {
        int type = image.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_ARGB : image.getType();
        BufferedImage copy = new BufferedImage(image.getWidth(), image.getHeight(), type);
        Graphics2D graphics = copy.createGraphics();
        graphics.setComposite(AlphaComposite.Src);
        graphics.drawImage(image, 0, 0, null);
        graphics.dispose();
        return copy;
    }

    /**
     * Splits the materials into groups whose planned sources fit one atlas of the LOD cap each, so a large tile gets
     * several atlases instead of one heavily shrunk atlas. Beyond the atlas count limit the remaining sources go to the
     * emptiest group and batchTextures shrinks that group to fit.
     */
    public List<List<GaiaMaterial>> groupMaterialsForAtlases(LevelOfDetail lod) {
        if (globalOptions.isPhotogrammetry()) {
            return List.of(materials);
        }
        List<GaiaBatchImage> planned = materials.stream().map(material -> getPlannedImage(material, lod, false)).toList();
        int[] widths = planned.stream().mapToInt(image -> (int) image.getOriginBoundary().getWidth()).toArray();
        int[] heights = planned.stream().mapToInt(image -> (int) image.getOriginBoundary().getHeight()).toArray();
        List<List<Integer>> groups = groupForAtlases(widths, heights, getBatchAtlasMaxSize(lod), GlobalConstants.DEFAULT_BATCH_ATLAS_MAX_COUNT);
        return groups.stream().map(group -> group.stream().map(materials::get).toList()).toList();
    }

    /**
     * First-fit decreasing by area. An atlas counts as full at 85% of its cap area, which the MaxRects packer reaches
     * for mixed sizes; a slightly over-full group is shrunk a little by batchTextures.
     */
    static List<List<Integer>> groupForAtlases(int[] widths, int[] heights, int atlasMaxSize, int maxCount) {
        long capacity = (long) (atlasMaxSize * (double) atlasMaxSize * 0.85);
        Integer[] order = new Integer[widths.length];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingLong((Integer i) -> (long) widths[i] * heights[i]).reversed());
        List<List<Integer>> groups = new ArrayList<>();
        List<Long> areas = new ArrayList<>();
        for (int index : order) {
            long area = (long) widths[index] * heights[index];
            int target = -1;
            for (int g = 0; g < groups.size() && target < 0; g++) {
                if (areas.get(g) + area <= capacity) {
                    target = g;
                }
            }
            if (target < 0 && groups.size() < maxCount) {
                groups.add(new ArrayList<>());
                areas.add(0L);
                target = groups.size() - 1;
            }
            if (target < 0) {
                target = areas.indexOf(Collections.min(areas));
            }
            groups.get(target).add(index);
            areas.set(target, areas.get(target) + area);
        }
        groups.forEach(Collections::sort);
        return groups;
    }

    static double getTargetTexelDensity(LevelOfDetail lod) {
        double lod0Density = GlobalConstants.DEFAULT_BATCH_ATLAS_LOD0_TEXEL_DENSITY;
        if (lod.getLevel() <= 0 || lod.getGeometricError() <= 0) {
            return lod0Density;
        }
        double texelsPerMeter = GlobalConstants.DEFAULT_BATCH_ATLAS_TEXELS_PER_SCREEN_PIXEL * GlobalConstants.DEFAULT_SCREEN_SPACE_ERROR / lod.getGeometricError();
        return Math.min(lod0Density, texelsPerMeter * texelsPerMeter);
    }

    /**
     * Linear source scale that brings a texel density down to the target; 1 when it is already below or unknown.
     */
    static double getDensityScale(double texelDensity, double targetDensity) {
        if (!(texelDensity > targetDensity) || targetDensity <= 0 || Double.isInfinite(texelDensity)) {
            return 1.0;
        }
        return Math.sqrt(targetDensity / texelDensity);
    }

    /**
     * Source texels per square meter of surface: UV area in texels of a width x height source over the triangle area.
     * NaN when either area is zero.
     */
    static double getTexelDensity(List<GaiaBufferDataSet> dataSets, int width, int height) {
        double uvArea = 0.0;
        double surfaceArea = 0.0;
        for (GaiaBufferDataSet dataSet : dataSets) {
            GaiaBuffer indexBuffer = dataSet.getBuffers().get(AttributeType.INDICE);
            GaiaBuffer positionBuffer = dataSet.getBuffers().get(AttributeType.POSITION);
            GaiaBuffer texcoordBuffer = dataSet.getBuffers().get(AttributeType.TEXCOORD);
            if (indexBuffer == null || positionBuffer == null || texcoordBuffer == null || indexBuffer.getInts() == null) {
                continue;
            }
            int[] indices = indexBuffer.getInts();
            float[] positions = positionBuffer.getFloats();
            float[] texcoords = texcoordBuffer.getFloats();
            for (int i = 0; i + 2 < indices.length; i += 3) {
                int a = indices[i] < 0 ? indices[i] + 65536 : indices[i];
                int b = indices[i + 1] < 0 ? indices[i + 1] + 65536 : indices[i + 1];
                int c = indices[i + 2] < 0 ? indices[i + 2] + 65536 : indices[i + 2];
                int maxIndex = Math.max(a, Math.max(b, c));
                if (maxIndex * 3 + 2 >= positions.length || maxIndex * 2 + 1 >= texcoords.length) {
                    continue;
                }
                double ux = positions[b * 3] - positions[a * 3], uy = positions[b * 3 + 1] - positions[a * 3 + 1], uz = positions[b * 3 + 2] - positions[a * 3 + 2];
                double vx = positions[c * 3] - positions[a * 3], vy = positions[c * 3 + 1] - positions[a * 3 + 1], vz = positions[c * 3 + 2] - positions[a * 3 + 2];
                double cx = uy * vz - uz * vy, cy = uz * vx - ux * vz, cz = ux * vy - uy * vx;
                surfaceArea += 0.5 * Math.sqrt(cx * cx + cy * cy + cz * cz);
                double su = texcoords[b * 2] - texcoords[a * 2], sv = texcoords[b * 2 + 1] - texcoords[a * 2 + 1];
                double tu = texcoords[c * 2] - texcoords[a * 2], tv = texcoords[c * 2 + 1] - texcoords[a * 2 + 1];
                uvArea += 0.5 * Math.abs(su * tv - tu * sv);
            }
        }
        if (surfaceArea <= 0.0 || uvArea <= 0.0) {
            return Double.NaN;
        }
        return uvArea * width * height / surfaceArea;
    }

    static int getBatchAtlasMaxSize(LevelOfDetail lod) {
        return lod.getLevel() <= 0 ? GlobalConstants.DEFAULT_BATCH_ATLAS_LOD0_MAX_TEXTURE_SIZE : GlobalConstants.DEFAULT_BATCH_ATLAS_MAX_TEXTURE_SIZE;
    }

    /**
     * Maps a texcoord into [0, 1] the way the atlas remap does: values within [-1, 1] are kept, the rest wrapped.
     */
    static double toUnitTexcoord(float value) {
        double unit = Math.abs(value) - 1.0 < 1e-8 ? value : value - Math.floor(value);
        return Math.max(0.0, Math.min(1.0, unit));
    }

    /**
     * Pixel region {x0, y0, x1, y1} of a width x height source that the texcoords reference, grown by padding so
     * bilinear filtering and mipmaps sample real neighboring texels instead of the next atlas entry.
     */
    static int[] getUsedSourceRegion(List<float[]> texcoordArrays, int width, int height, int padding) {
        double minU = Double.MAX_VALUE, minV = Double.MAX_VALUE, maxU = -Double.MAX_VALUE, maxV = -Double.MAX_VALUE;
        for (float[] texcoords : texcoordArrays) {
            for (int i = 0; i + 1 < texcoords.length; i += 2) {
                double u = toUnitTexcoord(texcoords[i]);
                double v = toUnitTexcoord(texcoords[i + 1]);
                minU = Math.min(minU, u);
                maxU = Math.max(maxU, u);
                minV = Math.min(minV, v);
                maxV = Math.max(maxV, v);
            }
        }
        if (minU > maxU) {
            return new int[]{0, 0, width, height};
        }
        int x0 = Math.max(0, Math.min(width - 1, (int) Math.floor(minU * width) - padding));
        int y0 = Math.max(0, Math.min(height - 1, (int) Math.floor(minV * height) - padding));
        int x1 = Math.min(width, Math.max(x0 + 1, (int) Math.ceil(maxU * width) + padding));
        int y1 = Math.min(height, Math.max(y0 + 1, (int) Math.ceil(maxV * height) + padding));
        return new int[]{x0, y0, x1, y1};
    }

    private List<GaiaBufferDataSet> findDataSets(int materialId) {
        return bufferDataSets.stream().filter(bufferDataSet -> bufferDataSet.getMaterialId() == materialId).toList();
    }

    static void tintRegion(BufferedImage image, int x, int y, int width, int height, Vector4d color) {
        if (color == null || (color.x >= 1.0 && color.y >= 1.0 && color.z >= 1.0)) {
            return;
        }
        double r = Math.max(0.0, Math.min(1.0, color.x)), g = Math.max(0.0, Math.min(1.0, color.y)), b = Math.max(0.0, Math.min(1.0, color.z));
        int[] pixels = image.getRGB(x, y, width, height, null, 0, width);
        for (int i = 0; i < pixels.length; i++) {
            int argb = pixels[i];
            int red = (int) Math.round(((argb >> 16) & 0xFF) * r);
            int green = (int) Math.round(((argb >> 8) & 0xFF) * g);
            int blue = (int) Math.round((argb & 0xFF) * b);
            pixels[i] = (argb & 0xFF000000) | (red << 16) | (green << 8) | blue;
        }
        image.setRGB(x, y, width, height, pixels, 0, width);
    }

    static int alignToBlock(int size) {
        return size <= 0 ? size : (size + 3) / 4 * 4;
    }

    static int[] scaleSizes(int[] sizes, double scale) {
        return Arrays.stream(sizes).map(size -> Math.max(1, (int) Math.floor(size * scale))).toArray();
    }

    private void limitAtlasSize(int maximumSize) {
        if (this.atlasImage == null || maximumSize <= 0) {
            return;
        }
        int width = this.atlasImage.getWidth();
        int height = this.atlasImage.getHeight();
        int longestSide = Math.max(width, height);
        if (longestSide <= maximumSize) {
            return;
        }

        double scale = maximumSize / (double) longestSide;
        int resizeWidth = Math.max(1, (int) Math.round(width * scale));
        int resizeHeight = Math.max(1, (int) Math.round(height * scale));
        log.debug("Resize batched atlas: {}x{} -> {}x{}", width, height, resizeWidth, resizeHeight);
        this.atlasImage = ImageResizer.resizeMultiStepSmart(this.atlasImage, resizeWidth, resizeHeight);
    }

    private void updateAtlasTextureMetadata(int fallbackWidth, int fallbackHeight) {
        int width = this.atlasImage != null ? this.atlasImage.getWidth() : fallbackWidth;
        int height = this.atlasImage != null ? this.atlasImage.getHeight() : fallbackHeight;
        for (GaiaMaterial material : materials) {
            Map<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
            if (textures == null || textures.isEmpty()) {
                continue;
            }
            GaiaTexture texture = textures.get(0);
            texture.setBufferedImage(this.atlasImage);
            texture.setWidth(width);
            texture.setHeight(height);
        }
    }

    private void writeAtlasImageForTest(boolean existPngTextures, LevelOfDetail lod, String suffix) {
        String extension = "jpg";
        if (existPngTextures) {
            extension = "png";
        }
        String imageName = ATLAS_IMAGE + "_Lod_" + lod.getLevel() + "_" + (int) (Math.random() * 100000) + suffix;
        writeBatchedImage(imageName, extension);
    }

    private void writeBatchedImage(String imageName, String imageExtension) {
        File tempPath = new File(globalOptions.getTempPath(), "atlas");
        if (!tempPath.exists()) {
            if (!tempPath.mkdirs()) {
                log.error("[ERROR] Failed to create directory");
            }
        }

        Path outputPath = tempPath.toPath();
        Path output = tempPath.toPath().resolve(imageName + "." + imageExtension);
        if (!outputPath.toFile().exists()) {
            if (!outputPath.toFile().mkdir()) {
                log.error("[ERROR] Failed to create directory");
            }
        }
        if (this.atlasImage != null) {
            try {
                File outputImage = output.toFile();
                log.info("[Write Image IO] : {}", outputImage.getAbsolutePath());
                ImageIO.write(this.atlasImage, imageExtension, outputImage);
            } catch (IOException e) {
                log.error("[ERROR] :", e);
            }
        }
    }

    private GaiaMaterial findMaterial(int materialId) {
        return materials.stream().filter(material -> material.getId() == materialId).findFirst().orElseThrow(() -> new RuntimeException("not found material"));
    }

    private int getMaxWidth(List<GaiaBatchImage> compareImages) {
        return compareImages.stream().mapToInt(splitImage -> (int) splitImage.getBatchedBoundary().getMaxX()).max().orElse(0);
    }

    private int getMaxHeight(List<GaiaBatchImage> compareImages) {
        return compareImages.stream().mapToInt(splitImage -> (int) splitImage.getBatchedBoundary().getMaxY()).max().orElse(0);
    }
}


