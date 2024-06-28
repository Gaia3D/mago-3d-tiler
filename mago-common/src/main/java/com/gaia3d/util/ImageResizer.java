package com.gaia3d.util;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Utility class for resizing images.
 * @author znkim
 * @since 1.0.0
 */
public class ImageResizer {
    public BufferedImage resizeImageGraphic2D(BufferedImage originalImage, int width, int height) {
        BufferedImage outputImage = new BufferedImage(width, height, originalImage.getType());
        Graphics2D graphics2D = outputImage.createGraphics();
        graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR); // RenderingHints.VALUE_INTERPOLATION_BILINEAR
        graphics2D.setComposite(AlphaComposite.Src);
        graphics2D.drawImage(originalImage, 0, 0, width, height, null);
        return outputImage;
    }
}
