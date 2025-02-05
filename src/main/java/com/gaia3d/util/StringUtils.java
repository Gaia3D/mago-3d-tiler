package com.gaia3d.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Vector;

@Slf4j
public class StringUtils {
    public static String doPadding4Bytes(String text) {
        return doPaddingBytes(text, 4);
    }

    public static String doPadding8Bytes(String text) {
        return doPaddingBytes(text, 8);
    }

    private static String doPaddingBytes(String text, int byteSize) {
        int length = text.length();
        StringBuilder featureTableText = new StringBuilder(text);
        int featureTableJsonOffset = length % byteSize;
        if (featureTableJsonOffset != 0) {
            int padding = 8 - featureTableJsonOffset;
            featureTableText.append(" ".repeat(Math.max(0, padding)));
        }
        return featureTableText.toString();
    }

    public static void splitString(String wordToSplit, String delimiter, Vector<String> resultSplittedStrings, boolean skipEmptyStrings) {
        String[] splitStrings = wordToSplit.split(delimiter);
        for (String word : splitStrings) {
            if (skipEmptyStrings) {
                if (!word.isEmpty()) {
                    resultSplittedStrings.add(word);
                }
            } else {
                resultSplittedStrings.add(word);
            }
        }
    }

    public static String getFileNameFromPath(String path) {
        File file = new File(path);
        return file.getName();
    }

    public static String getRawFileName(String fileName) {
        return fileName.substring(0, fileName.lastIndexOf('.'));
    }

    public static Optional<String> getExtensionByStringHandling(String filename) {
        return Optional.ofNullable(filename).filter(f -> f.contains(".")).map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    public static String convertUTF8(String ascii) {
        if (ascii == null) {
            return "";
        }
        ByteBuffer buffer = StandardCharsets.UTF_8.encode(ascii);
        String utf8EncodedString = StandardCharsets.UTF_8.decode(buffer).toString();
        if (utf8EncodedString.isBlank()) {
            return "";
        }
        return utf8EncodedString;
    }

    public static String findFirstDifferentFolder(String path1, String path2) {
        String[] parts1 = Paths.get(path1).normalize().toString().split("\\\\");
        String[] parts2 = Paths.get(path2).normalize().toString().split("\\\\");

        int minLength = Math.min(parts1.length, parts2.length);
        for (int i = 0; i < minLength; i++) {
            if (!parts1[i].equals(parts2[i])) {
                return parts1[i]; // Devolvemos el primer folder diferente
            }
        }

        // Si no hay diferencias dentro del rango común
        if (parts1.length > parts2.length) {
            return parts1[minLength]; // Folder adicional en path1
        } else if (parts2.length > parts1.length) {
            return parts2[minLength]; // Folder adicional en path2
        }

        // Las rutas son iguales
        return null;
    }
}
