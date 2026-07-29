package com.gaia3d.process.pipeline;

import com.gaia3d.basic.types.FormatType;
import com.gaia3d.command.LoggingConfiguration;
import com.gaia3d.command.mago.GlobalOptions;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.File;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
class BatchedTilingPipelineTest {

    static {
        LoggingConfiguration.initConsoleLogger();
        GlobalOptions.getInstance();
    }

    @Test
    void execute() {
        ClassLoader classLoader = getClass().getClassLoader();
        File input = new File(classLoader.getResource("./sample-3ds").getFile());
        File output = new File(classLoader.getResource("./sample-output").getFile());
        FormatType inputType = FormatType.MAX_3DS;
        FormatType outputType = FormatType.B3DM;

        GlobalOptions globalOptions = GlobalOptions.getInstance();
        globalOptions.setInputPath(input.getAbsolutePath());
        globalOptions.setOutputPath(output.getAbsolutePath());
        globalOptions.setInputFormat(inputType);
        globalOptions.setOutputFormat(outputType);

        BatchedTilingPipeline pipeline = new BatchedTilingPipeline();
        assertDoesNotThrow(pipeline::execute);
    }
}