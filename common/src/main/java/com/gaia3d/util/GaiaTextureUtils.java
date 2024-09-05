package com.gaia3d.util;

import com.gaia3d.basic.structure.GaiaTexture;
import org.joml.Vector2d;
import org.joml.Vector4d;

import java.awt.image.BufferedImage;

public class GaiaTextureUtils {
    public static Vector2d getTexCoordPositiveQuadrant(Vector2d texCoord) {
        Vector2d positiveQuadrantTexCoord = new Vector2d(texCoord);

        if (positiveQuadrantTexCoord.x < 0) {
            // is repeatMode, so, recalculate the col.***
            while(positiveQuadrantTexCoord.x < 0) {
                positiveQuadrantTexCoord.x += 1.0;
            }
        }
        else if (positiveQuadrantTexCoord.x > 1.0) {
            // is repeatMode, so, recalculate the col.***
            while(positiveQuadrantTexCoord.x > 1.0) {
                positiveQuadrantTexCoord.x -= 1.0;
            }
        }

        if (positiveQuadrantTexCoord.y < 0) {
            // is repeatMode, so, recalculate the row.***
            while(positiveQuadrantTexCoord.y < 0) {
                positiveQuadrantTexCoord.y += 1.0;
            }
        }
        else if (positiveQuadrantTexCoord.y > 1.0) {
            // is repeatMode, so, recalculate the row.***
            while(positiveQuadrantTexCoord.y > 1.0) {
                positiveQuadrantTexCoord.y -= 1.0;
            }
        }

        return positiveQuadrantTexCoord;
    }
    public static Vector4d getColorOfTexture(GaiaTexture texture, Vector2d texCoord) {
        Vector4d color = new Vector4d(1.0, 1.0, 1.0, 1.0);
        if (texture == null) {
            return null;
        }

        if (texture.getBufferedImage() == null) {
            return null;
        }

        Vector2d correctedTexCoord = getTexCoordPositiveQuadrant(texCoord);

        BufferedImage bufferedImage = texture.getBufferedImage();
        int texWidth = bufferedImage.getWidth();
        int texHeight = bufferedImage.getHeight();
        int col = (int) (correctedTexCoord.x * texWidth);
        int row = (int) (correctedTexCoord.y * texHeight);

        if (col < 0) {
            col = 0;
        }
        else if (col >= texWidth) {
            col = texWidth;
        }

        if (row < 0) {
            row = 0;
        }
        else if (row >= texHeight) {
            row = texHeight;
        }

        int rgb = bufferedImage.getRGB(col, row);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        color.set(red / 255.0, green / 255.0, blue / 255.0, 1.0);

        // alpha channel
        if (bufferedImage.getColorModel().hasAlpha()) {
            int alpha = (rgb >> 24) & 0xFF;
            color.w = alpha / 255.0;
        }

        return color;
    }

    public static Vector4d getAverageColorOfTexture(GaiaTexture texture, Vector2d texCoord0, Vector2d texCoord1, Vector2d texCoord2)
    {
        // calculate the rectangle.***
        Vector2d minTexCoord = new Vector2d();
        Vector2d maxTexCoord = new Vector2d();

        texCoord0 = getTexCoordPositiveQuadrant(texCoord0);
        texCoord1 = getTexCoordPositiveQuadrant(texCoord1);
        texCoord2 = getTexCoordPositiveQuadrant(texCoord2);

        minTexCoord.set(texCoord0);
        maxTexCoord.set(texCoord0);
        if (texCoord1.x < minTexCoord.x) minTexCoord.x = texCoord1.x;
        if (texCoord1.y < minTexCoord.y) minTexCoord.y = texCoord1.y;
        if (texCoord1.x > maxTexCoord.x) maxTexCoord.x = texCoord1.x;
        if (texCoord1.y > maxTexCoord.y) maxTexCoord.y = texCoord1.y;
        if (texCoord2.x < minTexCoord.x) minTexCoord.x = texCoord2.x;
        if (texCoord2.y < minTexCoord.y) minTexCoord.y = texCoord2.y;
        if (texCoord2.x > maxTexCoord.x) maxTexCoord.x = texCoord2.x;
        if (texCoord2.y > maxTexCoord.y) maxTexCoord.y = texCoord2.y;

        Vector4d averageColor = new Vector4d();

        if (texture == null) {
            return averageColor;
        }

        if (texture.getBufferedImage() == null) {
            return averageColor;
        }

        BufferedImage bufferedImage = texture.getBufferedImage();
        int texWidth = bufferedImage.getWidth();
        int texHeight = bufferedImage.getHeight();

        int minCol = (int) (minTexCoord.x * texWidth);
        int minRow = (int) (minTexCoord.y * texHeight);
        int maxCol = (int) (maxTexCoord.x * texWidth);
        int maxRow = (int) (maxTexCoord.y * texHeight);

        if (minCol < 0) {
            minCol = 0;
        }
        else if (minCol >= texWidth) {
            minCol = texWidth-1;
        }

        if (minRow < 0) {
            minRow = 0;
        }
        else if (minRow >= texHeight) {
            minRow = texHeight-1;
        }

        if (maxCol < 0) {
            maxCol = 0;
        }
        else if (maxCol >= texWidth) {
            maxCol = texWidth-1;
        }

        if (maxRow < 0) {
            maxRow = 0;
        }
        else if (maxRow >= texHeight) {
            maxRow = texHeight-1;
        }

        int red = 0;
        int green = 0;
        int blue = 0;
        int alpha = 0;

        for (int row = minRow; row <= maxRow; row++) {
            for (int col = minCol; col <= maxCol; col++) {
                int rgb = bufferedImage.getRGB(col, row);
                red += (rgb >> 16) & 0xFF;
                green += (rgb >> 8) & 0xFF;
                blue += rgb & 0xFF;
                if (bufferedImage.getColorModel().hasAlpha()) {
                    alpha += (rgb >> 24) & 0xFF;
                }
                else {
                    alpha += 255;
                }
            }
        }

        int count = (maxRow - minRow + 1) * (maxCol - minCol + 1);
        averageColor.set(red / (count * 255.0), green / (count * 255.0), blue / (count * 255.0), alpha / (count * 255.0));
        return averageColor;
    }
}
