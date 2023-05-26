package geometry.exchangable;

import geometry.basic.GaiaRectangle;
import geometry.structure.GaiaMaterial;
import geometry.structure.GaiaTexture;
import geometry.types.TextureType;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;
import util.ImageUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Getter
@Setter
public class GaiaTextureCoordinator {
    List<GaiaMaterial> materials = new ArrayList<>();
    List<GaiaBufferDataSet> bufferDataSets = new ArrayList<>();
    List<GaiaRectangle> boundingRectangles = new ArrayList<>();

    BufferedImage batchImage;
    GaiaRectangle batchBoundingRectangle;
    int batchWidth;
    int batchHeight;

    public GaiaTextureCoordinator(List<GaiaMaterial> materials, List<GaiaBufferDataSet> bufferDataSets) {
        this.materials = materials;
        this.bufferDataSets = bufferDataSets;
        this.initBatchImage(0, 0);
    }

    private void initBatchImage(int width, int height) {
        if (width > 0 || height > 0) {
            this.batchImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        } else {
            this.batchImage = null;
        }
        this.batchWidth = width;
        this.batchHeight = height;
        this.batchBoundingRectangle = new GaiaRectangle();
        this.batchBoundingRectangle.setInit(new Vector2d(0, 0));
        this.batchBoundingRectangle.addPoint(new Vector2d(width, height));
    }

    public void batchTextures() {
        double error = 10E-5;
        //error = 10E-5;

        log.info((0.0001 == 10E-5) + " : ?");


        int imageType = 0;
        List<GaiaSplittedImage> splittedImages = new ArrayList<>();
        for (GaiaMaterial material : materials) {
            LinkedHashMap<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
            GaiaTexture texture = textures.get(0);
            if (texture.getBufferedImage() == null) {
                texture.loadImage();
            }
            BufferedImage bufferedImage = texture.getBufferedImage();
            imageType = bufferedImage.getType();

            bufferedImage = ImageUtils.resizeImageGraphic2D(bufferedImage, 256, 256);
            texture.setBufferedImage(bufferedImage);

            Vector2d minPoint = new Vector2d(0, 0);
            //Vector2d maxPoint = new Vector2d(bufferedImage.getWidth(), bufferedImage.getHeight());
            Vector2d maxPoint = new Vector2d(256, 256);
            GaiaSplittedImage splittedImage = new GaiaSplittedImage();
            splittedImage.setOriginalRectangle(new GaiaRectangle(minPoint, maxPoint));
            splittedImage.setMaterialId(material.getId());
            splittedImages.add(splittedImage);
        }

        // 사이즈 큰->작은 정렬
        splittedImages = splittedImages.stream()
                .sorted(Comparator.comparing(splittedImage -> splittedImage.getOriginalRectangle().getArea()))
                .collect(Collectors.toList());
        Collections.reverse(splittedImages);

        // 분할이미지
        for (GaiaSplittedImage target : splittedImages) {
            GaiaRectangle targetRectangle = target.getOriginalRectangle();
            List<GaiaSplittedImage> compareImages = getListWithoutSelf(target, splittedImages);

            if (compareImages.isEmpty()) {
                target.setSplittedRectangle(targetRectangle);
            } else {
                List<GaiaSplittedImage> leftBottomFiltered = compareImages.stream().filter((compareSplittedImage) -> {
                            GaiaRectangle compare = compareSplittedImage.getSplittedRectangle();
                            GaiaRectangle leftBottom = getLeftBottom(targetRectangle, compare);
                            List<GaiaSplittedImage> filteredCompareImages = getListWithoutSelf(compareSplittedImage, compareImages);
                            for (GaiaSplittedImage filteredCompareSplittedImage : filteredCompareImages) {
                                GaiaRectangle compareRectangle = filteredCompareSplittedImage.getSplittedRectangle();
                                if (compareRectangle.intersects(leftBottom, error)) {
                                    return false;
                                }
                            }
                            return true;
                        }).sorted(Comparator.comparing(compareSplittedImage -> {
                            return getLeftBottom(targetRectangle, compareSplittedImage.getSplittedRectangle()).getBoundingArea();
                        })).collect(Collectors.toList());


                List<GaiaSplittedImage> rightTopFiltered = compareImages.stream()
                        .filter((compareSplittedImage) -> {
                            GaiaRectangle compare = compareSplittedImage.getSplittedRectangle();
                            GaiaRectangle rightTop = getRightTop(targetRectangle, compare);
                            List<GaiaSplittedImage> filteredCompareImages = getListWithoutSelf(compareSplittedImage, compareImages);
                            for (GaiaSplittedImage filteredCompareSplittedImage : filteredCompareImages) {
                                GaiaRectangle compareRectangle = filteredCompareSplittedImage.getSplittedRectangle();
                                if (compareRectangle.intersects(rightTop, error)) {
                                    return false;
                                }
                            }
                            return true;
                        }).sorted(Comparator.comparing(compareSplittedImage -> {
                            return getRightTop(targetRectangle, compareSplittedImage.getSplittedRectangle()).getBoundingArea();
                        })).collect(Collectors.toList());

                if (!leftBottomFiltered.isEmpty() && !rightTopFiltered.isEmpty()) {
                    GaiaSplittedImage leftBottomImage = leftBottomFiltered.get(0);
                    GaiaRectangle leftBottomCompare = leftBottomImage.getSplittedRectangle();
                    GaiaRectangle leftBottom = getLeftBottom(targetRectangle, leftBottomCompare);


                    GaiaSplittedImage rightTopImage = rightTopFiltered.get(0);
                    GaiaRectangle rightTopCompare = rightTopImage.getSplittedRectangle();
                    GaiaRectangle rightTop = getRightTop(targetRectangle, rightTopCompare);

                    if (leftBottom.getBoundingArea() >= rightTop.getBoundingArea()) {
                        target.setSplittedRectangle(rightTop);
                        log.info("rightTop : " + (int) rightTop.getBoundingArea() + ":" + (int) Math.sqrt( (int)rightTop.getBoundingArea()));
                    } else {
                        target.setSplittedRectangle(leftBottom);
                        log.info("leftBottom : " + (int) leftBottom.getBoundingArea() + ":" + (int) Math.sqrt( (int)leftBottom.getBoundingArea()));
                    }
                } else if (!leftBottomFiltered.isEmpty()) {
                    GaiaSplittedImage notCompareImage = leftBottomFiltered.get(0);
                    GaiaRectangle compare = notCompareImage.getSplittedRectangle();
                    GaiaRectangle leftBottom = getLeftBottom(targetRectangle, compare);
                    target.setSplittedRectangle(leftBottom);
                    log.info("leftBottom : " + (int) leftBottom.getBoundingArea() + ":" + (int) Math.sqrt( (int)leftBottom.getBoundingArea()));
                } else if (!rightTopFiltered.isEmpty()) {
                    GaiaSplittedImage notCompareImage = rightTopFiltered.get(0);
                    GaiaRectangle compare = notCompareImage.getSplittedRectangle();
                    GaiaRectangle rightTop = getRightTop(targetRectangle, compare);
                    target.setSplittedRectangle(rightTop);
                    log.info("rightTop : " + (int) rightTop.getBoundingArea() + ":" + (int) Math.sqrt( (int)rightTop.getBoundingArea()));
                } else {
                    log.info("filtered isNotPresent");
                }
            }
        }

        int maxWidth = splittedImages.stream()
                .mapToInt(splittedImage -> (int) splittedImage.getSplittedRectangle().getMaxX()).max().getAsInt();
        int maxHeight = splittedImages.stream()
                .mapToInt(splittedImage -> (int) splittedImage.getSplittedRectangle().getMaxY()).max().getAsInt();

        BufferedImage image = new BufferedImage(maxWidth, maxHeight, imageType);
        Graphics graphics = image.getGraphics();

        for (GaiaSplittedImage splittedImage : splittedImages) {
            GaiaRectangle splittedRectangle = splittedImage.getSplittedRectangle();
            GaiaMaterial material = findMaterial(splittedImage.getMaterialId());

            LinkedHashMap<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
            GaiaTexture texture = textures.get(0);

            BufferedImage source = texture.getBufferedImage();
            graphics.drawImage(source, (int) splittedRectangle.getMinX(), (int) splittedRectangle.getMinY(), (int) splittedRectangle.getMaxX(), (int) splittedRectangle.getMaxY(),null);

            //log.info((int)splittedRectangle.getMinX() + "," + (int)splittedRectangle.getMinY() + " " + (int)splittedRectangle.getMaxX() + "," + (int)splittedRectangle.getMaxY());

            //graphics.drawImage(source, (int) splittedRectangle.getMinX(), (int) splittedRectangle.getMinY(), null);
            //graphics.dispose();
        }

        log.info("test");
    }

    //findMaterial
    private GaiaMaterial findMaterial(int materialId) {
        return materials.stream()
                .filter(material -> material.getId() == materialId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("not found material"));
    }

    private List<GaiaSplittedImage> getListWithoutSelf(GaiaSplittedImage targetSplittedImage, List<GaiaSplittedImage> splittedImages) {
        return splittedImages.stream()
                .filter(splittedImage -> (splittedImage != targetSplittedImage) && (splittedImage.getSplittedRectangle() != null))
                .collect(Collectors.toList());
    }

    private List<GaiaSplittedImage> getLongSplittedImages(List<GaiaSplittedImage> splittedImages) {
        return splittedImages.stream().filter(splittedImage -> {
            double width = splittedImage.getOriginalRectangle().getRange().x;
            double height = splittedImage.getOriginalRectangle().getRange().y;
            return height > width;
        }).collect(Collectors.toList());
    }
    private List<GaiaSplittedImage> getWideSplittedImages(List<GaiaSplittedImage> splittedImages) {
        return splittedImages.stream().filter(splittedImage -> {
            double width = splittedImage.getOriginalRectangle().getRange().x;
            double height = splittedImage.getOriginalRectangle().getRange().y;
            return height <= width;
        }).collect(Collectors.toList());
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

    public void findIntersect(GaiaRectangle target, GaiaRectangle compare) {
        double error = 0.0001;
        if (compare != null) {
            Vector2d rightTopPoint = target.getRightTopPoint();
            Vector2d rightTopMaxPoint = new Vector2d(rightTopPoint.x + target.getMaxX(), rightTopPoint.y + target.getMaxY());
            GaiaRectangle rightTopRectangle = new GaiaRectangle();
            rightTopRectangle.setInit(rightTopPoint);
            rightTopRectangle.addPoint(rightTopMaxPoint);

            Vector2d leftBottomPoint = compare.getLeftBottomPoint();
            Vector2d leftBottomMaxPoint = new Vector2d(leftBottomPoint.x + target.getMaxX(), leftBottomPoint.y + target.getMaxY());
            GaiaRectangle leftBottomRectangle = new GaiaRectangle();
            leftBottomRectangle.setInit(leftBottomPoint);
            leftBottomRectangle.addPoint(leftBottomMaxPoint);

            if (rightTopRectangle.intersects(leftBottomRectangle, error)) {
                log.info("test");
            }
        }
    }

    public void batchTextures2() {
        for (GaiaMaterial material : materials) {
            LinkedHashMap<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
            GaiaTexture texture = textures.get(0);
            if (texture.getBufferedImage() == null) {
                texture.loadImage();
            }

            BufferedImage bufferedImage = texture.getBufferedImage();
            bufferedImage = ImageUtils.resizeImageGraphic2D(bufferedImage, 256, 256);

            addImage(bufferedImage);

            log.info("material: {}", material.getName());
            //TODO
        }


    }

    public void clipImage(BufferedImage bufferedImage) {
        Graphics2D graphics = (Graphics2D) bufferedImage.getGraphics();
        //graphics.
    }

    public void addImage(BufferedImage bufferedImage) {
        GaiaRectangle boundingRectangle = new GaiaRectangle();
        BufferedImage tempImage = this.copyImage(this.batchImage);
        Graphics2D graphics = null;

        if (this.batchWidth <= 0 || this.batchHeight <= 0) {
            initBatchImage(bufferedImage.getWidth(), bufferedImage.getHeight());
            boundingRectangle.setInit(new Vector2d(0, 0));
            boundingRectangle.addPoint(new Vector2d(bufferedImage.getWidth(), bufferedImage.getHeight()));
            graphics = (Graphics2D) this.batchImage.getGraphics();
            graphics.drawImage(bufferedImage, 0, 0, null);
            graphics.dispose();
        } else {
            int preWidth = this.batchWidth;
            int preHeight = this.batchHeight;

            int calcWidth = this.batchWidth;
            int calcHeight = this.batchHeight;
            boolean heightFlag = (calcHeight >= calcWidth);
            boolean widthFlag = (calcHeight < calcWidth);
            if (heightFlag) {
                calcWidth += bufferedImage.getWidth();
            } else if (widthFlag) {
                calcHeight += bufferedImage.getHeight();
            } else {
                calcWidth += bufferedImage.getWidth();
                calcHeight += bufferedImage.getHeight();
            }

            initBatchImage(calcWidth, calcHeight);
            graphics = (Graphics2D) this.batchImage.getGraphics();
            graphics.setBackground(Color.WHITE);
            graphics.drawImage(tempImage, 0, 0, null);

            boundingRectangle.setInit(new Vector2d(preWidth, preHeight));
            boundingRectangle.addPoint(new Vector2d(preWidth + bufferedImage.getWidth(), preHeight + bufferedImage.getHeight()));

            if (heightFlag) {
                graphics.drawImage(bufferedImage, preWidth, 0, null);
            } else if (widthFlag) {
                graphics.drawImage(bufferedImage, 0, preHeight, null);
            } else {
                graphics.drawImage(bufferedImage, preWidth, preHeight, null);
            }
            graphics.dispose();
        }
        boundingRectangles.add(boundingRectangle);
    }

    //private void drawRight;

    private BufferedImage copyImage(BufferedImage source){
        if (source == null) {
            return null;
        }
        BufferedImage image = new BufferedImage(source.getWidth(), source.getHeight(), source.getType());
        Graphics graphics = image.getGraphics();
        graphics.drawImage(source, 0, 0, null);
        graphics.dispose();
        return image;
    }
}


