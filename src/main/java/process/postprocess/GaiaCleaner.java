package process.postprocess;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import process.tileprocess.tile.ContentInfo;

import java.io.IOException;
import java.nio.file.Path;

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
            log.error("Failed to delete temp directory: {}", tempPath);
            throw new RuntimeException(e);
        }
        return contentInfo;
    }
}
