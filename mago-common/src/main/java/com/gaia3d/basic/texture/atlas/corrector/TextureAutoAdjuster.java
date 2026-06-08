package com.gaia3d.basic.texture.atlas.corrector;

public class TextureAutoAdjuster {

    public TextureCorrectionParameters estimate(TextureStatistics stats) {
        TextureCorrectionParameters p = new TextureCorrectionParameters();

        double meanLum = stats.getMeanLuminance();
        double stddev = stats.getStdDevLuminance();
        double meanSat = stats.getMeanSaturation();

        double targetMean = 0.42;
        p.exposure = clamp(targetMean / Math.max(meanLum, 1e-6), 0.9, 1.6);

        if (meanLum < 0.26) p.gamma = 0.80;
        else if (meanLum < 0.33) p.gamma = 0.86;
        else if (meanLum < 0.40) p.gamma = 0.93;
        else p.gamma = 1.0;

        if (stddev < 0.10) p.contrast = 1.15;
        else if (stddev < 0.14) p.contrast = 1.10;
        else if (stddev < 0.18) p.contrast = 1.05;
        else p.contrast = 1.0;

        if (meanSat < 0.20) p.saturation = 1.10;
        else if (meanSat < 0.28) p.saturation = 1.06;
        else p.saturation = 1.02;

        return p;
    }

    private double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
