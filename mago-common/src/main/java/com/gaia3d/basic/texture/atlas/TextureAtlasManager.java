package com.gaia3d.basic.texture.atlas;

import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.geometry.modifier.topology.GaiaExtractor;
import com.gaia3d.basic.halfedge.*;
import com.gaia3d.basic.model.*;
import com.gaia3d.util.GaiaTextureUtils;
import lombok.extern.slf4j.Slf4j;
import org.joml.Vector2d;

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class TextureAtlasManager {
    public static int calculateExpandedPixels(double pixelWidth, double pixelHeight) {
        double maxSize = Math.max(pixelWidth, pixelHeight);
        double minSize = Math.min(pixelWidth, pixelHeight);

        int expanded = (int) Math.ceil(maxSize * 0.02); // 3.5%

        expanded = Math.max(expanded, 6);
        expanded = Math.min(expanded, 12);

        return expanded;
    }

    public static void dilateBackgroundColor(
            BufferedImage image,
            Color backgroundColor
    ) {
        if (image == null || backgroundColor == null) {
            return;
        }

        log.debug("*** start dilate - Background - Color ***");

        final int width = image.getWidth();
        final int height = image.getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        final int pixelCount = width * height;
        final int rgbMask = 0x00FFFFFF;
        final int backgroundRGB = backgroundColor.getRGB() & rgbMask;

        /*
         * Cuando la imagen tiene almacenamiento int[] compatible,
         * trabajamos directamente sobre sus píxeles.
         *
         * Así evitamos:
         *   image.getRGB(...)
         *   image.setRGB(...)
         *   una copia completa de la imagen
         */
        int[] pixels = null;
        boolean directPixelAccess = false;

        int imageType = image.getType();
        WritableRaster raster = image.getRaster();

        if ((imageType == BufferedImage.TYPE_INT_ARGB
                || imageType == BufferedImage.TYPE_INT_RGB)
                && raster.getDataBuffer() instanceof DataBufferInt dataBuffer
                && raster.getSampleModel()
                instanceof SinglePixelPackedSampleModel sampleModel
                && sampleModel.getScanlineStride() == width
                && raster.getSampleModelTranslateX() == 0
                && raster.getSampleModelTranslateY() == 0
                && dataBuffer.getOffset() == 0
                && dataBuffer.getData().length >= pixelCount) {

            pixels = dataBuffer.getData();
            directPixelAccess = true;
        }

        /*
         * Fallback seguro para otros tipos de BufferedImage,
         * por ejemplo TYPE_3BYTE_BGR.
         */
        if (!directPixelAccess) {
            pixels = image.getRGB(
                    0,
                    0,
                    width,
                    height,
                    null,
                    0,
                    width
            );
        }

        /*
         * byte[] evita el coste de estructuras como HashSet.
         *
         * 0 = todavía no añadido
         * 1 = ya añadido
         */
        byte[] queued = new byte[pixelCount];

        /*
         * Cola primitiva.
         *
         * Cada píxel puede añadirse como máximo una vez,
         * por lo que pixelCount es capacidad suficiente.
         */
        int[] queue = new int[pixelCount];

        int head = 0;
        int tail = 0;

        /*
         * Primera fase:
         * localizar píxeles de fondo que tocan algún píxel coloreado.
         */
        for (int y = 0; y < height; y++) {
            int rowStart = y * width;

            for (int x = 0; x < width; x++) {
                int index = rowStart + x;

                if ((pixels[index] & rgbMask) != backgroundRGB) {
                    continue;
                }

                boolean touchesColoredPixel = false;

                // Derecha.
                if (x + 1 < width
                        && (pixels[index + 1] & rgbMask) != backgroundRGB) {
                    touchesColoredPixel = true;
                }
                // Izquierda.
                else if (x > 0
                        && (pixels[index - 1] & rgbMask) != backgroundRGB) {
                    touchesColoredPixel = true;
                }
                // Abajo.
                else if (index + width < pixelCount
                        && (pixels[index + width] & rgbMask) != backgroundRGB) {
                    touchesColoredPixel = true;
                }
                // Arriba.
                else if (index >= width
                        && (pixels[index - width] & rgbMask) != backgroundRGB) {
                    touchesColoredPixel = true;
                }

                if (touchesColoredPixel) {
                    queued[index] = 1;
                    queue[tail++] = index;
                }
            }
        }

        /*
         * Segunda fase:
         * propagación BFS del color.
         */
        while (head < tail) {
            int index = queue[head++];

            /*
             * Solo necesitamos x para controlar los límites
             * izquierdo y derecho. No hace falta calcular y.
             */
            int x = index % width;

            int replacement;

            /*
             * Conservamos el mismo orden que el código original:
             * derecha, izquierda, abajo, arriba.
             */
            if (x + 1 < width
                    && (pixels[index + 1] & rgbMask) != backgroundRGB) {

                replacement = pixels[index + 1];

            } else if (x > 0
                    && (pixels[index - 1] & rgbMask) != backgroundRGB) {

                replacement = pixels[index - 1];

            } else if (index + width < pixelCount
                    && (pixels[index + width] & rgbMask) != backgroundRGB) {

                replacement = pixels[index + width];

            } else if (index >= width
                    && (pixels[index - width] & rgbMask) != backgroundRGB) {

                replacement = pixels[index - width];

            } else {
                /*
                 * En condiciones normales no debería ocurrir,
                 * porque el píxel se añade solamente cuando toca
                 * un píxel ya coloreado.
                 */
                continue;
            }

            pixels[index] = replacement;

            /*
             * Añadir los píxeles de fondo vecinos.
             */

            // Derecha.
            if (x + 1 < width) {
                int neighborIndex = index + 1;

                if (queued[neighborIndex] == 0
                        && (pixels[neighborIndex] & rgbMask) == backgroundRGB) {

                    queued[neighborIndex] = 1;
                    queue[tail++] = neighborIndex;
                }
            }

            // Izquierda.
            if (x > 0) {
                int neighborIndex = index - 1;

                if (queued[neighborIndex] == 0
                        && (pixels[neighborIndex] & rgbMask) == backgroundRGB) {

                    queued[neighborIndex] = 1;
                    queue[tail++] = neighborIndex;
                }
            }

            // Abajo.
            if (index + width < pixelCount) {
                int neighborIndex = index + width;

                if (queued[neighborIndex] == 0
                        && (pixels[neighborIndex] & rgbMask) == backgroundRGB) {

                    queued[neighborIndex] = 1;
                    queue[tail++] = neighborIndex;
                }
            }

            // Arriba.
            if (index >= width) {
                int neighborIndex = index - width;

                if (queued[neighborIndex] == 0
                        && (pixels[neighborIndex] & rgbMask) == backgroundRGB) {

                    queued[neighborIndex] = 1;
                    queue[tail++] = neighborIndex;
                }
            }
        }

        /*
         * Solo copiamos los píxeles de vuelta cuando no pudimos
         * acceder directamente al DataBufferInt.
         */
        if (!directPixelAccess) {
            image.setRGB(
                    0,
                    0,
                    width,
                    height,
                    pixels,
                    0,
                    width
            );
        }

        log.debug(
                "--- end dilate - Background - Color. Dilated pixels: {} ---",
                tail
        );
    }

    public static void dilateBackgroundColor_original(
            BufferedImage image,
            Color backgroundColor
    ) {
        if (image == null || backgroundColor == null) {
            return;
        }

        log.debug("*** start dilate - Background - Color ***");

        final int width = image.getWidth();
        final int height = image.getHeight();
        final int bgRGB = backgroundColor.getRGB() & 0x00FFFFFF;

        int[] pixels = image.getRGB(0, 0, width, height, null, 0, width);
        boolean[] queued = new boolean[pixels.length];

        ArrayDeque<Integer> queue = new ArrayDeque<>();

        int[] dx4 = {1, -1, 0, 0};
        int[] dy4 = {0, 0, 1, -1};

        // 1. Inicializar cola con píxeles magenta vecinos a píxeles no-magenta
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int idx = y * width + x;
                int rgb = pixels[idx] & 0x00FFFFFF;

                if (rgb != bgRGB) {
                    continue;
                }

                for (int n = 0; n < 4; n++) {
                    int nx = x + dx4[n];
                    int ny = y + dy4[n];

                    if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                        continue;
                    }

                    int nIdx = ny * width + nx;
                    int nRgb = pixels[nIdx] & 0x00FFFFFF;

                    if (nRgb != bgRGB) {
                        queued[idx] = true;
                        queue.add(idx);
                        break;
                    }
                }
            }
        }

        // 2. Propagar color
        while (!queue.isEmpty()) {
            int idx = queue.poll();

            if ((pixels[idx] & 0x00FFFFFF) != bgRGB) {
                continue;
            }

            int x = idx % width;
            int y = idx / width;

            int replacement = 0;
            boolean found = false;

            // buscar vecino ya coloreado
            for (int n = 0; n < 4 && !found; n++) {
                int nx = x + dx4[n];
                int ny = y + dy4[n];

                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    continue;
                }

                int nIdx = ny * width + nx;
                int nRgb = pixels[nIdx] & 0x00FFFFFF;

                if (nRgb != bgRGB) {
                    replacement = pixels[nIdx];
                    found = true;
                }
            }

            if (!found) {
                continue;
            }

            pixels[idx] = replacement;

            // añadir vecinos magenta
            for (int n = 0; n < 4; n++) {
                int nx = x + dx4[n];
                int ny = y + dy4[n];

                if (nx < 0 || nx >= width || ny < 0 || ny >= height) {
                    continue;
                }

                int nIdx = ny * width + nx;

                if (!queued[nIdx] && (pixels[nIdx] & 0x00FFFFFF) == bgRGB) {
                    queued[nIdx] = true;
                    queue.add(nIdx);
                }
            }
        }

        image.setRGB(0, 0, width, height, pixels, 0, width);
        log.debug("--- end dilate - Background - Color ---");
    }

    private static void safeCopyPixel(
            Raster srcRaster,
            WritableRaster dstRaster,
            int srcX,
            int srcY,
            int dstX,
            int dstY,
            int[] pixelBuffer
    ) {
        int srcMinX = srcRaster.getMinX();
        int srcMinY = srcRaster.getMinY();
        int srcMaxX = srcMinX + srcRaster.getWidth() - 1;
        int srcMaxY = srcMinY + srcRaster.getHeight() - 1;

        int dstMinX = dstRaster.getMinX();
        int dstMinY = dstRaster.getMinY();
        int dstMaxX = dstMinX + dstRaster.getWidth() - 1;
        int dstMaxY = dstMinY + dstRaster.getHeight() - 1;

        if (dstX < dstMinX || dstX > dstMaxX || dstY < dstMinY || dstY > dstMaxY) {
            return;
        }

        int sx = clampInt(srcX, srcMinX, srcMaxX);
        int sy = clampInt(srcY, srcMinY, srcMaxY);

        srcRaster.getPixel(sx, sy, pixelBuffer);
        dstRaster.setPixel(dstX, dstY, pixelBuffer);
    }

    private static int clampInt(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    public void doAtlasTextureProcessByScissorDates(List<GaiaTextureScissorData> textureScissorDates) {
        // here calculates the batchedBoundaries of each textureScissorData
        int textureScissorDatasCount = textureScissorDates.size();
        log.debug("[Tile][Photogrammetry][Atlas] doTextureAtlasProcess() : textureScissorDatasCount = " + textureScissorDatasCount);

        GuillotinePacker guillotinePacker = new GuillotinePacker();

        for (int i = 0; i < textureScissorDatasCount; i++) {
            GaiaTextureScissorData textureScissorData = textureScissorDates.get(i);
            if (!guillotinePacker.insert(textureScissorData)) {
                log.debug("[Tile][Photogrammetry][Atlas] doTextureAtlasProcess() : guillotinePacker.insert() failed.");
            }
        }
    }

    public void doAtlasTextureProcessByScissorDatesFull(
            List<GaiaTextureScissorDataFull> textureScissorDatesFull
    ) {
        int textureScissorDatasCount = textureScissorDatesFull.size();

        log.debug("[Tile][Photogrammetry][Atlas] doTextureAtlasProcess() : textureScissorDatasCount = "
                + textureScissorDatasCount);

        List<GaiaTextureScissorDataFull> sortedScissors =
                sortScissorDataAlternatingWidthHeight(textureScissorDatesFull);

        GuillotinePacker guillotinePacker = new GuillotinePacker();

        for (int i = 0; i < sortedScissors.size(); i++) {
            GaiaTextureScissorDataFull textureScissorData = sortedScissors.get(i);

            if (!guillotinePacker.insert(textureScissorData)) {
                log.debug("[Tile][Photogrammetry][Atlas] doTextureAtlasProcess() : guillotinePacker.insert() failed.");
            }
        }
    }

    private List<GaiaTextureScissorDataFull> sortScissorDataAlternatingWidthHeight(
            List<GaiaTextureScissorDataFull> input
    ) {
        if (input == null || input.isEmpty()) {
            return new ArrayList<>();
        }

        List<GaiaTextureScissorDataFull> byWidth = new ArrayList<>(input);
        List<GaiaTextureScissorDataFull> byHeight = new ArrayList<>(input);

        byWidth.sort((a, b) -> Integer.compare(
                getImageWidth(b),
                getImageWidth(a)
        ));

        byHeight.sort((a, b) -> Integer.compare(
                getImageHeight(b),
                getImageHeight(a)
        ));

        List<GaiaTextureScissorDataFull> result = new ArrayList<>(input.size());
        Set<GaiaTextureScissorDataFull> used = Collections.newSetFromMap(new IdentityHashMap<>());

        int widthIdx = 0;
        int heightIdx = 0;

        while (result.size() < input.size()) {
            // 1) Add next widest
            while (widthIdx < byWidth.size() && used.contains(byWidth.get(widthIdx))) {
                widthIdx++;
            }

            if (widthIdx < byWidth.size()) {
                GaiaTextureScissorDataFull data = byWidth.get(widthIdx++);
                result.add(data);
                used.add(data);
            }

            if (result.size() >= input.size()) {
                break;
            }

            // 2) Add next tallest
            while (heightIdx < byHeight.size() && used.contains(byHeight.get(heightIdx))) {
                heightIdx++;
            }

            if (heightIdx < byHeight.size()) {
                GaiaTextureScissorDataFull data = byHeight.get(heightIdx++);
                result.add(data);
                used.add(data);
            }
        }

        return result;
    }

    private int getImageWidth(GaiaTextureScissorDataFull data) {
        if (data == null || data.getScissoredImage() == null) {
            return 0;
        }

        return data.getScissoredImage().getWidth();
    }

    private int getImageHeight(GaiaTextureScissorDataFull data) {
        if (data == null || data.getScissoredImage() == null) {
            return 0;
        }

        return data.getScissoredImage().getHeight();
    }

    private int getMaxWidthScissorDates(List<GaiaTextureScissorData> compareImages) {
        return compareImages.stream().mapToInt(textureScissorData -> (int) textureScissorData.getBatchedBoundary().getMaxX()).max().orElse(0);
    }

    private int getMaxHeightScissorDates(List<GaiaTextureScissorData> compareImages) {
        return compareImages.stream().mapToInt(textureScissorData -> (int) textureScissorData.getBatchedBoundary().getMaxY()).max().orElse(0);
    }

    public List<GaiaTextureScissorData> calculateTextureScissorDates(List<List<HalfEdgeFace>> mergedWeldedFacesGroups,
                                                                     int texWidth,
                                                                     int texHeight,
                                                                     boolean existPngTextures,
                                                                     BufferedImage srcImage,
                                                                     GaiaTexture resultTextureAtlas,
                                                                     boolean paintUsedPixels) {
        // now, for each faceGroup, create a scissorData
        // there are 2 types of scissorData :
        // 1- more width than height.
        // 2- more height than width.
        List<GaiaTextureScissorData> textureScissorDatasWidth = new ArrayList<>();
        List<GaiaTextureScissorData> textureScissorDatasHeight = new ArrayList<>();
        int weldedFacesGroupsCount = mergedWeldedFacesGroups.size();

        List<HalfEdgeVertex> faceVertices = new ArrayList<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> groupVertexMap = new HashMap<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> visitedVertexMap = new HashMap<>();

        log.debug("ScissorTextures : weldedFacesCount" + weldedFacesGroupsCount + " " + textureScissorDatasWidth.size());
        boolean invertTexCoordY = false;
        List<HalfEdgeVertex> memSaveVertices = new ArrayList<>();
        for (int i = 0; i < weldedFacesGroupsCount; i++) {
            List<HalfEdgeFace> weldedFacesGroup = mergedWeldedFacesGroups.get(i);
            GaiaRectangle groupTexCoordBRect = new GaiaRectangle();
            int weldedFacesCount = weldedFacesGroup.size();
            for (int j = 0; j < weldedFacesCount; j++) {
                GaiaRectangle texCoordBRect = new GaiaRectangle();
                HalfEdgeFace face = weldedFacesGroup.get(j);
                memSaveVertices.clear();
                texCoordBRect = face.getTexCoordBoundingRectangle(texCoordBRect, invertTexCoordY, memSaveVertices);

                if (j == 0) {
                    groupTexCoordBRect.copyFrom(texCoordBRect);
                } else {
                    groupTexCoordBRect.addBoundingRectangle(texCoordBRect);
                }
            }

            // check if must translate to positive quadrant
            if (groupTexCoordBRect.getMinX() < 0.0 || groupTexCoordBRect.getMinX() > 1.0 || groupTexCoordBRect.getMinY() < 0.0 || groupTexCoordBRect.getMinY() > 1.0) {
                double texCoordOriginX = groupTexCoordBRect.getMinX();
                double texCoordOriginY = groupTexCoordBRect.getMinY();
                double offsetX = 0.0;
                double offsetY = 0.0;
                if (texCoordOriginX < 0.0 || texCoordOriginX > 1.0) {
                    offsetX = Math.floor(texCoordOriginX);
                }

                if (texCoordOriginY < 0.0 || texCoordOriginY > 1.0) {
                    offsetY = Math.floor(texCoordOriginY);
                }

                if (offsetX != 0.0 || offsetY != 0.0) {
                    // must translate to positive quadrant
                    int facesCount = weldedFacesGroup.size();
                    for (int j = 0; j < facesCount; j++) {
                        HalfEdgeFace face = weldedFacesGroup.get(j);
                        faceVertices.clear();
                        faceVertices = face.getVertices(faceVertices);
                        int verticesCount = faceVertices.size();
                        for (int k = 0; k < verticesCount; k++) {
                            HalfEdgeVertex vertex = faceVertices.get(k);
                            if (visitedVertexMap.containsKey(vertex)) {
                                continue;
                            }
                            Vector2d texCoord = vertex.getTexcoords();
                            texCoord.x -= offsetX;
                            texCoord.y -= offsetY;
                            visitedVertexMap.put(vertex, vertex);
                        }
                    }
                }
            }

            // create a new GaiaTextureScissorData
            GaiaTextureScissorData textureScissorData = new GaiaTextureScissorData();
            textureScissorData.setTexCoordBoundary(groupTexCoordBRect);

            // calculate the expanded boundary in pixels
            double groupTexCoordMinX = groupTexCoordBRect.getMinX();
            double groupTexCoordMinY = groupTexCoordBRect.getMinY();
            double groupTexCoordMaxX = groupTexCoordBRect.getMaxX();
            double groupTexCoordMaxY = groupTexCoordBRect.getMaxY();
            double minPixelPosX = groupTexCoordMinX * (double) texWidth;
            double minPixelPosY = groupTexCoordMinY * (double) texHeight;
            double maxPixelPosX = groupTexCoordMaxX * (double) texWidth;
            double maxPixelPosY = groupTexCoordMaxY * (double) texHeight;
            GaiaRectangle noExpandedRect = new GaiaRectangle(minPixelPosX, minPixelPosY, maxPixelPosX, maxPixelPosY);
            textureScissorData.setNoExpandedBoundary(noExpandedRect);

            double width = groupTexCoordBRect.getWidthInt();
            double height = groupTexCoordBRect.getHeightInt();

            double pixelWidth = maxPixelPosX - minPixelPosX;
            double pixelHeight = maxPixelPosY - minPixelPosY;

            int expandedPixels = calculateExpandedPixels(pixelWidth, pixelHeight);

            minPixelPosX -= expandedPixels;
            minPixelPosY -= expandedPixels;
            maxPixelPosX += expandedPixels;
            maxPixelPosY += expandedPixels;

            textureScissorData.setExpandedPixel(expandedPixels);

            GaiaRectangle expandedCurrBoundary = new GaiaRectangle(minPixelPosX, minPixelPosY, maxPixelPosX, maxPixelPosY);
            textureScissorData.setCurrentBoundary(expandedCurrBoundary);

            textureScissorData.setFaces(weldedFacesGroup); // set the faces

            if (width == 0 || height == 0) {
                continue;
            }

            if (width > height) {
                textureScissorDatasWidth.add(textureScissorData);
            } else {
                textureScissorDatasHeight.add(textureScissorData);
            }
        }

        // Now, sort the textureScissorDatas by xLength & yLength (big to small)
        textureScissorDatasWidth = textureScissorDatasWidth.stream().sorted(Comparator.comparing(textureScissorData -> textureScissorData.getCurrentBoundary().getWidthInt())).collect(Collectors.toList());
        Collections.reverse(textureScissorDatasWidth);
        textureScissorDatasHeight = textureScissorDatasHeight.stream().sorted(Comparator.comparing(textureScissorData -> textureScissorData.getCurrentBoundary().getHeightInt())).collect(Collectors.toList());
        Collections.reverse(textureScissorDatasHeight);

        // make a unique textureScissorData, alternating width & height
        int textureScissorDatasWidthCount = textureScissorDatasWidth.size();
        int textureScissorDatasHeightCount = textureScissorDatasHeight.size();

        List<GaiaTextureScissorData> textureScissorDatas = new ArrayList<>();
        int maxCount = Math.max(textureScissorDatasWidthCount, textureScissorDatasHeightCount);
        for (int i = 0; i < maxCount; i++) {
            if (i < textureScissorDatasWidthCount) {
                textureScissorDatas.add(textureScissorDatasWidth.get(i));
            }

            if (i < textureScissorDatasHeightCount) {
                textureScissorDatas.add(textureScissorDatasHeight.get(i));
            }
        }

        doAtlasTextureProcessByScissorDates(textureScissorDatas);

        // recalculate texCoords.***************************************************************************************
        int maxWidth = getMaxWidthScissorDates(textureScissorDatas);
        int maxHeight = getMaxHeightScissorDates(textureScissorDatas);
        if (maxWidth == 0 || maxHeight == 0) {
            log.warn("[WARN] HalfEdgeSurface.scissorTextures() : maxWidth == 0 || maxHeight == 0.");
            return null;
        }

        double originalArea = texWidth * texHeight;
        double atlasArea = maxWidth * maxHeight;
        double diffPercent = (atlasArea - originalArea) / originalArea * 100.0;
        log.debug("scissorProcess : diffPercent ( % ) = " + (int) diffPercent + " %");
        if (atlasArea > originalArea) {
            log.debug("[WARN] HalfEdgeSurface.scissorTextures() : Atlas area is bigger than original area. diffPercent = " + (int) diffPercent + " %");
        }

        visitedVertexMap.clear();

        int textureScissorDatasCount = textureScissorDatas.size();
        log.debug("TextureScissorDatasCount : " + textureScissorDatasCount);
        for (int i = 0; i < textureScissorDatasCount; i++) {
            //log.debug("textureScissorDatas : " + i + " / " + textureScissorDatasCount);
            GaiaTextureScissorData textureScissorData = textureScissorDatas.get(i);
            if (!textureScissorData.validate()) {
                log.error("[ERROR] HalfEdgeSurface.scissorTextures() : textureScissorData.TEST_Check() == false.");
            }
            List<HalfEdgeFace> faceGroup = textureScissorData.getFaces();
            GaiaRectangle currentBoundary = textureScissorData.getCurrentBoundary();
            GaiaRectangle batchedBoundary = textureScissorData.getBatchedBoundary();
            GaiaRectangle texCoordBoundary = textureScissorData.getTexCoordBoundary();
            GaiaRectangle noExpandedRect = textureScissorData.getNoExpandedBoundary();

            //int badFacesCount0 = TestUtils.checkTexCoordsOfHalfEdgeFaces(faceGroup);

            if (texCoordBoundary == null) {
                log.error("[ERROR] HalfEdgeSurface.scissorTextures() : texCoordBoundary == null.");
            }

            // obtain all vertex of the faceGroup
            groupVertexMap.clear();
            int facesCount = faceGroup.size();
            //GaiaRectangle groupTexCoordBRect = new GaiaRectangle();
            for (int j = 0; j < facesCount; j++) {
                HalfEdgeFace face = faceGroup.get(j);
                if (face.getStatus() == ObjectStatus.DELETED) {

                }
                //groupTexCoordBRect = face.getTexCoordBoundingRectangle(groupTexCoordBRect, invertTexCoordY);
                faceVertices.clear();
                faceVertices = face.getVertices(faceVertices);
                int verticesCount = faceVertices.size();
                for (int k = 0; k < verticesCount; k++) {
                    HalfEdgeVertex vertex = faceVertices.get(k);
                    groupVertexMap.put(vertex, vertex);
                }
            }

            // now, calculate the vertex list from the map
            List<HalfEdgeVertex> vertexList = new ArrayList<>(groupVertexMap.values());
            int verticesCount = vertexList.size();
            int currBoundaryWidth = currentBoundary.getWidthInt();
            int currBoundaryHeight = currentBoundary.getHeightInt();
            double texCoordClampError = 1e-6; // Small epsilon to prevent clamping issues

            for (int k = 0; k < verticesCount; k++) {
                HalfEdgeVertex vertex = vertexList.get(k);
                if (visitedVertexMap.containsKey(vertex)) {
                    continue;
                }
                visitedVertexMap.put(vertex, vertex);
                Vector2d texCoord = vertex.getTexcoords();

                // transform the texCoords to texCoordRelToCurrentBoundary
                if (currBoundaryWidth != 0 || currBoundaryHeight != 0) {
                    double x = texCoord.x;
                    double y = texCoord.y;

                    int expandedPixels = textureScissorData.getExpandedPixel();

                    noExpandedRect = textureScissorData.getNoExpandedBoundary();
                    batchedBoundary = textureScissorData.getBatchedBoundary();

                    int srcX = (int) Math.floor(noExpandedRect.getMinX());
                    int srcY = (int) Math.floor(noExpandedRect.getMinY());

                    double originalPixelX = x * texWidth;
                    double originalPixelY = y * texHeight;

                    double innerMinX = batchedBoundary.getMinX() + expandedPixels;
                    double innerMinY = batchedBoundary.getMinY() + expandedPixels;

                    double xAtlas = (innerMinX + (originalPixelX - srcX)) / maxWidth;
                    double yAtlas = (innerMinY + (originalPixelY - srcY)) / maxHeight;

                    Vector2d texCoordFinal = new Vector2d(xAtlas, yAtlas);
                    GaiaTextureUtils.clampTextureCoordinate(texCoordFinal, texCoordClampError);

                    texCoord.set(texCoordFinal.x, texCoordFinal.y);
                    vertex.setTexcoords(texCoord);
                } else {
                    texCoord.set(0.0, 0.0);
                    vertex.setTexcoords(texCoord);
                }
            }

        }

        // make the atlas texture.**************************************************************************************
        int imageType = existPngTextures ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        log.debug("[Tile][Photogrammetry][Atlas] Atlas maxWidth : " + maxWidth + " , maxHeight : " + maxHeight);
        resultTextureAtlas.createImage(maxWidth, maxHeight, imageType);

        // Fill atlas background with known color
        BufferedImage atlasImage = resultTextureAtlas.getBufferedImage();

        Graphics2D g = atlasImage.createGraphics();
        try {
            g.setColor(new Color(255, 0, 255)); // magenta debug
            g.fillRect(0, 0, maxWidth, maxHeight);
        } finally {
            g.dispose();
        }

        WritableRaster atlasRaster = atlasImage.getRaster();

        Raster srcRaster = srcImage.getRaster();

        int atlasW = atlasImage.getWidth();
        int atlasH = atlasImage.getHeight();

        int bands = atlasRaster.getNumBands();
        int[] pixelBuffer = new int[bands];

        textureScissorDatasCount = textureScissorDatas.size();
        for (int i = 0; i < textureScissorDatasCount; i++) {
            GaiaTextureScissorData data = textureScissorDatas.get(i);

            GaiaRectangle noExp = data.getNoExpandedBoundary();
            GaiaRectangle batched = data.getBatchedBoundary();

            int expanded = Math.max(data.getExpandedPixel(), 0);

            int srcX = (int) Math.floor(noExp.getMinX());
            int srcY = (int) Math.floor(noExp.getMinY());
            int w = Math.max(noExp.getWidthInt(), 1);
            int h = Math.max(noExp.getHeightInt(), 1);

            int dstX = (int) Math.floor(batched.getMinX());
            int dstY = (int) Math.floor(batched.getMinY());

            // Si el destino completo está totalmente fuera, saltar.
            int totalDstW = expanded * 2 + w;
            int totalDstH = expanded * 2 + h;

            if (dstX >= atlasW || dstY >= atlasH || dstX + totalDstW <= 0 || dstY + totalDstH <= 0) {
                log.warn("Skipping scissor fully outside atlas: dstX={}, dstY={}, totalW={}, totalH={}, atlasW={}, atlasH={}",
                        dstX, dstY, totalDstW, totalDstH, atlasW, atlasH);
                continue;
            }

            // =========================
            // 1. COPIA CENTRAL
            // =========================
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX + x,
                            srcY + y,
                            dstX + expanded + x,
                            dstY + expanded + y,
                            pixelBuffer
                    );
                }
            }

            // =========================
            // 2. BORDES CLAMP
            // =========================

            // TOP
            for (int y = 0; y < expanded; y++) {
                for (int x = 0; x < w; x++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX + x,
                            srcY,
                            dstX + expanded + x,
                            dstY + y,
                            pixelBuffer
                    );
                }
            }

            // BOTTOM
            for (int y = 0; y < expanded; y++) {
                for (int x = 0; x < w; x++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX + x,
                            srcY + h - 1,
                            dstX + expanded + x,
                            dstY + expanded + h + y,
                            pixelBuffer
                    );
                }
            }

            // LEFT
            for (int x = 0; x < expanded; x++) {
                for (int y = 0; y < h; y++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX,
                            srcY + y,
                            dstX + x,
                            dstY + expanded + y,
                            pixelBuffer
                    );
                }
            }

            // RIGHT
            for (int x = 0; x < expanded; x++) {
                for (int y = 0; y < h; y++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX + w - 1,
                            srcY + y,
                            dstX + expanded + w + x,
                            dstY + expanded + y,
                            pixelBuffer
                    );
                }
            }

            // =========================
            // 3. ESQUINAS
            // =========================

            // TL
            for (int y = 0; y < expanded; y++) {
                for (int x = 0; x < expanded; x++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX,
                            srcY,
                            dstX + x,
                            dstY + y,
                            pixelBuffer
                    );
                }
            }

            // TR
            for (int y = 0; y < expanded; y++) {
                for (int x = 0; x < expanded; x++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX + w - 1,
                            srcY,
                            dstX + expanded + w + x,
                            dstY + y,
                            pixelBuffer
                    );
                }
            }

            // BL
            for (int y = 0; y < expanded; y++) {
                for (int x = 0; x < expanded; x++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX,
                            srcY + h - 1,
                            dstX + x,
                            dstY + expanded + h + y,
                            pixelBuffer
                    );
                }
            }

            // BR
            for (int y = 0; y < expanded; y++) {
                for (int x = 0; x < expanded; x++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX + w - 1,
                            srcY + h - 1,
                            dstX + expanded + w + x,
                            dstY + expanded + h + y,
                            pixelBuffer
                    );
                }
            }
        }

        if (paintUsedPixels) {
            paintUsedFacesByGroupColorOnAtlas(
                    atlasImage,
                    textureScissorDatas,
                    maxWidth,
                    maxHeight
            );

        }

        dilateBackgroundColor(
                resultTextureAtlas.getBufferedImage(),
                new Color(255, 0, 255));

        // check if textureAtlas width > 8192 and or height > 8192
        if (maxWidth > 8192 || maxHeight > 8192) {
            // resize the textureAtlas
            int newWidth = Math.min(maxWidth, 8192);
            int newHeight = Math.min(maxHeight, 8192);
            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, imageType);
            Graphics2D g2dResized = resizedImage.createGraphics();
            g2dResized.drawImage(resultTextureAtlas.getBufferedImage(), 0, 0, newWidth, newHeight, null);
            g2dResized.dispose();
            resultTextureAtlas.setBufferedImage(resizedImage);
            resultTextureAtlas.setWidth(newWidth);
            resultTextureAtlas.setHeight(newHeight);
        }

        return textureScissorDatas;
    }

    private void paintUsedFacesRedOnAtlas(
            BufferedImage atlasImage,
            List<GaiaTextureScissorData> textureScissorDatas,
            int atlasWidth,
            int atlasHeight
    ) {
        if (atlasImage == null || textureScissorDatas == null || textureScissorDatas.isEmpty()) {
            return;
        }

        Graphics2D g = atlasImage.createGraphics();
        try {
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            List<HalfEdgeVertex> faceVertices = new ArrayList<>();

            // 1. Pintar relleno rojo semitransparente
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));
            g.setColor(Color.RED);

            for (GaiaTextureScissorData data : textureScissorDatas) {
                // create random color.

                if (data == null || data.getFaces() == null || data.getFaces().isEmpty()) {
                    continue;
                }

                for (HalfEdgeFace face : data.getFaces()) {
                    if (face == null) {
                        continue;
                    }

                    faceVertices.clear();
                    face.getVertices(faceVertices);

                    if (faceVertices.size() < 3) {
                        continue;
                    }

                    Polygon polygon = new Polygon();

                    for (HalfEdgeVertex vertex : faceVertices) {
                        if (vertex == null || vertex.getTexcoords() == null) {
                            continue;
                        }

                        Vector2d uv = vertex.getTexcoords();

                        int px = (int) Math.round(uv.x * atlasWidth);
                        int py = (int) Math.round(uv.y * atlasHeight);

                        polygon.addPoint(px, py);
                    }

                    if (polygon.npoints >= 3) {
                        g.fillPolygon(polygon);
                    }
                }
            }

            // 2. Pintar bordes en rojo sólido para ver mejor las islas
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
            g.setColor(Color.RED);

            for (GaiaTextureScissorData data : textureScissorDatas) {
                if (data == null || data.getFaces() == null || data.getFaces().isEmpty()) {
                    continue;
                }

                for (HalfEdgeFace face : data.getFaces()) {
                    if (face == null) {
                        continue;
                    }

                    faceVertices.clear();
                    face.getVertices(faceVertices);

                    if (faceVertices.size() < 3) {
                        continue;
                    }

                    Polygon polygon = new Polygon();

                    for (HalfEdgeVertex vertex : faceVertices) {
                        if (vertex == null || vertex.getTexcoords() == null) {
                            continue;
                        }

                        Vector2d uv = vertex.getTexcoords();

                        int px = (int) Math.round(uv.x * atlasWidth);
                        int py = (int) Math.round(uv.y * atlasHeight);

                        polygon.addPoint(px, py);
                    }

                    if (polygon.npoints >= 3) {
                        g.drawPolygon(polygon);
                    }
                }
            }

        } finally {
            g.dispose();
        }
    }

    private void paintUsedFacesByGroupColorOnAtlas(
            BufferedImage atlasImage,
            List<GaiaTextureScissorData> textureScissorDatas,
            int atlasWidth,
            int atlasHeight
    ) {
        if (atlasImage == null || textureScissorDatas == null || textureScissorDatas.isEmpty()) {
            return;
        }

        Graphics2D g = atlasImage.createGraphics();
        try {
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            List<HalfEdgeVertex> faceVertices = new ArrayList<>();

            int totalGroups = textureScissorDatas.size();

            for (int groupIdx = 0; groupIdx < totalGroups; groupIdx++) {
                GaiaTextureScissorData data = textureScissorDatas.get(groupIdx);
                if (data == null || data.getFaces() == null || data.getFaces().isEmpty()) {
                    continue;
                }

                // Un color fijo y distinto por grupo.
                Color fillColor = getDebugColorForGroup(groupIdx, totalGroups);
                //fillColor = new Color(255,0,0); // solid
                Color lineColor = fillColor.darker();
                lineColor = new Color(0, 0, 0); // solid

                // 1. Relleno semitransparente para todas las faces del grupo.
                float alpha = 0.45f;
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g.setColor(fillColor);

                for (HalfEdgeFace face : data.getFaces()) {
                    if (face == null) {
                        continue;
                    }

                    faceVertices.clear();
                    face.getVertices(faceVertices);

                    if (faceVertices.size() < 3) {
                        continue;
                    }

                    Polygon polygon = new Polygon();

                    for (HalfEdgeVertex vertex : faceVertices) {
                        if (vertex == null || vertex.getTexcoords() == null) {
                            continue;
                        }

                        Vector2d uv = vertex.getTexcoords();

                        int px = (int) Math.round(uv.x * atlasWidth);
                        int py = (int) Math.round(uv.y * atlasHeight);

                        polygon.addPoint(px, py);
                    }

                    if (polygon.npoints >= 3) {
                        g.fillPolygon(polygon);
                    }
                }

                // 2. Bordes sólidos del mismo grupo.
                g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
                g.setColor(lineColor);

                for (HalfEdgeFace face : data.getFaces()) {
                    if (face == null) {
                        continue;
                    }

                    faceVertices.clear();
                    face.getVertices(faceVertices);

                    if (faceVertices.size() < 3) {
                        continue;
                    }

                    Polygon polygon = new Polygon();

                    for (HalfEdgeVertex vertex : faceVertices) {
                        if (vertex == null || vertex.getTexcoords() == null) {
                            continue;
                        }

                        Vector2d uv = vertex.getTexcoords();

                        int px = (int) Math.round(uv.x * atlasWidth);
                        int py = (int) Math.round(uv.y * atlasHeight);

                        polygon.addPoint(px, py);
                    }

                    if (polygon.npoints >= 3) {
                        g.drawPolygon(polygon);
                    }
                }
            }

        } finally {
            g.dispose();
        }
    }

    private void paintUsedFacesByCameraDirectionTypeOnAtlas(
            BufferedImage atlasImage,
            Map<GaiaFace, HalfEdgeFace> mapGaiaFaceToHalfEdgeFace,
            Map<GaiaFace, CameraDirectionTypeInfo> mapGaiaFaceToCameraDirectionTypeInfo,
            int atlasWidth,
            int atlasHeight
    ) {
        if (atlasImage == null ||
                mapGaiaFaceToHalfEdgeFace == null ||
                mapGaiaFaceToCameraDirectionTypeInfo == null ||
                mapGaiaFaceToCameraDirectionTypeInfo.isEmpty()) {
            return;
        }

        Graphics2D g = atlasImage.createGraphics();
        try {
            g.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON
            );

            List<HalfEdgeVertex> faceVertices = new ArrayList<>();

            // 1. Relleno semitransparente por CameraDirectionType.
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.35f));

            for (Map.Entry<GaiaFace, CameraDirectionTypeInfo> entry : mapGaiaFaceToCameraDirectionTypeInfo.entrySet()) {
                GaiaFace gaiaFace = entry.getKey();
                CameraDirectionTypeInfo info = entry.getValue();

                if (gaiaFace == null || info == null) {
                    continue;
                }

                HalfEdgeFace halfEdgeFace = mapGaiaFaceToHalfEdgeFace.get(gaiaFace);
                if (halfEdgeFace == null) {
                    continue;
                }

                CameraDirectionType cameraDirectionType = info.getCameraDirectionType();
                if (cameraDirectionType == null) {
                    cameraDirectionType = halfEdgeFace.getCameraDirectionType();
                }
                if (cameraDirectionType == null) {
                    cameraDirectionType = CameraDirectionType.ZNEG;
                }

                Color color = getDebugColorForCameraDirectionType(cameraDirectionType);
                g.setColor(color);

                Polygon polygon = createAtlasUvPolygon(
                        halfEdgeFace,
                        faceVertices,
                        atlasWidth,
                        atlasHeight
                );

                if (polygon != null && polygon.npoints >= 3) {
                    g.fillPolygon(polygon);
                }
            }

            // 2. Bordes sólidos.
            g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));

            for (Map.Entry<GaiaFace, CameraDirectionTypeInfo> entry : mapGaiaFaceToCameraDirectionTypeInfo.entrySet()) {
                GaiaFace gaiaFace = entry.getKey();
                CameraDirectionTypeInfo info = entry.getValue();

                if (gaiaFace == null || info == null) {
                    continue;
                }

                HalfEdgeFace halfEdgeFace = mapGaiaFaceToHalfEdgeFace.get(gaiaFace);
                if (halfEdgeFace == null) {
                    continue;
                }

                CameraDirectionType cameraDirectionType = info.getCameraDirectionType();
                if (cameraDirectionType == null) {
                    cameraDirectionType = halfEdgeFace.getCameraDirectionType();
                }
                if (cameraDirectionType == null) {
                    cameraDirectionType = CameraDirectionType.ZNEG;
                }

                Color color = getDebugColorForCameraDirectionType(cameraDirectionType).darker();
                g.setColor(color);

                Polygon polygon = createAtlasUvPolygon(
                        halfEdgeFace,
                        faceVertices,
                        atlasWidth,
                        atlasHeight
                );

                if (polygon != null && polygon.npoints >= 3) {
                    g.drawPolygon(polygon);
                }
            }

        } finally {
            g.dispose();
        }
    }

    private Polygon createAtlasUvPolygon(
            HalfEdgeFace face,
            List<HalfEdgeVertex> reusableVertices,
            int atlasWidth,
            int atlasHeight
    ) {
        if (face == null || reusableVertices == null) {
            return null;
        }

        reusableVertices.clear();
        face.getVertices(reusableVertices);

        if (reusableVertices.size() < 3) {
            return null;
        }

        Polygon polygon = new Polygon();

        for (HalfEdgeVertex vertex : reusableVertices) {
            if (vertex == null || vertex.getTexcoords() == null) {
                continue;
            }

            Vector2d uv = vertex.getTexcoords();

            int px = (int) Math.round(uv.x * atlasWidth);
            int py = (int) Math.round(uv.y * atlasHeight);

            polygon.addPoint(px, py);
        }

        return polygon.npoints >= 3 ? polygon : null;
    }

    private Color getDebugColorForCameraDirectionType(CameraDirectionType cameraDirectionType) {
        if (cameraDirectionType == null) {
            return new Color(255, 255, 255);
        }

        switch (cameraDirectionType) {
            case ZNEG:
                return new Color(255, 0, 0);       // rojo

            case XPOS_ZNEG:
                return new Color(0, 255, 0);       // verde

            case XNEG_ZNEG:
                return new Color(0, 120, 255);     // azul

            case YPOS_ZNEG:
                return new Color(255, 180, 0);     // naranja

            case YNEG_ZNEG:
                return new Color(255, 0, 255);     // magenta

            case XPOS_YPOS_ZNEG:
                return new Color(0, 255, 255);     // cian

            case XNEG_YPOS_ZNEG:
                return new Color(180, 0, 255);     // violeta

            case XPOS_YNEG_ZNEG:
                return new Color(255, 255, 0);     // amarillo

            case XNEG_YNEG_ZNEG:
                return new Color(120, 255, 120);   // verde claro

            default:
                return new Color(255, 255, 255);   // blanco
        }
    }

    private Color getDebugColorForGroup(int groupIdx, int totalGroups) {
        if (totalGroups <= 0) {
            totalGroups = 1;
        }

        float hue = (float) groupIdx / (float) totalGroups;
        float saturation = 0.85f;
        float brightness = 1.0f;

        return Color.getHSBColor(hue, saturation, brightness);
    }

    public List<GaiaTextureScissorData> calculateTextureScissorDates_original(List<List<HalfEdgeFace>> mergedWeldedFacesGroups,
                                                                              int texWidth,
                                                                              int texHeight,
                                                                              boolean existPngTextures,
                                                                              BufferedImage srcImage,
                                                                              GaiaTexture resultTextureAtlas) {
        // now, for each faceGroup, create a scissorData
        // there are 2 types of scissorData :
        // 1- more width than height.
        // 2- more height than width.
        List<GaiaTextureScissorData> textureScissorDatasWidth = new ArrayList<>();
        List<GaiaTextureScissorData> textureScissorDatasHeight = new ArrayList<>();
        int weldedFacesGroupsCount = mergedWeldedFacesGroups.size();

        List<HalfEdgeVertex> faceVertices = new ArrayList<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> groupVertexMap = new HashMap<>();
        Map<HalfEdgeVertex, HalfEdgeVertex> visitedVertexMap = new HashMap<>();

        log.debug("ScissorTextures : weldedFacesCount" + weldedFacesGroupsCount + " " + textureScissorDatasWidth.size());
        boolean invertTexCoordY = false;
        List<HalfEdgeVertex> memSaveVertices = new ArrayList<>();
        for (int i = 0; i < weldedFacesGroupsCount; i++) {
            List<HalfEdgeFace> weldedFacesGroup = mergedWeldedFacesGroups.get(i);
            GaiaRectangle groupTexCoordBRect = new GaiaRectangle();
            int weldedFacesCount = weldedFacesGroup.size();
            for (int j = 0; j < weldedFacesCount; j++) {
                GaiaRectangle texCoordBRect = new GaiaRectangle();
                HalfEdgeFace face = weldedFacesGroup.get(j);
                memSaveVertices.clear();
                texCoordBRect = face.getTexCoordBoundingRectangle(texCoordBRect, invertTexCoordY, memSaveVertices);

                if (j == 0) {
                    groupTexCoordBRect.copyFrom(texCoordBRect);
                } else {
                    groupTexCoordBRect.addBoundingRectangle(texCoordBRect);
                }
            }

            // check if must translate to positive quadrant
            if (groupTexCoordBRect.getMinX() < 0.0 || groupTexCoordBRect.getMinX() > 1.0 || groupTexCoordBRect.getMinY() < 0.0 || groupTexCoordBRect.getMinY() > 1.0) {
                double texCoordOriginX = groupTexCoordBRect.getMinX();
                double texCoordOriginY = groupTexCoordBRect.getMinY();
                double offsetX = 0.0;
                double offsetY = 0.0;
                if (texCoordOriginX < 0.0 || texCoordOriginX > 1.0) {
                    offsetX = Math.floor(texCoordOriginX);
                }

                if (texCoordOriginY < 0.0 || texCoordOriginY > 1.0) {
                    offsetY = Math.floor(texCoordOriginY);
                }

                if (offsetX != 0.0 || offsetY != 0.0) {
                    // must translate to positive quadrant
                    int facesCount = weldedFacesGroup.size();
                    for (int j = 0; j < facesCount; j++) {
                        HalfEdgeFace face = weldedFacesGroup.get(j);
                        faceVertices.clear();
                        faceVertices = face.getVertices(faceVertices);
                        int verticesCount = faceVertices.size();
                        for (int k = 0; k < verticesCount; k++) {
                            HalfEdgeVertex vertex = faceVertices.get(k);
                            if (visitedVertexMap.containsKey(vertex)) {
                                continue;
                            }
                            Vector2d texCoord = vertex.getTexcoords();
                            texCoord.x -= offsetX;
                            texCoord.y -= offsetY;
                            visitedVertexMap.put(vertex, vertex);
                        }
                    }
                }
            }

            // create a new GaiaTextureScissorData
            GaiaTextureScissorData textureScissorData = new GaiaTextureScissorData();
            textureScissorData.setTexCoordBoundary(groupTexCoordBRect);

            // calculate the expanded boundary in pixels
            double groupTexCoordMinX = groupTexCoordBRect.getMinX();
            double groupTexCoordMinY = groupTexCoordBRect.getMinY();
            double groupTexCoordMaxX = groupTexCoordBRect.getMaxX();
            double groupTexCoordMaxY = groupTexCoordBRect.getMaxY();
            double minPixelPosX = groupTexCoordMinX * (double) texWidth;
            double minPixelPosY = groupTexCoordMinY * (double) texHeight;
            double maxPixelPosX = groupTexCoordMaxX * (double) texWidth;
            double maxPixelPosY = groupTexCoordMaxY * (double) texHeight;
            GaiaRectangle noExpandedRect = new GaiaRectangle(minPixelPosX, minPixelPosY, maxPixelPosX, maxPixelPosY);
            textureScissorData.setNoExpandedBoundary(noExpandedRect);

            double width = groupTexCoordBRect.getWidthInt();
            double height = groupTexCoordBRect.getHeightInt();

            double pixelWidth = maxPixelPosX - minPixelPosX;
            double pixelHeight = maxPixelPosY - minPixelPosY;

            int expandedPixels = calculateExpandedPixels(pixelWidth, pixelHeight);

            minPixelPosX -= expandedPixels;
            minPixelPosY -= expandedPixels;
            maxPixelPosX += expandedPixels;
            maxPixelPosY += expandedPixels;

            textureScissorData.setExpandedPixel(expandedPixels);

            GaiaRectangle expandedCurrBoundary = new GaiaRectangle(minPixelPosX, minPixelPosY, maxPixelPosX, maxPixelPosY);
            textureScissorData.setCurrentBoundary(expandedCurrBoundary);

            textureScissorData.setFaces(weldedFacesGroup); // set the faces

            if (width == 0 || height == 0) {
                continue;
            }

            if (width > height) {
                textureScissorDatasWidth.add(textureScissorData);
            } else {
                textureScissorDatasHeight.add(textureScissorData);
            }
        }

        // Now, sort the textureScissorDatas by xLength & yLength (big to small)
        textureScissorDatasWidth = textureScissorDatasWidth.stream().sorted(Comparator.comparing(textureScissorData -> textureScissorData.getCurrentBoundary().getWidthInt())).collect(Collectors.toList());
        Collections.reverse(textureScissorDatasWidth);
        textureScissorDatasHeight = textureScissorDatasHeight.stream().sorted(Comparator.comparing(textureScissorData -> textureScissorData.getCurrentBoundary().getHeightInt())).collect(Collectors.toList());
        Collections.reverse(textureScissorDatasHeight);

        // make a unique textureScissorData, alternating width & height
        int textureScissorDatasWidthCount = textureScissorDatasWidth.size();
        int textureScissorDatasHeightCount = textureScissorDatasHeight.size();

        List<GaiaTextureScissorData> textureScissorDatas = new ArrayList<>();
        int maxCount = Math.max(textureScissorDatasWidthCount, textureScissorDatasHeightCount);
        for (int i = 0; i < maxCount; i++) {
            if (i < textureScissorDatasWidthCount) {
                textureScissorDatas.add(textureScissorDatasWidth.get(i));
            }

            if (i < textureScissorDatasHeightCount) {
                textureScissorDatas.add(textureScissorDatasHeight.get(i));
            }
        }

        doAtlasTextureProcessByScissorDates(textureScissorDatas);

        // recalculate texCoords.***************************************************************************************
        int maxWidth = getMaxWidthScissorDates(textureScissorDatas);
        int maxHeight = getMaxHeightScissorDates(textureScissorDatas);
        if (maxWidth == 0 || maxHeight == 0) {
            log.warn("[WARN] HalfEdgeSurface.scissorTextures() : maxWidth == 0 || maxHeight == 0.");
            return null;
        }

        double originalArea = texWidth * texHeight;
        double atlasArea = maxWidth * maxHeight;
        double diffPercent = (atlasArea - originalArea) / originalArea * 100.0;
        log.debug("scissorProcess : diffPercent ( % ) = " + (int) diffPercent + " %");
        if (atlasArea > originalArea) {
            log.debug("[WARN] HalfEdgeSurface.scissorTextures() : Atlas area is bigger than original area. diffPercent = " + (int) diffPercent + " %");
        }

        visitedVertexMap.clear();

        int textureScissorDatasCount = textureScissorDatas.size();
        log.debug("TextureScissorDatasCount : " + textureScissorDatasCount);
        for (int i = 0; i < textureScissorDatasCount; i++) {
            //log.debug("textureScissorDatas : " + i + " / " + textureScissorDatasCount);
            GaiaTextureScissorData textureScissorData = textureScissorDatas.get(i);
            if (!textureScissorData.validate()) {
                log.error("[ERROR] HalfEdgeSurface.scissorTextures() : textureScissorData.TEST_Check() == false.");
            }
            List<HalfEdgeFace> faceGroup = textureScissorData.getFaces();
            GaiaRectangle currentBoundary = textureScissorData.getCurrentBoundary();
            GaiaRectangle batchedBoundary = textureScissorData.getBatchedBoundary();
            GaiaRectangle texCoordBoundary = textureScissorData.getTexCoordBoundary();
            GaiaRectangle noExpandedRect = textureScissorData.getNoExpandedBoundary();

            //int badFacesCount0 = TestUtils.checkTexCoordsOfHalfEdgeFaces(faceGroup);

            if (texCoordBoundary == null) {
                log.error("[ERROR] HalfEdgeSurface.scissorTextures() : texCoordBoundary == null.");
            }

            // obtain all vertex of the faceGroup
            groupVertexMap.clear();
            int facesCount = faceGroup.size();
            //GaiaRectangle groupTexCoordBRect = new GaiaRectangle();
            for (int j = 0; j < facesCount; j++) {
                HalfEdgeFace face = faceGroup.get(j);
                if (face.getStatus() == ObjectStatus.DELETED) {

                }
                //groupTexCoordBRect = face.getTexCoordBoundingRectangle(groupTexCoordBRect, invertTexCoordY);
                faceVertices.clear();
                faceVertices = face.getVertices(faceVertices);
                int verticesCount = faceVertices.size();
                for (int k = 0; k < verticesCount; k++) {
                    HalfEdgeVertex vertex = faceVertices.get(k);
                    groupVertexMap.put(vertex, vertex);
                }
            }

            // now, calculate the vertex list from the map
            List<HalfEdgeVertex> vertexList = new ArrayList<>(groupVertexMap.values());
            int verticesCount = vertexList.size();
            int currBoundaryWidth = currentBoundary.getWidthInt();
            int currBoundaryHeight = currentBoundary.getHeightInt();
            double texCoordClampError = 1e-8;

            for (int k = 0; k < verticesCount; k++) {
                HalfEdgeVertex vertex = vertexList.get(k);
                if (visitedVertexMap.containsKey(vertex)) {
                    continue;
                }
                visitedVertexMap.put(vertex, vertex);
                Vector2d texCoord = vertex.getTexcoords();

                // transform the texCoords to texCoordRelToCurrentBoundary
                if (currBoundaryWidth != 0 || currBoundaryHeight != 0) {
                    double x = texCoord.x;
                    double y = texCoord.y;

                    double xRel = (x - texCoordBoundary.getMinX()) / texCoordBoundary.getWidth();
                    double yRel = (y - texCoordBoundary.getMinY()) / texCoordBoundary.getHeight(); // original

                    // now calculate the texCoordRel (0-1) inside the currentBoundary. The currentBoundary is a expandedPixels bigger than the originalBoundary
                    int expandedPixels = textureScissorData.getExpandedPixel();
                    double originalBoundaryMinX = currentBoundary.getMinX() + expandedPixels;
                    double originalBoundaryMinY = currentBoundary.getMinY() + expandedPixels;
                    double originalBoundaryMaxX = currentBoundary.getMaxX() - expandedPixels;
                    double originalBoundaryMaxY = currentBoundary.getMaxY() - expandedPixels;
                    double originalBoundaryWidth = originalBoundaryMaxX - originalBoundaryMinX;
                    double originalBoundaryHeight = originalBoundaryMaxY - originalBoundaryMinY;
                    xRel = (originalBoundaryMinX + xRel * originalBoundaryWidth - currentBoundary.getMinX()) / currBoundaryWidth;
                    yRel = (originalBoundaryMinY + yRel * originalBoundaryHeight - currentBoundary.getMinY()) / currBoundaryHeight;

                    Vector2d texCoordRel = new Vector2d(xRel, yRel);
                    GaiaTextureUtils.clampTextureCoordinate(texCoordRel, texCoordClampError);

                    xRel = texCoordRel.x;
                    yRel = texCoordRel.y;

                    // transform the texCoordRelToCurrentBoundary to atlasBoundary using batchedBoundary
//                    double xAtlas = (batchedBoundary.getMinX() + xRel * batchedBoundary.getWidthInt()) / maxWidth;
//                    double yAtlas = (batchedBoundary.getMinY() + yRel * batchedBoundary.getHeightInt()) / maxHeight;
                    double innerMinX = batchedBoundary.getMinX() + expandedPixels;
                    double innerMinY = batchedBoundary.getMinY() + expandedPixels;

                    double innerW = noExpandedRect.getWidthInt();
                    double innerH = noExpandedRect.getHeightInt();

                    double xAtlas = (innerMinX + 0.5 + xRel * Math.max(innerW - 1.0, 0.0)) / maxWidth;
                    double yAtlas = (innerMinY + 0.5 + yRel * Math.max(innerH - 1.0, 0.0)) / maxHeight;

                    Vector2d texCoordFinal = new Vector2d(xAtlas, yAtlas);
                    GaiaTextureUtils.clampTextureCoordinate(texCoordFinal, texCoordClampError);
                    texCoord.set(texCoordFinal.x, texCoordFinal.y);
                    vertex.setTexcoords(texCoord);
                } else {
                    texCoord.set(0.0, 0.0);
                    vertex.setTexcoords(texCoord);
                }
            }

        }

        // make the atlas texture.**************************************************************************************
        int imageType = existPngTextures ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB;
        log.debug("[Tile][Photogrammetry][Atlas] Atlas maxWidth : " + maxWidth + " , maxHeight : " + maxHeight);
        resultTextureAtlas.createImage(maxWidth, maxHeight, imageType);

        // Fill atlas background with known color
        BufferedImage atlasImage = resultTextureAtlas.getBufferedImage();

        Graphics2D g = atlasImage.createGraphics();
        try {
            g.setColor(new Color(255, 0, 255)); // magenta debug
            g.fillRect(0, 0, maxWidth, maxHeight);
        } finally {
            g.dispose();
        }

        WritableRaster atlasRaster = atlasImage.getRaster();

        Raster srcRaster = srcImage.getRaster();

        int atlasW = atlasImage.getWidth();
        int atlasH = atlasImage.getHeight();

        int bands = atlasRaster.getNumBands();
        int[] pixelBuffer = new int[bands];

        textureScissorDatasCount = textureScissorDatas.size();
        for (int i = 0; i < textureScissorDatasCount; i++) {
            GaiaTextureScissorData data = textureScissorDatas.get(i);

            GaiaRectangle noExp = data.getNoExpandedBoundary();
            GaiaRectangle batched = data.getBatchedBoundary();

            int expanded = Math.max(data.getExpandedPixel(), 0);

            int srcX = (int) Math.floor(noExp.getMinX());
            int srcY = (int) Math.floor(noExp.getMinY());
            int w = Math.max(noExp.getWidthInt(), 1);
            int h = Math.max(noExp.getHeightInt(), 1);

            int dstX = (int) Math.floor(batched.getMinX());
            int dstY = (int) Math.floor(batched.getMinY());

            // Si el destino completo está totalmente fuera, saltar.
            int totalDstW = expanded * 2 + w;
            int totalDstH = expanded * 2 + h;

            if (dstX >= atlasW || dstY >= atlasH || dstX + totalDstW <= 0 || dstY + totalDstH <= 0) {
                log.warn("Skipping scissor fully outside atlas: dstX={}, dstY={}, totalW={}, totalH={}, atlasW={}, atlasH={}",
                        dstX, dstY, totalDstW, totalDstH, atlasW, atlasH);
                continue;
            }

            // =========================
            // 1. COPIA CENTRAL
            // =========================
            for (int y = 0; y < h; y++) {
                for (int x = 0; x < w; x++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX + x,
                            srcY + y,
                            dstX + expanded + x,
                            dstY + expanded + y,
                            pixelBuffer
                    );
                }
            }

            // =========================
            // 2. BORDES CLAMP
            // =========================

            // TOP
            for (int y = 0; y < expanded; y++) {
                for (int x = 0; x < w; x++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX + x,
                            srcY,
                            dstX + expanded + x,
                            dstY + y,
                            pixelBuffer
                    );
                }
            }

            // BOTTOM
            for (int y = 0; y < expanded; y++) {
                for (int x = 0; x < w; x++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX + x,
                            srcY + h - 1,
                            dstX + expanded + x,
                            dstY + expanded + h + y,
                            pixelBuffer
                    );
                }
            }

            // LEFT
            for (int x = 0; x < expanded; x++) {
                for (int y = 0; y < h; y++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX,
                            srcY + y,
                            dstX + x,
                            dstY + expanded + y,
                            pixelBuffer
                    );
                }
            }

            // RIGHT
            for (int x = 0; x < expanded; x++) {
                for (int y = 0; y < h; y++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX + w - 1,
                            srcY + y,
                            dstX + expanded + w + x,
                            dstY + expanded + y,
                            pixelBuffer
                    );
                }
            }

            // =========================
            // 3. ESQUINAS
            // =========================

            // TL
            for (int y = 0; y < expanded; y++) {
                for (int x = 0; x < expanded; x++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX,
                            srcY,
                            dstX + x,
                            dstY + y,
                            pixelBuffer
                    );
                }
            }

            // TR
            for (int y = 0; y < expanded; y++) {
                for (int x = 0; x < expanded; x++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX + w - 1,
                            srcY,
                            dstX + expanded + w + x,
                            dstY + y,
                            pixelBuffer
                    );
                }
            }

            // BL
            for (int y = 0; y < expanded; y++) {
                for (int x = 0; x < expanded; x++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX,
                            srcY + h - 1,
                            dstX + x,
                            dstY + expanded + h + y,
                            pixelBuffer
                    );
                }
            }

            // BR
            for (int y = 0; y < expanded; y++) {
                for (int x = 0; x < expanded; x++) {
                    safeCopyPixel(
                            srcRaster,
                            atlasRaster,
                            srcX + w - 1,
                            srcY + h - 1,
                            dstX + expanded + w + x,
                            dstY + expanded + h + y,
                            pixelBuffer
                    );
                }
            }
        }

        dilateBackgroundColor(
                resultTextureAtlas.getBufferedImage(),
                new Color(255, 0, 255));

        // check if textureAtlas width > 8192 and or height > 8192
        if (maxWidth > 8192 || maxHeight > 8192) {
            // resize the textureAtlas
            int newWidth = Math.min(maxWidth, 8192);
            int newHeight = Math.min(maxHeight, 8192);
            BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, imageType);
            Graphics2D g2dResized = resizedImage.createGraphics();
            g2dResized.drawImage(resultTextureAtlas.getBufferedImage(), 0, 0, newWidth, newHeight, null);
            g2dResized.dispose();
            resultTextureAtlas.setBufferedImage(resizedImage);
            resultTextureAtlas.setWidth(newWidth);
            resultTextureAtlas.setHeight(newHeight);
        }

        return textureScissorDatas;
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

            List<GaiaRectangle> listRectanglesMaxX = maxXrectanglesMap.computeIfAbsent(maxX, k -> new ArrayList<>());
            listRectanglesMaxX.add(batchedBoundary);
        }
    }

    private Vector2d getBestPositionMosaicInAtlas(List<TexturesAtlasData> currProcessTextureAtlasDates, TexturesAtlasData texAtlasDataToPutInMosaic, Vector2d resultVec, GaiaRectangle beforeMosaicRectangle, List<GaiaRectangle> listRectangles, TreeMap<Double, List<GaiaRectangle>> map_maxRectangles) {
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
            if (!this.intersectsRectangleAtlasingProcess(listRectangles, texAtlasDataToPutInMosaic.getBatchedBoundary(), map_maxRectangles)) {
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
            if (!this.intersectsRectangleAtlasingProcess(listRectangles, texAtlasDataToPutInMosaic.getBatchedBoundary(), map_maxRectangles)) {
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

    private boolean intersectsRectangleAtlasingProcess(List<GaiaRectangle> listRectangles, GaiaRectangle rectangle, TreeMap<Double, List<GaiaRectangle>> mapMaxRectangles) {
        // this function returns true if the rectangle intersects with any existent rectangle of the listRectangles
        boolean intersects = false;
        double error = 10E-5;

        double currRectMinX = rectangle.getMinX();

        // check with mapMaxRectangles all rectangles that have maxX > currRectMinX
        for (Map.Entry<Double, List<GaiaRectangle>> entry : mapMaxRectangles.tailMap(currRectMinX).entrySet()) {
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

    public int getMaxWidthScissorDataFull(List<GaiaTextureScissorDataFull> compareImages) {
        return compareImages.stream()
                .filter(data -> data != null && data.getBatchedBoundary() != null)
                .mapToInt(data -> (int) Math.ceil(data.getBatchedBoundary().getMaxX()))
                .max()
                .orElse(0);
    }

    public int getMaxHeightScissorDataFull(List<GaiaTextureScissorDataFull> compareImages) {
        return compareImages.stream()
                .filter(data -> data != null && data.getBatchedBoundary() != null)
                .mapToInt(data -> (int) Math.ceil(data.getBatchedBoundary().getMaxY()))
                .max()
                .orElse(0);
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
        // Note : scene must join all surfaces before call this function
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

        GaiaExtractor extractor = new GaiaExtractor();
        List<GaiaFace> faces = extractor.extractAllFaces(gaiaScene);
        Map<Integer, List<GaiaFace>> mapClassificationFacesList = new HashMap<>();
        for (GaiaFace face : faces) {
            int classificationId = face.getClassifyId();
            List<GaiaFace> faceList = mapClassificationFacesList.computeIfAbsent(classificationId, k -> new ArrayList<>());
            faceList.add(face);
        }

        int texAtlasDataCount = texAtlasDatasList.size();
        for (int i = 0; i < texAtlasDataCount; i++) {
            TexturesAtlasData texAtlasData = texAtlasDatasList.get(i);
            int classifyId = texAtlasData.getClassifyId();

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
            faceVerticesMemSave.clear();
            this.getGaiaVerticesOfFaceGroup(faceGroup, primitive.getVertices(), faceVerticesMemSave);

            // now, calculate the vertex list from the map
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

    public void recalculateTexCoordsAfterTextureAtlasingObliqueCamera(HalfEdgeScene halfEdgeScene,
                                                                      List<TexturesAtlasData> texAtlasDatasList,
                                                                      Map<Integer, Map<CameraDirectionType, List<HalfEdgeFace>>> mapClassificationCamDirTypeFacesList) {
        int maxWidth = getMaxWidth(texAtlasDatasList);
        int maxHeight = getMaxHeight(texAtlasDatasList);

        if (maxWidth == 0 || maxHeight == 0) {
            return;
        }

        Map<HalfEdgeVertex, HalfEdgeVertex> groupVertexMapMemSave = new HashMap<>();
        List<HalfEdgeVertex> faceVerticesMemSave = new ArrayList<>();

        int texAtlasDataCount = texAtlasDatasList.size();
        for (int i = 0; i < texAtlasDataCount; i++) {
            TexturesAtlasData texAtlasData = texAtlasDatasList.get(i);
            int classifyId = texAtlasData.getClassifyId();
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
        log.debug("[Tile][Photogrammetry][makeAtlasTexture] Atlas maxWidth : " + maxWidth + " , maxHeight : " + maxHeight);
        textureAtlas.createImage(maxWidth, maxHeight, imageType);

        // draw the images into textureAtlas
        log.debug("HalfEdgeSurface.scissorTextures() : draw the images into textureAtlas.");
        Graphics2D g2d = textureAtlas.getBufferedImage().createGraphics();
        int textureAtlasDatasCount = texAtlasDatasList.size();
        log.debug(("TextureAtlasDatesCount : " + textureAtlasDatasCount));

        WritableRaster atlasRaster = textureAtlas.getBufferedImage().getRaster();

        for (int i = 0; i < textureAtlasDatasCount; i++) {
            log.debug("current atlas data : " + i + " / " + textureAtlasDatasCount);
            TexturesAtlasData textureAtlasData = texAtlasDatasList.get(i);
            GaiaRectangle batchedBoundary = textureAtlasData.getBatchedBoundary();

            BufferedImage subImage = textureAtlasData.getTextureImage();

            Raster subRaster = subImage.getRaster();

            int x = (int) batchedBoundary.getMinX();
            int y = (int) batchedBoundary.getMinY();

            atlasRaster.setRect(x, y, subRaster);

//            GaiaRectangle currentBoundary = textureAtlasData.getCurrentBoundary();
//            GaiaRectangle originBoundary = textureAtlasData.getOriginalBoundary();
//            Color randomColor = new Color((float) Math.random(), (float) Math.random(), (float) Math.random(), 0.8f);
//            BufferedImage randomColoredImage = new BufferedImage(subImage.getWidth(), subImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
//            Graphics2D randomGraphics = randomColoredImage.createGraphics();
//            randomGraphics.setColor(randomColor);
//            randomGraphics.fillRect(0, 0, subImage.getWidth(), subImage.getHeight());
//            randomGraphics.dispose();
//            g2d.drawImage(randomColoredImage, (int) batchedBoundary.getMinX(), (int) batchedBoundary.getMinY(), null); // test code
//            // end test.--------------------------------------------------------------------------------------------------------------------------------

            //g2d.drawImage(subImage, (int) batchedBoundary.getMinX(), (int) batchedBoundary.getMinY(), null); // original code

        }
        g2d.dispose();

        return textureAtlas;
    }

    public GaiaTexture makeAtlasTextureScissorDataFull(List<GaiaTextureScissorDataFull> scissoredDates, int imageType) {
        // calculate the maxWidth and maxHeight
        // TODO : is it wrong to calculate the maxWidth and maxHeight by using the batchedBoundary?***
        int maxWidth = getMaxWidthScissorDataFull(scissoredDates);
        int maxHeight = getMaxHeightScissorDataFull(scissoredDates);

        if (maxWidth == 0 || maxHeight == 0) {
            log.error("[ERROR] makeAtlasTexture() : maxWidth or maxHeight is 0.");
            return null;
        }

        GaiaTexture textureAtlas = new GaiaTexture();
        log.debug("[Tile][Photogrammetry][makeAtlasTexture] Atlas maxWidth : " + maxWidth + " , maxHeight : " + maxHeight);
        textureAtlas.createImage(maxWidth, maxHeight, imageType);

        // draw the images into textureAtlas
        log.debug("HalfEdgeSurface.scissorTextures() : draw the images into textureAtlas.");
        Graphics2D g2d = textureAtlas.getBufferedImage().createGraphics();
        Color backGroundColor = new Color(255, 0, 255);

        try {
            g2d.setColor(backGroundColor);
            g2d.fillRect(0, 0, maxWidth, maxHeight);

            int textureAtlasDatasCount = scissoredDates.size();

            for (int i = 0; i < textureAtlasDatasCount; i++) {
                GaiaTextureScissorDataFull scissorDataFull = scissoredDates.get(i);
                if (scissorDataFull == null) {
                    continue;
                }

                GaiaRectangle batchedBoundary = scissorDataFull.getBatchedBoundary();
                if (batchedBoundary == null) {
                    continue;
                }

                BufferedImage subImage = scissorDataFull.getScissoredImage();
                if (subImage == null) {
                    continue;
                }

                int x = (int) Math.floor(batchedBoundary.getMinX());
                int y = (int) Math.floor(batchedBoundary.getMinY());

                int w = subImage.getWidth();
                int h = subImage.getHeight();

                if (x < 0 || y < 0 || x + w > maxWidth || y + h > maxHeight) {
                    log.error(
                            "Scissor outside atlas. index={}, x={}, y={}, w={}, h={}, x+w={}, y+h={}, atlasW={}, atlasH={}",
                            i, x, y, w, h, x + w, y + h, maxWidth, maxHeight
                    );
                    continue;
                }

                g2d.drawImage(subImage, x, y, null);
            }
        } finally {
            g2d.dispose();
        }

        dilateBackgroundColor(textureAtlas.getBufferedImage(), backGroundColor);

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
