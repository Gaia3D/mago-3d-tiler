package com.gaia3d.process.tileprocess;

import com.gaia3d.converter.loader.FileLoader;

import java.io.IOException;

public interface Pipeline {
    void process(FileLoader fileLoader) throws IOException;
}
