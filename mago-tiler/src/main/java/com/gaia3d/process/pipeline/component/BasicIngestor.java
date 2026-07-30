package com.gaia3d.process.pipeline.component;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.mago.GlobalOptions;
import com.gaia3d.process.pipeline.TilingPipelineContext;
import com.gaia3d.process.pipeline.result.IngestionResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.util.List;

@Slf4j
public class BasicIngestor implements Ingestor {
    @Override
    public void ingest(TilingPipelineContext context) {
        GlobalOptions globalOptions = context.getGlobalOptions();
        IngestionResult result = new IngestionResult();
        File inputPath = new File(globalOptions.getInputPath());

        List<File> inputFiles;
        log.info(" - Ingesting files from input path: {}", globalOptions.getInputPath());
        if (inputPath.isFile()) {
            inputFiles = List.of(inputPath);
        } else if (inputPath.isDirectory()) {
            boolean recursive = globalOptions.isRecursive();
            FormatType formatType = globalOptions.getInputFormat();
            String[] extensions = getExtensions(formatType);
            inputFiles = (List<File>) FileUtils.listFiles(inputPath, extensions, recursive);
        } else {
            throw new IllegalArgumentException("Input path is neither a file nor a directory: " + globalOptions.getInputPath());
        }
        log.info(" - Found {} files to ingest.", inputFiles.size());

        result.setSourceFiles(inputFiles);
    }

    private String[] getExtensions(FormatType formatType) {
        String[] extensions = new String[4];
        extensions[0] = formatType.getExtension().toLowerCase();
        extensions[1] = formatType.getExtension().toUpperCase();
        extensions[2] = formatType.getSubExtension().toLowerCase();
        extensions[3] = formatType.getSubExtension().toUpperCase();
        return extensions;
    }
}
