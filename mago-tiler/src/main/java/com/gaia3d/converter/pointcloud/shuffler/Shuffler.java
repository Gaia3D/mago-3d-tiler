package com.gaia3d.converter.pointcloud.shuffler;

import java.io.File;

public interface Shuffler {
    void shuffle(File sourceFile, File targetFile, int blockSize);
    void clear();
}
