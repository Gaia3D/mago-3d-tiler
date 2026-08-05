package com.gaia3d.basic.texture.atlas.corrector;

import java.awt.image.BufferedImage;

public class TextureColorCorrector {
    public static BufferedImage correct(BufferedImage src, double gamma, double exposure, double contrast, double saturation) {
        int w = src.getWidth();
        int h = src.getHeight();

        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int argb = src.getRGB(x, y);

                int r = (argb >> 16) & 0xff;
                int g = (argb >> 8) & 0xff;
                int b = argb & 0xff;

                double rd = r / 255.0;
                double gd = g / 255.0;
                double bd = b / 255.0;

                // gamma: < 1.0 aclara medios tonos
                rd = Math.pow(rd, gamma);
                gd = Math.pow(gd, gamma);
                bd = Math.pow(bd, gamma);

                // exposure
                rd *= exposure;
                gd *= exposure;
                bd *= exposure;

                // contrast around 0.5
                rd = (rd - 0.5) * contrast + 0.5;
                gd = (gd - 0.5) * contrast + 0.5;
                bd = (bd - 0.5) * contrast + 0.5;

                // saturation
                double lum = 0.2126 * rd + 0.7152 * gd + 0.0722 * bd;
                rd = lum + (rd - lum) * saturation;
                gd = lum + (gd - lum) * saturation;
                bd = lum + (bd - lum) * saturation;

                int rr = clampToByte(rd);
                int gg = clampToByte(gd);
                int bb = clampToByte(bd);

                int rgb = (rr << 16) | (gg << 8) | bb;
                dst.setRGB(x, y, rgb);
            }
        }

        return dst;
    }

    private static int clampToByte(double v) {
        if (v < 0.0) {return 0;}
        if (v > 1.0) {return 255;}
        return (int) (v * 255.0 + 0.5);
    }
}
