package com.gaia3d.util;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Utility class for resizing images.
 */
@Slf4j
public class ImageResizer {
    public final static int MAX_TEXTURE_SIZE = 8192 * 2;
    public final static int MIN_TEXTURE_SIZE = 128;

    public BufferedImage resizeImageGraphic2D(BufferedImage originalImage, int width, int height) {
        return resizeImageGraphic2D(originalImage, width, height, false);
    }

    public BufferedImage resizeImageGraphic2D(BufferedImage originalImage, int width, int height, boolean interpolation) {
        if (width == originalImage.getWidth() && height == originalImage.getHeight()) {
            return originalImage;
        }

        // check if the width and height are within the bounds.
        if (width < MIN_TEXTURE_SIZE) {
            width = MIN_TEXTURE_SIZE;
            log.debug("width is less than {}", MIN_TEXTURE_SIZE);
        } else if (width > MAX_TEXTURE_SIZE) {
            width = MAX_TEXTURE_SIZE;
            log.debug("width is greater than {}", MAX_TEXTURE_SIZE);
        }
        if (height < MIN_TEXTURE_SIZE) {
            height = MIN_TEXTURE_SIZE;
            log.debug("height is less than {}", MIN_TEXTURE_SIZE);
        } else if (height > MAX_TEXTURE_SIZE) {
            height = MAX_TEXTURE_SIZE;
            log.debug("height is greater than {}", MAX_TEXTURE_SIZE);
        }


        int imageType = originalImage.getType() == 0 ? BufferedImage.TYPE_INT_ARGB : originalImage.getType();
        BufferedImage outputImage = new BufferedImage(width, height, imageType);
        Graphics2D graphics2D = outputImage.createGraphics();
        if (interpolation) {
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        } else {
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        }
        //graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        //graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.setComposite(AlphaComposite.Src);
        graphics2D.drawImage(originalImage, 0, 0, width, height, null);
        return outputImage;
    }
}
