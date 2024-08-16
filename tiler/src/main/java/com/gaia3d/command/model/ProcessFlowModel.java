package com.gaia3d.command.model;

import java.io.IOException;

public interface ProcessFlowModel {
    public void run() throws IOException;

    public String getModelName();
}
