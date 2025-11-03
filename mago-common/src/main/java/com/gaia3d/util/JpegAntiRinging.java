package com.gaia3d.util;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class JpegAntiRinging {

    public static class Options {
        public float quality = 0.90f;          // 0.0~1.0 (권장: 0.85~0.92)
        public boolean progressive = true;     // Progressive JPEG
        public boolean preBlur = true;         // 다운스케일 전 약한 블러
        public boolean multistepDownscale = true; // 멀티스텝 축소
        public int targetWidth = -1;           // 축소 목표(비율 유지하려면 하나만 지정)
        public int targetHeight = -1;
        public Color backgroundForAlpha = Color.WHITE; // 알파 제거 시 배경색
    }

    /** 메인 진입: BufferedImage -> (전처리/리사이즈) -> JPEG 파일 저장 */
    public static void writeAntiRingingJPEG(BufferedImage src, File out, Options opt) throws IOException {
        // 1) sRGB 강제 + 알파 제거(TYPE_INT_RGB)로 변환 (가끔 생기는 테두리/가마 현상 예방)
        BufferedImage img = toSRGBWithoutAlpha(src, opt.backgroundForAlpha);

        // 2) 필요시 약한 프리블러 (고주파 조금 깎아 링잉 억제)
        if (opt.preBlur) {
            img = gentlePreBlur3x3(img);
        }

        // 3) 필요시 다운스케일 (멀티스텝 + Bicubic)
        if (opt.targetWidth > 0 || opt.targetHeight > 0) {
            img = resize(img, opt.targetWidth, opt.targetHeight, opt.multistepDownscale);
        }

        // 4) JPEG 저장 (quality, progressive)
        writeJpegImageIO(img, out, opt.quality, opt.progressive);
    }

    /** sRGB로 변환 + 알파 제거(배경 합성) */
    public static BufferedImage toSRGBWithoutAlpha(BufferedImage src, Color bg) {
        // sRGB 색공간으로 변환
        ColorConvertOp toSRGB = new ColorConvertOp(ColorSpace.getInstance(ColorSpace.CS_sRGB), null);
        BufferedImage sRGB = new BufferedImage(src.getWidth(), src.getHeight(), hasAlpha(src) ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        toSRGB.filter(src, sRGB);

        if (!hasAlpha(sRGB)) return sRGB;

        // 알파를 배경색으로 합성하여 INT_RGB로 변환 (JPEG는 알파 없음)
        BufferedImage rgb = new BufferedImage(sRGB.getWidth(), sRGB.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g = rgb.createGraphics();
        g.setComposite(AlphaComposite.SrcOver);
        g.setColor(bg);
        g.fillRect(0, 0, rgb.getWidth(), rgb.getHeight());
        g.drawImage(sRGB, 0, 0, null);
        g.dispose();
        return rgb;
    }

    private static boolean hasAlpha(BufferedImage bi) {
        return bi.getColorModel().hasAlpha();
    }

    /** 약한 3x3 가우시안 근사 블러 (σ≈0.6) */
    public static BufferedImage gentlePreBlur3x3(BufferedImage src) {
        float[] k = {
                1/16f, 2/16f, 1/16f,
                2/16f, 4/16f, 2/16f,
                1/16f, 2/16f, 1/16f
        };
        ConvolveOp op = new ConvolveOp(new Kernel(3, 3, k), ConvolveOp.EDGE_NO_OP, null);
        return op.filter(src, null);
    }

    /** Bicubic 리사이즈: 큰 폭 축소는 멀티스텝(1/2씩) → 최종 크기 */
    public static BufferedImage resize(BufferedImage src, int targetW, int targetH, boolean multistep) {
        int sw = src.getWidth();
        int sh = src.getHeight();

        // 하나만 지정되면 비율 유지
        if (targetW <= 0 && targetH > 0) {
            targetW = (int) Math.round(sw * (targetH / (double) sh));
        } else if (targetH <= 0 && targetW > 0) {
            targetH = (int) Math.round(sh * (targetW / (double) sw));
        }
        if (targetW <= 0 || targetH <= 0) return src;
        if (targetW == sw && targetH == sh) return src;

        Object hint = RenderingHints.VALUE_INTERPOLATION_BICUBIC;

        if (!multistep || (targetW > sw) || (targetH > sh)) {
            // 단일 스텝(또는 업스케일)
            return scaleOnce(src, targetW, targetH, hint);
        }

        // 멀티스텝 다운스케일 (1/2씩 줄이며 품질 향상)
        BufferedImage cur = src;
        int cw = sw, ch = sh;
        while (cw / 2 >= targetW || ch / 2 >= targetH) {
            int nw = Math.max(targetW, cw / 2);
            int nh = Math.max(targetH, ch / 2);
            cur = scaleOnce(cur, nw, nh, hint);
            cw = nw; ch = nh;
        }
        if (cw != targetW || ch != targetH) {
            cur = scaleOnce(cur, targetW, targetH, hint);
        }
        return cur;
    }

    private static BufferedImage scaleOnce(BufferedImage src, int w, int h, Object interpHint) {
        BufferedImage dst = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = dst.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, interpHint);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return dst;
    }

    /** 표준 ImageIO로 JPEG 저장 (quality / progressive) */
    public static void writeJpegImageIO(BufferedImage img, File out, float quality, boolean progressive) throws IOException {
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) throw new IOException("No JPEG ImageWriter found");
        ImageWriter writer = writers.next();

        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(Math.max(0f, Math.min(1f, quality)));
        param.setProgressiveMode(progressive ? ImageWriteParam.MODE_DEFAULT : ImageWriteParam.MODE_DISABLED);

        try (ImageOutputStream ios = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(ios);
            writer.write(null, new IIOImage(img, null, null), param);
        } finally {
            writer.dispose();
        }
    }

    // 샘플 실행
    public static void main(String[] args) throws Exception {
        BufferedImage src = ImageIO.read(new File("input.png"));
        Options opt = new Options();
        opt.targetWidth = 1280;    // 비율 유지 축소
        opt.quality = 0.90f;
        opt.progressive = true;
        opt.preBlur = true;
        opt.multistepDownscale = true;
        writeAntiRingingJPEG(src, new File("out.jpg"), opt);
    }
}