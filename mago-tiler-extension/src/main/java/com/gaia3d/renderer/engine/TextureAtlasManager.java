package com.gaia3d.renderer.engine;

import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.halfedge.*;
import com.gaia3d.basic.model.*;
import com.gaia3d.util.GaiaTextureUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;

@Slf4j
public class TextureAtlasManager {

    public void doAtlasTextureProcessByPacker(List<TexturesAtlasData> texAtlasDatasList) {
        // here calculates the batchedBoundaries of each textureScissorData
        int textureScissorDatasCount = texAtlasDatasList.size();
        log.info("[Tile][Photogrammetry][Atlas] doTextureAtlasProcess() : textureScissorDatasCount = " + textureScissorDatasCount);

        TextureAtlasPacker gillotinePacker = new TextureAtlasPacker();

        for (int i = 0; i < textureScissorDatasCount; i++) {
            TexturesAtlasData textureScissorData = texAtlasDatasList.get(i);
            if (!gillotinePacker.insert(textureScissorData)) {
                log.info("[Tile][Photogrammetry][Atlas] doTextureAtlasProcess() : gillotinePacker.insert() failed.");
            }
        }
    }

    public void doAtlasTextureProcess(List<TexturesAtlasData> texAtlasDatasList) {
        // 1rst, sort the texAtlasData by width and height
        List<TexturesAtlasData> texAtlasDataWidther = new ArrayList<>();
        List<TexturesAtlasData> texAtlasDataHigher = new ArrayList<>();
        int texAtlasDataCount = texAtlasDatasList.size();
        for (int i = 0; i < texAtlasDataCount; i++) {
            TexturesAtlasData texAtlasDataAux = texAtlasDatasList.get(i);
            GaiaRectangle originalBoundary = texAtlasDataAux.getOriginalBoundary();

            double w = originalBoundary.getWidth();
            double h = originalBoundary.getHeight();
            if (w > h) {
                texAtlasDataWidther.add(texAtlasDataAux);
            } else {
                texAtlasDataHigher.add(texAtlasDataAux);
            }
        }

        // now, sort each list by width and height
        texAtlasDataWidther.sort((o1, o2) -> {
            GaiaRectangle originalBoundary1 = o1.getOriginalBoundary();
            GaiaRectangle originalBoundary2 = o2.getOriginalBoundary();
            double w1 = originalBoundary1.getWidth();
            double w2 = originalBoundary2.getWidth();
            return Double.compare(w2, w1);
        });

        texAtlasDataHigher.sort((o1, o2) -> {
            GaiaRectangle originalBoundary1 = o1.getOriginalBoundary();
            GaiaRectangle originalBoundary2 = o2.getOriginalBoundary();
            double h1 = originalBoundary1.getHeight();
            double h2 = originalBoundary2.getHeight();
            return Double.compare(h2, h1);
        });

        // make a unique atlasDataList alternating the texAtlasDataWidther and texAtlasDataHigher
        texAtlasDatasList.clear();
        int texAtlasDataWidtherCount = texAtlasDataWidther.size();
        int texAtlasDataHigherCount = texAtlasDataHigher.size();
        int texAtlasDataMaxCount = Math.max(texAtlasDataWidtherCount, texAtlasDataHigherCount);
        for (int i = 0; i < texAtlasDataMaxCount; i++) {
            if (i < texAtlasDataWidtherCount) {
                texAtlasDatasList.add(texAtlasDataWidther.get(i));
            }
            if (i < texAtlasDataHigherCount) {
                texAtlasDatasList.add(texAtlasDataHigher.get(i));
            }
        }

        // now, make the atlas texture
        GaiaRectangle beforeMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
        List<GaiaRectangle> rectangleList = new ArrayList<>();

        TreeMap<Double, List<GaiaRectangle>> maxXrectanglesMap = new TreeMap<>();

        Vector2d bestPosition = new Vector2d();
        List<TexturesAtlasData> currProcessTextureAtlasDates = new ArrayList<>();
        texAtlasDataCount = texAtlasDatasList.size();
        for (int i = 0; i < texAtlasDataCount; i++) {
            TexturesAtlasData texAtlasDataAux = texAtlasDatasList.get(i);
            GaiaRectangle originBoundary = texAtlasDataAux.getOriginalBoundary();

            GaiaRectangle batchedBoundary = null;
            if (i == 0) {
                // the 1rst textureScissorData
                batchedBoundary = new GaiaRectangle(0.0, 0.0, originBoundary.getWidthInt(), originBoundary.getHeightInt());
                texAtlasDataAux.setBatchedBoundary(batchedBoundary);
                beforeMosaicRectangle.copyFrom(batchedBoundary);
            } else {
                // 1rst, find the best position for image into atlas
                bestPosition = this.getBestPositionMosaicInAtlas(currProcessTextureAtlasDates, texAtlasDataAux, bestPosition, beforeMosaicRectangle, rectangleList, maxXrectanglesMap);
                batchedBoundary = new GaiaRectangle(bestPosition.x, bestPosition.y, bestPosition.x + originBoundary.getWidthInt(), bestPosition.y + originBoundary.getHeightInt());
                texAtlasDataAux.setBatchedBoundary(batchedBoundary);
                beforeMosaicRectangle.addBoundingRectangle(batchedBoundary);
            }

            rectangleList.add(batchedBoundary);
            currProcessTextureAtlasDates.add(texAtlasDataAux);

            // map
            double maxX = batchedBoundary.getMaxX();

            List<GaiaRectangle> list_rectanglesMaxX = maxXrectanglesMap.computeIfAbsent(maxX, k -> new ArrayList<>());
            list_rectanglesMaxX.add(batchedBoundary);
        }
    }

    private Vector2d getBestPositionMosaicInAtlas(List<TexturesAtlasData> currProcessTextureAtlasDates, TexturesAtlasData texAtlasDataToPutInMosaic,
                                                  Vector2d resultVec, GaiaRectangle beforeMosaicRectangle, List<GaiaRectangle> list_rectangles, TreeMap<Double, List<GaiaRectangle>> map_maxXrectangles) {
        if (resultVec == null) {
            resultVec = new Vector2d();
        }

        double currPosX, currPosY;
        double candidatePosX = 0.0, candidatePosY = 0.0;
        double currMosaicPerimeter, candidateMosaicPerimeter;
        candidateMosaicPerimeter = -1.0;
        double error = 1.0 - 1e-6;

        // Now, try to find the best positions to put our rectangle
        int existentTexAtlasDataCount = currProcessTextureAtlasDates.size();
        for (int i = 0; i < existentTexAtlasDataCount; i++) {
            TexturesAtlasData existentTexAtlasData = currProcessTextureAtlasDates.get(i);
            GaiaRectangle currRect = existentTexAtlasData.getBatchedBoundary();

            // for each existent rectangles, there are 2 possibles positions: leftUp & rightDown
            // in this 2 possibles positions we put our leftDownCorner of rectangle of "splitData_toPutInMosaic"

            // If in some of two positions our rectangle intersects with any other rectangle, then discard
            // If no intersects with others rectangles, then calculate the mosaic-perimeter.
            // We choose the minor perimeter of the mosaic

            double width = texAtlasDataToPutInMosaic.getOriginalBoundary().getWidthInt();
            double height = texAtlasDataToPutInMosaic.getOriginalBoundary().getHeightInt();

            // 1- leftUp corner
            currPosX = currRect.getMinX();
            currPosY = currRect.getMaxY();

            // setup our rectangle
            if (texAtlasDataToPutInMosaic.getBatchedBoundary() == null) {
                texAtlasDataToPutInMosaic.setBatchedBoundary(new GaiaRectangle(0.0, 0.0, 0.0, 0.0));
            }
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMinX(currPosX);
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMinY(currPosY);
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMaxX(currPosX + width);
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMaxY(currPosY + height);

            // put our rectangle into mosaic & check that no intersects with another rectangles
            if (!this.intersectsRectangleAtlasingProcess(list_rectangles, texAtlasDataToPutInMosaic.getBatchedBoundary(), map_maxXrectangles)) {
                GaiaRectangle afterMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
                afterMosaicRectangle.copyFrom(beforeMosaicRectangle);
                afterMosaicRectangle.addBoundingRectangle(texAtlasDataToPutInMosaic.getBatchedBoundary());

                // calculate the perimeter of the mosaic
                if (candidateMosaicPerimeter < 0.0) {
                    candidateMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    candidatePosX = currPosX;
                    candidatePosY = currPosY;
                } else {
                    currMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    if (candidateMosaicPerimeter >= currMosaicPerimeter * error) {
                        candidateMosaicPerimeter = currMosaicPerimeter;
                        candidatePosX = currPosX;
                        candidatePosY = currPosY;
                        break; // test delete**************************
                    }
                }
            }

            // 2- rightDown corner
            currPosX = currRect.getMaxX();
            currPosY = currRect.getMinY();

            // setup our rectangle
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMinX(currPosX);
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMinY(currPosY);
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMaxX(currPosX + width);
            texAtlasDataToPutInMosaic.getBatchedBoundary().setMaxY(currPosY + height);

            // put our rectangle into mosaic & check that no intersects with another rectangles
            if (!this.intersectsRectangleAtlasingProcess(list_rectangles, texAtlasDataToPutInMosaic.getBatchedBoundary(), map_maxXrectangles)) {
                GaiaRectangle afterMosaicRectangle = new GaiaRectangle(0.0, 0.0, 0.0, 0.0);
                afterMosaicRectangle.copyFrom(beforeMosaicRectangle);
                afterMosaicRectangle.addBoundingRectangle(texAtlasDataToPutInMosaic.getBatchedBoundary());

                // calculate the perimeter of the mosaic
                if (candidateMosaicPerimeter < 0.0) {
                    candidateMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    candidatePosX = currPosX;
                    candidatePosY = currPosY;
                } else {
                    currMosaicPerimeter = afterMosaicRectangle.getPerimeter();
                    if (candidateMosaicPerimeter >= currMosaicPerimeter * error) {
                        candidateMosaicPerimeter = currMosaicPerimeter;
                        candidatePosX = currPosX;
                        candidatePosY = currPosY;
                        break; // test delete**************************
                    }
                }
            }
        }

        resultVec.set(candidatePosX, candidatePosY);

        return resultVec;
    }

    private boolean intersectsRectangleAtlasingProcess(List<GaiaRectangle> listRectangles, GaiaRectangle rectangle, TreeMap<Double, List<GaiaRectangle>> map_maxXrectangles) {
        // this function returns true if the rectangle intersects with any existent rectangle of the listRectangles
        boolean intersects = false;
        double error = 10E-5;

        double currRectMinX = rectangle.getMinX();

        // check with map_maxXrectangles all rectangles that have maxX > currRectMinX
        for (Map.Entry<Double, List<GaiaRectangle>> entry : map_maxXrectangles.tailMap(currRectMinX).entrySet()) {
            List<GaiaRectangle> existentRectangles = entry.getValue();

            int existentRectanglesCount = existentRectangles.size();
            for (int i = 0; i < existentRectanglesCount; i++) {
                GaiaRectangle existentRectangle = existentRectangles.get(i);
                if (existentRectangle == rectangle) {
                    continue;
                }
                if (existentRectangle.intersects(rectangle, error)) {
                    return true;
                }
            }
        }


//        for (GaiaRectangle existentRectangle : listRectangles) {
//            if (existentRectangle == rectangle) {
//                continue;
//            }
//            if (existentRectangle.intersects(rectangle, error)) {
//                intersects = true;
//                break;
//            }
//        }
        return intersects;
    }

    public int getMaxWidth(List<TexturesAtlasData> compareImages) {
        int result = compareImages.stream().mapToInt(textureScissorData -> (int) textureScissorData.getBatchedBoundary().getMaxX()).max().orElse(0);
        return result;
    }

    public int getMaxHeight(List<TexturesAtlasData> compareImages) {
        int result = compareImages.stream().mapToInt(textureScissorData -> (int) textureScissorData.getBatchedBoundary().getMaxY()).max().orElse(0);
        return result;
    }

    private void getGaiaVerticesOfFaceGroup(List<GaiaFace> faceGroup, List<GaiaVertex> vertices, List<GaiaVertex> resultVertices) {
        Map<GaiaVertex, GaiaVertex> groupVertexMap = new HashMap<>();
        int facesCount = faceGroup.size();
        for (int j = 0; j < facesCount; j++) {
            GaiaFace face = faceGroup.get(j);
            int[] indices = face.getIndices();
            for (int k = 0; k < indices.length; k++) {
                int index = indices[k];
                GaiaVertex vertex = vertices.get(index);
                groupVertexMap.put(vertex, vertex);
            }
        }

        List<GaiaVertex> vertexList = new ArrayList<>(groupVertexMap.values());
        resultVertices.addAll(vertexList);
    }

    public void recalculateTexCoordsAfterTextureAtlasing(GaiaScene gaiaScene, List<TexturesAtlasData> texAtlasDatasList) {
        //*****************************************************************
        // Note : scene must join all surfaces before call this function
        //*****************************************************************
        int maxWidth = getMaxWidth(texAtlasDatasList);
        int maxHeight = getMaxHeight(texAtlasDatasList);

        if (maxWidth == 0 || maxHeight == 0) {
            return;
        }

        GaiaNode rootNode = gaiaScene.getNodes().get(0); // there are only one root node
        GaiaNode node = rootNode.getChildren().get(0); // there are only one child node
        GaiaMesh mesh = node.getMeshes().get(0); // there are only one mesh
        GaiaPrimitive primitive = mesh.getPrimitives().get(0); // there are only one primitive

        Map<GaiaVertex, GaiaVertex> groupVertexMapMemSave = new HashMap<>();
        //Map<GaiaVertex, GaiaVertex> visitedVertexMapMemSave = new HashMap<>();
        //visitedVertexMapMemSave.clear();

        List<GaiaVertex> faceVerticesMemSave = new ArrayList<>();
        List<GaiaFace> faces = new ArrayList<>();
        gaiaScene.extractGaiaFaces(faces);

        Map<Integer, List<GaiaFace>> mapClassificationFacesList = new HashMap<>();
        for (GaiaFace face : faces) {
            int classificationId = face.getClassifyId();
            List<GaiaFace> faceList = mapClassificationFacesList.computeIfAbsent(classificationId, k -> new ArrayList<>());
            faceList.add(face);
        }

        int texAtlasDatasCount = texAtlasDatasList.size();
        for (int i = 0; i < texAtlasDatasCount; i++) {
            TexturesAtlasData texAtlasData = texAtlasDatasList.get(i);
            int classifyId = texAtlasData.getClassifyId();
//            CameraDirectionType cameraDirectionType = texAtlasData.getCameraDirectionType();
//            List<HalfEdgeFace> faceGroup = mapClassificationCamDirTypeFacesList.get(classifyId).get(cameraDirectionType);

            List<GaiaFace> faceGroup = mapClassificationFacesList.get(classifyId);
            if (faceGroup == null) {
                continue;
            }

            GaiaRectangle originalBoundary = texAtlasData.getOriginalBoundary();
            GaiaRectangle batchedBoundary = texAtlasData.getBatchedBoundary();

            double texWidth = texAtlasData.getTextureImage().getWidth();
            double texHeight = texAtlasData.getTextureImage().getHeight();
            double xPixelSize = 1.0 / texWidth;
            double yPixelSize = 1.0 / texHeight;

            // obtain all vertex of the faceGroup
            //groupVertexMapMemSave.clear();
            faceVerticesMemSave.clear();
            this.getGaiaVerticesOfFaceGroup(faceGroup, primitive.getVertices(), faceVerticesMemSave);

            // now, calculate the vertex list from the map
            //List<GaiaVertex> vertexList = faceVerticesMemSave;
            int verticesCount = faceVerticesMemSave.size();
            double texCoordErrore = 0.0025;
            for (int k = 0; k < verticesCount; k++) {
                GaiaVertex vertex = faceVerticesMemSave.get(k);

                // calculate the real texCoords
                Vector2d texCoord = vertex.getTexcoords();
                double x = texCoord.x;
                double y = texCoord.y;

                double pixelX = x * texWidth;
                double pixelY = y * texHeight;

                // transform the texCoords to texCoordRelToCurrentBoundary
                double xRel = (pixelX - originalBoundary.getMinX()) / originalBoundary.getWidth();
                double yRel = (pixelY - originalBoundary.getMinY()) / originalBoundary.getHeight();

                // clamp the texRelCoords
                xRel = Math.max(0.0 + xPixelSize, Math.min(1.0 - xPixelSize, xRel));
                yRel = Math.max(0.0 + yPixelSize, Math.min(1.0 - yPixelSize, yRel));

                // transform the texCoordRelToCurrentBoundary to atlasBoundary using batchedBoundary
                double xAtlas = (batchedBoundary.getMinX() + xRel * batchedBoundary.getWidth()) / maxWidth;
                double yAtlas = (batchedBoundary.getMinY() + yRel * batchedBoundary.getHeight()) / maxHeight;

                if (xAtlas < 0.0 || xAtlas > 1.0 || yAtlas < 0.0 || yAtlas > 1.0) {
                    log.info("recalculateTexCoordsAfterTextureAtlasingObliqueCamera() : xAtlas or yAtlas is out of range.");
                }

                // clamp the texAtlasCoords
                Vector2d texCoordFinal = new Vector2d(xAtlas, yAtlas);
                GaiaTextureUtils.clampTextureCoordinate(texCoordFinal, texCoordErrore);
                vertex.setTexcoords(texCoordFinal);
            }
        }
    }

    public void recalculateTexCoordsAfterTextureAtlasingObliqueCamera(HalfEdgeScene halfEdgeScene, List<TexturesAtlasData> texAtlasDatasList,
                                                                      Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapClassificationCamDirTypeFacesList) {
        int maxWidth = getMaxWidth(texAtlasDatasList);
        int maxHeight = getMaxHeight(texAtlasDatasList);

        if (maxWidth == 0 || maxHeight == 0) {
            return;
        }

        Map<HalfEdgeVertex, HalfEdgeVertex> groupVertexMapMemSave = new HashMap<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> visitedVertexMapMemSave = new HashMap<>();
        visitedVertexMapMemSave.clear();

        List<HalfEdgeVertex> faceVerticesMemSave = new ArrayList<>();

        int texAtlasDatasCount = texAtlasDatasList.size();
        for (int i = 0; i < texAtlasDatasCount; i++) {
            TexturesAtlasData texAtlasData = texAtlasDatasList.get(i);
            int classifyId = texAtlasData.getClassifyId();
            //PlaneType planeType = texAtlasData.getPlaneType(); // old old old old old old
            CameraDirectionType cameraDirectionType = texAtlasData.getCameraDirectionType();
            List<HalfEdgeFace> faceGroup = mapClassificationCamDirTypeFacesList.get(classifyId).get(cameraDirectionType);

            if (faceGroup == null) {
                continue;
            }

            GaiaRectangle originalBoundary = texAtlasData.getOriginalBoundary();
            GaiaRectangle batchedBoundary = texAtlasData.getBatchedBoundary();

            double texWidth = texAtlasData.getTextureImage().getWidth();
            double texHeight = texAtlasData.getTextureImage().getHeight();
            double xPixelSize = 1.0 / texWidth;
            double yPixelSize = 1.0 / texHeight;

            // obtain all vertex of the faceGroup
            groupVertexMapMemSave.clear();
            int facesCount = faceGroup.size();
            for (int j = 0; j < facesCount; j++) {
                HalfEdgeFace face = faceGroup.get(j);
                faceVerticesMemSave.clear();
                faceVerticesMemSave = face.getVertices(faceVerticesMemSave);
                int verticesCount = faceVerticesMemSave.size();
                for (int k = 0; k < verticesCount; k++) {
                    HalfEdgeVertex vertex = faceVerticesMemSave.get(k);
                    groupVertexMapMemSave.put(vertex, vertex);
                }
            }

            // now, calculate the vertex list from the map
            List<HalfEdgeVertex> vertexList = new ArrayList<>(groupVertexMapMemSave.values());
            int verticesCount = vertexList.size();
            double texCoordErrore = 0.0025;
            for (int k = 0; k < verticesCount; k++) {
                HalfEdgeVertex vertex = vertexList.get(k);

                // calculate the real texCoords
                Vector2d texCoord = vertex.getTexcoords();
                double x = texCoord.x;
                double y = texCoord.y;

                double pixelX = x * texWidth;
                double pixelY = y * texHeight;

                // transform the texCoords to texCoordRelToCurrentBoundary
                double xRel = (pixelX - originalBoundary.getMinX()) / originalBoundary.getWidth();
                double yRel = (pixelY - originalBoundary.getMinY()) / originalBoundary.getHeight();

                // clamp the texRelCoords
                xRel = Math.max(0.0 + xPixelSize, Math.min(1.0 - xPixelSize, xRel));
                yRel = Math.max(0.0 + yPixelSize, Math.min(1.0 - yPixelSize, yRel));

                // transform the texCoordRelToCurrentBoundary to atlasBoundary using batchedBoundary
                double xAtlas = (batchedBoundary.getMinX() + xRel * batchedBoundary.getWidth()) / maxWidth;
                double yAtlas = (batchedBoundary.getMinY() + yRel * batchedBoundary.getHeight()) / maxHeight;

                if (xAtlas < 0.0 || xAtlas > 1.0 || yAtlas < 0.0 || yAtlas > 1.0) {
                    log.info("recalculateTexCoordsAfterTextureAtlasingObliqueCamera() : xAtlas or yAtlas is out of range.");
                }

                // clamp the texAtlasCoords
                Vector2d texCoordFinal = new Vector2d(xAtlas, yAtlas);
                GaiaTextureUtils.clampTextureCoordinate(texCoordFinal, texCoordErrore);
                vertex.setTexcoords(texCoordFinal);
            }
        }
    }

    public GaiaTexture makeAtlasTexture(List<TexturesAtlasData> texAtlasDatasList, int imageType) {
        // calculate the maxWidth and maxHeight
        // TODO : is it wrong to calculate the maxWidth and maxHeight by using the batchedBoundary?***
        TextureAtlasManager textureAtlasManager = new TextureAtlasManager();
        int maxWidth = textureAtlasManager.getMaxWidth(texAtlasDatasList);
        int maxHeight = textureAtlasManager.getMaxHeight(texAtlasDatasList);

        if (maxWidth == 0 || maxHeight == 0) {
            log.error("[ERROR] makeAtlasTexture() : maxWidth or maxHeight is 0.");
            return null;
        }

        GaiaTexture textureAtlas = new GaiaTexture();
        log.info("[Tile][Photogrammetry][makeAtlasTexture] Atlas maxWidth : " + maxWidth + " , maxHeight : " + maxHeight);
        textureAtlas.createImage(maxWidth, maxHeight, imageType);

        // draw the images into textureAtlas
        log.debug("HalfEdgeSurface.scissorTextures() : draw the images into textureAtlas.");
        Graphics2D g2d = textureAtlas.getBufferedImage().createGraphics();
        int textureAtlasDatasCount = texAtlasDatasList.size();
        for (int i = 0; i < textureAtlasDatasCount; i++) {
            TexturesAtlasData textureAtlasData = texAtlasDatasList.get(i);
            GaiaRectangle currentBoundary = textureAtlasData.getCurrentBoundary();
            GaiaRectangle batchedBoundary = textureAtlasData.getBatchedBoundary();
            GaiaRectangle originBoundary = textureAtlasData.getOriginalBoundary();

            BufferedImage subImage = textureAtlasData.getTextureImage();

//            Color randomColor = new Color((float) Math.random(), (float) Math.random(), (float) Math.random(), 0.8f);
//            BufferedImage randomColoredImage = new BufferedImage(subImage.getWidth(), subImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
//            Graphics2D randomGraphics = randomColoredImage.createGraphics();
//            randomGraphics.setColor(randomColor);
//            randomGraphics.fillRect(0, 0, subImage.getWidth(), subImage.getHeight());
//            randomGraphics.dispose();
//            g2d.drawImage(randomColoredImage, (int) batchedBoundary.getMinX(), (int) batchedBoundary.getMinY(), null); // test code
//            // end test.--------------------------------------------------------------------------------------------------------------------------------

            g2d.drawImage(subImage, (int) batchedBoundary.getMinX(), (int) batchedBoundary.getMinY(), null); // original code

        }
        g2d.dispose();

        return textureAtlas;
    }

    public void copyAtlasTextureProcess(List<TexturesAtlasData> texAtlasDatasListSource, List<TexturesAtlasData> texAtlasDatasListDest) {
        int texAtlasDatasCount = texAtlasDatasListSource.size();
        for (int i = 0; i < texAtlasDatasCount; i++) {
            TexturesAtlasData texAtlasDataSource = texAtlasDatasListSource.get(i);
            int sourceClassifyId = texAtlasDataSource.getClassifyId();

            // find the dest texAtlasData with the same classifyId
            boolean found = false;
            int destIndex = -1;
            int texAtlasDatasDestCount = texAtlasDatasListDest.size();
            for (int j = 0; j < texAtlasDatasDestCount; j++) {
                TexturesAtlasData texAtlasDataDest = texAtlasDatasListDest.get(j);
                int destClassifyId = texAtlasDataDest.getClassifyId();
                if (sourceClassifyId == destClassifyId) {
                    found = true;
                    destIndex = j;
                    break;
                }
            }
            if (!found) {
                log.error("[ERROR] copyAtlasTextureProcess() : cannot find the dest texAtlasData with classifyId = " + sourceClassifyId);
                continue;
            }
            TexturesAtlasData texAtlasDataDest = texAtlasDatasListDest.get(destIndex);

            GaiaRectangle batchedBoundary = texAtlasDataSource.getBatchedBoundary();
            GaiaRectangle batchedBoundaryCopy = new GaiaRectangle(batchedBoundary);
            texAtlasDataDest.setBatchedBoundary(batchedBoundaryCopy);

            GaiaRectangle currentBoundary = texAtlasDataSource.getCurrentBoundary();
            if (currentBoundary != null) {
                GaiaRectangle currentBoundaryCopy = new GaiaRectangle(currentBoundary);
                texAtlasDataDest.setCurrentBoundary(currentBoundaryCopy);
            }
        }
    }
}
