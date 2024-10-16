package com.gaia3d.command.model;

import java.io.IOException;

public interface ProcessFlowModel {
    void run() throws IOException;

    String getModelName();
}
