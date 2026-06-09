package com.gaia3d.basic.texture.atlas.corrector;

import org.joml.Vector2d;

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

    public void addStatisticsByUvTriangle(
            BufferedImage image,
            Vector2d uv0,
            Vector2d uv1,
            Vector2d uv2
    ) {
        if (image == null || uv0 == null || uv1 == null || uv2 == null) {
            return;
        }

        int width = image.getWidth();
        int height = image.getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        double areaUv = calculateUvTriangleArea(uv0, uv1, uv2);

        if (areaUv <= EPSILON) {
            return;
        }

        /*
         * Número de samples según área UV.
         * Si el triángulo ocupa mucha textura, tomamos más muestras.
         */
        double pixelArea = areaUv * width * height;

        int minSamples = pixelArea < 4.0 ? 1 : 4;

        int samples = (int) Math.ceil(pixelArea / 16.0);
        samples = clamp(samples, minSamples, 256);

        int grid = (int) Math.ceil(Math.sqrt(samples));

        for (int iy = 0; iy < grid; iy++) {
            for (int ix = 0; ix < grid; ix++) {
                /*
                 * Coordenadas baricéntricas simples.
                 * Generamos puntos dentro del triángulo.
                 */
                double a = (ix + 0.5) / grid;
                double b = (iy + 0.5) / grid;

                if (a + b > 1.0) {
                    a = 1.0 - a;
                    b = 1.0 - b;
                }

                double w0 = 1.0 - a - b;
                double w1 = a;
                double w2 = b;

                double u = uv0.x * w0 + uv1.x * w1 + uv2.x * w2;
                double v = uv0.y * w0 + uv1.y * w1 + uv2.y * w2;

                addStatisticsByUv(image, u, v);
            }
        }
    }

    private double calculateUvTriangleArea(Vector2d uv0, Vector2d uv1, Vector2d uv2) {
        double x1 = uv1.x - uv0.x;
        double y1 = uv1.y - uv0.y;

        double x2 = uv2.x - uv0.x;
        double y2 = uv2.y - uv0.y;

        return Math.abs(x1 * y2 - y1 * x2) * 0.5;
    }

    private void addStatisticsByUv(BufferedImage image, double u, double v) {
        int width = image.getWidth();
        int height = image.getHeight();

        /*
         * Para atlas normalmente queremos clamp.
         * Si tus UVs pueden usar repeat, habría que cambiar esto.
         */
        u = clamp(u, 0.0, 1.0);
        v = clamp(v, 0.0, 1.0);

        int x = (int) Math.floor(u * (width - 1));

        /*
         * Ojo con el eje Y.
         * Si tus UVs ya están invertidas, usa directamente:
         * int y = (int)Math.floor(v * (height - 1));
         */
        int y = (int) Math.floor((1.0 - v) * (height - 1));

        x = clamp(x, 0, width - 1);
        y = clamp(y, 0, height - 1);

        int argb = image.getRGB(x, y);

        int alpha = (argb >>> 24) & 0xff;

        if (alpha < 16) {
            return;
        }

        int r = (argb >>> 16) & 0xff;
        int g = (argb >>> 8) & 0xff;
        int b = argb & 0xff;

        // Background negro puro.
        if (r == 0 && g == 0 && b == 0) {
            return;
        }

        double rd = r / 255.0;
        double gd = g / 255.0;
        double bd = b / 255.0;

        double luminance = calculateLuminance(rd, gd, bd);
        double saturation = calculateSaturation(rd, gd, bd);

        addPixelStatistics(luminance, saturation);
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
         * BLACK POINT / DEHAZE SUAVE
         *
         * Si p05 está alto, las sombras están levantadas.
         * Eso suele indicar neblina, velo gris o imagen lavada.
         */
        if (p05 > 0.10 && meanSat < 0.35) {
            params.blackPoint = p05 * 0.85;
        } else if (p05 > 0.07 && meanSat < 0.32) {
            params.blackPoint = p05 * 0.75;
        } else if (p05 > 0.05 && meanSat < 0.28) {
            params.blackPoint = p05 * 0.60;
        } else if (p05 > 0.04 && meanSat < 0.25) {
            params.blackPoint = p05 * 0.45;
        } else {
            params.blackPoint = 0.0;
        }

        /*
         * Si la imagen ya tiene mucho contraste, suavizamos el blackPoint.
         * Pero no lo anulamos completamente, porque una imagen puede tener
         * contraste y aun así tener velo gris.
         */
        if (stddevLum > 0.28) {
            params.blackPoint *= 0.50;
        } else if (stddevLum > 0.24) {
            params.blackPoint *= 0.75;
        }

        params.blackPoint = clamp(params.blackPoint, 0.0, 0.10);
        params.whitePoint = 1.0;

        /*
         * Haze detector:
         * tonos oscuros levantados + poca saturación + medios tonos claros.
         */
        boolean hazyImage =
                p05 > 0.05 &&
                        meanSat < 0.30 &&
                        p50 > 0.30;

        if (hazyImage) {
            params.contrast = Math.max(params.contrast, 1.08);
            params.saturation = Math.max(params.saturation, 1.07);
        }

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
        double desiredStdDevLum = 0.205;

        params.contrast = desiredStdDevLum / Math.max(stddevLum, EPSILON);
        params.contrast = clamp(params.contrast, 1.00, 1.12);

        /*
         * Si hay muchas sombras profundas, no conviene endurecer mucho,
         * porque podríamos convertir vegetación y calles oscuras en manchas negras.
         */
        if (p05 < 0.04) {
            params.contrast = Math.min(params.contrast, 1.08);
        }

        /*
         * Si las altas luces ya son fuertes, moderamos el contraste.
         */
        if (p95 > 0.75) {
            params.contrast = Math.min(params.contrast, 1.05);
        }


        /*
         * SATURATION
         *
         * Las fotos nubladas suelen estar algo desaturadas.
         * Subimos suavemente, pero sin exagerar para no volver artificiales
         * tejados azules, vegetación o carreteras.
         */
        if (meanSat < 0.18) {
            params.saturation = 1.10;
        } else if (meanSat < 0.25) {
            params.saturation = 1.07;
        } else if (meanSat < 0.35) {
            params.saturation = 1.03;
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

                // 0. Dehaze suave.
                if (params.blackPoint > 0.0) {
                    rd = applyLevels(rd, params.blackPoint, params.whitePoint);
                    gd = applyLevels(gd, params.blackPoint, params.whitePoint);
                    bd = applyLevels(bd, params.blackPoint, params.whitePoint);
                }

                // 1. Gamma.
                rd = Math.pow(rd, params.gamma);
                gd = Math.pow(gd, params.gamma);
                bd = Math.pow(bd, params.gamma);

                // 2. Exposure protegido.
                double lumBeforeExposure = calculateLuminance(rd, gd, bd);
                double protectionFactor = 1.0 - smoothstep(0.50, 0.82, lumBeforeExposure);

                rd = applyProtectedExposure(rd, params.exposure, protectionFactor);
                gd = applyProtectedExposure(gd, params.exposure, protectionFactor);
                bd = applyProtectedExposure(bd, params.exposure, protectionFactor);

                // 3. Contrast.
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

    private double applyLevels(double value, double blackPoint, double whitePoint) {
        double denominator = Math.max(whitePoint - blackPoint, EPSILON);
        return clamp((value - blackPoint) / denominator, 0.0, 1.0);
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