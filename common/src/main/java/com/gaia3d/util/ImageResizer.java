package com.gaia3d.util;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Utility class for resizing images.
 */
public class ImageResizer {
    public final static int MAX_TEXTURE_SIZE = 8192;
    public final static int MIN_TEXTURE_SIZE = 32;

    public BufferedImage resizeImageGraphic2D(BufferedImage originalImage, int width, int height) {
        // check if the width and height are within the bounds.
        if (width < MIN_TEXTURE_SIZE) {
            width = MIN_TEXTURE_SIZE;
        } else if (width > MAX_TEXTURE_SIZE) {
            width = MAX_TEXTURE_SIZE;
        }
        if (height < MIN_TEXTURE_SIZE) {
            height = MIN_TEXTURE_SIZE;
        } else if (height > MAX_TEXTURE_SIZE) {
            height = MAX_TEXTURE_SIZE;
        }

        int imageType = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
        BufferedImage outputImage = new BufferedImage(width, height, imageType);
        Graphics2D graphics2D = outputImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); // RenderingHints.VALUE_INTERPOLATION_BILINEAR
        graphics2D.setComposite(AlphaComposite.Src);
        graphics2D.drawImage(originalImage, 0, 0, width, height, null);
        return outputImage;
    }
}
