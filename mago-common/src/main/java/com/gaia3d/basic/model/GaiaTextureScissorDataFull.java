package com.gaia3d.basic.model;

import com.gaia3d.basic.geometry.GaiaRectangle;
import com.gaia3d.basic.halfedge.HalfEdgeFace;
import com.gaia3d.basic.halfedge.HalfEdgeVertex;
import com.gaia3d.basic.halfedge.ObjectStatus;
import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2d;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Setter
@Getter
public class GaiaTextureScissorDataFull extends GaiaTextureScissorData {
    private int classifyId = -1;
    private BufferedImage scissoredImage;
    private int motherImageWidth;
    private int motherImageHeight;

    public void recalculateTexCoordsForAtlas(int atlasWidth, int atlasHeight) {
        if (faces == null || faces.isEmpty()) {
            return;
        }

        if (noExpandedBoundary == null || batchedBoundary == null) {
            return;
        }

        if (motherImageWidth <= 0 || motherImageHeight <= 0) {
            return;
        }

        int expand = Math.max(this.expandedPixel, 0);

        double srcMinX = noExpandedBoundary.getMinX();
        double srcMinY = noExpandedBoundary.getMinY();

        double dstMinX = batchedBoundary.getMinX();
        double dstMinY = batchedBoundary.getMinY();

        List<HalfEdgeVertex> vertices = new ArrayList<>();
        Set<HalfEdgeVertex> visitedVertices = new java.util.HashSet<>();

        for (HalfEdgeFace face : faces) {
            if (face == null || face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }

            vertices.clear();
            face.getVertices(vertices);

            for (HalfEdgeVertex vertex : vertices) {
                if(visitedVertices.contains(vertex)) {
                    continue;
                }
                visitedVertices.add(vertex);
                Vector2d texCoord = vertex.getTexcoords();

                if (texCoord == null) {
                    continue;
                }

                double oldU = texCoord.x;
                double oldV = texCoord.y;

                // UV original -> píxel en la textura madre
                double oldPixelX = oldU * motherImageWidth;
                double oldPixelY = oldV * motherImageHeight;

                // píxel madre -> píxel local dentro del scissor SIN expandir
                double localX = oldPixelX - srcMinX;
                double localY = oldPixelY - srcMinY;

                // píxel local -> píxel dentro del atlas
                double atlasPixelX = dstMinX + expand + localX;
                double atlasPixelY = dstMinY + expand + localY;

                // píxel atlas -> UV final
                texCoord.x = atlasPixelX / atlasWidth;
                texCoord.y = atlasPixelY / atlasHeight;
            }
        }
    }

    private GaiaRectangle getTexCoordBoundingRectangle(List<HalfEdgeFace> faces, boolean invertTexCoordY, GaiaRectangle resultTexCoordBRect) {
        if(resultTexCoordBRect == null) {
            resultTexCoordBRect = new GaiaRectangle();
        }
        boolean texCoordBBoxStarted = false;
        List<HalfEdgeVertex> memSaveVertices = new ArrayList<>();
        int facesCount = faces.size();
        GaiaRectangle faceTexCoordBRect = new GaiaRectangle();
        for (int i = 0; i < facesCount; i++) {
            HalfEdgeFace face = faces.get(i);
            if (face.getStatus() == ObjectStatus.DELETED) {
                continue;
            }
            memSaveVertices.clear();
            faceTexCoordBRect = face.getTexCoordBoundingRectangle(faceTexCoordBRect, invertTexCoordY, memSaveVertices);

            if (!texCoordBBoxStarted) {
                resultTexCoordBRect.copyFrom(faceTexCoordBRect);
                texCoordBBoxStarted = true;
            } else {
                resultTexCoordBRect.addBoundingRectangle(faceTexCoordBRect);
            }
        }

        return resultTexCoordBRect;
    }

    public void takeScissoredImageFromMotherImage(BufferedImage motherImage) {
        if (motherImage == null || faces == null || faces.isEmpty()) {
            scissoredImage = null;
            return;
        }

        this.motherImageWidth = motherImage.getWidth();
        this.motherImageHeight = motherImage.getHeight();

        boolean invertTexCoordY = false;
        GaiaRectangle texCoordBRect = getTexCoordBoundingRectangle(faces, invertTexCoordY, null);

        if (texCoordBRect == null) {
            scissoredImage = null;
            return;
        }

        int imageWidth = motherImage.getWidth();
        int imageHeight = motherImage.getHeight();

        double minU = clamp(texCoordBRect.getMinX(), 0.0, 1.0);
        double minV = clamp(texCoordBRect.getMinY(), 0.0, 1.0);
        double maxU = clamp(texCoordBRect.getMaxX(), 0.0, 1.0);
        double maxV = clamp(texCoordBRect.getMaxY(), 0.0, 1.0);

        if (maxU < minU || maxV < minV) {
            scissoredImage = null;
            return;
        }

        int x0 = (int) Math.floor(minU * imageWidth);
        int y0 = (int) Math.floor(minV * imageHeight);
        int x1 = (int) Math.ceil(maxU * imageWidth);
        int y1 = (int) Math.ceil(maxV * imageHeight);

        x0 = clamp(x0, 0, imageWidth - 1);
        y0 = clamp(y0, 0, imageHeight - 1);
        x1 = clamp(x1, x0 + 1, imageWidth);
        y1 = clamp(y1, y0 + 1, imageHeight);

        int scissorWidth = x1 - x0;
        int scissorHeight = y1 - y0;

        this.texCoordBoundary = texCoordBRect;
        this.noExpandedBoundary = new GaiaRectangle(x0, y0, x1, y1);

        this.originBoundary = new GaiaRectangle(
                0,
                0,
                scissorWidth,
                scissorHeight
        );

        this.currentBoundary = new GaiaRectangle(
                0,
                0,
                scissorWidth,
                scissorHeight
        );

        BufferedImage subImage = motherImage.getSubimage(
                x0,
                y0,
                scissorWidth,
                scissorHeight
        );

        BufferedImage copy = new BufferedImage(
                scissorWidth,
                scissorHeight,
                getSafeImageType(motherImage)
        );

        Graphics2D g = copy.createGraphics();
        g.drawImage(subImage, 0, 0, null);
        g.dispose();

        this.scissoredImage = copy;
    }

    private static double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private static int getSafeImageType(BufferedImage image) {
        int type = image.getType();

        if (type == BufferedImage.TYPE_CUSTOM) {
            return BufferedImage.TYPE_INT_ARGB;
        }

        return type;
    }

    public void expandScissorImage(int expandPixels) {
        if (scissoredImage == null) {
            return;
        }

        if (expandPixels <= 0) {
            return;
        }

        int oldWidth = scissoredImage.getWidth();
        int oldHeight = scissoredImage.getHeight();

        if (oldWidth <= 0 || oldHeight <= 0) {
            return;
        }

        int newWidth = oldWidth + expandPixels * 2;
        int newHeight = oldHeight + expandPixels * 2;

        BufferedImage expandedImage = new BufferedImage(
                newWidth,
                newHeight,
                getSafeImageType(scissoredImage)
        );

        // Copy pixels using nearest-border clamp.
        for (int y = 0; y < newHeight; y++) {
            int srcY = clamp(y - expandPixels, 0, oldHeight - 1);

            for (int x = 0; x < newWidth; x++) {
                int srcX = clamp(x - expandPixels, 0, oldWidth - 1);

                int argb = scissoredImage.getRGB(srcX, srcY);
                expandedImage.setRGB(x, y, argb);
            }
        }

        this.scissoredImage = expandedImage;
        this.expandedPixel += expandPixels;

        this.originBoundary = new GaiaRectangle(
                0,
                0,
                expandedImage.getWidth(),
                expandedImage.getHeight()
        );

        this.currentBoundary = new GaiaRectangle(
                0,
                0,
                expandedImage.getWidth(),
                expandedImage.getHeight()
        );
    }

    public void clear() {
        if(scissoredImage != null) {
            scissoredImage.flush();
        }
        scissoredImage = null;
        motherImageWidth = 0;
        motherImageHeight = 0;
    }
}
