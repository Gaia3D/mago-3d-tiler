package com.gaia3d.util;

import lombok.extern.slf4j.Slf4j;

import java.util.Optional;
import java.util.Vector;
import java.util.stream.Collectors;

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

    public static void splitString(String wordToSplit, String delimiter, Vector<String> resultSplittedStrings, boolean skipEmptyStrings) {
        String[] splittedStrings = wordToSplit.split(delimiter);

        // discard strings with length zero.***
        Integer stringsCount = splittedStrings.length;
        for (Integer i = 0; i < stringsCount; i++) {
            String word = splittedStrings[i];

            if(skipEmptyStrings) {
                if (word.length() != 0) {
                    resultSplittedStrings.add(word);
                }
            } else {
                resultSplittedStrings.add(word);
            }
        }
    }

    public static String getRawFileName(String fileName) {
        String rawFileName = fileName.substring(0, fileName.lastIndexOf('.'));
        return rawFileName;
    }

    public static Optional<String> getExtensionByStringHandling(String filename) {
        // https://www.baeldung.com/java-file-extension
        return Optional.ofNullable(filename).filter(f -> f.contains(".")).map(f -> f.substring(filename.lastIndexOf(".") + 1));
    }

    public static String convertUTF8(String ascii) {
        return ascii.chars()
                .mapToObj(c -> (char) c)
                .map(c -> c < 128 ? c : '_')
                .map(String::valueOf)
                .collect(Collectors.joining());
    }
}
