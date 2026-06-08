package com.gaia3d.basic.texture.atlas.corrector;

public class TextureStatistics {
    public long sampledPixels;
    public double luminanceSum;
    public double luminanceSqSum;
    public double saturationSum;
    public long[] luminanceHistogram = new long[256];

    public void accumulate(double lum, double sat) {
        sampledPixels++;
        luminanceSum += lum;
        luminanceSqSum += lum * lum;
        saturationSum += sat;

        int idx = (int)(lum * 255.0);
        if (idx < 0) idx = 0;
        if (idx > 255) idx = 255;
        luminanceHistogram[idx]++;
    }

    public double getMeanLuminance() {
        return sampledPixels == 0 ? 0.0 : luminanceSum / sampledPixels;
    }

    public double getStdDevLuminance() {
        if (sampledPixels == 0) return 0.0;
        double mean = luminanceSum / sampledPixels;
        double var = luminanceSqSum / sampledPixels - mean * mean;
        return Math.sqrt(Math.max(0.0, var));
    }

    public double getMeanSaturation() {
        return sampledPixels == 0 ? 0.0 : saturationSum / sampledPixels;
    }
}
