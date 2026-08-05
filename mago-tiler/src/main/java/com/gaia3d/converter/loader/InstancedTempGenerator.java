package com.gaia3d.converter.loader;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.converter.kml.AttributeReader;
import com.gaia3d.converter.kml.TileTransformInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@AllArgsConstructor
public class InstancedTempGenerator {
    private final AttributeReader attributeReader;

    public List<File> generate(File tempPath, List<File> fileList) {
        GlobalOptions options = GlobalOptions.getInstance();
        FormatType formatType = options.getInputFormat();
        if (!(formatType.equals(FormatType.GEOJSON) || formatType.equals(FormatType.SHP) || formatType.equals(FormatType.GEO_PACKAGE))) {
            return fileList;
        }

        List<File> tempFiles = new ArrayList<>();
        for (File file : fileList) {
            tempFiles.addAll(generateTempFiles(tempPath, file));
        }
        return tempFiles;
    }

    private List<File> generateTempFiles(File tempPath, File file) {
        List<File> tempFiles = new ArrayList<>();
        List<TileTransformInfo> chunk = InstancedTempFileHelper.newChunkBuffer();
        int chunkSize = InstancedTempFileHelper.defaultChunkSize();
        int[] chunkIndex = {0};
        long[] emittedCount = {0L};

        attributeReader.readEach(file, tileTransformInfo -> {
            emittedCount[0]++;
            chunk.add(tileTransformInfo);
            if (chunk.size() >= chunkSize) {
                flushChunk(tempPath, file, tempFiles, chunk, chunkIndex[0]++, emittedCount[0]);
            }
        });

        if (!chunk.isEmpty()) {
            flushChunk(tempPath, file, tempFiles, chunk, chunkIndex[0], emittedCount[0]);
        }
        return tempFiles;
    }

    private void flushChunk(File tempPath,
                            File sourceFile,
                            List<File> tempFiles,
                            List<TileTransformInfo> chunk,
                            int chunkIndex,
                            long emittedCount) {
        long estimatedMinChunks = (emittedCount + InstancedTempFileHelper.defaultChunkSize() - 1) / InstancedTempFileHelper.defaultChunkSize();
        log.info("[Pre] Flushing instanced temp chunk {}/{} for {} (chunk items: {}, emitted instances: {})",
                chunkIndex + 1,
                estimatedMinChunks,
                sourceFile.getName(),
                chunk.size(),
                emittedCount);
        File tempFile = InstancedTempFileHelper.createTempFile(tempPath, sourceFile, chunkIndex);
        InstancedTempFileHelper.write(tempFile, new ArrayList<>(chunk));
        tempFiles.add(tempFile);
        chunk.clear();
    }
}
