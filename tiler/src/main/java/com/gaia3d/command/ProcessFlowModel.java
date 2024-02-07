package com.gaia3d.command;

import org.apache.commons.cli.CommandLine;
import java.io.IOException;

public interface ProcessFlowModel {
    public void run() throws IOException;

    public String getModelName();
}
