package com.gaia3d.process.postprocess.batch;

import com.gaia3d.basic.exchangable.GaiaBuffer;
import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.model.GaiaMaterial;
import com.gaia3d.basic.model.GaiaTexture;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.tileprocess.tile.LevelOfDetail;
import com.gaia3d.util.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GaiaTextureCoordinator {
    private final String ATLAS_IMAGE;
    private final List<GaiaMaterial> materials;
    private final List<GaiaBufferDataSet> bufferDataSets;
    private final GlobalOptions globalOptions = GlobalOptions.getInstance();
    private BufferedImage atlasImage;

    public GaiaTextureCoordinator(String name, List<GaiaMaterial> materials, List<GaiaBufferDataSet> bufferDataSets) {
        this.ATLAS_IMAGE = name;
        this.materials = materials;
        this.bufferDataSets = bufferDataSets;
        this.initBatchImage(0, 0, BufferedImage.TYPE_INT_ARGB);
    }

    private void initBatchImage(int width, int height, int imageType) {
        // imageType :
        // TYPE_INT_RGB = 1
        // TYPE_INT_ARGB = 2
        // TYPE_4BYTE_ABGR = 6
        if (width > 0 || height > 0) {
            this.atlasImage = new BufferedImage(width, height, imageType);

            // now fill the image with white fuchsia.***
            Graphics2D graphics = this.atlasImage.createGraphics();
            graphics.setColor(new Color(255, 0, 255));
            graphics.fillRect(0, 0, width, height);
            graphics.dispose();
        } else {
            this.atlasImage = null;
        }
    }

    private boolean intersectsRectangleAtlasingProcess(List<GaiaRectangle> listRectangles, GaiaRectangle rectangle) {
        // this function returns true if the rectangle intersects with any existent rectangle of the listRectangles.***
        boolean intersects = false;
        double error = 10E-5;
        for (GaiaRectangle existentRectangle : listRectangles) {
            if (existentRectangle == rectangle) {
                continue;
            }
            if (existentRectangle.intersects(rectangle, error)) {
                intersects = true;
                break;
            }
        }
        return intersects;
    }

    private Vector2d getBestPositionMosaicInAtlas(List<GaiaBatchImage> listProcessSplitDataList, GaiaBatchImage splitDataToPutInMosaic) {
        Vector2d resultVec = new Vector2d();

        double currPosX, currPosY;
        double candidatePosX = 0.0, candidatePosY = 0.0;
        double currMosaicPerimeter, candidateMosaicPerimeter;
        candidateMosaicPerimeter = -1.0;

        //GaiaRectangle rect_toPutInMosaic = splitDataToPutInMosaic.getOriginBoundary();

        // make existent rectangles list using listProcessSplitDataList.***
        List<GaiaRectangle> listRectangles = new ArrayList<>();
        GaiaRectangle beforeMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
        int existentSplitDatasCount = listProcessSplitDataList.size();
        for (int i = 0; i < existentSplitDatasCount; i++) {
            GaiaBatchImage existentSplitData = listProcessSplitDataList.get(i);
            GaiaRectangle batchedBoundary = existentSplitData.batchedBoundary;
            if (i == 0) {
                beforeMosaicRectangle.copyFrom(batchedBoundary);
            } else {
                beforeMosaicRectangle.addBoundingRectangle(batchedBoundary);
            }
            listRectangles.add(batchedBoundary);
        }

        // Now, try to find the best positions to put our rectangle.***
        for (int i = 0; i < existentSplitDatasCount; i++) {
            GaiaBatchImage existentSplitData = listProcessSplitDataList.get(i);
            GaiaRectangle currRect = existentSplitData.batchedBoundary;

            // for each existent rectangles, there are 2 possibles positions: leftUp & rightDown.***
            // in this 2 possibles positions we put our leftDownCorner of rectangle of "splitDataToPutInMosaic".***

            // If in some of two positions our rectangle intersects with any other rectangle, then discard.***
            // If no intersects with others rectangles, then calculate the mosaic-perimeter.
            // We choose the minor perimeter of the mosaic.***

            double width = splitDataToPutInMosaic.getOriginBoundary().getWidth();
            double height = splitDataToPutInMosaic.getOriginBoundary().getHeight();

            // 1- leftUp corner.***
            currPosX = currRect.getMinX();
            currPosY = currRect.getMaxY();

            // setup our rectangle.***
            if (splitDataToPutInMosaic.batchedBoundary == null) {
                splitDataToPutInMosaic.batchedBoundary = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
            }
            splitDataToPutInMosaic.batchedBoundary.setMinX(currPosX);
            splitDataToPutInMosaic.batchedBoundary.setMinY(currPosY);
            splitDataToPutInMosaic.batchedBoundary.setMaxX(currPosX + width);
            splitDataToPutInMosaic.batchedBoundary.setMaxY(currPosY + height);

            // put our rectangle into mosaic & check that no intersects with another rectangles.***
            if (!this.intersectsRectangleAtlasingProcess(listRectangles, splitDataToPutInMosaic.batchedBoundary)) {
                GaiaRectangle afterMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
                afterMosaicRectangle.copyFrom(beforeMosaicRectangle);
                afterMosaicRectangle.addBoundingRectangle(splitDataToPutInMosaic.batchedBoundary);

                // calculate the perimeter of the mosaic.***
                if (candidateMosaicPerimeter < 0.0) {
                    candidateMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    candidatePosX = currPosX;
                    candidatePosY = currPosY;
                } else {
                    currMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    if (candidateMosaicPerimeter > currMosaicPerimeter) {
                        candidateMosaicPerimeter = currMosaicPerimeter;
                        candidatePosX = currPosX;
                        candidatePosY = currPosY;
                    }
                }
            }

            // 2- rightDown corner.***
            currPosX = currRect.getMaxX();
            currPosY = currRect.getMinY();

            // setup our rectangle.***
            splitDataToPutInMosaic.batchedBoundary.setMinX(currPosX);
            splitDataToPutInMosaic.batchedBoundary.setMinY(currPosY);
            splitDataToPutInMosaic.batchedBoundary.setMaxX(currPosX + width);
            splitDataToPutInMosaic.batchedBoundary.setMaxY(currPosY + height);

            // put our rectangle into mosaic & check that no intersects with another rectangles.***
            if (!this.intersectsRectangleAtlasingProcess(listRectangles, splitDataToPutInMosaic.batchedBoundary)) {
                GaiaRectangle afterMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
                afterMosaicRectangle.copyFrom(beforeMosaicRectangle);
                afterMosaicRectangle.addBoundingRectangle(splitDataToPutInMosaic.batchedBoundary);

                // calculate the perimeter of the mosaic.***
                if (candidateMosaicPerimeter < 0.0) {
                    candidateMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    candidatePosX = currPosX;
                    candidatePosY = currPosY;
                } else {
                    currMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    if (candidateMosaicPerimeter > currMosaicPerimeter) {
                        candidateMosaicPerimeter = currMosaicPerimeter;
                        candidatePosX = currPosX;
                        candidatePosY = currPosY;
                    }
                }
            }
        }

        resultVec.set(candidatePosX, candidatePosY);

        return resultVec;
    }

    private float modf(float value, Double intPart) {
        intPart = Math.floor(value);
        return (float) (value - intPart);
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
        // We have MaterialList & BufferDataSetList.********
        // 1- List<GaiaMaterial> this.materials;
        // 2- List<GaiaBufferDataSet> this.bufferDataSets;

        // The atlasImage is the final image.********
        // BufferedImage this.atlasImage;
        //--------------------------------------------------------

        boolean isPhotorealistic = globalOptions.isPhotorealistic();

        // 1rst, make a list of GaiaBatchImage (splittedImage).********
        List<GaiaBatchImage> splittedImages = new ArrayList<>();
        boolean existPngTextures = false;
        for (GaiaMaterial material : materials) {
            Map<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
            GaiaTexture texture = null;
            BufferedImage bufferedImage;
            if (!textures.isEmpty()) {
                texture = textures.get(0);
                if (texture.getPath().endsWith(".png") || texture.getPath().endsWith(".PNG")) {
                    existPngTextures = true;
                }

                if (isPhotorealistic) {
                    bufferedImage = texture.getBufferedImage();
                } else {
                    float scaleFactor = lod.getTextureScale();
                    bufferedImage = texture.getBufferedImage(scaleFactor);
                }
            } else {
                bufferedImage = createShamImage();
            }

            Vector2d minPoint = new Vector2d(0, 0);
            Vector2d maxPoint = new Vector2d(bufferedImage.getWidth(), bufferedImage.getHeight());

            GaiaBatchImage splittedImage = new GaiaBatchImage();
            splittedImage.setOriginBoundary(new GaiaRectangle(minPoint, maxPoint));
            splittedImage.setMaterialId(material.getId());
            splittedImages.add(splittedImage);
        }

        /*if (splittedImages.size() < 2) {
            log.error("There are no textures to batch.");
            return null;
        }*/

        // 사이즈 큰->작은 정렬
        splittedImages = splittedImages.stream().sorted(Comparator.comparing(splittedImage -> splittedImage.getOriginBoundary().getArea())).collect(Collectors.toList());
        Collections.reverse(splittedImages);

        // do the atlasing process.***
        List<GaiaBatchImage> listProcessSplitDatas = new ArrayList<>();
        for (int i = 0; i < splittedImages.size(); i++) {
            GaiaBatchImage splittedImage = splittedImages.get(i);
            GaiaRectangle originBoundary = splittedImage.getOriginBoundary();

            if (i == 0) {
                splittedImage.setBatchedBoundary(originBoundary);
            } else {
                // 1rst, find the best position for image into atlas.***
                Vector2d bestPosition = this.getBestPositionMosaicInAtlas(listProcessSplitDatas, splittedImage);
                splittedImage.batchedBoundary.setMinX(bestPosition.x);
                splittedImage.batchedBoundary.setMinY(bestPosition.y);
                splittedImage.batchedBoundary.setMaxX(bestPosition.x + originBoundary.getWidth());
                splittedImage.batchedBoundary.setMaxY(bestPosition.y + originBoundary.getHeight());
            }
            listProcessSplitDatas.add(splittedImage);
        }

        int maxWidth = getMaxWidth(splittedImages);
        int maxHeight = getMaxHeight(splittedImages);
        int imageType = existPngTextures ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        // existPngTextures
        initBatchImage(maxWidth, maxHeight, imageType);
        if (this.atlasImage == null) {
            log.error("atlasImage is null");
            return null;
        }

        Graphics graphics = this.atlasImage.getGraphics();

        for (GaiaBatchImage splittedImage : splittedImages) {
            GaiaRectangle splittedRectangle = splittedImage.getBatchedBoundary();
            GaiaMaterial material = findMaterial(splittedImage.getMaterialId());

            Map<TextureType, List<GaiaTexture>> textureMap = material.getTextures();
            List<GaiaTexture> textures = textureMap.get(TextureType.DIFFUSE);
            if (!textures.isEmpty()) {
                GaiaTexture texture = textures.get(0);
                BufferedImage source = texture.getBufferedImage();

//                // test random color for each splitImage.***************************************************************************************************
//                Color randomColor = new Color((float) Math.random(), (float) Math.random(), (float) Math.random(), 0.8f);
//                BufferedImage randomColoredImage = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_ARGB);
//                Graphics2D randomGraphics = randomColoredImage.createGraphics();
//                randomGraphics.setColor(randomColor);
//                randomGraphics.fillRect(0, 0, source.getWidth(), source.getHeight());
//                randomGraphics.dispose();

                graphics.drawImage(source, (int) splittedRectangle.getMinX(), (int) splittedRectangle.getMinY(), null); // original code.***
                //graphics.drawImage(randomColoredImage, (int) splittedRectangle.getMinX(), (int) splittedRectangle.getMinY(), null); // test code.***
            }
        }

        // Test.****************************************************************************
        /*if (globalOptions.isDebugLod()) {
            float[] debugColor = lod.getDebugColor();
            Color color = new Color(debugColor[0], debugColor[1], debugColor[2], 0.6f);
            graphics.setColor(color);
            graphics.fillRect(0, 0, maxWidth, maxHeight);
        }*/
        // End test.-------------------------------------------------------------------------

        for (GaiaBatchImage target : splittedImages) {
            GaiaRectangle splittedRectangle = target.getBatchedBoundary();

            int width = (int) splittedRectangle.getMaxX() - (int) splittedRectangle.getMinX();
            int height = (int) splittedRectangle.getMaxY() - (int) splittedRectangle.getMinY();
            double pixelXSize = 1.0 / splittedRectangle.getWidth();
            double pixelYSize = 1.0 / splittedRectangle.getHeight();

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

            List<GaiaBufferDataSet> materialBufferDataSets = bufferDataSets.stream().filter((bufferDataSet) -> bufferDataSet.getMaterialId() == target.getMaterialId()).collect(Collectors.toList());

            Double intPartX = null, intPartY = null;
            double fractPartX, fractPartY;
            double error = 1e-8;
            for (GaiaBufferDataSet materialBufferDataSet : materialBufferDataSets) {
                GaiaBuffer texcoordBuffer = materialBufferDataSet.getBuffers().get(AttributeType.TEXCOORD);
                if (texcoordBuffer != null) {
                    float[] texcoords = texcoordBuffer.getFloats();
                    for (int i = 0; i < texcoords.length; i += 2) {
                        float originX = texcoords[i];
                        float originY = texcoords[i + 1];

                        double u, v;
                        double u2, v2;

                        if (Math.abs(originX) - 1.0 < error) {
                            fractPartX = originX;
                        } else {
                            fractPartX = this.modf(originX, intPartX);
                        }

                        if (Math.abs(originY) - 1.0 < error) {
                            fractPartY = originY;
                        } else {
                            fractPartY = this.modf(originY, intPartY);
                        }

                        u = fractPartX;
                        v = fractPartY;

                        // clamp the u, v values.***
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

                        // "width" is the width of the splitRectangle.***
                        u2 = (splittedRectangle.getMinX() + u * width) / maxWidth;
                        v2 = (splittedRectangle.getMinY() + v * height) / maxHeight;

                        texcoords[i] = (float) (u2);
                        texcoords[i + 1] = (float) (v2);
                    }
                }

            }
        }

        /*if (isPhotorealistic) {
            // limit the max image size to 4096
            int lodLevel = lod.getLevel();
            boolean sizeChanged = false;
            int imageWidth = this.atlasImage.getWidth();
            int imageHeight = this.atlasImage.getHeight();

            int lod0size = 1024;
            int lod1size = 1024;
            int lod2size = 1024;
            int overSize = 1024;

            if (lodLevel == 0) {
                if (imageWidth > lod0size) {
                    imageWidth = lod0size;
                    sizeChanged = true;
                }
                if (imageHeight > lod0size) {
                    imageHeight = lod0size;
                    sizeChanged = true;
                }
            } else if (lodLevel == 1) {
                if (imageWidth > lod1size) {
                    imageWidth = lod1size;
                    sizeChanged = true;
                }
                if (imageHeight > lod1size) {
                    imageHeight = lod1size;
                    sizeChanged = true;
                }
            } else if (lodLevel == 2) {
                if (imageWidth > lod2size) {
                    imageWidth = lod2size;
                    sizeChanged = true;
                }
                if (imageHeight > lod2size) {
                    imageHeight = lod2size;
                    sizeChanged = true;
                }
            } else if (lodLevel > 2) {
                if (imageWidth > overSize) {
                    imageWidth = overSize;
                    sizeChanged = true;
                }
                if (imageHeight > overSize) {
                    imageHeight = overSize;
                    sizeChanged = true;
                }
            } else {
                if (imageWidth > overSize) {
                    imageWidth = overSize;
                    sizeChanged = true;
                }
                if (imageHeight > overSize) {
                    imageHeight = overSize;
                    sizeChanged = true;
                }
            }
            if (sizeChanged) {
                ImageResizer imageResizer = new ImageResizer();
                this.atlasImage = imageResizer.resizeImageGraphic2D(this.atlasImage, imageWidth, imageHeight);
            }
        }*/

        Color backGroundColor = new Color(255, 0, 255, 255);
        BufferedImage clamped = ImageUtils.clampBackGroundColor(this.atlasImage, backGroundColor, 1, 30);

        Graphics2D graphics2D = this.atlasImage.createGraphics();
        graphics2D.drawImage(clamped, 0, 0, null);
        graphics2D.dispose();

        // test save atlasTexture image.****
        // Test.----------------------------------------------
        if (globalOptions.isDebugLod()) {
            String extension = "jpg";
            if (existPngTextures) {
                extension = "png";
            }
            double random = Math.random();
            int intRandom = (int) (random * 100000);
            String imageName = ATLAS_IMAGE + "_Lod_" + lod.getLevel() + "_" + intRandom;
            this.writeBatchedImage(imageName, extension);
        }
        // end test.----------------------------------------------

        return this.atlasImage;
    }

    private void writeBatchedImage(String imageName, String imageExtension) {
        String outputPathString = globalOptions.getOutputPath();
        //File file = new File(outputPathString, "temp" + File.separator + "atlas");
        File file = new File(outputPathString, "atlas");
        if (!file.exists()) {
            if (!file.mkdirs()) {
                log.error("Failed to create directory");
            }
        }

        Path outputPath = file.toPath();
        Path output = file.toPath().resolve(imageName + "." + imageExtension);
        if (!outputPath.toFile().exists()) {
            if (!outputPath.toFile().mkdir()) {
                log.error("Failed to create directory");
            }
        }
        if (this.atlasImage != null) {
            try {
                File outputImage = output.toFile();
                log.info("[Write Image IO] : {}", outputImage.getAbsolutePath());
                ImageIO.write(this.atlasImage, imageExtension, outputImage);
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    //findMaterial
    private GaiaMaterial findMaterial(int materialId) {
        return materials.stream().filter(material -> material.getId() == materialId).findFirst().orElseThrow(() -> new RuntimeException("not found material"));
    }

    private int getMaxWidth(List<GaiaBatchImage> compareImages) {
        return compareImages.stream().mapToInt(splittedImage -> (int) splittedImage.getBatchedBoundary().getMaxX()).max().orElse(0);
    }

    private int getMaxHeight(List<GaiaBatchImage> compareImages) {
        return compareImages.stream().mapToInt(splittedImage -> (int) splittedImage.getBatchedBoundary().getMaxY()).max().orElse(0);
    }
}


