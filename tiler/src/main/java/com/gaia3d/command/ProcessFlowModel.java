package com.gaia3d.command;

import org.apache.commons.cli.CommandLine;
import java.io.IOException;

public interface ProcessFlowModel {
    public void run(CommandLine command) throws IOException;
}
