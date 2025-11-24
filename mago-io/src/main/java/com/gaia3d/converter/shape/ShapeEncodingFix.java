package com.gaia3d.converter.shape;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ShapeEncodingFix {
    private final Map<String, String> ENCODING_MAP = new HashMap<>();

    public ShapeEncodingFix() {
        ENCODING_MAP.put("65001", "UTF-8");
        ENCODING_MAP.put("ANSI", "ISO-8859-1");
        ENCODING_MAP.put("UTF8", "UTF-8");
        ENCODING_MAP.put("EUC-KR", "EUC-KR");
        ENCODING_MAP.put("CP932", "MS932");
        ENCODING_MAP.put("CP936", "MS936");
        ENCODING_MAP.put("CP949", "MS949");
        ENCODING_MAP.put("CP950", "MS950");
        ENCODING_MAP.put("MS932", "MS932");
        ENCODING_MAP.put("MS936", "MS936");
        ENCODING_MAP.put("MS949", "MS949");
        ENCODING_MAP.put("MS950", "MS950");
        ENCODING_MAP.put("ISO-8859-1", "ISO-8859-1");
        ENCODING_MAP.put("UTF-16", "UTF-16");
        ENCODING_MAP.put("UTF-16LE", "UTF-16LE");
        ENCODING_MAP.put("UTF-16BE", "UTF-16BE");
        ENCODING_MAP.put("1252", "MS1252");
        ENCODING_MAP.put("932", "MS932");
        ENCODING_MAP.put("936", "MS936");
        ENCODING_MAP.put("949", "MS949");
        ENCODING_MAP.put("950", "MS950");
        // TODO: if you need more encoding, add here
    }

    /**
     * check .cpg file and get encoding, if not exist, return default(UTF-8) encoding
     * @param file File
     * @return charset Charset
     */
    public Charset detectCharset(File file) {
        File cpgFile = new File(file.getParent(), file.getName().replace(".shp", ".cpg"));

        String encoding = "UTF-8";
        if (cpgFile.exists()) {
            try {
                List<String> lines = Files.readAllLines(Path.of(cpgFile.getAbsolutePath()));
                if (!lines.isEmpty()) {
                    String firstLine = lines.get(0);
                    encoding = ENCODING_MAP.getOrDefault(firstLine, "UTF-8");
                    log.info("Detected Encoding: {}", encoding);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            return Charset.forName(encoding);
        } catch (Exception e) {
            log.error("Failed to detect encoding: {}", encoding);
            return StandardCharsets.UTF_8;
        }
    }
}
