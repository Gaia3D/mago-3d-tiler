package com.gaia3d.util;

import lombok.extern.slf4j.Slf4j;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;

/**
 * Utility class for resizing images.
 */
@Slf4j
public class ImageResizer {
    public final static int MAX_TEXTURE_SIZE = 8192 * 2;
    public final static int MIN_TEXTURE_SIZE = 32;

    public BufferedImage resizeImageGraphic2D(BufferedImage originalImage, int width, int height) {
        return resizeImageGraphic2D(originalImage, width, height, false);
    }

    public BufferedImage gaussianBlur(BufferedImage original) {
        float[] kernel = {
                1 / 16f, 2 / 16f, 1 / 16f,
                2 / 16f, 4 / 16f, 2 / 16f,
                1 / 16f, 2 / 16f, 1 / 16f
        };

        Kernel k = new Kernel(3, 3, kernel);
        ConvolveOp op = new ConvolveOp(k, ConvolveOp.EDGE_NO_OP, null);
        return op.filter(original, null);
    }

    public BufferedImage resizeMultiStepSmart(
            BufferedImage original, int lod){
        int width = original.getWidth();
        int height = original.getHeight();

        int limiSize = 1024;
        if(lod > 0){
            limiSize = 512;
        }

        int widthPowerOf2 = ImageUtils.getNearestPowerOfTwo(width);
        int heightPowerOf2 = ImageUtils.getNearestPowerOfTwo(height);

        if(widthPowerOf2 > limiSize){
            widthPowerOf2  = limiSize;
        }

        if(heightPowerOf2 > limiSize){
            heightPowerOf2 = limiSize;
        }

        return resizeMultiStepSmart(original, widthPowerOf2, heightPowerOf2);
    }

    public BufferedImage resizeMultiStepSmart(
            BufferedImage original,
            int targetWidth,
            int targetHeight) {

        int type = original.getColorModel().hasAlpha() ?
                BufferedImage.TYPE_INT_ARGB :
                BufferedImage.TYPE_INT_RGB;

        BufferedImage current = original;
        int width = original.getWidth();
        int height = original.getHeight();

        // Redimensionar en pasos, reduciendo a 2/3 cada vez, hasta que el siguiente paso sea menor que el tamaño objetivo
        while ((2 * width / 3) >= targetWidth && (2 * height / 3) >= targetHeight) {

            width = 2 * width / 3;
            height = 2 * height / 3;

            BufferedImage tmp = new BufferedImage(width, height, type);
            Graphics2D g2 = tmp.createGraphics();

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING,
                    RenderingHints.VALUE_RENDER_QUALITY);

            g2.drawImage(current, 0, 0, width, height, null);
            g2.dispose();

            current = tmp;
        }

        // Último paso exacto al tamaño final
        if (width != targetWidth || height != targetHeight) {

            BufferedImage tmp = new BufferedImage(targetWidth, targetHeight, type);
            Graphics2D g2 = tmp.createGraphics();

            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                    RenderingHints.VALUE_INTERPOLATION_BICUBIC); // aquí sí puedes usar bicubic

            g2.drawImage(current, 0, 0, targetWidth, targetHeight, null);
            g2.dispose();

            current = tmp;
        }

        return current;
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

        int imageType = originalImage.getColorModel().hasAlpha()
                ? BufferedImage.TYPE_INT_ARGB
                : (originalImage.getType() == BufferedImage.TYPE_CUSTOM ? BufferedImage.TYPE_INT_RGB : originalImage.getType());
        BufferedImage outputImage = new BufferedImage(width, height, imageType);
        Graphics2D graphics2D = outputImage.createGraphics();
        if (interpolation) {
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            //graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            graphics2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics2D.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
            graphics2D.setRenderingHint(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_ENABLE);
            graphics2D.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
            graphics2D.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
            graphics2D.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        } else {
            graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
        }
        graphics2D.setComposite(AlphaComposite.Src);
        graphics2D.drawImage(originalImage, 0, 0, width, height, null);
        graphics2D.dispose();
        return outputImage;
    }
}
