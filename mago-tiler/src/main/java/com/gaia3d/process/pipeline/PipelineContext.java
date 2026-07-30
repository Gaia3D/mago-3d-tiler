package com.gaia3d.process.pipeline;

import com.gaia3d.command.mago.GlobalOptions;

public interface PipelineContext {

    default GlobalOptions getGlobalOptions() {
        return GlobalOptions.getInstance();
    }
}
