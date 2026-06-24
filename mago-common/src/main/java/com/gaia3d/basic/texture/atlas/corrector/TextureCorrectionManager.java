package com.gaia3d.basic.texture.atlas.corrector;

import org.joml.Vector2d;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;

public class TextureCorrectionManager {

    private static final double EPSILON = 1e-8;

    private long sampledPixels = 0;

    private double luminanceSum = 0.0;
    private double luminanceSqSum = 0.0;
    private double saturationSum = 0.0;

    private final long[] luminanceHistogram = new long[256];

    // Para acelerar. 1 = todos los píxeles, 2 = uno de cada 2, 4 = uno de cada 4...
    private int sampleStep = 4;

    public record TexturePixelData(
            int[] pixels,
            int width,
            int height,
            boolean hasAlpha
    ) {
    }

    public void mergeFrom(
            TextureCorrectionManager other
    ) {
        if (other == null
                || other.sampledPixels == 0) {
            return;
        }

        this.sampledPixels +=
                other.sampledPixels;

        this.luminanceSum +=
                other.luminanceSum;

        this.luminanceSqSum +=
                other.luminanceSqSum;

        this.saturationSum +=
                other.saturationSum;

        int histogramSize = Math.min(
                this.luminanceHistogram.length,
                other.luminanceHistogram.length
        );

        for (int i = 0; i < histogramSize; i++) {
            this.luminanceHistogram[i] +=
                    other.luminanceHistogram[i];
        }
    }

    public static TexturePixelData createPixelData(
            BufferedImage image
    ) {
        if (image == null) {
            return null;
        }

        int width = image.getWidth();
        int height = image.getHeight();
        boolean hasAlpha = image.getColorModel().hasAlpha();

        Raster raster = image.getRaster();
        int imageType = image.getType();

        boolean isIntImage =
                imageType == BufferedImage.TYPE_INT_RGB
                        || imageType == BufferedImage.TYPE_INT_ARGB
                        || imageType == BufferedImage.TYPE_INT_ARGB_PRE;

        if (isIntImage
                && raster.getDataBuffer() instanceof DataBufferInt dataBuffer
                && raster.getSampleModel()
                instanceof SinglePixelPackedSampleModel sampleModel
                && sampleModel.getScanlineStride() == width
                && raster.getSampleModelTranslateX() == 0
                && raster.getSampleModelTranslateY() == 0
                && dataBuffer.getOffset() == 0
                && dataBuffer.getData().length >= width * height) {

            /*
             * Acceso directo, sin copiar la imagen.
             */
            return new TexturePixelData(
                    dataBuffer.getData(),
                    width,
                    height,
                    hasAlpha
            );
        }

        /*
         * Fallback: una sola conversión para toda la textura.
         */
        int[] pixels = image.getRGB(
                0,
                0,
                width,
                height,
                null,
                0,
                width
        );

        return new TexturePixelData(
                pixels,
                width,
                height,
                hasAlpha
        );
    }

    public void addStatisticsByUvTriangle(
            TexturePixelData texture,
            Vector2d uv0,
            Vector2d uv1,
            Vector2d uv2
    ) {
        if (texture == null
                || texture.pixels() == null
                || uv0 == null
                || uv1 == null
                || uv2 == null) {
            return;
        }

        int width = texture.width();
        int height = texture.height();

        if (width <= 0 || height <= 0) {
            return;
        }

        double areaUv =
                calculateUvTriangleArea(uv0, uv1, uv2);

        if (areaUv <= EPSILON) {
            return;
        }

        double pixelArea =
                areaUv * width * height;

        int minSamples =
                pixelArea < 4.0 ? 1 : 4;

        int samples =
                (int) Math.ceil(pixelArea / 16.0);

        samples = clamp(
                samples,
                minSamples,
                256
        );

        int grid =
                (int) Math.ceil(Math.sqrt(samples));

        double inverseGrid = 1.0 / grid;

        for (int iy = 0; iy < grid; iy++) {
            double b = (iy + 0.5) * inverseGrid;

            for (int ix = 0; ix < grid; ix++) {
                double a = (ix + 0.5) * inverseGrid;
                double sampleB = b;

                if (a + sampleB > 1.0) {
                    a = 1.0 - a;
                    sampleB = 1.0 - sampleB;
                }

                double weight0 =
                        1.0 - a - sampleB;

                double u =
                        uv0.x * weight0
                                + uv1.x * a
                                + uv2.x * sampleB;

                double v =
                        uv0.y * weight0
                                + uv1.y * a
                                + uv2.y * sampleB;

                addStatisticsByUv(
                        texture,
                        u,
                        v
                );
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

    private void addStatisticsByUv(
            TexturePixelData texture,
            double u,
            double v
    ) {
        int width = texture.width();
        int height = texture.height();

        u = clamp(u, 0.0, 1.0);
        v = clamp(v, 0.0, 1.0);

        /*
         * Después del clamp, el cast equivale a floor()
         * para valores positivos.
         */
        int x = (int) (u * (width - 1));
        int y = (int) ((1.0 - v) * (height - 1));

        int argb =
                texture.pixels()[y * width + x];

        int alpha = texture.hasAlpha()
                ? (argb >>> 24) & 0xFF
                : 255;

        if (alpha < 16) {
            return;
        }

        int r = (argb >>> 16) & 0xFF;
        int g = (argb >>> 8) & 0xFF;
        int b = argb & 0xFF;

        if ((r | g | b) == 0) {
            return;
        }

        /*
         * Calcular directamente desde los canales enteros.
         */
        double luminance =
                (0.2126 * r
                        + 0.7152 * g
                        + 0.0722 * b)
                        / 255.0;

        int max = Math.max(r, Math.max(g, b));
        int min = Math.min(r, Math.min(g, b));

        double saturation = max == 0
                ? 0.0
                : (double) (max - min) / max;

        addPixelStatistics(
                luminance,
                saturation
        );
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

        double correctionNeed = estimateCorrectionNeed(
                meanLum,
                stddevLum,
                meanSat,
                p05,
                p50,
                p95
        );

        params.correctionStrength = correctionNeed;

        if (correctionNeed < 0.10) {
            params.enabled = false;

            params.gamma = 1.0;
            params.exposure = 1.0;
            params.contrast = 1.0;
            params.saturation = 1.0;
            params.blackPoint = 0.0;
            params.whitePoint = 1.0;

            return params;
        }

        blendParamsToNeutral(params, correctionNeed);

        return params;
    }

    public IdyllicLookParameters estimateIdyllicLookParams() {
        IdyllicLookParameters params = new IdyllicLookParameters();

        if (sampledPixels == 0) {
            return IdyllicLookParameters.disabled();
        }

        double meanLum = getMeanLuminance();
        double stddevLum = getStdDevLuminance();
        double meanSat = getMeanSaturation();

        double p05 = getLuminancePercentile(0.05);
        double p50 = getLuminancePercentile(0.50);
        double p95 = getLuminancePercentile(0.95);

        params.idyllicLookEnabled = true;

        /*
         * Fuerza general del look.
         * Más fuerte si la imagen está apagada, gris o poco saturada.
         */
        double strength = 0.45;

        if (meanSat < 0.22) {
            strength += 0.20;
        } else if (meanSat < 0.32) {
            strength += 0.12;
        }

        if (stddevLum < 0.16) {
            strength += 0.15;
        } else if (stddevLum < 0.22) {
            strength += 0.08;
        }

        if (p05 > 0.05 && meanSat < 0.32) {
            strength += 0.12; // velo gris / neblina
        }

        if (p95 > 0.82) {
            strength -= 0.10; // cuidado con escenas ya claras
        }

        params.idyllicStrength = clamp(strength, 0.35, 0.82);

        /*
         * Calidez.
         * Si ya hay amarillo o escena clara, mantener muy bajo.
         */
        params.warmBoost = 0.002;

        /*
         * Saturación.
         * Más saturación si la textura original está apagada.
         */
        if (meanSat < 0.18) {
            params.saturationBoost = 0.150;
        } else if (meanSat < 0.28) {
            params.saturationBoost = 0.125;
        } else if (meanSat < 0.38) {
            params.saturationBoost = 0.095;
        } else {
            params.saturationBoost = 0.060;
        }

        /*
         * Contraste estético.
         * Si la imagen está plana, subir más.
         */
        if (stddevLum < 0.15) {
            params.contrastBoost = 0.120;
        } else if (stddevLum < 0.22) {
            params.contrastBoost = 0.090;
        } else {
            params.contrastBoost = 0.055;
        }

        /*
         * Dehaze.
         * Más fuerte si las sombras están levantadas.
         */
        if (p05 > 0.08 && meanSat < 0.32) {
            params.dehazeBoost = 0.055;
        } else if (p05 > 0.05 && meanSat < 0.30) {
            params.dehazeBoost = 0.040;
        } else {
            params.dehazeBoost = 0.025;
        }

        /*
         * Luz en medios tonos.
         */
        if (p50 < 0.22) {
            params.midtoneLift = 0.085;
        } else if (p50 < 0.36) {
            params.midtoneLift = 0.070;
        } else {
            params.midtoneLift = 0.045;
        }

        /*
         * Proteger blancos.
         */
        if (p95 > 0.82) {
            params.highlightSoftness = 0.055;
        } else {
            params.highlightSoftness = 0.040;
        }

        /*
         * Vegetación.
         * Como ahora te gusta el look verde, lo dejamos vivo pero no radioactivo.
         */
        params.greenBoost = 0.105;
        params.greenLift = 0.28;

        /*
         * Cielo / azules.
         */
        if (meanSat < 0.25) {
            params.skyBlueBoost = 0.055;
        } else {
            params.skyBlueBoost = 0.040;
        }

        /*
         * Sombras suaves.
         */
        if (p05 < 0.025) {
            params.shadowSoftness = 0.055;
        } else {
            params.shadowSoftness = 0.040;
        }

        return params;
    }

    private static double smoothstep(
            double edge0,
            double edge1,
            double value
    ) {
        double t =
                (value - edge0) / (edge1 - edge0);

        t = clamp01(t);

        return t * t * (3.0 - 2.0 * t);
    }

    private double applyProtectedExposure(double value, double exposure, double protectionFactor) {
        double effectiveExposure = 1.0 + (exposure - 1.0) * protectionFactor;
        return value * effectiveExposure;
    }

    private void applyIdyllicLook(
            double r,
            double g,
            double b,
            IdyllicLookParameters params,
            double[] result
    ) {
        if (params == null
                || !params.idyllicLookEnabled
                || params.idyllicStrength <= 0.0) {

            result[0] = r;
            result[1] = g;
            result[2] = b;
            return;
        }

        double s = clamp(params.idyllicStrength, 0.0, 1.0);

        /*
         * 1. Dehaze estético suave.
         * Limpia el velo gris.
         */
        double dehaze = params.dehazeBoost * s;

        if (dehaze > 0.0) {
            r = applyLevels(r, dehaze, 1.0);
            g = applyLevels(g, dehaze, 1.0);
            b = applyLevels(b, dehaze, 1.0);
        }

        /*
         * 2. Midtone lift.
         * Levanta medios tonos sin quemar demasiado highlights.
         */
        double lum = calculateLuminance(r, g, b);

        double midFactor =
                smoothstep(0.15, 0.55, lum) *
                        (1.0 - smoothstep(0.65, 0.95, lum));

        double lift = params.midtoneLift * s * midFactor;

        r += (1.0 - r) * lift;
        g += (1.0 - g) * lift;
        b += (1.0 - b) * lift;

        /*
         * 3. Calidez global.
         * En clean/vivid/idyllic fresco conviene que warmBoost sea bajo.
         */
        double warm = params.warmBoost * s;

        r *= 1.0 + warm;
        g *= 1.0 + warm * 0.15;
        b *= 1.0 - warm * 0.30;

        /*
         * 4. Highlight softness.
         * Suaviza blancos y zonas muy claras.
         */
        lum = calculateLuminance(r, g, b);

        double hi = smoothstep(0.65, 0.95, lum);
        double soft = params.highlightSoftness * s * hi;

        r = r * (1.0 - soft) + lum * soft;
        g = g * (1.0 - soft) + lum * soft;
        b = b * (1.0 - soft) + lum * soft;

        /*
         * 5. Shadow softness.
         * Hace sombras más amables, menos duras.
         */
        lum = calculateLuminance(r, g, b);

        double shadow = 1.0 - smoothstep(0.12, 0.42, lum);
        double shadowLift = params.shadowSoftness * s * shadow;

        r += (0.18 - r) * shadowLift;
        g += (0.18 - g) * shadowLift;
        b += (0.20 - b) * shadowLift;

        /*
         * 6. Contraste estético.
         */
        double contrast = 1.0 + params.contrastBoost * s;

        r = (r - 0.5) * contrast + 0.5;
        g = (g - 0.5) * contrast + 0.5;
        b = (b - 0.5) * contrast + 0.5;

        /*
         * 7. Saturación estética general.
         */
        lum = calculateLuminance(r, g, b);

        double saturation = 1.0 + params.saturationBoost * s;

        r = lum + (r - lum) * saturation;
        g = lum + (g - lum) * saturation;
        b = lum + (b - lum) * saturation;

        /*
         * 8. Green / vegetation boost + green lift.
         *
         * Detector más permisivo:
         * - no exige verde puro
         * - acepta vegetación oscura, grisácea o marrón-verde
         * - aplica más lift en verdes oscuros/medios
         */
        double max = Math.max(r, Math.max(g, b));
        double min = Math.min(r, Math.min(g, b));
        double colorfulness = max - min;

        double lumNow = calculateLuminance(r, g, b);

        // Verde fuerte: G domina claramente.
        double greenDominance = g - Math.max(r, b);

        // Verde relativo: G es comparable a R/B.
        // Ayuda en vegetación oscura o grisácea.
        double greenRelative =
                (g + 0.04) - Math.max(r * 0.92, b * 0.95);

        double greenBaseA = clamp(greenDominance * 5.0, 0.0, 1.0);
        double greenBaseB = clamp(greenRelative * 4.0, 0.0, 1.0);

        double greenBase = Math.max(greenBaseA, greenBaseB);

        // No exigir demasiado color, porque la vegetación con neblina puede ser gris.
        double greenColor = clamp(colorfulness * 4.0 + 0.15, 0.0, 1.0);

        // Favorece verdes oscuros y medios, no highlights extremos.
        double darkGreenFactor = 1.0 - smoothstep(0.42, 0.78, lumNow);
        double midGreenFactor = smoothstep(0.08, 0.35, lumNow);

        double greenLumFactor = clamp(
                0.35 + darkGreenFactor * 0.45 + midGreenFactor * 0.35,
                0.0,
                1.0
        );

        double greenFactor = greenBase * greenColor * greenLumFactor;

        /*
         * Más verde.
         */
        double greenBoost = params.greenBoost * s * greenFactor;

        g *= 1.0 + greenBoost;
        r *= 1.0 - greenBoost * 0.08;
        b *= 1.0 - greenBoost * 0.05;

        /*
         * Más luminosidad en vegetación.
         * En vez de empujar hacia blanco puro, empuja hacia un verde más claro.
         */
        double greenLift = params.greenLift * s * greenFactor;

        double targetGreenR = 0.32;
        double targetGreenG = 0.62;
        double targetGreenB = 0.26;

        r += (targetGreenR - r) * greenLift * 0.35;
        g += (targetGreenG - g) * greenLift;
        b += (targetGreenB - b) * greenLift * 0.30;

        /*
         * 9. Blue/cyan boost suave.
         */
        max = Math.max(r, Math.max(g, b));
        min = Math.min(r, Math.min(g, b));
        colorfulness = max - min;

        double blueDominance = b - r;

        double blueFactor =
                clamp(blueDominance * 3.0, 0.0, 1.0) *
                        clamp(colorfulness * 2.5, 0.0, 1.0);

        double blueBoost = params.skyBlueBoost * s * blueFactor;

        b *= 1.0 + blueBoost;
        g *= 1.0 + blueBoost * 0.20;

        r = clamp(r, 0.0, 1.0);
        g = clamp(g, 0.0, 1.0);
        b = clamp(b, 0.0, 1.0);

        result[0] = clamp01(r);
        result[1] = clamp01(g);
        result[2] = clamp01(b);
    }

    public BufferedImage correctImage(
            BufferedImage source,
            TextureCorrectionParameters params,
            SunnyLookParameters sunnyParams,
            IdyllicLookParameters idyllicParams
    ) {
        if (source == null) {
            return null;
        }

        if (params == null) {
            params = new TextureCorrectionParameters();
        }

        if (sunnyParams == null) {
            sunnyParams = SunnyLookParameters.disabled();
        }

        final int width = source.getWidth();
        final int height = source.getHeight();
        final boolean hasAlpha = source.getColorModel().hasAlpha();

        BufferedImage result = new BufferedImage(
                width,
                height,
                hasAlpha
                        ? BufferedImage.TYPE_INT_ARGB
                        : BufferedImage.TYPE_INT_RGB
        );

        /*
         * Una sola conversión de la imagen de origen.
         */
        int[] sourcePixels = source.getRGB(
                0,
                0,
                width,
                height,
                null,
                0,
                width
        );

        /*
         * Escritura directa sobre la imagen de destino.
         */
        int[] resultPixels =
                ((java.awt.image.DataBufferInt)
                        result.getRaster().getDataBuffer())
                        .getData();

        final boolean useIdyllicLook =
                idyllicParams != null
                        && idyllicParams.idyllicLookEnabled
                        && idyllicParams.idyllicStrength > 0.0;

        final boolean useSunnyLook =
                !useIdyllicLook
                        && sunnyParams.sunnyLookEnabled
                        && sunnyParams.sunnyStrength > 0.0;

        final double sunnyStrength = useSunnyLook
                ? clamp01(sunnyParams.sunnyStrength)
                : 0.0;

        /*
         * Parámetros constantes calculados una vez.
         */
        double effectiveBlackPoint = params.blackPoint;

        if (useSunnyLook) {
            effectiveBlackPoint +=
                    sunnyParams.sunnyDehazeBoost
                            * sunnyStrength;
        }

        effectiveBlackPoint =
                clamp(effectiveBlackPoint, 0.0, 0.10);

        final double[] toneLut = createToneLut(
                effectiveBlackPoint,
                params.whitePoint,
                params.gamma
        );

        final double exposureDelta =
                params.exposure - 1.0;

        final double baseContrast =
                params.contrast;

        final double baseSaturation =
                params.saturation;

        final double[] idyllicRgb =
                useIdyllicLook
                        ? new double[3]
                        : null;

        for (int index = 0;
             index < sourcePixels.length;
             index++) {

            int argb = sourcePixels[index];

            int alpha = (argb >>> 24) & 0xFF;
            int r = (argb >>> 16) & 0xFF;
            int g = (argb >>> 8) & 0xFF;
            int b = argb & 0xFF;

            /*
             * Mantener el background negro.
             */
            if ((r | g | b) == 0) {
                resultPixels[index] = argb;
                continue;
            }

            /*
             * Levels + gamma mediante LUT.
             */
            double rd = toneLut[r];
            double gd = toneLut[g];
            double bd = toneLut[b];

            /*
             * Exposure protegido.
             */
            double luminanceBeforeExposure =
                    calculateLuminance(rd, gd, bd);

            double protectionFactor =
                    1.0 - smoothstep(
                            0.50,
                            0.82,
                            luminanceBeforeExposure
                    );

            double effectiveExposure =
                    1.0 + exposureDelta * protectionFactor;

            rd *= effectiveExposure;
            gd *= effectiveExposure;
            bd *= effectiveExposure;

            /*
             * Contraste.
             */
            rd = (rd - 0.5) * baseContrast + 0.5;
            gd = (gd - 0.5) * baseContrast + 0.5;
            bd = (bd - 0.5) * baseContrast + 0.5;

            /*
             * Saturación.
             */
            double luminance =
                    calculateLuminance(rd, gd, bd);

            rd = luminance
                    + (rd - luminance) * baseSaturation;

            gd = luminance
                    + (gd - luminance) * baseSaturation;

            bd = luminance
                    + (bd - luminance) * baseSaturation;

            /*
             * Conserva aquí el bloque Sunny actual.
             */
            if (useSunnyLook) {
                // Bloque Sunny Look actual.
            }

            if (useIdyllicLook) {
                applyIdyllicLook(
                        rd,
                        gd,
                        bd,
                        idyllicParams,
                        idyllicRgb
                );

                rd = idyllicRgb[0];
                gd = idyllicRgb[1];
                bd = idyllicRgb[2];
            }

            int rr = clampToByteFast(rd);
            int gg = clampToByteFast(gd);
            int bb = clampToByteFast(bd);

            resultPixels[index] = hasAlpha
                    ? (alpha << 24)
                      | (rr << 16)
                      | (gg << 8)
                      | bb
                    : (rr << 16)
                      | (gg << 8)
                      | bb;
        }

        return result;
    }

    private static double clamp01(double value) {
        if (value <= 0.0) {
            return 0.0;
        }

        if (value >= 1.0) {
            return 1.0;
        }

        return value;
    }

    private double[] createToneLut(
            double blackPoint,
            double whitePoint,
            double gamma
    ) {
        double[] lut = new double[256];

        double denominator =
                Math.max(whitePoint - blackPoint, EPSILON);

        for (int i = 0; i < 256; i++) {
            double value = i / 255.0;

            if (blackPoint > 0.0) {
                value = clamp01(
                        (value - blackPoint) / denominator
                );
            }

            lut[i] = gamma == 1.0
                    ? value
                    : Math.pow(value, gamma);
        }

        return lut;
    }

    private static int clampToByteFast(double value) {
        if (value <= 0.0) {
            return 0;
        }

        if (value >= 1.0) {
            return 255;
        }

        return (int) (value * 255.0 + 0.5);
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

    private double estimateCorrectionNeed(
            double meanLum,
            double stddevLum,
            double meanSat,
            double p05,
            double p50,
            double p95
    ) {
        double need = 0.0;

        /*
         * Imagen oscura.
         */
        if (meanLum < 0.26) {
            need += 0.35;
        } else if (meanLum < 0.32) {
            need += 0.20;
        }

        /*
         * Imagen demasiado clara / altas luces fuertes.
         */
        if (meanLum > 0.50) {
            need += 0.25;
        } else if (p95 > 0.88) {
            need += 0.20;
        }

        /*
         * Medios tonos demasiado bajos.
         */
        if (p50 < 0.22) {
            need += 0.25;
        } else if (p50 < 0.28) {
            need += 0.12;
        }

        /*
         * Neblina: sombras levantadas + poca saturación.
         */
        if (p05 > 0.06 && meanSat < 0.30) {
            need += 0.35;
        } else if (p05 > 0.04 && meanSat < 0.25) {
            need += 0.20;
        }

        /*
         * Imagen plana.
         */
        if (stddevLum < 0.14) {
            need += 0.25;
        } else if (stddevLum < 0.18) {
            need += 0.12;
        }

        /*
         * Imagen desaturada.
         */
        if (meanSat < 0.18) {
            need += 0.20;
        } else if (meanSat < 0.24) {
            need += 0.10;
        }

        return clamp(need, 0.0, 1.0);
    }

    private void blendParamsToNeutral(TextureCorrectionParameters params, double strength) {
        strength = clamp(strength, 0.0, 1.0);

        params.gamma = lerp(1.0, params.gamma, strength);
        params.exposure = lerp(1.0, params.exposure, strength);
        params.contrast = lerp(1.0, params.contrast, strength);
        params.saturation = lerp(1.0, params.saturation, strength);
        params.blackPoint = lerp(0.0, params.blackPoint, strength);

        params.whitePoint = 1.0;
    }

    private double lerp(double a, double b, double t) {
        return a + (b - a) * t;
    }
}