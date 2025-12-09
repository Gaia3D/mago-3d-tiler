package com.gaia3d.converter.pointcloud.shuffler;

import java.io.File;

public interface Shufflable {
    void shuffle(File sourceFile, File targetFile, int blockSize);
    void clear();
}
