package com.gaia3d.process.tileprocess;

import com.gaia3d.converter.FileLoader;

import java.io.IOException;

public interface Pipeline {
    public void process(FileLoader fileLoader) throws IOException;
}
