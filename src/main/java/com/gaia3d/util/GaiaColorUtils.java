package com.gaia3d.util;

public class GaiaColorUtils {
    public static byte[] decodeColor4(int colorCode) {
        byte r = (byte) ((colorCode >> 24) & 0xFF);
        byte g = (byte) ((colorCode >> 16) & 0xFF);
        byte b = (byte) ((colorCode >> 8) & 0xFF);
        byte a = (byte) (colorCode & 0xFF);
        return new byte[]{r, g, b, a};
    }

    public static int encodeColor4(byte r, byte g, byte b, byte a) {
        return ((r & 0xFF) << 24) | ((g & 0xFF) << 16) | ((b & 0xFF) << 8) | (a & 0xFF);
    }

    public static byte[] randomColor4() {
        byte r = (byte) (Math.random() * 255);
        byte g = (byte) (Math.random() * 255);
        byte b = (byte) (Math.random() * 255);
        byte a = (byte) (Math.random() * 255);
        return new byte[]{r, g, b, a};
    }

}
