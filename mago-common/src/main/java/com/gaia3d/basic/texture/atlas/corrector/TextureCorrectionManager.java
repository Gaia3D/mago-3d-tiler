package com.gaia3d.basic.texture.atlas.corrector;

import java.awt.image.BufferedImage;

public class TextureCorrectionManager {

    private static final double EPSILON = 1e-8;

    private long sampledPixels = 0;

    private double luminanceSum = 0.0;
    private double luminanceSqSum = 0.0;
    private double saturationSum = 0.0;

    private final long[] luminanceHistogram = new long[256];

    // Para acelerar. 1 = todos los píxeles, 2 = uno de cada 2, 4 = uno de cada 4...
    private int sampleStep = 4;

    public void setSampleStep(int sampleStep) {
        this.sampleStep = Math.max(1, sampleStep);
    }

    public void addStatistics(BufferedImage image) {
        if (image == null) {
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        for (int y = 0; y < height; y += sampleStep) {
            for (int x = 0; x < width; x += sampleStep) {
                int argb = image.getRGB(x, y);

                int alpha = (argb >>> 24) & 0xff;

                // Si la textura tiene alpha, ignoramos píxeles transparentes.
                // Para JPG normalmente alpha será 255.
                if (alpha < 16) {
                    continue;
                }

                int r = (argb >>> 16) & 0xff;
                int g = (argb >>> 8) & 0xff;
                int b = argb & 0xff;

                // Ignorar background negro puro del atlas.
                if (r == 0 && g == 0 && b == 0) {
                    continue;
                }

                double rd = r / 255.0;
                double gd = g / 255.0;
                double bd = b / 255.0;


                double luminance = calculateLuminance(rd, gd, bd);
                double saturation = calculateSaturation(rd, gd, bd);

                addPixelStatistics(luminance, saturation);
            }
        }
    }

    private void addPixelStatistics(double luminance, double saturation) {
        sampledPixels++;

        luminanceSum += luminance;
        luminanceSqSum += luminance * luminance;
        saturationSum += saturation;

        int index = (int) Math.round(luminance * 255.0);
        index = clamp(index, 0, 255);

        luminanceHistogram[index]++;
    }

    private double calculateLuminance(double r, double g, double b) {
        // Luminancia perceptual aproximada.
        return 0.2126 * r + 0.7152 * g + 0.0722 * b;
    }

    private double calculateSaturation(double r, double g, double b) {
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));

        if (max <= EPSILON) {
            return 0.0;
        }

        return (max - min) / max;
    }

    public TextureCorrectionParameters estimateCorrectionParams() {
        TextureCorrectionParameters params = new TextureCorrectionParameters();

        if (sampledPixels == 0) {
            params.gamma = 1.0;
            params.exposure = 1.0;
            params.contrast = 1.0;
            params.saturation = 1.0;
            return params;
        }

        double meanLum = getMeanLuminance();
        double stddevLum = getStdDevLuminance();
        double meanSat = getMeanSaturation();

        double p05 = getLuminancePercentile(0.05);
        double p50 = getLuminancePercentile(0.50);
        double p95 = getLuminancePercentile(0.95);

        /*
         * EXPOSURE
         *
         * La exposición se decide principalmente por la luminancia media,
         * pero se limita usando p95 para no quemar fachadas blancas, tejados
         * claros o carreteras muy iluminadas.
         *
         * Si p95 ya es alto, significa que ya hay zonas claras importantes,
         * así que usamos un targetMeanLum más bajo.
         */
        double targetMeanLum;

        if (p95 > 0.75) {
            // Escena ya bastante clara o con muchas altas luces.
            targetMeanLum = 0.32;
        } else if (p95 > 0.62) {
            // Escena intermedia, hay luces claras pero aún se puede levantar.
            targetMeanLum = 0.35;
        } else {
            // Escena oscura, se puede levantar más.
            targetMeanLum = 0.38;
        }

        params.exposure = targetMeanLum / Math.max(meanLum, EPSILON);

        /*
         * Protección de highlights.
         *
         * Queremos que el p95 corregido no se acerque demasiado a blanco puro.
         * 0.82 es conservador para realistic mesh con edificios blancos.
         */
        double highlightTarget = 0.82;
        double maxAllowedByHighlights = highlightTarget / Math.max(p95, EPSILON);

        params.exposure = Math.min(params.exposure, maxAllowedByHighlights);

        /*
         * Clamp general.
         *
         * 1.35 suele ser un máximo seguro. Si se sube más, las fachadas blancas
         * pueden perder textura aunque usemos protected exposure por píxel.
         */
        params.exposure = clamp(params.exposure, 0.85, 1.35);


        /*
         * GAMMA
         *
         * Gamma menor que 1.0 levanta medios tonos.
         * Usamos p50 porque representa mejor los tonos medios que la media,
         * especialmente cuando hay sombras, árboles y edificios blancos mezclados.
         */
        if (p50 < 0.12) {
            params.gamma = 0.84;
        } else if (p50 < 0.25) {
            params.gamma = 0.88;
        } else if (p50 < 0.38) {
            params.gamma = 0.94;
        } else {
            params.gamma = 1.00;
        }


        /*
         * CONTRAST
         *
         * Usamos stddevLum para estimar si la imagen está plana.
         * En vez de usar reglas fijas por tile, intentamos llevar el contraste
         * hacia un valor objetivo moderado.
         */
        double desiredStdDevLum = 0.19;

        params.contrast = desiredStdDevLum / Math.max(stddevLum, EPSILON);
        params.contrast = clamp(params.contrast, 1.00, 1.10);

        /*
         * Si hay muchas sombras profundas, no conviene endurecer mucho,
         * porque podríamos convertir vegetación y calles oscuras en manchas negras.
         */
        if (p05 < 0.04) {
            params.contrast = Math.min(params.contrast, 1.06);
        }

        /*
         * Si las altas luces ya son fuertes, moderamos el contraste.
         */
        if (p95 > 0.75) {
            params.contrast = Math.min(params.contrast, 1.04);
        }


        /*
         * SATURATION
         *
         * Las fotos nubladas suelen estar algo desaturadas.
         * Subimos suavemente, pero sin exagerar para no volver artificiales
         * tejados azules, vegetación o carreteras.
         */
        if (meanSat < 0.18) {
            params.saturation = 1.08;
        } else if (meanSat < 0.25) {
            params.saturation = 1.05;
        } else if (meanSat < 0.35) {
            params.saturation = 1.02;
        } else {
            params.saturation = 1.00;
        }

        return params;
    }

    private double smoothstep(double edge0, double edge1, double x) {
        double t = clamp((x - edge0) / (edge1 - edge0), 0.0, 1.0);
        return t * t * (3.0 - 2.0 * t);
    }

    private double applyProtectedExposure(double value, double exposure, double protectionFactor) {
        double effectiveExposure = 1.0 + (exposure - 1.0) * protectionFactor;
        return value * effectiveExposure;
    }

    public BufferedImage correctImage(BufferedImage source, TextureCorrectionParameters params) {
        if (source == null) {
            return null;
        }

        if (params == null) {
            params = new TextureCorrectionParameters();
        }

        int width = source.getWidth();
        int height = source.getHeight();

        boolean hasAlpha = source.getColorModel().hasAlpha();

        BufferedImage result = new BufferedImage(
                width,
                height,
                hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB
        );

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int argb = source.getRGB(x, y);

                int a = (argb >>> 24) & 0xff;
                int r = (argb >>> 16) & 0xff;
                int g = (argb >>> 8) & 0xff;
                int b = argb & 0xff;

                // Mantener background negro puro intacto.
                if (r == 0 && g == 0 && b == 0) {
                    result.setRGB(x, y, argb);
                    continue;
                }

                double rd = r / 255.0;
                double gd = g / 255.0;
                double bd = b / 255.0;

                // 1. Gamma. Gamma < 1.0 aclara medios tonos.
                rd = Math.pow(rd, params.gamma);
                gd = Math.pow(gd, params.gamma);
                bd = Math.pow(bd, params.gamma);

                // 2. Exposure protegido para no quemar fachadas blancas.
                double lumBeforeExposure = calculateLuminance(rd, gd, bd);

                /*
                 * protectionFactor:
                 * - Cerca de 1.0 en sombras y medios tonos.
                 * - Cerca de 0.0 en altas luces.
                 *
                 * Esto hace que la exposure afecte mucho a vegetación/calles/sombras,
                 * pero muy poco a edificios ya claros.
                 */
                double protectionFactor = 1.0 - smoothstep(0.50, 0.82, lumBeforeExposure);

                rd = applyProtectedExposure(rd, params.exposure, protectionFactor);
                gd = applyProtectedExposure(gd, params.exposure, protectionFactor);
                bd = applyProtectedExposure(bd, params.exposure, protectionFactor);

                // 3. Contrast alrededor de 0.5.
                rd = (rd - 0.5) * params.contrast + 0.5;
                gd = (gd - 0.5) * params.contrast + 0.5;
                bd = (bd - 0.5) * params.contrast + 0.5;

                // 4. Saturation.
                double lum = calculateLuminance(rd, gd, bd);

                rd = lum + (rd - lum) * params.saturation;
                gd = lum + (gd - lum) * params.saturation;
                bd = lum + (bd - lum) * params.saturation;

                int rr = clampToByte(rd);
                int gg = clampToByte(gd);
                int bb = clampToByte(bd);

                int correctedArgb;

                if (hasAlpha) {
                    correctedArgb = (a << 24) | (rr << 16) | (gg << 8) | bb;
                } else {
                    correctedArgb = (rr << 16) | (gg << 8) | bb;
                }

                result.setRGB(x, y, correctedArgb);
            }
        }

        return result;
    }

    public double getMeanLuminance() {
        if (sampledPixels == 0) {
            return 0.0;
        }

        return luminanceSum / sampledPixels;
    }

    public double getStdDevLuminance() {
        if (sampledPixels == 0) {
            return 0.0;
        }

        double mean = getMeanLuminance();
        double variance = luminanceSqSum / sampledPixels - mean * mean;

        return Math.sqrt(Math.max(0.0, variance));
    }

    public double getMeanSaturation() {
        if (sampledPixels == 0) {
            return 0.0;
        }

        return saturationSum / sampledPixels;
    }

    public double getLuminancePercentile(double percentile) {
        if (sampledPixels == 0) {
            return 0.0;
        }

        percentile = clamp(percentile, 0.0, 1.0);

        long target = (long) Math.ceil(sampledPixels * percentile);
        long accumulated = 0;

        for (int i = 0; i < luminanceHistogram.length; i++) {
            accumulated += luminanceHistogram[i];

            if (accumulated >= target) {
                return i / 255.0;
            }
        }

        return 1.0;
    }

    public long getSampledPixels() {
        return sampledPixels;
    }

    public void clear() {
        sampledPixels = 0;

        luminanceSum = 0.0;
        luminanceSqSum = 0.0;
        saturationSum = 0.0;

        for (int i = 0; i < luminanceHistogram.length; i++) {
            luminanceHistogram[i] = 0;
        }
    }

    private int clampToByte(double value) {
        value = clamp(value, 0.0, 1.0);
        return (int) Math.round(value * 255.0);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}