package com.gaia3d.process.postprocess;

import com.gaia3d.process.tileprocess.tile.ContentInfo;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.IOException;
import java.nio.file.Path;

@Deprecated
@Slf4j
@AllArgsConstructor
public class GaiaCleaner implements PostProcess {
    private final Path outputPath;

    @Override
    public ContentInfo run(ContentInfo contentInfo) {
        Path tempPath = outputPath.resolve("temp");
        try {
            FileUtils.deleteDirectory(tempPath.toFile());
        } catch (IOException e) {
             log.error("[ERROR] Failed to delete temp directory: {}", tempPath);
            throw new RuntimeException(e);
        }
        return contentInfo;
    }
}
