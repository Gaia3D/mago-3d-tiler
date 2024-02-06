package com.gaia3d.util;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class StringUtils {
    private static String doPaddingBytes(String text, int byteSize, String paddingChar) {
        int length = text.length();
        StringBuilder featureTableText = new StringBuilder(text);
        int featureTableJsonOffset = length % byteSize;
        if (featureTableJsonOffset != 0) {
            int padding = 8 - featureTableJsonOffset;
            featureTableText.append(" ".repeat(Math.max(0, padding)));
        }
        return featureTableText.toString();
    }

    public static String doPadding8Bytes(String text) {
        return doPaddingBytes(text, 8, " ").toString();
    }

    public static String doPadding4Bytes(String text) {
        return doPaddingBytes(text, 8, " ").toString();
    }
}
