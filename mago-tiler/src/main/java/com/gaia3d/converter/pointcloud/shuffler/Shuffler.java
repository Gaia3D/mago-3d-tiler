package com.gaia3d.converter.pointcloud.shuffler;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public abstract class Shuffler implements Shufflable {
    protected int processCount = 0;
    protected int totalProcessCount = 0;
}
