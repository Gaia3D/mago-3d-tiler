package geometry.batch;

import geometry.basic.GaiaRectangle;
import geometry.exchangable.GaiaBuffer;
import geometry.exchangable.GaiaBufferDataSet;
import geometry.structure.GaiaMaterial;
import geometry.structure.GaiaTexture;
import geometry.types.AttributeType;
import geometry.types.TextureType;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import util.ImageUtils;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GaiaTextureCoordinator {
    final private String ATLAS_IMAGE;
    final private double ERROR = 10E-5; //error = 10E-5;

    private final List<GaiaMaterial> materials;
    private final List<GaiaBufferDataSet> bufferDataSets;
    private BufferedImage atlasImage;

    public GaiaTextureCoordinator(String name, List<GaiaMaterial> materials, List<GaiaBufferDataSet> bufferDataSets) {
        this.ATLAS_IMAGE = name;
        this.materials = materials;
        this.bufferDataSets = bufferDataSets;
        this.initBatchImage(0, 0);
    }
    private void initBatchImage(int width, int height) {
        if (width > 0 || height > 0) {
            this.atlasImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        } else {
            this.atlasImage = null;
        }
    }
    public void writeBatchedImage(Path outputPath) {
        Path output = outputPath.resolve(ATLAS_IMAGE + ".jpg");
        if (!outputPath.toFile().exists()) {
            outputPath.toFile().mkdir();
        }

        if (this.atlasImage != null) {
            try {
                ImageIO.write(this.atlasImage, "jpg", output.toFile());
                ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("jpg").next();
                ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
                jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                jpgWriteParam.setCompressionQuality(0.0f);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }
    public void batchTextures(int lod) {
        List<GaiaBatchImage> splittedImages = new ArrayList<>();
        for (GaiaMaterial material : materials) {
            LinkedHashMap<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
            GaiaTexture texture = textures.get(0);
            if (texture.getBufferedImage() == null) {
                texture.loadImage();
            }
            BufferedImage bufferedImage = texture.getBufferedImage();

            float scale = 1.0f;
            if (lod == 1) {
                scale = 0.5f;
            } else if (lod == 2) {
                scale = 0.25f;
            } else if (lod == 3) {
                scale = 0.125f;
            }

            int resizeWidth = (int) (bufferedImage.getWidth() * scale);
            int resizeHeight = (int) (bufferedImage.getHeight() * scale);

            resizeWidth = ImageUtils.getNearestPowerOfTwo(resizeWidth);
            resizeHeight = ImageUtils.getNearestPowerOfTwo(resizeHeight);

            bufferedImage = ImageUtils.resizeImageGraphic2D(bufferedImage, resizeWidth, resizeHeight);
            texture.setBufferedImage(bufferedImage);

            Vector2d minPoint = new Vector2d(0, 0);
            Vector2d maxPoint = new Vector2d(bufferedImage.getWidth(), bufferedImage.getHeight());

            GaiaBatchImage splittedImage = new GaiaBatchImage();
            splittedImage.setOriginBoundary(new GaiaRectangle(minPoint, maxPoint));
            splittedImage.setMaterialId(material.getId());
            splittedImages.add(splittedImage);
        }

        // 사이즈 큰->작은 정렬
        splittedImages = splittedImages.stream()
                .sorted(Comparator.comparing(splittedImage -> splittedImage.getOriginBoundary().getArea()))
                .collect(Collectors.toList());
        Collections.reverse(splittedImages);

        // 분할이미지
        for (GaiaBatchImage target : splittedImages) {
            GaiaRectangle targetRectangle = target.getOriginBoundary();
            List<GaiaBatchImage> compareImages = getListWithoutSelf(target, splittedImages);

            if (compareImages.isEmpty()) {
                target.setBatchedBoundary(targetRectangle);
            } else {
                List<GaiaBatchImage> leftBottomFiltered = compareImages.stream().filter((compareSplittedImage) -> {
                            GaiaRectangle compare = compareSplittedImage.getBatchedBoundary();
                            GaiaRectangle leftBottom = getLeftBottom(targetRectangle, compare);
                            List<GaiaBatchImage> filteredCompareImages = getListWithoutSelf(compareSplittedImage, compareImages);
                            for (GaiaBatchImage filteredCompareSplittedImage : filteredCompareImages) {
                                GaiaRectangle compareRectangle = filteredCompareSplittedImage.getBatchedBoundary();
                                if (compareRectangle.intersects(leftBottom, ERROR)) {
                                    return false;
                                }
                            }
                            return true;
                        }).sorted(Comparator.comparing(compareSplittedImage -> {
                            GaiaRectangle compare = compareSplittedImage.getBatchedBoundary();
                            GaiaRectangle leftBottom = getLeftBottom(targetRectangle, compare);

                            int maxWidth = getMaxWidth(compareImages);
                            int maxHeight = getMaxHeight(compareImages);
                            maxWidth = maxWidth < leftBottom.getMaxX() ? (int) leftBottom.getMaxX() : maxWidth;
                            maxHeight = maxHeight < leftBottom.getMaxY() ? (int) leftBottom.getMaxY() : maxHeight;
                            return maxHeight * maxWidth;
                        })).collect(Collectors.toList());

                List<GaiaBatchImage> rightTopFiltered = compareImages.stream()
                        .filter((compareSplittedImage) -> {
                            GaiaRectangle compare = compareSplittedImage.getBatchedBoundary();
                            GaiaRectangle rightTop = getRightTop(targetRectangle, compare);
                            List<GaiaBatchImage> filteredCompareImages = getListWithoutSelf(compareSplittedImage, compareImages);
                            for (GaiaBatchImage filteredCompareSplittedImage : filteredCompareImages) {
                                GaiaRectangle compareRectangle = filteredCompareSplittedImage.getBatchedBoundary();
                                if (compareRectangle.intersects(rightTop, ERROR)) {
                                    return false;
                                }
                            }
                            return true;
                        }).sorted(Comparator.comparing(compareSplittedImage -> {
                            GaiaRectangle compare = compareSplittedImage.getBatchedBoundary();
                            GaiaRectangle rightTop = getRightTop(targetRectangle, compare);
                            int maxWidth = getMaxWidth(compareImages);
                            int maxHeight = getMaxHeight(compareImages);
                            maxWidth = maxWidth < rightTop.getMaxX() ? (int) rightTop.getMaxX() : maxWidth;
                            maxHeight = maxHeight < rightTop.getMaxY() ? (int) rightTop.getMaxY() : maxHeight;
                            return maxHeight * maxWidth;
                        })).collect(Collectors.toList());

                if (!leftBottomFiltered.isEmpty() && !rightTopFiltered.isEmpty()) {
                    GaiaBatchImage leftBottomImage = leftBottomFiltered.get(0);
                    GaiaRectangle leftBottomCompare = leftBottomImage.getBatchedBoundary();
                    GaiaRectangle leftBottom = getLeftBottom(targetRectangle, leftBottomCompare);

                    GaiaBatchImage rightTopImage = rightTopFiltered.get(0);
                    GaiaRectangle rightTopCompare = rightTopImage.getBatchedBoundary();
                    GaiaRectangle rightTop = getRightTop(targetRectangle, rightTopCompare);

                    if (leftBottom.getBoundingArea() >= rightTop.getBoundingArea()) {
                        target.setBatchedBoundary(rightTop);
                    } else {
                        target.setBatchedBoundary(leftBottom);
                    }
                } else if (!leftBottomFiltered.isEmpty()) {
                    GaiaBatchImage notCompareImage = leftBottomFiltered.get(0);
                    GaiaRectangle compare = notCompareImage.getBatchedBoundary();
                    GaiaRectangle leftBottom = getLeftBottom(targetRectangle, compare);
                    target.setBatchedBoundary(leftBottom);
                } else if (!rightTopFiltered.isEmpty()) {
                    GaiaBatchImage notCompareImage = rightTopFiltered.get(0);
                    GaiaRectangle compare = notCompareImage.getBatchedBoundary();
                    GaiaRectangle rightTop = getRightTop(targetRectangle, compare);
                    target.setBatchedBoundary(rightTop);
                }
            }
        }

        int maxWidth = getMaxWidth(splittedImages);
        int maxHeight = getMaxHeight(splittedImages);
        initBatchImage(maxWidth, maxHeight);

        if (this.atlasImage == null) {
            log.error("atlasImage is null");
            return;
        }

        Graphics graphics = this.atlasImage.getGraphics();
        for (GaiaBatchImage splittedImage : splittedImages) {
            GaiaRectangle splittedRectangle = splittedImage.getBatchedBoundary();
            GaiaMaterial material = findMaterial(splittedImage.getMaterialId());

            LinkedHashMap<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
            GaiaTexture texture = textures.get(0);

            BufferedImage source = texture.getBufferedImage();
            graphics.drawImage(source, (int) splittedRectangle.getMinX(), (int) splittedRectangle.getMinY(),null);
        }

        for (GaiaBatchImage target : splittedImages) {
            GaiaRectangle splittedRectangle = target.getBatchedBoundary();

            int width = (int) splittedRectangle.getMaxX() - (int) splittedRectangle.getMinX();
            int height = (int) splittedRectangle.getMaxY() - (int) splittedRectangle.getMinY();

            GaiaMaterial material = findMaterial(target.getMaterialId());
            LinkedHashMap<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
            GaiaTexture texture = textures.get(0);
            texture.setBufferedImage(this.atlasImage);
            texture.setWidth(maxWidth);
            texture.setHeight(maxHeight);
            texture.setPath(ATLAS_IMAGE + ".jpg");

            List<GaiaBufferDataSet> materialBufferDataSets = bufferDataSets.stream()
                    .filter((bufferDataSet) -> bufferDataSet.getMaterialId() == target.getMaterialId())
                    .collect(Collectors.toList());

            for (GaiaBufferDataSet materialBufferDataSet : materialBufferDataSets) {
                GaiaBuffer texcoordBuffer = materialBufferDataSet.getBuffers().get(AttributeType.TEXCOORD);
                if (texcoordBuffer != null) {
                    float[] texcoords = texcoordBuffer.getFloats();
                    for (int i = 0; i < texcoords.length; i+=2) {
                        float originX = texcoords[i];
                        float originY = texcoords[i + 1];
                        float convertX = originX * width;
                        float convertY = originY * height;
                        float offsetX = (float) (splittedRectangle.getMinX() + convertX);
                        float offsetY = (float) (splittedRectangle.getMinY() + convertY);
                        float resultX = offsetX / maxWidth;
                        float resultY = offsetY / maxHeight;
                        texcoords[i] = resultX;
                        texcoords[i + 1] = resultY;
                    }
                }

            }

        }
    }

    //findMaterial
    private GaiaMaterial findMaterial(int materialId) {
        return materials.stream()
                .filter(material -> material.getId() == materialId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("not found material"));
    }

    private List<GaiaBatchImage> getListWithoutSelf(GaiaBatchImage targetSplittedImage, List<GaiaBatchImage> splittedImages) {
        return splittedImages.stream()
                .filter(splittedImage -> (splittedImage != targetSplittedImage) && (splittedImage.getBatchedBoundary() != null))
                .collect(Collectors.toList());
    }

    private GaiaRectangle getRightTop(GaiaRectangle target, GaiaRectangle compare) {
        Vector2d rightTopPoint = compare.getRightTopPoint();
        Vector2d rightTopMaxPoint = new Vector2d(rightTopPoint.x + target.getMaxX(), rightTopPoint.y + target.getMaxY());
        GaiaRectangle rightTopRectangle = new GaiaRectangle();
        rightTopRectangle.setInit(rightTopPoint);
        rightTopRectangle.addPoint(rightTopMaxPoint);
        return rightTopRectangle;
    }
    private GaiaRectangle getLeftBottom(GaiaRectangle target, GaiaRectangle compare) {
        Vector2d leftBottomPoint = compare.getLeftBottomPoint();
        Vector2d leftBottomMaxPoint = new Vector2d(leftBottomPoint.x + target.getMaxX(), leftBottomPoint.y + target.getMaxY());
        GaiaRectangle leftBottomRectangle = new GaiaRectangle();
        leftBottomRectangle.setInit(leftBottomPoint);
        leftBottomRectangle.addPoint(leftBottomMaxPoint);
        return leftBottomRectangle;
    }

    private int getMaxWidth(List<GaiaBatchImage> compareImages) {
        return compareImages.stream()
                .mapToInt(splittedImage -> (int) splittedImage.getBatchedBoundary().getMaxX())
                .max()
                .orElse(0);
    }

    private int getMaxHeight(List<GaiaBatchImage> compareImages) {
        return compareImages.stream()
                .mapToInt(splittedImage -> (int) splittedImage.getBatchedBoundary().getMaxY())
                .max()
                .orElse(0);
    }
}


