package com.gaia3d.converter.loader;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gaia3d.converter.kml.TileTransformInfo;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class InstancedTempFileHelper {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final TypeReference<List<TileTransformInfo>> TILE_TRANSFORM_INFO_LIST = new TypeReference<>() {};
    private static final String TEMP_FILE_SUFFIX = ".instance-temp";
    private static final int DEFAULT_CHUNK_SIZE = 100000;

    private InstancedTempFileHelper() {
    }

    static int defaultChunkSize() {
        return DEFAULT_CHUNK_SIZE;
    }

    static boolean isTempFile(File file) {
        return file.getName().endsWith(TEMP_FILE_SUFFIX);
    }

    static File createTempFile(File tempPath, File sourceFile, int chunkIndex) {
        String tempName = UUID.randomUUID() + "_" + chunkIndex + "_" + sourceFile.getName() + TEMP_FILE_SUFFIX;
        return new File(tempPath, tempName);
    }

    static void write(File file, List<TileTransformInfo> tileTransformInfos) {
        try {
            OBJECT_MAPPER.writeValue(file, tileTransformInfos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to write instanced temp file: " + file.getAbsolutePath(), e);
        }
    }

    static List<TileTransformInfo> read(File file) {
        try {
            return OBJECT_MAPPER.readValue(file, TILE_TRANSFORM_INFO_LIST);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read instanced temp file: " + file.getAbsolutePath(), e);
        }
    }

    static List<TileTransformInfo> newChunkBuffer() {
        return new ArrayList<>(DEFAULT_CHUNK_SIZE);
    }
}
