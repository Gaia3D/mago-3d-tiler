package com.gaia3d.process.postprocess.batch;

import com.gaia3d.basic.exchangable.GaiaBuffer;
import com.gaia3d.basic.exchangable.GaiaBufferDataSet;
import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.structure.GaiaMaterial;
import com.gaia3d.basic.structure.GaiaTexture;
import com.gaia3d.basic.types.AttributeType;
import com.gaia3d.basic.types.TextureType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.ProcessOptions;
import com.gaia3d.process.tileprocess.tile.LevelOfDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.CommandLine;
import org.joml.Vector2d;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.Buffer;
import java.nio.file.Path;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class GaiaTextureCoordinator {
    final private String ATLAS_IMAGE;
    private final List<GaiaMaterial> materials;
    private final List<GaiaBufferDataSet> bufferDataSets;
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

    private Vector2d getBestPositionMosaicInAtlas(List<GaiaBatchImage> listProcessSplitDatas, GaiaBatchImage splitData_toPutInMosaic) {
        Vector2d resultVec = new Vector2d();

        double currPosX, currPosY;
        double candidatePosX = 0.0, candidatePosY = 0.0;
        double currMosaicPerimeter, candidateMosaicPerimeter;
        candidateMosaicPerimeter = -1.0;

        //GaiaRectangle rect_toPutInMosaic = splitData_toPutInMosaic.getOriginBoundary();

        // make existent rectangles list using listProcessSplitDatas.***
        List<GaiaRectangle> list_rectangles = new ArrayList<>();
        GaiaRectangle beforeMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
        int existentSplitDatasCount = listProcessSplitDatas.size();
        for (int i = 0; i < existentSplitDatasCount; i++) {
            GaiaBatchImage existentSplitData = listProcessSplitDatas.get(i);
            GaiaRectangle batchedBoundary = existentSplitData.batchedBoundary;
            if (i == 0) {
                beforeMosaicRectangle.copyFrom(batchedBoundary);
            } else {
                beforeMosaicRectangle.addBoundingRectangle(batchedBoundary);
            }
            list_rectangles.add(batchedBoundary);
        }

        // Now, try to find the best positions to put our rectangle.***
        for (int i = 0; i < existentSplitDatasCount; i++) {
            GaiaBatchImage existentSplitData = listProcessSplitDatas.get(i);
            GaiaRectangle currRect = existentSplitData.batchedBoundary;

            // for each existent rectangles, there are 2 possibles positions: leftUp & rightDown.***
            // in this 2 possibles positions we put our leftDownCorner of rectangle of "splitData_toPutInMosaic".***

            // If in some of two positions our rectangle intersects with any other rectangle, then discard.***
            // If no intersects with others rectangles, then calculate the mosaic-perimeter.
            // We choose the minor perimeter of the mosaic.***

            double width = splitData_toPutInMosaic.getOriginBoundary().getWidth();
            double height = splitData_toPutInMosaic.getOriginBoundary().getHeight();

            // 1- leftUp corner.***
            currPosX = currRect.getMinX();
            currPosY = currRect.getMaxY();

            // setup our rectangle.***
            if (splitData_toPutInMosaic.batchedBoundary == null) {
                splitData_toPutInMosaic.batchedBoundary = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
            }
            splitData_toPutInMosaic.batchedBoundary.setMinX(currPosX);
            splitData_toPutInMosaic.batchedBoundary.setMinY(currPosY);
            splitData_toPutInMosaic.batchedBoundary.setMaxX(currPosX + width);
            splitData_toPutInMosaic.batchedBoundary.setMaxY(currPosY + height);

            // put our rectangle into mosaic & check that no intersects with another rectangles.***
            if (!this.intersectsRectangleAtlasingProcess(list_rectangles, splitData_toPutInMosaic.batchedBoundary)) {
                GaiaRectangle afterMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
                afterMosaicRectangle.copyFrom(beforeMosaicRectangle);
                afterMosaicRectangle.addBoundingRectangle(splitData_toPutInMosaic.batchedBoundary);

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
            splitData_toPutInMosaic.batchedBoundary.setMinX(currPosX);
            splitData_toPutInMosaic.batchedBoundary.setMinY(currPosY);
            splitData_toPutInMosaic.batchedBoundary.setMaxX(currPosX + width);
            splitData_toPutInMosaic.batchedBoundary.setMaxY(currPosY + height);

            // put our rectangle into mosaic & check that no intersects with another rectangles.***
            if (!this.intersectsRectangleAtlasingProcess(list_rectangles, splitData_toPutInMosaic.batchedBoundary)) {
                GaiaRectangle afterMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
                afterMosaicRectangle.copyFrom(beforeMosaicRectangle);
                afterMosaicRectangle.addBoundingRectangle(splitData_toPutInMosaic.batchedBoundary);

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

    public void batchTextures(LevelOfDetail lod) {
        GlobalOptions globalOptions = GlobalOptions.getInstance();
        // We have MaterialList & BufferDataSetList.********
        // 1- List<GaiaMaterial> this.materials;
        // 2- List<GaiaBufferDataSet> this.bufferDataSets;

        // The atlasImage is the final image.********
        // BufferedImage this.atlasImage;
        //--------------------------------------------------------

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
                if(texture.getPath().endsWith(".png") || texture.getPath().endsWith(".PNG"))
                {
                    existPngTextures = true;
                }
                bufferedImage = texture.getBufferedImage(lod.getTextureScale());
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
            log.error("atlasImage is null" );
            return;
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
                graphics.drawImage(source, (int) splittedRectangle.getMinX(), (int) splittedRectangle.getMinY(), null);
            }
        }


        if (globalOptions.isDebug()) {
            // TODO : debug color
            float[] debugColor = lod.getDebugColor();
            Color color = new Color(debugColor[0], debugColor[1], debugColor[2], 0.5f);
            graphics.setColor(color);
            graphics.fillRect(0, 0, maxWidth, maxHeight);
        }

        for (GaiaBatchImage target : splittedImages) {
            GaiaRectangle splittedRectangle = target.getBatchedBoundary();

            int width = (int) splittedRectangle.getMaxX() - (int) splittedRectangle.getMinX();
            int height = (int) splittedRectangle.getMaxY() - (int) splittedRectangle.getMinY();

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

            // original.***********************************************************
//            if (command.hasOption(ProcessOptions.PNG_TEXTURE.getLongName())) {
//                texture.setPath(ATLAS_IMAGE + ".png");
//            } else {
//                texture.setPath(ATLAS_IMAGE + ".jpg");
//            }

            if (existPngTextures) {
                texture.setPath(ATLAS_IMAGE + ".png");
            } else {
                texture.setPath(ATLAS_IMAGE + ".jpg");
            }

            // test save atlasTexture image.****
            String extension = "jpg";
            if (existPngTextures) {
                extension = "png";
            }
            this.writeBatchedImage(extension);
            // end test.************************

            List<GaiaBufferDataSet> materialBufferDataSets = bufferDataSets.stream().filter((bufferDataSet) -> bufferDataSet.getMaterialId() == target.getMaterialId()).collect(Collectors.toList());

            Double intPart_x = null, intPart_y = null;
            double fractPart_x, fractPart_y;
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
                            fractPart_x = originX;
                        } else {
                            fractPart_x = this.modf(originX, intPart_x);
                        }

                        if (Math.abs(originY) - 1.0 < error) {
                            fractPart_y = originY;
                        } else {
                            fractPart_y = this.modf(originY, intPart_y);
                        }

                        u = fractPart_x;
                        v = fractPart_y;

                        if (u < 0.0) {
                            u = 1.0 + u;
                        }

                        u2 = (splittedRectangle.getMinX() + u * width) / maxWidth;
                        v2 = (splittedRectangle.getMinY() + v * height) / maxHeight;

                        texcoords[i] = (float) (u2);
                        texcoords[i + 1] = (float) (v2);
                    }
                }

            }
        }
    }

    private void writeBatchedImage(String imageExtension) {
        File file = new File("D:\\Result_mago3dTiler\\atlasImages\\" );
        if(!file.exists())
        {
            if (!file.mkdirs()) {
                log.error("Failed to create directory");
            }
        }

        Path outputPath = file.toPath();
        Path output = file.toPath().resolve(ATLAS_IMAGE + "." + imageExtension);
        if (!outputPath.toFile().exists()) {
            if (!outputPath.toFile().mkdir()) {
                log.error("Failed to create directory" );
            }
        }
        if (this.atlasImage != null) {
            try {
                ImageIO.write(this.atlasImage, imageExtension, output.toFile());
            } catch (IOException e) {
                log.error(e.getMessage());
            }
        }
    }

    //findMaterial
    private GaiaMaterial findMaterial(int materialId) {
        return materials.stream().filter(material -> material.getId() == materialId).findFirst().orElseThrow(() -> new RuntimeException("not found material" ));
    }

    private int getMaxWidth(List<GaiaBatchImage> compareImages) {
        return compareImages.stream().mapToInt(splittedImage -> (int) splittedImage.getBatchedBoundary().getMaxX()).max().orElse(0);
    }

    private int getMaxHeight(List<GaiaBatchImage> compareImages) {
        return compareImages.stream().mapToInt(splittedImage -> (int) splittedImage.getBatchedBoundary().getMaxY()).max().orElse(0);
    }
}


